package com.example.notifications

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
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
 * Centralized Reminder Scheduler abstraction (REM-02 / REM-03 / REM-04 / REM-05).
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
 */
interface ReminderScheduler {

    suspend fun rescheduleAll(horizonDays: Int = MAX_HORIZON_DAYS): RescheduleResult

    companion object {
        const val MAX_ALARMS = 200
        const val MAX_HORIZON_DAYS = 7

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
}
