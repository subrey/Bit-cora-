package com.example.jarvis.assistant

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutineParserTest {

    @Test
    fun `entiene una rutina de temporizador`() {
        val spec = RoutineParser.parse("cuando suene el temporizador de los huevos, apaga la música")!!
        assertEquals(RoutineTrigger.TIMER, spec.trigger)
        assertEquals("huevos", spec.triggerLabel)
        assertEquals(RoutineAction.STOP_MUSIC, spec.action)
    }

    @Test
    fun `entiene una rutina de recordatorio`() {
        val spec = RoutineParser.parse("cuando salte el recordatorio de la reunión, pon música")!!
        assertEquals(RoutineTrigger.REMINDER, spec.trigger)
        assertEquals("reunion", spec.triggerLabel)
        assertEquals(RoutineAction.PLAY_MUSIC, spec.action)
    }

    @Test
    fun `entiene una rutina horaria`() {
        val spec = RoutineParser.parse("cada mañana a las 8, recuérdame el café")!!
        assertEquals(RoutineTrigger.HOURLY, spec.trigger)
        assertEquals(8, spec.hour)
        assertEquals(0, spec.minute)
        assertEquals(RoutineAction.REMIND, spec.action)
        assertTrue(spec.payload.contains("cafe"))
    }

    @Test
    fun `entiene una rutina horaria con minutos`() {
        val spec = RoutineParser.parse("cada día a las 07:30 avísame")!!
        assertEquals(RoutineTrigger.HOURLY, spec.trigger)
        assertEquals(7, spec.hour)
        assertEquals(30, spec.minute)
        assertEquals(RoutineAction.NOTIFY, spec.action)
    }

    @Test
    fun `sin disparador ni accion no hay rutina`() {
        assertNull(RoutineParser.parse("pon música"))
        assertNull(RoutineParser.parse(""))
    }

    @Test
    fun `detecta que una frase es una rutina`() {
        assertTrue(RoutineParser.looksLikeRoutine("crea una rutina para el cafe"))
        assertTrue(RoutineParser.looksLikeRoutine("cuando suene el temporizador apaga la música"))
        assertTrue(!RoutineParser.looksLikeRoutine("pon música"))
    }
}
