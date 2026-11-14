package com.example.jarvis.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class AlarmParserTest {

    private val base = LocalDateTime.of(2026, 11, 14, 22, 0)

    @Test
    fun `detecta frases de alarma`() {
        assertTrue(AlarmParser.looksLikeAlarm("despiértame a las 7"))
        assertTrue(AlarmParser.looksLikeAlarm("pon una alarma a las 6:30"))
        assertFalse(AlarmParser.looksLikeAlarm("pon un temporizador de 5 minutos"))
    }

    @Test
    fun `alarma diaria a una hora`() {
        val spec = AlarmParser.parse("despiértame a las 7", base)
        assertNotNull(spec)
        spec!!
        assertEquals(7, spec.hour)
        assertEquals(0, spec.minute)
        assertTrue(spec.repeatDaily)
    }

    @Test
    fun `alarma con minutos`() {
        val spec = AlarmParser.parse("pon una alarma a las 6:30", base)
        assertNotNull(spec)
        spec!!
        assertEquals(6, spec.hour)
        assertEquals(30, spec.minute)
    }

    @Test
    fun `alarma relativa en minutos`() {
        val spec = AlarmParser.parse("despiértame en 20 minutos", base)
        assertNotNull(spec)
        spec!!
        assertEquals(22, spec.hour)
        assertEquals(20, spec.minute)
        assertFalse(spec.repeatDaily)
    }

    @Test
    fun `alarma de manana es de una vez`() {
        val spec = AlarmParser.parse("alarma mañana a las 8", base)
        assertNotNull(spec)
        spec!!
        assertEquals(8, spec.hour)
        assertFalse(spec.repeatDaily)
    }
}