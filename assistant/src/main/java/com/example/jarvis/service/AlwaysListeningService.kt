package com.example.jarvis.service

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.NotificationCompat
import com.example.jarvis.JarvisApplication
import com.example.jarvis.assistant.JarvisBus
import com.example.jarvis.assistant.Notifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Servicio en primer plano que escucha de forma continua y responde a la
 * palabra clave ("Jarvis"). La orden se envía por el bus interno.
 *
 * El reconocimiento de voz se mantiene vivo reintentando con espera
 * progresiva: si el sistema lo corta, se vuelve a escuchar al instante.
 */
class AlwaysListeningService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val handler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var listening = false
    private var failures = 0
    private var wakeWord = "jarvis"

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            JarvisBus.listening.value = true
            failures = 0
        }

        override fun onBeginningOfSpeech() = Unit

        override fun onRmsChanged(rmsd: Float) = Unit

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() = Unit

        override fun onError(error: Int) {
            JarvisBus.listening.value = false
            listening = false
            if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                JarvisBus.notice(
                    "⚠ Jarvis no puede escuchar: falta el permiso de micrófono. " +
                        "Actívalo en los ajustes del sistema para Android."
                )
                stopSelf()
                return
            }
            scheduleRestart()
        }

        override fun onResults(results: Bundle?) {
            handle(results)
            scheduleRestart()
        }

        override fun onPartialResults(partialResults: Bundle?) {
            handle(partialResults)
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private fun handle(bundle: Bundle?) {
        val spoken = bundle
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            .orEmpty()
        if (spoken.isBlank()) return
        val normalized = spoken.lowercase()
        val index = normalized.indexOf(wakeWord)
        if (index < 0) return
        val command = spoken.substring(index + wakeWord.length).trim()
        if (command.isNotBlank()) {
            JarvisBus.send(command)
        }
    }

    /** Vuelve a escuchar tras un corte, esperando cada vez un poco más. */
    private fun scheduleRestart() {
        handler.removeCallbacksAndMessages(null)
        val wait = when {
            failures <= 1 -> 300L
            failures <= 3 -> 1_200L
            else -> 2_500L
        }
        handler.postDelayed({
            failures++
            listening = false
            runCatching { recognizer?.cancel() }
            startListening()
        }, wait)
    }

    private fun startListening() {
        if (listening) return
        listening = true
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
        }
        runCatching { recognizer?.startListening(intent) }
            .onFailure {
                listening = false
                handler.postDelayed({ startListening() }, 1_500L)
            }
    }

    private fun stopListening() {
        handler.removeCallbacksAndMessages(null)
        runCatching { recognizer?.cancel() }
        listening = false
        JarvisBus.listening.value = false
    }

    override fun onCreate() {
        super.onCreate()
        Notifications.ensureChannel(this)
        val app = applicationContext as? JarvisApplication
        if (app != null) {
            scope.launch {
                app.settings.wakeWord.collect { word ->
                    wakeWord = word.ifBlank { "jarvis" }.lowercase()
                }
            }
        }
        handler.post {
            recognizer = runCatching {
                SpeechRecognizer.createSpeechRecognizer(this).also {
                    it.setRecognitionListener(listener)
                }
            }.getOrNull()
            startListening()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        val app = applicationContext as? JarvisApplication
        if (app != null) {
            scope.launch {
                val enabled = app.settings.alwaysListening.first()
                if (!enabled) {
                    stopSelf()
                    return@launch
                }
            }
        }
        handler.post { startListening() }
        return START_STICKY
    }

    private fun buildNotification() = NotificationCompat.Builder(this, Notifications.CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_btn_speak_now)
        .setContentTitle("JARVIS")
        .setContentText("Escuchando… di «Jarvis»")
        .setOngoing(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .build()

    override fun onDestroy() {
        stopListening()
        runCatching { recognizer?.destroy() }
        recognizer = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): android.os.IBinder? = null

    companion object {
        private const val NOTIFICATION_ID = 42

        fun start(context: android.content.Context) {
            val intent = Intent(context, AlwaysListeningService::class.java)
            runCatching {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }

        fun stop(context: android.content.Context) {
            runCatching {
                context.stopService(Intent(context, AlwaysListeningService::class.java))
            }
        }
    }
}
