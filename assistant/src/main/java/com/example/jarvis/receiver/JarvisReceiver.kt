package com.example.jarvis.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.jarvis.JarvisApplication
import com.example.jarvis.assistant.Notifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Recibe la alarma de los temporizadores y avisa al usuario. */
class JarvisReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_TIMER) return
        val id = intent.getLongExtra(EXTRA_ID, 0L)
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

    companion object {
        const val ACTION_TIMER = "com.example.jarvis.action.TIMER"
        const val EXTRA_ID = "com.example.jarvis.extra.ID"
        const val EXTRA_LABEL = "com.example.jarvis.extra.LABEL"
    }
}
