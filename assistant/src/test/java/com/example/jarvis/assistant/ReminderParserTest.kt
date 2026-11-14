package com.example.jarvis.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class ReminderParserTest {

    private val baseTime = LocalDateTime.of(2026, 11, 14, 10, 0) // sábado 14 de noviembre, 10:00

    @Test
    fun `parsea recordatorio en minutos`() {
        val result = ReminderParser.parse("sacar la basura en 30 minutos", baseTime)
        assertNotNull(result)
        assertTrue(result!!.message.contains("sacar la basura"))
        val expected = baseTime.plusMinutes(30)
            .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertEquals(expected, result.triggerAtMillis)
    }

    @Test
    fun `parsea recordatorio a una hora fija de la tarde`() {
        val result = ReminderParser.parse("comprar pan a las 5 de la tarde", baseTime)
        assertNotNull(result)
        assertEquals("comprar pan", result!!.message)
        val expected = LocalDateTime.of(2026, 11, 14, 17, 0)
            .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertEquals(expected, result.triggerAtMillis)
    }

    @Test
    fun `parsea recordatorio para manana`() {
        val result = ReminderParser.parse("reunion mañana a las 9", baseTime)
        assertNotNull(result)
        assertEquals("reunion", result!!.message)
        val expected = LocalDateTime.of(2026, 11, 15, 9, 0)
            .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertEquals(expected, result.triggerAtMillis)
    }

    @Test
    fun `sin modificador usa la convencion de las 12 horas`() {
        // Son las 10:00: «a las 6» se entiende a las 18:00.
        val result = ReminderParser.parse("comprar pan a las 6", baseTime)
        assertNotNull(result)
        val expected = LocalDateTime.of(2026, 11, 14, 18, 0)
            .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertEquals(expected, result.triggerAtMillis)
    }

    @Test
    fun `una hora ya pasada se entiende para otro dia`() {
        val evening = LocalDateTime.of(2026, 11, 14, 20, 0)
        val result = ReminderParser.parse("llamar al doctor a las 6", evening)
        assertNotNull(result)
        val expected = LocalDateTime.of(2026, 11, 15, 18, 0)
            .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertEquals(expected, result!!.triggerAtMillis)
    }

    @Test
    fun `quita el ruido del principio del mensaje`() {
        val result = ReminderParser.parse("recuérdame sacar el pan del horno", baseTime)
        assertNotNull(result)
        assertEquals("sacar el pan del horno", result!!.message)
    }

    @Test
    fun `con varios numeros elige la hora con articulo`() {
        val result = ReminderParser.parse("llamar a 3 personas a las 6", baseTime)
        assertNotNull(result)
        val expected = LocalDateTime.of(2026, 11, 14, 18, 0)
            .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertEquals(expected, result!!.triggerAtMillis)
    }

    @Test
    fun `a la manana no se confunde con el dia manana`() {
        val result = ReminderParser.parse("desayuno a la mañana", baseTime)
        assertNotNull(result)
        // Sin hora reconocible: dentro de 10 minutos, no mañana.
        val expected = baseTime.plusMinutes(10)
            .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertEquals(expected, result!!.triggerAtMillis)
    }

    @Test
    fun `encuentra el recordatorio al cancelar`() {
        assertTrue(ReminderParser.matchesMessage("comprar el pan", "de comprar pan"))
        assertTrue(ReminderParser.matchesMessage("comprar el pan", "comprar pan"))
        assertFalse(ReminderParser.matchesMessage("comprar el pan", "llamar al medico"))
    }
}
