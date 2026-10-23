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
import com.example.domain.medication.AlertStyle
import com.example.domain.medication.DoseStatus
import com.example.domain.medication.IntervalModel
import com.example.domain.medication.MedicationDoseItem
import com.example.domain.medication.MedicationForm
import com.example.domain.medication.MedicineScheduleType
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

        // Medicine Actions
        const val ACTION_MEDICINE_REMINDER = "com.example.lifeos.ACTION_MEDICINE_REMINDER"
        const val ACTION_MEDICINE_TAKEN = "com.example.lifeos.ACTION_MEDICINE_TAKEN"
        const val ACTION_MEDICINE_SNOOZE = "com.example.lifeos.ACTION_MEDICINE_SNOOZE"
        const val ACTION_MEDICINE_SKIP = "com.example.lifeos.ACTION_MEDICINE_SKIP"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val receivedAction = intent.action ?: return

        // Security hardening: For internal LifeOS actions, reject any intent targeted from other packages
        if (receivedAction != Intent.ACTION_BOOT_COMPLETED) {
            val pkg = intent.`package` ?: intent.component?.packageName
            if (pkg != null && pkg != context.packageName) {
                return
            }
        }

        val database = AppDatabase.getInstance(context.applicationContext)
        val preferences = UserPreferences(context.applicationContext)
        val entitlementRepo = com.example.ui.theme.engine.LocalPremiumEntitlementRepository(context.applicationContext)
        val repository = LifeRepository(database, preferences, entitlementRepo)

        when (receivedAction) {
            Intent.ACTION_BOOT_COMPLETED -> {
                NotificationHelper.scheduleDailyReminder(context, 8, 0)
                CoroutineScope(Dispatchers.IO).launch {
                    val todayIso = DateUtils.todayString()
                    val tasks = repository.getTasksForDateFlow(todayIso).first()
                    tasks.filter { (it.isReminderEnabled || it.reminders.isNotEmpty()) && !it.isCompletedForDate }.forEach { task ->
                        NotificationHelper.scheduleAllTaskReminders(context, task, todayIso)
                    }

                    // Reschedule active medicine doses for today
                    val doses = database.medicationDao().getDosesForDate(todayIso)
                    val overrideQuiet = preferences.medicineOverrideQuietHours.first()
                    doses.filter { it.status == "SCHEDULED" }.forEach { doseEntity ->
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
                    }
                }
            }

            ACTION_DAILY_REMINDER -> {
                CoroutineScope(Dispatchers.IO).launch {
                    val isDailySummaryEnabled = preferences.notifDailySummaryEnabled.first()
                    if (isDailySummaryEnabled) {
                        NotificationHelper.showDailyDigestNotification(
                            context = context,
                            title = "⚡ LifeOS Daily Momentum",
                            message = "Check off your daily habits, top priorities, and 75-Day Challenge rules for today!"
                        )
                    }
                }
            }

            ACTION_TASK_REMINDER -> {
                val taskId = intent.getLongExtra(NotificationHelper.EXTRA_TASK_ID, -1L)
                val title = intent.getStringExtra(NotificationHelper.EXTRA_TASK_TITLE) ?: "Task Reminder"
                val emoji = intent.getStringExtra(NotificationHelper.EXTRA_TASK_EMOJI) ?: "✅"
                val dateIso = intent.getStringExtra(NotificationHelper.EXTRA_DATE_ISO) ?: DateUtils.todayString()
                val dueTime = intent.getStringExtra("extra_due_time")

                if (taskId != -1L) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val isTasksNotifEnabled = preferences.notifTasksEnabled.first()
                        if (isTasksNotifEnabled) {
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
                }
            }

            ACTION_TASK_COMPLETE -> {
                val taskId = intent.getLongExtra(NotificationHelper.EXTRA_TASK_ID, -1L)
                val dateIso = intent.getStringExtra(NotificationHelper.EXTRA_DATE_ISO) ?: DateUtils.todayString()

                if (taskId != -1L) {
                    val notificationId = (taskId.toInt() * 31) + dateIso.hashCode().and(0x7FFFFFFF) % 10000
                    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    nm.cancel(notificationId)

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
                    val notificationId = (taskId.toInt() * 31) + dateIso.hashCode().and(0x7FFFFFFF) % 10000
                    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    nm.cancel(notificationId)

                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                    if (alarmManager != null) {
                        CoroutineScope(Dispatchers.IO).launch {
                            val snoozeMins = preferences.defaultSnoozeMinutes.first()
                            val snoozeTrigger = System.currentTimeMillis() + (snoozeMins * 60 * 1000L)
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

            // ==========================================
            // MEDICINE ACTIONS
            // ==========================================

            ACTION_MEDICINE_REMINDER -> {
                val doseId = intent.getLongExtra(NotificationHelper.EXTRA_DOSE_ID, -1L)
                val medicationId = intent.getLongExtra(NotificationHelper.EXTRA_MEDICATION_ID, -1L)
                val taskId = intent.getLongExtra(NotificationHelper.EXTRA_TASK_ID, -1L)
                val name = intent.getStringExtra(NotificationHelper.EXTRA_MEDICATION_NAME) ?: "Medication"
                val form = intent.getStringExtra(NotificationHelper.EXTRA_MEDICATION_FORM) ?: "TABLET"
                val dosage = intent.getStringExtra(NotificationHelper.EXTRA_MEDICATION_DOSAGE) ?: "1 dose"
                val instructions = intent.getStringExtra(NotificationHelper.EXTRA_MEDICATION_INSTRUCTIONS) ?: ""
                val scheduledTime = intent.getStringExtra(NotificationHelper.EXTRA_SCHEDULED_TIME) ?: "08:00"
                val dateIso = intent.getStringExtra(NotificationHelper.EXTRA_DATE_ISO) ?: DateUtils.todayString()

                if (doseId != -1L) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val isMedNotifEnabled = preferences.notifMedicineEnabled.first()
                        if (!isMedNotifEnabled) return@launch

                        val doseEntity = database.medicationDao().getDoseById(doseId)
                        // Only show if not already taken or skipped
                        if (doseEntity == null || doseEntity.status == "TAKEN" || doseEntity.status == "SKIPPED") {
                            return@launch
                        }

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
                                    action = ACTION_MEDICINE_REMINDER
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
                                } catch (e: Exception) {}
                            }
                        }
                    }
                }
            }

            ACTION_MEDICINE_TAKEN -> {
                val doseId = intent.getLongExtra(NotificationHelper.EXTRA_DOSE_ID, -1L)
                val medicationId = intent.getLongExtra(NotificationHelper.EXTRA_MEDICATION_ID, -1L)
                val taskId = intent.getLongExtra(NotificationHelper.EXTRA_TASK_ID, -1L)
                val dateIso = intent.getStringExtra(NotificationHelper.EXTRA_DATE_ISO) ?: DateUtils.todayString()

                if (doseId != -1L) {
                    val notificationId = (doseId.toInt() * 37) + 50000
                    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    nm.cancel(notificationId)

                    CoroutineScope(Dispatchers.IO).launch {
                        // Idempotent: record taken in medication_doses
                        database.medicationDao().updateDoseStatus(
                            id = doseId,
                            status = DoseStatus.TAKEN.name,
                            timestamp = System.currentTimeMillis(),
                            notes = "Dose confirmed taken at ${DateUtils.currentTimeString()}"
                        )

                        // If all doses for today are taken, mark parent task completed for date
                        if (medicationId != -1L && taskId != -1L) {
                            val todayDoses = database.medicationDao().getDosesForDate(dateIso).filter { it.medicationId == medicationId }
                            if (todayDoses.isNotEmpty() && todayDoses.all { it.status == "TAKEN" || it.status == "SKIPPED" }) {
                                repository.markTaskCompletedForDate(taskId, dateIso)
                            }
                        }
                    }
                }
            }

            ACTION_MEDICINE_SNOOZE -> {
                val doseId = intent.getLongExtra(NotificationHelper.EXTRA_DOSE_ID, -1L)
                if (doseId != -1L) {
                    val notificationId = (doseId.toInt() * 37) + 50000
                    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    nm.cancel(notificationId)

                    CoroutineScope(Dispatchers.IO).launch {
                        database.medicationDao().incrementSnoozeCount(doseId)
                        val snoozeMins = preferences.defaultSnoozeMinutes.first()
                        val snoozeTrigger = System.currentTimeMillis() + (snoozeMins * 60 * 1000L)

                        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                        if (alarmManager != null) {
                            val snoozeIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
                                action = ACTION_MEDICINE_REMINDER
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
                    }
                }
            }

            ACTION_MEDICINE_SKIP -> {
                val doseId = intent.getLongExtra(NotificationHelper.EXTRA_DOSE_ID, -1L)
                if (doseId != -1L) {
                    val notificationId = (doseId.toInt() * 37) + 50000
                    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    nm.cancel(notificationId)

                    CoroutineScope(Dispatchers.IO).launch {
                        database.medicationDao().updateDoseStatus(
                            id = doseId,
                            status = DoseStatus.SKIPPED.name,
                            timestamp = System.currentTimeMillis(),
                            notes = "Skipped by user at ${DateUtils.currentTimeString()}"
                        )
                    }
                }
            }
        }
    }
}
