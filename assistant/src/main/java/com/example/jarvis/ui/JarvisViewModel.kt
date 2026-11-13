package com.example.jarvis.ui

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.jarvis.JarvisApplication
import com.example.jarvis.ai.AiResult
import com.example.jarvis.ai.GeminiProvider
import com.example.jarvis.assistant.AssistantResponse
import com.example.jarvis.assistant.CrashReporter
import com.example.jarvis.assistant.IntentParser
import com.example.jarvis.assistant.JarvisBus
import com.example.jarvis.assistant.VoicePolicy
import com.example.jarvis.assistant.voice.VoiceRecognizer
import com.example.jarvis.data.MemoryItem
import com.example.jarvis.data.TimerItem
import com.example.jarvis.data.TodoItem
import com.example.jarvis.domain.model.AssistantState
import com.example.jarvis.domain.model.ChatMessage
import com.example.jarvis.service.AlwaysListeningService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as JarvisApplication
    private val repository = app.repository
    private val settings = app.settings
    private val engine = app.engine

    val todos: StateFlow<List<TodoItem>> = repository.observeTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val memories: StateFlow<List<MemoryItem>> = repository.observeMemories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val timers: StateFlow<List<TimerItem>> = repository.observeTimers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val playerState = app.musicPlayer.state

    val ttsEnabled: StateFlow<Boolean> = settings.ttsEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val alwaysListening: StateFlow<Boolean> = settings.alwaysListening
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val wakeWord: StateFlow<String> = settings.wakeWord
        .stateIn(viewModelScope, SharingStarted.Eagerly, "jarvis")

    val accent: StateFlow<String> = settings.accent
        .stateIn(viewModelScope, SharingStarted.Eagerly, "green")

    /** Voz en el dispositivo (sin pitidos) frente al motor normal. */
    val onDeviceVoice: StateFlow<Boolean> = settings.onDeviceVoice
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** Tras una orden hablada, ¿se reabre el micrófono para seguir hablando? */
    val continuousVoice: StateFlow<Boolean> = settings.continuousVoice
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun setOnDeviceVoice(value: Boolean) {
        viewModelScope.launch { settings.setOnDeviceVoice(value) }
    }

    fun setContinuousVoice(value: Boolean) {
        viewModelScope.launch { settings.setContinuousVoice(value) }
    }

    // --- IA conversacional (Fase 4) --------------------------------------

    private val secure = app.secureSettings

    /** ¿Hay una clave de Gemini guardada de forma cifrada? */
    private val _aiConfigured = MutableStateFlow(secure.hasKey())
    val aiConfigured: StateFlow<Boolean> = _aiConfigured.asStateFlow()

    /** Modelo configurado, o el gratuito por defecto. */
    private val _aiModel = MutableStateFlow(
        GeminiProvider.effectiveModel(secure.model()),
    )
    val aiModel: StateFlow<String> = _aiModel.asStateFlow()

    /** Resultado legible del último guardado o prueba de conexión. */
    private val _aiStatus = MutableStateFlow<String?>(null)
    val aiStatus: StateFlow<String?> = _aiStatus.asStateFlow()

    fun saveAiKey(key: String) {
        val clean = key.trim()
        if (clean.isBlank()) {
            _aiStatus.value = "Pegue una clave de API válida."
            return
        }
        if (!secure.available) {
            _aiStatus.value = "El almacenamiento cifrado no está disponible en este teléfono."
            return
        }
        viewModelScope.launch {
            runCatching { secure.setApiKey(clean) }
                .onSuccess {
                    _aiConfigured.value = true
                    _aiStatus.value = "Clave guardada de forma cifrada."
                }
                .onFailure {
                    _aiStatus.value = "No se pudo guardar la clave de forma cifrada."
                }
        }
    }

    fun clearAiKey() {
        viewModelScope.launch {
            runCatching { secure.setApiKey("") }
            _aiConfigured.value = false
            _aiStatus.value = "Clave de IA borrada."
        }
    }

    fun saveAiModel(value: String) {
        val clean = GeminiProvider.effectiveModel(value)
        viewModelScope.launch {
            runCatching { secure.setModel(clean) }
            _aiModel.value = clean
            _aiStatus.value = "Modelo guardado: $clean."
        }
    }

    fun testAi() {
        if (!secure.hasKey()) {
            _aiStatus.value = "Configure primero la clave de IA."
            return
        }
        viewModelScope.launch {
            _aiStatus.value = "Probando conexión con la IA…"
            val result = runCatching { app.aiProvider.test() }
                .getOrElse { AiResult.Failure("Error inesperado al probar la IA.") }
            _aiStatus.value = when (result) {
                is AiResult.Success -> "Conexión correcta: la IA ha respondido."
                is AiResult.Failure -> "Error: ${result.message}"
            }
        }
    }

    /** Histórico completo de la conversación (persistente entre sesiones). */
    val messages: StateFlow<List<ChatMessage>> = repository.observeChat()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _state = MutableStateFlow(AssistantState.IDLE)

    /** Estado del núcleo: en espera, escuchando, procesando o hablando. */
    val assistantState: StateFlow<AssistantState> = _state.asStateFlow()

    /** Lo que el reconocedor va entendiendo, para mostrarlo en pantalla. */
    private val _liveTranscript = MutableStateFlow("")
    val liveTranscript: StateFlow<String> = _liveTranscript.asStateFlow()

    private var recognizer: VoiceRecognizer? = null
    private var appVisible = false

    /** El usuario ha cortado una respuesta de JARVIS. */
    private var interrupted = false

    /** Reintentos silenciosos cuando el micrófono aún está ocupado. */
    private var busyRetries = 0

    /** Evita cerrar el micro por un onStop pasajero (permiso, overlay…). */
    private var visibilityJob: Job? = null

    /** Identifica el turno actual: un envío nuevo invalida los avisos del anterior. */
    private var turnId = 0

    init {
        viewModelScope.launch {
            runCatching {
                if (repository.chatCount() == 0) {
                    repository.addChat(
                        "Buenos días, señor. Jarvis en línea. Escriba o diga una orden, " +
                            "por ejemplo «pon música».",
                        fromUser = false,
                    )
                }
                // Si la sesión anterior terminó con un cierre, se explica aquí.
                CrashReporter.consume(getApplication<Application>())?.let { report ->
                    repository.addChat(report, fromUser = false)
                }
            }
        }
    }

    /**
     * Envía una orden al asistente y guarda la respuesta en el histórico.
     *
     * [fromVoice] indica que la orden llegó por el micrófono: si la
     * conversación continua está activa, al acabar de hablar se vuelve a
     * escuchar sin que el usuario toque nada.
     */
    fun submit(text: String, fromVoice: Boolean = false) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        // Si la música se bajó para escuchar la orden, vuelve a su volumen
        // mientras se procesa; si JARVIS responde, se bajará otra vez.
        app.musicPlayer.restoreAfterWakeWord()
        val currentTurn = ++turnId
        viewModelScope.launch {
            interrupted = false
            // Si estaba hablando y llega una orden nueva (escrita o del bus),
            // se calla para atenderla.
            if (_state.value == AssistantState.SPEAKING) app.speaker.stop()
            _state.value = AssistantState.THINKING
            try {
                runCatching { repository.addChat(clean, fromUser = true) }
                val intent = IntentParser.parse(clean)
                val response = runCatching { engine.execute(intent) }.getOrElse {
                    AssistantResponse("Se me ha complicado la orden, señor. ¿Puede repetirla?")
                }
                runCatching { repository.addChat(response.text, fromUser = false) }
                if (response.speak && ttsEnabled.value) {
                    // Mientras Jarvis habla, el micro se pausa: así no se oye a
                    // sí mismo ni se pierde la siguiente orden.
                    _state.value = AssistantState.SPEAKING
                    JarvisBus.speaking.value = true
                    app.musicPlayer.duckForWakeWord()
                    runCatching { app.speaker.speak(response.text) }
                    app.musicPlayer.restoreAfterWakeWord()
                }
            } finally {
                // Un turno viejo no debe pisar el estado del turno actual.
                if (currentTurn != turnId) {
                    if (_state.value != AssistantState.SPEAKING) {
                        JarvisBus.speaking.value = false
                    }
                } else {
                    JarvisBus.speaking.value = false
                    if (_state.value == AssistantState.SPEAKING ||
                        _state.value == AssistantState.THINKING
                    ) {
                        _state.value = AssistantState.IDLE
                    }
                    if (VoicePolicy.shouldAutoListen(
                            continuous = continuousVoice.value,
                            fromVoice = fromVoice,
                            appVisible = appVisible,
                            interrupted = interrupted,
                        )
                    ) {
                        // Pequeña pausa: deja que el motor de voz y el altavoz
                        // se liberen antes de reabrir el micrófono.
                        startVoice(delayMillis = 500)
                    } else {
                        releaseServiceMic()
                    }
                }
            }
        }
    }

    // --- Voz ------------------------------------------------------------

    /** Abre el micrófono de la app (sin el diálogo ni los pitidos del sistema). */
    fun startVoice(delayMillis: Long = 0L) {
        val context = getApplication<Application>()
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            JarvisBus.notice("⚠ Conceda el permiso de micrófono para hablar con Jarvis.")
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _state.value = AssistantState.IDLE
            systemMessage(
                "No encuentro un motor de reconocimiento de voz en este teléfono. " +
                    "Puede escribir la orden o instalar la aplicación «Voz» de Google.",
            )
            return
        }
        if (_state.value == AssistantState.LISTENING) return

        interrupted = false
        busyRetries = 0
        _liveTranscript.value = ""
        _state.value = AssistantState.LISTENING

        // La app pasa a ser la dueña del micrófono: el servicio de palabra
        // clave se pausa al ver esta señal y destruye su reconocedor.
        JarvisBus.appMic.value = true

        viewModelScope.launch {
            if (delayMillis > 0) delay(delayMillis)
            // El servicio se pausa solo al ver appMic y destruye su
            // reconocedor. Se espera a que lo suelte; si no está corriendo,
            // no hay nada que esperar.
            var waited = 0L
            while (AlwaysListeningService.isRunning &&
                !AlwaysListeningService.micReleased &&
                waited < 1_500L
            ) {
                delay(50)
                waited += 50
            }
            // Pequeño margen para que el sistema cierre la sesión anterior.
            delay(300)
            if (_state.value == AssistantState.LISTENING) {
                beginListening()
            }
        }
    }

    fun stopVoice() {
        recognizer?.stop()
        _liveTranscript.value = ""
        app.musicPlayer.restoreAfterWakeWord()
        if (_state.value == AssistantState.LISTENING) {
            _state.value = AssistantState.IDLE
        }
        releaseServiceMic()
    }

    /**
     * Un solo botón: escuchar, detener la escucha o interrumpir a JARVIS para
     * tomar la palabra.
     */
    fun toggleVoice() {
        when (_state.value) {
            AssistantState.LISTENING -> stopVoice()
            AssistantState.SPEAKING -> {
                interrupted = true
                app.speaker.stop()
                startVoice()
            }
            AssistantState.THINKING -> Unit
            else -> startVoice()
        }
    }

    /** La app pasa a primer plano o se oculta. */
    fun setAppVisible(visible: Boolean) {
        appVisible = visible
        visibilityJob?.cancel()
        visibilityJob = null
        if (!visible) {
            // Un onStop pasajero (permiso, overlay del reconocedor…) no debe
            // cerrar el micrófono nada más abrirlo. Se espera un poco.
            visibilityJob = viewModelScope.launch {
                delay(700)
                if (!appVisible) {
                    stopVoice()
                    if (alwaysListening.value) {
                        AlwaysListeningService.start(getApplication<Application>())
                    }
                }
            }
        }
    }

    /**
     * Arranca el reconocedor sin que ningún fallo inesperado pueda cerrar la
     * app: si algo va mal, se anota y se trata como un error de voz normal.
     */
    private fun beginListening() {
        runCatching { ensureRecognizer().start(onDeviceVoice.value) }
            .onFailure {
                CrashReporter.note(getApplication<Application>(), "Abrir micrófono", it)
                handleVoiceError(SpeechRecognizer.ERROR_CLIENT)
            }
    }

    private fun ensureRecognizer(): VoiceRecognizer {
        recognizer?.let { return it }
        val created = VoiceRecognizer(
            context = getApplication<Application>(),
            onReady = {},
            onSpeechStart = {
                // Si estaba hablando y empieza una voz, se calla para escuchar.
                if (JarvisBus.speaking.value) {
                    interrupted = true
                    app.speaker.stop()
                }
                // Con música de fondo, bajarla al empezar a hablar facilita
                // que el reconocedor entienda la orden.
                app.musicPlayer.duckForWakeWord()
            },
            onPartial = { text -> _liveTranscript.value = text },
            onResult = { text ->
                busyRetries = 0
                _liveTranscript.value = ""
                if (_state.value == AssistantState.LISTENING) {
                    if (text.isBlank()) stopVoice() else submit(text, fromVoice = true)
                }
            },
            onError = ::handleVoiceError,
        )
        recognizer = created
        return created
    }

    private fun handleVoiceError(error: Int) {
        _liveTranscript.value = ""
        app.musicPlayer.restoreAfterWakeWord()
        if (VoicePolicy.isTransientError(error) &&
            _state.value == AssistantState.LISTENING &&
            busyRetries < 4
        ) {
            // El micrófono aún se estaba liberando: reintentos silenciosos.
            // Cada intento crea un reconocedor nuevo si el anterior falló.
            busyRetries++
            val wait = 500L * busyRetries
            viewModelScope.launch {
                delay(wait)
                if (_state.value == AssistantState.LISTENING) {
                    beginListening()
                }
            }
            return
        }
        if (VoicePolicy.isTransientError(error)) {
            stopVoice()
            JarvisBus.notice(
                "⚠ El micrófono sigue ocupado (código $error). " +
                    "Espere un momento y vuelva a intentarlo."
            )
            return
        }
        if (VoicePolicy.isSilenceError(error)) {
            stopVoice()
            return
        }
        if (error == VoiceRecognizer.ERROR_UNAVAILABLE) {
            stopVoice()
            systemMessage(
                "No encuentro un motor de reconocimiento de voz en este teléfono. " +
                    "Puede escribir la orden o instalar la aplicación «Voz» de Google.",
            )
            return
        }
        stopVoice()
        when (error) {
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                JarvisBus.notice("⚠ Falta el permiso de micrófono para escucharle.")

            SpeechRecognizer.ERROR_NETWORK,
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
            SpeechRecognizer.ERROR_SERVER ->
                JarvisBus.notice(
                    "⚠ No he podido conectar con el servicio de voz. " +
                        "Compruebe la conexión o active «Voz en el dispositivo».",
                )

            SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
            SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE,
            SpeechRecognizer.ERROR_CANNOT_CHECK_SUPPORT ->
                if (onDeviceVoice.value) {
                    // El modelo local no sirve en este teléfono: se apaga para
                    // que la próxima orden use el motor normal.
                    viewModelScope.launch { settings.setOnDeviceVoice(false) }
                    JarvisBus.notice(
                        "⚠ El modelo de voz en el dispositivo no está disponible. " +
                            "Descárguelo en los ajustes de Google o desactive " +
                            "«Voz en el dispositivo».",
                    )
                }
        }
    }

    /** Devuelve el micrófono al servicio de palabra clave si lo tenía prestado. */
    private fun releaseServiceMic() {
        if (_state.value == AssistantState.LISTENING) return
        app.musicPlayer.restoreAfterWakeWord()
        // Suelta el reconocedor de la app antes de devolver el micrófono:
        // así el servicio no se encuentra una instancia a medias.
        recognizer?.release()
        // La conversación ha terminado: el servicio puede volver a escuchar.
        JarvisBus.appMic.value = false
        if (alwaysListening.value) {
            // También cubre el caso de que el servicio estuviera apagándose:
            // startService lo relanza si hace falta.
            AlwaysListeningService.start(getApplication<Application>())
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearChat()
            repository.addChat("Sesión reiniciada. ¿En qué puedo ayudarle?", fromUser = false)
        }
    }

    // --- Música ---------------------------------------------------------

    fun togglePlayback() {
        app.musicPlayer.toggle()
    }

    fun playTrack(track: com.example.jarvis.assistant.Track) {
        app.musicPlayer.play(track)
    }

    fun scanMusic() {
        // La biblioteca puede ser grande: el escaneo va fuera del hilo principal.
        viewModelScope.launch {
            withContext(Dispatchers.IO) { app.musicPlayer.scanLibrary() }
        }
    }

    fun setVolume(value: Float) {
        app.musicPlayer.setVolume(value)
    }

    fun setShuffle(enabled: Boolean) {
        app.musicPlayer.setShuffle(enabled)
    }

    fun setRepeat(enabled: Boolean) {
        app.musicPlayer.setRepeat(enabled)
    }

    fun playDemoTone() {
        app.musicPlayer.playDemoTone()
    }

    fun toggleTodo(item: TodoItem) {
        viewModelScope.launch { repository.toggleTodo(item) }
    }

    fun deleteTodo(item: TodoItem) {
        viewModelScope.launch { repository.deleteTodo(item) }
    }

    fun addTodo(text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch { repository.addTodo(clean) }
    }

    fun addMemory(text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch { repository.addMemory(clean) }
    }

    fun deleteMemory(item: MemoryItem) {
        viewModelScope.launch { repository.deleteMemory(item) }
    }

    fun deleteTimer(item: TimerItem) {
        viewModelScope.launch {
            app.scheduler.cancel(item.id)
            repository.deleteTimer(item)
        }
    }

    fun setTtsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settings.setTtsEnabled(enabled)
            if (!enabled) app.speaker.stop()
        }
    }

    fun setAlwaysListening(enabled: Boolean) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            if (enabled) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO,
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) {
                    JarvisBus.notice(
                        "⚠ Necesito el permiso de micrófono para escuchar de forma permanente."
                    )
                    settings.setAlwaysListening(false)
                    return@launch
                }
                settings.setAlwaysListening(true)
                AlwaysListeningService.start(context)
            } else {
                settings.setAlwaysListening(false)
                AlwaysListeningService.stop(context)
            }
        }
    }

    /** Mensaje del sistema dentro de la conversación. */
    fun systemMessage(text: String) {
        viewModelScope.launch { repository.addChat(text, fromUser = false) }
    }

    fun setWakeWord(word: String) {
        val clean = word.trim().lowercase()
        if (clean.isEmpty()) return
        viewModelScope.launch { settings.setWakeWord(clean) }
    }

    fun setAccent(accent: String) {
        viewModelScope.launch { settings.setAccent(accent) }
    }

    /** Envía al bus una orden detectada por el servicio de escucha permanente. */
    fun forwardFromBus(command: String) {
        submit(command, fromVoice = true)
    }

    fun speak(text: String) {
        viewModelScope.launch { app.speaker.speak(text) }
    }

    override fun onCleared() {
        recognizer?.destroy()
        recognizer = null
        app.musicPlayer.restoreAfterWakeWord()
        // Si la app muere con el micrófono en la mano, el servicio debe poder
        // recuperarlo en el siguiente arranque.
        JarvisBus.appMic.value = false
        if (alwaysListening.value) {
            AlwaysListeningService.start(getApplication<Application>())
        }
        super.onCleared()
    }

    companion object {
        fun bus(): JarvisBus = JarvisBus
    }
}
