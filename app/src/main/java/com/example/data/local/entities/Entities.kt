package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    indices = [
        Index("profileId"),
        Index("projectId"),
        Index("dueDateIso")
    ]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val profileId: Long = 1,
    val title: String,
    val emoji: String = "",
    val description: String = "",
    val category: String,
    val priority: String = "MEDIUM",
    val dueDateIso: String,
    val dueTime: String? = null,
    val durationMinutes: Int = 30,
    val schedulingMode: String = "DURATION",
    val durationAmount: Int = 30,
    val durationUnit: String = "MINUTES",
    val deadlineIso: String? = null,
    val deadlineTime: String? = null,
    val isAllDay: Boolean = true,
    val recurrenceType: String = "NONE",
    val recurrenceInterval: Int = 1,
    val recurrenceUnit: String = "DAYS",
    val recurrenceDaysOfWeek: String = "", // comma-separated e.g. "TUESDAY,THURSDAY,SUNDAY"
    val recurrenceEndCondition: String = "NEVER",
    val recurrenceEndDateIso: String? = null,
    val recurrenceCount: Int = 1,
    val recurrenceWindowStartDay: Int? = null,
    val recurrenceWindowEndDay: Int? = null,
    val isOneTimeCompleted: Boolean = false,
    val completedDatesJson: String = "[]", // JSON array of "YYYY-MM-DD"
    val skippedDatesJson: String = "[]", // JSON array of skipped "YYYY-MM-DD"
    val rescheduledDatesJson: String = "{}", // JSON map of original "YYYY-MM-DD" -> new "YYYY-MM-DD"
    val subtasksJson: String = "[]", // JSON array of subtasks
    val notes: String = "",
    val isReminderEnabled: Boolean = false,
    val reminderMinutesBefore: Int = 0,
    val isArchived: Boolean = false,
    val isPaused: Boolean = false,
    val isQuickReminder: Boolean = false,
    val projectId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "habits",
    indices = [
        Index("profileId"),
        Index("orderIndex")
    ]
)
data class HabitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val profileId: Long = 1,
    val title: String,
    val emoji: String = "✨",
    val description: String = "",
    val reminderTime: String? = null,
    val colorHex: Long = 0xFF3B82F6,
    val completedDatesJson: String = "[]", // JSON array of "YYYY-MM-DD"
    val orderIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "challenges")
data class ChallengeEntity(
    @PrimaryKey
    val id: Long = 1,
    val startDateIso: String,
    val targetDays: Int = 75,
    val status: String = "ACTIVE",
    val rulesJson: String = "[]",
    val dailyLogsJson: String = "{}",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "water_logs")
data class WaterLogEntity(
    @PrimaryKey
    val dateIso: String,
    val amountMl: Int = 0,
    val goalMl: Int = 2000,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "honeycomb_shortcuts",
    indices = [
        Index("profileId"),
        Index("orderIndex")
    ]
)
data class HoneycombShortcutEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val profileId: Long = 1,
    val type: String, // "TASK", "HABIT", "CHALLENGE_RULE", "WATER", "CUSTOM"
    val targetId: String,
    val title: String,
    val customTileName: String? = null,
    val emoji: String = "✅",
    val customColorHex: Long? = null,
    val colorSource: String = "PROJECT", // "PROJECT", "CUSTOM", "THEME_DEFAULT"
    val shape: String = "HEXAGON", // "HEXAGON", "CIRCLE", "SQUARE"
    val orderIndex: Int = 0,
    val coordQ: Int = 0,
    val coordR: Int = 0,
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
