package com.example.jarvis.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IntentParserTest {

    @Test
    fun `detecta poner musica`() {
        val intent = IntentParser.parse("Pon música por favor")
        assertEquals(IntentKind.MUSIC_PLAY, intent.kind)
    }

    @Test
    fun `detecta pausa`() {
        assertEquals(IntentKind.MUSIC_PAUSE, IntentParser.parse("pausa la música").kind)
    }

    @Test
    fun `detecta siguiente cancion`() {
        assertEquals(IntentKind.MUSIC_NEXT, IntentParser.parse("siguiente canción").kind)
    }

    @Test
    fun `detecta cancion anterior`() {
        assertEquals(IntentKind.MUSIC_PREVIOUS, IntentParser.parse("la canción anterior").kind)
    }

    @Test
    fun `detecta volumen`() {
        assertEquals(IntentKind.VOLUME_UP, IntentParser.parse("sube el volumen").kind)
        assertEquals(IntentKind.VOLUME_DOWN, IntentParser.parse("baja volumen").kind)
    }

    @Test
    fun `detecta la hora`() {
        assertEquals(IntentKind.TIME, IntentParser.parse("¿qué hora es?").kind)
    }

    @Test
    fun `detecta la fecha`() {
        assertEquals(IntentKind.DATE, IntentParser.parse("qué día es hoy").kind)
    }

    @Test
    fun `extrae la expresion de calculo`() {
        val intent = IntentParser.parse("calcula 12 * (3 + 4)")
        assertEquals(IntentKind.CALCULATE, intent.kind)
        assertEquals("12 * (3 + 4)", intent.payload)
    }

    @Test
    fun `extrae la conversion`() {
        val intent = IntentParser.parse("convierte 10 km a millas")
        assertEquals(IntentKind.CONVERT, intent.kind)
        assertEquals("10 km a millas", intent.payload)
    }

    @Test
    fun `detecta temporizador`() {
        assertEquals(IntentKind.TIMER, IntentParser.parse("pon un temporizador de 10 minutos").kind)
    }

    @Test
    fun `detecta tarea`() {
        val intent = IntentParser.parse("añade tarea comprar leche")
        assertEquals(IntentKind.TODO, intent.kind)
        assertEquals("comprar leche", intent.payload)
    }

    @Test
    fun `detecta memoria`() {
        val intent = IntentParser.parse("memoriza el número del hotel")
        assertEquals(IntentKind.MEMORY, intent.kind)
        assertTrue(intent.payload.contains("hotel"))
    }

    @Test
    fun `detecta ayuda`() {
        assertEquals(IntentKind.HELP, IntentParser.parse("¿qué sabes hacer?").kind)
    }

    @Test
    fun `normaliza acentos y mayusculas`() {
        assertEquals("pon musica", IntentParser.normalize("  ¡Pon MÚSICA!  "))
    }
}
