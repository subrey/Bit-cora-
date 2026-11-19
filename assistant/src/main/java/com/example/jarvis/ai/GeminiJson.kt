package com.example.jarvis.ai

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Traducción entre [AiRequest] y el JSON de la API de Gemini. Separado del
 * cliente HTTP para poder probar la forma exacta de la petición y de la
 * respuesta sin hacer red.
 */
object GeminiJson {

    fun requestBody(request: AiRequest): JsonObject = buildJsonObject {
        put(
            "systemInstruction",
            buildJsonObject {
                put(
                    "parts",
                    buildJsonArray {
                        add(buildJsonObject { put("text", request.systemPrompt) })
                    },
                )
            },
        )
        put(
            "contents",
            buildJsonArray {
                request.turns.forEach { turn ->
                    add(
                        buildJsonObject {
                            put("role", if (turn.fromUser) "user" else "model")
                            put(
                                "parts",
                                buildJsonArray {
                                    add(buildJsonObject { put("text", turn.text) })
                                },
                            )
                        },
                    )
                }
            },
        )
        put(
            "generationConfig",
            buildJsonObject {
                put("temperature", request.temperature)
                // margen para los tokens de razonamiento de Gemini 3
                put("maxOutputTokens", 2048)
            },
        )
        if (request.useGoogleSearch) {
            // Grounding con Google Search: el modelo busca en la web y
            // devuelve además las fuentes en groundingMetadata.
            put(
                "tools",
                buildJsonArray {
                    add(buildJsonObject { put("google_search", buildJsonObject {}) })
                },
            )
        }
    }

    /** Texto de la primera respuesta, o null si viene vacía o bloqueada. */
    fun parseText(body: String): String? = runCatching {
        val json = Json.parseToJsonElement(body).jsonObject
        val parts = json["candidates"]
            ?.jsonArray
            ?.firstOrNull()
            ?.jsonObject
            ?.get("content")
            ?.jsonObject
            ?.get("parts")
            ?.jsonArray
        parts
            ?.mapNotNull { it.jsonObject["text"]?.jsonPrimitive?.contentOrNull }
            ?.joinToString("")
            ?.trim()
            ?.ifBlank { null }
    }.getOrNull()

    /** Mensaje de error que devuelve la API, si lo hay. */
    fun parseError(body: String): String? = runCatching {
        Json.parseToJsonElement(body)
            .jsonObject["error"]
            ?.jsonObject
            ?.get("message")
            ?.jsonPrimitive
            ?.contentOrNull
    }.getOrNull()

    /** Motivo por el que el modelo ha bloqueado la respuesta, si lo hay. */
    fun blockReason(body: String): String? = runCatching {
        Json.parseToJsonElement(body)
            .jsonObject["promptFeedback"]
            ?.jsonObject
            ?.get("blockReason")
            ?.jsonPrimitive
            ?.contentOrNull
    }.getOrNull()

    /** Enlaces web usados por el grounding, deduplicados y limitados. */
    fun parseSources(body: String): List<WebSource> = runCatching {
        val chunks = Json.parseToJsonElement(body).jsonObject
            .get("candidates")?.jsonArray?.firstOrNull()?.jsonObject
            ?.get("groundingMetadata")?.jsonObject
            ?.get("groundingChunks")?.jsonArray ?: return@runCatching emptyList()
        chunks.mapNotNull { chunk ->
            val web = chunk.jsonObject["web"]?.jsonObject ?: return@mapNotNull null
            val url = web["uri"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val title = web["title"]?.jsonPrimitive?.contentOrNull.orEmpty()
            WebSource(title.ifBlank { url }, url)
        }.distinctBy { it.url }.take(5)
    }.getOrDefault(emptyList())
}
