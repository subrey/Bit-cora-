package com.example.jarvis.ai

import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GeminiJsonTest {

    @Test
    fun `incluye la personalidad y los turnos con su rol`() {
        val body = GeminiJson.requestBody(
            AiRequest(
                systemPrompt = "Eres JARVIS.",
                turns = listOf(
                    AiTurn(fromUser = true, text = "hola"),
                    AiTurn(fromUser = false, text = "buenas"),
                ),
            ),
        )

        val system = body["systemInstruction"]!!
            .jsonObject["parts"]!!
            .jsonArray[0]
            .jsonObject["text"]!!
            .jsonPrimitive.content
        assertEquals("Eres JARVIS.", system)

        val contents = body["contents"]!!.jsonArray
        assertEquals("user", contents[0].jsonObject["role"]!!.jsonPrimitive.content)
        assertEquals("model", contents[1].jsonObject["role"]!!.jsonPrimitive.content)
        assertEquals(
            "hola",
            contents[0].jsonObject["parts"]!!.jsonArray[0]
                .jsonObject["text"]!!.jsonPrimitive.content,
        )
    }

    @Test
    fun `extrae el texto de la respuesta`() {
        val json = """
            {"candidates":[{"content":{"parts":[{"text":"Hola, "},{"text":"señor."}]}}]}
        """.trimIndent()
        assertEquals("Hola, señor.", GeminiJson.parseText(json))
    }

    @Test
    fun `devuelve null si no hay candidatos`() {
        assertNull(GeminiJson.parseText("""{"promptFeedback":{"blockReason":"SAFETY"}}"""))
    }

    @Test
    fun `extrae el mensaje de error de la API`() {
        val json = """{"error":{"code":403,"message":"API key not valid"}}"""
        assertEquals("API key not valid", GeminiJson.parseError(json))
    }

    @Test
    fun `extrae el motivo de bloqueo`() {
        val json = """{"promptFeedback":{"blockReason":"SAFETY"}}"""
        assertEquals("SAFETY", GeminiJson.blockReason(json))
    }
}
