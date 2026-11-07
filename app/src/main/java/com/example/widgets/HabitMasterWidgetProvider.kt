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
import com.example.data.local.entities.HabitEntity
import com.example.data.preferences.UserPreferences
import com.example.data.repository.JsonMappers
import com.example.notifications.runAsync
import com.example.utils.DateUtils
import kotlinx.coroutines.flow.first

class HabitMasterWidgetProvider : AppWidgetProvider() {

    companion object {
        private val rowIds = intArrayOf(
            R.id.habit_row_0,
            R.id.habit_row_1,
            R.id.habit_row_2,
            R.id.habit_row_3,
            R.id.habit_row_4
        )
        private val titleIds = intArrayOf(
            R.id.habit_title_0,
            R.id.habit_title_1,
            R.id.habit_title_2,
            R.id.habit_title_3,
            R.id.habit_title_4
        )
        private val checkIds = intArrayOf(
            R.id.habit_check_0,
            R.id.habit_check_1,
            R.id.habit_check_2,
            R.id.habit_check_3,
            R.id.habit_check_4
        )

        fun notifyDataChanged(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                android.content.ComponentName(context, HabitMasterWidgetProvider::class.java)
            )
            if (ids.isNotEmpty()) {
                val intent = Intent(context, HabitMasterWidgetProvider::class.java).apply {
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

            appWidgetIds.forEach { widgetId ->
                try {
                    val profileId = WidgetProfileManager.getWidgetProfileId(
                        context, widgetId, defaultProfileId
                    )
                    val profileHabits = database.habitDao().getHabitsForProfileList(profileId)
                        .sortedWith(compareBy(HabitEntity::orderIndex, HabitEntity::createdAt))
                    val habits = profileHabits.take(rowIds.size)
                    val views = RemoteViews(context.packageName, R.layout.widget_habit_master)
                    views.setTextViewText(R.id.habit_master_profile, "🌱 ${profileHabits.size} habits")

                    for (slot in rowIds.indices) {
                        if (slot < habits.size) {
                            val habit = habits[slot]
                            val isComplete = JsonMappers.parseStringList(habit.completedDatesJson).contains(today)
                            views.setViewVisibility(rowIds[slot], View.VISIBLE)
                            views.setTextViewText(
                                titleIds[slot],
                                "${habit.emoji} ${habit.title}"
                            )
                            views.setTextViewText(checkIds[slot], if (isComplete) "✓" else "○")
                            views.setOnClickPendingIntent(
                                rowIds[slot],
                                HabitMasterActionReceiver.createPendingIntent(
                                    context = context,
                                    appWidgetId = widgetId,
                                    slotIndex = slot,
                                    habitId = habit.id,
                                    profileId = profileId
                                )
                            )
                        } else {
                            views.setViewVisibility(rowIds[slot], View.GONE)
                        }
                    }
                    views.setViewVisibility(
                        R.id.habit_master_empty,
                        if (habits.isEmpty()) View.VISIBLE else View.GONE
                    )

                    val openIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("extra_profile_id", profileId)
                        data = "lifeos://widget/habit-master/$widgetId".toUri()
                    }
                    val openPendingIntent = PendingIntent.getActivity(
                        context,
                        widgetId * 100 + 52,
                        openIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.habit_master_header, openPendingIntent)
                    views.setOnClickPendingIntent(R.id.habit_master_empty, openPendingIntent)
                    appWidgetManager.updateAppWidget(widgetId, views)
                } catch (exception: Exception) {
                    android.util.Log.e(
                        "HabitMasterWidget",
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
}
