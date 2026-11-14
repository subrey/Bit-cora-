package com.example.jarvis.assistant

import com.example.jarvis.data.RoutineItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutinePlanTest {

    private fun routine(
        trigger: String = "TIMER",
        label: String = "huevos",
        action: String = "STOP_MUSIC",
        payload: String = "",
        enabled: Boolean = true,
    ) = RoutineItem(
        id = 1,
        trigger = trigger,
        triggerLabel = label,
        action = action,
        payload = payload,
        enabled = enabled,
    )

    @Test
    fun `una rutina sin etiqueta se aplica a cualquier temporizador`() {
        assertTrue(
            RoutinePlan.matches(routine(label = ""), RoutineTrigger.TIMER, "cualquier cosa"),
        )
    }

    @Test
    fun `una rutina con etiqueta solo se aplica a esa`() {
        assertTrue(RoutinePlan.matches(routine(), RoutineTrigger.TIMER, "huevos"))
        assertFalse(RoutinePlan.matches(routine(), RoutineTrigger.TIMER, "pasta"))
    }

    @Test
    fun `una rutina de recordatorio no se aplica a los temporizadores`() {
        val deRecordatorio = routine(trigger = "REMINDER")
        assertFalse(RoutinePlan.matches(deRecordatorio, RoutineTrigger.TIMER, "huevos"))
        assertTrue(RoutinePlan.matches(deRecordatorio, RoutineTrigger.REMINDER, "huevos"))
        assertFalse(RoutinePlan.matches(routine(), RoutineTrigger.REMINDER, "huevos"))
    }

    @Test
    fun `una rutina desactivada no se ejecuta`() {
        assertFalse(RoutinePlan.matches(routine(enabled = false), RoutineTrigger.TIMER, "huevos"))
    }

    @Test
    fun `selecciona solo las rutinas que aplican`() {
        val list = listOf(
            routine(label = "huevos"),
            routine(label = "pasta"),
            routine(label = "", enabled = false),
        )
        val pending = RoutinePlan.pending(list, RoutineTrigger.TIMER, "huevos")
        assertEquals(1, pending.size)
        assertEquals("huevos", pending.first().triggerLabel)
    }

    @Test
    fun `describe la rutina de forma legible`() {
        assertEquals(
            "cuando suene el temporizador huevos, quita la música.",
            RoutinePlan.describe(routine()),
        )
        assertEquals(
            "cada día a las 08:00, crea un recordatorio «el cafe».",
            RoutinePlan.describe(
                routine(trigger = "HOURLY", label = "08:00", action = "REMIND", payload = "el cafe"),
            ),
        )
    }

    @Test
    fun `el texto por defecto depende de la accion`() {
        assertTrue(RoutinePlan.actionText(routine()).contains("música"))
        assertTrue(
            RoutinePlan.actionText(routine(action = "NOTIFY")).contains("Aviso"),
        )
    }
}
