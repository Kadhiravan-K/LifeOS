package com.example.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.entities.TrackerEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TrackerWidgetProvider : AppWidgetProvider() {

    companion object {
        fun notifyDataChanged(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, TrackerWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                val intent = Intent(context, TrackerWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(intent)
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getInstance(context)
                val list = database.trackerDao().getAllTrackersList().filter { !it.isArchived }

                val summaryText = if (list.isEmpty()) {
                    "No active trackers.\nTap to setup essentials in LifeOS."
                } else {
                    list.take(4).joinToString("\n\n") { t ->
                        val stockStr = if (t.currentStock > 0) " • 📦 ${t.currentStock}" else ""
                        val targetStr = if (t.trackDistance) {
                            t.distanceUnit.lowercase()
                        } else if (t.trackDuration) {
                            "Duration cycle"
                        } else {
                            "Active"
                        }
                        "${t.emoji} ${t.title}${stockStr}\n   $targetStr"
                    }
                }

                for (widgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_essentials_tracker)
                    views.setTextViewText(R.id.widget_tracker_content, summaryText)

                    val openIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        widgetId,
                        openIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_tracker_root, pendingIntent)

                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
