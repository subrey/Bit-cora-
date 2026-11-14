package com.example.jarvis.assistant

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.jarvis.MainActivity
import com.example.jarvis.receiver.JarvisReceiver
import java.util.Calendar

/** Programa y cancela alarmas del despertador con AlarmManager. */
class AlarmScheduler(private val context: Context) {

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(alarmId: Long, triggerAtMillis: Long) {
        val fire = if (triggerAtMillis <= System.currentTimeMillis()) {
            triggerAtMillis + 60_000L
        } else triggerAtMillis
        val pending = pendingIntent(alarmId)
        val show = showIntent(alarmId)
        runCatching {
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(fire, show), pending)
        }.onFailure {
            // Fallback si setAlarmClock no está disponible por alguna razón
            runCatching {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fire, pending)
            }
        }
    }

    fun cancel(alarmId: Long) {
        runCatching { alarmManager.cancel(pendingIntent(alarmId)) }
    }

    /** Calcula la siguiente ocurrencia para una hora dada (hoy o mañana). */
    fun nextOccurrence(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val next = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now.timeInMillis) add(Calendar.DAY_OF_YEAR, 1)
        }
        return next.timeInMillis
    }

    private fun pendingIntent(alarmId: Long): PendingIntent {
        val intent = Intent(context, JarvisReceiver::class.java).apply {
            action = JarvisReceiver.ACTION_ALARM
            putExtra(JarvisReceiver.EXTRA_ID, alarmId)
        }
        val request = (ALARM_OFFSET + alarmId).toInt()
        return PendingIntent.getBroadcast(
            context,
            request,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun showIntent(alarmId: Long): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_ALARM_ID, alarmId)
        }
        val request = (ALARM_SHOW_OFFSET + alarmId).toInt()
        return PendingIntent.getActivity(
            context,
            request,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val ALARM_OFFSET = 300_000
        const val ALARM_SHOW_OFFSET = 400_000
        const val SNOOZE_MINUTES = 9
    }
}