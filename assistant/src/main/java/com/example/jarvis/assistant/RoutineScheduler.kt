package com.example.jarvis.assistant

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.jarvis.receiver.JarvisReceiver

/**
 * Programa la alarma diaria de las rutinas horarias («cada mañana a las 8»).
 */
class RoutineScheduler(private val context: Context) {

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /** Programa (o cancela) la alarma diaria de una rutina horaria. */
    fun scheduleHourly(routineId: Long, hour: Int, minute: Int) {
        val pending = pendingIntent(routineId)
        val triggerAt = nextOccurrence(hour, minute)
        runCatching {
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                AlarmManager.INTERVAL_DAY,
                pending,
            )
        }
    }

    fun cancel(routineId: Long) {
        runCatching { alarmManager.cancel(pendingIntent(routineId)) }
    }

    /** Próxima ocurrencia de esa hora y minuto, a partir de ahora. */
    fun nextOccurrence(hour: Int, minute: Int): Long {
        val now = java.util.Calendar.getInstance()
        val next = (now.clone() as java.util.Calendar).apply {
            set(java.util.Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(java.util.Calendar.MINUTE, minute.coerceIn(0, 59))
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
            if (timeInMillis <= now.timeInMillis) add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        return next.timeInMillis
    }

    private fun pendingIntent(routineId: Long): PendingIntent {
        val intent = Intent(context, JarvisReceiver::class.java).apply {
            action = JarvisReceiver.ACTION_ROUTINE_HOURLY
            putExtra(JarvisReceiver.EXTRA_ID, routineId)
        }
        return PendingIntent.getBroadcast(
            context,
            routineId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
