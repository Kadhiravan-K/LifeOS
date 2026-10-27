package com.example.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.RemoteViews
import androidx.core.net.toUri
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.preferences.UserPreferences
import com.example.data.repository.LifeRepository
import com.example.ui.theme.engine.LocalPremiumEntitlementRepository
import com.example.utils.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class QuickRemindersWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_WIDGET_TOGGLE_TASK = "com.example.lifeos.ACTION_WIDGET_TOGGLE_TASK"

        fun notifyDataChanged(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, QuickRemindersWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list_view)
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateAppWidget(context, appWidgetManager, appWidgetId)
        notifyDataChanged(context)
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_quick_reminders)

        // Bind RemoteViewsService to ListView
        val serviceIntent = Intent(context, QuickRemindersRemoteViewsService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
        }
        views.setRemoteAdapter(R.id.widget_list_view, serviceIntent)
        views.setEmptyView(R.id.widget_list_view, R.id.widget_empty_view)

        // PendingIntent template for clicking on QR tiles
        val clickIntent = Intent(context, QuickRemindersWidgetProvider::class.java).apply {
            action = ACTION_WIDGET_TOGGLE_TASK
            data = "lifeos://widget/qr/click/$appWidgetId".toUri()
        }
        val clickPendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId,
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        views.setPendingIntentTemplate(R.id.widget_list_view, clickPendingIntent)

        // Header click to open app
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            1,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_header, openAppPendingIntent)
        views.setOnClickPendingIntent(R.id.widget_empty_view, openAppPendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_WIDGET_TOGGLE_TASK) {
            val type = intent.getStringExtra("extra_type") ?: "TASK"
            val taskId = intent.getLongExtra("extra_task_id", -1L)
            val targetId = intent.getStringExtra("extra_target_id") ?: ""

            val db = AppDatabase.getInstance(context)
            val prefs = UserPreferences(context)
            val entitlementRepo = LocalPremiumEntitlementRepository(context)
            val repository = LifeRepository(db, prefs, entitlementRepo)
            val todayIso = DateUtils.todayString()

            CoroutineScope(Dispatchers.IO).launch {
                when (type) {
                    "TASK" -> {
                        if (taskId != -1L) {
                            repository.toggleTaskCompletion(taskId, todayIso)
                            notifyDataChanged(context)
                        } else {
                            val openIntent = Intent(context, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            }
                            context.startActivity(openIntent)
                        }
                    }
                    "HABIT" -> {
                        val habitId = targetId.toLongOrNull()
                        if (habitId != null) {
                            repository.toggleHabitCompletion(habitId, todayIso)
                            notifyDataChanged(context)
                        } else {
                            val habits = db.habitDao().getAllHabitsList()
                            val matched = habits.find { it.title.equals(targetId, ignoreCase = true) }
                            if (matched != null) {
                                repository.toggleHabitCompletion(matched.id, todayIso)
                                notifyDataChanged(context)
                            }
                        }
                    }
                    "WATER" -> {
                        repository.addWater(250, todayIso)
                        notifyDataChanged(context)
                    }
                    "CHALLENGE_RULE" -> {
                        if (targetId.isNotBlank()) {
                            repository.toggleChallengeRule(targetId, todayIso)
                            notifyDataChanged(context)
                        }
                    }
                    else -> {
                        val openIntent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        context.startActivity(openIntent)
                    }
                }
            }
        }
    }
}
