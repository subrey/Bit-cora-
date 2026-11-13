package com.example.jarvis.ui

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.jarvis.JarvisApplication
import com.example.jarvis.assistant.AssistantResponse
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

    /** El micrófono de la app ha apartado temporalmente al servicio de escucha. */
    private var micBorrowedFromService = false

    /** El usuario ha cortado una respuesta de JARVIS. */
    private var interrupted = false
    private var retriedBusy = false

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
                    runCatching { app.speaker.speak(response.text) }
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
                        startVoice()
                    } else {
                        releaseServiceMic()
                    }
                }
            }
        }
    }

    // --- Voz ------------------------------------------------------------

    /** Abre el micrófono de la app (sin el diálogo ni los pitidos del sistema). */
    fun startVoice() {
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
        retriedBusy = false
        _liveTranscript.value = ""
        _state.value = AssistantState.LISTENING

        // Dos reconocedores no pueden usar el micrófono a la vez: se aparta
        // el servicio de palabra clave mientras dure esta conversación.
        val justStoppedService = AlwaysListeningService.isRunning
        if (justStoppedService) {
            micBorrowedFromService = true
            AlwaysListeningService.stop(context)
        }

        viewModelScope.launch {
            if (justStoppedService) delay(350)
            if (_state.value == AssistantState.LISTENING) {
                ensureRecognizer().start(onDeviceVoice.value)
            }
        }
    }

    fun stopVoice() {
        recognizer?.stop()
        _liveTranscript.value = ""
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
        if (!visible) {
            stopVoice()
            if (alwaysListening.value) AlwaysListeningService.start(getApplication<Application>())
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
            },
            onPartial = { text -> _liveTranscript.value = text },
            onResult = { text ->
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
        if (VoicePolicy.isTransientError(error) &&
            !retriedBusy &&
            _state.value == AssistantState.LISTENING
        ) {
            // El micrófono aún se estaba liberando: un reintento silencioso.
            retriedBusy = true
            viewModelScope.launch {
                delay(650)
                if (_state.value == AssistantState.LISTENING) {
                    ensureRecognizer().start(onDeviceVoice.value)
                }
            }
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
        if (!micBorrowedFromService) return
        if (_state.value == AssistantState.LISTENING) return
        micBorrowedFromService = false
        if (alwaysListening.value) AlwaysListeningService.start(getApplication<Application>())
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
        app.musicPlayer.scanLibrary()
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
        super.onCleared()
    }

    companion object {
        fun bus(): JarvisBus = JarvisBus
    }
}
