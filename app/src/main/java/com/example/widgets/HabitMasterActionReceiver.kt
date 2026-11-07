package com.example.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.example.data.local.AppDatabase
import com.example.data.preferences.UserPreferences
import com.example.data.repository.LifeRepository
import com.example.notifications.runAsync
import com.example.ui.theme.engine.LocalPremiumEntitlementRepository
import com.example.utils.DateUtils
import kotlinx.coroutines.flow.first

class HabitMasterActionReceiver : BroadcastReceiver() {

    companion object {
        private const val ACTION_TOGGLE_HABIT = "com.example.lifeos.ACTION_HABIT_MASTER_TOGGLE"
        private const val EXTRA_HABIT_ID = "extra_habit_id"
        private const val EXTRA_WIDGET_ID = "extra_habit_widget_id"
        private const val EXTRA_PROFILE_ID = "extra_habit_profile_id"

        fun createPendingIntent(
            context: Context,
            appWidgetId: Int,
            slotIndex: Int,
            habitId: Long,
            profileId: Long
        ): PendingIntent {
            val intent = Intent(context, HabitMasterActionReceiver::class.java).apply {
                action = ACTION_TOGGLE_HABIT
                putExtra(EXTRA_HABIT_ID, habitId)
                putExtra(EXTRA_WIDGET_ID, appWidgetId)
                putExtra(EXTRA_PROFILE_ID, profileId)
                data = "lifeos://widget/habit-master/$appWidgetId/$slotIndex/$habitId".toUri()
            }
            val requestCode = ((appWidgetId.toLong() * 31 + habitId).toInt() and Int.MAX_VALUE)
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_TOGGLE_HABIT) return
        if (intent.data?.scheme != "lifeos") {
            android.util.Log.w("HabitMasterAction", "Ignoring habit action with an invalid URI scheme")
            return
        }

        val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
        val appWidgetId = intent.getIntExtra(EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val profileId = intent.getLongExtra(EXTRA_PROFILE_ID, -1L)
        if (habitId <= 0L || appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID || profileId <= 0L) {
            android.util.Log.w("HabitMasterAction", "Ignoring habit action with invalid identifiers")
            return
        }

        runAsync(timeoutMs = 9_000L) {
            val appContext = context.applicationContext
            val database = AppDatabase.getInstance(appContext)
            val preferences = UserPreferences(appContext)
            val activeProfileId = preferences.activeProfileId.first()
            val configuredProfileId = WidgetProfileManager.getWidgetProfileId(
                appContext,
                appWidgetId,
                activeProfileId
            )
            val habit = database.habitDao().getHabitById(habitId)
            if (habit == null || habit.profileId != profileId ||
                configuredProfileId != profileId
            ) {
                android.util.Log.w(
                    "HabitMasterAction",
                    "Ignoring stale or mismatched habit action for widget $appWidgetId and habit $habitId"
                )
                return@runAsync
            }

            val repository = LifeRepository(
                database,
                preferences,
                LocalPremiumEntitlementRepository(appContext)
            )
            repository.toggleHabitCompletion(habitId, DateUtils.todayString())
            WidgetRefreshCoordinator.notifyAllWidgetsChanged(appContext)
        }
    }
}
