package com.example.jarvis.assistant

import java.text.Normalizer
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale

/** Especificación de una alarma interpretada de una frase. */
data class AlarmSpec(
    val hour: Int,
    val minute: Int,
    val label: String,
    /** true si suena todos los días. */
    val repeatDaily: Boolean,
    /** Momento exacto del primer (o único) disparo. */
    val triggerAtMillis: Long,
)

/**
 * Interpreta frases como «despiértame a las 7», «pon una alarma mañana a las
 * 6:30» o «despiértame en 20 minutos». Sin dependencias de Android.
 */
object AlarmParser {

    private val words = listOf(
        "despiertame", "despertarme", "despierta", "alarma", "levantame", "avisame a las",
        "pon una alarma", "pon alarma", "pon la alarma",
    )

    fun normalize(text: String): String {
        val lower = text.lowercase(Locale.forLanguageTag("es"))
        return Normalizer.normalize(lower, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
    }

    fun looksLikeAlarm(text: String): Boolean {
        val clean = normalize(text)
        return words.any { clean.contains(it) }
    }

    fun parse(payload: String, base: LocalDateTime = LocalDateTime.now()): AlarmSpec? {
        val clean = normalize(payload).trim()
        if (clean.isBlank()) return null

        // 1) Relativa: «en 20 minutos», «en 2 horas»
        val rel = Regex("\\ben\\s+(\\d+)\\s*(minutos?|min|horas?|h)\\b").find(clean)
        if (rel != null) {
            val amount = rel.groupValues[1].toLongOrNull() ?: return null
            val unit = rel.groupValues[2]
            val whenTime = if (unit.startsWith("h")) base.plusHours(amount) else base.plusMinutes(amount)
            return AlarmSpec(
                hour = whenTime.hour,
                minute = whenTime.minute,
                label = labelOf(clean),
                repeatDaily = false,
                triggerAtMillis = toMillis(whenTime),
            )
        }

        // 2) Hora fija: «a las 7», «a las 6:30», «a la 1»
        val at = Regex("\\ba\\s+las?\\s+(\\d{1,2})(?::(\\d{2}))?\\s*(de la tarde|de la manana|de la noche|de la madrugada|pm|am)?")
            .find(clean) ?: return null
        var hour = at.groupValues[1].toIntOrNull() ?: return null
        val minute = at.groupValues[2].toIntOrNull() ?: 0
        val mod = at.groupValues[3]
        if ((mod.contains("tarde") || mod.contains("noche") || mod == "pm") && hour < 12) hour += 12
        if ((mod.contains("manana") || mod == "am") && hour == 12) hour = 0
        val time = runCatching { LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59)) }.getOrNull() ?: return null

        val oneShot = clean.contains("manana") || clean.contains("pasado manana") || clean.contains("hoy")
        val date = when {
            clean.contains("pasado manana") -> base.toLocalDate().plusDays(2)
            clean.contains("manana") -> base.toLocalDate().plusDays(1)
            else -> base.toLocalDate()
        }
        var whenTime = LocalDateTime.of(date, time)
        if (!oneShot && whenTime.isBefore(base)) whenTime = whenTime.plusDays(1)
        if (oneShot && clean.contains("hoy") && whenTime.isBefore(base)) whenTime = whenTime.plusDays(1)

        return AlarmSpec(
            hour = time.hour,
            minute = time.minute,
            label = labelOf(clean),
            repeatDaily = !oneShot,
            triggerAtMillis = toMillis(whenTime),
        )
    }

    private fun toMillis(dt: LocalDateTime): Long =
        dt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun labelOf(clean: String): String {
        val stripped = clean
            .replace(Regex("\\b(me despiertas|me pones una|me pones|despiertame|despertarme|despiertas|despierta|levantame|pon una alarma|pon la alarma|pon alarma|alarma|puedes|podrias|por favor)\\b"), " ")
            .replace(Regex("\\ba\\s+las?\\s+\\d{1,2}(:\\d{2})?(\\s*(de la tarde|de la manana|de la noche|de la madrugada|pm|am))?\\b"), " ")
            .replace(Regex("\\ben\\s+\\d+\\s*(minutos?|min|horas?|h)\\b"), " ")
            .replace(Regex("\\b(manana|pasado manana|hoy|para|de|que)\\b"), " ")
            .replace(Regex("[¿?¡!.,]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        return stripped.take(40)
    }
}