package com.example.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/**
 * Centralized coordinator for real-time synchronization between the LifeOS application
 * and home screen launcher widgets (Quick Reminders, Subscriptions, Trackers).
 *
 * RC-5 / RC-9 fix: notifyAllWidgetsChanged now broadcasts ACTION_APPWIDGET_UPDATE
 * (triggering a full onUpdate() cycle) instead of just notifyAppWidgetViewDataChanged
 * (which only reloaded adapter data in the old ListView architecture).
 */
object WidgetRefreshCoordinator {

    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /**
     * Refreshes all active LifeOS launcher widgets by broadcasting full update intents.
     * This triggers onUpdate() on each provider, ensuring layout + data are both refreshed.
     */
    fun notifyAllWidgetsChanged(context: Context? = null) {
        val ctx = context?.applicationContext ?: appContext ?: return

        // 1. Refresh Quick Reminders Honeycomb Widget (full update via broadcast)
        try {
            refreshWidget(ctx, QuickRemindersWidgetProvider::class.java)
        } catch (_: Exception) {}

        // 2. Refresh Subscriptions Widget
        try {
            SubscriptionWidgetProvider.notifyDataChanged(ctx)
        } catch (_: Exception) {}

        // 3. Refresh Smart Trackers Widget
        try {
            TrackerWidgetProvider.notifyDataChanged(ctx)
        } catch (_: Exception) {}

        // 4. Refresh Productivity Radar Widget (#17)
        try {
            ProductivityRadarWidgetProvider.notifyDataChanged(ctx)
        } catch (_: Exception) {}
    }

    /**
     * Sends a full ACTION_APPWIDGET_UPDATE broadcast for all instances of the given widget class.
     * This triggers onUpdate() which re-binds the entire RemoteViews layout, not just adapter data.
     */
    private fun <T : android.appwidget.AppWidgetProvider> refreshWidget(
        ctx: Context,
        widgetClass: Class<T>
    ) {
        val mgr = AppWidgetManager.getInstance(ctx)
        val ids = mgr.getAppWidgetIds(ComponentName(ctx, widgetClass))
        if (ids != null && ids.isNotEmpty()) {
            val intent = Intent(ctx, widgetClass).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            ctx.sendBroadcast(intent)
        }
    }
}
