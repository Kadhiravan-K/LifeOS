package com.example.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import androidx.core.net.toUri
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.preferences.UserPreferences
import com.example.data.repository.JsonMappers
import com.example.notifications.runAsync
import com.example.utils.DateUtils
import kotlinx.coroutines.flow.first

class ProductivityAnalysisWidgetProvider : AppWidgetProvider() {

    companion object {
        fun notifyDataChanged(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                android.content.ComponentName(context, ProductivityAnalysisWidgetProvider::class.java)
            )
            if (ids.isNotEmpty()) {
                val intent = Intent(context, ProductivityAnalysisWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
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
        runAsync(timeoutMs = 9_000L) {
            val database = AppDatabase.getInstance(context.applicationContext)
            val preferences = UserPreferences(context.applicationContext)
            val today = DateUtils.todayString()
            val defaultProfileId = preferences.activeProfileId.first()
            val waterGoal = preferences.waterGoalMl.first().coerceAtLeast(1)

            appWidgetIds.forEach { widgetId ->
                try {
                    val profileId = WidgetProfileManager.getWidgetProfileId(
                        context, widgetId, defaultProfileId
                    )
                    val tasks = database.taskDao().getTasksForProfileList(profileId)
                        .filter { !it.isArchived && it.dueDateIso == today }
                    val habits = database.habitDao().getHabitsForProfileList(profileId)
                    val tasksDone = tasks.count {
                        it.isOneTimeCompleted ||
                            JsonMappers.parseStringList(it.completedDatesJson).contains(today)
                    }
                    val habitsDone = habits.count {
                        JsonMappers.parseStringList(it.completedDatesJson).contains(today)
                    }
                    val waterAmount = database.waterLogDao().getWaterLog(today)?.amountMl ?: 0
                    val taskPercent = percent(tasksDone, tasks.size)
                    val habitPercent = percent(habitsDone, habits.size)
                    val waterPercent = ((waterAmount * 100) / waterGoal).coerceIn(0, 100)

                    val views = RemoteViews(context.packageName, R.layout.widget_productivity_analysis)
                    views.setTextViewText(R.id.analysis_task_value, "$tasksDone / ${tasks.size}")
                    views.setTextViewText(R.id.analysis_habit_value, "$habitsDone / ${habits.size}")
                    views.setTextViewText(R.id.analysis_water_value, "$waterAmount / $waterGoal ml")
                    views.setProgressBar(R.id.analysis_task_progress, 100, taskPercent, false)
                    views.setProgressBar(R.id.analysis_habit_progress, 100, habitPercent, false)
                    views.setProgressBar(R.id.analysis_water_progress, 100, waterPercent, false)
                    views.setViewVisibility(
                        R.id.analysis_empty,
                        if (tasks.isEmpty() && habits.isEmpty() && waterAmount == 0) View.VISIBLE else View.GONE
                    )

                    val openIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("extra_destination", "analytics")
                        putExtra("extra_profile_id", profileId)
                        data = "lifeos://widget/productivity-analysis/$widgetId".toUri()
                    }
                    val openPendingIntent = PendingIntent.getActivity(
                        context,
                        widgetId * 100 + 51,
                        openIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.analysis_root, openPendingIntent)
                    appWidgetManager.updateAppWidget(widgetId, views)
                } catch (exception: Exception) {
                    android.util.Log.e(
                        "ProductivityAnalysisWidget",
                        "Unable to update widget id $widgetId",
                        exception
                    )
                }
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        appWidgetIds.forEach { WidgetProfileManager.removeWidget(context, it) }
    }

    private fun percent(completed: Int, total: Int): Int =
        if (total == 0) 0 else (completed * 100 / total).coerceIn(0, 100)
}
