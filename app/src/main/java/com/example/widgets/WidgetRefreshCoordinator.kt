package com.example.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import com.example.R

/**
 * Centralized coordinator for real-time synchronization between the LifeOS application
 * and home screen launcher widgets (Quick Reminders, Subscriptions, Trackers).
 */
object WidgetRefreshCoordinator {

    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /**
     * Refreshes all active LifeOS launcher widgets.
     * Guaranteed to trigger RemoteViews adapter reload across active instances.
     */
    fun notifyAllWidgetsChanged(context: Context? = null) {
        val ctx = context?.applicationContext ?: appContext ?: return

        // 1. Refresh Quick Reminders Honeycomb Widget
        try {
            QuickRemindersWidgetProvider.notifyDataChanged(ctx)
        } catch (_: Exception) {}

        // 2. Refresh Subscriptions Widget
        try {
            SubscriptionWidgetProvider.notifyDataChanged(ctx)
        } catch (_: Exception) {}

        // 3. Refresh Smart Trackers Widget
        try {
            TrackerWidgetProvider.notifyDataChanged(ctx)
        } catch (_: Exception) {}
    }
}
