package com.example.jarvis.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Proveedor de Groq compatible con la API de OpenAI. Tiene un nivel gratuito
 * propio y, con los modelos GPT-OSS, incluye búsqueda en navegador integrada:
 * sirve como respaldo cuando Gemini se queda sin cuota.
 */
class GroqProvider(
    private val apiKey: () -> String,
    private val client: OkHttpClient = defaultClient(),
) : AiProvider {

    // browser_search corre en la infraestructura de Groq: no gasta la cuota
    // de Google Search de Gemini, así que va primero en las búsquedas.
    override val supportsWebSearch: Boolean = true

    override suspend fun isConfigured(): Boolean =
        withContext(Dispatchers.IO) { apiKey().isNotBlank() }

    override suspend fun currentModel(): String = CHAT_MODEL

    override suspend fun keyStatus(): String = withContext(Dispatchers.IO) {
        "groq: ${if (apiKey().isNotBlank()) "sí" else "no"}"
    }

    override suspend fun complete(request: AiRequest): AiResult = withContext(Dispatchers.IO) {
        val key = apiKey().trim()
        if (key.isBlank()) {
            return@withContext AiResult.Failure(
                "No hay ninguna clave de Groq configurada. Añádala en Ajustes.",
            )
        }

        val body = GroqJson.requestBody(request, SEARCH_MODEL, CHAT_MODEL).toString()
        val httpRequest = Request.Builder()
            .url("$BASE_URL/chat/completions")
            .header("Authorization", "Bearer $key")
            .header("Content-Type", "application/json")
            .post(body.toRequestBody(JSON))
            .build()

        val response = try {
            client.newCall(httpRequest).await()
        } catch (e: SocketTimeoutException) {
            return@withContext AiResult.Failure(
                "Groq ha tardado demasiado en responder (${e.javaClass.simpleName}).",
            )
        } catch (e: IOException) {
            return@withContext AiResult.Failure(
                "No hay conexión con Groq (${e.javaClass.simpleName}: ${e.message.orEmpty()}).",
            )
        }

        response.use { parseResponse(it) }
    }

    private fun parseResponse(response: Response): AiResult {
        val body = runCatching { response.body?.string().orEmpty() }.getOrDefault("")
        if (!response.isSuccessful) {
            val detail = GroqJson.parseError(body)
            return AiResult.Failure(
                "Groq ha devuelto un error (${response.code}). " +
                    (detail ?: "Compruebe la clave y el plan gratuito."),
            )
        }
        val text = GroqJson.parseText(body)
            ?: return AiResult.Failure("Groq no ha devuelto ninguna respuesta.")
        return AiResult.Success(AiText.clean(text))
    }

    /** OkHttp bloquea el hilo; se envuelve para poder cancelar con la corrutina. */
    private suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
        enqueue(
            object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (cont.isActive) cont.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (cont.isActive) cont.resume(response) else response.close()
                }
            },
        )
        cont.invokeOnCancellation { runCatching { cancel() } }
    }

    companion object {
        /** Modelo rápido para conversación y resúmenes sin búsqueda. */
        const val CHAT_MODEL = "llama-3.1-8b-instant"

        /** Modelo con herramienta de búsqueda en navegador integrada. */
        const val SEARCH_MODEL = "openai/gpt-oss-20b"

        private const val BASE_URL = "https://api.groq.com/openai/v1"
        private val JSON = "application/json; charset=utf-8".toMediaType()

        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .callTimeout(35, TimeUnit.SECONDS)
            .build()
    }
}
