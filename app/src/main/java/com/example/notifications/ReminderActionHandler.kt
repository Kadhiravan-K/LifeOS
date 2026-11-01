package com.example.notifications

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.preferences.UserPreferences
import com.example.data.repository.LifeRepository
import com.example.domain.medication.AlertStyle
import com.example.domain.medication.DoseStatus
import com.example.domain.medication.MedicationDoseItem
import com.example.domain.medication.MedicationForm
import com.example.ui.theme.engine.LocalPremiumEntitlementRepository
import com.example.utils.DateUtils
import com.example.widgets.QuickRemindersWidgetProvider
import kotlinx.coroutines.flow.first

/**
 * Domain action handler for LifeOS notification and reminder events (RFR-04).
 *
 * Encapsulates all reminder-related business logic in pure, testable suspend functions,
 * decoupling domain mutations from BroadcastReceiver component lifecycles.
 */
object ReminderActionHandler {

    private const val TAG = "ReminderActionHandler"

    fun calculateTaskNotificationId(taskId: Long, dateIso: String): Int {
        return (taskId.toInt() * 31) + dateIso.hashCode().and(0x7FFFFFFF) % 10000
    }

    fun calculateMedicineNotificationId(doseId: Long): Int {
        return (doseId.toInt() * 37) + 50000
    }

    fun cancelTaskNotification(context: Context, taskId: Long, dateIso: String) {
        if (taskId <= 0L) return
        val notificationId = calculateTaskNotificationId(taskId, dateIso)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.cancel(notificationId)
    }

    fun cancelMedicineNotification(context: Context, doseId: Long) {
        if (doseId <= 0L) return
        val notificationId = calculateMedicineNotificationId(doseId)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.cancel(notificationId)
    }

    suspend fun handleDailyReminder(context: Context) {
        val preferences = UserPreferences(context.applicationContext)
        val isDailySummaryEnabled = preferences.notifDailySummaryEnabled.first()
        if (isDailySummaryEnabled) {
            NotificationHelper.showDailyDigestNotification(
                context = context,
                title = "⚡ LifeOS Daily Momentum",
                message = "Check off your daily habits, top priorities, and 75-Day Challenge rules for today!"
            )
        }
    }

    suspend fun handleTaskReminder(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(NotificationHelper.EXTRA_TASK_ID, -1L)
        if (taskId <= 0L) {
            Log.w(TAG, "Ignoring task reminder with invalid ID: $taskId")
            return
        }

        val title = intent.getStringExtra(NotificationHelper.EXTRA_TASK_TITLE) ?: "Task Reminder"
        val emoji = intent.getStringExtra(NotificationHelper.EXTRA_TASK_EMOJI) ?: "✅"
        val dateIso = intent.getStringExtra(NotificationHelper.EXTRA_DATE_ISO) ?: DateUtils.todayString()
        val dueTime = intent.getStringExtra("extra_due_time")
        val isP1 = intent.getBooleanExtra("extra_is_p1", false)
        val snoozeCount = intent.getIntExtra("extra_snooze_count", 0)

        val preferences = UserPreferences(context.applicationContext)
        val isTasksNotifEnabled = preferences.notifTasksEnabled.first()
        val isP1AlarmPrefEnabled = preferences.p1AlarmEnabled.first()

        if (isTasksNotifEnabled || (isP1 && isP1AlarmPrefEnabled)) {
            NotificationHelper.showTaskReminderNotification(
                context = context,
                taskId = taskId,
                title = title,
                emoji = emoji,
                dueTime = dueTime,
                dateIso = dateIso,
                isP1 = isP1,
                snoozeCount = snoozeCount
            )
        }
    }

    suspend fun handleTaskComplete(context: Context, taskId: Long, dateIso: String) {
        if (taskId <= 0L) {
            Log.w(TAG, "Ignoring task complete action with invalid taskId: $taskId")
            return
        }

        val database = AppDatabase.getInstance(context.applicationContext)
        val preferences = UserPreferences(context.applicationContext)
        val entitlementRepo = LocalPremiumEntitlementRepository(context.applicationContext)
        val repository = LifeRepository(database, preferences, entitlementRepo)

        repository.toggleTaskCompletion(taskId, dateIso)
        QuickRemindersWidgetProvider.notifyDataChanged(context.applicationContext)
    }

    suspend fun handleTaskSnooze(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(NotificationHelper.EXTRA_TASK_ID, -1L)
        if (taskId <= 0L) {
            Log.w(TAG, "Ignoring task snooze with invalid taskId: $taskId")
            return
        }

        val title = intent.getStringExtra(NotificationHelper.EXTRA_TASK_TITLE) ?: "Task Reminder"
        val emoji = intent.getStringExtra(NotificationHelper.EXTRA_TASK_EMOJI) ?: "✅"
        val dateIso = intent.getStringExtra(NotificationHelper.EXTRA_DATE_ISO) ?: DateUtils.todayString()
        val isP1 = intent.getBooleanExtra("extra_is_p1", false)
        val snoozeCount = intent.getIntExtra("extra_snooze_count", 0)
        val explicitSnoozeMins = intent.getIntExtra("extra_snooze_minutes", -1)

        val preferences = UserPreferences(context.applicationContext)
        val maxSnooze = if (isP1) preferences.p1MaxSnoozeCount.first() else 5
        if (snoozeCount >= maxSnooze) {
            Log.i(TAG, "Task $taskId reached maximum snooze count ($snoozeCount >= $maxSnooze)")
            return
        }

        val snoozeMins = if (explicitSnoozeMins > 0) {
            explicitSnoozeMins
        } else if (isP1) {
            preferences.p1SnoozeMinutes.first()
        } else {
            preferences.defaultSnoozeMinutes.first()
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val snoozeTrigger = System.currentTimeMillis() + (snoozeMins * 60 * 1000L)
        val snoozeIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_TASK_REMINDER
            putExtra(NotificationHelper.EXTRA_TASK_ID, taskId)
            putExtra(NotificationHelper.EXTRA_TASK_TITLE, title)
            putExtra(NotificationHelper.EXTRA_TASK_EMOJI, emoji)
            putExtra(NotificationHelper.EXTRA_DATE_ISO, dateIso)
            putExtra("extra_is_p1", isP1)
            putExtra("extra_snooze_count", snoozeCount + 1)
        }

        val notificationId = calculateTaskNotificationId(taskId, dateIso)
        val pending = PendingIntent.getBroadcast(
            context,
            notificationId + 300000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeTrigger, pending)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeTrigger, pending)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeTrigger, pending)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed scheduling task snooze alarm", e)
        }
    }

    suspend fun handleMedicineReminder(context: Context, intent: Intent) {
        val doseId = intent.getLongExtra(NotificationHelper.EXTRA_DOSE_ID, -1L)
        if (doseId <= 0L) {
            Log.w(TAG, "Ignoring medicine reminder with invalid doseId: $doseId")
            return
        }

        val preferences = UserPreferences(context.applicationContext)
        val isMedNotifEnabled = preferences.notifMedicineEnabled.first()
        if (!isMedNotifEnabled) return

        val database = AppDatabase.getInstance(context.applicationContext)
        val doseEntity = database.medicationDao().getDoseById(doseId)
        if (doseEntity == null || doseEntity.status == "TAKEN" || doseEntity.status == "SKIPPED") {
            return
        }

        val medicationId = intent.getLongExtra(NotificationHelper.EXTRA_MEDICATION_ID, -1L)
        val taskId = intent.getLongExtra(NotificationHelper.EXTRA_TASK_ID, -1L)
        val name = intent.getStringExtra(NotificationHelper.EXTRA_MEDICATION_NAME) ?: "Medication"
        val form = intent.getStringExtra(NotificationHelper.EXTRA_MEDICATION_FORM) ?: "TABLET"
        val dosage = intent.getStringExtra(NotificationHelper.EXTRA_MEDICATION_DOSAGE) ?: "1 dose"
        val instructions = intent.getStringExtra(NotificationHelper.EXTRA_MEDICATION_INSTRUCTIONS) ?: ""
        val scheduledTime = intent.getStringExtra(NotificationHelper.EXTRA_SCHEDULED_TIME) ?: "08:00"
        val dateIso = intent.getStringExtra(NotificationHelper.EXTRA_DATE_ISO) ?: DateUtils.todayString()

        val privacyEnabled = preferences.notificationPrivacyEnabled.first()
        val defaultStyleStr = preferences.defaultAlertStyle.first()
        val alertStyle = try { AlertStyle.valueOf(defaultStyleStr) } catch (e: Exception) { AlertStyle.STANDARD }

        NotificationHelper.showMedicineReminderNotification(
            context = context,
            doseId = doseId,
            medicationId = medicationId,
            taskId = taskId,
            name = name,
            form = form,
            dosage = dosage,
            instructions = instructions,
            scheduledTime = scheduledTime,
            dateIso = dateIso,
            privacySafe = privacyEnabled,
            alertStyle = alertStyle
        )

        // Follow-up scheduling if enabled
        val isFollowUpOn = preferences.followUpEnabled.first()
        val maxFollowUps = preferences.maxFollowUpCount.first()
        if (isFollowUpOn && doseEntity.followUpCount < maxFollowUps) {
            database.medicationDao().incrementFollowUpCount(doseId)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            if (alarmManager != null) {
                val followUpTrigger = System.currentTimeMillis() + (15 * 60 * 1000L)
                val followUpIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
                    action = ReminderBroadcastReceiver.ACTION_MEDICINE_REMINDER
                    putExtras(intent)
                }
                val pendingFollowUp = PendingIntent.getBroadcast(
                    context,
                    (doseId.toInt() * 37) + 80000 + doseEntity.followUpCount,
                    followUpIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                try {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, followUpTrigger, pendingFollowUp)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed scheduling medicine follow-up alarm", e)
                }
            }
        }
    }

    suspend fun handleMedicineTaken(
        context: Context,
        doseId: Long,
        medicationId: Long,
        taskId: Long,
        dateIso: String
    ) {
        if (doseId <= 0L) {
            Log.w(TAG, "Ignoring medicine taken action with invalid doseId: $doseId")
            return
        }

        val database = AppDatabase.getInstance(context.applicationContext)
        val preferences = UserPreferences(context.applicationContext)
        val entitlementRepo = LocalPremiumEntitlementRepository(context.applicationContext)
        val repository = LifeRepository(database, preferences, entitlementRepo)

        database.medicationDao().updateDoseStatus(
            id = doseId,
            status = DoseStatus.TAKEN.name,
            timestamp = System.currentTimeMillis(),
            notes = "Dose confirmed taken at ${DateUtils.currentTimeString()}"
        )

        if (medicationId > 0L && taskId > 0L) {
            val todayDoses = database.medicationDao().getDosesForDate(dateIso).filter { it.medicationId == medicationId }
            if (todayDoses.isNotEmpty() && todayDoses.all { it.status == "TAKEN" || it.status == "SKIPPED" }) {
                repository.markTaskCompletedForDate(taskId, dateIso)
            }
        }
    }

    suspend fun handleMedicineSnooze(context: Context, intent: Intent) {
        val doseId = intent.getLongExtra(NotificationHelper.EXTRA_DOSE_ID, -1L)
        if (doseId <= 0L) {
            Log.w(TAG, "Ignoring medicine snooze with invalid doseId: $doseId")
            return
        }

        val database = AppDatabase.getInstance(context.applicationContext)
        val preferences = UserPreferences(context.applicationContext)

        database.medicationDao().incrementSnoozeCount(doseId)
        val snoozeMins = preferences.defaultSnoozeMinutes.first()
        val snoozeTrigger = System.currentTimeMillis() + (snoozeMins * 60 * 1000L)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val snoozeIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_MEDICINE_REMINDER
            putExtras(intent)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            (doseId.toInt() * 37) + 90000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeTrigger, pending)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeTrigger, pending)
            }
        } catch (e: Exception) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, snoozeTrigger, pending)
        }
    }

    suspend fun handleMedicineSkip(context: Context, doseId: Long) {
        if (doseId <= 0L) {
            Log.w(TAG, "Ignoring medicine skip with invalid doseId: $doseId")
            return
        }

        val database = AppDatabase.getInstance(context.applicationContext)
        database.medicationDao().updateDoseStatus(
            id = doseId,
            status = DoseStatus.SKIPPED.name,
            timestamp = System.currentTimeMillis(),
            notes = "Skipped by user at ${DateUtils.currentTimeString()}"
        )
    }

    suspend fun rescheduleAllAlarmsOnBoot(context: Context) {
        ReminderScheduler.rescheduleAll(context, horizonDays = 7)
    }
}
