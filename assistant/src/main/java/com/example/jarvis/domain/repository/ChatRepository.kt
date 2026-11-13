package com.example.jarvis.domain.repository

import com.example.jarvis.domain.model.ChatMessage
import kotlinx.coroutines.flow.Flow

/**
 * Contrato del histórico de conversación.
 *
 * La implementación vive en la capa de datos (Room) y la interfaz solo ve
 * modelos de dominio. Así se puede cambiar el almacenamiento (por ejemplo,
 * cifrarlo o moverlo a otro sitio) sin tocar la UI ni el motor.
 */
interface ChatRepository {

    /** Emite la conversación completa cada vez que cambia. */
    fun observeChat(): Flow<List<ChatMessage>>

    /** Número de mensajes guardados. */
    suspend fun chatCount(): Int

    /** Añade un mensaje del usuario o del asistente. */
    suspend fun addChat(text: String, fromUser: Boolean)

    /** Borra todo el histórico. */
    suspend fun clearChat()

    /** Últimos mensajes escritos por el usuario (contexto para la IA). */
    suspend fun recentUserMessages(limit: Int): List<String>

    /** Últimos mensajes completos (usuario y asistente) en orden cronológico. */
    suspend fun recentMessages(limit: Int): List<ChatMessage>
}
