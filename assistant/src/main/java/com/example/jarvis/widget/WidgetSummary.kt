package com.example.jarvis.widget

import com.example.jarvis.data.ReminderItem
import com.example.jarvis.data.TimerItem
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Lo que el widget muestra en pantalla. */
data class WidgetState(
    val status: String,
    val nextText: String,
    val countsText: String,
)

/**
 * Prepara el texto del widget de escritorio. Sin dependencias de Android: se
 * puede probar en tests unitarios.
 */
object WidgetSummary {

    private val spanish = Locale.forLanguageTag("es")
    private val moment = DateTimeFormatter.ofPattern("d MMM · HH:mm", spanish)

    /** Estado del asistente según lo que esté haciendo ahora mismo. */
    fun status(listening: Boolean, speaking: Boolean): String = when {
        speaking -> "Hablando"
        listening -> "Escuchando"
        else -> "En espera"
    }

    /**
     * Elige el próximo aviso: primero el recordatorio más cercano y, si no
     * hay ninguno, el temporizador activo más antiguo.
     */
    fun next(
        reminder: ReminderItem?,
        timer: TimerItem?,
    ): String? {
        if (reminder != null) {
            return "${reminder.text} · ${format(reminder.triggerAt)}"
        }
        if (timer != null) {
            val minutes = timer.durationSeconds / 60
            val when_ = if (minutes >= 1) "$minutes min" else "${timer.durationSeconds} s"
            return "${timer.label} · $when_"
        }
        return null
    }

    /** Resumen de lo que hay pendiente. */
    fun counts(pendingTasks: Int, pendingReminders: Int): String {
        val tasks = "$pendingTasks tarea(s)"
        val reminders = "$pendingReminders recordatorio(s)"
        return "$tasks · $reminders"
    }

    /** Texto completo del widget. */
    fun build(
        listening: Boolean,
        speaking: Boolean,
        reminder: ReminderItem?,
        timer: TimerItem?,
        pendingTasks: Int,
        pendingReminders: Int,
    ): WidgetState = WidgetState(
        status = status(listening, speaking),
        nextText = next(reminder, timer) ?: "Nada programado",
        countsText = counts(pendingTasks, pendingReminders),
    )

    private fun format(millis: Long): String =
        Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
            .format(moment)
}
