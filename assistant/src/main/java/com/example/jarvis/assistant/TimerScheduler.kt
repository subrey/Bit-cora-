package com.example.jarvis.assistant

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.jarvis.receiver.JarvisReceiver

/** Programa y cancela temporizadores/recordatorios con AlarmManager. */
class TimerScheduler(private val context: Context) {

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(timerId: Long, label: String, durationSeconds: Int) {
        val pending = pendingIntent(timerId, label)
        val triggerAt = System.currentTimeMillis() + durationSeconds * 1000L
        runCatching {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            }
        }
    }

    fun cancel(timerId: Long) {
        runCatching { alarmManager.cancel(pendingIntent(timerId, "Temporizador")) }
    }

    private fun pendingIntent(timerId: Long, label: String): PendingIntent {
        val intent = Intent(context, JarvisReceiver::class.java).apply {
            action = JarvisReceiver.ACTION_TIMER
            putExtra(JarvisReceiver.EXTRA_ID, timerId)
            putExtra(JarvisReceiver.EXTRA_LABEL, label)
        }
        return PendingIntent.getBroadcast(
            context,
            timerId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
