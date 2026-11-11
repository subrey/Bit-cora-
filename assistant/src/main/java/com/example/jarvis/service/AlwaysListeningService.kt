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
import androidx.core.app.NotificationManagerCompat
import com.example.jarvis.JarvisApplication
import com.example.jarvis.assistant.JarvisBus
import com.example.jarvis.assistant.Notifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Servicio en primer plano que escucha de forma continua y responde a la
 * palabra clave ("Jarvis"). La orden se envía por el bus interno.
 */
class AlwaysListeningService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val handler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var listening = false
    private var wakeWord = "jarvis"

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) = Unit
        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsd: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() = Unit

        override fun onError(error: Int) {
            restart()
        }

        override fun onResults(results: Bundle?) {
            handle(results)
            restart()
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

    private fun restart() {
        handler.postDelayed({
            runCatching { recognizer?.cancel() }
            startListening()
        }, 900)
    }

    private fun startListening() {
        if (listening) return
        listening = true
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        runCatching { recognizer?.startListening(intent) }
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
        runCatching { recognizer?.destroy() }
        recognizer = null
        listening = false
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
