package com.example.jarvis.assistant

import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale

/** Alarma interpretada a partir de una frase. */
data class AlarmSpec(
    val hour: Int,
    val minute: Int,
    val label: String,
    /** true si suena todos los días. */
    val repeatDaily: Boolean,
    /** Momento exacto de la primera vez (null si es diaria). */
    val firstTriggerAt: Long?,
)

/**
 * Interpreta frases como «despiértame a las 7», «pon una alarma para mañana a
 * las 6:30» o «despiértame en 20 minutos». Función pura: se puede probar.
 */
object AlarmParser {

    /** Palabras que delatan una alarma. */
    private val ALARM_WORDS = listOf(
        "despiertame", "despierta", "despiértame", "alarma", "avisame a las",
        "levantame", "levántame",
    )

    fun normalize(text: String): String = text.lowercase(Locale.forLanguageTag("es"))

    /** true si la frase habla de una alarma. */
    fun looksLikeAlarm(text: String): Boolean {
        val clean = normalize(text)
        return ALARM_WORDS.any { clean.contains(it) }
    }

    /**
     * Interpreta la frase. Devuelve null si no hay hora reconocible.
     */
    fun parse(payload: String, base: LocalDateTime = LocalDateTime.now()): AlarmSpec? {
        val clean = normalize(payload)
        if (clean.isBlank()) return null

        // 1. Hora relativa: «en 20 minutos», «en 2 horas».
        val relative = Regex("\\ben\\s+(\\d+)\\s*(minutos?|min|horas?|h)\\b").find(clean)
        if (relative != null) {
            val amount = relative.groupValues[1].toLongOrNull() ?: return null
            val unit = relative.groupValues[2]
            val when_ = if (unit.startsWith("h")) {
                base.plusHours(amount)
            } else {
                base.plusMinutes(amount)
            }
            return AlarmSpec(
                hour = when_.hour,
                minute = when_.minute,
                label = labelOf(clean),
                repeatDaily = false,
                firstTriggerAt = when_.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            )
        }

        // 2. Hora fija: «a las 7», «a las 6:30», «a la 1».
        val at = Regex("\\ba\\s+las?\\s+(\\d{1,2})(?::(\\d{2}))?\\s*(de la tarde|de la manana|de la noche|de la madrugada|pm|am)?")
            .find(clean) ?: return null
        var hour = at.groupValues[1].toIntOrNull() ?: return null
        val minute = at.groupValues[2].toIntOrNull() ?: 0
        val modifier = at.groupValues[3]
        if ((modifier.contains("tarde") || modifier.contains("noche") || modifier == "pm") && hour < 12) {
            hour += 12
        } else if ((modifier.contains("manana") || modifier == "am") && hour == 12) {
            hour = 0
        }
        val time = runCatching { LocalTime.of(hour, minute) }.getOrNull() ?: return null

        // 3. ¿Un día concreto? Entonces la alarma salta solo esa vez.
        val oneShot = clean.contains("manana") || clean.contains("pasado manana") ||
            clean.contains("hoy") || clean.contains("esta noche")
        val date = when {
            clean.contains("pasado manana") -> base.toLocalDate().plusDays(2)
            clean.contains("manana") -> base.toLocalDate().plusDays(1)
            else -> base.toLocalDate()
        }
        var when_ = LocalDateTime.of(date, time)
        if (!oneShot && when_.isBefore(base)) {
            // Sin día concreto y la hora ya pasó: mañana.
            when_ = when_.plusDays(1)
        }

        return AlarmSpec(
            hour = when_.hour,
            minute = when_.minute,
            label = labelOf(clean),
            repeatDaily = !oneShot,
            firstTriggerAt = if (oneShot) {
                when_.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            } else {
                null
            },
        )
    }

    /** Etiqueta de la alarma: lo que el usuario ha dicho después de la orden. */
    private fun labelOf(clean: String): String {
        val withoutTrigger = clean
            .removePrefix("despiertame")
            .removePrefix("despierta")
            .removePrefix("pon una alarma")
            .removePrefix("pon la alarma")
            .removePrefix("pon alarma")
            .removePrefix("alarma")
            .trim()
        val cleaned = withoutTrigger
            .replace(Regex("\\b(por favor|que|para|a las|alarma|despiertame)\\b"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        return cleaned.take(40)
    }
}
