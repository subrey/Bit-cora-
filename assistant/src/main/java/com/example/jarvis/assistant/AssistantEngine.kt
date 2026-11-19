package com.example.jarvis.assistant

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
import com.example.jarvis.ai.GeminiProvider
import com.example.jarvis.data.JarvisRepository
import com.example.jarvis.data.AlarmItem
import com.example.jarvis.data.RoutineItem
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
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
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
    private val routineScheduler: RoutineScheduler = RoutineScheduler(context),
    private val alarmScheduler: AlarmScheduler = AlarmScheduler(context),
    private val weather: WeatherProvider? = null,
    private val cityProvider: suspend () -> String = { "" },
) {

    suspend fun execute(intent: ParsedIntent): AssistantResponse {
        val now = LocalDateTime.now()
        val conversational = conversationActive()
        val effective = resolve(intent)
        var keepConversation = effective.kind == IntentKind.CHAT ||
            effective.kind == IntentKind.KNOWLEDGE ||
            effective.kind == IntentKind.WEB_SEARCH
        val response = when (effective.kind) {
            IntentKind.GREETING -> AssistantResponse(
                "Buenos días, señor. Jarvis en línea. Todo el sistema operativo."
            )

            IntentKind.CHAT -> chat(intent.raw)

            IntentKind.HELP -> AssistantResponse(helpText())

            IntentKind.NETWORK_TEST -> networkTest()

            IntentKind.TIME -> AssistantResponse("Son las ${Tools.formatTime(now)}.")

            IntentKind.DATE -> AssistantResponse("Hoy es ${Tools.formatDate(now)}.")

            IntentKind.MUSIC_PLAY -> playMusic()

            IntentKind.MUSIC_SEARCH -> playByRequest(intent.payload)

            IntentKind.ROUTINE_CREATE -> createRoutine(intent.payload)

            IntentKind.ROUTINE_LIST -> listRoutines()

            IntentKind.ROUTINE_DELETE -> deleteRoutine(intent.payload)

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

            IntentKind.ALARM_CREATE -> createAlarm(intent.payload)

            IntentKind.ALARM_LIST -> listAlarms()

            IntentKind.ALARM_DELETE -> deleteAlarm(intent.payload)

            IntentKind.TIMER -> createTimer(intent.payload)

            IntentKind.TODO -> {
                val text = intent.payload.ifBlank { intent.raw }
                val parsed = ReminderParser.parseStrict(text)
                if (parsed != null) {
                    // La tarea lleva fecha: «llamar al banco mañana a las 10».
                    repository.addTodo(parsed.message, parsed.triggerAtMillis)
                    AssistantResponse(
                        "Tarea añadida para ${Agenda.relativeLabel(parsed.triggerAtMillis, LocalDate.now())} " +
                            "a las ${Agenda.time(parsed.triggerAtMillis)}: ${parsed.message}."
                    )
                } else {
                    repository.addTodo(text)
                    AssistantResponse("Tarea añadida a la lista: $text.")
                }
            }

            IntentKind.AGENDA_TODAY -> agendaToday()

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
                conversational = conversational,
            )

            IntentKind.BATTERY -> AssistantResponse(batteryText())

            IntentKind.STORAGE -> AssistantResponse(storageText())

            IntentKind.TORCH -> AssistantResponse(toggleTorch())

            IntentKind.OPEN_APP -> AssistantResponse(openApp(intent.payload))

            IntentKind.JOKE -> AssistantResponse(joke())

            IntentKind.WEATHER -> weatherNow()

            IntentKind.WEATHER_TOMORROW -> weatherTomorrow()

            IntentKind.WEATHER_ADVICE -> weatherAdvice()

            IntentKind.WEB_SEARCH -> webSearch(intent.payload)

            IntentKind.SUMMARY -> AssistantResponse(summary())

            IntentKind.UNKNOWN -> {
                if (ai.isConfigured()) {
                    // Cualquier frase que no sea una orden local se trata como
                    // conversación: así JARVIS responde como una app de IA.
                    keepConversation = true
                    lastKind = IntentKind.CHAT
                    chat(intent.raw)
                } else {
                    AssistantResponse(
                        "No he entendido esa orden, señor. Diga «ayuda» para ver lo que sé hacer."
                    )
                }
            }
        }

        if (keepConversation) {
            lastConversationAt = System.currentTimeMillis()
        } else if (effective.kind != IntentKind.GREETING) {
            lastConversationAt = 0L
        }
        return response
    }

    /**
     * Responde una pregunta: primero la base local; después Wikipedia para las
     * preguntas factuales; y, si no hay resultado, la IA. En una conversación,
     * las preguntas de seguimiento usan el historial.
     */
    private suspend fun knowledge(
        subject: String,
        raw: String,
        conversational: Boolean = false,
    ): AssistantResponse {
        val clean = subject.trim()
        if (clean.isBlank()) {
            return AssistantResponse("Dígame sobre qué tema quiere que le informe, señor.")
        }

        // Una opinión o una frase de charla no debe resolverse con una
        // definición local o de Wikipedia: que la responda la IA.
        if (isConversationalQuestion(raw) && ai.isConfigured()) {
            return chat(raw, factual = false)
        }

        val local = Knowledge.search(clean)
        if (local != null) {
            return AssistantResponse("${local.title}. ${local.text}")
        }

        // En una conversación, una pregunta como «¿y por qué?» no debe buscar
        // en Wikipedia: es mejor que la IA use el historial.
        if (!conversational) {
            val wiki = wikipediaAnswer(clean)
            if (wiki != null) return wiki
        }

        if (ai.isConfigured()) return chat(raw, factual = !conversational)

        if (conversational) {
            val wiki = wikipediaAnswer(clean)
            if (wiki != null) return wiki
        }

        return AssistantResponse(
            "No tengo ese tema en mi base local y no he podido consultar la red, señor. " +
                "Pruebe con: «qué sabes de la Revolución Mexicana», «quién fue Einstein» " +
                "o «explicame el sistema solar»."
        )
    }

    private suspend fun wikipediaAnswer(subject: String): AssistantResponse? {
        val wiki = Wikipedia.summary(subject) ?: return null
        val short = Wikipedia.shorten(wiki)
        return AssistantResponse("$short\n\n(Fuente: Wikipedia en español)")
    }

    /**
     * Busca en internet y devuelve un resumen estructurado. Usa el grounding
     * de Google Search cuando está disponible; si el modelo o la cuota no lo
     * permiten, cae a Wikipedia + el propio modelo y lo dice con claridad.
     */
    private suspend fun webSearch(subject: String): AssistantResponse {
        val clean = subject.trim()
        if (clean.isBlank()) {
            return AssistantResponse("Dígame qué tema quiere que busque en internet, señor.")
        }

        if (!ai.isConfigured()) {
            val wiki = Wikipedia.summary(clean)
            return if (wiki != null) {
                val short = Wikipedia.shorten(wiki, 700)
                AssistantResponse(
                    "He encontrado esto en Wikipedia:\n\n$short\n\n" +
                        "Para buscar en toda la web y resumirlo, configure una clave gratuita " +
                        "de Gemini en Ajustes → Inteligencia artificial."
                )
            } else {
                AssistantResponse(
                    "No puedo buscar en internet sin una clave de IA, señor. " +
                        "Configúrela en Ajustes → Inteligencia artificial."
                )
            }
        }

        val basePrompt = JarvisPersona.systemPromptForToday()
        val searchPrompt = buildString {
            appendLine("Busca en internet información reciente y fiable sobre este tema: «$clean».")
            appendLine()
            appendLine("Devuelve un resumen en español de España con esta estructura:")
            appendLine("- Una línea de titular.")
            appendLine("- Después, de 4 a 6 puntos clave, uno por línea, cada uno empezando por «• ».")
            appendLine("- Por último, una frase de cierre con lo esencial.")
            appendLine("Usa solo información de las fuentes encontradas. No inventes datos ni fechas.")
        }
        val request = AiRequest(
            systemPrompt = AiPrompt.factualPrompt(basePrompt),
            turns = listOf(AiTurn(fromUser = true, text = searchPrompt)),
            temperature = 0.2,
            useGoogleSearch = true,
        )

        val current = ai.currentModel()
        val alternative = if (current == GeminiProvider.QUALITY_MODEL) {
            GeminiProvider.DEFAULT_MODEL
        } else {
            GeminiProvider.QUALITY_MODEL
        }

        val primary = ai.complete(request)
        if (primary is AiResult.Success) return formatSearchResult(primary)

        val primaryError = (primary as AiResult.Failure).message
        if (primaryError.contains("clave", ignoreCase = true)) {
            return AssistantResponse("No he podido buscar en internet, señor. $primaryError")
        }

        // Si el modelo actual no admite la búsqueda, se prueba con el otro
        // antes de renunciar a la consulta en directo.
        val groundingRetry = ai.complete(request.copy(model = alternative))
        if (groundingRetry is AiResult.Success) return formatSearchResult(groundingRetry)

        // Sin búsqueda: se intenta el resumen con ambos modelos usando
        // Wikipedia como apoyo.
        val wiki = Wikipedia.summary(clean)
        val support = wiki?.let {
            "Datos de Wikipedia: ${Wikipedia.shorten(it, 700)}\n\n"
        }.orEmpty()
        val fallbackTurn = "Haz un resumen en español de los puntos más importantes " +
            "sobre «$clean». $support" +
            "Usa titular, puntos con «• » y una frase final. " +
            "Si no tienes datos suficientes, dilo."
        val attempts = listOf(
            AiRequest(
                systemPrompt = AiPrompt.factualPrompt(basePrompt),
                turns = listOf(AiTurn(fromUser = true, text = fallbackTurn)),
                temperature = 0.2,
                model = alternative,
            ),
            AiRequest(
                systemPrompt = AiPrompt.factualPrompt(basePrompt),
                turns = listOf(AiTurn(fromUser = true, text = fallbackTurn)),
                temperature = 0.2,
                model = current,
            ),
        ).distinctBy { it.model }

        val searchErrors = listOfNotNull(
            primaryError.takeIf { it.isNotBlank() },
            (groundingRetry as? AiResult.Failure)?.message?.takeIf { it.isNotBlank() },
        ).joinToString(" | ").take(180)
        val note = if (searchErrors.isBlank()) {
            "(Nota: no he podido consultar la web en directo; " +
                "el resumen usa Wikipedia y lo que sé.)"
        } else {
            "(Nota: búsqueda en directo no disponible: $searchErrors. " +
                "El resumen usa Wikipedia y lo que sé.)"
        }
        for (attempt in attempts) {
            when (val result = ai.complete(attempt)) {
                is AiResult.Success -> return AssistantResponse("${result.text.trim()}\n\n$note")
                is AiResult.Failure -> Unit
            }
        }

        // Último recurso: resumen extractivo de lo que sí hemos podido leer.
        val wikiText = support.removePrefix("Datos de Wikipedia: ").trim()
        return if (wikiText.isNotBlank()) {
            AssistantResponse(
                "He encontrado esto en Wikipedia:\n\n$wikiText\n\n" +
                    "(La IA no está disponible ahora mismo; este es el resumen que he podido consultar.)"
            )
        } else {
            AssistantResponse(
                "No he podido buscar en internet, señor. " +
                    "La IA no está disponible ahora mismo. Inténtelo de nuevo en unos segundos."
            )
        }
    }

    /** Da formato al resumen encontrado y añade hasta tres fuentes web. */
    private fun formatSearchResult(result: AiResult.Success): AssistantResponse {
        val text = buildString {
            append(result.text.trim())
            if (result.sources.isNotEmpty()) {
                appendLine()
                appendLine()
                append("Fuentes:")
                result.sources.take(3).forEachIndexed { index, source ->
                    val host = source.url
                        .substringAfter("://")
                        .substringBefore("/")
                        .removePrefix("www.")
                    appendLine()
                    append("${index + 1}. ${source.title} — $host")
                }
            }
        }
        return AssistantResponse(text.trim())
    }

    /**
     * Llama a la IA y, si el modelo actual falla por un problema de servicio,
     * lo intenta una vez con el otro modelo gratuito. No reintenta errores de
     * clave, cuota ni bloqueo: esos no se arreglan cambiando de modelo.
     */
    private suspend fun completeAi(request: AiRequest): AiResult {
        val first = ai.complete(request)
        if (first is AiResult.Success) return first
        val message = (first as AiResult.Failure).message
        if (message.contains("clave", ignoreCase = true) ||
            message.contains("límite", ignoreCase = true) ||
            message.contains("no existe", ignoreCase = true) ||
            message.contains("bloqueado", ignoreCase = true)
        ) {
            return first
        }
        val current = ai.currentModel()
        val alternative = if (current == GeminiProvider.QUALITY_MODEL) {
            GeminiProvider.DEFAULT_MODEL
        } else {
            GeminiProvider.QUALITY_MODEL
        }
        return ai.complete(request.copy(model = alternative))
    }

    /**
     * Conversación libre con el modelo de lenguaje. Se envía la personalidad,
     * los recuerdos guardados y los últimos turnos; si no hay clave, se explica
     * cómo configurarla sin romper el resto del asistente.
     *
     * Para preguntas factuales no se envía el historial y se baja la
     * temperatura: así el modelo responde a lo preguntado y no mezcla temas.
     */
    private suspend fun chat(text: String, factual: Boolean = false): AssistantResponse {
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

        val basePrompt = JarvisPersona.systemPromptForToday()
        val request = if (factual) {
            // Pregunta factual: sin historial y con temperatura baja. El
            // historial puede contaminar la respuesta con el tema anterior.
            AiRequest(
                systemPrompt = AiPrompt.factualPrompt(basePrompt),
                turns = listOf(AiTurn(fromUser = true, text = clean)),
                temperature = 0.2,
            )
        } else {
            val history = runCatching { repository.recentMessages(AiPrompt.MAX_TURNS) }
                .getOrDefault(emptyList())
                .map { AiTurn(fromUser = it.fromUser, text = it.text) }
            val memories = runCatching { repository.observeMemories().first().map { it.text } }
                .getOrDefault(emptyList())
            AiRequest(
                systemPrompt = AiPrompt.systemPrompt(basePrompt, memories),
                turns = AiPrompt.turns(history, clean),
            )
        }

        return when (val result = completeAi(request)) {
            is AiResult.Success -> AssistantResponse(result.text)
            is AiResult.Failure -> AssistantResponse("No he podido consultar la IA. ${result.message}")
        }
    }

    /** «qué tengo hoy»: agenda del día con tareas, avisos y tiempo. */
    private suspend fun agendaToday(): AssistantResponse {
        val now = LocalDate.now()
        val todos = repository.observeTodos().first()
        val reminders = repository.observeReminders().first()
        val weather = weatherReport()?.let { WeatherText.now(it) }
        val text = Agenda.summary(
            todos = todos,
            reminders = reminders,
            now = now,
            weatherLine = weather,
            batteryLine = batteryText(),
        )
        return AssistantResponse(text)
    }

    /** «crea una rutina: cuando suene el temporizador de los huevos, apaga la música» */
    private suspend fun createRoutine(payload: String): AssistantResponse {
        val spec = RoutineParser.parse(payload) ?: return AssistantResponse(
            "Dígame la rutina así: «cuando suene el temporizador de los huevos, apaga la música» " +
                "o «cada mañana a las 8, recuérdame el café»."
        )
        val item = RoutineItem(
            trigger = spec.trigger.name,
            triggerLabel = spec.triggerLabel,
            action = spec.action.name,
            payload = spec.payload,
        )
        val id = repository.addRoutine(item)
        if (spec.trigger == RoutineTrigger.HOURLY) {
            routineScheduler.scheduleHourly(id, spec.hour, spec.minute)
        }
        JarvisWidgetProvider.updateAll(context)
        return AssistantResponse("Rutina creada: ${RoutinePlan.describe(item)}")
    }

    /** «qué rutinas tengo» */
    private suspend fun listRoutines(): AssistantResponse {
        val routines = repository.observeRoutines().first()
        if (routines.isEmpty()) {
            return AssistantResponse("No tiene rutinas creadas, señor.")
        }
        val list = routines.take(4).joinToString(" · ") { RoutinePlan.describe(it) }
        val more = if (routines.size > 4) " Y ${routines.size - 4} más." else ""
        return AssistantResponse("Tiene ${routines.size} rutina(s): $list.$more")
    }

    /** «borra la rutina de los huevos» */
    private suspend fun deleteRoutine(payload: String): AssistantResponse {
        val target = payload.trim()
        val routines = repository.observeRoutines().first()
        val match = routines.lastOrNull { routine ->
            target.isBlank() || routine.triggerLabel.contains(target, ignoreCase = true)
        }
        if (match == null) {
            return AssistantResponse("No encuentro ninguna rutina con ese nombre.")
        }
        routineScheduler.cancel(match.id)
        repository.deleteRoutine(match)
        JarvisWidgetProvider.updateAll(context)
        return AssistantResponse("Rutina borrada: ${RoutinePlan.describe(match)}")
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
        val songs = tracks.map { it.toSong() }

        val request = MusicRequestParser.parse(payload)
        if (request == null) {
            // No se ha entendido la búsqueda: se pone música normal.
            val track = tracks.random()
            player.play(track)
            return AssistantResponse("Reproduciendo ${track.title}, de ${track.artist}.")
        }

        val best = MusicSearch.search(songs, request).firstOrNull()
        if (best != null) {
            val track = tracks.firstOrNull { it.id == best.song.id }
            if (track != null) {
                player.play(track)
                return AssistantResponse("Reproduciendo ${track.title}, de ${track.artist}.")
            }
        }

        val chosen = askAiForTrack(request, songs)
        if (chosen != null) {
            val track = tracks.firstOrNull { it.id == chosen.id }
            if (track != null) {
                player.play(track)
                return AssistantResponse("He elegido ${track.title}, de ${track.artist}.")
            }
        }

        return AssistantResponse(
            "No encuentro nada ${request.label} en su biblioteca. " +
                "Pruebe con «pon algo de rock», «música para estudiar» o el nombre de un artista."
        )
    }

    /** Le pregunta a la IA qué artista de la biblioteca encaja con la petición. */
    private suspend fun askAiForTrack(request: MusicRequest, songs: List<Song>): Song? {
        if (!ai.isConfigured()) return null
        val artists = MusicSearch.artists(songs)
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
        return songs.firstOrNull {
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

    /** «despiértame a las 7» / «pon una alarma mañana a las 6:30» */
    private suspend fun createAlarm(payload: String): AssistantResponse {
        val spec = AlarmParser.parse(payload) ?: return AssistantResponse(
            "Dígame la hora de la alarma, por ejemplo «despiértame a las 7» o «alarma mañana a las 6:30»."
        )
        val item = AlarmItem(
            hour = spec.hour,
            minute = spec.minute,
            label = spec.label,
            repeatDaily = spec.repeatDaily,
            enabled = true,
            triggerAtMillis = spec.triggerAtMillis,
        )
        val id = repository.addAlarm(item)
        alarmScheduler.schedule(id, spec.triggerAtMillis)
        JarvisWidgetProvider.updateAll(context)
        val whenText = if (spec.repeatDaily) {
            "cada día a las ${String.format("%02d:%02d", spec.hour, spec.minute)}"
        } else {
            formatMoment(spec.triggerAtMillis)
        }
        val label = spec.label.takeIf { it.isNotBlank() }?.let { " ($it)" } ?: ""
        return AssistantResponse("Alarma programada para $whenText$label.")
    }

    /** «qué alarmas tengo» */
    private suspend fun listAlarms(): AssistantResponse {
        val alarms = repository.observeAlarms().first().filter { it.enabled }
        if (alarms.isEmpty()) return AssistantResponse("No tiene alarmas activas, señor.")
        val list = alarms.take(5).joinToString(" · ") { alarm ->
            val time = String.format("%02d:%02d", alarm.hour, alarm.minute)
            val kind = if (alarm.repeatDaily) "diaria" else "una vez"
            val label = alarm.label.takeIf { it.isNotBlank() }?.let { " $it" } ?: ""
            "$time ($kind)$label"
        }
        val more = if (alarms.size > 5) " Y ${alarms.size - 5} más." else ""
        return AssistantResponse("Tiene ${alarms.size} alarma(s): $list.$more")
    }

    /** «borra la alarma de las 7» / «cancela la alarma» */
    private suspend fun deleteAlarm(payload: String): AssistantResponse {
        val alarms = repository.observeAlarms().first().filter { it.enabled }
        if (alarms.isEmpty()) return AssistantResponse("No hay alarmas activas que borrar.")
        val target = payload.trim()
        val match = when {
            target.isBlank() -> alarms.firstOrNull()
            Regex("\\b(\\d{1,2})(?::(\\d{2}))?\\b").find(target) != null -> {
                val m = Regex("\\b(\\d{1,2})(?::(\\d{2}))?\\b").find(target)!!
                val h = m.groupValues[1].toIntOrNull() ?: -1
                val min = m.groupValues[2].toIntOrNull() ?: 0
                alarms.firstOrNull { it.hour == h && it.minute == min } ?: alarms.lastOrNull()
            }
            else -> alarms.firstOrNull { alarm ->
                alarm.label.contains(target, ignoreCase = true)
            } ?: alarms.lastOrNull()
        } ?: return AssistantResponse("No encuentro esa alarma.")
        alarmScheduler.cancel(match.id)
        repository.deleteAlarm(match)
        JarvisWidgetProvider.updateAll(context)
        val time = String.format("%02d:%02d", match.hour, match.minute)
        return AssistantResponse("Alarma de las $time eliminada.")
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
            "subir o bajar el volumen, calcular expresiones, convertir unidades, poner temporizadores, " +
            "recordatorios y alarmas, listar y cancelar recordatorios o alarmas, consultar el tiempo y su previsión, " +
            "añadir tareas, memorizar notas, " +
            "decir la hora y la fecha, informar de la " +
            "batería y del almacenamiento, encender la linterna, abrir aplicaciones, contar un chiste, " +
            "recordar lo que hemos hablado o darme un resumen. " +
            "Si configura una clave gratuita de Gemini en Ajustes, también puedo conversar, explicar " +
            "temas, buscar en internet y resumir lo más importante de cualquier asunto. " +
            "Inténtelo con: «pon música», «quita la música», «pon un temporizador de 10 minutos», " +
            "«recuérdame comprar pan a las 6 de la tarde», «despiértame a las 7», «qué alarmas tengo», " +
            "«qué tiempo hace», «qué recordatorios tengo», «calcula 12 * (3 + 4)», " +
            "«explícame la fotosíntesis» o «busca en internet la fotosíntesis y resúmenmela». " +
            "Si algo falla, diga «diagnóstico de red»."

    /**
     * Prueba rápida de red para distinguir entre «sin internet», «la app no
     * tiene permiso de datos» y «el servicio de IA está caído».
     */
    private suspend fun networkTest(): AssistantResponse = withContext(Dispatchers.IO) {
        runCatching {
            val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = manager.activeNetwork
            val capabilities = network?.let { manager.getNetworkCapabilities(it) }
            val transport = when {
                capabilities == null -> "sin red activa"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "datos móviles"
                else -> "otro tipo de red"
            }
            val hasInternet = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            val lines = mutableListOf(
                "Red: $transport (acceso a internet: ${if (hasInternet) "sí" else "no"})",
            )

            fun probe(name: String, url: String) {
                val result = runCatching {
                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.connectTimeout = 8000
                    connection.readTimeout = 8000
                    connection.requestMethod = "GET"
                    val code = connection.responseCode
                    connection.disconnect()
                    "HTTP $code"
                }.getOrElse { "${it.javaClass.simpleName}: ${it.message.orEmpty()}" }
                lines += "$name: $result"
            }

            probe("Wikipedia", "https://es.wikipedia.org/api/rest_v1/page/summary/Gatos")
            probe("Gemini", "https://generativelanguage.googleapis.com/v1beta/models")
            val wikiFailed = lines.any {
                it.startsWith("Wikipedia:") && !it.contains("HTTP 2")
            }
            val text = buildString {
                append("Diagnóstico de red:\n")
                lines.forEach { appendLine(it) }
                if (wikiFailed) {
                    appendLine()
                    append("Wikipedia falla aquí: revise Ahorro de datos o las " +
                        "restricciones de datos de JARVIS en Ajustes del teléfono.")
                }
            }
            AssistantResponse(text)
        }.getOrElse { AssistantResponse("No he podido hacer el diagnóstico: ${it.message}") }
    }

    // --- Conversación larga: entender continuaciones ------------------------

    /** Última intención resuelta, para interpretar frases como «y sube el volumen». */
    private var lastKind: IntentKind? = null

    /**
     * Marca del último turno conversacional. Durante unos minutos, las
     * preguntas de seguimiento usan el historial en lugar de consultar
     * Wikipedia como si fueran temas nuevos.
     */
    private var lastConversationAt: Long = 0L

    /** Vuelve al modo de órdenes: la próxima pregunta será factual. */
    fun resetConversation() {
        lastKind = null
        lastConversationAt = 0L
    }

    private fun conversationActive(): Boolean =
        lastConversationAt > 0L &&
            System.currentTimeMillis() - lastConversationAt < CONVERSATION_WINDOW_MS

    companion object {
        /** Mensajes máximos que se exportan al archivo de texto. */
        const val EXPORT_MAX_MESSAGES = 500

        /** Tiempo durante el que se recuerda que hay una conversación abierta. */
        private const val CONVERSATION_WINDOW_MS = 5 * 60_000L
    }

    private fun resolve(intent: ParsedIntent): ParsedIntent {
        if (intent.kind == IntentKind.UNKNOWN && isFollowUp(intent.raw)) {
            val previous = lastKind
            if (previous == IntentKind.CHAT || previous == IntentKind.KNOWLEDGE ||
                previous == IntentKind.WEB_SEARCH
            ) {
                lastKind = IntentKind.CHAT
                return intent.copy(kind = IntentKind.CHAT)
            }
            if (previous != null &&
                previous != IntentKind.UNKNOWN &&
                previous != IntentKind.GREETING &&
                previous != IntentKind.HELP
            ) {
                return intent.copy(kind = previous)
            }
        }
        if (intent.kind != IntentKind.UNKNOWN) {
            lastKind = intent.kind
        }
        return intent
    }

    private fun isConversationalQuestion(text: String): Boolean {
        val n = IntentParser.normalize(text)
        val markers = listOf(
            "que opinas", "que piensas", "que crees", "crees", "que sientes",
            "prefieres", "te gusta", "te gustan", "tu opinion", "tu punto de vista",
            "como te sientes", "quien eres", "hablame de ti", "cuentame de ti",
            "me ayudas", "puedes ayudarme", "tienes tiempo", "que tal", "como estas",
            "que haces", "en que piensas", "que te parece", "recomiendas", "deberia",
        )
        return markers.any { n.contains(it) }
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
