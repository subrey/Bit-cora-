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
    MUSIC_PAUSE,
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
    TODO,
    MEMORY,
    LIST_MEMORIES,
    HISTORY,
    KNOWLEDGE,
    BATTERY,
    STORAGE,
    TORCH,
    OPEN_APP,
    JOKE,
    WEATHER,
    SUMMARY,
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
        if (has("que sabes hacer", "que puedes hacer", "lista de comandos", "ayuda", "comandos")) {
            return ParsedIntent(IntentKind.HELP, raw = raw)
        }
        if (n == "hola" || n.startsWith("hola ") || has("buenos dias", "buenas tardes", "buenas noches", "hey jarvis")) {
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

        // --- Música ------------------------------------------------------
        if (has("sube volumen", "sube el volumen", "volumen arriba", "mas alto", "sube")) {
            return ParsedIntent(IntentKind.VOLUME_UP, raw = raw)
        }
        if (has(
                "baja volumen", "baja el volumen", "volumen abajo", "mas bajo", "baja",
                "silencia el volumen",
            )
        ) {
            return ParsedIntent(IntentKind.VOLUME_DOWN, raw = raw)
        }
        if (has("pausa", "pause", "para la musica", "para la cancion", "para la canción")) {
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
        if (has("calcula", "cuanto es", "resultado de")) {
            val expression = n.substringAfter("calcula", "")
                .ifBlank { n.substringAfter("cuanto es", "").ifBlank { n.substringAfter("resultado de", "") } }
            return ParsedIntent(IntentKind.CALCULATE, payload = expression.trim(), raw = raw)
        }
        if (has("convierte", "convertir", "equivale a")) {
            val body = n.substringAfter("convierte", "")
                .ifBlank { n.substringAfter("convertir", "") }
            return ParsedIntent(IntentKind.CONVERT, payload = body.trim(), raw = raw)
        }

        // --- Temporizadores y recordatorios -------------------------------
        if (has("temporizador", "recordatorio", "avisame en", "avisa en", "pon alarma", "alarma")) {
            return ParsedIntent(IntentKind.TIMER, payload = n, raw = raw)
        }

        // --- Tareas y memorias --------------------------------------------
        if (has("que notas", "mis notas", "mis recuerdos", "que has memorizado")) {
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
        if (has("nueva tarea", "anade tarea", "agenda", "apunta tarea", "crea tarea", "tarea")) {
            val body = n.substringAfter("tarea", "").trim()
            return ParsedIntent(IntentKind.TODO, payload = body, raw = raw)
        }
        if (has("memoriza", "recuerda que", "apunta que", "guarda esta nota", "apunta", "nota")) {
            val body = n.substringAfter("recuerda que", "")
                .ifBlank { n.substringAfter("apunta que", "") }
                .ifBlank { n.substringAfter("memoriza", "") }
                .ifBlank { n.substringAfter("apunta", "") }
                .ifBlank { n.substringAfter("nota", "") }
            return ParsedIntent(IntentKind.MEMORY, payload = body.trim(), raw = raw)
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
        if (has("clima", "que tiempo hace", "temperatura exterior", "el tiempo")) {
            return ParsedIntent(IntentKind.WEATHER, raw = raw)
        }
        if (has("resumen", "estado general", "informe")) {
            return ParsedIntent(IntentKind.SUMMARY, raw = raw)
        }
        if (has("chiste", "cuenta un chiste", "algo gracioso", "entreteneme")) {
            return ParsedIntent(IntentKind.JOKE, raw = raw)
        }
        if (has("abre ", "abrir ", "ejecuta ", "lanza ")) {
            val body = n.substringAfter("abre ", "")
                .ifBlank { n.substringAfter("abrir ", "") }
                .ifBlank { n.substringAfter("ejecuta ", "") }
                .ifBlank { n.substringAfter("lanza ", "") }
            return ParsedIntent(IntentKind.OPEN_APP, payload = body.trim(), raw = raw)
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

    /** Quita las frases de arranque para quedarse con el asunto de la pregunta. */
    private fun stripQuestion(text: String): String {
        val prefixes = listOf(
            "dime que sabes de", "dime que sabes sobre", "que sabes de", "que sabes sobre",
            "dime sobre", "dime de", "dime que", "dime", "sabes de", "sabes sobre",
            "cuentame sobre", "cuentame de", "cuentame", "hablame de", "hablame sobre",
            "hablame", "explicame", "informame de", "informame", "info de",
            "de que va", "que es", "quien es", "que significa", "de donde viene",
            "cual es", "cuales son", "por que", "que",
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
