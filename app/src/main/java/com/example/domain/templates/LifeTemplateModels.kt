package com.example.domain.templates

import com.example.domain.model.ChallengeRule
import com.example.domain.model.RecurrenceType
import com.example.domain.model.ShortcutType
import com.example.domain.model.TaskPriority

enum class TemplateCategory(val displayName: String, val emoji: String) {
    DAILY_ROUTINE("Daily Routine", "☀️"),
    MORNING_ROUTINE("Morning Routine", "🌅"),
    EVENING_ROUTINE("Evening Routine", "🌙"),
    PRODUCTIVITY("Productivity", "⚡"),
    STUDY_LEARNING("Study & Learning", "📚"),
    FITNESS("Fitness & Movement", "💪"),
    HEALTH_HYDRATION("Health & Hydration", "💧"),
    HABIT_BUILDING("Habit Building", "🌱"),
    WEEKLY_PLANNING("Weekly Planning", "📅"),
    PERSONAL_DEV("Personal Development", "🧠"),
    CHALLENGE_75("75-Day Challenge", "🔥"),
    BUCKET_LIST("Special Bucket List", "✨"),
    COUPLES_GOALS("Couples Goals", "💑"),
    TRAVEL_CHECKLIST("Travel & Packing", "🧳"),
    EVENT_PLANNING("Event Planning", "💍"),
    STARTUP_LAUNCH("Startup & Business", "🚀");

    companion object {
        fun fromString(value: String): TemplateCategory {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: DAILY_ROUTINE
        }
    }
}

data class TemplateTaskDef(
    val title: String,
    val description: String = "",
    val category: String = "Routine",
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val dueTime: String? = null,
    val durationMinutes: Int = 30,
    val recurrenceType: RecurrenceType = RecurrenceType.DAILY,
    val recurrenceDaysOfWeek: List<String> = emptyList(),
    val isReminderEnabled: Boolean = false,
    val reminderMinutesBefore: Int = 15,
    val emoji: String = "📝",
    val schedulingMode: String = "DURATION",
    val projectName: String? = null,
    val labels: List<String> = emptyList()
)

data class TemplateHabitDef(
    val title: String,
    val emoji: String,
    val description: String = "",
    val reminderTime: String? = null,
    val colorHex: Long = 0xFF4CAF50
)

data class TemplateShortcutDef(
    val type: ShortcutType,
    val targetId: String,
    val title: String,
    val emoji: String,
    val customColorHex: Long? = null,
    val coordQ: Int = 0,
    val coordR: Int = 0
)

data class TemplateProjectDef(
    val name: String,
    val emoji: String = "📁",
    val colorId: String = "blue",
    val description: String = ""
)

data class TemplateChallengeDef(
    val name: String,
    val targetDays: Int = 75,
    val rules: List<ChallengeRule>
)

data class LifeTemplate(
    val id: String,
    val version: Int = 1,
    val title: String,
    val description: String,
    val category: TemplateCategory,
    val emoji: String,
    val tasks: List<TemplateTaskDef> = emptyList(),
    val habits: List<TemplateHabitDef> = emptyList(),
    val shortcuts: List<TemplateShortcutDef> = emptyList(),
    val projects: List<TemplateProjectDef> = emptyList(),
    val challenge: TemplateChallengeDef? = null,
    val tags: List<String> = emptyList()
) {
    val iconEmoji: String
        get() = emoji

    val project: TemplateProjectDef?
        get() = projects.firstOrNull()

    val totalItemsCount: Int
        get() = tasks.size + habits.size + shortcuts.size + projects.size
}

data class TemplateImportSummary(
    val templateId: String,
    val templateTitle: String,
    val tasksImported: Int,
    val habitsImported: Int,
    val shortcutsImported: Int,
    val projectsImported: Int,
    val challengeConfigured: Boolean = false,
    val xpAwarded: Int = 25
) {
    val shortcutsAdded: Int
        get() = shortcutsImported

    val projectsAdded: Int
        get() = projectsImported
}

data class TemplateBundle(
    val id: String,
    val title: String,
    val description: String,
    val emoji: String,
    val isSpecialCollection: Boolean = false,
    val templateIds: List<String>,
    val bonusXp: Int = 50
)

