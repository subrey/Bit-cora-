package com.example.jarvis.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.jarvis.JarvisApplication
import com.example.jarvis.MainActivity
import com.example.jarvis.R
import com.example.jarvis.assistant.JarvisBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Widget de escritorio de JARVIS: estado del asistente, próximo aviso y dos
 * acciones rápidas (hablar y poner/quitar la música).
 */
class JarvisWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE_MUSIC) {
            toggleMusic(context)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as? JarvisApplication ?: return@launch
                val summary = WidgetSummary.build(
                    listening = JarvisBus.listening.value,
                    speaking = JarvisBus.speaking.value,
                    reminder = app.repository.nextReminder(),
                    timer = app.repository.nextTimer(),
                    pendingTasks = app.repository.pendingTodoCount(),
                    pendingReminders = app.repository.pendingReminderCount(),
                )
                appWidgetIds.forEach { widgetId ->
                    appWidgetManager.updateAppWidget(
                        widgetId,
                        buildViews(context, summary),
                    )
                }
            } catch (_: Exception) {
                // un fallo al pintar el widget no debe romper nada
            } finally {
                pendingResult.finish()
            }
        }
    }

    /** El botón de música pone o quita la reproducción sin abrir la app. */
    private fun toggleMusic(context: Context) {
        val app = context.applicationContext as? JarvisApplication ?: return
        runCatching {
            if (app.musicPlayer.state.value.isPlaying) {
                app.musicPlayer.stop()
            } else {
                // El escaneo de la biblioteca va en segundo plano.
                app.musicPlayer.playRandom()
            }
        }
        updateAll(context)
    }

    companion object {

        /** Acción del botón de música. */
        const val ACTION_TOGGLE_MUSIC = "com.example.jarvis.action.TOGGLE_MUSIC"

        /** Pide al sistema que refresque todos los widgets de la app. */
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, JarvisWidgetProvider::class.java)
            )
            if (ids.isEmpty()) return

            val intent = Intent(context, JarvisWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }

        private fun buildViews(context: Context, summary: WidgetState): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_jarvis)

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            views.setOnClickPendingIntent(
                R.id.widget_root,
                pendingActivity(context, requestCode = 0, intent = openIntent),
            )

            val micIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(MainActivity.EXTRA_START_LISTENING, true)
            }
            views.setOnClickPendingIntent(
                R.id.widget_mic,
                pendingActivity(context, requestCode = 1, intent = micIntent),
            )

            val musicIntent = Intent(context, JarvisWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE_MUSIC
            }
            views.setOnClickPendingIntent(
                R.id.widget_music,
                pendingBroadcast(context, requestCode = 2, intent = musicIntent),
            )

            views.setTextViewText(R.id.widget_status, summary.status)
            views.setTextViewText(R.id.widget_next, summary.nextText)
            views.setTextViewText(R.id.widget_counts, summary.countsText)

            return views
        }

        private fun pendingActivity(
            context: Context,
            requestCode: Int,
            intent: Intent,
        ): PendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        private fun pendingBroadcast(
            context: Context,
            requestCode: Int,
            intent: Intent,
        ): PendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
