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
 * Proveedor de Google Gemini mediante su API REST.
 *
 * La clave se pide en cada llamada y se envía en la cabecera `x-goog-api-key`,
 * nunca en la URL ni en los registros. El nivel gratuito de Gemini permite
 * conversar sin coste con límites de uso.
 */
class GeminiProvider(
    private val apiKey: () -> String,
    private val model: () -> String,
    private val backupApiKey: () -> String = { "" },
    private val client: OkHttpClient = defaultClient(),
) : AiProvider {

    /** Clave que funcionó por última vez, para probarla primero. */
    private var preferredKeyIndex = 0

    private fun configuredKeys(): List<String> =
        listOf(apiKey(), backupApiKey())
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

    override suspend fun isConfigured(): Boolean =
        withContext(Dispatchers.IO) { configuredKeys().isNotEmpty() }

    override suspend fun currentModel(): String = effectiveModel(model())

    override suspend fun keyStatus(): String = withContext(Dispatchers.IO) {
        "principal: ${if (apiKey().isNotBlank()) "sí" else "no"}, " +
            "respaldo: ${if (backupApiKey().isNotBlank()) "sí" else "no"}"
    }

    override suspend fun complete(request: AiRequest): AiResult = withContext(Dispatchers.IO) {
        val keys = configuredKeys()
        if (keys.isEmpty()) {
            return@withContext AiResult.Failure(
                "No hay ninguna clave de IA configurada. Añádala en Ajustes → Inteligencia artificial.",
            )
        }

        val ordered = if (preferredKeyIndex == 0) keys else keys.reversed()
        val modelName = effectiveModel(request.model ?: model())
        val body = GeminiJson.requestBody(request).toString()
        val failures = mutableListOf<String>()

        for (entry in ordered.withIndex()) {
            val key = entry.value
            val httpRequest = Request.Builder()
                .url("$BASE_URL/models/$modelName:generateContent")
                .header("x-goog-api-key", key)
                .header("Content-Type", "application/json")
                .post(body.toRequestBody(JSON))
                .build()

            val result = try {
                val response = client.newCall(httpRequest).await()
                response.use { parseResponse(it, modelName, request.useGoogleSearch) }
            } catch (e: SocketTimeoutException) {
                AiResult.Failure(
                    "La IA ha tardado demasiado en responder (${e.javaClass.simpleName}: " +
                        "${e.message.orEmpty()}). Inténtelo de nuevo.",
                )
            } catch (e: IOException) {
                AiResult.Failure(
                    "No hay conexión con el servicio de IA (${e.javaClass.simpleName}: " +
                        "${e.message.orEmpty()}). Compruebe la red e inténtelo de nuevo.",
                )
            }

            if (result is AiResult.Success) {
                preferredKeyIndex = if (key == keys.first()) 0 else 1
                return@withContext result
            }
            val failure = result as AiResult.Failure
            val label = if (key == keys.first()) "principal" else "respaldo"
            failures += "[$label] ${failure.message}"
            if (!isRetryableWithOtherKey(failure.message) || entry.index == ordered.lastIndex) {
                return@withContext AiResult.Failure(failures.joinToString(" | "))
            }
        }
        return@withContext AiResult.Failure(
            failures.joinToString(" | ").ifBlank { "La IA no ha devuelto ninguna respuesta." },
        )
    }

    /** Errores que pueden resolverse usando la otra clave. */
    private fun isRetryableWithOtherKey(message: String): Boolean =
        listOf(
            "límite", "429", "clave", "no está disponible",
            "No hay conexión", "tardado demasiado",
        ).any { message.contains(it, ignoreCase = true) }

    private fun parseResponse(
        response: Response,
        modelName: String,
        search: Boolean,
    ): AiResult {
        val body = runCatching { response.body?.string().orEmpty() }.getOrDefault("")
        if (!response.isSuccessful) {
            val detail = GeminiJson.parseError(body)
            return AiResult.Failure(
                when (response.code) {
                    400, 401, 403 ->
                        "La clave de IA no es válida o no tiene permisos (${response.code}). " +
                            (detail ?: "Revísela en Ajustes.")
                    404 ->
                        "El modelo «$modelName» no existe o ya no está disponible. " +
                            "Use «$DEFAULT_MODEL» en Ajustes."
                    429 ->
                        (if (search) {
                            "He alcanzado el límite gratuito de búsqueda en internet. "
                        } else {
                            "He alcanzado el límite gratuito de la IA. "
                        }) + (detail ?: "Espere un minuto y vuelva a intentarlo.")
                    in 500..599 ->
                        "El servicio de IA no está disponible (${response.code}). " +
                            (detail ?: "Inténtelo de nuevo en unos segundos.")
                    else -> "La IA ha devuelto un error (${response.code}). " +
                        (detail ?: "Inténtelo de nuevo.")
                },
            )
        }

        GeminiJson.blockReason(body)?.let { reason ->
            return AiResult.Failure("La IA ha bloqueado la respuesta por seguridad ($reason).")
        }

        val text = GeminiJson.parseText(body)
            ?: return AiResult.Failure("La IA no ha devuelto ninguna respuesta. Inténtelo de nuevo.")
        return AiResult.Success(text, GeminiJson.parseSources(body))
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
        /**
         * Modelo gratuito por defecto. Flash-Lite tiene más cuota diaria en el
         * nivel gratuito que Flash; se puede cambiar en Ajustes.
         */
        const val DEFAULT_MODEL = "gemini-3.5-flash-lite"

        /** Modelo de más calidad que se prueba si el actual falla. */
        const val QUALITY_MODEL = "gemini-3.8-flash"

        /** Modelos que Google ya no sirve a claves nuevas: se migran solos. */
        private val RETIRED_MODELS = setOf(
            "gemini-2.0-flash",
            "gemini-2.0-flash-lite",
            "gemini-2.5-flash",
            "gemini-2.5-flash-lite",
            "gemini-2.5-pro",
            "gemini-1.5-flash",
            "gemini-1.5-pro",
            "gemini-3.1-flash-lite-preview",
            "gemini-3-pro-preview",
        )

        /**
         * Devuelve el modelo que se usará de verdad: respeta el elegido por el
         * usuario salvo que esté vacío o sea uno de los retirados.
         */
        fun effectiveModel(value: String): String {
            val clean = value.trim()
            return if (clean.isBlank() || clean in RETIRED_MODELS) DEFAULT_MODEL else clean
        }

        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta"
        private val JSON = "application/json; charset=utf-8".toMediaType()

        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
