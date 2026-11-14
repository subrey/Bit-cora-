package com.example.jarvis.assistant

import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** Recordatorio extraído de una frase en lenguaje natural. */
data class ParsedReminder(
    val message: String,
    val triggerAtMillis: Long,
)

/**
 * Analiza frases en lenguaje natural para extraer fecha, hora y mensaje de un
 * recordatorio. Sin dependencias de Android: se puede probar en tests.
 *
 * Ejemplos entendidos:
 * - "comprar leche a las 6 de la tarde"
 * - "llamar a mamá mañana a las 10:30"
 * - "reunión en 15 minutos"
 * - "pon un recordatorio para el viernes a las 9"
 */
object ReminderParser {

    /** Palabras que se pueden quitar del principio del mensaje. */
    private val LEADING_NOISE = setOf(
        "recuérdame", "recuerdame", "recordarme", "avísame", "avisame", "avisa",
        "ponme", "pon", "crea", "crear", "hazme", "un", "una", "unos", "unas",
        "recordatorio", "alarma", "aviso", "el", "la", "los", "las", "de", "del",
        "que", "para", "por", "favor", "me", "a",
    )

    /** Palabras vacías que no sirven para identificar un recordatorio. */
    private val STOP_WORDS = setOf(
        "el", "la", "los", "las", "un", "una", "de", "del", "y", "para", "por",
        "con", "que", "me", "a", "en", "mi", "mis", "recordatorio", "aviso", "alarma",
    )

    fun parse(payload: String, baseTime: LocalDateTime = LocalDateTime.now()): ParsedReminder? {
        val clean = IntentParser.normalize(payload)
        if (clean.isBlank()) return null

        var text = clean
        var target: LocalDateTime? = null

        // --- 1. "en 30 minutos" / "en 2 horas" --------------------------
        val relative = Regex("\\ben\\s+(\\d+)\\s*(minutos?|min|horas?|h)\\b").find(text)
        if (relative != null) {
            val amount = relative.groupValues[1].toLongOrNull() ?: return null
            val unit = relative.groupValues[2]
            target = if (unit.startsWith("h")) {
                baseTime.plusHours(amount)
            } else {
                baseTime.plusMinutes(amount)
            }
            text = text.replace(relative.value, " ")
        }

        // --- 2. Día relativo ---------------------------------------------
        // «mañana» como día, pero no como «a la mañana» (que es la mañana).
        val mananaWord = Regex("(?<!de la )(?<!a la )(?<!por la )\\bmanana\\b")
        var dayOffset: Long? = null
        when {
            text.contains("pasado manana") || text.contains("pasado mañana") -> {
                dayOffset = 2
                text = text.replace("pasado manana", " ").replace("pasado mañana", " ")
            }

            mananaWord.containsMatchIn(text) -> {
                dayOffset = 1
                text = text.replace(mananaWord, " ")
            }

            text.contains("hoy") -> {
                dayOffset = 0
                text = text.replace("hoy", " ")
            }
        }

        // --- 3. Hora concreta: "a las 6", "a las 10:30", "a la 1" ------
        if (target == null) {
            val at = bestTimeMatch(text)
            if (at != null) {
                var hour = at.hour
                val minute = at.minute
                val modifier = at.modifier
                val isPm = modifier.contains("tarde") || modifier.contains("noche") || modifier == "pm"
                val isAm = modifier.contains("manana") || modifier == "am"
                if (isPm && hour < 12) hour += 12
                if (isAm && hour == 12) hour = 0
                val time = runCatching { LocalTime.of(hour, minute) }.getOrNull()
                if (time != null) {
                    val date = baseTime.toLocalDate().plusDays(dayOffset ?: 0L)
                    var dt = LocalDateTime.of(date, time)
                    if (modifier.isBlank()) {
                        // Sin «de la tarde» se usa la convención de las 12 horas:
                        // si la mañana ya pasó, se entiende la tarde.
                        val am = dt
                        val pm = if (hour < 12) {
                            runCatching { LocalDateTime.of(date, LocalTime.of(hour + 12, minute)) }.getOrNull()
                        } else {
                            null
                        }
                        dt = when {
                            !am.isBefore(baseTime) -> am
                            pm != null && !pm.isBefore(baseTime) -> pm
                            pm != null -> pm.plusDays(1)
                            else -> am.plusDays(1)
                        }
                    } else if (dayOffset == null && dt.isBefore(baseTime)) {
                        dt = dt.plusDays(1)
                    }
                    // «hoy» con una hora ya pasada: el aviso salta casi al momento.
                    if (dayOffset == 0L && dt.isBefore(baseTime)) dt = baseTime.plusMinutes(1)
                    target = dt
                    text = text.replace(at.text, " ")
                }
            }
        }

        // Sin hora reconocible: se avisa dentro de 10 minutos.
        if (target == null) {
            target = baseTime.plusMinutes(10)
        }

        val message = cleanMessage(text)
        val millis = target.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return ParsedReminder(message = message, triggerAtMillis = millis)
    }

    /**
     * Elige la hora más probable de la frase. Una frase puede tener varios
     * números («llamar a 3 personas a las 6»), así que se puntúa cada
     * coincidencia y gana la que lleva artículo o modificador.
     */
    private fun bestTimeMatch(text: String): TimeMatch? {
        val regex = Regex(
            "\\ba\\s+(las\\s+|la\\s+)?(\\d{1,2})(?::(\\d{2}))?\\s*(de la tarde|de la manana|de la noche|de la madrugada|pm|am)?"
        )
        var best: TimeMatch? = null
        var bestScore = -1
        for (match in regex.findAll(text)) {
            val hour = match.groupValues[2].toIntOrNull() ?: continue
            val minute = match.groupValues[3].toIntOrNull() ?: 0
            val modifier = match.groupValues[4]
            var score = 0
            if (match.groupValues[1].isNotBlank()) score += 2
            if (modifier.isNotBlank()) score += 1
            if (score > bestScore) {
                bestScore = score
                best = TimeMatch(match.value, hour, minute, modifier)
            }
        }
        return best
    }

    private data class TimeMatch(
        val text: String,
        val hour: Int,
        val minute: Int,
        val modifier: String,
    )

    /** Quita el ruido del principio y normaliza los espacios. */
    private fun cleanMessage(text: String): String {
        val tokens = text.split(" ").filter { it.isNotBlank() }.toMutableList()
        while (tokens.isNotEmpty()) {
            val head = tokens.first().lowercase()
            if (head in LEADING_NOISE) {
                tokens.removeAt(0)
            } else {
                break
            }
        }
        val cleaned = tokens.joinToString(" ")
            .replace(Regex("\\s+"), " ")
            .trim()
        return cleaned.ifBlank { "Recordatorio" }.take(60)
    }

    /**
     * Comprueba si un recordatorio corresponde a lo que el usuario ha pedido.
     * Ignora las palabras vacías: «cancela el recordatorio de comprar pan»
     * encuentra «comprar el pan».
     */
    fun matchesMessage(reminderText: String, query: String): Boolean {
        val reminderTokens = significantTokens(reminderText)
        val queryTokens = significantTokens(query)
        if (queryTokens.isEmpty() || reminderTokens.isEmpty()) return false
        return queryTokens.all { token -> reminderTokens.any { it.contains(token) } }
    }

    private fun significantTokens(text: String): List<String> =
        IntentParser.normalize(text)
            .split(" ")
            .filter { it.isNotBlank() && it !in STOP_WORDS }
}
