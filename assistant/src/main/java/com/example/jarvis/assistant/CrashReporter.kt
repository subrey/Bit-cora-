package com.example.jarvis.assistant

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Guarda el rastro de los cierres y fallos de voz para poder mostrarlo en el
 * chat la próxima vez que se abre la app.
 *
 * En este proyecto no hay acceso a `logcat`, así que el diagnóstico viaja
 * dentro de la propia aplicación:
 *  - una excepción no capturada deja su pila en `last_crash.txt`;
 *  - un fallo controlado (por ejemplo, un callback de voz que lanza) se anota
 *    en `last_error.txt` en vez de cerrar la app;
 *  - si la app muere mientras se activaba el micrófono sin dejar excepción de
 *    Java (cierre nativo o del sistema), queda marcada la sesión de micrófono.
 */
object CrashReporter {

    private const val TAG = "JarvisCrash"
    private const val PREFS = "jarvis_diagnostics"
    private const val KEY_MIC_OPEN = "mic_open"
    private const val CRASH_FILE = "last_crash.txt"
    private const val ERROR_FILE = "last_error.txt"

    /** Instala el capturador global de excepciones no controladas. */
    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                File(app.filesDir, CRASH_FILE).writeText(
                    buildString {
                        appendLine("Fecha: ${stamp()}")
                        appendLine("Dispositivo: ${deviceLine()}")
                        appendLine("Hilo: ${thread.name}")
                        appendLine(error.stackTraceToString())
                    },
                )
                setMicOpen(app, false)
            }
            // Se deja actuar al sistema para que muestre su diálogo habitual.
            if (previous != null) {
                previous.uncaughtException(thread, error)
            } else {
                android.os.Process.killProcess(android.os.Process.myPid())
                kotlin.system.exitProcess(2)
            }
        }
    }

    /** El micrófono se está abriendo: si la app muere ahora, lo sabremos. */
    fun markMicOpen(context: Context) {
        setMicOpen(context.applicationContext, true)
    }

    /** El micrófono ya respondió: la activación no ha cerrado la app. */
    fun markMicReady(context: Context) {
        setMicOpen(context.applicationContext, false)
    }

    /** Anota un fallo que se ha podido capturar sin cerrar la aplicación. */
    fun note(context: Context, message: String, error: Throwable? = null) {
        Log.e(TAG, message, error)
        runCatching {
            File(context.applicationContext.filesDir, ERROR_FILE).writeText(
                buildString {
                    appendLine("Fecha: ${stamp()}")
                    appendLine("Dispositivo: ${deviceLine()}")
                    appendLine(message)
                    if (error != null) appendLine(error.stackTraceToString())
                },
            )
        }
    }

    /**
     * Devuelve el diagnóstico pendiente (si lo hay) y lo consume, para no
     * repetirlo en cada arranque.
     */
    fun consume(context: Context): String? {
        val app = context.applicationContext
        val crash = readAndClear(app, CRASH_FILE)
        val nonFatal = readAndClear(app, ERROR_FILE)
        val micOpen = micOpen(app)
        setMicOpen(app, false)
        return when {
            crash != null ->
                "⚠ La aplicación se cerró la última vez:\n" + crash.take(1_500)
            nonFatal != null ->
                "⚠ Último fallo de voz registrado:\n" + nonFatal.take(1_000)
            micOpen ->
                "⚠ La última vez la app se cerró al activar el micrófono y no dejó " +
                    "un error de Java. Puede ser un fallo del motor de voz del sistema; " +
                    "pruebe a desactivar «Voz en el dispositivo» en Ajustes."
            else -> null
        }
    }

    private fun readAndClear(app: Context, name: String): String? {
        val file = File(app.filesDir, name)
        val text = runCatching { if (file.exists()) file.readText() else null }.getOrNull()
        if (text != null) runCatching { file.delete() }
        return text?.takeIf { it.isNotBlank() }
    }

    private fun micOpen(app: Context): Boolean =
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_MIC_OPEN, false)

    private fun setMicOpen(app: Context, open: Boolean) {
        runCatching {
            // commit(): la marca debe quedar escrita aunque la app muera justo
            // después, así que no se puede dejar en segundo plano.
            app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_MIC_OPEN, open)
                .commit()
        }
    }

    private fun deviceLine(): String =
        "${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE} " +
            "(API ${Build.VERSION.SDK_INT})"

    private fun stamp(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(Date())
}
