package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val emoji: String = "✅",
    val description: String = "",
    val category: String,
    val priority: String = "MEDIUM",
    val dueDateIso: String,
    val dueTime: String? = null,
    val durationMinutes: Int = 30,
    val recurrenceType: String = "NONE",
    val recurrenceInterval: Int = 1,
    val recurrenceDaysOfWeek: String = "", // comma-separated e.g. "TUESDAY,THURSDAY,SUNDAY"
    val recurrenceEndDateIso: String? = null,
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
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
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
