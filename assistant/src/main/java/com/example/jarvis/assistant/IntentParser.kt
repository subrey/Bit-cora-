package com.example.jarvis.assistant

import java.text.Normalizer

/** Intención detectada en una frase del usuario. */
enum class IntentKind {
    GREETING,
    CHAT,
    HELP,
    TIME,
    DATE,
    MUSIC_PLAY,
    MUSIC_SEARCH,
    MUSIC_PAUSE,
    ROUTINE_CREATE,
    ROUTINE_LIST,
    ROUTINE_DELETE,
    MUSIC_NEXT,
    MUSIC_PREVIOUS,
    MUSIC_STOP,
    MUSIC_SHUFFLE,
    MUSIC_REPEAT,
    VOLUME_UP,
    VOLUME_DOWN,
    CALCULATE,
    CONVERT,
    TIMER,
    REMINDER,
    LIST_REMINDERS,
    CANCEL_REMINDER,
    ALARM_CREATE,
    ALARM_LIST,
    ALARM_DELETE,
    TODO,
    MEMORY,
    LIST_MEMORIES,
    HISTORY,
    EXPORT_HISTORY,
    KNOWLEDGE,
    BATTERY,
    STORAGE,
    TORCH,
    OPEN_APP,
    JOKE,
    WEATHER,
    WEATHER_TOMORROW,
    WEATHER_ADVICE,
    SUMMARY,
    AGENDA_TODAY,
    UNKNOWN,
}

data class ParsedIntent(
    val kind: IntentKind,
    val payload: String = "",
    val raw: String = "",
)

/**
 * Interpreta frases en español (o inglés) y las convierte en intenciones.
 * Sin dependencias de Android: se puede probar en tests unitarios.
 */
object IntentParser {

    fun normalize(text: String): String {
        val lower = text.lowercase()
        val withoutAccents = Normalizer.normalize(lower, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        return withoutAccents
            .replace("[^\\p{L}\\p{N}\\s:,.+*/()%?\\-]".toRegex(), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun parse(text: String): ParsedIntent {
        val raw = text.trim()
        val n = normalize(raw)
        if (n.isBlank()) return ParsedIntent(IntentKind.UNKNOWN, raw = raw)

        fun has(vararg words: String) = words.any { n.contains(it) }

        // --- Ayuda y saludo -----------------------------------------------
        if (has("que sabes hacer", "que puedes hacer", "lista de comandos", "que comandos") ||
            n == "ayuda" || n.startsWith("ayuda ") ||
            n.startsWith("necesito ayuda") || n.startsWith("dame ayuda") ||
            n.startsWith("muestra ayuda") || n == "comandos"
        ) {
            return ParsedIntent(IntentKind.HELP, raw = raw)
        }
        if (isPureGreeting(n)) {
            return ParsedIntent(IntentKind.GREETING, raw = raw)
        }
        if (has(
                "como estas", "que tal estas", "como te va", "que haces",
                "en que piensas", "hablemos", "conversemos",
            )
        ) {
            return ParsedIntent(IntentKind.CHAT, raw = raw)
        }

        // --- Reloj -------------------------------------------------------
        if (has("que hora es", "la hora", "dime la hora", "hora es ahora")) {
            return ParsedIntent(IntentKind.TIME, raw = raw)
        }
        if (has("que dia es", "que fecha", "fecha de hoy", "dia de hoy")) {
            return ParsedIntent(IntentKind.DATE, raw = raw)
        }

        // --- Rutinas (antes que la música: «cuando suene…, apaga la música») ---
        if (has(
                "mis rutinas", "que rutinas tengo", "lista mis rutinas", "rutinas activas",
                "que hay programado", "mis automatizaciones",
            )
        ) {
            return ParsedIntent(IntentKind.ROUTINE_LIST, raw = raw)
        }
        if (has(
                "borra la rutina", "elimina la rutina", "quita la rutina",
                "cancela la rutina", "desactiva la rutina",
            )
        ) {
            val body = n.substringAfter("rutina", "").trim()
            return ParsedIntent(IntentKind.ROUTINE_DELETE, payload = body, raw = raw)
        }
        if (RoutineParser.looksLikeRoutine(n)) {
            return ParsedIntent(IntentKind.ROUTINE_CREATE, payload = n, raw = raw)
        }

        // --- Agenda del día (antes que las tareas: «mi agenda de hoy») ------
        if (has(
                "que tengo hoy", "que hay hoy", "mi agenda", "agenda de hoy",
                "que debo hacer hoy", "mi dia de hoy", "resumen del dia",
                "que me toca hoy", "mis cosas de hoy", "tareas de hoy",
                "que tareas tengo", "mis tareas", "lista mis tareas",
            )
        ) {
            return ParsedIntent(IntentKind.AGENDA_TODAY, raw = raw)
        }

        // --- Preguntas y charla antes que las órdenes ambiguas -------------
        // Definiciones: «qué es la música» o «qué es una tarea» no deben
        // activar el reproductor ni crear una tarea.
        if (n.startsWith("que es ") || n.startsWith("que son ") ||
            n.startsWith("quien es ") || n.startsWith("quienes son ") ||
            n.startsWith("que significa ") || n.startsWith("como funciona ") ||
            n.startsWith("como se ") || n.startsWith("de que ")
        ) {
            return ParsedIntent(IntentKind.KNOWLEDGE, payload = stripQuestion(n), raw = raw)
        }

        // Cualquier pregunta o frase de charla que no sea una consulta local
        // ni una orden clara se envía a la IA, no a la música o al volumen.
        if (looksLikeQuestionOrChat(n) &&
            !looksLikeLocalQuery(n) &&
            !looksLikeClearCommand(n)
        ) {
            return ParsedIntent(
                kind = if (hasChatMarker(n) || isTopicStatement(n)) {
                    IntentKind.CHAT
                } else {
                    IntentKind.KNOWLEDGE
                },
                payload = stripQuestion(n),
                raw = raw,
            )
        }

        // --- Música ------------------------------------------------------
        // Una búsqueda concreta («algo de rock», «para estudiar», «de Coldplay»)
        // tiene prioridad sobre el simple «pon música».
        if (has("musica para", "cancion para", "algo para", "música para", "canciones para")) {
            val body = n.substringAfter("para", "").trim().trim('?', '¿', '.', '!')
            return ParsedIntent(IntentKind.MUSIC_SEARCH, payload = body.ifBlank { n }, raw = raw)
        }
        if (has("algo de", "algo del", "musica de", "música de", "canciones de", "cancion de")) {
            val body = n.substringAfter("algo de", "")
                .ifBlank { n.substringAfter("algo del", "") }
                .ifBlank { n.substringAfter("musica de", "") }
                .ifBlank { n.substringAfter("música de", "") }
                .ifBlank { n.substringAfter("canciones de", "") }
                .ifBlank { n.substringAfter("cancion de", "") }
                .trim()
                .trim('?', '¿', '.', '!')
            return ParsedIntent(IntentKind.MUSIC_SEARCH, payload = body.ifBlank { n }, raw = raw)
        }
        if (has(
                "sube volumen", "sube el volumen", "subir volumen", "subir el volumen",
                "aumenta el volumen", "aumentar el volumen", "volumen arriba", "mas alto", "sube",
            )
        ) {
            return ParsedIntent(IntentKind.VOLUME_UP, raw = raw)
        }
        if (has(
                "baja volumen", "baja el volumen", "bajar volumen", "bajar el volumen",
                "reduce el volumen", "reducir el volumen", "volumen abajo", "mas bajo", "baja",
                "silencia el volumen",
            )
        ) {
            return ParsedIntent(IntentKind.VOLUME_DOWN, raw = raw)
        }
        if (has(
                "pausa", "pause", "para la musica", "para la cancion",
                "parar la musica", "parar la cancion",
                "detener la musica", "detener la cancion",
            )
        ) {
            return ParsedIntent(IntentKind.MUSIC_PAUSE, raw = raw)
        }
        if (has("siguiente", "next", "cambia de cancion", "otra cancion", "saltar adelante")) {
            return ParsedIntent(IntentKind.MUSIC_NEXT, raw = raw)
        }
        if (has("anterior", "prev", "cancion anterior", "atras", "saltar atras")) {
            return ParsedIntent(IntentKind.MUSIC_PREVIOUS, raw = raw)
        }
        if (has("repite", "repeat", "otra vez la cancion")) {
            return ParsedIntent(IntentKind.MUSIC_REPEAT, raw = raw)
        }
        if (has("aleatoria", "shuffle", "mezcla", "al azar", "cambia la musica")) {
            return ParsedIntent(IntentKind.MUSIC_SHUFFLE, raw = raw)
        }
        if (has(
                "deten", "stop", "apaga la musica", "apaga el sonido", "corta la musica",
                "quita la musica", "quitar la musica", "quita el sonido", "quitar el sonido",
                "silencia la musica", "silenciar la musica", "silencia el sonido",
                "no mas musica", "no quiero musica", "sin musica", "termina la musica",
                "fin de la musica", "para la musica", "para la cancion", "apaga", "silencia",
            )
        ) {
            return ParsedIntent(IntentKind.MUSIC_STOP, raw = raw)
        }
        if (has(
                "pon musica", "ponme musica", "reproduce", "play", "pon una cancion",
                "musica", "cancion", "suena",
            )
        ) {
            return ParsedIntent(IntentKind.MUSIC_PLAY, raw = raw)
        }

        // --- Utilidades ---------------------------------------------------
        if (has("calcula ", "calcular", "cuanto es ", "resultado de") ||
            n == "calcula" || n == "cuanto es"
        ) {
            val expression = n.substringAfter("calcula ", "")
                .ifBlank { n.substringAfter("calcular", "") }
                .ifBlank { n.substringAfter("cuanto es ", "") }
                .ifBlank { n.substringAfter("resultado de", "") }
                .trim()
                .trimEnd('?', '¿', '.', '!')
            return ParsedIntent(IntentKind.CALCULATE, payload = expression, raw = raw)
        }
        if (has("convierte", "convertir", "equivale a")) {
            val body = n.substringAfter("convierte", "")
                .ifBlank { n.substringAfter("convertir", "") }
                .trim()
                .trimEnd('?', '¿', '.', '!')
            return ParsedIntent(IntentKind.CONVERT, payload = body, raw = raw)
        }

        // --- Alarmas (despertador) ----------------------------------------
        if (has("que alarmas", "mis alarmas", "lista mis alarmas", "alarmas puestas", "alarmas activas")) {
            return ParsedIntent(IntentKind.ALARM_LIST, raw = raw)
        }
        if (has("borra la alarma", "elimina la alarma", "quita la alarma", "cancela la alarma", "apaga la alarma", "desactiva la alarma")) {
            val body = n.substringAfter("alarma", "").trim()
            return ParsedIntent(IntentKind.ALARM_DELETE, payload = body, raw = raw)
        }
        if (AlarmParser.looksLikeAlarm(n)) {
            return ParsedIntent(IntentKind.ALARM_CREATE, payload = n, raw = raw)
        }

        // --- Temporizadores y recordatorios -------------------------------
        if (has(
                "que recordatorios", "mis recordatorios", "lista mis recordatorios",
                "que avisos tengo", "mis avisos", "que alertas tengo", "recordatorios pendientes",
            )
        ) {
            return ParsedIntent(IntentKind.LIST_REMINDERS, raw = raw)
        }
        if (has(
                "cancela el recordatorio", "cancela recordatorio", "borra el recordatorio",
                "borra recordatorio", "elimina el recordatorio", "quita el recordatorio",
                "anula el recordatorio", "anula recordatorio",
            )
        ) {
            val body = n.substringAfter("recordatorio", "").trim()
            return ParsedIntent(IntentKind.CANCEL_REMINDER, payload = body, raw = raw)
        }
        if (has(
                "recuerdame", "recordarme", "me recuerdas", "pon un recordatorio", "pon recordatorio",
                "ponme un recordatorio", "crea un recordatorio", "nuevo recordatorio",
                "pon un aviso", "recuerdame de",
            )
        ) {
            val body = n.substringAfter("recuerdame", "")
                .ifBlank { n.substringAfter("recordarme", "") }
                .ifBlank { n.substringAfter("me recuerdas", "") }
                .ifBlank { n.substringAfter("recordatorio", "") }
                .ifBlank { n.substringAfter("aviso", "") }
                .trim()
                .trim('?', '¿', '.', '!')
            return ParsedIntent(IntentKind.REMINDER, payload = body.ifBlank { n }, raw = raw)
        }
        if (has("temporizador", "avisame en", "avisa en")) {
            return ParsedIntent(IntentKind.TIMER, payload = n, raw = raw)
        }

        // --- Tareas y memorias --------------------------------------------
        if (has(
                "exporta el historial", "exportar historial", "guarda el historial",
                "guardar historial", "comparte el historial", "comparte el historial",
                "descarga el historial", "exporta la conversacion", "guarda la conversacion",
            )
        ) {
            return ParsedIntent(IntentKind.EXPORT_HISTORY, raw = raw)
        }
        if (has(
                "que notas", "mis notas", "mis recuerdos", "que has memorizado",
            )
        ) {
            return ParsedIntent(IntentKind.LIST_MEMORIES, raw = raw)
        }
        if (has(
                "que dijiste", "que he dicho", "que hemos hablado", "que hablamos",
                "historial", "nuestra conversacion", "resume la conversacion",
                "que me dijiste",
            )
        ) {
            return ParsedIntent(IntentKind.HISTORY, raw = raw)
        }
        if (has(
                "nueva tarea", "anade tarea", "anadir tarea", "anadir una tarea",
                "agenda", "apunta tarea", "crea tarea", "tarea",
            )
        ) {
            val body = n.substringAfter("tarea", "")
                .trim()
                .trim('?', '¿', '.', '!')
            if (body.isBlank()) {
                return ParsedIntent(IntentKind.CHAT, raw = raw)
            }
            return ParsedIntent(IntentKind.TODO, payload = body, raw = raw)
        }
        if (has(
                "memoriza", "memorizar", "recuerda que", "apunta que",
                "guarda esta nota", "apunta", "nota",
            )
        ) {
            val body = n.substringAfter("recuerda que", "")
                .ifBlank { n.substringAfter("apunta que", "") }
                .ifBlank { n.substringAfter("memorizar", "") }
                .ifBlank { n.substringAfter("memoriza", "") }
                .ifBlank { n.substringAfter("apunta", "") }
                .ifBlank { n.substringAfter("nota", "") }
                .trim()
                .trim('?', '¿', '.', '!')
            if (body.isBlank() || body in setOf("esto", "eso", "aquello")) {
                return ParsedIntent(IntentKind.CHAT, raw = raw)
            }
            return ParsedIntent(IntentKind.MEMORY, payload = body, raw = raw)
        }

        // --- Estado del dispositivo ---------------------------------------
        if (has("bateria", "cuanta bateria", "carga")) {
            return ParsedIntent(IntentKind.BATTERY, raw = raw)
        }
        if (has("almacenamiento", "espacio libre", "cuanto espacio", "memoria del telefono")) {
            return ParsedIntent(IntentKind.STORAGE, raw = raw)
        }
        if (has("linterna", "flash")) {
            return ParsedIntent(IntentKind.TORCH, raw = raw)
        }
        if (has(
                "que tiempo hace manana", "tiempo de manana", "el tiempo manana",
                "prevision de manana", "que tiempo tendra manana", "mañana llovera",
            )
        ) {
            return ParsedIntent(IntentKind.WEATHER_TOMORROW, raw = raw)
        }
        if (has(
                "debo llevar paraguas", "necesito un paraguas", "llevo paraguas",
                "debo salir", "puedo salir", "hace frio", "hace calor", "va a llover",
                "va a hacer frio", "consejo del tiempo", "como me visto hoy",
            )
        ) {
            return ParsedIntent(IntentKind.WEATHER_ADVICE, raw = raw)
        }
        if (has(
                "clima", "que tiempo hace", "temperatura exterior", "el tiempo",
                "que temperatura", "cuanta calor hace", "tiempo en la ciudad",
            )
        ) {
            return ParsedIntent(IntentKind.WEATHER, raw = raw)
        }
        if (has("resumen", "estado general", "informe")) {
            return ParsedIntent(IntentKind.SUMMARY, raw = raw)
        }
        if (has(
                "cuenta un chiste", "cuentame un chiste", "cuentame otro chiste",
                "me cuentas un chiste", "me cuentas otro chiste",
                "contarme un chiste", "contarme otro chiste",
                "dime un chiste", "dime otro chiste", "otro chiste",
                "algo gracioso", "entreteneme",
            )
        ) {
            return ParsedIntent(IntentKind.JOKE, raw = raw)
        }
        if (has(
                "abre ", "abrir ", "abres ", "abrirme ",
                "ejecuta ", "ejecutar ", "lanza ", "lanzar ",
            )
        ) {
            val body = n.substringAfter("abre ", "")
                .ifBlank { n.substringAfter("abrir ", "") }
                .ifBlank { n.substringAfter("abres ", "") }
                .ifBlank { n.substringAfter("abrirme ", "") }
                .ifBlank { n.substringAfter("ejecuta ", "") }
                .ifBlank { n.substringAfter("ejecutar ", "") }
                .ifBlank { n.substringAfter("lanza ", "") }
                .ifBlank { n.substringAfter("lanzar ", "") }
                .trim()
                .trim('?', '¿', '.', '!')
            return ParsedIntent(IntentKind.OPEN_APP, payload = body, raw = raw)
        }

        // --- Preguntas y conocimiento general ------------------------------
        if (has(
                "que sabes", "sabes de", "sabes sobre", "dime sobre", "dime de", "dime que",
                "cuéntame", "cuentame", "hablame de", "hablame sobre", "explicame",
                "informame de", "info de", "de que va", "que es", "quien es", "que significa",
                "de donde viene", "cual es el", "cuales son", "por que", "por qué",
            )
        ) {
            val subject = stripQuestion(n)
            return ParsedIntent(IntentKind.KNOWLEDGE, payload = subject, raw = raw)
        }

        // Cualquier otra frase con interrogación se trata como pregunta.
        if (n.contains("?") || n.startsWith("que ") || n.startsWith("quien ") ||
            n.startsWith("cuando ") || n.startsWith("donde ") || n.startsWith("como ") ||
            n.startsWith("cual ")
        ) {
            return ParsedIntent(IntentKind.KNOWLEDGE, payload = stripQuestion(n), raw = raw)
        }

        return ParsedIntent(IntentKind.UNKNOWN, raw = raw)
    }

    /** Saludos puros; si traen una orden o una pregunta, no son solo un saludo. */
    private fun isPureGreeting(n: String): Boolean {
        val compact = n.replace(",", "").replace("!", "").replace(".", "").trim()
        return compact == "hola" || compact == "hola jarvis" ||
            compact == "buenos dias" || compact == "buenos dias jarvis" ||
            compact == "buenas tardes" || compact == "buenas tardes jarvis" ||
            compact == "buenas noches" || compact == "buenas noches jarvis" ||
            compact == "hey jarvis"
    }

    /** Preguntas, peticiones de explicación y frases de charla. */
    private fun looksLikeQuestionOrChat(n: String): Boolean {
        if (n.contains("?")) return true
        if (n.startsWith("que ") || n.startsWith("quien ") ||
            n.startsWith("cuando ") || n.startsWith("donde ") ||
            n.startsWith("como ") || n.startsWith("cual ") ||
            n.startsWith("cuanto ") || n.startsWith("por que ") ||
            n.startsWith("para que ")
        ) {
            return true
        }
        if (!n.contains("chiste") &&
            (n.startsWith("hablame") || n.startsWith("cuentame") ||
                n.startsWith("explicame") || n.startsWith("dime sobre") ||
                n.startsWith("dime de") || n.startsWith("dime que") ||
                n.startsWith("sabes de") || n.startsWith("sabes sobre") ||
                n.startsWith("informame") || n.startsWith("de que va"))
        ) {
            return true
        }
        if (hasChatMarker(n)) return true
        return isTopicStatement(n)
    }

    /** Una frase como «la música es genial» o «el volumen está alto». */
    private fun isTopicStatement(n: String): Boolean {
        if (n.contains("?")) return false
        if (n.startsWith("que ") || n.startsWith("quien ") ||
            n.startsWith("cuando ") || n.startsWith("donde ") ||
            n.startsWith("como ") || n.startsWith("cual ") ||
            n.startsWith("cuanto ") || n.startsWith("por que ")
        ) {
            return false
        }
        return (n.contains(" es ") || n.contains(" son ") ||
            n.contains(" esta ") || n.contains(" estan ")) &&
            listOf(
                "musica", "cancion", "volumen", "pausa", "siguiente", "anterior",
                "mezcla", "apaga", "linterna", "bateria", "tarea", "alarma",
                "recordatorio", "temporizador", "nota", "historial", "resumen",
                "chiste", "tiempo", "clima", "temperatura",
            ).any { n.contains(it) }
    }

    /** Frases que piden opinión, charla o información sobre el propio JARVIS. */
    private fun hasChatMarker(n: String): Boolean {
        val markers = listOf(
            "que opinas", "que piensas", "que crees", "crees", "que sientes",
            "que te parece", "te gusta", "te gustan", "me gusta", "me encanta",
            "opino", "creo que", "me siento", "quiero hablar", "hablemos de",
            "como estas", "que tal", "como te va", "que haces", "en que piensas",
            "quien eres", "como te llamas", "tu nombre", "quien te creo",
            "quien te hizo", "que eres", "donde vives", "tienes sentimientos",
            "estas vivo", "en que puedes ayudarme", "me ayudas", "puedes ayudarme",
            "tienes tiempo", "recomiendas", "deberia", "cuentame de ti", "hablame de ti",
        )
        return markers.any { n.contains(it) }
    }

    /** Consultas que el asistente resuelve localmente sin pasar por la IA. */
    private fun looksLikeLocalQuery(n: String): Boolean {
        val phrases = listOf(
            "que hora", "la hora", "dime la hora", "hora es ahora",
            "que dia", "que fecha", "fecha de hoy", "dia de hoy",
            "mis rutinas", "que rutinas tengo", "lista mis rutinas", "rutinas activas",
            "que hay programado", "mis automatizaciones",
            "borra la rutina", "elimina la rutina", "quita la rutina",
            "cancela la rutina", "desactiva la rutina",
            "que tengo hoy", "que hay hoy", "mi agenda", "agenda de hoy",
            "que debo hacer hoy", "mi dia de hoy", "resumen del dia",
            "que me toca hoy", "mis cosas de hoy", "tareas de hoy",
            "que tareas tengo", "mis tareas", "lista mis tareas",
            "que alarmas", "mis alarmas", "lista mis alarmas", "alarmas puestas", "alarmas activas",
            "borra la alarma", "elimina la alarma", "quita la alarma",
            "cancela la alarma", "apaga la alarma", "desactiva la alarma",
            "que recordatorios", "mis recordatorios", "lista mis recordatorios",
            "que avisos tengo", "mis avisos", "que alertas tengo", "recordatorios pendientes",
            "cancela el recordatorio", "cancela recordatorio", "borra el recordatorio",
            "borra recordatorio", "elimina el recordatorio", "quita el recordatorio",
            "anula el recordatorio", "anula recordatorio",
            "pon un temporizador", "poner un temporizador", "temporizador de",
            "avisame en", "avisa en",
            "que notas tengo", "mis notas", "mis recuerdos", "que has memorizado",
            "que dijiste", "que he dicho", "que hemos hablado", "que hablamos",
            "que me dijiste", "nuestra conversacion", "resume la conversacion",
            "dame el historial", "muestra el historial",
            "cuanta bateria", "bateria tengo", "nivel de bateria", "carga del telefono",
            "estado de la bateria", "como esta la bateria",
            "cuanto almacenamiento", "espacio libre", "cuanto espacio", "memoria del telefono",
            "enciende la linterna", "encender la linterna", "apaga la linterna",
            "apagar la linterna", "prende la linterna", "prender la linterna",
            "que tiempo hace manana", "tiempo de manana", "el tiempo manana",
            "prevision de manana", "que tiempo tendra manana", "manana llovera",
            "debo llevar paraguas", "necesito un paraguas", "llevo paraguas",
            "debo salir", "puedo salir", "hace frio", "hace calor", "va a llover",
            "va a hacer frio", "consejo del tiempo", "como me visto hoy",
            "que tiempo hace", "temperatura exterior", "que temperatura",
            "cuanta calor hace", "tiempo en la ciudad",
            "como esta el tiempo", "el tiempo en", "el tiempo esta",
            "como esta el clima", "que clima hace", "clima de hoy", "el clima en",
            "dame un resumen", "haz un resumen", "me das un resumen",
            "dame el resumen", "hazme un resumen", "estado general", "informe",
            "cuentame un chiste", "cuenta un chiste", "cuentas un chiste",
            "cuentame otro chiste", "me cuentas un chiste", "me cuentas otro chiste",
            "contarme un chiste", "contarme otro chiste", "puedes contarme un chiste",
            "dime un chiste", "dime otro chiste", "otro chiste",
            "algo gracioso", "entreteneme",
            "que sabes hacer", "que puedes hacer", "lista de comandos", "que comandos",
        )
        if (phrases.any { n.contains(it) }) return true
        if ((n == "cuanto es" || n.contains("cuanto es ")) && n.any { it.isDigit() }) return true
        if (n == "calcula" || n.startsWith("calcula ") || n.contains("calcular") ||
            n.contains("resultado de")
        ) {
            return true
        }
        if (n.contains("convierte") || n.contains("convertir") || n.contains("equivale a")) return true
        return false
    }

    /** Órdenes inequívocas, incluso cuando se piden con «¿puedes…?». */
    private fun looksLikeClearCommand(n: String): Boolean {
        if (n.contains("calculadora")) return false
        val phrases = listOf(
            "pon musica", "ponme musica", "poner musica", "pon una cancion",
            "poner una cancion", "pon la cancion", "poner la cancion",
            "pon la musica", "poner la musica", "me pones la musica",
            "me pones musica", "me pones una cancion",
            "pon algo de", "poner algo de", "ponme algo de", "me pones algo de",
            "reproduce musica", "reproduce una cancion",
            "reproduce algo", "quiero escuchar", "quiero oir", "quiero musica",
            "sube el volumen", "sube volumen", "subir el volumen", "subir volumen",
            "me subes el volumen", "me subes volumen", "subeme el volumen", "subeme volumen",
            "volumen arriba", "baja el volumen", "baja volumen", "bajar el volumen",
            "bajar volumen", "me bajas el volumen", "me bajas volumen",
            "bajame el volumen", "bajame volumen", "volumen abajo", "silencia el volumen",
            "pausa la musica", "pausar la musica", "pausa la cancion", "pausar la cancion",
            "puedes pausar", "puedes parar la musica", "puedes parar la cancion",
            "parar la musica", "parar la cancion", "detener la musica", "detener la cancion",
            "para la musica", "para la cancion", "cambia de cancion",
            "pon otra cancion", "poner otra cancion", "saltar adelante",
            "pon la cancion anterior", "poner la cancion anterior", "saltar atras",
            "otra vez la cancion", "pon aleatoria", "poner aleatoria",
            "modo aleatorio", "reproduce aleatoria", "cambia la musica",
            "apaga la musica", "apagar la musica", "apaga el sonido", "apagar el sonido",
            "corta la musica", "cortar la musica", "quita la musica", "quitar la musica",
            "quita el sonido", "quitar el sonido", "silencia la musica", "silenciar la musica",
            "silencia el sonido", "no mas musica", "no quiero musica", "sin musica",
            "termina la musica", "fin de la musica",
            "calcula", "calcular", "resultado de", "convierte", "convertir", "equivale a",
            "enciende la linterna", "encender la linterna", "apaga la linterna",
            "apagar la linterna", "prende la linterna", "prender la linterna",
            "linterna encendida", "linterna apagada",
            "pon una alarma", "poner una alarma", "pon la alarma", "poner la alarma",
            "crea una alarma", "crear una alarma", "despiertame", "despertarme",
            "me despiertas", "me pones una alarma",
            "pon un temporizador", "poner un temporizador",
            "pon el temporizador", "poner el temporizador", "me pones un temporizador",
            "recuerdame", "recordarme", "me recuerdas",
            "pon un recordatorio", "poner un recordatorio",
            "ponerme un recordatorio", "crea un recordatorio", "crear un recordatorio",
            "anade tarea", "anadir tarea", "anadir una tarea", "nueva tarea",
            "crea tarea", "crear tarea", "memoriza", "memorizar", "recuerda que",
            "apunta que", "guarda esta nota",
            "abre ", "abrir ", "abres ", "abrirme ",
            "ejecuta ", "ejecutar ", "lanza ", "lanzar ",
        )
        return phrases.any { n.contains(it) }
    }

    /** Quita las frases de arranque para quedarse con el asunto de la pregunta. */
    private fun stripQuestion(text: String): String {
        val prefixes = listOf(
            "dime que sabes de", "dime que sabes sobre", "que sabes de", "que sabes sobre",
            "explicame lo que es", "explicame lo que son", "dime lo que es", "dime lo que son",
            "cuentame lo que es", "cuentame lo que son", "hablame de lo que es",
            "hablame de lo que son", "que es lo que es", "que es lo que son",
            "lo que es", "lo que son",
            "dime sobre", "dime de", "dime que es", "dime que son", "dime que", "dime",
            "sabes de", "sabes sobre", "cuentame sobre", "cuentame de", "cuentame",
            "hablame de", "hablame sobre", "hablame", "explicame", "informame de",
            "informame", "info de", "de que va", "que es", "que son", "quien es",
            "que significa", "de donde viene", "cual es", "cuales son", "por que", "que ",
        )
        var result = text
        prefixes.forEach { prefix ->
            if (result.startsWith(prefix)) {
                result = result.removePrefix(prefix).trim()
            }
        }
        return result.trim().trimEnd('?').trim()
    }
}
