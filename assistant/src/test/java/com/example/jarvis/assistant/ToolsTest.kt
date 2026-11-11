package com.example.jarvis.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolsTest {

    @Test
    fun `evalua operaciones con prioridad`() {
        assertEquals(14.0, Tools.evaluate("2 + 3 * 4")!!, 0.0001)
    }

    @Test
    fun `evalua parentesis`() {
        assertEquals(84.0, Tools.evaluate("12 * (3 + 4)")!!, 0.0001)
    }

    @Test
    fun `evalua division`() {
        assertEquals(2.5, Tools.evaluate("10 / 4")!!, 0.0001)
    }

    @Test
    fun `rechaza expresiones invalidas`() {
        assertNull(Tools.evaluate("abc"))
        assertNull(Tools.evaluate("10 / 0"))
    }

    @Test
    fun `convierte unidades de longitud`() {
        val result = Tools.convert(10.0, "km", "milla")!!
        assertEquals(6.2137, result, 0.001)
    }

    @Test
    fun `convierte unidades de masa`() {
        assertEquals(1000.0, Tools.convert(1.0, "kg", "g")!!, 0.0001)
    }

    @Test
    fun `convierte temperatura`() {
        assertEquals(32.0, Tools.convert(0.0, "c", "f")!!, 0.01)
    }

    @Test
    fun `no mezcla familias distintas`() {
        assertNull(Tools.convert(10.0, "km", "kg"))
    }

    @Test
    fun `interpreta frases de conversion`() {
        val parsed = Tools.parseConversion("10 km a millas")!!
        assertEquals(10.0, parsed.first, 0.0001)
        assertEquals("km", parsed.second)
        assertEquals("millas", parsed.third)
    }

    @Test
    fun `formatea fechas en espanol`() {
        val text = Tools.formatDate(java.time.LocalDateTime.of(2026, 3, 15, 21, 5))
        assertTrue(text.contains("2026"))
    }
}
