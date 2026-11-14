package com.example.jarvis.widget

import com.example.jarvis.data.ReminderItem
import com.example.jarvis.data.TimerItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetSummaryTest {

    @Test
    fun `muestra el estado segun lo que hace el asistente`() {
        assertEquals("En espera", WidgetSummary.status(listening = false, speaking = false))
        assertEquals("Escuchando", WidgetSummary.status(listening = true, speaking = false))
        assertEquals("Hablando", WidgetSummary.status(listening = true, speaking = true))
    }

    @Test
    fun `el proximo aviso es el recordatorio mas cercano`() {
        val reminder = ReminderItem(id = 1, text = "comprar pan", triggerAt = 1_700_000_000_000)
        val next = WidgetSummary.next(reminder = reminder, timer = null)
        assertTrue(next!!.contains("comprar pan"))
    }

    @Test
    fun `sin recordatorio muestra el temporizador`() {
        val timer = TimerItem(id = 1, label = "huevos", durationSeconds = 600)
        val next = WidgetSummary.next(reminder = null, timer = timer)
        assertTrue(next!!.contains("huevos"))
        assertTrue(next.contains("10 min"))
    }

    @Test
    fun `sin nada programado lo dice`() {
        assertEquals(null, WidgetSummary.next(reminder = null, timer = null))
        assertEquals("Nada programado", WidgetSummary.build(
            listening = false,
            speaking = false,
            reminder = null,
            timer = null,
            pendingTasks = 0,
            pendingReminders = 0,
        ).nextText)
    }

    @Test
    fun `resume las tareas y recordatorios pendientes`() {
        assertEquals("2 tarea(s) · 1 recordatorio(s)", WidgetSummary.counts(2, 1))
    }
}
