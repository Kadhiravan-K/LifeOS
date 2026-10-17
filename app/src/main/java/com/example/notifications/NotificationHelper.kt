package com.example.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskReminderItem
import com.example.utils.DateUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object NotificationHelper {

    const val CHANNEL_TASK_REMINDERS = "lifeos_task_reminders"
    const val CHANNEL_DAILY_MOMENTUM = "lifeos_discipline_channel"

    const val EXTRA_TASK_ID = "extra_task_id"
    const val EXTRA_TASK_TITLE = "extra_task_title"
    const val EXTRA_TASK_EMOJI = "extra_task_emoji"
    const val EXTRA_DATE_ISO = "extra_date_iso"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Task Reminders Channel (High importance for user-requested reminders)
            val taskChannel = NotificationChannel(
                CHANNEL_TASK_REMINDERS,
                "LifeOS Task Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for scheduled tasks and habits"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(taskChannel)

            // Daily Momentum Digest Channel
            val dailyChannel = NotificationChannel(
                CHANNEL_DAILY_MOMENTUM,
                "LifeOS Daily Momentum",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily habit & priority check-in"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(dailyChannel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showTaskReminderNotification(
        context: Context,
        taskId: Long,
        title: String,
        emoji: String,
        dueTime: String?,
        dateIso: String
    ) {
        if (!hasNotificationPermission(context)) return
        createNotificationChannels(context)

        val notificationId = (taskId.toInt() * 31) + dateIso.hashCode().and(0x7FFFFFFF) % 10000

        // 1. Open App Intent
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. Complete Action Intent
        val completeIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_TASK_COMPLETE
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_DATE_ISO, dateIso)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 100000,
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Snooze Action Intent (15 min snooze)
        val snoozeIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_TASK_SNOOZE
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TASK_TITLE, title)
            putExtra(EXTRA_TASK_EMOJI, emoji)
            putExtra(EXTRA_DATE_ISO, dateIso)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 200000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timeText = if (!dueTime.isNullOrBlank()) "Scheduled for $dueTime" else "Today's priority"

        val notification = NotificationCompat.Builder(context, CHANNEL_TASK_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("$emoji $title")
            .setContentText(timeText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openPendingIntent)
            .setAutoCancel(true)
            .addAction(android.R.drawable.checkbox_on_background, "✅ Complete", completePendingIntent)
            .addAction(android.R.drawable.ic_menu_recent_history, "⏰ Snooze 15m", snoozePendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }

    fun scheduleTaskAlarm(
        context: Context,
        task: TaskItem,
        dateIso: String,
        quietHoursStartMins: Int = 21 * 60, // 9:00 PM
        quietHoursEndMins: Int = 5 * 60 + 30 // 5:30 AM
    ) {
        if (!task.isReminderEnabled || task.dueTime.isNullOrBlank()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val taskStartMins = DateUtils.timeToMinutes(task.dueTime)
        if (taskStartMins < 0) return

        // Calculate reminder trigger time
        var triggerMinutes = taskStartMins - task.reminderMinutesBefore
        if (triggerMinutes < 0) triggerMinutes = 0

        // Quiet Hours check: if trigger falls in quiet hours, postpone to quiet hours end
        if (triggerMinutes >= quietHoursStartMins || triggerMinutes < quietHoursEndMins) {
            triggerMinutes = quietHoursEndMins
        }

        val triggerCal = Calendar.getInstance().apply {
            try {
                val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateIso) ?: Date()
                time = parsed
            } catch (e: Exception) {}
            set(Calendar.HOUR_OF_DAY, triggerMinutes / 60)
            set(Calendar.MINUTE, triggerMinutes % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Do not schedule if time has already passed
        if (triggerCal.timeInMillis <= System.currentTimeMillis()) {
            return
        }

        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_TASK_REMINDER
            putExtra(EXTRA_TASK_ID, task.id)
            putExtra(EXTRA_TASK_TITLE, task.title)
            putExtra(EXTRA_TASK_EMOJI, task.emoji)
            putExtra(EXTRA_DATE_ISO, dateIso)
            putExtra("extra_due_time", task.dueTime)
        }

        val requestCode = (task.id.toInt() * 31) + dateIso.hashCode().and(0x7FFFFFFF) % 50000
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerCal.timeInMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerCal.timeInMillis, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerCal.timeInMillis, pendingIntent)
            }
        } catch (e: Exception) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerCal.timeInMillis, pendingIntent)
        }
    }

    fun cancelTaskAlarm(context: Context, taskId: Long, dateIso: String = DateUtils.todayString()) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_TASK_REMINDER
        }
        val requestCode = (taskId.toInt() * 31) + dateIso.hashCode().and(0x7FFFFFFF) % 50000
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun scheduleTaskReminderAlarm(
        context: Context,
        task: TaskItem,
        reminder: TaskReminderItem,
        dateIso: String,
        quietHoursStartMins: Int = 21 * 60,
        quietHoursEndMins: Int = 5 * 60 + 30
    ) {
        if (!reminder.isEnabled || task.dueTime.isNullOrBlank()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val taskStartMins = DateUtils.timeToMinutes(task.dueTime)
        if (taskStartMins < 0) return

        var triggerMinutes = when (reminder.type) {
            "SPECIFIC_TIME" -> reminder.customTime?.let { DateUtils.timeToMinutes(it) } ?: taskStartMins
            else -> taskStartMins - reminder.minutesBefore
        }
        if (triggerMinutes < 0) triggerMinutes = 0

        // Quiet Hours check
        if (triggerMinutes >= quietHoursStartMins || triggerMinutes < quietHoursEndMins) {
            triggerMinutes = quietHoursEndMins
        }

        val triggerCal = Calendar.getInstance().apply {
            try {
                val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateIso) ?: Date()
                time = parsed
            } catch (e: Exception) {}
            set(Calendar.HOUR_OF_DAY, triggerMinutes / 60)
            set(Calendar.MINUTE, triggerMinutes % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (triggerCal.timeInMillis <= System.currentTimeMillis()) return

        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_TASK_REMINDER
            putExtra(EXTRA_TASK_ID, task.id)
            putExtra(EXTRA_TASK_TITLE, task.title)
            putExtra(EXTRA_TASK_EMOJI, task.emoji)
            putExtra(EXTRA_DATE_ISO, dateIso)
            putExtra("extra_due_time", task.dueTime)
            putExtra("extra_reminder_id", reminder.id)
        }

        val requestCode = ((task.id.toInt() * 31 + reminder.id.toInt()) * 31) + dateIso.hashCode().and(0x7FFFFFFF) % 100000
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerCal.timeInMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerCal.timeInMillis, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerCal.timeInMillis, pendingIntent)
            }
        } catch (e: Exception) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerCal.timeInMillis, pendingIntent)
        }
    }

    fun scheduleAllTaskReminders(
        context: Context,
        task: TaskItem,
        dateIso: String
    ) {
        if (task.reminders.isNotEmpty()) {
            task.reminders.forEach { reminder ->
                scheduleTaskReminderAlarm(context, task, reminder, dateIso)
            }
        } else if (task.isReminderEnabled) {
            scheduleTaskAlarm(context, task, dateIso)
        }
    }

    fun cancelAllTaskReminders(
        context: Context,
        task: TaskItem,
        dateIso: String = DateUtils.todayString()
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        task.reminders.forEach { reminder ->
            val requestCode = ((task.id.toInt() * 31 + reminder.id.toInt()) * 31) + dateIso.hashCode().and(0x7FFFFFFF) % 100000
            val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
                action = ReminderBroadcastReceiver.ACTION_TASK_REMINDER
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
        cancelTaskAlarm(context, task.id, dateIso)
    }

    fun scheduleDailyReminder(context: Context, hour: Int = 8, minute: Int = 0) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_DAILY_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            2001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        } catch (e: Exception) {}
    }

    fun showDailyDigestNotification(context: Context, title: String, message: String) {
        if (!hasNotificationPermission(context)) return
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            3001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_DAILY_MOMENTUM)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(3001, notification)
    }
}
