package com.example.jarvis.ai

/**
 * Proveedor compuesto: prueba los proveedores en orden y usa el primero que
 * responde. En conversación manda Gemini; en búsquedas manda Groq, porque su
 * herramienta `browser_search` no comparte la cuota de Google Search de
 * Gemini (que se agota y deja a JARVIS sin búsqueda en directo).
 */
class FallbackAiProvider(
    private val providers: List<AiProvider>,
) : AiProvider {

    override suspend fun isConfigured(): Boolean {
        for (provider in providers) {
            if (provider.isConfigured()) return true
        }
        return false
    }

    override suspend fun currentModel(): String {
        for (provider in providers) {
            if (provider.isConfigured()) return provider.currentModel()
        }
        return ""
    }

    override suspend fun keyStatus(): String {
        val parts = mutableListOf<String>()
        for (provider in providers) {
            if (provider.isConfigured()) parts += provider.keyStatus()
        }
        return parts.joinToString(" · ")
    }

    override suspend fun complete(request: AiRequest): AiResult {
        val failures = mutableListOf<String>()
        for (provider in ordered(request)) {
            if (!provider.isConfigured()) continue
            val name = when (provider) {
                is GeminiProvider -> "Gemini"
                is GroqProvider -> "Groq"
                else -> "IA"
            }
            when (val result = provider.complete(request)) {
                is AiResult.Success -> return result
                is AiResult.Failure -> {
                    failures += "[$name] ${result.message}"
                    if (!isRetryable(result.message)) {
                        return AiResult.Failure(failures.joinToString(" | "))
                    }
                }
            }
        }
        return AiResult.Failure(
            failures.joinToString(" | ").ifBlank {
                "No hay ningún proveedor de IA configurado."
            },
        )
    }

    /**
     * Groq va delante solo en las peticiones de búsqueda; el resto mantiene
     * el orden configurado (Gemini primero). Si Groq no tiene clave se omite
     * y se sigue con Gemini, así que no se rompe nada.
     */
    private fun ordered(request: AiRequest): List<AiProvider> =
        if (request.useGoogleSearch) {
            providers.filter { it.supportsWebSearch } +
                providers.filterNot { it.supportsWebSearch }
        } else {
            providers
        }

    /** Errores que pueden resolverse probando el siguiente proveedor. */
    private fun isRetryable(message: String): Boolean =
        listOf(
            "límite", "cuota", "429", "clave", "no está disponible",
            "No hay conexión", "tardado demasiado", "quota", "plan",
            "búsqueda", "browser", "herramienta", "no soportado",
        ).any { message.contains(it, ignoreCase = true) }
}
