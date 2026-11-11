package com.example.jarvis.service

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
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
 * palabra clave ("Jarvis").
 *
 * Claves para que no se corte:
 *  - la sesión de reconocimiento se mantiene abierta con silencios largos,
 *    para que dé tiempo a hablar después de encender el micro;
 *  - al terminar una sesión se vuelve a escuchar de inmediato, sin cancelar
 *    antes (cancelar produce los pitidos de apagado/encendido);
 *  - si el sistema insiste en cortar el micro, se avisa y se para en lugar
 *    de ficar en bucle.
 */
class AlwaysListeningService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val handler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var sessionActive = false
    private var failures = 0
    private var successes = 0
    private var wakeWord = "jarvis"

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            JarvisBus.listening.value = true
        }

        override fun onBeginningOfSpeech() = Unit

        override fun onRmsChanged(rmsd: Float) = Unit

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() = Unit

        override fun onError(error: Int) {
            JarvisBus.listening.value = false
            sessionActive = false
            when (error) {
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                    JarvisBus.notice(
                        "⚠ Jarvis no puede escuchar: falta el permiso de micrófono. " +
                            "Actívalo en los ajustes del sistema para Android."
                    )
                    stopSelf()
                }

                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> scheduleRestart(delay = 700L)

                else -> {
                    failures++
                    if (failures > 8 && successes == 0) {
                        JarvisBus.notice(
                            "⚠ El sistema está cortando el micrófono en segundo plano. " +
                                "Deja la app abierta en pantalla para hablar con Jarvis, " +
                                "o usa el botón del micrófono para darle órdenes."
                        )
                        stopSelf()
                    } else {
                        scheduleRestart()
                    }
                }
            }
        }

        override fun onResults(results: Bundle?) {
            JarvisBus.listening.value = false
            sessionActive = false
            failures = 0
            successes++
            handle(results)
            scheduleRestart(delay = 400L)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            // Los parciales solo sirven para detección temprana de la palabra clave.
            if (partialResults == null) return
            val spoken = partialResults
                .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                .orEmpty()
            if (spoken.lowercase().contains(wakeWord)) {
                JarvisBus.listening.value = true
            }
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

    /** Vuelve a escuchar con una espera corta, sin cancelar la sesión anterior. */
    private fun scheduleRestart(delay: Long = 600L) {
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({
            startListening()
        }, delay)
    }

    private fun startListening() {
        if (sessionActive) return
        sessionActive = true
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            // Sesiones largas: da tiempo a hablar después de encender el micro.
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 5_000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2_500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1_200L)
        }
        runCatching { recognizer?.startListening(intent) }
            .onFailure {
                sessionActive = false
                handler.postDelayed({ startListening() }, 900L)
            }
    }

    private fun stopListening() {
        handler.removeCallbacksAndMessages(null)
        runCatching { recognizer?.cancel() }
        sessionActive = false
        JarvisBus.listening.value = false
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
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
        val micGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        if (!micGranted) {
            // Sin permiso de micrófono no se puede levantar un servicio de este tipo.
            stopSelf()
            return START_NOT_STICKY
        }
        runCatching { startForeground(NOTIFICATION_ID, buildNotification()) }.onFailure {
            stopSelf()
            return START_NOT_STICKY
        }
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
        when (intent?.action) {
            ACTION_REARM -> {
                // La app vuelve al primer plano: el reconocimiento es mucho más
                // fiable en pantalla, así que se reinicia la sesión.
                handler.post {
                    runCatching { recognizer?.cancel() }
                    sessionActive = false
                    failures = 0
                    startListening()
                }
            }

            else -> handler.post { startListening() }
        }
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
        isRunning = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): android.os.IBinder? = null

    companion object {
        private const val NOTIFICATION_ID = 42
        const val ACTION_REARM = "com.example.jarvis.action.REARM"

        /** true mientras el servicio de escucha está vivo en este proceso. */
        @Volatile
        var isRunning: Boolean = false

        fun start(context: android.content.Context) {
            if (!hasMicPermission(context)) return
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

        /**
         * Relanza la escucha: útil cuando la app pasa al primer plano.
         * Solo si el servicio ya está vivo y hay permiso de micrófono,
         * porque levantar un servicio de micrófono sin permiso revienta la app.
         */
        fun rearm(context: android.content.Context) {
            if (!isRunning) return
            if (!hasMicPermission(context)) return
            val intent = Intent(context, AlwaysListeningService::class.java).setAction(ACTION_REARM)
            runCatching {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }

        private fun hasMicPermission(context: android.content.Context): Boolean =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED
    }
}
