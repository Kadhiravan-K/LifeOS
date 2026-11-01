package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * System Boot Broadcast Receiver.
 * Exported strictly for the Android framework to deliver ACTION_BOOT_COMPLETED,
 * protected by android.permission.RECEIVE_BOOT_COMPLETED.
 * Reschedules all active alarms and reminders without exposing internal receivers to third parties.
 */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            ReminderBroadcastReceiver.rescheduleAllAlarmsOnBoot(context)
        }
    }
}
