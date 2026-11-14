package com.example.jarvis.assistant

import com.example.jarvis.data.ReminderItem
import com.example.jarvis.data.TodoItem
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * El día de JARVIS: qué hay que hacer hoy, qué avisos hay y cómo está el
 * teléfono. Funciones puras: se pueden probar en tests unitarios.
 */
object Agenda {

    private val spanish = Locale.forLanguageTag("es")
    private val clock = DateTimeFormatter.ofPattern("HH:mm", spanish)

    /** Instante en milisegundos del final del día local. */
    fun endOfDay(date: LocalDate): Long = date.plusDays(1)
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

    /** Instante en milisegundos del principio del día local. */
    fun startOfDay(date: LocalDate): Long = date
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

    /** Tareas que tocan hoy: las que tienen fecha y no han pasado de hoy. */
    fun todosForToday(todos: List<TodoItem>, now: LocalDate): List<TodoItem> {
        val limit = endOfDay(now)
        return todos
            .filter { !it.done && (it.dueAt ?: return@filter false) <= limit }
            .sortedBy { it.dueAt }
    }

    /** Recordatorios que saltan hoy. */
    fun remindersForToday(reminders: List<ReminderItem>, now: LocalDate): List<ReminderItem> {
        val limit = endOfDay(now)
        return reminders
            .filter { !it.done && it.triggerAt <= limit }
            .sortedBy { it.triggerAt }
    }

    /** «hoy» o «mañana», según el momento. */
    fun relativeLabel(millis: Long, now: LocalDate): String {
        val date = Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        return when (date) {
            now -> "hoy"
            now.plusDays(1) -> "mañana"
            else -> date.format(DateTimeFormatter.ofPattern("d 'de' MMMM", spanish))
        }
    }

    /** Hora local de un instante. */
    fun time(millis: Long): String = Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
        .format(clock)

    /**
     * Resumen del día, listo para hablar o para una notificación.
     */
    fun summary(
        todos: List<TodoItem>,
        reminders: List<ReminderItem>,
        now: LocalDate,
        weatherLine: String? = null,
        batteryLine: String? = null,
    ): String {
        val taskList = todosForToday(todos, now)
        val reminderList = remindersForToday(reminders, now)
        val parts = mutableListOf<String>()

        if (taskList.isEmpty() && reminderList.isEmpty()) {
            parts += "No tiene nada pendiente para hoy"
        } else {
            if (taskList.isNotEmpty()) {
                val list = taskList.take(4).joinToString(", ") { task ->
                    val hour = task.dueAt?.let { time(it) }
                    if (hour != null) "$task.text (a las $hour)" else task.text
                }
                val more = if (taskList.size > 4) " y ${taskList.size - 4} más" else ""
                parts += "${taskList.size} tarea(s): $list$more"
            }
            if (reminderList.isNotEmpty()) {
                val list = reminderList.take(3).joinToString(", ") { reminder ->
                    val hour = reminder.triggerAt.let { time(it) }
                    "$reminder.text (a las $hour)"
                }
                val more = if (reminderList.size > 3) " y ${reminderList.size - 3} más" else ""
                parts += "${reminderList.size} aviso(s): $list$more"
            }
        }

        weatherLine?.takeIf { it.isNotBlank() }?.let { parts += it }
        batteryLine?.takeIf { it.isNotBlank() }?.let { parts += it }

        return parts.joinToString(". ") + "."
    }
}
