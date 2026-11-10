package com.example.bitacora.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.bitacora.MainActivity
import com.example.bitacora.NotasApplication
import com.example.bitacora.R
import com.example.bitacora.data.Note
import com.example.bitacora.ui.excerpt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Widget de pantalla de inicio con la última nota (o la última destacada).
 */
class BitacoraWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as NotasApplication
                val note = app.repository.latestNote()
                appWidgetIds.forEach { widgetId ->
                    appWidgetManager.updateAppWidget(widgetId, buildViews(context, note))
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {

        /** Pide al sistema que refresque todos los widgets de la app. */
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, BitacoraWidgetProvider::class.java)
            )
            if (ids.isEmpty()) return

            val intent = Intent(context, BitacoraWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }

        private fun buildViews(context: Context, note: Note?): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_bitacora)

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            views.setOnClickPendingIntent(
                R.id.widget_root,
                pendingActivity(context, requestCode = 0, intent = openIntent),
            )

            val newNoteIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(MainActivity.EXTRA_NEW_NOTE, true)
            }
            views.setOnClickPendingIntent(
                R.id.widget_add,
                pendingActivity(context, requestCode = 1, intent = newNoteIntent),
            )

            if (note == null) {
                views.setTextViewText(
                    R.id.widget_title,
                    context.getString(R.string.widget_empty_title),
                )
                views.setTextViewText(
                    R.id.widget_content,
                    context.getString(R.string.widget_empty_body),
                )
            } else {
                views.setTextViewText(
                    R.id.widget_title,
                    note.title.ifBlank { context.getString(R.string.widget_untitled) },
                )
                views.setTextViewText(
                    R.id.widget_content,
                    excerpt(note.content, 140).ifBlank {
                        context.getString(R.string.widget_no_content)
                    },
                )
            }

            return views
        }

        private fun pendingActivity(context: Context, requestCode: Int, intent: Intent): PendingIntent =
            PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
    }
}
