package com.example.jarvis.domain.model

/**
 * Mensaje de la conversación, ya en el lenguaje del dominio.
 *
 * La capa de datos (Room) guarda [com.example.jarvis.data.ChatEntry] y la
 * traduce a este modelo: la interfaz y los casos de uso nunca dependen de
 * columnas ni de anotaciones de base de datos.
 */
data class ChatMessage(
    val id: Long,
    val text: String,
    val fromUser: Boolean,
    val createdAt: Long = System.currentTimeMillis(),
) {
    /** Hora corta para pintar el mensaje, por ejemplo «21:04». */
    fun timeLabel(): String = java.time.format.DateTimeFormatter
        .ofPattern("HH:mm")
        .format(java.time.Instant.ofEpochMilli(createdAt).atZone(java.time.ZoneId.systemDefault()))
}
