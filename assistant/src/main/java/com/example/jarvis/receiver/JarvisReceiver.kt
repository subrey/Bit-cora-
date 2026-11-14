package com.example.jarvis.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.jarvis.JarvisApplication
import com.example.jarvis.assistant.Agenda
import com.example.jarvis.assistant.JarvisBus
import com.example.jarvis.assistant.Notifications
import com.example.jarvis.assistant.RoutineAction
import com.example.jarvis.assistant.RoutinePlan
import com.example.jarvis.assistant.RoutineTrigger
import com.example.jarvis.data.RoutineItem
import com.example.jarvis.widget.JarvisWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

/** Recibe la alarma de los temporizadores y de los recordatorios y avisa al usuario. */
class JarvisReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_ID, 0L)
        when (intent.action) {
            ACTION_TIMER -> onTimer(context, id, intent)
            ACTION_REMINDER -> onReminder(context, id, intent)
            ACTION_REMINDER_DONE -> onReminderDone(context, id)
            ACTION_REMINDER_SNOOZE -> onReminderSnooze(context, id)
            ACTION_ROUTINE_HOURLY -> onHourlyRoutine(context, id)
        }
    }

    private fun onTimer(context: Context, id: Long, intent: Intent) {
        val label = intent.getStringExtra(EXTRA_LABEL) ?: "Temporizador"
        val app = context.applicationContext as? JarvisApplication ?: return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val timer = app.repository.getTimer(id)
                if (timer != null && !timer.fired) {
                    app.repository.markTimerFired(timer)
                    app.scheduler.cancel(id)
                }
                Notifications.showTimer(context, label)
                app.speaker.speak("El temporizador ha terminado: $label")
                // Las rutinas que escuchan a los temporizadores.
                runRoutines(app, context, RoutineTrigger.TIMER, label)
            } catch (_: Exception) {
                // si algo falla, al menos la notificación de arriba se ha mostrado
            } finally {
                pending.finish()
            }
        }
    }

    /** Salta el recordatorio: avisa con notificación y con la voz. */
    private fun onReminder(context: Context, id: Long, intent: Intent) {
        val label = intent.getStringExtra(EXTRA_LABEL) ?: "Recordatorio"
        val app = context.applicationContext as? JarvisApplication ?: return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                Notifications.showReminder(context, id, label)
                // El aviso ya ha salido: deja de estar pendiente. Si el usuario
                // lo pospone, vuelve a la lista.
                app.repository.getReminder(id)?.let { reminder ->
                    if (!reminder.done) app.repository.markReminderDone(reminder)
                }
                // Mientras JARVIS habla se pausa el micrófono del servicio:
                // así no se oye a sí mismo.
                JarvisBus.speaking.value = true
                app.speaker.speak("Recordatorio, señor: $label")
                // Las rutinas que escuchan a los recordatorios.
                runRoutines(app, context, RoutineTrigger.REMINDER, label)
            } catch (_: Exception) {
                // la notificación ya ha mostrado el avis o
            } finally {
                JarvisBus.speaking.value = false
                JarvisWidgetProvider.updateAll(context)
                pending.finish()
            }
        }
    }

    /** Acción «Hecho»: el recordatorio se marca como completado. */
    private fun onReminderDone(context: Context, id: Long) {
        val app = context.applicationContext as? JarvisApplication ?: return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val reminder = app.repository.getReminder(id)
                if (reminder != null && !reminder.done) {
                    app.repository.markReminderDone(reminder)
                }
                app.reminderScheduler.cancel(id)
                Notifications.cancelReminder(context, id)
            } catch (_: Exception) {
                // nada que hacer
            } finally {
                JarvisWidgetProvider.updateAll(context)
                pending.finish()
            }
        }
    }

    /** Acción «Posponer»: se retrasa el recordatorio unos minutos. */
    private fun onReminderSnooze(context: Context, id: Long) {
        val app = context.applicationContext as? JarvisApplication ?: return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val reminder = app.repository.getReminder(id)
                if (reminder != null) {
                    val newTime = System.currentTimeMillis() + SNOOZE_MINUTES * 60_000L
                    app.repository.updateReminder(reminder.copy(triggerAt = newTime, done = false))
                    app.reminderScheduler.schedule(id, reminder.text, newTime)
                }
                Notifications.cancelReminder(context, id)
            } catch (_: Exception) {
                // nada que hacer
            } finally {
                JarvisWidgetProvider.updateAll(context)
                pending.finish()
            }
        }
    }

    /** Rutina horaria: «cada mañana a las 8». */
    private fun onHourlyRoutine(context: Context, id: Long) {
        val app = context.applicationContext as? JarvisApplication ?: return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val routine = app.repository.getRoutine(id) ?: return@launch
                if (!routine.enabled) return@launch
                execute(app, context, routine)
            } catch (_: Exception) {
                // nada que hacer
            } finally {
                pending.finish()
            }
        }
    }

    /** Ejecuta las rutinas que corresponden a lo que acaba de pasar. */
    private suspend fun runRoutines(
        app: JarvisApplication,
        context: Context,
        trigger: RoutineTrigger,
        label: String,
    ) {
        val routines = app.repository.activeRoutines(trigger.name)
        val matching = RoutinePlan.pending(routines, trigger, label)
        matching.forEach { routine -> execute(app, context, routine) }
    }

    /** Ejecuta una rutina concreta. */
    private suspend fun execute(app: JarvisApplication, context: Context, routine: RoutineItem) {
        val action = runCatching { RoutineAction.valueOf(routine.action) }.getOrNull() ?: return
        val text = routineText(app, routine)
        when (action) {
            RoutineAction.STOP_MUSIC -> {
                app.musicPlayer.stop()
                app.musicPlayer.restoreAfterWakeWord()
            }

            RoutineAction.PLAY_MUSIC -> {
                val track = app.musicPlayer.scanLibrary().randomOrNull()
                if (track != null) app.musicPlayer.play(track)
            }

            RoutineAction.NOTIFY -> Notifications.showTimer(context, text)

            RoutineAction.SPEAK -> {
                JarvisBus.speaking.value = true
                app.speaker.speak(text)
                JarvisBus.speaking.value = false
            }

            RoutineAction.REMIND -> {
                val parsed = com.example.jarvis.assistant.ReminderParser.parse(text)
                if (parsed != null) {
                    val reminderId = app.repository.addReminder(parsed.message, parsed.triggerAtMillis)
                    app.reminderScheduler.schedule(reminderId, parsed.message, parsed.triggerAtMillis)
                }
            }
        }
        JarvisWidgetProvider.updateAll(context)
    }

    /**
     * Texto de la rutina. Si pide el resumen, se construye con lo que hay
     * pendiente hoy (tareas y avisos).
     */
    private suspend fun routineText(app: JarvisApplication, routine: RoutineItem): String {
        val payload = routine.payload.trim()
        if (!payload.contains("resumen", ignoreCase = true)) {
            return RoutinePlan.actionText(routine)
        }
        val todos = app.repository.observeTodos().first()
        val reminders = app.repository.observeReminders().first()
        return Agenda.summary(
            todos = todos,
            reminders = reminders,
            now = java.time.LocalDate.now(),
        )
    }

    companion object {
        const val ACTION_TIMER = "com.example.jarvis.action.TIMER"
        const val ACTION_REMINDER = "com.example.jarvis.action.REMINDER"
        const val ACTION_REMINDER_DONE = "com.example.jarvis.action.REMINDER_DONE"
        const val ACTION_REMINDER_SNOOZE = "com.example.jarvis.action.REMINDER_SNOOZE"
        const val ACTION_ROUTINE_HOURLY = "com.example.jarvis.action.ROUTINE_HOURLY"
        const val EXTRA_ID = "com.example.jarvis.extra.ID"
        const val EXTRA_LABEL = "com.example.jarvis.extra.LABEL"

        /** Minutos que se retrasa un recordatorio al pulsar «Posponer». */
        const val SNOOZE_MINUTES = 10
    }
}
