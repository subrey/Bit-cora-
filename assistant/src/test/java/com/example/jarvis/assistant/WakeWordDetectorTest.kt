package com.example.jarvis.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WakeWordDetectorTest {

    @Test
    fun `reconoce la palabra clave exacta`() {
        assertTrue(WakeWordDetector.isWakeWord("Jarvis", "jarvis"))
        assertTrue(WakeWordDetector.isWakeWord("jarvis.", "jarvis"))
    }

    @Test
    fun `tolera un error de letra`() {
        assertTrue(WakeWordDetector.isWakeWord("halvis", "jarvis"))
        assertTrue(WakeWordDetector.isWakeWord("jarvls", "jarvis"))
        assertTrue(WakeWordDetector.isWakeWord("javis", "jarvis"))
        assertTrue(WakeWordDetector.isWakeWord("jarvís", "jarvis"))
    }

    @Test
    fun `no confunde palabras parecidas`() {
        assertFalse(WakeWordDetector.isWakeWord("jardin", "jarvis"))
        assertFalse(WakeWordDetector.isWakeWord("salir", "jarvis"))
    }

    @Test
    fun `extrae la orden despues de la palabra clave`() {
        assertEquals(
            "pon música",
            WakeWordDetector.extractCommand("Oye jarvis, pon música", "jarvis"),
        )
    }

    @Test
    fun `extrae la orden con la palabra clave mal oida`() {
        assertEquals(
            "pausa la música",
            WakeWordDetector.extractCommand("Oye jarvis pausa la música", "jarvis"),
        )
        assertEquals(
            "pausa la música",
            WakeWordDetector.extractCommand("pausa la música halvis", "jarvis"),
        )
    }

    @Test
    fun `devuelve null si no hay palabra clave`() {
        assertNull(WakeWordDetector.extractCommand("pon música", "jarvis"))
    }

    @Test
    fun `devuelve null si solo dice la palabra clave`() {
        assertNull(WakeWordDetector.extractCommand("Oye jarvis", "jarvis"))
    }

    @Test
    fun `la distancia de levenshtein funciona`() {
        assertEquals(0, WakeWordDetector.distance("jarvis", "jarvis"))
        assertEquals(2, WakeWordDetector.distance("jarvis", "halvis"))
        assertEquals(3, WakeWordDetector.distance("jarvis", "halvas"))
        assertEquals(2, WakeWordDetector.distance("jarvis", "jardin"))
    }
}
