package com.example.jarvis.assistant

import com.example.jarvis.domain.model.ChatMessage
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatExporterTest {

    @Test
    fun `exporta la conversacion con autor y hora`() {
        val messages = listOf(
            ChatMessage(id = 1, text = "Pon música", fromUser = true, createdAt = 1_700_000_000_000),
            ChatMessage(id = 2, text = "Música en marcha", fromUser = false, createdAt = 1_700_000_001_000),
        )
        val text = ChatExporter.format(messages)
        assertTrue(text.contains("TÚ: Pon música"))
        assertTrue(text.contains("JARVIS: Música en marcha"))
        assertTrue(text.contains("HISTORIAL DE JARVIS"))
    }

    @Test
    fun `sin mensajes escribe un aviso`() {
        assertTrue(ChatExporter.format(emptyList()).contains("no hay conversación"))
    }
}
