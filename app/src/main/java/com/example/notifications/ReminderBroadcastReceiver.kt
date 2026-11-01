package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.utils.DateUtils

/**
 * Internal BroadcastReceiver for LifeOS reminders and alarms (SEC-01).
 *
 * Security Architecture:
 * - Marked android:exported="false" in AndroidManifest.xml.
 * - System alarms and notifications target this receiver explicitly via Explicit PendingIntents.
 * - External components cannot inject or broadcast intents to this receiver.
 * - Enforces strict action whitelisting and parameter validation before execution.
 * - Delegates all asynchronous domain operations to ReminderActionHandler via runAsync() (REM-01 / RFR-04).
 */
class ReminderBroadcastReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DAILY_REMINDER = "com.example.lifeos.ACTION_DAILY_REMINDER"
        const val ACTION_TASK_REMINDER = "com.example.lifeos.ACTION_TASK_REMINDER"
        const val ACTION_TASK_COMPLETE = "com.example.lifeos.ACTION_TASK_COMPLETE"
        const val ACTION_TASK_SNOOZE = "com.example.lifeos.ACTION_TASK_SNOOZE"
        const val ACTION_TASK_DISMISS = "com.example.lifeos.ACTION_TASK_DISMISS"

        // Medicine Actions
        const val ACTION_MEDICINE_REMINDER = "com.example.lifeos.ACTION_MEDICINE_REMINDER"
        const val ACTION_MEDICINE_TAKEN = "com.example.lifeos.ACTION_MEDICINE_TAKEN"
        const val ACTION_MEDICINE_SNOOZE = "com.example.lifeos.ACTION_MEDICINE_SNOOZE"
        const val ACTION_MEDICINE_SKIP = "com.example.lifeos.ACTION_MEDICINE_SKIP"

        // Diagnostics Test Action
        const val ACTION_TEST_REMINDER = "com.example.lifeos.ACTION_TEST_REMINDER"

        val ALLOWED_ACTIONS: Set<String> = setOf(
            ACTION_DAILY_REMINDER,
            ACTION_TASK_REMINDER,
            ACTION_TASK_COMPLETE,
            ACTION_TASK_SNOOZE,
            ACTION_TASK_DISMISS,
            ACTION_MEDICINE_REMINDER,
            ACTION_MEDICINE_TAKEN,
            ACTION_MEDICINE_SNOOZE,
            ACTION_MEDICINE_SKIP,
            ACTION_TEST_REMINDER
        )

        /**
         * Delegation hook for boot rescheduling (RFR-04).
         */
        suspend fun rescheduleAllAlarmsOnBoot(context: Context) {
            ReminderActionHandler.rescheduleAllAlarmsOnBoot(context)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action.isNullOrBlank() || action !in ALLOWED_ACTIONS) {
            Log.w("ReminderReceiver", "Rejected unauthorized or unknown action: '$action'")
            return
        }

        when (action) {
            ACTION_DAILY_REMINDER -> {
                runAsync {
                    ReminderActionHandler.handleDailyReminder(context)
                }
            }

            ACTION_TASK_REMINDER -> {
                runAsync {
                    ReminderActionHandler.handleTaskReminder(context, intent)
                }
            }

            ACTION_TASK_COMPLETE -> {
                val taskId = intent.getLongExtra(NotificationHelper.EXTRA_TASK_ID, -1L)
                val dateIso = intent.getStringExtra(NotificationHelper.EXTRA_DATE_ISO) ?: DateUtils.todayString()
                if (taskId <= 0L) {
                    Log.w("ReminderReceiver", "Rejected ACTION_TASK_COMPLETE with invalid taskId: $taskId")
                    return
                }

                // Immediately cancel notification on main thread
                ReminderActionHandler.cancelTaskNotification(context, taskId, dateIso)

                runAsync {
                    ReminderActionHandler.handleTaskComplete(context, taskId, dateIso)
                }
            }

            ACTION_TASK_DISMISS -> {
                val taskId = intent.getLongExtra(NotificationHelper.EXTRA_TASK_ID, -1L)
                val dateIso = intent.getStringExtra(NotificationHelper.EXTRA_DATE_ISO) ?: DateUtils.todayString()
                if (taskId <= 0L) {
                    Log.w("ReminderReceiver", "Rejected ACTION_TASK_DISMISS with invalid taskId: $taskId")
                    return
                }
                ReminderActionHandler.cancelTaskNotification(context, taskId, dateIso)
            }

            ACTION_TASK_SNOOZE -> {
                val taskId = intent.getLongExtra(NotificationHelper.EXTRA_TASK_ID, -1L)
                val dateIso = intent.getStringExtra(NotificationHelper.EXTRA_DATE_ISO) ?: DateUtils.todayString()
                if (taskId <= 0L) {
                    Log.w("ReminderReceiver", "Rejected ACTION_TASK_SNOOZE with invalid taskId: $taskId")
                    return
                }
                ReminderActionHandler.cancelTaskNotification(context, taskId, dateIso)
                runAsync {
                    ReminderActionHandler.handleTaskSnooze(context, intent)
                }
            }

            ACTION_MEDICINE_REMINDER -> {
                val doseId = intent.getLongExtra(NotificationHelper.EXTRA_DOSE_ID, -1L)
                if (doseId <= 0L) {
                    Log.w("ReminderReceiver", "Rejected ACTION_MEDICINE_REMINDER with invalid doseId: $doseId")
                    return
                }
                runAsync {
                    ReminderActionHandler.handleMedicineReminder(context, intent)
                }
            }

            ACTION_MEDICINE_TAKEN -> {
                val doseId = intent.getLongExtra(NotificationHelper.EXTRA_DOSE_ID, -1L)
                if (doseId <= 0L) {
                    Log.w("ReminderReceiver", "Rejected ACTION_MEDICINE_TAKEN with invalid doseId: $doseId")
                    return
                }
                ReminderActionHandler.cancelMedicineNotification(context, doseId)

                val medicationId = intent.getLongExtra(NotificationHelper.EXTRA_MEDICATION_ID, -1L)
                val taskId = intent.getLongExtra(NotificationHelper.EXTRA_TASK_ID, -1L)
                val dateIso = intent.getStringExtra(NotificationHelper.EXTRA_DATE_ISO) ?: DateUtils.todayString()

                runAsync {
                    ReminderActionHandler.handleMedicineTaken(context, doseId, medicationId, taskId, dateIso)
                }
            }

            ACTION_MEDICINE_SNOOZE -> {
                val doseId = intent.getLongExtra(NotificationHelper.EXTRA_DOSE_ID, -1L)
                if (doseId <= 0L) {
                    Log.w("ReminderReceiver", "Rejected ACTION_MEDICINE_SNOOZE with invalid doseId: $doseId")
                    return
                }
                ReminderActionHandler.cancelMedicineNotification(context, doseId)
                runAsync {
                    ReminderActionHandler.handleMedicineSnooze(context, intent)
                }
            }

            ACTION_MEDICINE_SKIP -> {
                val doseId = intent.getLongExtra(NotificationHelper.EXTRA_DOSE_ID, -1L)
                if (doseId <= 0L) {
                    Log.w("ReminderReceiver", "Rejected ACTION_MEDICINE_SKIP with invalid doseId: $doseId")
                    return
                }
                ReminderActionHandler.cancelMedicineNotification(context, doseId)
                runAsync {
                    ReminderActionHandler.handleMedicineSkip(context, doseId)
                }
            }

            ACTION_TEST_REMINDER -> {
                Log.i("ReminderReceiver", "Received ACTION_TEST_REMINDER; delivering test reminder notification")
                runAsync {
                    ReminderActionHandler.handleTestReminder(context)
                }
            }
        }
    }
}
