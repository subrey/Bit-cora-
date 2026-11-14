package com.example.jarvis.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.jarvis.JarvisApplication
import com.example.jarvis.assistant.JarvisBus
import com.example.jarvis.assistant.Notifications
import com.example.jarvis.widget.JarvisWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Recibe la alarma de los temporizadores y de los recordatorios y avisa al usuario. */
class JarvisReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_ID, 0L)
        when (intent.action) {
            ACTION_TIMER -> onTimer(context, id, intent)
            ACTION_REMINDER -> onReminder(context, id, intent)
            ACTION_REMINDER_DONE -> onReminderDone(context, id)
            ACTION_REMINDER_SNOOZE -> onReminderSnooze(context, id)
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
            } catch (_: Exception) {
                // la notificación ya ha mostrado el aviso
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

    companion object {
        const val ACTION_TIMER = "com.example.jarvis.action.TIMER"
        const val ACTION_REMINDER = "com.example.jarvis.action.REMINDER"
        const val ACTION_REMINDER_DONE = "com.example.jarvis.action.REMINDER_DONE"
        const val ACTION_REMINDER_SNOOZE = "com.example.jarvis.action.REMINDER_SNOOZE"
        const val EXTRA_ID = "com.example.jarvis.extra.ID"
        const val EXTRA_LABEL = "com.example.jarvis.extra.LABEL"

        /** Minutos que se retrasa un recordatorio al pulsar «Posponer». */
        const val SNOOZE_MINUTES = 10
    }
}
