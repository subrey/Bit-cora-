package com.example.jarvis.assistant.voice

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.example.jarvis.assistant.CrashReporter

/**
 * Micrófono del asistente dentro de la app.
 *
 * Usa [SpeechRecognizer] directamente en vez del diálogo del sistema: así no
 * hay ventana emergente, se pueden mostrar resultados parciales y JARVIS puede
 * encadenar una conversación sin que el usuario toque nada entre turnos.
 * Los pitidos de inicio dependen del motor de voz; el motor en el dispositivo
 * no los reproduce.
 *
 * Todos los métodos deben llamarse desde el hilo principal.
 */
class VoiceRecognizer(
    private val context: Context,
    private val onReady: () -> Unit,
    private val onSpeechStart: () -> Unit,
    private val onPartial: (String) -> Unit,
    private val onResult: (String) -> Unit,
    private val onError: (Int) -> Unit,
) {

    private var recognizer: SpeechRecognizer? = null
    private var onDevice = false
    private var listening = false

    /**
     * Un reconocedor que ha fallado puede quedarse «colgado» y no volver a
     * escuchar. En ese caso se destruye y se crea uno nuevo al reintentar.
     */
    private var needsRecreate = false

    val isListening: Boolean
        get() = listening

    fun start(preferOnDevice: Boolean) {
        if (listening) return
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            safe("Comprobar motor de voz") { onError(ERROR_UNAVAILABLE) }
            return
        }
        if (recognizer == null || preferOnDevice != onDevice || needsRecreate) {
            create(preferOnDevice)
        }
        val engine = recognizer ?: run {
            safe("Crear motor de voz") { onError(ERROR_UNAVAILABLE) }
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            // Respuesta rápida: al callar, la orden se cierra en ~2 s.
            // Margen suficiente para empezar a hablar sin que la sesión se
            // cierre sola antes de que el usuario diga la orden.
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 4_000L)
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                2_000L,
            )
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1_000L)
        }

        listening = true
        // Deja marcado que el micrófono se estaba abriendo: si la app muere
        // justo ahora, el próximo arranque podrá explicarlo.
        CrashReporter.markMicOpen(context)
        runCatching { engine.startListening(intent) }
            .onFailure {
                listening = false
                needsRecreate = true
                CrashReporter.markMicReady(context)
                safe("Iniciar escucha") { onError(SpeechRecognizer.ERROR_CLIENT) }
            }
    }

    fun stop() {
        listening = false
        CrashReporter.markMicReady(context)
        runCatching { recognizer?.cancel() }
    }

    /**
     * Suelta el reconocedor por completo. Se usa al devolver el micrófono al
     * servicio de escucha permanente: así no queda una instancia a medias
     * reteniendo el micrófono.
     */
    fun release() {
        stop()
        runCatching { recognizer?.destroy() }
        recognizer = null
        needsRecreate = true
    }

    fun destroy() {
        stop()
        runCatching { recognizer?.destroy() }
        recognizer = null
    }

    private fun create(preferOnDevice: Boolean) {
        runCatching { recognizer?.destroy() }
        onDevice = preferOnDevice
        // El motor en el dispositivo solo se intenta si el sistema dice que
        // está disponible: en algunos teléfonos crearlo revienta la app.
        val onDeviceAvailable = preferOnDevice &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            runCatching { SpeechRecognizer.isOnDeviceRecognitionAvailable(context) }
                .getOrDefault(false)
        val engine = if (onDeviceAvailable) {
            runCatching { SpeechRecognizer.createOnDeviceSpeechRecognizer(context) }
                .getOrElse { runCatching { SpeechRecognizer.createSpeechRecognizer(context) }.getOrNull() }
        } else {
            runCatching { SpeechRecognizer.createSpeechRecognizer(context) }.getOrNull()
        }
        engine?.setRecognitionListener(listener)
        recognizer = engine
        needsRecreate = engine == null
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            needsRecreate = false
            CrashReporter.markMicReady(context)
            safe("Callback onReady") { onReady() }
        }

        override fun onBeginningOfSpeech() {
            safe("Callback onSpeechStart") { onSpeechStart() }
        }

        override fun onRmsChanged(rmsdB: Float) = Unit

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() = Unit

        override fun onError(error: Int) {
            listening = false
            needsRecreate = true
            CrashReporter.markMicReady(context)
            safe("Callback onError") { onError(error) }
        }

        override fun onResults(results: Bundle?) {
            listening = false
            needsRecreate = false
            CrashReporter.markMicReady(context)
            val text = firstResult(results)
            safe("Callback onResults") { onResult(text) }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val text = firstResult(partialResults)
            if (text.isNotBlank()) {
                safe("Callback onPartial") { onPartial(text) }
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private fun firstResult(bundle: Bundle?): String = bundle
        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        ?.firstOrNull()
        .orEmpty()

    /**
     * Un fallo dentro de un callback no debe cerrar la app: se anota y se
     * sigue. El diagnóstico aparecerá en el chat en el próximo arranque.
     */
    private inline fun safe(tag: String, block: () -> Unit) {
        runCatching(block).onFailure { CrashReporter.note(context, tag, it) }
    }

    companion object {
        /** No hay ningún motor de reconocimiento instalado en el teléfono. */
        const val ERROR_UNAVAILABLE = -100
    }
}
