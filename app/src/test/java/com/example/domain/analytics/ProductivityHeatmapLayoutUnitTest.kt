package com.example.domain.analytics

import com.example.domain.model.HabitItem
import com.example.domain.model.TaskItem
import com.example.ui.components.HeatmapCalendarUtils
import com.example.ui.components.HeatmapMetric
import com.example.ui.components.HeatmapPalette
import com.example.ui.screens.dashboard.DashboardSection
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ProductivityHeatmapLayoutUnitTest {

    @Test
    fun `heatmap month labels use short names and respect month boundaries`() {
        val start = LocalDate.of(2025, 12, 29)
        val end = LocalDate.of(2026, 2, 12)

        val labels = HeatmapCalendarUtils.buildMonthHeaders(start, end)

        assertEquals(listOf("Dec", "Jan", "Feb"), labels.map { it.label })
        assertEquals(listOf(0, 1, 5), labels.map { it.weekIndex })
    }

    @Test
    fun `heatmap cells include each date number and apply the selected metric`() {
        val start = LocalDate.of(2026, 1, 1)
        val end = LocalDate.of(2026, 1, 7)

        val tasks = listOf(
            TaskItem(id = 1L, title = "Task 1", dueDateIso = "2026-01-02", isOneTimeCompleted = true),
            TaskItem(id = 2L, title = "Task 2", dueDateIso = "2026-01-05", isOneTimeCompleted = true)
        )
        val habits = listOf(
            HabitItem(id = 1L, title = "Habit A", completedDates = setOf("2026-01-02", "2026-01-06")),
            HabitItem(id = 2L, title = "Habit B", completedDates = setOf("2026-01-05"))
        )

        val taskCells = HeatmapCalendarUtils.buildDayCells(start, end, tasks, habits, HeatmapMetric.TASKS)
        val habitCells = HeatmapCalendarUtils.buildDayCells(start, end, tasks, habits, HeatmapMetric.HABITS)

        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7), taskCells.map { it.dayOfMonth })
        assertEquals(listOf(0, 1, 0, 0, 1, 0, 0), taskCells.map { it.count })
        assertEquals(listOf(0, 1, 0, 0, 1, 1, 0), habitCells.map { it.count })
        assertEquals(1, taskCells.first { it.date == LocalDate.of(2026, 1, 2) }.count)
    }

    @Test
    fun `heatmap productivity levels remain visually distinct`() {
        val colors = (0..4).map { HeatmapCalendarUtils.resolveCellColor(it, HeatmapPalette.EMERALD) }

        assertEquals(5, colors.distinct().size)
    }

    @Test
    fun `radar metric labels and dimension ordering match the underlying calculations`() {
        val result = ProductivityRadarEngine.calculate(
            tasks = listOf(TaskItem(id = 1L, title = "T1", dueDateIso = "2026-11-01", isOneTimeCompleted = true)),
            habits = listOf(HabitItem(id = 1L, title = "H1", isCompletedToday = true)),
            waterCurrentMl = 1500,
            waterGoalMl = 2000,
            plans = listOf(
                com.example.domain.model.PlanItem(
                    id = 1L,
                    title = "Plan",
                    startDateIso = "2026-11-01",
                    targetDateIso = "2026-11-01",
                    milestones = listOf(com.example.domain.model.PlanMilestone(id = 1L, planId = 1L, title = "M1", isCompleted = true))
                )
            ),
            currentStreak = 7,
            period = ProductivityPeriod.DAILY,
            todayIso = "2026-11-01"
        )

        assertEquals(
            listOf("Tasks", "Habits", "Hydration", "Planning", "Discipline"),
            RadarDimension.entries.map { it.displayName }
        )
        assertEquals(RadarDimension.entries, result.dimensions.map { it.dimension })
        assertEquals(RadarDimension.TASKS, result.dimensions[0].dimension)
        assertEquals(RadarDimension.HABITS, result.dimensions[1].dimension)
        assertEquals(RadarDimension.WELLNESS, result.dimensions[2].dimension)
        assertEquals(RadarDimension.PLANNING, result.dimensions[3].dimension)
        assertEquals(RadarDimension.DISCIPLINE, result.dimensions[4].dimension)
        assertEquals(100, result.scoreFor(RadarDimension.TASKS))
        assertEquals(100, result.scoreFor(RadarDimension.HABITS))
        assertEquals(75, result.scoreFor(RadarDimension.WELLNESS))
        assertEquals(100, result.scoreFor(RadarDimension.PLANNING))
        assertEquals(100, result.scoreFor(RadarDimension.DISCIPLINE))
    }

    @Test
    fun `dashboard section ordering matches the required home dashboard sequence`() {
        assertEquals(
            listOf(
                DashboardSection.HEADER,
                DashboardSection.NOW_IN_PROGRESS,
                DashboardSection.QR,
                DashboardSection.DAILY_HYDRATION,
                DashboardSection.DAILY_HABIT,
                DashboardSection.CHALLENGE,
                DashboardSection.DAILY_TASK,
                DashboardSection.SCHEDULE,
                DashboardSection.PRODUCTIVITY_ANALYSIS,
                DashboardSection.MULTI_HORIZON_PLANNING,
                DashboardSection.HUNTER_RANK
            ),
            DashboardSection.entries
        )
    }
}
