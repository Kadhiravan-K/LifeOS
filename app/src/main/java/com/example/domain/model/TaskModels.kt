package com.example.domain.model

import androidx.compose.ui.graphics.Color

enum class TaskPriority(
    val code: String,
    val displayName: String,
    val level: Int,
    val colorHex: Long
) {
    URGENT("P1", "P1 (Urgent)", 4, 0xFFDB4035L),
    HIGH("P2", "P2 (High)", 3, 0xFFFF9933L),
    MEDIUM("P3", "P3 (Medium)", 2, 0xFF4073FFL),
    LOW("P4", "P4 (Low)", 1, 0xFF808080L);

    val composeColor: Color get() = Color(colorHex)

    companion object {
        val P1 = URGENT
        val P2 = HIGH
        val P3 = MEDIUM
        val P4 = LOW

        val allPriorities: List<TaskPriority> = listOf(P1, P2, P3, P4)

        fun fromString(value: String): TaskPriority {
            return when (value.uppercase()) {
                "P1", "URGENT" -> URGENT
                "P2", "HIGH" -> HIGH
                "P3", "MEDIUM" -> MEDIUM
                "P4", "LOW" -> LOW
                else -> MEDIUM
            }
        }
    }
}

enum class RecurrenceType(val displayName: String) {
    NONE("Once"),
    DAILY("Every day"),
    EVERY_N_DAYS("Every N days"),
    WEEKDAYS("Every weekday (Mon–Fri)"),
    SELECTED_WEEKDAYS("Selected weekdays"),
    WEEKLY("Every week"),
    EVERY_N_WEEKS("Every N weeks"),
    MONTHLY("Every month"),
    EVERY_N_MONTHS("Every N months"),
    EVERY_6_MONTHS("Every 6 months"),
    YEARLY("Every year"),
    EVERY_N_YEARS("Every N years"),
    CUSTOM("Custom");

    companion object {
        fun fromString(value: String): RecurrenceType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: NONE
        }
    }
}

data class TaskCategoryItem(
    val id: String,
    val name: String,
    val emoji: String,
    val colorHex: Long
)

object DefaultTaskCategories {
    val list = listOf(
        TaskCategoryItem("habits_routine", "Habits & Daily Routine", "🌅", 0xFFF59E0B),
        TaskCategoryItem("personal_care", "Personal & Self-Care", "👤", 0xFF8B5CF6),
        TaskCategoryItem("work_career", "Work & Career", "💼", 0xFF3B82F6),
        TaskCategoryItem("home_cleaning", "Home & Cleaning", "🏠", 0xFF10B981),
        TaskCategoryItem("finance_budget", "Finance & Budget", "💰", 0xFF059669),
        TaskCategoryItem("learning_skills", "Learning & Skills", "📚", 0xFF6366F1),
        TaskCategoryItem("health_family", "Hospital & Family Health", "🏥", 0xFFEF4444),
        TaskCategoryItem("shopping_essentials", "Shopping & Essentials", "🛒", 0xFFEC4899),
        TaskCategoryItem("family_social", "Family & Social", "👨‍👩‍👧", 0xFFF97316),
        TaskCategoryItem("planning_reviews", "Planning & Reviews", "📅", 0xFF14B8A6),
        TaskCategoryItem("challenge_75", "75-Day Challenge", "🔥", 0xFFDC2626)
    )

    fun getCategory(name: String): TaskCategoryItem {
        return list.firstOrNull { it.name.equals(name, ignoreCase = true) }
            ?: TaskCategoryItem("custom", name, "📌", 0xFF6B7280)
    }
}

data class Subtask(
    val id: String,
    val title: String,
    val isCompleted: Boolean = false
)

data class ProjectItem(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val colorId: String = "blue",
    val emoji: String = "📁",
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val orderIndex: Int = 0,
    val taskCount: Int = 0,
    val completedCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val hexColor: String get() = TodoistColorPalette.getColorHex(colorId)
    val composeColor: Color get() = Color(TodoistColorPalette.getColorLong(colorId))
}

data class LabelItem(
    val id: Long = 0,
    val name: String,
    val colorId: String = "blue",
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val orderIndex: Int = 0,
    val taskCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val hexColor: String get() = TodoistColorPalette.getColorHex(colorId)
    val composeColor: Color get() = Color(TodoistColorPalette.getColorLong(colorId))
}

enum class SchedulingMode {
    DURATION,
    DEADLINE;

    companion object {
        fun fromString(value: String?): SchedulingMode {
            return when (value?.uppercase()) {
                "DEADLINE" -> DEADLINE
                else -> DURATION
            }
        }
    }
}

enum class DurationUnit(val displayName: String, val singularName: String) {
    MINUTES("minutes", "minute"),
    HOURS("hours", "hour"),
    DAYS("days", "day"),
    WEEKS("weeks", "week"),
    MONTHS("months", "month"),
    QUARTERS("quarters", "quarter"),
    YEARS("years", "year");

    companion object {
        fun fromString(value: String?): DurationUnit {
            return when (value?.uppercase()) {
                "HOURS" -> HOURS
                "DAYS" -> DAYS
                "WEEKS" -> WEEKS
                "MONTHS" -> MONTHS
                "QUARTERS" -> QUARTERS
                "YEARS" -> YEARS
                else -> MINUTES
            }
        }
    }
}

data class TaskReminderItem(
    val id: Long = 0,
    val taskId: Long = 0,
    val minutesBefore: Int = 0, // 0 = at time, 15 = 15m before, etc.
    val customTime: String? = null,
    val type: String = "OFFSET", // "OFFSET" or "SPECIFIC_TIME"
    val referenceType: String = "START_TIME", // "START_TIME" or "DEADLINE"
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    val displayLabel: String
        get() = when {
            type == "SPECIFIC_TIME" && !customTime.isNullOrBlank() -> "At $customTime"
            minutesBefore == 0 -> if (referenceType == "DEADLINE") "At deadline" else "At time of task"
            minutesBefore == 5 -> "5 minutes before"
            minutesBefore == 10 -> "10 minutes before"
            minutesBefore == 15 -> "15 minutes before"
            minutesBefore == 30 -> "30 minutes before"
            minutesBefore == 45 -> "45 minutes before"
            minutesBefore == 60 -> "1 hour before"
            minutesBefore == 120 -> "2 hours before"
            minutesBefore == 1440 -> "1 day before"
            minutesBefore == 2880 -> "2 days before"
            minutesBefore % 1440 == 0 -> "${minutesBefore / 1440} days before"
            minutesBefore % 60 == 0 -> "${minutesBefore / 60} hours before"
            else -> "$minutesBefore minutes before"
        }
}

data class TaskItem(
    val id: Long = 0,
    val profileId: Long = 1,
    val title: String,
    val emoji: String = "",
    val description: String = "",
    val category: String = "Habits & Daily Routine",
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val dueDateIso: String, // "YYYY-MM-DD"
    val dueTime: String? = null, // "HH:mm"
    val durationMinutes: Int = 30, // Duration in elapsed minutes (default 30)
    val schedulingMode: SchedulingMode = SchedulingMode.DURATION,
    val durationAmount: Int = 30,
    val durationUnit: DurationUnit = DurationUnit.MINUTES,
    val deadlineIso: String? = null,
    val deadlineTime: String? = null,
    val isAllDay: Boolean = true,
    val recurrenceType: RecurrenceType = RecurrenceType.NONE,
    val recurrenceInterval: Int = 1, // For EVERY_N_DAYS, EVERY_N_WEEKS, etc.
    val recurrenceUnit: String = "DAYS", // "DAYS", "WEEKS", "MONTHS", "YEARS"
    val recurrenceDaysOfWeek: List<String> = emptyList(), // e.g. ["TUESDAY", "THURSDAY", "SUNDAY"]
    val recurrenceEndCondition: String = "NEVER", // "NEVER", "ON_DATE", "AFTER_COUNT"
    val recurrenceEndDateIso: String? = null, // Optional end date for recurrence
    val recurrenceCount: Int = 1, // Optional occurrence count limit for recurrence
    val isCompletedForDate: Boolean = false, // Dynamically computed for query date
    val isOneTimeCompleted: Boolean = false, // Base DB flag for one-time tasks
    val completedDates: Set<String> = emptySet(), // Set of "YYYY-MM-DD"
    val skippedDates: Set<String> = emptySet(), // Set of "YYYY-MM-DD" skipped occurrences
    val rescheduledDates: Map<String, String> = emptyMap(), // originalDate -> newDate
    val subtasks: List<Subtask> = emptyList(),
    val notes: String = "",
    val isReminderEnabled: Boolean = false,
    val reminderMinutesBefore: Int = 0,
    val isArchived: Boolean = false,
    val isPaused: Boolean = false,
    val isQuickReminder: Boolean = false,
    val projectId: Long? = null,
    val projectName: String? = null,
    val projectColorId: String? = null,
    val labels: List<LabelItem> = emptyList(),
    val reminders: List<TaskReminderItem> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)
