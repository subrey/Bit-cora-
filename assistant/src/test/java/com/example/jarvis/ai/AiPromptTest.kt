package com.example.jarvis.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiPromptTest {

    @Test
    fun `sin recuerdos devuelve el prompt intacto`() {
        assertEquals("base", AiPrompt.systemPrompt("base", emptyList()))
    }

    @Test
    fun `anade los recuerdos al prompt`() {
        val prompt = AiPrompt.systemPrompt("base", listOf("hotel 123", " "))
        assertTrue(prompt.startsWith("base"))
        assertTrue(prompt.contains("- hotel 123"))
    }

    @Test
    fun `no duplica el mensaje actual`() {
        val history = listOf(
            AiTurn(fromUser = true, text = "hola"),
            AiTurn(fromUser = false, text = "buenas"),
            AiTurn(fromUser = true, text = "¿qué tal?"),
        )
        val turns = AiPrompt.turns(history, "¿qué tal?")
        assertEquals(3, turns.size)
        assertEquals("¿qué tal?", turns.last().text)
    }

    @Test
    fun `anade el mensaje actual si no esta en el historial`() {
        val turns = AiPrompt.turns(listOf(AiTurn(fromUser = true, text = "hola")), "nueva pregunta")
        assertEquals(2, turns.size)
        assertEquals("nueva pregunta", turns.last().text)
        assertTrue(turns.last().fromUser)
    }

    @Test
    fun `recorta el historial y respeta el maximo`() {
        val history = (1..20).map { AiTurn(fromUser = true, text = "mensaje $it") }
        val turns = AiPrompt.turns(history, "actual")
        assertEquals(AiPrompt.MAX_TURNS, turns.size)
        assertEquals("mensaje 10", turns.first().text)
        assertEquals("actual", turns.last().text)
    }
}
