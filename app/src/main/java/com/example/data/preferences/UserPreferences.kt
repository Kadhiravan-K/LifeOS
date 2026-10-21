package com.example.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "lifeos_user_prefs")

class UserPreferences(private val context: Context) {

    companion object {
        val KEY_USER_NAME = stringPreferencesKey("user_name")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_ACTIVE_THEME_ID = stringPreferencesKey("active_theme_id")
        val KEY_WATER_GOAL = intPreferencesKey("water_goal_ml")
        val KEY_HAS_INSTALLED_DEFAULTS = booleanPreferencesKey("has_installed_default_routines")
        val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val KEY_REMINDER_TIME = stringPreferencesKey("daily_reminder_time")

        // Phase 2 Settings
        val KEY_WAKE_UP_TIME = stringPreferencesKey("wake_up_time")
        val KEY_BED_TIME = stringPreferencesKey("bed_time")
        val KEY_QUIET_HOURS_START = stringPreferencesKey("quiet_hours_start")
        val KEY_QUIET_HOURS_END = stringPreferencesKey("quiet_hours_end")
        val KEY_DEFAULT_REMINDER_BEHAVIOR = stringPreferencesKey("default_reminder_behavior")
        val KEY_FIRST_DAY_OF_WEEK = stringPreferencesKey("first_day_of_week")
        val KEY_CLOCK_FORMAT = stringPreferencesKey("clock_format")
        val KEY_TASK_SORT_PREFERENCE = stringPreferencesKey("task_sort_preference")
        val KEY_DEFAULT_TASK_DURATION = intPreferencesKey("default_task_duration")

        // Phase 2.6 Honeycomb Appearance
        val KEY_HONEYCOMB_DEFAULT_COLOR = stringPreferencesKey("honeycomb_default_color")
        val KEY_HONEYCOMB_BORDER_COLOR = stringPreferencesKey("honeycomb_border_color")
        val KEY_HONEYCOMB_BORDER_WIDTH = intPreferencesKey("honeycomb_border_width")
        val KEY_HONEYCOMB_SIZE = intPreferencesKey("honeycomb_size")
        val KEY_HONEYCOMB_SPACING = intPreferencesKey("honeycomb_spacing")
        val KEY_HONEYCOMB_ICON_SIZE = intPreferencesKey("honeycomb_icon_size")
        val KEY_HONEYCOMB_ANIMATION = booleanPreferencesKey("honeycomb_animation")

        // Phase 3A Color Picker Preferences
        val KEY_COLOR_ORDERING_MODE = stringPreferencesKey("color_ordering_mode")
        val KEY_COLOR_USAGE_COUNTS = stringPreferencesKey("color_usage_counts")
        val KEY_COLOR_CUSTOM_ORDER = stringPreferencesKey("color_custom_order")

        // Release v1.0 Task Completion Feedback
        val KEY_COMPLETION_SOUND = booleanPreferencesKey("completion_sound_enabled")
        val KEY_COMPLETION_VIBRATION = booleanPreferencesKey("completion_vibration_enabled")
        val KEY_COMPLETION_ANIMATION = booleanPreferencesKey("completion_animation_enabled")

        // Clean Onboarding & Templates
        val KEY_HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
        val KEY_INSTALLED_TEMPLATE_IDS = stringPreferencesKey("installed_template_ids_json")

        // Gamification Feedback Preferences
        val KEY_GAMIFICATION_SOUND_ENABLED = booleanPreferencesKey("gamification_sound_enabled")
        val KEY_GAMIFICATION_VIBRATION_ENABLED = booleanPreferencesKey("gamification_vibration_enabled")
    }

    val userName: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_USER_NAME] ?: "Master"
    }

    val themeMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_THEME_MODE] ?: "SYSTEM"
    }

    val activeThemeId: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_ACTIVE_THEME_ID] ?: "default"
    }

    val waterGoalMl: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_WATER_GOAL] ?: 2000
    }

    val hasInstalledDefaultRoutines: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_HAS_INSTALLED_DEFAULTS] ?: false
    }

    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_NOTIFICATIONS_ENABLED] ?: true
    }

    val reminderTime: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_REMINDER_TIME] ?: "08:00"
    }

    val wakeUpTime: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_WAKE_UP_TIME] ?: "05:30"
    }

    val bedTime: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_BED_TIME] ?: "21:30"
    }

    val quietHoursStart: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_QUIET_HOURS_START] ?: "21:00"
    }

    val quietHoursEnd: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_QUIET_HOURS_END] ?: "05:30"
    }

    val defaultReminderBehavior: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_DEFAULT_REMINDER_BEHAVIOR] ?: "AT_TIME"
    }

    val firstDayOfWeek: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_FIRST_DAY_OF_WEEK] ?: "MONDAY"
    }

    val clockFormat: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_CLOCK_FORMAT] ?: "12_HOUR"
    }

    val taskSortPreference: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_TASK_SORT_PREFERENCE] ?: "Priority"
    }

    val defaultTaskDuration: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_DEFAULT_TASK_DURATION] ?: 30
    }

    val hasCompletedOnboarding: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_HAS_COMPLETED_ONBOARDING] ?: false
    }

    val installedTemplateIds: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        val raw = preferences[KEY_INSTALLED_TEMPLATE_IDS] ?: ""
        if (raw.isBlank()) emptySet() else raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    val gamificationSoundEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_GAMIFICATION_SOUND_ENABLED] ?: true
    }

    val gamificationVibrationEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_GAMIFICATION_VIBRATION_ENABLED] ?: true
    }

    suspend fun setHasCompletedOnboarding(completed: Boolean) {
        context.dataStore.edit { it[KEY_HAS_COMPLETED_ONBOARDING] = completed }
    }

    suspend fun addInstalledTemplateId(templateId: String) {
        context.dataStore.edit { prefs ->
            val currentRaw = prefs[KEY_INSTALLED_TEMPLATE_IDS] ?: ""
            val currentSet = if (currentRaw.isBlank()) mutableSetOf() else currentRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableSet()
            currentSet.add(templateId)
            prefs[KEY_INSTALLED_TEMPLATE_IDS] = currentSet.joinToString(",")
        }
    }

    suspend fun removeInstalledTemplateId(templateId: String) {
        context.dataStore.edit { prefs ->
            val currentRaw = prefs[KEY_INSTALLED_TEMPLATE_IDS] ?: ""
            val currentSet = if (currentRaw.isBlank()) mutableSetOf() else currentRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableSet()
            currentSet.remove(templateId)
            prefs[KEY_INSTALLED_TEMPLATE_IDS] = currentSet.joinToString(",")
        }
    }

    suspend fun clearInstalledTemplates() {
        context.dataStore.edit { it.remove(KEY_INSTALLED_TEMPLATE_IDS) }
    }

    suspend fun setGamificationSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_GAMIFICATION_SOUND_ENABLED] = enabled }
    }

    suspend fun setGamificationVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_GAMIFICATION_VIBRATION_ENABLED] = enabled }
    }

    suspend fun setUserName(name: String) {
        context.dataStore.edit { it[KEY_USER_NAME] = name }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[KEY_THEME_MODE] = mode }
    }

    suspend fun setActiveThemeId(themeId: String) {
        context.dataStore.edit { it[KEY_ACTIVE_THEME_ID] = themeId }
    }

    suspend fun setWaterGoalMl(goal: Int) {
        context.dataStore.edit { it[KEY_WATER_GOAL] = goal }
    }

    suspend fun setHasInstalledDefaultRoutines(installed: Boolean) {
        context.dataStore.edit { it[KEY_HAS_INSTALLED_DEFAULTS] = installed }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setReminderTime(time: String) {
        context.dataStore.edit { it[KEY_REMINDER_TIME] = time }
    }

    suspend fun setWakeUpTime(time: String) {
        context.dataStore.edit { it[KEY_WAKE_UP_TIME] = time }
    }

    suspend fun setBedTime(time: String) {
        context.dataStore.edit { it[KEY_BED_TIME] = time }
    }

    suspend fun setQuietHours(start: String, end: String) {
        context.dataStore.edit {
            it[KEY_QUIET_HOURS_START] = start
            it[KEY_QUIET_HOURS_END] = end
        }
    }

    suspend fun setDefaultReminderBehavior(behavior: String) {
        context.dataStore.edit { it[KEY_DEFAULT_REMINDER_BEHAVIOR] = behavior }
    }

    suspend fun setFirstDayOfWeek(day: String) {
        context.dataStore.edit { it[KEY_FIRST_DAY_OF_WEEK] = day }
    }

    suspend fun setClockFormat(format: String) {
        context.dataStore.edit { it[KEY_CLOCK_FORMAT] = format }
    }

    suspend fun setTaskSortPreference(sort: String) {
        context.dataStore.edit { it[KEY_TASK_SORT_PREFERENCE] = sort }
    }

    suspend fun setDefaultTaskDuration(minutes: Int) {
        context.dataStore.edit { it[KEY_DEFAULT_TASK_DURATION] = minutes }
    }

    // Task Completion Feedback Flows
    val completionSoundEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_COMPLETION_SOUND] ?: true
    }

    val completionVibrationEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_COMPLETION_VIBRATION] ?: true
    }

    val completionAnimationEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_COMPLETION_ANIMATION] ?: true
    }

    suspend fun setCompletionSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_COMPLETION_SOUND] = enabled }
    }

    suspend fun setCompletionVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_COMPLETION_VIBRATION] = enabled }
    }

    suspend fun setCompletionAnimationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_COMPLETION_ANIMATION] = enabled }
    }

    // Honeycomb Flows
    val honeycombDefaultColorHex: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[KEY_HONEYCOMB_DEFAULT_COLOR]?.toLongOrNull() ?: 0xFF10B981L
    }

    val honeycombBorderColor: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_HONEYCOMB_BORDER_COLOR] ?: "DEFAULT"
    }

    val honeycombBorderWidthDp: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_HONEYCOMB_BORDER_WIDTH] ?: 4
    }

    val honeycombSizeDp: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_HONEYCOMB_SIZE] ?: 74
    }

    val honeycombSpacingDp: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_HONEYCOMB_SPACING] ?: 6
    }

    val honeycombIconSizeSp: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_HONEYCOMB_ICON_SIZE] ?: 26
    }

    val honeycombAnimationEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_HONEYCOMB_ANIMATION] ?: true
    }

    suspend fun setHoneycombDefaultColor(hex: Long) {
        context.dataStore.edit { it[KEY_HONEYCOMB_DEFAULT_COLOR] = hex.toString() }
    }

    suspend fun setHoneycombBorderColor(type: String) {
        context.dataStore.edit { it[KEY_HONEYCOMB_BORDER_COLOR] = type }
    }

    suspend fun setHoneycombBorderWidth(widthDp: Int) {
        context.dataStore.edit { it[KEY_HONEYCOMB_BORDER_WIDTH] = widthDp }
    }

    suspend fun setHoneycombSize(sizeDp: Int) {
        context.dataStore.edit { it[KEY_HONEYCOMB_SIZE] = sizeDp }
    }

    suspend fun setHoneycombSpacing(spacingDp: Int) {
        context.dataStore.edit { it[KEY_HONEYCOMB_SPACING] = spacingDp }
    }

    suspend fun setHoneycombIconSize(sizeSp: Int) {
        context.dataStore.edit { it[KEY_HONEYCOMB_ICON_SIZE] = sizeSp }
    }

    suspend fun setHoneycombAnimationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_HONEYCOMB_ANIMATION] = enabled }
    }

    suspend fun resetHoneycombAppearance() {
        context.dataStore.edit {
            it[KEY_HONEYCOMB_DEFAULT_COLOR] = 0xFF10B981L.toString()
            it[KEY_HONEYCOMB_BORDER_COLOR] = "DEFAULT"
            it[KEY_HONEYCOMB_BORDER_WIDTH] = 4
            it[KEY_HONEYCOMB_SIZE] = 74
            it[KEY_HONEYCOMB_SPACING] = 6
            it[KEY_HONEYCOMB_ICON_SIZE] = 26
            it[KEY_HONEYCOMB_ANIMATION] = true
        }
    }

    // Phase 3A Color Picker Flows & Helpers
    val colorOrderingMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_COLOR_ORDERING_MODE] ?: "AUTO"
    }

    val colorUsageCounts: Flow<Map<String, Int>> = context.dataStore.data.map { preferences ->
        val raw = preferences[KEY_COLOR_USAGE_COUNTS] ?: ""
        parseUsageCounts(raw)
    }

    val colorCustomOrder: Flow<List<String>> = context.dataStore.data.map { preferences ->
        val raw = preferences[KEY_COLOR_CUSTOM_ORDER] ?: ""
        if (raw.isBlank()) emptyList() else raw.split(",").map { it.trim() }
    }

    suspend fun setColorOrderingMode(mode: String) {
        context.dataStore.edit { it[KEY_COLOR_ORDERING_MODE] = mode }
    }

    suspend fun recordColorUsage(colorId: String) {
        context.dataStore.edit { prefs ->
            val currentRaw = prefs[KEY_COLOR_USAGE_COUNTS] ?: ""
            val map = parseUsageCounts(currentRaw).toMutableMap()
            map[colorId] = (map[colorId] ?: 0) + 1
            prefs[KEY_COLOR_USAGE_COUNTS] = map.entries.joinToString(",") { "${it.key}:${it.value}" }
        }
    }

    suspend fun saveCustomColorOrder(order: List<String>) {
        context.dataStore.edit {
            it[KEY_COLOR_CUSTOM_ORDER] = order.joinToString(",")
        }
    }

    suspend fun resetCustomColorOrder() {
        context.dataStore.edit {
            it.remove(KEY_COLOR_CUSTOM_ORDER)
        }
    }

    private fun parseUsageCounts(raw: String): Map<String, Int> {
        if (raw.isBlank()) return emptyMap()
        return raw.split(",").mapNotNull { part ->
            val pieces = part.split(":")
            if (pieces.size == 2) {
                pieces[0].trim() to (pieces[1].trim().toIntOrNull() ?: 0)
            } else null
        }.toMap()
    }
}
