package com.example.bitacora.ui

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

private val spanish = Locale.forLanguageTag("es")
private val dayMonthFormatter = DateTimeFormatter.ofPattern("d MMM", spanish)
private val dayMonthYearFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", spanish)

/** "Hoy", "Ayer", "3 mar" o "3 mar 2024" según la fecha. */
fun formatDate(millis: Long): String {
    val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    val today = LocalDate.now()
    return when {
        date == today -> "Hoy"
        date == today.minusDays(1) -> "Ayer"
        date.year == today.year -> date.format(dayMonthFormatter)
        else -> date.format(dayMonthYearFormatter)
    }
}

fun wordCount(text: String): Int {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return 0
    return trimmed.split(Regex("\\s+")).size
}

fun readingMinutes(text: String): Int = max(1, (wordCount(text) + 199) / 200)

/** Primeras [limit] letras del contenido, en una sola línea. */
fun excerpt(text: String, limit: Int = 150): String {
    val clean = text.replace(Regex("\\s+"), " ").trim()
    return if (clean.length <= limit) clean else clean.take(limit).trimEnd() + "…"
}
