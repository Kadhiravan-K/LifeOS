package com.example.domain.analytics

import com.example.domain.model.HabitItem
import com.example.domain.model.PlanItem
import com.example.domain.model.PlanLevel
import com.example.domain.model.PlanMilestone
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskPriority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Comprehensive Unit Tests for ProductivityRadarEngine (#17).
 * Validates deterministic mathematical scoring across 5 axes:
 * 1. Focus (Tasks)
 * 2. Consistency (Habits)
 * 3. Wellness (Hydration)
 * 4. Vision (Planning)
 * 5. Discipline (Streaks)
 *
 * Covers all time periods: Daily, Weekly, Monthly, Yearly,
 * zero division protection, sparse data guidance, and score bounds.
 */
class ProductivityRadarEngineUnitTest {

    private val todayIso = "2026-11-01"

    @Test
    fun `test empty data yields neutral baseline and uncalibrated guidance`() {
        val result = ProductivityRadarEngine.calculate(
            tasks = emptyList(),
            habits = emptyList(),
            waterCurrentMl = 0,
            waterGoalMl = 2000,
            plans = emptyList(),
            currentStreak = 0,
            period = ProductivityPeriod.DAILY,
            todayIso = todayIso
        )

        assertFalse("Empty data must indicate hasSufficientData == false", result.hasSufficientData)
        assertTrue("Guidance tip should prompt user to start logging", result.guidanceTip.contains("calibrated with neutral baselines"))
        assertEquals("Neutral task baseline", 80, result.scoreFor(RadarDimension.TASKS))
        assertEquals("Neutral habit baseline", 75, result.scoreFor(RadarDimension.HABITS))
        assertEquals("Zero hydration", 0, result.scoreFor(RadarDimension.WELLNESS))
        assertEquals("Neutral planning baseline", 70, result.scoreFor(RadarDimension.PLANNING))
        assertEquals("Base streak discipline", 40, result.scoreFor(RadarDimension.DISCIPLINE))
    }

    @Test
    fun `test daily calculation 100 percent completion`() {
        val tasks = listOf(
            TaskItem(id = 1, title = "Task 1", dueDateIso = todayIso, isOneTimeCompleted = true),
            TaskItem(id = 2, title = "Task 2", dueDateIso = todayIso, isOneTimeCompleted = true)
        )
        val habits = listOf(
            HabitItem(id = 1, title = "Habit 1", isCompletedToday = true),
            HabitItem(id = 2, title = "Habit 2", isCompletedToday = true)
        )
        val plans = listOf(
            PlanItem(
                id = 1,
                title = "Launch Release 3",
                level = PlanLevel.MONTHLY,
                startDateIso = todayIso,
                targetDateIso = todayIso,
                milestones = listOf(
                    PlanMilestone(id = 1, planId = 1, title = "Feature Complete", isCompleted = true),
                    PlanMilestone(id = 2, planId = 1, title = "Testing", isCompleted = true)
                )
            )
        )

        val result = ProductivityRadarEngine.calculate(
            tasks = tasks,
            habits = habits,
            waterCurrentMl = 2500,
            waterGoalMl = 2000,
            plans = plans,
            currentStreak = 7,
            period = ProductivityPeriod.DAILY,
            todayIso = todayIso
        )

        assertTrue(result.hasSufficientData)
        assertEquals(100, result.scoreFor(RadarDimension.TASKS))
        assertEquals(100, result.scoreFor(RadarDimension.HABITS))
        assertEquals(100, result.scoreFor(RadarDimension.WELLNESS))
        assertEquals(100, result.scoreFor(RadarDimension.PLANNING))
        assertEquals(100, result.scoreFor(RadarDimension.DISCIPLINE))
        assertEquals(100, result.overallScorePercent)
        assertEquals("Mastery & Peak Flow", result.ratingTier)
        assertEquals("⚡", result.ratingTierEmoji)
    }

    @Test
    fun `test daily calculation partial completion`() {
        val tasks = listOf(
            TaskItem(id = 1, title = "Task 1", dueDateIso = todayIso, isOneTimeCompleted = true),
            TaskItem(id = 2, title = "Task 2", dueDateIso = todayIso, isOneTimeCompleted = false),
            TaskItem(id = 3, title = "Task 3", dueDateIso = todayIso, isOneTimeCompleted = false),
            TaskItem(id = 4, title = "Task 4", dueDateIso = todayIso, isOneTimeCompleted = false)
        )
        val habits = listOf(
            HabitItem(id = 1, title = "Habit 1", isCompletedToday = true),
            HabitItem(id = 2, title = "Habit 2", isCompletedToday = false)
        )

        val result = ProductivityRadarEngine.calculate(
            tasks = tasks,
            habits = habits,
            waterCurrentMl = 1000,
            waterGoalMl = 2000,
            plans = emptyList(),
            currentStreak = 3,
            period = ProductivityPeriod.DAILY,
            todayIso = todayIso
        )

        assertTrue(result.hasSufficientData)
        assertEquals(25, result.scoreFor(RadarDimension.TASKS))
        assertEquals(50, result.scoreFor(RadarDimension.HABITS))
        assertEquals(50, result.scoreFor(RadarDimension.WELLNESS))
        assertEquals(70, result.scoreFor(RadarDimension.PLANNING)) // neutral baseline
        // streak 3 of 7: 40 + (3/7 * 60) ≈ 40 + 25.7 ≈ 66%
        assertTrue("Discipline score between 60 and 70", result.scoreFor(RadarDimension.DISCIPLINE) in 60..70)
    }

    @Test
    fun `test weekly period evaluates past 7 days logs`() {
        val today = LocalDate.parse(todayIso)
        val pastDates = (0..6).map { today.minusDays(it.toLong()).format(DateTimeFormatter.ISO_DATE) }

        val habits = listOf(
            HabitItem(
                id = 1,
                title = "Morning Run",
                completedDates = setOf(pastDates[0], pastDates[1], pastDates[2], pastDates[3], pastDates[4]) // 5 days
            ),
            HabitItem(
                id = 2,
                title = "Reading",
                completedDates = setOf(pastDates[0], pastDates[2], pastDates[4]) // 3 days
            )
        )

        val result = ProductivityRadarEngine.calculate(
            tasks = emptyList(),
            habits = habits,
            waterCurrentMl = 1500,
            waterGoalMl = 2000,
            plans = emptyList(),
            currentStreak = 5,
            period = ProductivityPeriod.WEEKLY,
            todayIso = todayIso
        )

        // Total possible habit logs = 2 * 7 = 14. Actual completed = 5 + 3 = 8. Rate = 8/14 = 57%
        assertEquals(57, result.scoreFor(RadarDimension.HABITS))
        assertEquals(ProductivityPeriod.WEEKLY, result.period)
    }

    @Test
    fun `test monthly period evaluates past 30 days logs`() {
        val today = LocalDate.parse(todayIso)
        val pastDates = (0..29).map { today.minusDays(it.toLong()).format(DateTimeFormatter.ISO_DATE) }

        val habits = listOf(
            HabitItem(
                id = 1,
                title = "Hydration Habit",
                completedDates = pastDates.take(15).toSet() // 15 of 30 days = 50%
            )
        )

        val result = ProductivityRadarEngine.calculate(
            tasks = emptyList(),
            habits = habits,
            waterCurrentMl = 2000,
            waterGoalMl = 2000,
            plans = emptyList(),
            currentStreak = 15,
            period = ProductivityPeriod.MONTHLY,
            todayIso = todayIso
        )

        assertEquals(50, result.scoreFor(RadarDimension.HABITS))
        assertEquals(ProductivityPeriod.MONTHLY, result.period)
    }

    @Test
    fun `test personalized guidance tip targets lowest scoring pillar`() {
        // Wellness trailing
        val resultWellnessTrailing = ProductivityRadarEngine.calculate(
            tasks = listOf(TaskItem(id = 1, title = "Task", dueDateIso = todayIso, isOneTimeCompleted = true)),
            habits = listOf(HabitItem(id = 1, title = "Habit", isCompletedToday = true)),
            waterCurrentMl = 100, // Very low wellness
            waterGoalMl = 2500,
            plans = emptyList(),
            currentStreak = 7,
            todayIso = todayIso
        )
        assertTrue(
            "Guidance tip must mention hydration or wellness",
            resultWellnessTrailing.guidanceTip.contains("Hydration", ignoreCase = true)
        )

        // Focus trailing
        val resultFocusTrailing = ProductivityRadarEngine.calculate(
            tasks = listOf(
                TaskItem(id = 1, title = "T1", dueDateIso = todayIso, isOneTimeCompleted = false),
                TaskItem(id = 2, title = "T2", dueDateIso = todayIso, isOneTimeCompleted = false),
                TaskItem(id = 3, title = "T3", dueDateIso = todayIso, isOneTimeCompleted = false)
            ),
            habits = listOf(HabitItem(id = 1, title = "Habit", isCompletedToday = true)),
            waterCurrentMl = 2500,
            waterGoalMl = 2000,
            plans = emptyList(),
            currentStreak = 7,
            todayIso = todayIso
        )
        assertTrue(
            "Guidance tip must mention tasks or focus",
            resultFocusTrailing.guidanceTip.contains("tasks", ignoreCase = true) ||
            resultFocusTrailing.guidanceTip.contains("focus", ignoreCase = true)
        )
    }

    @Test
    fun `test zero division safety with zero water goal and empty lists`() {
        val result = ProductivityRadarEngine.calculate(
            tasks = emptyList(),
            habits = emptyList(),
            waterCurrentMl = 0,
            waterGoalMl = 0, // 0 water goal must default to safe fallback 2000ml without dividing by zero
            plans = emptyList(),
            currentStreak = 0,
            todayIso = todayIso
        )

        assertEquals(0, result.scoreFor(RadarDimension.WELLNESS))
        assertTrue("Overall score must remain bounded in 0..100", result.overallScorePercent in 0..100)
    }
}
