package com.example.jarvis.assistant

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.jarvis.receiver.JarvisReceiver

/**
 * Programa y cancela recordatorios con fecha y hora exactas usando
 * `AlarmManager`. A diferencia de [TimerScheduler], aquí el momento del aviso
 * es una fecha absoluta, no una duración.
 */
class ReminderScheduler(private val context: Context) {

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(reminderId: Long, text: String, triggerAtMillis: Long) {
        val pending = pendingIntent(reminderId, text)
        runCatching {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pending,
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pending,
                )
            }
        }
    }

    fun cancel(reminderId: Long) {
        runCatching { alarmManager.cancel(pendingIntent(reminderId, "Recordatorio")) }
    }

    private fun pendingIntent(reminderId: Long, text: String): PendingIntent {
        val intent = Intent(context, JarvisReceiver::class.java).apply {
            action = JarvisReceiver.ACTION_REMINDER
            putExtra(JarvisReceiver.EXTRA_ID, reminderId)
            putExtra(JarvisReceiver.EXTRA_LABEL, text)
        }
        return PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
