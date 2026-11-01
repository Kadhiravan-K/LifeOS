package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * System Boot Broadcast Receiver (SEC-01 / REM-01).
 *
 * Exported strictly for the Android framework to deliver ACTION_BOOT_COMPLETED,
 * guarded by the signature/privileged android.permission.RECEIVE_BOOT_COMPLETED.
 * Reschedules all active alarms and reminders asynchronously using runAsync() (goAsync()).
 */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i("BootCompletedReceiver", "Received BOOT_COMPLETED; scheduling alarm restoration via runAsync")
            runAsync(timeoutMs = 9_000L) {
                ReminderActionHandler.rescheduleAllAlarmsOnBoot(context)
            }
        }
    }
}
