package com.example.jarvis.ai

/**
 * Proveedor compuesto: prueba los proveedores en orden y usa el primero que
 * responde. Así un agotamiento de cuota de Gemini no deja a JARVIS sin IA:
 * se pasa a Groq, que tiene cuota y búsqueda en navegador propias.
 */
class FallbackAiProvider(
    private val providers: List<AiProvider>,
) : AiProvider {

    override suspend fun isConfigured(): Boolean =
        providers.any { it.isConfigured() }

    override suspend fun currentModel(): String =
        providers.firstOrNull { it.isConfigured() }?.currentModel().orEmpty()

    override suspend fun keyStatus(): String =
        providers.filter { it.isConfigured() }.joinToString(" · ") { it.keyStatus() }

    override suspend fun complete(request: AiRequest): AiResult {
        val failures = mutableListOf<String>()
        for (provider in providers) {
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

    /** Errores que pueden resolverse probando el siguiente proveedor. */
    private fun isRetryable(message: String): Boolean =
        listOf(
            "límite", "cuota", "429", "clave", "no está disponible",
            "No hay conexión", "tardado demasiado", "quota", "plan",
        ).any { message.contains(it, ignoreCase = true) }
}
