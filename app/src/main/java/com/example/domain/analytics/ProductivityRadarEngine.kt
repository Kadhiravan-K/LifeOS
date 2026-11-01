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
 *
 * Supports multi-period evaluation: Daily, Weekly, Monthly, and Yearly.
 */
object ProductivityRadarEngine {

    fun calculate(
        tasks: List<TaskItem>,
        habits: List<HabitItem>,
        waterCurrentMl: Int,
        waterGoalMl: Int,
        plans: List<PlanItem> = emptyList(),
        currentStreak: Int = 0,
        period: ProductivityPeriod = ProductivityPeriod.DAILY,
        todayIso: String = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
    ): ProductivityRadarData {
        val today = try {
            LocalDate.parse(todayIso, DateTimeFormatter.ISO_DATE)
        } catch (_: Exception) {
            LocalDate.now()
        }

        // Determine date range for period
        val periodStartDate = when (period) {
            ProductivityPeriod.DAILY -> today
            ProductivityPeriod.WEEKLY -> today.minusDays(6)
            ProductivityPeriod.MONTHLY -> today.minusDays(29)
            ProductivityPeriod.YEARLY -> today.minusDays(364)
        }
        val periodStartDateIso = periodStartDate.format(DateTimeFormatter.ISO_DATE)

        val hasAnyData = tasks.isNotEmpty() || habits.isNotEmpty() || plans.isNotEmpty() || waterCurrentMl > 0 || currentStreak > 0

        // 1. Focus (Tasks)
        val evaluatedTasks = when (period) {
            ProductivityPeriod.DAILY -> {
                val dueToday = tasks.filter { it.dueDateIso == todayIso }
                if (dueToday.isNotEmpty()) dueToday else tasks.filter { !it.isArchived }
            }
            ProductivityPeriod.WEEKLY, ProductivityPeriod.MONTHLY, ProductivityPeriod.YEARLY -> {
                val inRange = tasks.filter {
                    val d = it.dueDateIso
                    !d.isNullOrBlank() && d >= periodStartDateIso && d <= todayIso
                }
                if (inRange.isNotEmpty()) inRange else tasks.filter { !it.isArchived }
            }
        }
        val totalTasks = evaluatedTasks.size
        val completedTasks = evaluatedTasks.count { it.isCompletedForDate || it.isOneTimeCompleted }
        val taskScore = when {
            totalTasks == 0 -> 80 // Neutral baseline when no tasks are scheduled
            else -> ((completedTasks.toFloat() / totalTasks.toFloat()) * 100f).roundToInt().coerceIn(0, 100)
        }
        val taskSummary = when {
            totalTasks == 0 -> "No tasks scheduled for ${period.displayName.lowercase()}"
            else -> "$completedTasks of $totalTasks tasks completed"
        }
        val taskScoreModel = RadarDimensionScore(
            dimension = RadarDimension.TASKS,
            scorePercent = taskScore,
            rawValue = "$completedTasks done",
            targetValue = "$totalTasks total",
            statusSummary = taskSummary
        )

        // 2. Consistency (Habits)
        val totalHabits = habits.size
        val habitScore: Int
        val habitRaw: String
        val habitTarget: String
        val habitSummary: String

        when (period) {
            ProductivityPeriod.DAILY -> {
                val completedToday = habits.count { it.isCompletedToday }
                habitScore = when {
                    totalHabits == 0 -> 75
                    else -> ((completedToday.toFloat() / totalHabits.toFloat()) * 100f).roundToInt().coerceIn(0, 100)
                }
                habitRaw = "$completedToday checked"
                habitTarget = "$totalHabits active"
                habitSummary = if (totalHabits == 0) "No active habits" else "$completedToday of $totalHabits habits built"
            }
            ProductivityPeriod.WEEKLY -> {
                if (totalHabits == 0) {
                    habitScore = 75
                    habitRaw = "0 checked"
                    habitTarget = "0 habits"
                    habitSummary = "No active habits configured"
                } else {
                    // Check completion across past 7 days
                    val past7Dates = (0..6).map { today.minusDays(it.toLong()).format(DateTimeFormatter.ISO_DATE) }.toSet()
                    val totalPossibleLogs = totalHabits * 7
                    val actualCompletedLogs = habits.sumOf { h -> h.completedDates.count { it in past7Dates } }
                    habitScore = if (totalPossibleLogs > 0) {
                        ((actualCompletedLogs.toFloat() / totalPossibleLogs.toFloat()) * 100f).roundToInt().coerceIn(0, 100)
                    } else 75
                    habitRaw = "$actualCompletedLogs logs"
                    habitTarget = "$totalPossibleLogs possible"
                    habitSummary = "$actualCompletedLogs completions across 7 days"
                }
            }
            ProductivityPeriod.MONTHLY -> {
                if (totalHabits == 0) {
                    habitScore = 75
                    habitRaw = "0 checked"
                    habitTarget = "0 habits"
                    habitSummary = "No active habits configured"
                } else {
                    val past30Dates = (0..29).map { today.minusDays(it.toLong()).format(DateTimeFormatter.ISO_DATE) }.toSet()
                    val totalPossibleLogs = totalHabits * 30
                    val actualCompletedLogs = habits.sumOf { h -> h.completedDates.count { it in past30Dates } }
                    habitScore = if (totalPossibleLogs > 0) {
                        ((actualCompletedLogs.toFloat() / totalPossibleLogs.toFloat()) * 100f).roundToInt().coerceIn(0, 100)
                    } else 75
                    habitRaw = "$actualCompletedLogs logs"
                    habitTarget = "$totalPossibleLogs possible"
                    habitSummary = "$actualCompletedLogs completions across 30 days"
                }
            }
            ProductivityPeriod.YEARLY -> {
                if (totalHabits == 0) {
                    habitScore = 75
                    habitRaw = "0 checked"
                    habitTarget = "0 habits"
                    habitSummary = "No active habits configured"
                } else {
                    val avgStreak = habits.map { it.currentStreak }.average().toFloat()
                    habitScore = (avgStreak * 5f).roundToInt().coerceIn(30, 100)
                    habitRaw = "${avgStreak.roundToInt()}d avg"
                    habitTarget = "20d streak"
                    habitSummary = "Macro habit consistency"
                }
            }
        }

        val habitScoreModel = RadarDimensionScore(
            dimension = RadarDimension.HABITS,
            scorePercent = habitScore,
            rawValue = habitRaw,
            targetValue = habitTarget,
            statusSummary = habitSummary
        )

        // 3. Wellness (Hydration & Physical Vitality)
        val safeWaterGoal = if (waterGoalMl > 0) waterGoalMl else 2000
        val waterScore = when (period) {
            ProductivityPeriod.DAILY -> ((waterCurrentMl.toFloat() / safeWaterGoal.toFloat()) * 100f).roundToInt().coerceIn(0, 100)
            else -> {
                // For periods > daily, evaluate relative to base goal with stability floor
                if (waterCurrentMl > 0) {
                    ((waterCurrentMl.toFloat() / safeWaterGoal.toFloat()) * 100f).roundToInt().coerceIn(0, 100)
                } else 60
            }
        }
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
            totalMilestones == 0 && plans.isEmpty() -> 70
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
        val streakTarget = when (period) {
            ProductivityPeriod.DAILY, ProductivityPeriod.WEEKLY -> 7
            ProductivityPeriod.MONTHLY -> 30
            ProductivityPeriod.YEARLY -> 100
        }
        val streakRatio = (currentStreak.toFloat() / streakTarget.toFloat()).coerceIn(0f, 1f)
        val disciplineScore = when {
            currentStreak <= 0 -> 40
            currentStreak >= streakTarget -> 100
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

        // Actionable guidance tip
        val guidanceTip = if (!hasAnyData) {
            "Your radar is calibrated with neutral baselines. Complete tasks, log habits, or drink water to generate your live productivity polygon."
        } else {
            val lowestDim = dimensionScores.minByOrNull { it.scorePercent }
            when (lowestDim?.dimension) {
                RadarDimension.WELLNESS -> "Hydration is trailing your focus score. A quick glass of water boosts both wellness and cognitive performance!"
                RadarDimension.TASKS -> "Daily focus tasks need attention today. Tackle your top priority task first!"
                RadarDimension.HABITS -> "Habit consistency has room to grow. Complete a quick routine now!"
                RadarDimension.PLANNING -> "Add milestones to your strategic plans to boost your vision horizon!"
                RadarDimension.DISCIPLINE -> "Build your daily streak consistency to elevate your discipline rating!"
                else -> if (overallPercent >= 85) "Phenomenal balance across all 5 dimensions. Keep maintaining your peak flow!" else "Steady progress across all pillars."
            }
        }

        return ProductivityRadarData(
            overallScorePercent = overallPercent,
            ratingTier = tierName,
            ratingTierEmoji = tierEmoji,
            dimensions = dimensionScores,
            calculatedAtIso = todayIso,
            period = period,
            hasSufficientData = hasAnyData,
            guidanceTip = guidanceTip
        )
    }
}

