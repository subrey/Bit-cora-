package com.example.jarvis.assistant

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceTranscriptSelectorTest {

    @Test
    fun `elige la alternativa que se puede interpretar`() {
        val chosen = VoiceTranscriptSelector.best(listOf("no se", "pon música"))
        assertEquals("pon música", chosen)
    }

    @Test
    fun `respeta la primera si ya se entiende`() {
        assertEquals(
            "pausa la música",
            VoiceTranscriptSelector.best(listOf("pausa la música", "otra cosa")),
        )
    }

    @Test
    fun `con palabra clave elige la que la contiene`() {
        assertEquals(
            "jarvis pon música",
            VoiceTranscriptSelector.best(
                listOf("pon música", "jarvis pon música"),
                wakeWord = "jarvis",
                requireWake = true,
            ),
        )
    }

    @Test
    fun `si ninguna lleva la palabra clave devuelve la primera`() {
        assertEquals(
            "pon música",
            VoiceTranscriptSelector.best(
                listOf("pon música", "pausa la música"),
                wakeWord = "jarvis",
                requireWake = true,
            ),
        )
    }
}
