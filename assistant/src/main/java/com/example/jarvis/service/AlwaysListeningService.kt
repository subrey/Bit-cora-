package com.example.jarvis.service

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
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
 * Para evitar el bucle de pitidos (micro que se enciende y se apaga sin parar):
 *  - se prefiere el reconocimiento EN EL DISPOSITIVO, que no pita ni usa red;
 *  - las sesiones se mantienen abiertas con silencios largos;
 *  - entre sesión y sesión hay un enfriamiento mínimo, y si el sistema insiste
 *    se va aumentando la espera.
 */
class AlwaysListeningService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val handler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var sessionActive = false
    private var consecutiveFailures = 0
    private var emptyResults = 0
    private var unheard = 0
    private var wakeWord = "jarvis"
    private var lastStartAt = 0L
    private var onDeviceVoice = false

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

                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> scheduleRestart(900L)

                else -> {
                    consecutiveFailures++
                    if (consecutiveFailures > 10) {
                        JarvisBus.notice(
                            "⚠ El sistema sigue cortando el micrófono en segundo plano. " +
                                "Deja la app abierta en pantalla para hablar con Jarvis, " +
                                "o usa el botón del micrófono para darle órdenes."
                        )
                        stopSelf()
                    } else {
                        scheduleRestart(backoff())
                    }
                }
            }
        }

        override fun onResults(results: Bundle?) {
            JarvisBus.listening.value = false
            sessionActive = false
            consecutiveFailures = 0
            val spoken = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                .orEmpty()
            if (spoken.isBlank()) {
                emptyResults++
                if (onDeviceVoice && emptyResults == 5) {
                    JarvisBus.notice(
                        "⚠ El modelo de voz en el dispositivo no está disponible y no reconoce nada. " +
                            "Desactive «Voz en el dispositivo» en Ajustes para usar el motor normal."
                    )
                }
                scheduleRestart(1_800L)
                return
            }
            emptyResults = 0
            val handled = handle(results)
            if (!handled && unheard < 3) {
                unheard++
                JarvisBus.notice("Te he oído «$spoken», pero esperaba la palabra «$wakeWord».")
            }
            // Tras una orden se reabre rápido; si no hubo orden, se espera más
            // para no encadenar sesiones (y por tanto pitidos).
            scheduleRestart(if (handled) 700L else 1_800L)
        }

        override fun onPartialResults(partialResults: Bundle?) {
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

    /** @return true si la orden se ha enviado al asistente. */
    private fun handle(bundle: Bundle?): Boolean {
        val spoken = bundle
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            .orEmpty()
        if (spoken.isBlank()) return false
        val normalized = spoken.lowercase()
        val index = normalized.indexOf(wakeWord)
        if (index < 0) return false
        val command = spoken.substring(index + wakeWord.length).trim()
        if (command.isBlank()) return false
        JarvisBus.send(command)
        return true
    }

    /** Espera creciente entre sesiones si el sistema corta el micrófono. */
    private fun backoff(): Long = when {
        consecutiveFailures <= 2 -> 1_500L
        consecutiveFailures <= 5 -> 3_000L
        else -> 6_000L
    }

    private fun scheduleRestart(delay: Long = 1_500L) {
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ startListening() }, delay)
    }

    private fun startListening() {
        if (sessionActive) return
        // Enfriamiento: nunca encender el micro más de una vez cada 1,2 s.
        val since = System.currentTimeMillis() - lastStartAt
        if (since < 1_200L) {
            handler.postDelayed({ startListening() }, 1_200L - since)
            return
        }
        sessionActive = true
        lastStartAt = System.currentTimeMillis()
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            // Sesiones largas: hay tiempo de hablar después de encender el micro.
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 8_000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 4_000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1_500L)
        }
        runCatching { recognizer?.startListening(intent) }
            .onFailure {
                sessionActive = false
                consecutiveFailures++
                handler.postDelayed({ startListening() }, backoff())
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
            recognizer = createRecognizer(onDeviceVoice)
            startListening()
        }
        if (app != null) {
            scope.launch {
                app.settings.onDeviceVoice.collect { onDevice ->
                    if (onDevice != onDeviceVoice) {
                        onDeviceVoice = onDevice
                        handler.post { switchRecognizer(onDevice) }
                    }
                }
            }
        }
    }

    /** Cambia entre el motor de red y el que vive en el dispositivo. */
    private fun switchRecognizer(onDevice: Boolean) {
        runCatching { recognizer?.cancel() }
        runCatching { recognizer?.destroy() }
        recognizer = createRecognizer(onDevice)
        sessionActive = false
        consecutiveFailures = 0
        startListening()
    }

    /**
     * El motor en el dispositivo no pita ni usa red, pero si el sistema no tiene
     * el modelo de voz descargado no devuelve resultados: por eso es opcional.
     */
    private fun createRecognizer(onDevice: Boolean): SpeechRecognizer? {
        if (onDevice && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching {
                SpeechRecognizer.createOnDeviceSpeechRecognizer(this).also {
                    it.setRecognitionListener(listener)
                }
            }.getOrNull()?.let { return it }
        }
        return runCatching {
            SpeechRecognizer.createSpeechRecognizer(this).also {
                it.setRecognitionListener(listener)
            }
        }.getOrNull()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val micGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        if (!micGranted) {
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
                    consecutiveFailures = 0
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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
