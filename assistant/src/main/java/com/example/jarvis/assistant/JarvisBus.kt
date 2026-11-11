package com.example.jarvis.assistant

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Bus interno: comunica el servicio de escucha permanente con la interfaz.
 */
object JarvisBus {

    private val _commands = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val commands: SharedFlow<String> = _commands.asSharedFlow()

    private val _notices = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val notices: SharedFlow<String> = _notices.asSharedFlow()

    /** true mientras el micrófono está capturando de verdad. */
    val listening: MutableStateFlow<Boolean> = MutableStateFlow(false)

    /** true mientras Jarvis está hablando: el micro se apaga para no oírse. */
    val speaking: MutableStateFlow<Boolean> = MutableStateFlow(false)

    fun send(command: String) {
        _commands.tryEmit(command)
    }

    fun notice(text: String) {
        _notices.tryEmit(text)
    }
}
