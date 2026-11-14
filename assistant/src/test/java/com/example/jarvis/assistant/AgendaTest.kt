package com.example.jarvis.assistant

import com.example.jarvis.data.ReminderItem
import com.example.jarvis.data.TodoItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AgendaTest {

    private val today = LocalDate.of(2026, 11, 14)
    private val tomorrow = LocalDate.of(2026, 11, 15)

    private fun todo(text: String, dueAt: Long?, done: Boolean = false) =
        TodoItem(id = 1, text = text, done = done, dueAt = dueAt)

    private fun reminder(text: String, triggerAt: Long, done: Boolean = false) =
        ReminderItem(id = 1, text = text, triggerAt = triggerAt, done = done)

    @Test
    fun `solo cuenta las tareas de hoy`() {
        val end = Agenda.endOfDay(today)
        val todos = listOf(
            todo("comprar pan", end - 3_600_000L),
            todo("llamar al banco", Agenda.endOfDay(tomorrow)),
            todo("sin fecha", null),
            todo("ya hecha", end - 60_000L, done = true),
        )
        val deHoy = Agenda.todosForToday(todos, today)
        assertEquals(1, deHoy.size)
        assertEquals("comprar pan", deHoy.first().text)
    }

    @Test
    fun `solo cuenta los avisos de hoy`() {
        val end = Agenda.endOfDay(today)
        val avisos = listOf(
            reminder("reunion", end - 7_200_000L),
            reminder("mañana", Agenda.endOfDay(tomorrow)),
            reminder("hecho", end - 60_000L, done = true),
        )
        val deHoy = Agenda.remindersForToday(avisos, today)
        assertEquals(1, deHoy.size)
        assertEquals("reunion", deHoy.first().text)
    }

    @Test
    fun `resume el dia con tareas y avisos`() {
        val end = Agenda.endOfDay(today)
        val text = Agenda.summary(
            todos = listOf(todo("comprar pan", end - 3_600_000L)),
            reminders = listOf(reminder("llamar al banco", end - 7_200_000L)),
            now = today,
        )
        assertTrue(text.contains("1 tarea(s)"))
        assertTrue(text.contains("comprar pan"))
        assertTrue(text.contains("1 aviso(s)"))
        assertTrue(text.contains("llamar al banco"))
    }

    @Test
    fun `resume el dia vacio`() {
        val text = Agenda.summary(emptyList(), emptyList(), today)
        assertTrue(text.contains("No tiene nada pendiente"))
    }

    @Test
    fun `añade el tiempo y la bateria al resumen`() {
        val text = Agenda.summary(
            todos = emptyList(),
            reminders = emptyList(),
            now = today,
            weatherLine = "En Madrid hay 12 grados",
            batteryLine = "La batería está al 80 por ciento",
        )
        assertTrue(text.contains("12 grados"))
        assertTrue(text.contains("80 por ciento"))
    }

    @Test
    fun `etiqueta las fechas relativas`() {
        val end = Agenda.endOfDay(today)
        val manana = Agenda.startOfDay(tomorrow) + 3_600_000L
        assertEquals("hoy", Agenda.relativeLabel(end - 3_600_000L, today))
        assertEquals("mañana", Agenda.relativeLabel(manana, today))
        assertEquals("16 de noviembre", Agenda.relativeLabel(manana + 86_400_000L, today))
    }
}
