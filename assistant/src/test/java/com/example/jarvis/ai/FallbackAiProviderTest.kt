package com.example.jarvis.ai

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/** El orden depende de si la orden pide buscar en internet o no. */
class FallbackAiProviderTest {

    private class FakeProvider(
        override val supportsWebSearch: Boolean = false,
        private val configured: Boolean = true,
        private val answer: AiResult = AiResult.Failure("sin respuesta"),
    ) : AiProvider {
        var called = false
            private set

        override suspend fun isConfigured(): Boolean = configured

        override suspend fun complete(request: AiRequest): AiResult {
            called = true
            return answer
        }
    }

    private fun request(search: Boolean) = AiRequest(
        systemPrompt = "Eres JARVIS.",
        turns = listOf(AiTurn(fromUser = true, text = "prueba")),
        useGoogleSearch = search,
    )

    @Test
    fun `en conversacion responde primero el proveedor configurado`() = runBlocking {
        val gemini = FakeProvider(answer = AiResult.Success("gemini"))
        val groq = FakeProvider(supportsWebSearch = true, answer = AiResult.Success("groq"))
        val fallback = FallbackAiProvider(listOf(gemini, groq))

        assertEquals("gemini", (fallback.complete(request(false)) as AiResult.Success).text)
        assertEquals(true, gemini.called)
        assertEquals(false, groq.called)
    }

    @Test
    fun `en busquedas responde primero el proveedor con busqueda propia`() = runBlocking {
        val gemini = FakeProvider(answer = AiResult.Success("gemini"))
        val groq = FakeProvider(supportsWebSearch = true, answer = AiResult.Success("groq"))
        val fallback = FallbackAiProvider(listOf(gemini, groq))

        assertEquals("groq", (fallback.complete(request(true)) as AiResult.Success).text)
        assertEquals(true, groq.called)
        assertEquals(false, gemini.called)
    }

    @Test
    fun `si el proveedor de busquedas no tiene clave se usa el siguiente`() = runBlocking {
        val gemini = FakeProvider(answer = AiResult.Success("gemini"))
        val groq = FakeProvider(
            supportsWebSearch = true,
            configured = false,
            answer = AiResult.Success("groq"),
        )
        val fallback = FallbackAiProvider(listOf(gemini, groq))

        assertEquals("gemini", (fallback.complete(request(true)) as AiResult.Success).text)
        assertEquals(false, groq.called)
    }

    @Test
    fun `un fallo de busquedas no impide intentar con gemini`() = runBlocking {
        val gemini = FakeProvider(answer = AiResult.Success("gemini"))
        val groq = FakeProvider(
            supportsWebSearch = true,
            answer = AiResult.Failure("browser_search no soportado"),
        )
        val fallback = FallbackAiProvider(listOf(gemini, groq))

        assertEquals("gemini", (fallback.complete(request(true)) as AiResult.Success).text)
        assertEquals(true, groq.called)
    }
}
