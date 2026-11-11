package com.example.jarvis.ui

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.jarvis.JarvisApplication
import com.example.jarvis.assistant.AssistantResponse
import com.example.jarvis.assistant.IntentParser
import com.example.jarvis.assistant.JarvisBus
import com.example.jarvis.data.MemoryItem
import com.example.jarvis.data.TimerItem
import com.example.jarvis.data.TodoItem
import com.example.jarvis.service.AlwaysListeningService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: Long,
    val text: String,
    val fromUser: Boolean,
)

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

    fun setOnDeviceVoice(value: Boolean) {
        viewModelScope.launch { settings.setOnDeviceVoice(value) }
    }

    /** Histórico completo de la conversación (persistente entre sesiones). */
    val messages: StateFlow<List<ChatMessage>> = repository.observeChat()
        .map { entries -> entries.map { ChatMessage(it.id, it.text, it.fromUser) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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

    /** Envía una orden al asistente y guarda la respuesta en el histórico. */
    fun submit(text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            runCatching { repository.addChat(clean, fromUser = true) }
            val intent = IntentParser.parse(clean)
            val response = runCatching { engine.execute(intent) }.getOrElse {
                AssistantResponse("Se me ha complicado la orden, señor. ¿Puede repetirla?")
            }
            runCatching { repository.addChat(response.text, fromUser = false) }
            if (response.speak && ttsEnabled.value) {
                app.speaker.speak(response.text)
            }
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
        submit(command)
    }

    fun speak(text: String) {
        viewModelScope.launch { app.speaker.speak(text) }
    }

    companion object {
        fun bus(): JarvisBus = JarvisBus
    }
}
