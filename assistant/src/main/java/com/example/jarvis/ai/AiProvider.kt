package com.example.jarvis.ai

/** Un turno de la conversación tal como se envía al modelo. */
data class AiTurn(val fromUser: Boolean, val text: String)

/** Petición completa: personalidad + turnos de conversación. */
data class AiRequest(
    val systemPrompt: String,
    val turns: List<AiTurn>,
)

/** Resultado de una llamada a la IA, sin excepciones hacia la interfaz. */
sealed interface AiResult {
    data class Success(val text: String) : AiResult
    data class Failure(val message: String) : AiResult
}

/**
 * Proveedor de lenguaje. La app solo conoce esta interfaz, así que se puede
 * cambiar Gemini por otro servicio gratuito sin tocar el motor ni la interfaz.
 */
interface AiProvider {

    /** ¿Hay una clave configurada y utilizable? */
    suspend fun isConfigured(): Boolean

    /** Envía la conversación y devuelve una respuesta o un error legible. */
    suspend fun complete(request: AiRequest): AiResult

    /** Prueba rápida desde Ajustes. */
    suspend fun test(): AiResult = complete(
        AiRequest(
            systemPrompt = "Eres JARVIS, un asistente breve y educado. Responde en español.",
            turns = listOf(AiTurn(fromUser = true, text = "Responde solo: conexión correcta.")),
        ),
    )
}
