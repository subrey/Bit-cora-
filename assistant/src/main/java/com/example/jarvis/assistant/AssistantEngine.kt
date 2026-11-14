package com.example.jarvis.assistant

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import com.example.jarvis.ai.AiPrompt
import com.example.jarvis.ai.AiProvider
import com.example.jarvis.ai.AiRequest
import com.example.jarvis.ai.AiResult
import com.example.jarvis.ai.AiTurn
import com.example.jarvis.data.JarvisRepository
import com.example.jarvis.domain.model.JarvisPersona
import com.example.jarvis.weather.OpenMeteoProvider
import com.example.jarvis.weather.WeatherProvider
import com.example.jarvis.weather.WeatherReport
import com.example.jarvis.weather.WeatherResult
import com.example.jarvis.weather.WeatherText
import com.example.jarvis.widget.JarvisWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
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
    private val ai: AiProvider,
    private val reminderScheduler: ReminderScheduler = ReminderScheduler(context),
    private val weather: WeatherProvider? = null,
    private val cityProvider: suspend () -> String = { "" },
) {

    suspend fun execute(intent: ParsedIntent): AssistantResponse {
        val now = LocalDateTime.now()
        val effective = resolve(intent)
        return when (effective.kind) {
            IntentKind.GREETING -> AssistantResponse(
                "Buenos días, señor. Jarvis en línea. Todo el sistema operativo."
            )

            IntentKind.CHAT -> chat(intent.raw)

            IntentKind.HELP -> AssistantResponse(helpText())

            IntentKind.TIME -> AssistantResponse("Son las ${Tools.formatTime(now)}.")

            IntentKind.DATE -> AssistantResponse("Hoy es ${Tools.formatDate(now)}.")

            IntentKind.MUSIC_PLAY -> playMusic()

            IntentKind.MUSIC_SEARCH -> playByRequest(intent.payload)

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

            IntentKind.REMINDER -> createReminder(intent.payload)

            IntentKind.LIST_REMINDERS -> listReminders()

            IntentKind.CANCEL_REMINDER -> cancelReminder(intent.payload)

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

            IntentKind.EXPORT_HISTORY -> exportHistory()

            IntentKind.KNOWLEDGE -> knowledge(
                effective.payload.ifBlank { intent.raw },
                intent.raw,
            )

            IntentKind.BATTERY -> AssistantResponse(batteryText())

            IntentKind.STORAGE -> AssistantResponse(storageText())

            IntentKind.TORCH -> AssistantResponse(toggleTorch())

            IntentKind.OPEN_APP -> AssistantResponse(openApp(intent.payload))

            IntentKind.JOKE -> AssistantResponse(joke())

            IntentKind.WEATHER -> weatherNow()

            IntentKind.WEATHER_TOMORROW -> weatherTomorrow()

            IntentKind.WEATHER_ADVICE -> weatherAdvice()

            IntentKind.SUMMARY -> AssistantResponse(summary())

            IntentKind.UNKNOWN -> AssistantResponse(
                "No he entendido esa orden, señor. Diga «ayuda» para ver lo que sé hacer."
            )
        }
    }

    /**
     * Responde una pregunta: primero la base local, después Wikipedia y, si no
     * hay resultado, la IA conversacional con la pregunta original completa.
     */
    private suspend fun knowledge(subject: String, raw: String): AssistantResponse {
        val clean = subject.trim()
        if (clean.isBlank()) {
            return AssistantResponse("Dígame sobre qué tema quiere que le informe, señor.")
        }

        val local = Knowledge.search(clean)
        if (local != null) {
            return AssistantResponse("${local.title}. ${local.text}")
        }

        val wiki = Wikipedia.summary(clean)
        if (wiki != null) {
            val short = Wikipedia.shorten(wiki)
            return AssistantResponse("$short\n\n(Fuente: Wikipedia en español)")
        }

        if (ai.isConfigured()) return chat(raw)

        return AssistantResponse(
            "No tengo ese tema en mi base local y no he podido consultar la red, señor. " +
                "Pruebe con: «qué sabes de la Revolución Mexicana», «quién fue Einstein» " +
                "o «explicame el sistema solar»."
        )
    }

    /**
     * Conversación libre con el modelo de lenguaje. Se envía la personalidad,
     * los recuerdos guardados y los últimos turnos; si no hay clave, se explica
     * cómo configurarla sin romper el resto del asistente.
     */
    private suspend fun chat(text: String): AssistantResponse {
        val clean = text.trim()
        if (clean.isBlank()) {
            return AssistantResponse("Dígame qué quiere que le responda, señor.")
        }
        if (!ai.isConfigured()) {
            return AssistantResponse(
                "Estoy en línea y funcionando, señor. Para conversar libremente, configure una " +
                    "clave gratuita de Google Gemini en Ajustes → Inteligencia artificial. " +
                    "Mientras tanto, «ayuda» muestra las órdenes que entiendo."
            )
        }

        val history = runCatching { repository.recentMessages(AiPrompt.MAX_TURNS) }
            .getOrDefault(emptyList())
            .map { AiTurn(fromUser = it.fromUser, text = it.text) }
        val memories = runCatching { repository.observeMemories().first().map { it.text } }
            .getOrDefault(emptyList())
        val request = AiRequest(
            systemPrompt = AiPrompt.systemPrompt(JarvisPersona.systemPromptForToday(), memories),
            turns = AiPrompt.turns(history, clean),
        )

        return when (val result = ai.complete(request)) {
            is AiResult.Success -> AssistantResponse(result.text)
            is AiResult.Failure -> AssistantResponse("No he podido consultar la IA. ${result.message}")
        }
    }

    /**
     * Busca en la biblioteca lo que el usuario ha pedido: un género, un
     * estado de ánimo, un artista o una época. Si no hay coincidencia y hay
     * clave de IA, se le pregunta al modelo cuál de los artistas encaja.
     */
    private suspend fun playByRequest(payload: String): AssistantResponse {
        val tracks = withContext(Dispatchers.IO) { player.scanLibrary() }
        if (tracks.isEmpty()) {
            return AssistantResponse(
                "No encuentro música en el dispositivo. Conceda el permiso de audio o use el tono de prueba."
            )
        }

        val request = MusicRequestParser.parse(payload)
        if (request == null) {
            // No se ha entendido la búsqueda: se pone música normal.
            val track = tracks.random()
            player.play(track)
            return AssistantResponse("Reproduciendo ${track.title}, de ${track.artist}.")
        }

        val best = MusicSearch.search(tracks, request).firstOrNull()
        if (best != null) {
            player.play(best.track)
            return AssistantResponse("Reproduciendo ${best.track.title}, de ${best.track.artist}.")
        }

        val chosen = askAiForTrack(request, tracks)
        if (chosen != null) {
            player.play(chosen)
            return AssistantResponse("He elegido ${chosen.title}, de ${chosen.artist}.")
        }

        return AssistantResponse(
            "No encuentro nada ${request.label} en su biblioteca. " +
                "Pruebe con «pon algo de rock», «música para estudiar» o el nombre de un artista."
        )
    }

    /** Le pregunta a la IA qué artista de la biblioteca encaja con la petición. */
    private suspend fun askAiForTrack(request: MusicRequest, tracks: List<Track>): Track? {
        if (!ai.isConfigured()) return null
        val artists = MusicSearch.artists(tracks)
        if (artists.isEmpty()) return null
        val prompt = "El usuario ha pedido música ${request.label}. " +
            "Estos son los artistas de su biblioteca: ${artists.joinToString(", ")}. " +
            "Responde únicamente con el nombre de un artista de la lista que encaje mejor."
        val result = ai.complete(
            AiRequest(
                systemPrompt = "Eres JARVIS, un asistente musical. Respondes en español y de forma breve.",
                turns = listOf(AiTurn(fromUser = true, text = prompt)),
            ),
        )
        if (result !is AiResult.Success) return null
        val name = result.text.trim()
            .removeSurrounding("\"")
            .removeSurrounding("«")
            .removeSurrounding("»")
            .trim()
            .substringBefore("\n")
            .trim()
        if (name.isBlank()) return null
        return tracks.firstOrNull {
            it.artist.equals(name, ignoreCase = true) ||
                it.artist.contains(name, ignoreCase = true)
        }
    }

    private suspend fun playMusic(): AssistantResponse {
        // La consulta de MediaStore puede tardar segundos con una biblioteca
        // grande: nunca en el hilo principal, o Android muestra "no responde".
        val tracks = withContext(Dispatchers.IO) { player.scanLibrary() }
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

    private suspend fun createReminder(payload: String): AssistantResponse {
        val parsed = ReminderParser.parse(payload) ?: return AssistantResponse(
            "Indíqueme por ejemplo: «recuérdame comprar pan a las 5 de la tarde» o «en 20 minutos»."
        )
        val id = repository.addReminder(parsed.message, parsed.triggerAtMillis)
        reminderScheduler.schedule(id, parsed.message, parsed.triggerAtMillis)
        JarvisWidgetProvider.updateAll(context)
        return AssistantResponse(
            "Recordatorio programado para ${formatMoment(parsed.triggerAtMillis)}: ${parsed.message}."
        )
    }

    /** «qué recordatorios tengo» / «lista mis recordatorios» */
    private suspend fun listReminders(): AssistantResponse {
        val pending = repository.observeReminders().first().filter { !it.done }
        if (pending.isEmpty()) {
            return AssistantResponse("No tiene recordatorios pendientes, señor.")
        }
        val list = pending.joinToString(" · ") { "${it.text} (${formatMoment(it.triggerAt)})" }
        return AssistantResponse("Tiene ${pending.size} recordatorio(s): $list.")
    }

    /** «cancela el recordatorio de comprar pan» */
    private suspend fun cancelReminder(payload: String): AssistantResponse {
        val target = payload.trim()
        if (target.isBlank()) {
            return AssistantResponse("Dígame qué recordatorio quiere cancelar.")
        }
        val pending = repository.observeReminders().first().filter { !it.done }
        val match = pending.lastOrNull { ReminderParser.matchesMessage(it.text, target) }
        if (match == null) {
            return AssistantResponse("No encuentro ningún recordatorio con ese nombre.")
        }
        reminderScheduler.cancel(match.id)
        repository.deleteReminder(match)
        JarvisWidgetProvider.updateAll(context)
        return AssistantResponse("Recordatorio cancelado: ${match.text}.")
    }

    /** «qué tiempo hace»: estado actual, previsión de mañana y un consejo. */
    private suspend fun weatherNow(): AssistantResponse {
        val report = weatherReport() ?: return weatherUnavailable()
        val tomorrow = report.days.getOrNull(1)
        return AssistantResponse(
            buildString {
                append(WeatherText.now(report))
                if (tomorrow != null) {
                    WeatherText.day(report, tomorrow.date, "Mañana")?.let {
                        append(" ")
                        append(it)
                    }
                }
                append(" ")
                append(WeatherText.advice(report))
            },
        )
    }

    /** «qué tiempo hace mañana»: previsión del día siguiente. */
    private suspend fun weatherTomorrow(): AssistantResponse {
        val report = weatherReport() ?: return weatherUnavailable()
        val tomorrow = report.days.getOrNull(1)
            ?: return AssistantResponse("No tengo previsión para mañana, señor.")
        return AssistantResponse(
            WeatherText.day(report, tomorrow.date, "Mañana")
                ?: "No tengo previsión para mañana, señor.",
        )
    }

    /** «¿debo llevar paraguas?»: un consejo breve según el tiempo. */
    private suspend fun weatherAdvice(): AssistantResponse {
        val report = weatherReport() ?: return weatherUnavailable()
        return AssistantResponse(WeatherText.advice(report))
    }

    /** Pide el informe a la fuente del tiempo y anota los errores. */
    private suspend fun weatherReport(): WeatherReport? {
        val provider = weather ?: return null
        val city = cityProvider().trim().ifBlank { OpenMeteoProvider.DEFAULT_CITY }
        return when (val result = provider.report(city)) {
            is WeatherResult.Success -> result.report
            is WeatherResult.Failure -> {
                JarvisBus.notice("⚠ ${result.message}")
                null
            }
        }
    }

    private fun weatherUnavailable(): AssistantResponse = AssistantResponse(
        "No he podido consultar el tiempo. Compruebe la conexión e inténtelo de nuevo, señor.",
    )

    /** «exporta el historial»: guarda la conversación y abre el menú de compartir. */
    private suspend fun exportHistory(): AssistantResponse {
        val messages = repository.recentMessages(EXPORT_MAX_MESSAGES)
        if (messages.isEmpty()) {
            return AssistantResponse("Todavía no hay conversación que exportar, señor.")
        }
        val text = ChatExporter.format(messages)
        val ok = ChatExporter.saveAndShare(context, text)
        return if (ok) {
            AssistantResponse(
                "He guardado el historial en un archivo de texto y he abierto la ventana " +
                    "para que lo comparta."
            )
        } else {
            AssistantResponse("No he podido guardar el historial, señor.")
        }
    }

    /** Fecha y hora legible para hablar y para las notificaciones. */
    private fun formatMoment(millis: Long): String {
        val dateTime = java.time.Instant.ofEpochMilli(millis)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDateTime()
        return "${Tools.formatDate(dateTime)} a las ${Tools.formatTime(dateTime)}"
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
        JarvisWidgetProvider.updateAll(context)
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
        val reminders = repository.pendingReminderCount()
        return buildString {
            append("Estado del sistema: ")
            append("$pending tarea(s) pendiente(s), ")
            append("$memories memoria(s), ")
            append("$timers temporizador(es) activo(s) y ")
            append("$reminders recordatorio(s) pendiente(s). ")
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
            "y recordatorios, listar y cancelar recordatorios, consultar el tiempo y su previsión, " +
            "añadir tareas, memorizar notas, " +
            "decir la hora y la fecha, informar de la " +
            "batería y del almacenamiento, encender la linterna, abrir aplicaciones, contar un chiste, " +
            "recordar lo que hemos hablado o darme un resumen. " +
            "Si configura una clave gratuita de Gemini en Ajustes, también puedo conversar, explicar " +
            "temas y responder preguntas abiertas. " +
            "Inténtelo con: «pon música», «quita la música», «pon un temporizador de 10 minutos», " +
            "«recuérdame comprar pan a las 6 de la tarde», «qué tiempo hace», «qué tiempo hace mañana», " +
            "«qué recordatorios tengo», «calcula 12 * (3 + 4)» o «explícame la fotosíntesis»."

    // --- Conversación larga: entender continuaciones ------------------------

    /** Última intención resuelta, para interpretar frases como «y sube el volumen». */
    private var lastKind: IntentKind? = null

    companion object {
        /** Mensajes máximos que se exportan al archivo de texto. */
        const val EXPORT_MAX_MESSAGES = 500
    }

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
        // Una pregunta o una petición de explicación nunca es una continuación
        // de la orden anterior: debe llegar a la IA.
        if (n.contains("?") ||
            n.startsWith("que ") || n.startsWith("quien ") || n.startsWith("cuando ") ||
            n.startsWith("donde ") || n.startsWith("como ") || n.startsWith("cual ") ||
            n.contains("explica") || n.contains("cuentame") || n.contains("hablame")
        ) {
            return false
        }
        val markers = listOf(
            " y ", "y el", "y la", "y un", "tambien", "ahora", "otra vez", "sigue",
            "de nuevo", " mas ", "mas el", "mas la", "un poco mas", "el mismo",
        )
        return markers.any { n.contains(it) } || n.startsWith("y ") || n.startsWith("y")
    }
}
