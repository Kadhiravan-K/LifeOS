package com.example.domain.reset

/**
 * Resettable productivity and data categories in LifeOS.
 * Each category is strictly isolated to the active profile.
 * Protected data (profile identity, credentials, security lock) is NEVER included here.
 */
enum class ResetCategory(
    val title: String,
    val description: String,
    val iconEmoji: String
) {
    TASKS(
        title = "Tasks & Checklists",
        description = "Deletes all active and completed tasks, subtasks, task reminders, and scheduled obligations.",
        iconEmoji = "✅"
    ),
    HABITS(
        title = "Habits & Routines",
        description = "Deletes all daily habits, completion history, streaks, and habit strength scores.",
        iconEmoji = "🔥"
    ),
    PROJECTS(
        title = "Projects & Workspaces",
        description = "Deletes custom projects, workspace items, custom statuses, properties, and views.",
        iconEmoji = "📁"
    ),
    LABELS(
        title = "Tags & Labels",
        description = "Deletes custom tag labels and task-label associations.",
        iconEmoji = "🏷️"
    ),
    PLANS(
        title = "Strategic Planning Boards",
        description = "Deletes Daily, Weekly, Monthly, and Yearly strategic plans and milestones.",
        iconEmoji = "🎯"
    ),
    SHIFTS(
        title = "Shift Planner & Rotations",
        description = "Deletes work shifts, rotation schedules, and calendar shift exceptions.",
        iconEmoji = "⏰"
    ),
    GIVE_GET(
        title = "Give & Get Financials",
        description = "Deletes lent money, borrowed items, gifts, and return reminder tracking.",
        iconEmoji = "💸"
    ),
    TRACKERS(
        title = "Trackers & Cycles",
        description = "Deletes cycle trackers, stock inventory, usage logs, and purchase records.",
        iconEmoji = "📊"
    ),
    SUBSCRIPTIONS(
        title = "Subscriptions & Bills",
        description = "Deletes recurring subscriptions, payment logs, renewal reminders, and bill history.",
        iconEmoji = "💳"
    ),
    MEDICATIONS(
        title = "Medications & Health",
        description = "Deletes medication schedules, dosage times, reminders, and adherence history.",
        iconEmoji = "💊"
    ),
    HONEYCOMB(
        title = "Honeycomb Shortcuts & QR",
        description = "Deletes custom honeycomb shortcut tiles and dashboard arrangements.",
        iconEmoji = "⬡"
    ),
    GAMIFICATION(
        title = "Gamification & XP Progress",
        description = "Resets earned XP points, unlocked badges, streaks, and productivity level to baseline.",
        iconEmoji = "🏆"
    ),
    TRASH(
        title = "Trash Bin Recovery",
        description = "Permanently purges all soft-deleted items currently in the trash bin.",
        iconEmoji = "🗑️"
    )
}

/**
 * Result report for an executed data reset operation.
 */
data class ResetExecutionResult(
    val totalRequested: Int,
    val successfulCategories: List<ResetCategory>,
    val failedCategories: Map<ResetCategory, String>
) {
    val isCompleteSuccess: Boolean get() = failedCategories.isEmpty() && successfulCategories.size == totalRequested
    val isPartialSuccess: Boolean get() = successfulCategories.isNotEmpty() && failedCategories.isNotEmpty()
    val isCompleteFailure: Boolean get() = successfulCategories.isEmpty() && failedCategories.isNotEmpty()
}

/**
 * Security and normalization policy for destructive operations.
 */
object ResetSecurityPolicy {

    /**
     * Normalizes and validates username confirmation.
     * Rejects empty, whitespace-only, or non-matching input.
     * Whitespace is trimmed, and comparison is case-insensitive.
     * If the active profile lacks a usable name, requires "CONFIRM".
     */
    fun isUsernameConfirmed(enteredText: String, profileName: String?): Boolean {
        val trimmedInput = enteredText.trim()
        if (trimmedInput.isBlank()) return false
        val expected = getRequiredConfirmationPhrase(profileName)
        return trimmedInput.equals(expected, ignoreCase = true)
    }

    /**
     * Resolves the required confirmation phrase for the given profile name.
     */
    fun getRequiredConfirmationPhrase(profileName: String?): String {
        return if (!profileName.isNullOrBlank()) profileName.trim() else "CONFIRM"
    }
}
