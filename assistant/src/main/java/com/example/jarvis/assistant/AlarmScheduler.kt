package com.example.jarvis.assistant

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.jarvis.receiver.JarvisReceiver
import java.util.Calendar

/**
 * Programa las alarmas del despertador con `AlarmManager.setAlarmClock`: el
 * sistema las trata como alarmas de verdad (icono en la barra de estado y
 * salida del modo de ahorro de energía).
 */
class AlarmScheduler(private val context: Context) {

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /** Programa una alarma diaria a esa hora. */
    fun scheduleDaily(alarmId: Long, hour: Int, minute: Int) {
        val triggerAt = nextOccurrence(hour, minute)
        setClock(alarmId, triggerAt)
    }

    /** Programa una alarma para un instante concreto. */
    fun scheduleOnce(alarmId: Long, triggerAtMillis: Long) {
        setClock(alarmId, triggerAtMillis)
    }

    /** Programa la siguiente ocurrencia de una alarma diaria. */
    fun reschedule(alarmId: Long, hour: Int, minute: Int) {
        scheduleDaily(alarmId, hour, minute)
    }

    fun cancel(alarmId: Long) {
        runCatching { alarmManager.cancel(pendingIntent(alarmId)) }
    }

    private fun setClock(alarmId: Long, triggerAtMillis: Long) {
        val pending = pendingIntent(alarmId)
        val show = showIntent(alarmId)
        runCatching {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerAtMillis, show),
                pending,
            )
        }
    }

    /** Próxima vez que suena esa hora y minuto. */
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
        return PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** Al abrir la app, el sistema muestra el icono de «hay una alarma puesta». */
    private fun showIntent(alarmId: Long): PendingIntent {
        val intent = Intent(context, com.example.jarvis.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(com.example.jarvis.MainActivity.EXTRA_ALARM_ID, alarmId)
        }
        return PendingIntent.getActivity(
            context,
            alarmId.toInt() + SHOW_OFFSET,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private companion object {
        /** Separa los identificadores de la alarma y de su icono. */
        const val SHOW_OFFSET = 200_000
    }
}
