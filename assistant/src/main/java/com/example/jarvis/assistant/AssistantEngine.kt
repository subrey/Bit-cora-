package com.example.jarvis.assistant

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import com.example.jarvis.data.JarvisRepository
import java.time.LocalDateTime
import java.util.Locale
import kotlin.math.roundToInt

data class AssistantResponse(val text: String, val speak: Boolean = true)

/**
 * Motor del asistente: ejecuta la intención detectada y devuelve la respuesta.
 */
class AssistantEngine(
    private val context: Context,
    private val repository: JarvisRepository,
    private val player: MusicPlayer,
    private val scheduler: TimerScheduler,
) {

    suspend fun execute(intent: ParsedIntent): AssistantResponse {
        val now = LocalDateTime.now()
        val effective = resolve(intent)
        return when (effective.kind) {
            IntentKind.GREETING -> AssistantResponse(
                "Buenos días, señor. Jarvis en línea. Todo el sistema operativo."
            )

            IntentKind.HELP -> AssistantResponse(helpText())

            IntentKind.TIME -> AssistantResponse("Son las ${Tools.formatTime(now)}.")

            IntentKind.DATE -> AssistantResponse("Hoy es ${Tools.formatDate(now)}.")

            IntentKind.MUSIC_PLAY -> playMusic()

            IntentKind.MUSIC_PAUSE -> {
                player.toggle()
                AssistantResponse("Música en pausa. Silencio en la sala.")
            }

            IntentKind.MUSIC_NEXT -> {
                player.next()
                AssistantResponse("Cambio de canción, señor.")
            }

            IntentKind.MUSIC_PREVIOUS -> {
                player.previous()
                AssistantResponse("Vuelvo a la canción anterior.")
            }

            IntentKind.MUSIC_STOP -> {
                player.stop()
                AssistantResponse("Música detenida.")
            }

            IntentKind.MUSIC_SHUFFLE -> {
                player.setShuffle(true)
                AssistantResponse("Modo aleatorio activado.")
            }

            IntentKind.MUSIC_REPEAT -> {
                val ok = player.repeatCurrent()
                if (ok) {
                    AssistantResponse("Repito la canción.")
                } else {
                    AssistantResponse("No hay ninguna canción en reproducción.")
                }
            }

            IntentKind.VOLUME_UP -> {
                val next = player.state.value.volume + 0.1f
                player.setVolume(next)
                AssistantResponse("Volumen al ${(next * 100).roundToInt()} por ciento.")
            }

            IntentKind.VOLUME_DOWN -> {
                val next = player.state.value.volume - 0.1f
                player.setVolume(next)
                AssistantResponse("Volumen al ${(next * 100).roundToInt()} por ciento.")
            }

            IntentKind.CALCULATE -> {
                val value = Tools.evaluate(intent.payload)
                if (value == null) {
                    AssistantResponse("No he podido calcular esa expresión.")
                } else {
                    AssistantResponse("El resultado es ${formatNumber(value)}.")
                }
            }

            IntentKind.CONVERT -> convert(intent.payload)

            IntentKind.TIMER -> createTimer(intent.payload)

            IntentKind.TODO -> {
                val text = intent.payload.ifBlank { intent.raw }
                repository.addTodo(text)
                AssistantResponse("Tarea añadida a la lista: $text.")
            }

            IntentKind.MEMORY -> {
                val text = intent.payload.ifBlank { intent.raw }
                repository.addMemory(text)
                AssistantResponse("Memorizado: $text.")
            }

            IntentKind.LIST_MEMORIES -> {
                val latest = repository.latestMemory()
                val info = if (latest == null) {
                    "Todavía no he memorizado nada."
                } else {
                    "Lo último que memoricé fue: ${latest.text}"
                }
                AssistantResponse("$info Hay ${repository.memoryCount()} memoria(s) guardada(s).")
            }

            IntentKind.HISTORY -> {
                val recent = repository.recentUserMessages(5)
                if (recent.isEmpty()) {
                    AssistantResponse("Todavía no hemos hablado, señor.")
                } else {
                    val list = recent.joinToString(" · ") { "«$it»" }
                    AssistantResponse("Lo último que hemos hablado: $list.")
                }
            }

            IntentKind.BATTERY -> AssistantResponse(batteryText())

            IntentKind.STORAGE -> AssistantResponse(storageText())

            IntentKind.TORCH -> AssistantResponse(toggleTorch())

            IntentKind.OPEN_APP -> AssistantResponse(openApp(intent.payload))

            IntentKind.JOKE -> AssistantResponse(joke())

            IntentKind.WEATHER -> AssistantResponse(
                "No dispongo de conexión a los servicios meteorológicos, señor. " +
                    "Le sugiero consultar el widget del tiempo del sistema."
            )

            IntentKind.SUMMARY -> AssistantResponse(summary())

            IntentKind.UNKNOWN -> AssistantResponse(
                "No he entendido esa orden, señor. Diga «ayuda» para ver lo que sé hacer."
            )
        }
    }

    private fun playMusic(): AssistantResponse {
        val tracks = player.scanLibrary()
        if (tracks.isEmpty()) {
            return AssistantResponse(
                "No encuentro música en el dispositivo. Conceda el permiso de audio o use el tono de prueba."
            )
        }
        val track = tracks.random()
        player.play(track)
        return AssistantResponse("Reproduciendo ${track.title}, de ${track.artist}.")
    }

    private fun convert(payload: String): AssistantResponse {
        val parsed = Tools.parseConversion(payload)
        if (parsed == null) {
            return AssistantResponse("Indíqueme por ejemplo: convierte 10 km a millas.")
        }
        val (value, from, to) = parsed
        val result = Tools.convert(value, from, to)
        if (result == null) {
            return AssistantResponse("No conozco esas unidades o no son del mismo tipo.")
        }
        return AssistantResponse(
            "${formatNumber(value)} ${from.trim()} son ${formatNumber(result)} ${to.trim()}."
        )
    }

    private suspend fun createTimer(payload: String): AssistantResponse {
        val seconds = parseDurationSeconds(payload)
        if (seconds == null || seconds <= 0) {
            return AssistantResponse(
                "Dígame el tiempo así: «pon un temporizador de 10 minutos»."
            )
        }
        val label = extractTimerLabel(payload)
        val id = repository.addTimer(label, seconds)
        scheduler.schedule(id, label, seconds)
        val minutes = seconds / 60
        val text = if (minutes >= 1) "$minutes minuto(s)" else "$seconds segundo(s)"
        return AssistantResponse("Temporizador programado: $text. Le avisaré al terminar.")
    }

    private fun extractTimerLabel(payload: String): String {
        val cleaned = payload.replace(Regex("\\b\\d+\\s*(min|minutos?|h|horas?|seg|segundos?)\\b"), "")
            .replace(Regex("\\b(pon|un|una|temporizador|recordatorio|avisame|avisa|en|al|por)\\b"), "")
            .trim()
        return cleaned.ifBlank { "Temporizador" }.take(40)
    }

    fun parseDurationSeconds(payload: String): Int? {
        val regex = Regex("(\\d+)\\s*(minutos|min|segundos|seg|horas|hora|h|s)")
        val match = regex.find(payload.lowercase(Locale.forLanguageTag("es"))) ?: return null
        val amount = match.groupValues[1].toIntOrNull() ?: return null
        val unit = match.groupValues[2]
        return when (unit) {
            "h", "hora", "horas" -> amount * 3600
            "s", "seg", "segundo", "segundos" -> amount
            else -> amount * 60
        }
    }

    private fun batteryText(): String {
        val manager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val intent = context.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        return "La batería está al $level por ciento" +
            if (charging) " y se está cargando." else "."
    }

    private fun storageText(): String {
        val stat = StatFs(Environment.getDataDirectory().path)
        val total = stat.totalBytes
        val available = stat.availableBytes
        val totalGb = total / 1_073_741_824.0
        val availableGb = available / 1_073_741_824.0
        return "Dispone de ${formatNumber(totalGb)} GB en total y ${formatNumber(availableGb)} GB libres."
    }

    private fun toggleTorch(): String {
        return runCatching {
            val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = manager.cameraIdList.firstOrNull { id ->
                manager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            if (cameraId == null) {
                "Este dispositivo no tiene linterna."
            } else {
                val torchOn = !torchState
                manager.setTorchMode(cameraId, torchOn)
                torchState = torchOn
                if (torchOn) "Linterna encendida." else "Linterna apagada."
            }
        }.getOrElse { "No puedo controlar la linterna." }
    }

    private var torchState = false

    private fun openApp(name: String): String {
        if (name.isBlank()) return "¿Qué aplicación wishes que abra?"
        val pm = context.packageManager
        val target = name.trim().lowercase(Locale.forLanguageTag("es"))
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN)
            .addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        val apps = runCatching { pm.queryIntentActivities(intent, 0) }.getOrElse { emptyList() }
        val match = apps.firstOrNull { info ->
            val label = runCatching { info.loadLabel(pm).toString() }.getOrElse { "" }.lowercase(Locale.forLanguageTag("es"))
            label.contains(target)
        }
        return if (match == null) {
            "No encuentro ninguna aplicación llamada $name."
        } else {
            val launch = match.activityInfo?.packageName?.let { pm.getLaunchIntentForPackage(it) }
            if (launch == null) {
                "No he podido abrir $name."
            } else {
                launch.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(launch) }
                "Abriendo $name."
            }
        }
    }

    private fun joke(): String {
        val jokes = listOf(
            "¿Por qué los computers nunca tienen frío? Porque tienen procesadores.",
            "Un byte entra en una barra y sale del bar con la misma capacidad.",
            "¿Qué hace un escáner de IA en la playa? ¡Saca!",
            "Había una vez un antivirus que se fue a vivir al campo. Ya no mordía.",
            "Los programadores no tienen miedo: les da miedo el segmento de memoria.",
            "¿Por qué elSCI no sale en las fiestas? Porque le gusta el C#.",
        )
        return jokes.random()
    }

    private suspend fun summary(): String {
        val pending = repository.pendingTodoCount()
        val memories = repository.memoryCount()
        val timers = repository.activeTimerCount()
        return buildString {
            append("Estado del sistema: ")
            append("$pending tarea(s) pendiente(s), ")
            append("$memories memoria(s) y ")
            append("$timers temporizador(es) activo(s). ")
            append(batteryText())
        }
    }

    private fun formatNumber(value: Double): String {
        val rounded = (value * 100.0).roundToInt() / 100.0
        return if (rounded == rounded.toLong().toDouble()) {
            rounded.toLong().toString()
        } else {
            rounded.toString()
        }
    }

    private fun helpText(): String =
        "Puedo hacer esto: poner música, pausar, quitar la música, cambiar o repetir canción, " +
            "subir o bajar el volumen, calcular expresiones, convertir unidades, poner temporizadores " +
            "y recordatorios, añadir tareas, memorizar notas, decir la hora y la fecha, informar de la " +
            "batería y del almacenamiento, encender la linterna, abrir aplicaciones, contar un chiste, " +
            "recordar lo que hemos hablado o darme un resumen. " +
            "Inténtelo con: «pon música», «quita la música», «pon un temporizador de 10 minutos», " +
            "«calcula 12 * (3 + 4)»."

    // --- Conversación larga: entender continuaciones ------------------------

    /** Última intención resuelta, para interpretar frases como «y sube el volumen». */
    private var lastKind: IntentKind? = null

    private fun resolve(intent: ParsedIntent): ParsedIntent {
        if (intent.kind == IntentKind.UNKNOWN && isFollowUp(intent.raw)) {
            val previous = lastKind
            if (previous != null && previous != IntentKind.UNKNOWN) {
                return intent.copy(kind = previous)
            }
        }
        if (intent.kind != IntentKind.UNKNOWN) {
            lastKind = intent.kind
        }
        return intent
    }

    private fun isFollowUp(text: String): Boolean {
        val n = text.lowercase(Locale.forLanguageTag("es")).trim()
        val markers = listOf(
            " y ", "y el", "y la", "y un", "tambien", "ahora", "otra vez", "sigue",
            "de nuevo", " mas ", "mas el", "mas la", "un poco mas", "el mismo",
        )
        return markers.any { n.contains(it) } || n.startsWith("y ") || n.startsWith("y")
    }
}
