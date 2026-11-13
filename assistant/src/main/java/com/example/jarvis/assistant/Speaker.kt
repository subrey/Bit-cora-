package com.example.jarvis.assistant

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale

/**
 * Voz de JARVIS con [TextToSpeech].
 *
 * [speak] suspende la corrutina hasta que la locución termina (o se
 * interrumpe), de modo que la conversación continua puede abrir el micrófono
 * justo al acabar de hablar, sin pausas estimadas.
 */
class Speaker(context: Context) {

    private val ready = CompletableDeferred<Boolean>()
    private var tts: TextToSpeech? = null

    @Volatile
    private var pending: CompletableDeferred<Unit>? = null

    @Volatile
    private var currentUtterance: String? = null
    private var counter = 0

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            val ok = status == TextToSpeech.SUCCESS
            if (ok) {
                runCatching {
                    val engine = tts ?: return@runCatching
                    val locale = Locale.forLanguageTag("es-ES")
                    val language = engine.setLanguage(locale)
                    if (language == TextToSpeech.LANG_MISSING_DATA ||
                        language == TextToSpeech.LANG_NOT_SUPPORTED
                    ) {
                        engine.setLanguage(Locale.getDefault())
                    }
                    engine.setSpeechRate(1.0f)
                    engine.setPitch(0.97f)
                    engine.setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANT)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build(),
                    )
                    engine.setOnUtteranceProgressListener(listener)
                }
            }
            ready.complete(ok)
        }
    }

    /** Habla y espera al final de la locución. */
    suspend fun speak(text: String) {
        if (text.isBlank()) return
        val available = withTimeoutOrNull(4_000) { ready.await() } ?: false
        if (!available) return

        val done = CompletableDeferred<Unit>()
        val id = "jarvis-${++counter}"
        pending = done
        currentUtterance = id

        val result = withContext(Dispatchers.Main) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
        }
        if (result != TextToSpeech.SUCCESS) done.complete(Unit)

        // Límite de seguridad por si el motor de voz nunca avisa del final.
        withTimeoutOrNull(90_000) { done.await() }

        if (pending === done) {
            pending = null
            currentUtterance = null
        }
    }

    /** Detiene la locución actual; [speak] termina en el acto. */
    fun stop() {
        runCatching { tts?.stop() }
        currentUtterance = null
        pending?.complete(Unit)
        pending = null
    }

    fun shutdown() {
        stop()
        runCatching { tts?.shutdown() }
        tts = null
    }

    private val listener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) = Unit

        override fun onDone(utteranceId: String?) = finish(utteranceId)

        @Deprecated("Método obsoleto del sistema, se mantiene por compatibilidad")
        override fun onError(utteranceId: String?) = finish(utteranceId)

        override fun onError(utteranceId: String?, errorCode: Int) = finish(utteranceId)

        override fun onStop(utteranceId: String?, interrupted: Boolean) = finish(utteranceId)
    }

    private fun finish(utteranceId: String?) {
        if (utteranceId != null && utteranceId == currentUtterance) {
            pending?.complete(Unit)
        }
    }
}
