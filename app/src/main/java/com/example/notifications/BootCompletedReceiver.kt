package com.example.notifications

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * System Broadcast Receiver for Boot & Reschedule Events (SEC-01 / REM-01 / REM-02 / REM-03 / REM-04).
 *
 * Exported strictly for the Android framework to deliver system lifecycle and time-change events,
 * guarded by the privileged android.permission.RECEIVE_BOOT_COMPLETED.
 *
 * Handles:
 * - Intent.ACTION_BOOT_COMPLETED
 * - Intent.ACTION_MY_PACKAGE_REPLACED
 * - Intent.ACTION_TIME_CHANGED
 * - Intent.ACTION_TIMEZONE_CHANGED
 * - AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
 *
 * Evaluates actions against a strict whitelist, rejects unauthorized actions,
 * and executes centralized scheduling asynchronously via runAsync() (goAsync()).
 */
class BootCompletedReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootCompletedReceiver"

        val ALLOWED_ACTIONS: Set<String> = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
        )
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == null || action !in ALLOWED_ACTIONS) {
            Log.w(TAG, "Ignoring unrecognized or unapproved system broadcast action: $action")
            return
        }

        Log.i(TAG, "Received system broadcast '$action'; rescheduling alarms via runAsync")
        runAsync(timeoutMs = 9_000L) {
            ReminderScheduler.rescheduleAll(context, horizonDays = 7)
        }
    }
}
