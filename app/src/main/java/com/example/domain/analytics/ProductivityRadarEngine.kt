package com.example.domain.analytics

import com.example.domain.model.HabitItem
import com.example.domain.model.PlanItem
import com.example.domain.model.TaskItem
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * Pure, deterministic calculation engine for the LifeOS 5-Axis Productivity Radar Chart.
 * Computes scores across:
 * 1. Focus (Tasks)
 * 2. Consistency (Habits)
 * 3. Wellness (Hydration & Health)
 * 4. Vision (Planning & Milestones)
 * 5. Discipline (Streaks & Discipline)
 */
object ProductivityRadarEngine {

    fun calculate(
        tasks: List<TaskItem>,
        habits: List<HabitItem>,
        waterCurrentMl: Int,
        waterGoalMl: Int,
        plans: List<PlanItem> = emptyList(),
        currentStreak: Int = 0,
        todayIso: String = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
    ): ProductivityRadarData {
        // 1. Focus (Tasks): Ratio of completed tasks to total relevant tasks
        val todayTasks = tasks.filter { it.dueDateIso == todayIso }
        val evaluatedTasks = if (todayTasks.isNotEmpty()) todayTasks else tasks.filter { !it.isArchived }
        val totalTasks = evaluatedTasks.size
        val completedTasks = evaluatedTasks.count { it.isCompletedForDate || it.isOneTimeCompleted }
        val taskScore = when {
            totalTasks == 0 -> 80 // Neutral baseline when no tasks are scheduled
            else -> ((completedTasks.toFloat() / totalTasks.toFloat()) * 100f).roundToInt().coerceIn(0, 100)
        }
        val taskScoreModel = RadarDimensionScore(
            dimension = RadarDimension.TASKS,
            scorePercent = taskScore,
            rawValue = "$completedTasks done",
            targetValue = "$totalTasks total",
            statusSummary = if (totalTasks == 0) "No tasks scheduled today" else "$completedTasks of $totalTasks completed"
        )

        // 2. Consistency (Habits): Ratio of habits completed today
        val totalHabits = habits.size
        val completedHabits = habits.count { it.isCompletedToday }
        val habitScore = when {
            totalHabits == 0 -> 75 // Neutral baseline when no habits configured
            else -> ((completedHabits.toFloat() / totalHabits.toFloat()) * 100f).roundToInt().coerceIn(0, 100)
        }
        val habitScoreModel = RadarDimensionScore(
            dimension = RadarDimension.HABITS,
            scorePercent = habitScore,
            rawValue = "$completedHabits checked",
            targetValue = "$totalHabits active",
            statusSummary = if (totalHabits == 0) "No active habits" else "$completedHabits of $totalHabits habits built"
        )

        // 3. Wellness (Hydration & Physical Vitality)
        val safeWaterGoal = if (waterGoalMl > 0) waterGoalMl else 2000
        val waterScore = ((waterCurrentMl.toFloat() / safeWaterGoal.toFloat()) * 100f).roundToInt().coerceIn(0, 100)
        val wellnessScoreModel = RadarDimensionScore(
            dimension = RadarDimension.WELLNESS,
            scorePercent = waterScore,
            rawValue = "${waterCurrentMl}ml",
            targetValue = "${safeWaterGoal}ml",
            statusSummary = "${waterCurrentMl}ml / ${safeWaterGoal}ml water logged"
        )

        // 4. Vision (Planning & Milestones)
        val totalMilestones = plans.sumOf { it.milestones.size }
        val completedMilestones = plans.sumOf { it.milestones.count { m -> m.isCompleted } }
        val planningScore = when {
            totalMilestones == 0 && plans.isEmpty() -> 70 // Baseline
            totalMilestones == 0 -> 75
            else -> ((completedMilestones.toFloat() / totalMilestones.toFloat()) * 100f).roundToInt().coerceIn(0, 100)
        }
        val planningScoreModel = RadarDimensionScore(
            dimension = RadarDimension.PLANNING,
            scorePercent = planningScore,
            rawValue = "$completedMilestones milestones",
            targetValue = "$totalMilestones total",
            statusSummary = if (totalMilestones == 0) "${plans.size} active strategic plans" else "$completedMilestones of $totalMilestones milestones reached"
        )

        // 5. Discipline (Streak momentum)
        // 7 days target for peak weekly streak discipline
        val streakTarget = 7
        val streakRatio = (currentStreak.toFloat() / streakTarget.toFloat()).coerceIn(0f, 1f)
        val disciplineScore = when {
            currentStreak <= 0 -> 40
            currentStreak >= 7 -> 100
            else -> (40f + (streakRatio * 60f)).roundToInt().coerceIn(0, 100)
        }
        val disciplineScoreModel = RadarDimensionScore(
            dimension = RadarDimension.DISCIPLINE,
            scorePercent = disciplineScore,
            rawValue = "$currentStreak days",
            targetValue = "$streakTarget days",
            statusSummary = "$currentStreak day streak ongoing"
        )

        val dimensionScores = listOf(
            taskScoreModel,
            habitScoreModel,
            wellnessScoreModel,
            planningScoreModel,
            disciplineScoreModel
        )

        val overallPercent = (dimensionScores.map { it.scorePercent }.average()).roundToInt().coerceIn(0, 100)

        val (tierName, tierEmoji) = when {
            overallPercent >= 90 -> "Mastery & Peak Flow" to "⚡"
            overallPercent >= 75 -> "Optimal Productivity" to "🚀"
            overallPercent >= 60 -> "Consistent Flow" to "🌱"
            overallPercent >= 40 -> "Building Momentum" to "🔨"
            else -> "Reset & Focus" to "🧘"
        }

        return ProductivityRadarData(
            overallScorePercent = overallPercent,
            ratingTier = tierName,
            ratingTierEmoji = tierEmoji,
            dimensions = dimensionScores,
            calculatedAtIso = todayIso
        )
    }
}
