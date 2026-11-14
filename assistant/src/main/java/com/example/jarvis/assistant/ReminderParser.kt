package com.example.jarvis.assistant

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class ParsedReminder(
    val message: String,
    val triggerAtMillis: Long,
)

/**
 * Analiza frases en lenguaje natural para extraer fecha, hora y mensaje de recordatorio.
 * Soporta expresiones como:
 * - "comprar leche a las 6 de la tarde"
 * - "llamar a mamá mañana a las 10:30"
 * - "reunión en 15 minutos"
 */
object ReminderParser {

    fun parse(payload: String, baseTime: LocalDateTime = LocalDateTime.now()): ParsedReminder? {
        val clean = IntentParser.normalize(payload)
        if (clean.isBlank()) return null

        var targetDateTime: LocalDateTime? = null
        var textToClean = clean

        // Caso 1: "en X minutos / horas"
        val inTimeRegex = Regex("(?:en)\\s+(\\d+)\\s*(minutos?|min|horas?|h)")
        val inMatch = inTimeRegex.find(clean)
        if (inMatch != null) {
            val amount = inMatch.groupValues[1].toLongOrNull() ?: return null
            val unit = inMatch.groupValues[2]
            targetDateTime = if (unit.startsWith("h")) {
                baseTime.plusHours(amount)
            } else {
                baseTime.plusMinutes(amount)
            }
            textToClean = clean.replace(inMatch.value, "")
        }

        // Caso 2: "a las HH:mm" o "a las 5 de la tarde"
        if (targetDateTime == null) {
            val atTimeRegex = Regex("(?:a las?)\\s+(\\d{1,2})(?::(\\d{2}))?\\s*(de la tarde|de la manana|de la noche|pm|am)?")
            val atMatch = atTimeRegex.find(clean)
            if (atMatch != null) {
                var hour = atMatch.groupValues[1].toIntOrNull() ?: return null
                val minute = atMatch.groupValues[2].toIntOrNull() ?: 0
                val modifier = atMatch.groupValues[3]

                if ((modifier == "de la tarde" || modifier == "de la noche" || modifier == "pm") && hour < 12) {
                    hour += 12
                } else if ((modifier == "de la manana" || modifier == "am") && hour == 12) {
                    hour = 0
                }

                var date = baseTime.toLocalDate()
                if (clean.contains("manana")) {
                    date = date.plusDays(1)
                    textToClean = textToClean.replace("manana", "")
                } else if (clean.contains("pasado manana")) {
                    date = date.plusDays(2)
                    textToClean = textToClean.replace("pasado manana", "")
                } else if (clean.contains("hoy")) {
                    textToClean = textToClean.replace("hoy", "")
                }

                val time = runCatching { LocalTime.of(hour, minute) }.getOrNull() ?: return null
                var dt = LocalDateTime.of(date, time)
                // Si la hora ya pasó hoy y no se especificó día, asumimos mañana
                if (!clean.contains("manana") && !clean.contains("hoy") && dt.isBefore(baseTime)) {
                    dt = dt.plusDays(1)
                }
                targetDateTime = dt
                textToClean = textToClean.replace(atMatch.value, "")
            }
        }

        // Si no se pudo determinar una hora/fecha específica, por defecto en 10 minutos
        if (targetDateTime == null) {
            targetDateTime = baseTime.plusMinutes(10)
        }

        val reminderMessage = textToClean
            .replace(Regex("\\b(que|de|a|el|la|los|las|por favor)\\b"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .ifBlank { "Recordatorio" }

        val millis = targetDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        return ParsedReminder(
            message = reminderMessage,
            triggerAtMillis = millis,
        )
    }
}
