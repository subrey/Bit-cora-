package com.example.jarvis.ai

/** Un turno de la conversación tal como se envía al modelo. */
data class AiTurn(val fromUser: Boolean, val text: String)

/** Fuente web encontrada por la búsqueda con grounding. */
data class WebSource(val title: String, val url: String)

/** Petición completa: personalidad + turnos de conversación. */
data class AiRequest(
    val systemPrompt: String,
    val turns: List<AiTurn>,
    /** Más bajo para preguntas factuales: menos invención y menos deriva. */
    val temperature: Double = 0.7,
    /** true para que el modelo consulte Google Search antes de responder. */
    val useGoogleSearch: Boolean = false,
    /** Permite probar otro modelo sin cambiar la preferencia guardada. */
    val model: String? = null,
)

/** Resultado de una llamada a la IA, sin excepciones hacia la interfaz. */
sealed interface AiResult {
    data class Success(
        val text: String,
        /** Enlaces que el modelo ha usado cuando se pidió búsqueda web. */
        val sources: List<WebSource> = emptyList(),
    ) : AiResult
    data class Failure(val message: String) : AiResult
}

/**
 * Proveedor de lenguaje. La app solo conoce esta interfaz, así que se puede
 * cambiar Gemini por otro servicio gratuito sin tocar el motor ni la interfaz.
 */
interface AiProvider {

    /**
     * true si el proveedor puede buscar en la web con su propia herramienta,
     * sin consumir la cuota de búsqueda de Gemini. Groq es el caso actual.
     */
    val supportsWebSearch: Boolean get() = false

    /** ¿Hay una clave configurada y utilizable? */
    suspend fun isConfigured(): Boolean

    /** Envía la conversación y devuelve una respuesta o un error legible. */
    suspend fun complete(request: AiRequest): AiResult

    /** Modelo que se está usando ahora, para poder probar el alternativo. */
    suspend fun currentModel(): String = ""

    /** Estado de las claves guardadas, para el diagnóstico. */
    suspend fun keyStatus(): String = ""

    /** Prueba rápida desde Ajustes. */
    suspend fun test(): AiResult = complete(
        AiRequest(
            systemPrompt = "Eres JARVIS, un asistente breve y educado. Responde en español.",
            turns = listOf(AiTurn(fromUser = true, text = "Responde solo: conexión correcta.")),
        ),
    )
}
