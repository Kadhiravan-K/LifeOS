package com.example.domain.intelligence

import com.example.domain.model.HabitItem
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskPriority
import java.time.LocalDate

enum class OverloadRiskLevel {
    LOW,
    MODERATE,
    HIGH,
    CRITICAL
}

enum class SuggestionType {
    OVERLOAD_WARNING,
    DEEP_WORK_WINDOW,
    RESCHEDULE_OVERDUE,
    HABIT_MOMENTUM,
    RADAR_BALANCE,
    DAILY_ALIGNMENT
}

data class PlanningSuggestion(
    val id: String,
    val title: String,
    val description: String,
    val type: SuggestionType,
    val actionRecommendation: String
)

data class WorkloadAnalysis(
    val totalScheduled: Int,
    val completedCount: Int,
    val pendingCount: Int,
    val highPriorityCount: Int,
    val overdueCount: Int,
    val habitsPendingCount: Int,
    val overloadScore: Int, // 0-100
    val riskLevel: OverloadRiskLevel,
    val suggestions: List<PlanningSuggestion>
)

/**
 * Deterministic, 100% offline intelligent heuristics engine (#3).
 * Evaluates cognitive workload, deadline distribution, habit momentum,
 * and generates non-destructive proactive suggestions.
 */
object IntelligentPlanningEngine {

    fun analyzeWorkload(
        tasks: List<TaskItem>,
        habits: List<HabitItem>,
        todayIso: String = LocalDate.now().toString(),
        lowestRadarDimension: String? = null
    ): WorkloadAnalysis {
        val today = try {
            LocalDate.parse(todayIso)
        } catch (_: Exception) {
            LocalDate.now()
        }

        val totalScheduled = tasks.size
        val completedCount = tasks.count { it.isOneTimeCompleted }
        val pendingTasks = tasks.filter { !it.isOneTimeCompleted }
        val pendingCount = pendingTasks.size
        val highPriorityCount = pendingTasks.count { it.priority == TaskPriority.HIGH }

        val overdueTasks = pendingTasks.filter { task ->
            task.dueDateIso?.let { dateStr ->
                try {
                    LocalDate.parse(dateStr).isBefore(today)
                } catch (_: Exception) {
                    false
                }
            } ?: false
        }
        val overdueCount = overdueTasks.size

        val habitsPendingCount = habits.count { !it.isCompletedToday }

        // Compute overload risk score (0-100)
        var score = 0
        score += (pendingCount * 5).coerceAtMost(35)
        score += (highPriorityCount * 12).coerceAtMost(36)
        score += (overdueCount * 15).coerceAtMost(30)
        score = score.coerceIn(0, 100)

        val riskLevel = when {
            score >= 75 -> OverloadRiskLevel.CRITICAL
            score >= 50 -> OverloadRiskLevel.HIGH
            score >= 25 -> OverloadRiskLevel.MODERATE
            else -> OverloadRiskLevel.LOW
        }

        val suggestions = mutableListOf<PlanningSuggestion>()

        // 1. Overload Warning
        if (highPriorityCount >= 5 || pendingCount >= 10) {
            suggestions.add(
                PlanningSuggestion(
                    id = "sugg_overload",
                    title = "Cognitive Overload Risk",
                    description = "You have $highPriorityCount high-priority and $pendingCount total pending tasks today.",
                    type = SuggestionType.OVERLOAD_WARNING,
                    actionRecommendation = "Apply the Eisenhower Matrix: defer non-urgent items or downgrade priority on secondary tasks."
                )
            )
        }

        // 2. Overdue Rescheduling
        if (overdueCount > 0) {
            suggestions.add(
                PlanningSuggestion(
                    id = "sugg_overdue",
                    title = "$overdueCount Overdue Task${if (overdueCount > 1) "s" else ""}",
                    description = "Uncompleted tasks from prior dates are creating mental drag and attention residue.",
                    type = SuggestionType.RESCHEDULE_OVERDUE,
                    actionRecommendation = "Reschedule them to dedicated time-blocks later this week or clear them if no longer relevant."
                )
            )
        }

        // 3. Deep Work Window
        if (highPriorityCount in 1..4) {
            suggestions.add(
                PlanningSuggestion(
                    id = "sugg_deep_work",
                    title = "Optimal Deep Work Blocks",
                    description = "You have $highPriorityCount focused priorities.",
                    type = SuggestionType.DEEP_WORK_WINDOW,
                    actionRecommendation = "Schedule a 90-minute uninterrupted deep work block in the morning to finish core priorities early."
                )
            )
        }

        // 4. Habit Momentum
        val longestStreak = habits.maxOfOrNull { it.currentStreak } ?: 0
        if (longestStreak >= 3) {
            suggestions.add(
                PlanningSuggestion(
                    id = "sugg_habit_streak",
                    title = "Habit Momentum Active ($longestStreak Days)",
                    description = "$habitsPendingCount daily habit${if (habitsPendingCount != 1) "s" else ""} remaining for completion today.",
                    type = SuggestionType.HABIT_MOMENTUM,
                    actionRecommendation = "Stack remaining habits onto established routines to safeguard your streak."
                )
            )
        }

        // 5. Radar Dimension Balance
        if (!lowestRadarDimension.isNullOrBlank()) {
            suggestions.add(
                PlanningSuggestion(
                    id = "sugg_radar",
                    title = "Productivity Radar: $lowestRadarDimension Balance",
                    description = "Your lowest scoring dimension on the 5-axis productivity radar is $lowestRadarDimension.",
                    type = SuggestionType.RADAR_BALANCE,
                    actionRecommendation = "Consider adding a micro-habit or milestone aligned with $lowestRadarDimension to balance your polygon."
                )
            )
        }

        return WorkloadAnalysis(
            totalScheduled = totalScheduled,
            completedCount = completedCount,
            pendingCount = pendingCount,
            highPriorityCount = highPriorityCount,
            overdueCount = overdueCount,
            habitsPendingCount = habitsPendingCount,
            overloadScore = score,
            riskLevel = riskLevel,
            suggestions = suggestions
        )
    }
}
