package com.example.jarvis.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class JarvisPersonaTest {

    @Test
    fun `el prompt incluye la fecha en espanol`() {
        val prompt = JarvisPersona.systemPromptForToday(LocalDate.of(2026, 11, 13))
        assertTrue(prompt.contains("viernes 13 de noviembre de 2026"))
    }

    @Test
    fun `el prompt define la personalidad y los limites`() {
        val prompt = JarvisPersona.systemPromptForToday(LocalDate.of(2026, 11, 13))
        assertTrue(prompt.contains("JARVIS"))
        assertTrue(prompt.contains("nunca inventes"))
        assertFalse(prompt.contains("%s"))
    }

    @Test
    fun `los estados ocupados se reconocen`() {
        assertTrue(AssistantState.THINKING.isBusy)
        assertTrue(AssistantState.SPEAKING.isBusy)
        assertFalse(AssistantState.IDLE.isBusy)
        assertFalse(AssistantState.LISTENING.isBusy)
        assertEquals("EN ESPERA", AssistantState.IDLE.label)
    }
}
