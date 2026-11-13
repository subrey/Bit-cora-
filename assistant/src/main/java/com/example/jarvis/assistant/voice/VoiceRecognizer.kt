package com.example.jarvis.assistant.voice

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

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

    val isListening: Boolean
        get() = listening

    fun start(preferOnDevice: Boolean) {
        if (listening) return
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError(ERROR_UNAVAILABLE)
            return
        }
        if (recognizer == null || preferOnDevice != onDevice) {
            create(preferOnDevice)
        }
        val engine = recognizer ?: run {
            onError(ERROR_UNAVAILABLE)
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            // Respuesta rápida: al callar, la orden se cierra en ~2 s.
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2_200L)
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                1_200L,
            )
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 800L)
        }

        listening = true
        runCatching { engine.startListening(intent) }
            .onFailure {
                listening = false
                onError(SpeechRecognizer.ERROR_CLIENT)
            }
    }

    fun stop() {
        listening = false
        runCatching { recognizer?.cancel() }
    }

    fun destroy() {
        stop()
        runCatching { recognizer?.destroy() }
        recognizer = null
    }

    private fun create(preferOnDevice: Boolean) {
        runCatching { recognizer?.destroy() }
        onDevice = preferOnDevice
        // Si el modelo local no está disponible se cae al motor normal en
        // lugar de dejar al usuario sin micrófono.
        val engine = if (preferOnDevice && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching { SpeechRecognizer.createOnDeviceSpeechRecognizer(context) }
                .getOrElse { runCatching { SpeechRecognizer.createSpeechRecognizer(context) }.getOrNull() }
        } else {
            runCatching { SpeechRecognizer.createSpeechRecognizer(context) }.getOrNull()
        }
        engine?.setRecognitionListener(listener)
        recognizer = engine
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            onReady()
        }

        override fun onBeginningOfSpeech() {
            onSpeechStart()
        }

        override fun onRmsChanged(rmsdB: Float) = Unit

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() = Unit

        override fun onError(error: Int) {
            listening = false
            onError(error)
        }

        override fun onResults(results: Bundle?) {
            listening = false
            onResult(firstResult(results))
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val text = firstResult(partialResults)
            if (text.isNotBlank()) onPartial(text)
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private fun firstResult(bundle: Bundle?): String = bundle
        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        ?.firstOrNull()
        .orEmpty()

    companion object {
        /** No hay ningún motor de reconocimiento instalado en el teléfono. */
        const val ERROR_UNAVAILABLE = -100
    }
}
