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
    private val client: OkHttpClient = defaultClient(),
) : AiProvider {

    override suspend fun isConfigured(): Boolean =
        withContext(Dispatchers.IO) { apiKey().isNotBlank() }

    override suspend fun complete(request: AiRequest): AiResult = withContext(Dispatchers.IO) {
        val key = apiKey().trim()
        if (key.isBlank()) {
            return@withContext AiResult.Failure(
                "No hay ninguna clave de IA configurada. Añádala en Ajustes → Inteligencia artificial.",
            )
        }

        val modelName = effectiveModel(model())
        val body = GeminiJson.requestBody(request).toString()
        val httpRequest = Request.Builder()
            .url("$BASE_URL/models/$modelName:generateContent")
            .header("x-goog-api-key", key)
            .header("Content-Type", "application/json")
            .post(body.toRequestBody(JSON))
            .build()

        val response = try {
            client.newCall(httpRequest).await()
        } catch (e: SocketTimeoutException) {
            return@withContext AiResult.Failure(
                "La IA ha tardado demasiado en responder. Inténtelo de nuevo.",
            )
        } catch (e: IOException) {
            return@withContext AiResult.Failure(
                "No hay conexión con el servicio de IA. Compruebe la red e inténtelo de nuevo.",
            )
        }

        response.use { parseResponse(it, modelName) }
    }

    private fun parseResponse(response: Response, modelName: String): AiResult {
        val body = runCatching { response.body?.string().orEmpty() }.getOrDefault("")
        if (!response.isSuccessful) {
            val detail = GeminiJson.parseError(body)
            return AiResult.Failure(
                when (response.code) {
                    400, 401, 403 ->
                        "La clave de IA no es válida o no tiene permisos. Revísela en Ajustes."
                    404 ->
                        "El modelo «$modelName» no existe o ya no está disponible. " +
                            "Use «$DEFAULT_MODEL» en Ajustes."
                    429 ->
                        "He alcanzado el límite gratuito de la IA. Espere un minuto y vuelva a intentarlo."
                    in 500..599 ->
                        "El servicio de IA no está disponible ahora mismo. Inténtelo más tarde."
                    else -> detail ?: "La IA ha devuelto un error (${response.code})."
                },
            )
        }

        GeminiJson.blockReason(body)?.let { reason ->
            return AiResult.Failure("La IA ha bloqueado la respuesta por seguridad ($reason).")
        }

        val text = GeminiJson.parseText(body)
            ?: return AiResult.Failure("La IA no ha devuelto ninguna respuesta. Inténtelo de nuevo.")
        return AiResult.Success(text)
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
