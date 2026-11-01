package com.example.notifications

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.example.data.local.AppDatabase
import com.example.data.preferences.UserPreferences
import com.example.data.repository.LifeRepository
import com.example.domain.medication.DoseStatus
import com.example.domain.medication.MedicationDoseItem
import com.example.domain.medication.MedicationForm
import com.example.domain.model.TaskPriority
import com.example.ui.theme.engine.LocalPremiumEntitlementRepository
import com.example.utils.DateUtils
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Representation of the system exact-alarm scheduling permission status (REM-05).
 */
enum class ExactAlarmStatus {
    GRANTED,
    NOT_GRANTED,
    NOT_NEEDED;

    val displayText: String
        get() = when (this) {
            GRANTED -> "Granted"
            NOT_GRANTED -> "Not granted"
            NOT_NEEDED -> "Not needed"
        }
}

/**
 * Health status for an individual Android notification channel (IMP-02 / F-04).
 */
data class ChannelHealthStatus(
    val id: String,
    val name: String,
    val isEnabled: Boolean,
    val isAvailable: Boolean
)

/**
 * Read-only representation of an upcoming scheduled reminder item (F-04).
 */
data class ScheduledReminderItem(
    val id: String,
    val title: String,
    val associationType: String, // "TASK", "MEDICATION", "DAILY"
    val scheduledDateIso: String,
    val scheduledTime: String,
    val triggerEpochMillis: Long,
    val reminderType: String // "P1_ALARM", "TASK_REMINDER", "MEDICINE_DOSE", "DAILY_DIGEST"
)

/**
 * Result metrics for a completed reminder rescheduling run.
 */
data class RescheduleResult(
    val horizonDays: Int,
    val tasksScheduled: Int,
    val dosesScheduled: Int,
    val totalScheduled: Int,
    val cappedByLimit: Boolean
)

/**
 * Centralized Reminder Scheduler abstraction (REM-02 / REM-03 / REM-04 / REM-05 / F-04).
 *
 * Guarantees:
 * 1. Rebuilds task reminders across a bounded horizon (up to 7 days).
 * 2. Rebuilds today's and tomorrow's medication dose reminders.
 * 3. Enforces a hard ceiling of 200 alarms to avoid exceeding framework quotas.
 * 4. Preserves existing deterministic request-code and PendingIntent identity schemes.
 * 5. Strictly idempotent: repeated scheduling replaces existing alarms without duplicate triggers.
 * 6. Never cancels alarms belonging to unrelated task IDs.
 * 7. Safely handles missing, completed, or deleted tasks.
 * 8. Resilient to exact-alarm permission restrictions (catches SecurityException, falls back to inexact).
 * 9. Completely independent of Activity / UI lifecycles.
 * 10. Exposes read-only queries for upcoming triggers without mutating records.
 */
interface ReminderScheduler {

    suspend fun rescheduleAll(horizonDays: Int = MAX_HORIZON_DAYS): RescheduleResult

    suspend fun getUpcomingReminders(limit: Int = 5): List<ScheduledReminderItem> = emptyList()

    companion object {
        const val MAX_ALARMS = 200
        const val MAX_HORIZON_DAYS = 7
        const val TEST_REMINDER_REQUEST_CODE = 77777

        private const val TAG = "ReminderScheduler"

        @Volatile
        private var customInstance: ReminderScheduler? = null

        fun getInstance(context: Context): ReminderScheduler {
            return customInstance ?: synchronized(this) {
                customInstance ?: DefaultReminderScheduler(context.applicationContext)
            }
        }

        fun setInstanceForTesting(scheduler: ReminderScheduler?) {
            customInstance = scheduler
        }

        suspend fun rescheduleAll(context: Context, horizonDays: Int = MAX_HORIZON_DAYS): RescheduleResult {
            return getInstance(context).rescheduleAll(horizonDays)
        }

        suspend fun getUpcomingReminders(context: Context, limit: Int = 5): List<ScheduledReminderItem> {
            return getInstance(context).getUpcomingReminders(limit)
        }

        fun checkExactAlarmStatus(context: Context): ExactAlarmStatus {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                if (alarmManager?.canScheduleExactAlarms() == true) {
                    ExactAlarmStatus.GRANTED
                } else {
                    ExactAlarmStatus.NOT_GRANTED
                }
            } else {
                ExactAlarmStatus.NOT_NEEDED
            }
        }

        fun openExactAlarmSettings(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    try {
                        val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(fallback)
                    } catch (e2: Exception) {
                        Log.e(TAG, "Failed opening exact alarm settings", e2)
                    }
                }
            }
        }

        fun checkNotificationPermission(context: Context): Boolean {
            return NotificationManagerCompat.from(context).areNotificationsEnabled()
        }

        fun openNotificationSettings(context: Context) {
            try {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                try {
                    val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(fallback)
                } catch (_: Exception) {}
            }
        }

        fun checkBatteryOptimizationStatus(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
            } else {
                true
            }
        }

        fun openBatteryOptimizationSettings(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    try {
                        val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(fallback)
                    } catch (e2: Exception) {
                        Log.e(TAG, "Failed opening battery optimization settings", e2)
                    }
                }
            }
        }

        fun checkAllChannelsStatus(context: Context): List<ChannelHealthStatus> {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                return emptyList()
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return emptyList()
            NotificationHelper.createNotificationChannels(context)

            val channelDefinitions = listOf(
                Pair(NotificationHelper.CHANNEL_MEDICINE, "Medication Reminders"),
                Pair(NotificationHelper.CHANNEL_TASK_REMINDERS, "Task Reminders"),
                Pair(NotificationHelper.CHANNEL_HABITS, "Habit Reminders"),
                Pair(NotificationHelper.CHANNEL_HYDRATION, "Hydration Reminders"),
                Pair(NotificationHelper.CHANNEL_CALENDAR, "Calendar Alerts"),
                Pair(NotificationHelper.CHANNEL_QUICK_REMINDERS, "Quick Reminders (QR)"),
                Pair(NotificationHelper.CHANNEL_CHALLENGES, "75-Day Challenge"),
                Pair(NotificationHelper.CHANNEL_GAMIFICATION, "Gamification & Rank"),
                Pair(NotificationHelper.CHANNEL_DAILY_MOMENTUM, "Daily Momentum Digest")
            )

            return channelDefinitions.map { (id, name) ->
                val channel = nm.getNotificationChannel(id)
                val isEnabled = channel != null && channel.importance != NotificationManager.IMPORTANCE_NONE
                ChannelHealthStatus(
                    id = id,
                    name = name,
                    isEnabled = isEnabled,
                    isAvailable = channel != null
                )
            }
        }

        fun openChannelSettings(context: Context, channelId: String) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    openNotificationSettings(context)
                }
            } else {
                openNotificationSettings(context)
            }
        }

        fun scheduleTestReminder(context: Context, delaySeconds: Long = 60L): Boolean {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
            val triggerTime = System.currentTimeMillis() + (delaySeconds * 1000L)

            val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
                action = ReminderBroadcastReceiver.ACTION_TEST_REMINDER
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                TEST_REMINDER_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            return try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    } else {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
                true
            } catch (e: Exception) {
                try {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    true
                } catch (_: Exception) {
                    false
                }
            }
        }

        fun cancelTestReminder(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
                action = ReminderBroadcastReceiver.ACTION_TEST_REMINDER
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                TEST_REMINDER_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }

        fun isTestReminderScheduled(context: Context): Boolean {
            val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
                action = ReminderBroadcastReceiver.ACTION_TEST_REMINDER
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                TEST_REMINDER_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            return pendingIntent != null
        }
    }
}

/**
 * Default production implementation of ReminderScheduler.
 */
class DefaultReminderScheduler(
    private val context: Context,
    private val database: AppDatabase = AppDatabase.getInstance(context.applicationContext),
    private val preferences: UserPreferences = UserPreferences(context.applicationContext),
    private val repository: LifeRepository = LifeRepository(
        database,
        preferences,
        LocalPremiumEntitlementRepository(context.applicationContext)
    )
) : ReminderScheduler {

    override suspend fun rescheduleAll(horizonDays: Int): RescheduleResult {
        val clampedHorizon = horizonDays.coerceIn(1, ReminderScheduler.MAX_HORIZON_DAYS)
        val todayIso = DateUtils.todayString()
        var totalScheduled = 0
        var tasksScheduled = 0
        var dosesScheduled = 0
        var capped = false

        // 1. Daily recurring reminder (always 1 alarm)
        try {
            NotificationHelper.scheduleDailyReminder(context, 8, 0)
            totalScheduled++
        } catch (e: Exception) {
            Log.w("ReminderScheduler", "Failed scheduling daily reminder", e)
        }

        // 2. Medication dose reminders for today (day 0) and tomorrow (day 1)
        val overrideQuiet = try {
            preferences.medicineOverrideQuietHours.first()
        } catch (_: Exception) {
            true
        }

        for (dayOffset in 0..1) {
            if (totalScheduled >= ReminderScheduler.MAX_ALARMS) {
                capped = true
                break
            }
            val dateIso = DateUtils.addDays(todayIso, dayOffset)
            val doses = try {
                database.medicationDao().getDosesForDate(dateIso)
            } catch (e: Exception) {
                Log.w("ReminderScheduler", "Could not query medication doses for $dateIso", e)
                emptyList()
            }

            for (doseEntity in doses) {
                if (doseEntity.status != "SCHEDULED") continue
                if (totalScheduled >= ReminderScheduler.MAX_ALARMS) {
                    capped = true
                    break
                }
                val item = MedicationDoseItem(
                    id = doseEntity.id,
                    medicationId = doseEntity.medicationId,
                    taskId = doseEntity.taskId,
                    medicationName = doseEntity.medicationName,
                    medicationForm = MedicationForm.fromString(doseEntity.medicationForm),
                    dosage = doseEntity.dosage,
                    instructions = doseEntity.instructions,
                    scheduledDateIso = doseEntity.scheduledDateIso,
                    scheduledTime = doseEntity.scheduledTime,
                    status = DoseStatus.valueOf(doseEntity.status)
                )
                NotificationHelper.scheduleMedicineDoseAlarm(
                    context = context,
                    dose = item,
                    overrideQuietHours = overrideQuiet
                )
                totalScheduled++
                dosesScheduled++
            }
        }

        // 3. Task reminders across horizon (days 0 until clampedHorizon)
        for (dayOffset in 0 until clampedHorizon) {
            if (totalScheduled >= ReminderScheduler.MAX_ALARMS) {
                capped = true
                break
            }
            val dateIso = DateUtils.addDays(todayIso, dayOffset)
            val tasks = try {
                repository.getTasksForDateFlow(dateIso).first()
            } catch (e: Exception) {
                Log.w("ReminderScheduler", "Could not query tasks for $dateIso", e)
                emptyList()
            }

            val candidateTasks = tasks.filter { task ->
                !task.isArchived &&
                !task.isCompletedForDate &&
                (task.priority == TaskPriority.P1 || task.isReminderEnabled || task.reminders.isNotEmpty()) &&
                !task.dueTime.isNullOrBlank()
            }

            for (task in candidateTasks) {
                if (totalScheduled >= ReminderScheduler.MAX_ALARMS) {
                    capped = true
                    break
                }

                if (task.reminders.isNotEmpty()) {
                    for (reminder in task.reminders) {
                        if (!reminder.isEnabled) continue
                        if (totalScheduled >= ReminderScheduler.MAX_ALARMS) {
                            capped = true
                            break
                        }
                        NotificationHelper.scheduleTaskReminderAlarm(context, task, reminder, dateIso)
                        totalScheduled++
                        tasksScheduled++
                    }
                } else if (task.isReminderEnabled || task.priority == TaskPriority.P1) {
                    NotificationHelper.scheduleTaskAlarm(context, task, dateIso)
                    totalScheduled++
                    tasksScheduled++
                }
            }
        }

        return RescheduleResult(
            horizonDays = clampedHorizon,
            tasksScheduled = tasksScheduled,
            dosesScheduled = dosesScheduled,
            totalScheduled = totalScheduled,
            cappedByLimit = capped
        )
    }

    override suspend fun getUpcomingReminders(limit: Int): List<ScheduledReminderItem> {
        val now = System.currentTimeMillis()
        val todayIso = DateUtils.todayString()
        val items = mutableListOf<ScheduledReminderItem>()

        // 1. Medication doses (today & tomorrow)
        for (dayOffset in 0..1) {
            val dateIso = DateUtils.addDays(todayIso, dayOffset)
            val doses = try {
                database.medicationDao().getDosesForDate(dateIso)
            } catch (_: Exception) {
                emptyList()
            }

            for (dose in doses) {
                if (dose.status != "SCHEDULED") continue
                val triggerMillis = calculateDoseTriggerEpoch(dateIso, dose.scheduledTime)
                if (triggerMillis > now) {
                    items.add(
                        ScheduledReminderItem(
                            id = "dose_${dose.id}",
                            title = "💊 Take ${dose.medicationName} (${dose.dosage})",
                            associationType = "MEDICATION",
                            scheduledDateIso = dateIso,
                            scheduledTime = dose.scheduledTime,
                            triggerEpochMillis = triggerMillis,
                            reminderType = "MEDICINE_DOSE"
                        )
                    )
                }
            }
        }

        // 2. Active tasks across 7-day horizon
        for (dayOffset in 0 until ReminderScheduler.MAX_HORIZON_DAYS) {
            val dateIso = DateUtils.addDays(todayIso, dayOffset)
            val tasks = try {
                repository.getTasksForDateFlow(dateIso).first()
            } catch (_: Exception) {
                emptyList()
            }

            for (task in tasks) {
                if (task.isArchived || task.isCompletedForDate || task.dueTime.isNullOrBlank()) continue
                val isP1 = task.priority == TaskPriority.P1

                if (task.reminders.isNotEmpty()) {
                    for (reminder in task.reminders) {
                        if (!reminder.isEnabled) continue
                        val minsBefore = if (reminder.type == "SPECIFIC_TIME") {
                            reminder.customTime?.let { DateUtils.timeToMinutes(it) } ?: DateUtils.timeToMinutes(task.dueTime)
                        } else {
                            reminder.minutesBefore
                        }
                        val triggerMillis = calculateTaskTriggerEpoch(dateIso, task.dueTime, minsBefore, reminder.type == "SPECIFIC_TIME")
                        if (triggerMillis > now) {
                            items.add(
                                ScheduledReminderItem(
                                    id = "task_${task.id}_reminder_${reminder.id}",
                                    title = "${task.emoji} ${task.title}".trim(),
                                    associationType = "TASK",
                                    scheduledDateIso = dateIso,
                                    scheduledTime = task.dueTime ?: "",
                                    triggerEpochMillis = triggerMillis,
                                    reminderType = if (isP1) "P1_ALARM" else "TASK_REMINDER"
                                )
                            )
                        }
                    }
                } else if (task.isReminderEnabled || isP1) {
                    val triggerMillis = calculateTaskTriggerEpoch(dateIso, task.dueTime, task.reminderMinutesBefore, false)
                    if (triggerMillis > now) {
                        items.add(
                            ScheduledReminderItem(
                                id = "task_${task.id}",
                                title = "${task.emoji} ${task.title}".trim(),
                                associationType = "TASK",
                                scheduledDateIso = dateIso,
                                scheduledTime = task.dueTime ?: "",
                                triggerEpochMillis = triggerMillis,
                                reminderType = if (isP1) "P1_ALARM" else "TASK_REMINDER"
                            )
                        )
                    }
                }
            }
        }

        // 3. Sort chronologically and take requested limit
        return items.sortedBy { it.triggerEpochMillis }.take(limit)
    }

    private fun calculateDoseTriggerEpoch(dateIso: String, timeStr: String): Long {
        val mins = DateUtils.timeToMinutes(timeStr)
        if (mins < 0) return -1L
        val cal = Calendar.getInstance().apply {
            try {
                val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateIso) ?: Date()
                time = parsed
            } catch (_: Exception) {}
            set(Calendar.HOUR_OF_DAY, mins / 60)
            set(Calendar.MINUTE, mins % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun calculateTaskTriggerEpoch(dateIso: String, timeStr: String?, minsBefore: Int, isSpecificTime: Boolean): Long {
        if (timeStr.isNullOrBlank()) return -1L
        val taskStartMins = DateUtils.timeToMinutes(timeStr)
        if (taskStartMins < 0) return -1L

        var triggerMinutes = if (isSpecificTime) minsBefore else taskStartMins - minsBefore
        if (triggerMinutes < 0) triggerMinutes = 0

        val cal = Calendar.getInstance().apply {
            try {
                val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateIso) ?: Date()
                time = parsed
            } catch (_: Exception) {}
            set(Calendar.HOUR_OF_DAY, triggerMinutes / 60)
            set(Calendar.MINUTE, triggerMinutes % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}
