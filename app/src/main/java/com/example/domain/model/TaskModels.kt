package com.example.domain.model

enum class TaskPriority(val displayName: String, val level: Int) {
    LOW("Low", 1),
    MEDIUM("Medium", 2),
    HIGH("High", 3),
    URGENT("Urgent", 4);

    companion object {
        fun fromString(value: String): TaskPriority {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: MEDIUM
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

data class TaskItem(
    val id: Long = 0,
    val title: String,
    val emoji: String = "✅",
    val description: String = "",
    val category: String = "Habits & Daily Routine",
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val dueDateIso: String, // "YYYY-MM-DD"
    val dueTime: String? = null, // "HH:mm"
    val durationMinutes: Int = 30, // Duration in minutes (default 30)
    val recurrenceType: RecurrenceType = RecurrenceType.NONE,
    val recurrenceInterval: Int = 1, // For EVERY_N_DAYS, EVERY_N_WEEKS, EVERY_N_MONTHS
    val recurrenceDaysOfWeek: List<String> = emptyList(), // e.g. ["TUESDAY", "THURSDAY", "SUNDAY"]
    val recurrenceEndDateIso: String? = null, // Optional end date for recurrence
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
    val createdAt: Long = System.currentTimeMillis()
)
