package com.example.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.legal.LegalConsentManager
import com.example.domain.legal.LegalDocuments
import com.example.domain.model.DashboardShape
import com.example.ui.theme.engine.CustomColorPalette
import com.example.ui.theme.engine.IndependentThemeCustomization
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "lifeos_user_prefs")

class UserPreferences(private val context: Context) {

    companion object {
        val KEY_USER_NAME = stringPreferencesKey("user_name")
        val KEY_USER_PROFILE_EMOJI = stringPreferencesKey("user_profile_emoji")
        val KEY_ACTIVE_PROFILE_ID = longPreferencesKey("active_profile_id")
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
        val KEY_DASHBOARD_SHAPE = stringPreferencesKey("dashboard_shape")

        // Phase 3A Color Picker Preferences
        val KEY_COLOR_ORDERING_MODE = stringPreferencesKey("color_ordering_mode")
        val KEY_COLOR_USAGE_COUNTS = stringPreferencesKey("color_usage_counts")
        val KEY_COLOR_CUSTOM_ORDER = stringPreferencesKey("color_custom_order")

        // Release v1.0 Task Completion Feedback
        val KEY_COMPLETION_SOUND = booleanPreferencesKey("completion_sound_enabled")
        val KEY_COMPLETION_VIBRATION = booleanPreferencesKey("completion_vibration_enabled")
        val KEY_COMPLETION_ANIMATION = booleanPreferencesKey("completion_animation_enabled")
        val KEY_COMPLETION_SOUND_TYPE = stringPreferencesKey("completion_sound_type")
        val KEY_CUSTOM_SOUND_URI = stringPreferencesKey("custom_sound_uri")
        val KEY_CUSTOM_SOUND_NAME = stringPreferencesKey("custom_sound_name")
        val KEY_TRASH_RETENTION_DAYS = intPreferencesKey("trash_retention_days")

        // Clean Onboarding & Templates
        val KEY_HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
        val KEY_INSTALLED_TEMPLATE_IDS = stringPreferencesKey("installed_template_ids_json")

        // Gamification Feedback Preferences
        val KEY_GAMIFICATION_SOUND_ENABLED = booleanPreferencesKey("gamification_sound_enabled")
        val KEY_GAMIFICATION_VIBRATION_ENABLED = booleanPreferencesKey("gamification_vibration_enabled")

        // Smart Alerts & Notifications Preferences
        val KEY_NOTIF_MEDICINE = booleanPreferencesKey("notif_medicine_enabled")
        val KEY_NOTIF_TASKS = booleanPreferencesKey("notif_tasks_enabled")
        val KEY_NOTIF_HABITS = booleanPreferencesKey("notif_habits_enabled")
        val KEY_NOTIF_HYDRATION = booleanPreferencesKey("notif_hydration_enabled")
        val KEY_NOTIF_CALENDAR = booleanPreferencesKey("notif_calendar_enabled")
        val KEY_NOTIF_QR = booleanPreferencesKey("notif_qr_enabled")
        val KEY_NOTIF_CHALLENGES = booleanPreferencesKey("notif_challenges_enabled")
        val KEY_NOTIF_GAMIFICATION = booleanPreferencesKey("notif_gamification_enabled")
        val KEY_NOTIF_DAILY_SUMMARY = booleanPreferencesKey("notif_daily_summary_enabled")

        val KEY_DEFAULT_ALERT_STYLE = stringPreferencesKey("notif_default_alert_style")
        val KEY_NOTIFICATION_SOUND = stringPreferencesKey("notif_sound_choice")
        val KEY_NOTIFICATION_VIBRATION = booleanPreferencesKey("notif_vibration_enabled")
        val KEY_DEFAULT_SNOOZE_MINUTES = intPreferencesKey("notif_snooze_minutes")
        val KEY_FOLLOW_UP_ENABLED = booleanPreferencesKey("notif_follow_up_enabled")
        val KEY_MAX_FOLLOW_UP_COUNT = intPreferencesKey("notif_max_follow_up_count")
        val KEY_MEDICINE_OVERRIDE_QUIET_HOURS = booleanPreferencesKey("notif_medicine_override_quiet_hours")
        val KEY_NOTIFICATION_PRIVACY_ENABLED = booleanPreferencesKey("notif_privacy_enabled")
        val KEY_P1_ALARM_ENABLED = booleanPreferencesKey("notif_p1_alarm_enabled")
        val KEY_P1_SNOOZE_MINUTES = intPreferencesKey("notif_p1_snooze_minutes")
        val KEY_P1_MAX_SNOOZE_COUNT = intPreferencesKey("notif_p1_max_snooze_count")

        // Requirement #22: Legal Consent Preferences
        val KEY_ACCEPTED_TERMS_VERSION = stringPreferencesKey("accepted_terms_version")
        val KEY_ACCEPTED_PRIVACY_VERSION = stringPreferencesKey("accepted_privacy_version")
        val KEY_TERMS_ACCEPTED_TIMESTAMP = longPreferencesKey("terms_accepted_timestamp")

        // Release 3 Phase 2 Preferences (#4, #23)
        val KEY_ANIMATION_SPEED_MULTIPLIER = floatPreferencesKey("animation_speed_multiplier")
        val KEY_CUSTOM_COLORS_ENABLED = booleanPreferencesKey("custom_colors_enabled")

        // Light mode custom colors (Stored as Long ARGB)
        val KEY_CUSTOM_LIGHT_PRIMARY = longPreferencesKey("custom_light_primary")
        val KEY_CUSTOM_LIGHT_SECONDARY = longPreferencesKey("custom_light_secondary")
        val KEY_CUSTOM_LIGHT_ACCENT = longPreferencesKey("custom_light_accent")
        val KEY_CUSTOM_LIGHT_CARD = longPreferencesKey("custom_light_card")
        val KEY_CUSTOM_LIGHT_SURFACE = longPreferencesKey("custom_light_surface")
        val KEY_CUSTOM_LIGHT_BACKGROUND = longPreferencesKey("custom_light_background")

        // Dark mode custom colors (Stored as Long ARGB)
        val KEY_CUSTOM_DARK_PRIMARY = longPreferencesKey("custom_dark_primary")
        val KEY_CUSTOM_DARK_SECONDARY = longPreferencesKey("custom_dark_secondary")
        val KEY_CUSTOM_DARK_ACCENT = longPreferencesKey("custom_dark_accent")
        val KEY_CUSTOM_DARK_CARD = longPreferencesKey("custom_dark_card")
        val KEY_CUSTOM_DARK_SURFACE = longPreferencesKey("custom_dark_surface")
        val KEY_CUSTOM_DARK_BACKGROUND = longPreferencesKey("custom_dark_background")
    }

    // Legal Consent Flows
    val acceptedTermsVersion: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_ACCEPTED_TERMS_VERSION]
    }

    val acceptedPrivacyVersion: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_ACCEPTED_PRIVACY_VERSION]
    }

    val termsAcceptedTimestamp: Flow<Long?> = context.dataStore.data.map { preferences ->
        preferences[KEY_TERMS_ACCEPTED_TIMESTAMP]
    }

    val isLegalConsentComplete: Flow<Boolean> = context.dataStore.data.map { preferences ->
        LegalConsentManager.isConsentComplete(
            acceptedTermsVersion = preferences[KEY_ACCEPTED_TERMS_VERSION],
            acceptedPrivacyVersion = preferences[KEY_ACCEPTED_PRIVACY_VERSION]
        )
    }

    val userName: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_USER_NAME] ?: "Master"
    }

    val userProfileEmoji: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_USER_PROFILE_EMOJI] ?: "👤"
    }

    val activeProfileId: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[KEY_ACTIVE_PROFILE_ID] ?: 1L
    }

    val themeMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_THEME_MODE] ?: "SYSTEM"
    }

    val activeThemeId: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_ACTIVE_THEME_ID] ?: "default"
    }

    val animationSpeedMultiplier: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[KEY_ANIMATION_SPEED_MULTIPLIER] ?: 1.0f
    }

    val customColorsEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_CUSTOM_COLORS_ENABLED] ?: false
    }

    val independentThemeCustomization: Flow<IndependentThemeCustomization> = context.dataStore.data.map { preferences ->
        IndependentThemeCustomization(
            isEnabled = preferences[KEY_CUSTOM_COLORS_ENABLED] ?: false,
            lightPalette = CustomColorPalette(
                primaryHex = preferences[KEY_CUSTOM_LIGHT_PRIMARY],
                secondaryHex = preferences[KEY_CUSTOM_LIGHT_SECONDARY],
                accentHex = preferences[KEY_CUSTOM_LIGHT_ACCENT],
                cardHex = preferences[KEY_CUSTOM_LIGHT_CARD],
                surfaceHex = preferences[KEY_CUSTOM_LIGHT_SURFACE],
                backgroundHex = preferences[KEY_CUSTOM_LIGHT_BACKGROUND]
            ),
            darkPalette = CustomColorPalette(
                primaryHex = preferences[KEY_CUSTOM_DARK_PRIMARY],
                secondaryHex = preferences[KEY_CUSTOM_DARK_SECONDARY],
                accentHex = preferences[KEY_CUSTOM_DARK_ACCENT],
                cardHex = preferences[KEY_CUSTOM_DARK_CARD],
                surfaceHex = preferences[KEY_CUSTOM_DARK_SURFACE],
                backgroundHex = preferences[KEY_CUSTOM_DARK_BACKGROUND]
            )
        )
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

    suspend fun setUserProfileEmoji(emoji: String) {
        context.dataStore.edit { it[KEY_USER_PROFILE_EMOJI] = emoji }
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

    val completionSoundType: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_COMPLETION_SOUND_TYPE] ?: "TASK"
    }

    val customSoundUri: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_CUSTOM_SOUND_URI]
    }

    val customSoundFileName: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_CUSTOM_SOUND_NAME]
    }

    val trashRetentionDays: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_TRASH_RETENTION_DAYS] ?: 30
    }

    suspend fun setCompletionSoundType(type: String) {
        context.dataStore.edit { it[KEY_COMPLETION_SOUND_TYPE] = type }
    }

    suspend fun setCustomSound(uri: String?, fileName: String?) {
        context.dataStore.edit { preferences ->
            if (uri != null) {
                preferences[KEY_CUSTOM_SOUND_URI] = uri
            } else {
                preferences.remove(KEY_CUSTOM_SOUND_URI)
            }
            if (fileName != null) {
                preferences[KEY_CUSTOM_SOUND_NAME] = fileName
            } else {
                preferences.remove(KEY_CUSTOM_SOUND_NAME)
            }
        }
    }

    suspend fun setCustomSoundUri(uri: String?) {
        setCustomSound(uri, null)
    }

    suspend fun clearCustomSound() {
        setCustomSound(null, null)
    }

    suspend fun setTrashRetentionDays(days: Int) {
        context.dataStore.edit { it[KEY_TRASH_RETENTION_DAYS] = days }
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

    val dashboardShape: Flow<DashboardShape> = context.dataStore.data.map { preferences ->
        val raw = preferences[KEY_DASHBOARD_SHAPE] ?: "HEXAGON"
        DashboardShape.fromString(raw)
    }

    suspend fun setDashboardShape(shape: DashboardShape) {
        context.dataStore.edit { it[KEY_DASHBOARD_SHAPE] = shape.name }
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
            it[KEY_DASHBOARD_SHAPE] = DashboardShape.HEXAGON.name
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

    // Smart Alerts & Notifications Flows
    val notifMedicineEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_NOTIF_MEDICINE] ?: true }
    val notifTasksEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_NOTIF_TASKS] ?: true }
    val notifHabitsEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_NOTIF_HABITS] ?: true }
    val notifHydrationEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_NOTIF_HYDRATION] ?: true }
    val notifCalendarEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_NOTIF_CALENDAR] ?: true }
    val notifQrEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_NOTIF_QR] ?: true }
    val notifChallengesEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_NOTIF_CHALLENGES] ?: true }
    val notifGamificationEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_NOTIF_GAMIFICATION] ?: true }
    val notifDailySummaryEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_NOTIF_DAILY_SUMMARY] ?: true }

    val defaultAlertStyle: Flow<String> = context.dataStore.data.map { it[KEY_DEFAULT_ALERT_STYLE] ?: "STANDARD" }
    val notificationSound: Flow<String> = context.dataStore.data.map { it[KEY_NOTIFICATION_SOUND] ?: "CHIME" }
    val notificationVibrationEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_NOTIFICATION_VIBRATION] ?: true }
    val defaultSnoozeMinutes: Flow<Int> = context.dataStore.data.map { it[KEY_DEFAULT_SNOOZE_MINUTES] ?: 10 }
    val followUpEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_FOLLOW_UP_ENABLED] ?: true }
    val maxFollowUpCount: Flow<Int> = context.dataStore.data.map { it[KEY_MAX_FOLLOW_UP_COUNT] ?: 2 }
    val medicineOverrideQuietHours: Flow<Boolean> = context.dataStore.data.map { it[KEY_MEDICINE_OVERRIDE_QUIET_HOURS] ?: true }
    val notificationPrivacyEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_NOTIFICATION_PRIVACY_ENABLED] ?: true }

    val p1AlarmEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_P1_ALARM_ENABLED] ?: true }
    val p1SnoozeMinutes: Flow<Int> = context.dataStore.data.map { it[KEY_P1_SNOOZE_MINUTES] ?: 10 }
    val p1MaxSnoozeCount: Flow<Int> = context.dataStore.data.map { it[KEY_P1_MAX_SNOOZE_COUNT] ?: 3 }

    suspend fun setP1AlarmEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_P1_ALARM_ENABLED] = enabled }
    }

    suspend fun setP1SnoozeMinutes(minutes: Int) {
        context.dataStore.edit { it[KEY_P1_SNOOZE_MINUTES] = minutes }
    }

    suspend fun setP1MaxSnoozeCount(count: Int) {
        context.dataStore.edit { it[KEY_P1_MAX_SNOOZE_COUNT] = count }
    }

    suspend fun setNotifCategoryEnabled(category: String, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            when (category.uppercase()) {
                "MEDICINE" -> prefs[KEY_NOTIF_MEDICINE] = enabled
                "TASKS" -> prefs[KEY_NOTIF_TASKS] = enabled
                "HABITS" -> prefs[KEY_NOTIF_HABITS] = enabled
                "HYDRATION" -> prefs[KEY_NOTIF_HYDRATION] = enabled
                "CALENDAR" -> prefs[KEY_NOTIF_CALENDAR] = enabled
                "QR" -> prefs[KEY_NOTIF_QR] = enabled
                "CHALLENGES" -> prefs[KEY_NOTIF_CHALLENGES] = enabled
                "GAMIFICATION" -> prefs[KEY_NOTIF_GAMIFICATION] = enabled
                "DAILY_SUMMARY" -> prefs[KEY_NOTIF_DAILY_SUMMARY] = enabled
            }
        }
    }

    suspend fun setDefaultAlertStyle(style: String) {
        context.dataStore.edit { it[KEY_DEFAULT_ALERT_STYLE] = style }
    }

    suspend fun setNotificationSound(sound: String) {
        context.dataStore.edit { it[KEY_NOTIFICATION_SOUND] = sound }
    }

    suspend fun setNotificationVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_NOTIFICATION_VIBRATION] = enabled }
    }

    suspend fun setDefaultSnoozeMinutes(minutes: Int) {
        context.dataStore.edit { it[KEY_DEFAULT_SNOOZE_MINUTES] = minutes }
    }

    suspend fun setFollowUpEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_FOLLOW_UP_ENABLED] = enabled }
    }

    suspend fun setMaxFollowUpCount(count: Int) {
        context.dataStore.edit { it[KEY_MAX_FOLLOW_UP_COUNT] = count }
    }

    suspend fun setMedicineOverrideQuietHours(override: Boolean) {
        context.dataStore.edit { it[KEY_MEDICINE_OVERRIDE_QUIET_HOURS] = override }
    }

    suspend fun setNotificationPrivacyEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_NOTIFICATION_PRIVACY_ENABLED] = enabled }
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

    suspend fun recordLegalConsent(
        termsVersion: String = LegalDocuments.CURRENT_TERMS_VERSION,
        privacyVersion: String = LegalDocuments.CURRENT_PRIVACY_VERSION,
        timestamp: Long = System.currentTimeMillis()
    ) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ACCEPTED_TERMS_VERSION] = termsVersion
            preferences[KEY_ACCEPTED_PRIVACY_VERSION] = privacyVersion
            preferences[KEY_TERMS_ACCEPTED_TIMESTAMP] = timestamp
        }
    }

    suspend fun setAnimationSpeedMultiplier(multiplier: Float) {
        context.dataStore.edit { it[KEY_ANIMATION_SPEED_MULTIPLIER] = multiplier }
    }

    suspend fun setCustomColorsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_CUSTOM_COLORS_ENABLED] = enabled }
    }

    suspend fun updateCustomColor(
        isLightMode: Boolean,
        slot: String,
        colorHex: Long?
    ) {
        context.dataStore.edit { prefs ->
            val key = when (slot.lowercase()) {
                "primary" -> if (isLightMode) KEY_CUSTOM_LIGHT_PRIMARY else KEY_CUSTOM_DARK_PRIMARY
                "secondary" -> if (isLightMode) KEY_CUSTOM_LIGHT_SECONDARY else KEY_CUSTOM_DARK_SECONDARY
                "accent" -> if (isLightMode) KEY_CUSTOM_LIGHT_ACCENT else KEY_CUSTOM_DARK_ACCENT
                "card" -> if (isLightMode) KEY_CUSTOM_LIGHT_CARD else KEY_CUSTOM_DARK_CARD
                "surface" -> if (isLightMode) KEY_CUSTOM_LIGHT_SURFACE else KEY_CUSTOM_DARK_SURFACE
                "background" -> if (isLightMode) KEY_CUSTOM_LIGHT_BACKGROUND else KEY_CUSTOM_DARK_BACKGROUND
                else -> null
            }
            if (key != null) {
                if (colorHex != null) {
                    prefs[key] = colorHex
                } else {
                    prefs.remove(key)
                }
            }
        }
    }

    suspend fun resetCustomPalette(isLightMode: Boolean? = null) {
        context.dataStore.edit { prefs ->
            if (isLightMode == null || isLightMode) {
                prefs.remove(KEY_CUSTOM_LIGHT_PRIMARY)
                prefs.remove(KEY_CUSTOM_LIGHT_SECONDARY)
                prefs.remove(KEY_CUSTOM_LIGHT_ACCENT)
                prefs.remove(KEY_CUSTOM_LIGHT_CARD)
                prefs.remove(KEY_CUSTOM_LIGHT_SURFACE)
                prefs.remove(KEY_CUSTOM_LIGHT_BACKGROUND)
            }
            if (isLightMode == null || !isLightMode) {
                prefs.remove(KEY_CUSTOM_DARK_PRIMARY)
                prefs.remove(KEY_CUSTOM_DARK_SECONDARY)
                prefs.remove(KEY_CUSTOM_DARK_ACCENT)
                prefs.remove(KEY_CUSTOM_DARK_CARD)
                prefs.remove(KEY_CUSTOM_DARK_SURFACE)
                prefs.remove(KEY_CUSTOM_DARK_BACKGROUND)
            }
        }
    }

    suspend fun setActiveProfileId(profileId: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACTIVE_PROFILE_ID] = profileId
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
