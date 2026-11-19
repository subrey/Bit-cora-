package com.example.jarvis.ai

import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GroqJsonTest {

    @Test
    fun `usa el modelo de busqueda y la herramienta cuando se pide`() {
        val body = GroqJson.requestBody(
            AiRequest(
                systemPrompt = "Eres JARVIS.",
                turns = listOf(AiTurn(fromUser = true, text = "busca esto")),
                useGoogleSearch = true,
            ),
            searchModel = "openai/gpt-oss-20b",
            chatModel = "llama-3.1-8b-instant",
        )

        assertEquals(
            "openai/gpt-oss-20b",
            body["model"]!!.jsonPrimitive.content,
        )
        val tools = body["tools"]!!.jsonArray
        assertEquals("browser_search", tools[0].jsonObject["type"]!!.jsonPrimitive.content)
        assertEquals("required", body["tool_choice"]!!.jsonPrimitive.content)
    }

    @Test
    fun `sin busqueda usa el modelo rapido y no lleva herramientas`() {
        val body = GroqJson.requestBody(
            AiRequest(
                systemPrompt = "Eres JARVIS.",
                turns = listOf(AiTurn(fromUser = true, text = "hola")),
            ),
            searchModel = "openai/gpt-oss-20b",
            chatModel = "llama-3.1-8b-instant",
        )

        assertEquals("llama-3.1-8b-instant", body["model"]!!.jsonPrimitive.content)
        assertNull(body["tools"])
        val messages = body["messages"]!!.jsonArray
        assertEquals("system", messages[0].jsonObject["role"]!!.jsonPrimitive.content)
        assertEquals("user", messages[1].jsonObject["role"]!!.jsonPrimitive.content)
    }

    @Test
    fun `extrae el texto y el error`() {
        val ok = """{"choices":[{"message":{"content":"Resumen"}}]}"""
        assertEquals("Resumen", GroqJson.parseText(ok))

        val bad = """{"error":{"message":"rate limit"}}"""
        assertEquals("rate limit", GroqJson.parseError(bad))
        assertTrue(GroqJson.parseText(bad) == null)
    }
}
