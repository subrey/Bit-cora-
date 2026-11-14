package com.example.jarvis.assistant

import com.example.jarvis.data.RoutineItem

/**
 * Convierte una rutina guardada en algo que se puede ejecutar. Funciones
 * puras: se pueden probar en tests unitarios.
 */
object RoutinePlan {

    /** ¿Le afecta esta rutina lo que acaba de pasar? */
    fun matches(routine: RoutineItem, trigger: RoutineTrigger, label: String): Boolean {
        if (!routine.enabled) return false
        if (routine.trigger != trigger.name) return false
        val wanted = routine.triggerLabel.trim().lowercase()
        if (wanted.isBlank()) return true
        val actual = label.trim().lowercase()
        if (actual.isBlank()) return false
        return wanted.contains(actual) || actual.contains(wanted)
    }

    /** Rutinas que deben ejecutarse ahora. */
    fun pending(
        routines: List<RoutineItem>,
        trigger: RoutineTrigger,
        label: String,
    ): List<RoutineItem> = routines.filter { matches(it, trigger, label) }

    /** Texto de la acción, con un texto por defecto si no se indicó. */
    fun actionText(routine: RoutineItem): String {
        if (routine.payload.isNotBlank()) return routine.payload
        return when (routine.action) {
            "STOP_MUSIC" -> "La música se ha detenido."
            "PLAY_MUSIC" -> "Empiezo a poner música."
            "NOTIFY" -> "Aviso de Jarvis."
            "SPEAK" -> "Tengo algo que decirle."
            "REMIND" -> "Tengo un recordatorio para usted."
            else -> "Aviso de Jarvis."
        }
    }

    /** Descripción legible de la rutina, para la lista y para hablar. */
    fun describe(routine: RoutineItem): String {
        if (routine.trigger == "HOURLY") {
            val clock = routine.triggerLabel.trim()
            val action = when (routine.action) {
                "STOP_MUSIC" -> "quita la música"
                "PLAY_MUSIC" -> "pone música"
                "NOTIFY" -> "avisa"
                "SPEAK" -> "lo dice"
                "REMIND" -> "crea un recordatorio"
                else -> "avisa"
            }
            val payload = routine.payload.trim()
            return if (payload.isBlank()) {
                "cada día a las $clock, $action."
            } else {
                "cada día a las $clock, $action «$payload»."
            }
        }
        val trigger = when (routine.trigger) {
            "TIMER" -> "cuando suene el temporizador"
            "REMINDER" -> "cuando salte el recordatorio"
            else -> "cuando ocurra"
        }
        val label = routine.triggerLabel.trim()
        val when_ = if (label.isBlank()) {
            trigger
        } else {
            "$trigger $label"
        }
        val action = when (routine.action) {
            "STOP_MUSIC" -> "quita la música"
            "PLAY_MUSIC" -> "pone música"
            "NOTIFY" -> "avisa"
            "SPEAK" -> "lo dice"
            "REMIND" -> "crea un recordatorio"
            else -> "avisa"
        }
        val payload = routine.payload.trim()
        return if (payload.isBlank()) {
            "$when_, $action."
        } else {
            "$when_, $action «$payload»."
        }
    }
}
