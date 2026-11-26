package com.example.jarvis.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class AiTextTest {

    @Test
    fun `quita las citas de Gemini`() {
        val text = "Es un proceso biológico【1†L14-L19】【2†L34-L36】."
        assertEquals("Es un proceso biológico.", AiText.clean(text))
    }

    @Test
    fun `quita citas entre corchetes`() {
        assertEquals("La fotosíntesis produce oxígeno.", AiText.clean("La fotosíntesis [1] produce oxígeno [2†L40-L45]."))
    }

    @Test
    fun `conserva los saltos y los puntos clave`() {
        val text = "Titular\n\n• Primer punto.\n• Segundo punto."
        assertEquals(text, AiText.clean(text))
    }
}
