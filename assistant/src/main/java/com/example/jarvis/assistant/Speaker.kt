package com.example.jarvis.assistant

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Lee en voz alta las respuestas del asistente. */
class Speaker(context: Context) {

    private val ready = kotlinx.coroutines.CompletableDeferred<Boolean>()
    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                runCatching { tts?.setLanguage(Locale.forLanguageTag("es-ES")) }
            }
            ready.complete(status == TextToSpeech.SUCCESS)
        }
    }

    suspend fun speak(text: String) {
        val ok = runCatching { ready.await() }.getOrDefault(false)
        if (!ok) return
        runCatching { tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis") }
    }

    fun stop() {
        runCatching { tts?.stop() }
    }

    fun shutdown() {
        runCatching { tts?.shutdown() }
        tts = null
    }
}
