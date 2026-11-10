package com.example.bitacora.ui.theme

import androidx.compose.ui.graphics.Color

val Paper = Color(0xFFFDFBF7)
val Ink = Color(0xFF1C1B1F)
val Violet = Color(0xFF6B4EFF)

/** Par de colores para el icono/portada de cada nota. */
data class NoteAccent(val container: Color, val content: Color)

private val lightAccents = listOf(
    NoteAccent(Color(0xFFFFE3E3), Color(0xFF8C2F39)), // coral
    NoteAccent(Color(0xFFFFEFC7), Color(0xFF7A5A00)), // ámbar
    NoteAccent(Color(0xFFDFF3E3), Color(0xFF1E5B33)), // menta
    NoteAccent(Color(0xFFDDEBFF), Color(0xFF1F4E8C)), // cielo
    NoteAccent(Color(0xFFEDE3FF), Color(0xFF4A2C8F)), // lavanda
    NoteAccent(Color(0xFFFFE6F2), Color(0xFF8C2F63)), // rosa
)

private val darkAccents = listOf(
    NoteAccent(Color(0xFF4A2327), Color(0xFFFFC2C6)),
    NoteAccent(Color(0xFF4A3A12), Color(0xFFFFDF9E)),
    NoteAccent(Color(0xFF1F3B28), Color(0xFFA9E2BC)),
    NoteAccent(Color(0xFF1E344E), Color(0xFFBBD9FF)),
    NoteAccent(Color(0xFF332659), Color(0xFFD9CCFF)),
    NoteAccent(Color(0xFF45223A), Color(0xFFFFBFDF)),
)

fun noteAccent(index: Int, dark: Boolean): NoteAccent {
    val palette = if (dark) darkAccents else lightAccents
    val safeIndex = ((index % palette.size) + palette.size) % palette.size
    return palette[safeIndex]
}

/** Emojis disponibles para decorar una nota. */
val noteEmojis = listOf(
    "📝", "💡", "📚", "🎯", "🌱", "🔥",
    "✨", "🧠", "✈️", "☕", "🎵", "📖",
)
