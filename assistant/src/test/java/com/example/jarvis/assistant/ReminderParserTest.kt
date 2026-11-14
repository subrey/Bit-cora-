package com.example.jarvis.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class ReminderParserTest {

    private val baseTime = LocalDateTime.of(2026, 11, 14, 10, 0) // 14 Nov 2026 10:00 AM

    @Test
    fun `parsea recordatorio en minutos`() {
        val result = ReminderParser.parse("sacar la basura en 30 minutos", baseTime)
        assertNotNull(result)
        assertTrue(result!!.message.contains("sacar basura"))
    }

    @Test
    fun `parsea recordatorio a una hora fija con modificador de tarde`() {
        val result = ReminderParser.parse("comprar pan a las 5 de la tarde", baseTime)
        assertNotNull(result)
        assertTrue(result!!.message.contains("comprar pan"))
    }

    @Test
    fun `parsea recordatorio para manana`() {
        val result = ReminderParser.parse("reunion manana a las 9", baseTime)
        assertNotNull(result)
        assertTrue(result!!.message.contains("reunion"))
    }
}
