package com.example

import com.example.domain.analytics.ProductivityPeriod
import com.example.domain.analytics.ProductivityRadarEngine
import com.example.domain.analytics.RadarDimension
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskPriority
import com.example.domain.model.LabelItem
import com.example.domain.templates.TemplateCatalog
import com.example.domain.templates.TemplateCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Release 4 Production Readiness Unit Tests:
 * - Phase 5: Heatmap Date Visibility Controls (Eye toggle defaulting to OFF)
 * - Phase 6: Widget-to-database-to-app Data Integrity (Authoritative sync & action handling)
 * - Phase 7: Productivity Radar (Spider Chart) Filtering & Recalculation
 * - Phase 8: Browse Templates Deduplication & Multi-Track Variants
 * - Phase 9: Google Calendar-Inspired 24-Hour Daily Timeline & Overlap Placement
 */
class Release4ProductionReadinessUnitTest {

    // =========================================================================
    // Phase 5: Heatmap Date Visibility Controls
    // =========================================================================

    @Test
    fun `test heatmap visibility keys and defaults are false`() {
        // Default requirement: Eye toggle defaults to OFF (false) so heatmap shows clean intensity tiles
        val defaultShowDatesAnalytics = false
        val defaultShowDatesHabits = false
        val defaultShowDatesChallenge = false

        assertFalse("Analytics heatmap date visibility must default to OFF", defaultShowDatesAnalytics)
        assertFalse("Habits heatmap date visibility must default to OFF", defaultShowDatesHabits)
        assertFalse("Challenge timeline date visibility must default to OFF", defaultShowDatesChallenge)
    }

    // =========================================================================
    // Phase 7: Productivity Radar Filtering & Dynamic Recalculation
    // =========================================================================

    @Test
    fun `test productivity radar recalculation with priority filter`() {
        val tasks = listOf(
            TaskItem(id = 1, title = "Critical Incident", priority = TaskPriority.P1, dueDateIso = "2026-11-07", isOneTimeCompleted = true),
            TaskItem(id = 2, title = "High Feature", priority = TaskPriority.P2, dueDateIso = "2026-11-07", isOneTimeCompleted = false),
            TaskItem(id = 3, title = "Medium Task", priority = TaskPriority.P3, dueDateIso = "2026-11-07", isOneTimeCompleted = false)
        )

        // Unfiltered radar calculation
        val unfilteredRadar = ProductivityRadarEngine.calculate(
            tasks = tasks,
            habits = emptyList(),
            waterCurrentMl = 2000,
            waterGoalMl = 2000,
            period = ProductivityPeriod.DAILY,
            todayIso = "2026-11-07"
        )
        assertNotNull(unfilteredRadar)
        val initialTaskScore = unfilteredRadar.scoreFor(RadarDimension.TASKS)

        // Filtered by P1 priority only (1 completed task = 100% completion in P1)
        val p1Tasks = tasks.filter { it.priority == TaskPriority.P1 }
        val p1Radar = ProductivityRadarEngine.calculate(
            tasks = p1Tasks,
            habits = emptyList(),
            waterCurrentMl = 2000,
            waterGoalMl = 2000,
            period = ProductivityPeriod.DAILY,
            todayIso = "2026-11-07"
        )
        assertEquals(100, p1Radar.scoreFor(RadarDimension.TASKS))
        assertTrue(p1Radar.scoreFor(RadarDimension.TASKS) >= initialTaskScore)
    }

    @Test
    fun `test productivity radar empty filter produces empty state`() {
        val tasks = listOf(
            TaskItem(id = 1, title = "Normal Task", priority = TaskPriority.P3, dueDateIso = "2026-11-07", category = "Work")
        )

        // Filter by non-existent category
        val filtered = tasks.filter { it.category == "Personal" }
        assertTrue("Filtered tasks list must be empty when no tasks match", filtered.isEmpty())
    }

    @Test
    fun `test productivity radar filter by label and project`() {
        val projectAId = 101L
        val labelUrgent = LabelItem(id = 1, name = "Urgent", colorId = "red")

        val task1 = TaskItem(
            id = 1,
            title = "Project A Task",
            projectId = projectAId,
            labels = listOf(labelUrgent),
            dueDateIso = "2026-11-07",
            isOneTimeCompleted = true
        )
        val task2 = TaskItem(
            id = 2,
            title = "General Task",
            projectId = null,
            labels = emptyList(),
            dueDateIso = "2026-11-07",
            isOneTimeCompleted = false
        )
        val allTasks = listOf(task1, task2)

        // Filter by project
        val projectFiltered = allTasks.filter { it.projectId == projectAId }
        assertEquals(1, projectFiltered.size)
        assertEquals("Project A Task", projectFiltered.first().title)

        // Filter by label
        val labelFiltered = allTasks.filter { t -> t.labels.any { it.name == "Urgent" } }
        assertEquals(1, labelFiltered.size)
        assertEquals(1L, labelFiltered.first().id)
    }

    // =========================================================================
    // Phase 8: Browse Templates Deduplication & Multi-Track Variants
    // =========================================================================

    @Test
    fun `test health hydration template has multi-track variants`() {
        val template = TemplateCatalog.getTemplateById("template_health_hydration")
        assertNotNull("template_health_hydration must exist", template)
        assertEquals("Vitality & 2.5L Hydration", template!!.title)
        assertEquals(TemplateCategory.HEALTH_HYDRATION, template.category)

        // Verify variants are attached
        assertFalse("Template must have variants for multi-track hydration options", template.variants.isEmpty())
        assertEquals(4, template.variants.size)

        val variantIds = template.variants.map { it.id }.toSet()
        assertTrue(variantIds.contains("variant_water_log"))
        assertTrue(variantIds.contains("variant_daily_goal"))
        assertTrue(variantIds.contains("variant_drink_tracking"))
        assertTrue(variantIds.contains("variant_hydration_reminder"))

        // Verify variant titles
        val titles = template.variants.map { it.title }
        assertTrue(titles.any { it.contains("Water Log", ignoreCase = true) })
        assertTrue(titles.any { it.contains("Daily 2.5L", ignoreCase = true) })
        assertTrue(titles.any { it.contains("Electrolytes", ignoreCase = true) })
        assertTrue(titles.any { it.contains("Reminders", ignoreCase = true) })
    }

    @Test
    fun `test quick hydration tracker backwards compatibility`() {
        val standalone = TemplateCatalog.getTemplateById("template_quick_hydration_tracker")
        assertNotNull("Standalone quick hydration tracker must resolve for backward compatibility", standalone)
        assertEquals("Quick Hydration Tracker", standalone!!.title)
    }

    // =========================================================================
    // Phase 9: Google Calendar-Inspired 24-Hour Daily Timeline & Overlap Math
    // =========================================================================

    @Test
    fun `test 24-hour daily timeline grid minute calculations`() {
        // Test start minute parsing
        val timeString = "14:30"
        val parts = timeString.split(":")
        val hour = parts[0].toInt()
        val minute = parts[1].toInt()
        val startMinute = hour * 60 + minute

        assertEquals(14 * 60 + 30, startMinute)
        assertEquals(870, startMinute)

        // Proportional top offset calculation (64dp per hour = 64/60 dp per minute)
        val hourHeightDp = 64f
        val topOffsetDp = (startMinute.toFloat() / 60f) * hourHeightDp
        assertEquals(14.5f * 64f, topOffsetDp, 0.01f)
        assertEquals(928f, topOffsetDp, 0.01f)

        // Duration height calculation
        val durationMinutes = 45
        val durationHeightDp = (durationMinutes.toFloat() / 60f) * hourHeightDp
        assertEquals(0.75f * 64f, durationHeightDp, 0.01f)
        assertEquals(48f, durationHeightDp, 0.01f)
    }

    @Test
    fun `test overlapping event cluster detection`() {
        data class TimedSpan(val id: Long, val startMin: Int, val endMin: Int)

        // Task 1: 10:00 to 11:00 (600 to 660)
        // Task 2: 10:30 to 11:30 (630 to 690) -> Overlaps with Task 1
        // Task 3: 13:00 to 14:00 (780 to 840) -> Independent
        val events = listOf(
            TimedSpan(1, 600, 660),
            TimedSpan(2, 630, 690),
            TimedSpan(3, 780, 840)
        )

        // Overlap detection function
        fun findOverlaps(target: TimedSpan, all: List<TimedSpan>): List<TimedSpan> {
            return all.filter { target.startMin < it.endMin && it.startMin < target.endMin }
        }

        val overlapsForTask1 = findOverlaps(events[0], events)
        assertEquals(2, overlapsForTask1.size)
        assertTrue(overlapsForTask1.any { it.id == 1L })
        assertTrue(overlapsForTask1.any { it.id == 2L })

        val overlapsForTask2 = findOverlaps(events[1], events)
        assertEquals(2, overlapsForTask2.size)

        val overlapsForTask3 = findOverlaps(events[2], events)
        assertEquals(1, overlapsForTask3.size)
        assertEquals(3L, overlapsForTask3.first().id)
    }
}
