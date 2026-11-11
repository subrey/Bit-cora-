package com.example.jarvis.assistant

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Bus interno: comunica el servicio de escucha permanente con la interfaz.
 */
object JarvisBus {

    private val _commands = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val commands: SharedFlow<String> = _commands.asSharedFlow()

    private val _spoken = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val spoken: SharedFlow<String> = _spoken.asSharedFlow()

    fun send(command: String) {
        _commands.tryEmit(command)
    }

    fun speak(text: String) {
        _spoken.tryEmit(text)
    }
}
