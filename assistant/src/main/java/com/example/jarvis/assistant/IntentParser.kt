package com.example.jarvis.assistant

import java.text.Normalizer

/** Intención detectada en una frase del usuario. */
enum class IntentKind {
    GREETING,
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
            .replace("[^\\p{L}\\p{N}\\s:,.+*/()%\\-]".toRegex(), " ")
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

        // --- Reloj -------------------------------------------------------
        if (has("que hora es", "la hora", "dime la hora", "hora es ahora")) {
            return ParsedIntent(IntentKind.TIME, raw = raw)
        }
        if (has("que dia es", "que fecha", "fecha de hoy", "dia de hoy")) {
            return ParsedIntent(IntentKind.DATE, raw = raw)
        }

        // --- Música ------------------------------------------------------
        if (has("sube volumen", "sube el volumen", "volumen arriba", "mas alto")) {
            return ParsedIntent(IntentKind.VOLUME_UP, raw = raw)
        }
        if (has("baja volumen", "baja el volumen", "volumen abajo", "mas bajo", "silencia")) {
            return ParsedIntent(IntentKind.VOLUME_DOWN, raw = raw)
        }
        if (has("pausa", "pause", "para la musica", "deten la musica", "para el canto")) {
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
        if (has("aleatoria", "shuffle", "mezcla", "al azar")) {
            return ParsedIntent(IntentKind.MUSIC_SHUFFLE, raw = raw)
        }
        if (has("deten", "stop", "apaga la musica", "corta la musica", "silenciar musica")) {
            return ParsedIntent(IntentKind.MUSIC_STOP, raw = raw)
        }
        if (has("pon musica", "ponme musica", "reproduce", "play", "pon una cancion", "musica", "cancion", "suena")) {
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
        if (has("que notas", "mis notas", "mis recuerdos", "que has memorizado")) {
            return ParsedIntent(IntentKind.LIST_MEMORIES, raw = raw)
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
        if (has("resumen", "estado general", "como estas", "informe")) {
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

        return ParsedIntent(IntentKind.UNKNOWN, raw = raw)
    }
}
