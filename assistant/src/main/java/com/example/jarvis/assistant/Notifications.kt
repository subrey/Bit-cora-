package com.example.jarvis.assistant

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.jarvis.MainActivity
import com.example.jarvis.R
import com.example.jarvis.receiver.JarvisReceiver

object Notifications {

    const val CHANNEL_ID = "jarvis_alerts"

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Alertas de Jarvis",
            NotificationManager.IMPORTANCE_HIGH,
        )
        manager.createNotificationChannel(channel)
    }

    fun showTimer(context: Context, label: String) {
        ensureChannel(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Jarvis")
            .setContentText(label.ifBlank { "El temporizador ha terminado" })
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(label.hashCode(), notification)
        }
    }

    /**
     * Notificación de recordatorio con dos acciones rápidas: marcar como hecho
     * o posponerlo unos minutos.
     */
    fun showReminder(context: Context, reminderId: Long, text: String) {
        ensureChannel(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val done = PendingIntent.getBroadcast(
            context,
            reminderId.toInt() + DONE_OFFSET,
            Intent(context, JarvisReceiver::class.java).apply {
                action = JarvisReceiver.ACTION_REMINDER_DONE
                putExtra(JarvisReceiver.EXTRA_ID, reminderId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val snooze = PendingIntent.getBroadcast(
            context,
            reminderId.toInt() + SNOOZE_OFFSET,
            Intent(context, JarvisReceiver::class.java).apply {
                action = JarvisReceiver.ACTION_REMINDER_SNOOZE
                putExtra(JarvisReceiver.EXTRA_ID, reminderId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Recordatorio")
            .setContentText(text.ifBlank { "Tienes un recordatorio pendiente" })
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .addAction(android.R.drawable.ic_menu_save, "Hecho", done)
            .addAction(android.R.drawable.ic_menu_recent_history, "Posponer", snooze)
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(reminderId.toInt(), notification)
        }
    }

    fun cancelReminder(context: Context, reminderId: Long) {
        runCatching {
            NotificationManagerCompat.from(context).cancel(reminderId.toInt())
        }
    }

    private const val DONE_OFFSET = 50_000
    private const val SNOOZE_OFFSET = 100_000
}
