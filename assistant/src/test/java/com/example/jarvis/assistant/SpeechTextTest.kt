package com.example.jarvis.assistant

import org.junit.Assert.assertEquals
import org.junit.Test

/** La pronunciación se corrige solo en voz; el texto del chat se conserva. */
class SpeechTextTest {

    @Test
    fun `la hora se dice con palabras`() {
        assertEquals("Son las siete y media.", SpeechText.forSpeech("Son las 07:30."))
        assertEquals("Son las ocho en punto.", SpeechText.forSpeech("Son las 08:00."))
        assertEquals("Son las doce y cuarto.", SpeechText.forSpeech("Son las 12:15."))
        assertEquals("Son las dos y cinco.", SpeechText.forSpeech("Son las 2:05."))
    }

    @Test
    fun `los resultados se dicen con palabras`() {
        assertEquals(
            "El resultado es mil doscientos treinta y cuatro.",
            SpeechText.forSpeech("El resultado es 1234."),
        )
        assertEquals("El resultado es veinte.", SpeechText.forSpeech("El resultado es 20."))
        assertEquals("El resultado es veintiuno.", SpeechText.forSpeech("El resultado es 21."))
        assertEquals(
            "El resultado es un millon.",
            SpeechText.forSpeech("El resultado es 1000000."),
        )
    }

    @Test
    fun `los decimales se leen con coma`() {
        assertEquals(
            "Dispone de doce coma cinco gigabytes en total.",
            SpeechText.forSpeech("Dispone de 12.5 GB en total."),
        )
    }

    @Test
    fun `los negativos se leen con menos`() {
        assertEquals("Hace menos tres grados.", SpeechText.forSpeech("Hace -3°."))
        assertEquals("Hace menos tres coma cinco grados.", SpeechText.forSpeech("Hace -3.5 grados."))
    }

    @Test
    fun `el texto sin cifras no cambia`() {
        val text = "La batería está al ochenta por ciento y se está cargando."
        assertEquals(text, SpeechText.forSpeech(text))
    }
}
