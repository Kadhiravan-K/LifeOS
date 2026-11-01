package com.example.widgets

import android.content.Context

/**
 * Manages per-widget-instance configuration preferences (profile binding and content display mode)
 * stored in SharedPreferences keyed by appWidgetId.
 */
object WidgetProfileManager {

    private const val PREFS_NAME = "lifeos_widget_instance_prefs"
    private const val KEY_PROFILE_PREFIX = "widget_profile_id_"
    private const val KEY_CONTENT_MODE_PREFIX = "widget_content_mode_"

    enum class WidgetContentMode(val id: String, val displayName: String, val emoji: String) {
        RADAR("RADAR", "Productivity Radar", "⚡"),
        TODAY_TASKS("TODAY_TASKS", "Today's Tasks", "📅"),
        UPCOMING("UPCOMING", "Upcoming Tasks", "⏳"),
        PRIORITY("PRIORITY", "Priority Overview", "🎯"),
        COMPLETION_SUMMARY("COMPLETION_SUMMARY", "Completion Summary", "📊"),
        CONSISTENCY("CONSISTENCY", "Consistency Score", "🔥");

        companion object {
            fun fromId(id: String?): WidgetContentMode {
                return entries.find { it.id.equals(id, ignoreCase = true) } ?: RADAR
            }
        }
    }

    /**
     * Stores the bound profile ID for a specific widget instance.
     */
    fun saveWidgetProfileId(context: Context, appWidgetId: Int, profileId: Long) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sp.edit().putLong("$KEY_PROFILE_PREFIX$appWidgetId", profileId).apply()
    }

    /**
     * Retrieves the profile ID bound to this widget instance, falling back to [defaultProfileId].
     */
    fun getWidgetProfileId(context: Context, appWidgetId: Int, defaultProfileId: Long = 1L): Long {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sp.getLong("$KEY_PROFILE_PREFIX$appWidgetId", defaultProfileId)
    }

    /**
     * Stores the selected content mode for the productivity radar widget instance.
     */
    fun saveWidgetContentMode(context: Context, appWidgetId: Int, mode: WidgetContentMode) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sp.edit().putString("$KEY_CONTENT_MODE_PREFIX$appWidgetId", mode.id).apply()
    }

    /**
     * Retrieves the content mode for this productivity widget instance.
     */
    fun getWidgetContentMode(context: Context, appWidgetId: Int): WidgetContentMode {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val modeId = sp.getString("$KEY_CONTENT_MODE_PREFIX$appWidgetId", WidgetContentMode.RADAR.id)
        return WidgetContentMode.fromId(modeId)
    }

    /**
     * Cleans up preferences when a widget instance is deleted from the launcher.
     */
    fun removeWidget(context: Context, appWidgetId: Int) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sp.edit()
            .remove("$KEY_PROFILE_PREFIX$appWidgetId")
            .remove("$KEY_CONTENT_MODE_PREFIX$appWidgetId")
            .apply()
    }
}
