package com.example.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.domain.medication.AlertStyle
import com.example.domain.medication.DoseStatus
import com.example.domain.medication.MedicationDoseItem
import com.example.domain.medication.MedicationForm
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskReminderItem
import com.example.utils.DateUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.time.LocalDate
import java.time.ZoneId

enum class TaskAlarmScheduleStatus {
    NOT_REQUIRED,
    SCHEDULED_EXACTLY,
    SCHEDULED_INEXACTLY,
    PAST_DUE,
    INVALID_SCHEDULE,
    FAILED
}

data class TaskAlarmScheduleResult(
    val status: TaskAlarmScheduleStatus,
    val message: String? = null
)

object NotificationHelper {

    // Notification Channels (9 Categories)
    const val CHANNEL_MEDICINE = "lifeos_medicine_reminders"
    const val CHANNEL_TASK_REMINDERS = "lifeos_task_reminders"
    const val CHANNEL_HABITS = "lifeos_habit_reminders"
    const val CHANNEL_HYDRATION = "lifeos_hydration_reminders"
    const val CHANNEL_CALENDAR = "lifeos_calendar_alerts"
    const val CHANNEL_QUICK_REMINDERS = "lifeos_qr_alerts"
    const val CHANNEL_CHALLENGES = "lifeos_challenge_alerts"
    const val CHANNEL_GAMIFICATION = "lifeos_gamification_alerts"
    const val CHANNEL_DAILY_MOMENTUM = "lifeos_discipline_channel"

    val NOTIFICATION_SOUNDS = listOf("CHIME", "BELL", "DROP", "RADAR", "GENTLE")

    // Intent Extras
    const val EXTRA_TASK_ID = "extra_task_id"
    const val EXTRA_TASK_TITLE = "extra_task_title"
    const val EXTRA_TASK_EMOJI = "extra_task_emoji"
    const val EXTRA_DATE_ISO = "extra_date_iso"

    const val EXTRA_MEDICATION_ID = "extra_medication_id"
    const val EXTRA_DOSE_ID = "extra_dose_id"
    const val EXTRA_MEDICATION_NAME = "extra_medication_name"
    const val EXTRA_MEDICATION_FORM = "extra_medication_form"
    const val EXTRA_MEDICATION_DOSAGE = "extra_medication_dosage"
    const val EXTRA_MEDICATION_INSTRUCTIONS = "extra_medication_instructions"
    const val EXTRA_SCHEDULED_TIME = "extra_scheduled_time"
    const val EXTRA_PRIVACY_SAFE = "extra_privacy_safe"
    const val EXTRA_ALERT_STYLE = "extra_alert_style"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 1. Medicine Channel (Urgent/High Importance)
            val medicineChannel = NotificationChannel(
                CHANNEL_MEDICINE,
                "LifeOS Medicine Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical scheduled medication doses and adherence alerts"
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(medicineChannel)

            // 2. Task Reminders Channel
            val taskChannel = NotificationChannel(
                CHANNEL_TASK_REMINDERS,
                "LifeOS Task Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for scheduled tasks and daily priorities"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(taskChannel)

            // 3. Habits Channel
            val habitChannel = NotificationChannel(
                CHANNEL_HABITS,
                "LifeOS Habit Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily habit check-ins and routine prompts"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(habitChannel)

            // 4. Hydration Channel
            val hydrationChannel = NotificationChannel(
                CHANNEL_HYDRATION,
                "LifeOS Hydration Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Smart water intake prompts and goal check-ins"
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(hydrationChannel)

            // 5. Calendar Alerts Channel
            val calendarChannel = NotificationChannel(
                CHANNEL_CALENDAR,
                "LifeOS Calendar Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Scheduled agenda events and timeline conflict warnings"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(calendarChannel)

            // 6. Quick Reminders Channel
            val qrChannel = NotificationChannel(
                CHANNEL_QUICK_REMINDERS,
                "LifeOS Quick Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Instant alerts for active QR board tiles"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(qrChannel)

            // 7. Challenges Channel
            val challengeChannel = NotificationChannel(
                CHANNEL_CHALLENGES,
                "LifeOS 75-Day Challenge",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily challenge milestones and rule check-ins"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(challengeChannel)

            // 8. Gamification Channel
            val gamificationChannel = NotificationChannel(
                CHANNEL_GAMIFICATION,
                "LifeOS Hunter Rank & Achievements",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Level up notices, hunter rank promotions, and unlocked rewards"
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(gamificationChannel)

            // 9. Daily Momentum Digest Channel
            val dailyChannel = NotificationChannel(
                CHANNEL_DAILY_MOMENTUM,
                "LifeOS Daily Momentum",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Morning kickoff and evening wind-down summaries"
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

    // ==========================================
    // MEDICINE REMINDERS
    // ==========================================

    fun showMedicineReminderNotification(
        context: Context,
        doseId: Long,
        medicationId: Long,
        taskId: Long,
        name: String,
        form: String,
        dosage: String,
        instructions: String,
        scheduledTime: String,
        dateIso: String,
        privacySafe: Boolean = true,
        alertStyle: AlertStyle = AlertStyle.STANDARD
    ) {
        if (!hasNotificationPermission(context)) return
        createNotificationChannels(context)

        val notificationId = (doseId.toInt() * 37) + 50000

        // 1. Open App Intent
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DOSE_ID, doseId)
            putExtra(EXTRA_MEDICATION_ID, medicationId)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. Taken Action Intent
        val takenIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_MEDICINE_TAKEN
            putExtra(EXTRA_DOSE_ID, doseId)
            putExtra(EXTRA_MEDICATION_ID, medicationId)
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_DATE_ISO, dateIso)
        }
        val takenPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 100000,
            takenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Snooze Action Intent (10 min snooze)
        val snoozeIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_MEDICINE_SNOOZE
            putExtra(EXTRA_DOSE_ID, doseId)
            putExtra(EXTRA_MEDICATION_ID, medicationId)
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_MEDICATION_NAME, name)
            putExtra(EXTRA_MEDICATION_FORM, form)
            putExtra(EXTRA_MEDICATION_DOSAGE, dosage)
            putExtra(EXTRA_MEDICATION_INSTRUCTIONS, instructions)
            putExtra(EXTRA_SCHEDULED_TIME, scheduledTime)
            putExtra(EXTRA_DATE_ISO, dateIso)
            putExtra(EXTRA_PRIVACY_SAFE, privacySafe)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 200000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 4. Skip Action Intent
        val skipIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_MEDICINE_SKIP
            putExtra(EXTRA_DOSE_ID, doseId)
            putExtra(EXTRA_MEDICATION_ID, medicationId)
            putExtra(EXTRA_DATE_ISO, dateIso)
        }
        val skipPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 300000,
            skipIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formObj = MedicationForm.fromString(form)
        val publicTitle = "Medication Reminder"
        val publicText = "Scheduled dose at $scheduledTime"

        val privateTitle = "${formObj.emoji} Take $name ($dosage)"
        val privateText = if (instructions.isNotBlank()) {
            "$instructions • Scheduled for $scheduledTime"
        } else {
            "Scheduled for $scheduledTime"
        }

        val publicNotification = NotificationCompat.Builder(context, CHANNEL_MEDICINE)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(publicTitle)
            .setContentText(publicText)
            .build()

        val builder = NotificationCompat.Builder(context, CHANNEL_MEDICINE)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(if (privacySafe) publicTitle else privateTitle)
            .setContentText(if (privacySafe) publicText else privateText)
            .setPriority(if (alertStyle == AlertStyle.ALARM) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openPendingIntent)
            .setAutoCancel(true)
            .setPublicVersion(publicNotification)
            .setVisibility(if (privacySafe) NotificationCompat.VISIBILITY_PRIVATE else NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(android.R.drawable.checkbox_on_background, "✅ Taken", takenPendingIntent)
            .addAction(android.R.drawable.ic_menu_recent_history, "⏰ Snooze", snoozePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "⏭️ Skip", skipPendingIntent)

        if (alertStyle == AlertStyle.SILENT) {
            builder.setSilent(true)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, builder.build())
    }

    fun scheduleMedicineDoseAlarm(
        context: Context,
        dose: MedicationDoseItem,
        quietHoursStartMins: Int = 21 * 60,
        quietHoursEndMins: Int = 5 * 60 + 30,
        overrideQuietHours: Boolean = true
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val doseTimeMins = DateUtils.timeToMinutes(dose.scheduledTime)
        if (doseTimeMins < 0) return

        var triggerMinutes = doseTimeMins

        // If quiet hours override is disabled, postpone to quiet hours end
        if (!overrideQuietHours) {
            if (triggerMinutes >= quietHoursStartMins || triggerMinutes < quietHoursEndMins) {
                triggerMinutes = quietHoursEndMins
            }
        }

        val triggerCal = Calendar.getInstance().apply {
            try {
                val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dose.scheduledDateIso) ?: Date()
                time = parsed
            } catch (e: Exception) {}
            set(Calendar.HOUR_OF_DAY, triggerMinutes / 60)
            set(Calendar.MINUTE, triggerMinutes % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (triggerCal.timeInMillis <= System.currentTimeMillis()) return

        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_MEDICINE_REMINDER
            putExtra(EXTRA_DOSE_ID, dose.id)
            putExtra(EXTRA_MEDICATION_ID, dose.medicationId)
            putExtra(EXTRA_TASK_ID, dose.taskId)
            putExtra(EXTRA_MEDICATION_NAME, dose.medicationName)
            putExtra(EXTRA_MEDICATION_FORM, dose.medicationForm.name)
            putExtra(EXTRA_MEDICATION_DOSAGE, dose.dosage)
            putExtra(EXTRA_MEDICATION_INSTRUCTIONS, dose.instructions)
            putExtra(EXTRA_SCHEDULED_TIME, dose.scheduledTime)
            putExtra(EXTRA_DATE_ISO, dose.scheduledDateIso)
        }

        val requestCode = (dose.id.toInt() * 37) + 50000
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
        } catch (se: SecurityException) {
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerCal.timeInMillis, pendingIntent)
            } catch (_: Exception) {}
        } catch (e: Exception) {
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerCal.timeInMillis, pendingIntent)
            } catch (_: Exception) {}
        }
    }

    fun cancelMedicineDoseAlarm(context: Context, doseId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_MEDICINE_REMINDER
        }
        val requestCode = (doseId.toInt() * 37) + 50000
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

    // ==========================================
    // TASK REMINDERS
    // ==========================================

    fun showTaskReminderNotification(
        context: Context,
        taskId: Long,
        title: String,
        emoji: String,
        dueTime: String?,
        dateIso: String,
        isP1: Boolean = false,
        snoozeCount: Int = 0
    ) {
        if (!hasNotificationPermission(context)) return
        createNotificationChannels(context)

        val notificationId = (taskId.toInt() * 31) + dateIso.hashCode().and(0x7FFFFFFF) % 10000

        // 1. Open App Intent
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TASK_ID, taskId)
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

        // 3. Snooze Action Intent (10 min snooze for P1, 15 min standard)
        val snoozeMins = if (isP1) 10 else 15
        val snoozeIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_TASK_SNOOZE
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TASK_TITLE, title)
            putExtra(EXTRA_TASK_EMOJI, emoji)
            putExtra(EXTRA_DATE_ISO, dateIso)
            putExtra("extra_is_p1", isP1)
            putExtra("extra_snooze_count", snoozeCount)
            putExtra("extra_snooze_minutes", snoozeMins)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 200000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 4. Dismiss Action Intent (Dismisses alarm without completing task)
        val dismissIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_TASK_DISMISS
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_DATE_ISO, dateIso)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 400000,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timeText = if (!dueTime.isNullOrBlank()) "Scheduled for $dueTime" else "Today's priority"
        val displayTitle = if (isP1) "🚨 P1 ALARM: $emoji $title" else "$emoji $title"

        val builder = NotificationCompat.Builder(context, CHANNEL_TASK_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(displayTitle)
            .setContentText(timeText)
            .setPriority(if (isP1) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_HIGH)
            .setCategory(if (isP1) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openPendingIntent)
            .setAutoCancel(true)
            .addAction(android.R.drawable.checkbox_on_background, "✅ Complete", completePendingIntent)
            .addAction(android.R.drawable.ic_menu_recent_history, "⏰ Snooze ${snoozeMins}m", snoozePendingIntent)

        if (isP1) {
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "❌ Dismiss", dismissPendingIntent)
            builder.setFullScreenIntent(openPendingIntent, true)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, builder.build())
    }

    fun scheduleTaskAlarm(
        context: Context,
        task: TaskItem,
        dateIso: String,
        quietHoursStartMins: Int = 21 * 60,
        quietHoursEndMins: Int = 5 * 60 + 30
    ): TaskAlarmScheduleResult {
        val isP1 = task.priority == com.example.domain.model.TaskPriority.P1
        if (!task.isReminderEnabled && !isP1) {
            return TaskAlarmScheduleResult(TaskAlarmScheduleStatus.NOT_REQUIRED)
        }
        if (task.dueTime.isNullOrBlank()) {
            return TaskAlarmScheduleResult(
                TaskAlarmScheduleStatus.INVALID_SCHEDULE,
                "A start time is required to schedule this task alarm."
            )
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            ?: return TaskAlarmScheduleResult(TaskAlarmScheduleStatus.FAILED, "Alarm service is unavailable.")
        val taskStartMins = DateUtils.timeToMinutes(task.dueTime)
        if (taskStartMins < 0) {
            return TaskAlarmScheduleResult(TaskAlarmScheduleStatus.INVALID_SCHEDULE, "The task start time is invalid.")
        }

        var triggerMinutes = taskStartMins - task.reminderMinutesBefore
        if (triggerMinutes < 0) triggerMinutes = 0

        val nextDayOffset = if (triggerMinutes >= quietHoursStartMins) 1 else 0
        if (nextDayOffset == 1 || triggerMinutes < quietHoursEndMins) {
            triggerMinutes = quietHoursEndMins
        }

        val triggerTime = try {
            LocalDate.parse(dateIso)
                .atStartOfDay(ZoneId.systemDefault())
                .plusMinutes((triggerMinutes + nextDayOffset * 24 * 60).toLong())
                .toInstant()
                .toEpochMilli()
        } catch (error: java.time.DateTimeException) {
            Log.e("NotificationHelper", "Invalid task alarm date: $dateIso", error)
            return TaskAlarmScheduleResult(TaskAlarmScheduleStatus.INVALID_SCHEDULE, "The task date is invalid.")
        }

        if (triggerTime <= System.currentTimeMillis()) {
            return TaskAlarmScheduleResult(TaskAlarmScheduleStatus.PAST_DUE, "The task time has already passed.")
        }

        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_TASK_REMINDER
            data = taskAlarmUri(task.id, dateIso)
            putExtra(EXTRA_TASK_ID, task.id)
            putExtra(EXTRA_TASK_TITLE, task.title)
            putExtra(EXTRA_TASK_EMOJI, task.emoji)
            putExtra(EXTRA_DATE_ISO, dateIso)
            putExtra("extra_due_time", task.dueTime)
            putExtra("extra_is_p1", isP1)
        }

        val requestCode = (task.id xor (task.id ushr 32)).toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return scheduleAlarm(alarmManager, triggerTime, pendingIntent)
    }

    fun cancelTaskAlarm(context: Context, taskId: Long, dateIso: String = DateUtils.todayString()) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_TASK_REMINDER
            data = taskAlarmUri(taskId, dateIso)
        }
        val requestCode = (taskId xor (taskId ushr 32)).toInt()
        cancelAlarm(alarmManager, context, requestCode, intent)
        val legacyRequestCode = (taskId.toInt() * 31) + dateIso.hashCode().and(0x7FFFFFFF) % 50000
        cancelAlarm(
            alarmManager,
            context,
            legacyRequestCode,
            Intent(context, ReminderBroadcastReceiver::class.java).apply {
                action = ReminderBroadcastReceiver.ACTION_TASK_REMINDER
            }
        )
    }

    fun scheduleTaskReminderAlarm(
        context: Context,
        task: TaskItem,
        reminder: TaskReminderItem,
        dateIso: String,
        quietHoursStartMins: Int = 21 * 60,
        quietHoursEndMins: Int = 5 * 60 + 30
    ): TaskAlarmScheduleResult {
        if (!reminder.isEnabled) return TaskAlarmScheduleResult(TaskAlarmScheduleStatus.NOT_REQUIRED)
        if (task.dueTime.isNullOrBlank()) {
            return TaskAlarmScheduleResult(
                TaskAlarmScheduleStatus.INVALID_SCHEDULE,
                "A start time is required to schedule this task reminder."
            )
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            ?: return TaskAlarmScheduleResult(TaskAlarmScheduleStatus.FAILED, "Alarm service is unavailable.")
        val taskStartMins = DateUtils.timeToMinutes(task.dueTime)
        if (taskStartMins < 0) {
            return TaskAlarmScheduleResult(TaskAlarmScheduleStatus.INVALID_SCHEDULE, "The task start time is invalid.")
        }

        val requestedTriggerMinutes = when (reminder.type) {
            "SPECIFIC_TIME" -> reminder.customTime?.let { DateUtils.timeToMinutes(it) } ?: -1
            else -> taskStartMins - reminder.minutesBefore
        }
        if (reminder.type == "SPECIFIC_TIME" && requestedTriggerMinutes < 0) {
            return TaskAlarmScheduleResult(TaskAlarmScheduleStatus.INVALID_SCHEDULE, "The reminder time is invalid.")
        }
        var triggerMinutes = requestedTriggerMinutes.coerceAtLeast(0)
        if (triggerMinutes !in 0..1439) {
            return TaskAlarmScheduleResult(TaskAlarmScheduleStatus.INVALID_SCHEDULE, "The reminder time is invalid.")
        }

        val nextDayOffset = if (triggerMinutes >= quietHoursStartMins) 1 else 0
        if (nextDayOffset == 1 || triggerMinutes < quietHoursEndMins) {
            triggerMinutes = quietHoursEndMins
        }

        val triggerTime = try {
            LocalDate.parse(dateIso)
                .atStartOfDay(ZoneId.systemDefault())
                .plusMinutes((triggerMinutes + nextDayOffset * 24 * 60).toLong())
                .toInstant()
                .toEpochMilli()
        } catch (error: java.time.DateTimeException) {
            Log.e("NotificationHelper", "Invalid task reminder date: $dateIso", error)
            return TaskAlarmScheduleResult(TaskAlarmScheduleStatus.INVALID_SCHEDULE, "The task date is invalid.")
        }

        if (triggerTime <= System.currentTimeMillis()) {
            return TaskAlarmScheduleResult(TaskAlarmScheduleStatus.PAST_DUE, "The reminder time has already passed.")
        }

        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_TASK_REMINDER
            data = taskReminderUri(task.id, reminder, dateIso)
            putExtra(EXTRA_TASK_ID, task.id)
            putExtra(EXTRA_TASK_TITLE, task.title)
            putExtra(EXTRA_TASK_EMOJI, task.emoji)
            putExtra(EXTRA_DATE_ISO, dateIso)
            putExtra("extra_due_time", task.dueTime)
            putExtra("extra_reminder_id", reminder.id)
        }

        val requestCode = (task.id xor (task.id ushr 32)).toInt() * 31 + reminder.id.toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return scheduleAlarm(alarmManager, triggerTime, pendingIntent)
    }

    fun scheduleAllTaskReminders(
        context: Context,
        task: TaskItem,
        dateIso: String,
        quietHoursStartMins: Int = 21 * 60,
        quietHoursEndMins: Int = 5 * 60 + 30
    ): List<TaskAlarmScheduleResult> {
        val isP1 = task.priority == com.example.domain.model.TaskPriority.P1
        val enabledReminders = task.reminders.filter { it.isEnabled }
        if (enabledReminders.isNotEmpty()) {
            val results = enabledReminders.map { reminder ->
                scheduleTaskReminderAlarm(
                    context,
                    task,
                    reminder,
                    dateIso,
                    quietHoursStartMins,
                    quietHoursEndMins
                )
            }
            val hasP1TaskTimeAlarm = enabledReminders.any { reminder ->
                reminder.referenceType == "START_TIME" &&
                    ((reminder.type == "SPECIFIC_TIME" && reminder.customTime == task.dueTime) ||
                        (reminder.type != "SPECIFIC_TIME" && reminder.minutesBefore == 0))
            }
            return if (isP1 && !hasP1TaskTimeAlarm) {
                results + scheduleTaskAlarm(
                    context,
                    task,
                    dateIso,
                    quietHoursStartMins,
                    quietHoursEndMins
                )
            } else {
                results
            }
        } else if (task.isReminderEnabled || isP1) {
            return listOf(
                scheduleTaskAlarm(
                    context,
                    task,
                    dateIso,
                    quietHoursStartMins,
                    quietHoursEndMins
                )
            )
        }
        return emptyList()
    }

    fun cancelAllTaskReminders(
        context: Context,
        task: TaskItem,
        dateIso: String = DateUtils.todayString()
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        task.reminders.forEach { reminder ->
            val requestCode = (task.id xor (task.id ushr 32)).toInt() * 31 + reminder.id.toInt()
            val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
                action = ReminderBroadcastReceiver.ACTION_TASK_REMINDER
                data = taskReminderUri(task.id, reminder, dateIso)
            }
            cancelAlarm(alarmManager, context, requestCode, intent)
            val legacyRequestCode =
                ((task.id.toInt() * 31 + reminder.id.toInt()) * 31) +
                    dateIso.hashCode().and(0x7FFFFFFF) % 100000
            cancelAlarm(
                alarmManager,
                context,
                legacyRequestCode,
                Intent(context, ReminderBroadcastReceiver::class.java).apply {
                    action = ReminderBroadcastReceiver.ACTION_TASK_REMINDER
                }
            )
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

    private fun taskAlarmUri(taskId: Long, dateIso: String): Uri =
        Uri.Builder()
            .scheme("lifeos")
            .authority("task-reminder")
            .appendPath(taskId.toString())
            .appendPath(dateIso)
            .build()

    private fun taskReminderUri(taskId: Long, reminder: TaskReminderItem, dateIso: String): Uri =
        Uri.Builder()
            .scheme("lifeos")
            .authority("task-reminder")
            .appendPath(taskId.toString())
            .appendPath(dateIso)
            .appendPath("${reminder.id}:${reminder.type}:${reminder.minutesBefore}:${reminder.customTime.orEmpty()}")
            .build()

    private fun cancelAlarm(
        alarmManager: AlarmManager,
        context: Context,
        requestCode: Int,
        intent: Intent
    ) {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun scheduleAlarm(
        alarmManager: AlarmManager,
        triggerTime: Long,
        pendingIntent: PendingIntent
    ): TaskAlarmScheduleResult {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                TaskAlarmScheduleResult(
                    TaskAlarmScheduleStatus.SCHEDULED_INEXACTLY,
                    "Exact alarm access is unavailable; the reminder is scheduled inexactly."
                )
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                TaskAlarmScheduleResult(TaskAlarmScheduleStatus.SCHEDULED_EXACTLY)
            }
        } catch (error: SecurityException) {
            Log.e("NotificationHelper", "Exact task alarm was denied; attempting inexact scheduling.", error)
            try {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                TaskAlarmScheduleResult(
                    TaskAlarmScheduleStatus.SCHEDULED_INEXACTLY,
                    "Exact alarm access was denied; the reminder is scheduled inexactly."
                )
            } catch (fallbackError: SecurityException) {
                Log.e("NotificationHelper", "Inexact task alarm scheduling was denied.", fallbackError)
                TaskAlarmScheduleResult(TaskAlarmScheduleStatus.FAILED, "Alarm permission was denied.")
            } catch (fallbackError: RuntimeException) {
                Log.e("NotificationHelper", "Inexact task alarm scheduling failed.", fallbackError)
                TaskAlarmScheduleResult(TaskAlarmScheduleStatus.FAILED, "The alarm could not be scheduled.")
            }
        } catch (error: RuntimeException) {
            Log.e("NotificationHelper", "Exact task alarm scheduling failed.", error)
            TaskAlarmScheduleResult(TaskAlarmScheduleStatus.FAILED, "The alarm could not be scheduled.")
        }
    }

    const val TEST_REMINDER_NOTIFICATION_ID = 77777

    fun showTestReminderNotification(context: Context) {
        if (!hasNotificationPermission(context)) return
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            TEST_REMINDER_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_TASK_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("🧪 LifeOS Test Reminder")
            .setContentText("Notification delivery is working! Alarms and background triggers are operational.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(TEST_REMINDER_NOTIFICATION_ID, notification)
    }
}
