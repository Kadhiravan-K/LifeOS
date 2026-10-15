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
    }

    val userName: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_USER_NAME] ?: "Master"
    }

    val themeMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_THEME_MODE] ?: "SYSTEM"
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

    suspend fun setUserName(name: String) {
        context.dataStore.edit { it[KEY_USER_NAME] = name }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[KEY_THEME_MODE] = mode }
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
}
