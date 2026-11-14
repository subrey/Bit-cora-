package com.example.jarvis.assistant

import java.text.Normalizer
import java.util.Locale

/** Qué dispara una rutina. */
enum class RoutineTrigger {
    /** Cuando termina un temporizador. */
    TIMER,

    /** Cuando salta un recordatorio. */
    REMINDER,

    /** A una hora concreta, cada día. */
    HOURLY,
}

/** Qué hace una rutina cuando se dispara. */
enum class RoutineAction {
    /** Quita la música. */
    STOP_MUSIC,

    /** Pone música. */
    PLAY_MUSIC,

    /** Muestra una notificación. */
    NOTIFY,

    /** Lo dice con la voz. */
    SPEAK,

    /** Crea un recordatorio. */
    REMIND,
}

/** Rutina interpretada a partir de una frase. */
data class RoutineSpec(
    val trigger: RoutineTrigger,
    /** Etiqueta a la que se limita (vacío = cualquiera). */
    val triggerLabel: String,
    val action: RoutineAction,
    /** Texto de la acción (mensaje o recordatorio). */
    val payload: String,
    /** Texto para decir en voz alta. */
    val label: String,
    /** Hora y minuto de las rutinas horarias (0 si no aplica). */
    val hour: Int = 0,
    val minute: Int = 0,
)

/**
 * Interpreta frases como «cuando suene el temporizador de los huevos, apaga la
 * música» o «cada mañana a las 8, recuérdame el café». Función pura: se puede
 * probar en tests unitarios.
 */
object RoutineParser {

    /** Palabras que indican que hay que crear una rutina. */
    private val CREATE_HINTS = listOf(
        "crea una rutina", "crea el rutina", "haz una rutina", "crea una automatizacion",
        "pon una rutina", "programa que", "cada vez que", "cuando suene", "cuando termine",
        "cuando salte", "cada dia", "cada mañana", "todas las noches", "todos los dias",
    )

    /** Mapea el disparador con sus palabras. */
    private val TRIGGERS: List<Pair<RoutineTrigger, List<String>>> = listOf(
        RoutineTrigger.TIMER to listOf("temporizador", "alarma", "cronometro"),
        RoutineTrigger.REMINDER to listOf("recordatorio", "aviso"),
    )

    private val STOP_MUSIC_WORDS = listOf(
        "apaga la musica", "quita la musica", "para la musica", "quitar la musica",
        "silencia la musica", "apaga el sonido", "quita el sonido", "sin musica",
    )
    private val PLAY_MUSIC_WORDS = listOf(
        "pon la musica", "pon musica", "empieza la musica", "play la musica", "suena musica",
    )
    private val NOTIFY_WORDS = listOf("avisame", "avisa", "notificame", "notifica")
    private val SPEAK_WORDS = listOf("dime", "hablame", "anuncia", "diganos")
    private val REMIND_WORDS = listOf("recordame", "recuérdame", "recuerdame", "avisa de")

    fun normalize(text: String): String {
        val lower = text.lowercase(Locale.forLanguageTag("es"))
        val noAccents = Normalizer.normalize(lower, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        return noAccents.replace(Regex("\\s+"), " ").trim()
    }

    /** true si la frase parece querer crear una rutina. */
    fun looksLikeRoutine(text: String): Boolean {
        val clean = normalize(text)
        return CREATE_HINTS.any { clean.contains(it) }
    }

    /**
     * Interpreta la frase. Devuelve null si no se puede entender.
     */
    fun parse(text: String): RoutineSpec? {
        val clean = normalize(text)
        if (clean.isBlank()) return null

        // 1. Disparador horario: «cada mañana a las 8».
        val hourly = HOURLY_REGEX.find(clean)
        if (hourly != null) {
            val action = findAction(clean) ?: return null
            val payload = actionPayload(clean, action)
            val hour = hourly.groupValues[1].toIntOrNull() ?: 0
            val minute = hourly.groupValues[2].toIntOrNull() ?: 0
            val clock = String.format("%02d:%02d", hour, minute)
            return RoutineSpec(
                trigger = RoutineTrigger.HOURLY,
                triggerLabel = clock,
                action = action,
                payload = payload,
                label = "cada dia a las $hour, ${actionName(action)}",
                hour = hour,
                minute = minute,
            )
        }

        // 2. Disparador por temporizador o recordatorio.
        for ((trigger, words) in TRIGGERS) {
            val word = words.firstOrNull { clean.contains(it) } ?: continue
            val action = findAction(clean) ?: continue
            val label = extractLabel(clean, word)
            val payload = actionPayload(clean, action)
            return RoutineSpec(
                trigger = trigger,
                triggerLabel = label,
                action = action,
                payload = payload,
                label = "cuando suene ${if (label.isBlank()) "el temporizador" else label}, " +
                    actionName(action),
            )
        }

        return null
    }

    /** Acción que pide la frase. */
    fun findAction(text: String): RoutineAction? {
        val clean = normalize(text)
        if (STOP_MUSIC_WORDS.any { clean.contains(it) }) return RoutineAction.STOP_MUSIC
        if (PLAY_MUSIC_WORDS.any { clean.contains(it) }) return RoutineAction.PLAY_MUSIC
        if (REMIND_WORDS.any { clean.contains(it) }) return RoutineAction.REMIND
        if (NOTIFY_WORDS.any { clean.contains(it) }) return RoutineAction.NOTIFY
        if (SPEAK_WORDS.any { clean.contains(it) }) return RoutineAction.SPEAK
        return null
    }

    /** Nombre legible de la acción. */
    fun actionName(action: RoutineAction): String = when (action) {
        RoutineAction.STOP_MUSIC -> "quitar la música"
        RoutineAction.PLAY_MUSIC -> "poner música"
        RoutineAction.NOTIFY -> "avisar"
        RoutineAction.SPEAK -> "decirlo"
        RoutineAction.REMIND -> "recordar"
    }

    /**
     * Texto de la acción: lo que hay después de la palabra que la launches.
     */
    private fun actionPayload(text: String, action: RoutineAction): String {
        val clean = normalize(text)
        val marker = when (action) {
            RoutineAction.STOP_MUSIC -> STOP_MUSIC_WORDS.firstOrNull { clean.contains(it) }
            RoutineAction.PLAY_MUSIC -> PLAY_MUSIC_WORDS.firstOrNull { clean.contains(it) }
            RoutineAction.NOTIFY -> NOTIFY_WORDS.firstOrNull { clean.contains(it) }
            RoutineAction.SPEAK -> SPEAK_WORDS.firstOrNull { clean.contains(it) }
            RoutineAction.REMIND -> REMIND_WORDS.firstOrNull { clean.contains(it) }
        }
        if (marker == null) return ""
        val after = clean.substringAfter(marker).trim().trim(',', '.', ':', ';')
        return after.trim().take(60)
    }

    /**
     * Etiqueta del disparador: lo que hay entre el nombre del temporizador y la
     * acción. «el temporizador de los huevos, apaga la música» → «huevos».
     */
    private fun extractLabel(text: String, word: String): String {
        val clean = normalize(text)
        var rest = clean.substringAfter(word).trim()
        rest = rest.removePrefix("de").removePrefix("del").trim()
        // Se corta en la primera coma o en la palabra de la acción.
        rest = rest.substringBefore(",")
        for (action in STOP_MUSIC_WORDS + PLAY_MUSIC_WORDS + NOTIFY_WORDS + SPEAK_WORDS + REMIND_WORDS) {
            val index = rest.indexOf(action)
            if (index >= 0) rest = rest.substring(0, index)
        }
        rest = rest.trim().trim(',', '.', ':', ';')
        // Se quita el artículo inicial.
        rest = rest.removePrefix("el ").removePrefix("la ").removePrefix("los ")
            .removePrefix("las ").trim()
        return rest.take(40)
    }

    private val HOURLY_REGEX = Regex("\\b(?:cada|todos los|todas las)\\s+(?:dia|dias|manana|martes|miercoles|jueves|viernes|sabado|domingo|noche)\\b.*?\\ba las\\s+(\\d{1,2})(?:[:.](\\d{2}))?")
}
