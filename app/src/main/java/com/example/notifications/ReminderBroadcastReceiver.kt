package com.example.notifications

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.local.AppDatabase
import com.example.data.preferences.UserPreferences
import com.example.data.repository.LifeRepository
import com.example.utils.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReminderBroadcastReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DAILY_REMINDER = "com.example.lifeos.ACTION_DAILY_REMINDER"
        const val ACTION_TASK_REMINDER = "com.example.lifeos.ACTION_TASK_REMINDER"
        const val ACTION_TASK_COMPLETE = "com.example.lifeos.ACTION_TASK_COMPLETE"
        const val ACTION_TASK_SNOOZE = "com.example.lifeos.ACTION_TASK_SNOOZE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val database = AppDatabase.getInstance(context.applicationContext)
        val preferences = UserPreferences(context.applicationContext)
        val repository = LifeRepository(database, preferences)

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                // Device restart: reschedule reminders
                NotificationHelper.scheduleDailyReminder(context, 8, 0)
                CoroutineScope(Dispatchers.IO).launch {
                    val todayIso = DateUtils.todayString()
                    val tasks = repository.getTasksForDateFlow(todayIso).first()
                    tasks.filter { (it.isReminderEnabled || it.reminders.isNotEmpty()) && !it.isCompletedForDate }.forEach { task ->
                        NotificationHelper.scheduleAllTaskReminders(context, task, todayIso)
                    }
                }
            }

            ACTION_DAILY_REMINDER -> {
                NotificationHelper.showDailyDigestNotification(
                    context = context,
                    title = "⚡ LifeOS Daily Momentum",
                    message = "Check off your daily habits, top priorities, and 75-Day Challenge rules for today!"
                )
            }

            ACTION_TASK_REMINDER -> {
                val taskId = intent.getLongExtra(NotificationHelper.EXTRA_TASK_ID, -1L)
                val title = intent.getStringExtra(NotificationHelper.EXTRA_TASK_TITLE) ?: "Task Reminder"
                val emoji = intent.getStringExtra(NotificationHelper.EXTRA_TASK_EMOJI) ?: "✅"
                val dateIso = intent.getStringExtra(NotificationHelper.EXTRA_DATE_ISO) ?: DateUtils.todayString()
                val dueTime = intent.getStringExtra("extra_due_time")

                if (taskId != -1L) {
                    NotificationHelper.showTaskReminderNotification(
                        context = context,
                        taskId = taskId,
                        title = title,
                        emoji = emoji,
                        dueTime = dueTime,
                        dateIso = dateIso
                    )
                }
            }

            ACTION_TASK_COMPLETE -> {
                val taskId = intent.getLongExtra(NotificationHelper.EXTRA_TASK_ID, -1L)
                val dateIso = intent.getStringExtra(NotificationHelper.EXTRA_DATE_ISO) ?: DateUtils.todayString()

                if (taskId != -1L) {
                    // Dismiss notification
                    val notificationId = (taskId.toInt() * 31) + dateIso.hashCode().and(0x7FFFFFFF) % 10000
                    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    nm.cancel(notificationId)

                    // Mark complete in repository
                    CoroutineScope(Dispatchers.IO).launch {
                        repository.toggleTaskCompletion(taskId, dateIso)
                    }
                }
            }

            ACTION_TASK_SNOOZE -> {
                val taskId = intent.getLongExtra(NotificationHelper.EXTRA_TASK_ID, -1L)
                val title = intent.getStringExtra(NotificationHelper.EXTRA_TASK_TITLE) ?: "Task Reminder"
                val emoji = intent.getStringExtra(NotificationHelper.EXTRA_TASK_EMOJI) ?: "✅"
                val dateIso = intent.getStringExtra(NotificationHelper.EXTRA_DATE_ISO) ?: DateUtils.todayString()

                if (taskId != -1L) {
                    // Dismiss active notification
                    val notificationId = (taskId.toInt() * 31) + dateIso.hashCode().and(0x7FFFFFFF) % 10000
                    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    nm.cancel(notificationId)

                    // Schedule alarm 15 minutes from now
                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                    if (alarmManager != null) {
                        val snoozeTrigger = System.currentTimeMillis() + (15 * 60 * 1000L)
                        val snoozeIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
                            action = ACTION_TASK_REMINDER
                            putExtra(NotificationHelper.EXTRA_TASK_ID, taskId)
                            putExtra(NotificationHelper.EXTRA_TASK_TITLE, title)
                            putExtra(NotificationHelper.EXTRA_TASK_EMOJI, emoji)
                            putExtra(NotificationHelper.EXTRA_DATE_ISO, dateIso)
                        }
                        val pending = PendingIntent.getBroadcast(
                            context,
                            notificationId + 300000,
                            snoozeIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            if (alarmManager.canScheduleExactAlarms()) {
                                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeTrigger, pending)
                            } else {
                                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeTrigger, pending)
                            }
                        } else {
                            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeTrigger, pending)
                        }
                    }
                }
            }
        }
    }
}
