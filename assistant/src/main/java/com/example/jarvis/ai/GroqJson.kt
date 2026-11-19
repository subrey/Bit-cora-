package com.example.jarvis.ai

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Traducción entre [AiRequest] y el formato OpenAI-compatible de Groq.
 * Separado del cliente HTTP para poder probar el JSON sin red.
 */
object GroqJson {

    fun requestBody(request: AiRequest, searchModel: String, chatModel: String) = buildJsonObject {
        put("model", if (request.useGoogleSearch) searchModel else chatModel)
        put(
            "messages",
            buildJsonArray {
                add(
                    buildJsonObject {
                        put("role", "system")
                        put("content", request.systemPrompt)
                    },
                )
                request.turns.forEach { turn ->
                    add(
                        buildJsonObject {
                            put("role", if (turn.fromUser) "user" else "assistant")
                            put("content", turn.text)
                        },
                    )
                }
            },
        )
        put("temperature", request.temperature)
        put("max_completion_tokens", 2048)
        put("top_p", 1)
        put("stream", false)
        if (request.useGoogleSearch) {
            // Herramienta de búsqueda en navegador integrada en Groq.
            put(
                "tools",
                buildJsonArray {
                    add(buildJsonObject { put("type", "browser_search") })
                },
            )
            put("tool_choice", "required")
        }
    }

    /** Texto de la primera respuesta, o null si viene vacía. */
    fun parseText(body: String): String? = runCatching {
        Json.parseToJsonElement(body).jsonObject
            .get("choices")?.jsonArray?.firstOrNull()?.jsonObject
            ?.get("message")?.jsonObject
            ?.get("content")?.jsonPrimitive?.contentOrNull
            ?.trim()
            ?.ifBlank { null }
    }.getOrNull()

    /** Mensaje de error devuelto por Groq, si lo hay. */
    fun parseError(body: String): String? = runCatching {
        Json.parseToJsonElement(body).jsonObject
            .get("error")?.jsonObject
            ?.get("message")?.jsonPrimitive?.contentOrNull
    }.getOrNull()
}
