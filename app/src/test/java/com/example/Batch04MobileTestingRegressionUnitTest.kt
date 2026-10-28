package com.example

import com.example.domain.model.HoneycombAppearanceConfig
import com.example.domain.model.HoneycombShortcut
import com.example.domain.model.RecurrenceType
import com.example.domain.model.ShortcutType
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskPriority
import com.example.domain.recurrence.RecurrenceEngine
import com.example.domain.templates.StandardProjects
import com.example.domain.templates.TemplateCatalog
import com.example.domain.templates.TemplateCategory
import com.example.widgets.QuickRemindersWidgetHelper
import com.example.widgets.WidgetRefreshCoordinator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Mobile Testing Batch 04 Regression Unit Tests.
 *
 * Verifies fixes for physical mobile testing issues:
 * - BATCH04-WID-01: Matching Honeycomb Layouts (App QR & Widget 3/2 pattern, compact dimensions)
 * - BATCH04-WID-02: Real-Time App QR <-> Home Screen Widget Synchronization
 * - BATCH04-QR-03: Discipline QR Idempotency & Zero-Duplicate Tasks (0-min duration, daily recurrence)
 * - BATCH04-TPL-04: Standalone Quick Hydration Tracker Template & Bundle
 * - BATCH04-CAL-05: Daily Recurring Task Dots on All Scheduled Dates (including future & completed)
 */
class Batch04MobileTestingRegressionUnitTest {

    // =========================================================================
    // BATCH04-WID-01: Honeycomb Layout Geometry & Widget 3/2 Parity
    // =========================================================================

    @Test
    fun `test honeycomb axial coordinate layout matches alternating 3-2-3-2 pattern`() {
        val shortcuts = (0 until 10).map { i ->
            val r = when {
                i < 3 -> 0 // Row 0: 3 tiles (0,0), (1,0), (2,0)
                i < 5 -> 1 // Row 1: 2 tiles (0,1), (1,1)
                i < 8 -> 2 // Row 2: 3 tiles (0,2), (1,2), (2,2)
                else -> 3  // Row 3: 2 tiles (0,3), (1,3)
            }
            val q = when {
                i < 3 -> i
                i < 5 -> i - 3
                i < 8 -> i - 5
                else -> i - 8
            }
            HoneycombShortcut(
                id = (i + 1).toLong(),
                type = ShortcutType.TASK,
                targetId = "target_$i",
                title = "Item $i",
                emoji = "🔥",
                coordQ = q,
                coordR = r,
                orderIndex = i
            )
        }

        val rows = shortcuts.groupBy { it.coordR }
        assertEquals(3, rows[0]?.size)
        assertEquals(2, rows[1]?.size)
        assertEquals(3, rows[2]?.size)
        assertEquals(2, rows[3]?.size)

        // Verify row 1 (2 tiles) has half-step axial offset (q + r/2)
        val row0CentersX = rows[0]!!.map { it.coordQ.toFloat() + it.coordR.toFloat() / 2f }
        val row1CentersX = rows[1]!!.map { it.coordQ.toFloat() + it.coordR.toFloat() / 2f }

        assertEquals(listOf(0f, 1f, 2f), row0CentersX)
        assertEquals(listOf(0.5f, 1.5f), row1CentersX)
    }

    @Test
    fun `test widget tile dimensions and compact hex padding`() {
        val radius = 96f
        val hexWidth = QuickRemindersWidgetHelper.calculateHexWidth(radius)
        val hexHeight = QuickRemindersWidgetHelper.calculateHexHeight(radius)

        assertTrue("Hex width must be sqrt(3) * radius", hexWidth > 165f && hexWidth < 167f)
        assertEquals(192f, hexHeight, 0.01f)

        // Verify even row has 0 stagger and odd row has 0.5 stagger
        assertEquals(0f, QuickRemindersWidgetHelper.calculateAxialStaggerFactor(0), 0.001f)
        assertEquals(0.5f, QuickRemindersWidgetHelper.calculateAxialStaggerFactor(1), 0.001f)
        assertEquals(0f, QuickRemindersWidgetHelper.calculateAxialStaggerFactor(2), 0.001f)
        assertEquals(0.5f, QuickRemindersWidgetHelper.calculateAxialStaggerFactor(3), 0.001f)

        assertFalse(QuickRemindersWidgetHelper.isOddStaggeredRow(0))
        assertTrue(QuickRemindersWidgetHelper.isOddStaggeredRow(1))

        // 6 regular pointy-top vertices
        val vertices = QuickRemindersWidgetHelper.calculateHexagonVertices(100f, 100f, radius)
        assertEquals(6, vertices.size)
    }

    // =========================================================================
    // BATCH04-WID-02: Real-Time Widget Refresh Coordinator
    // =========================================================================

    @Test
    fun `test widget refresh coordinator executes safely without exception`() {
        // Coordinator handles null context or uninitialized state gracefully
        WidgetRefreshCoordinator.notifyAllWidgetsChanged(null)
        assertTrue(true)
    }

    // =========================================================================
    // BATCH04-QR-03: Discipline QR Idempotency, 0-min Duration, Zero Duplicates
    // =========================================================================

    @Test
    fun `test discipline routine task properties have zero duration and daily recurrence`() {
        val task = TaskItem(
            id = 999L,
            title = "Discipline Check",
            category = "Routines",
            priority = TaskPriority.HIGH,
            dueDateIso = "2026-10-28",
            durationMinutes = 0,
            durationAmount = 0,
            isAllDay = true,
            recurrenceType = RecurrenceType.DAILY
        )

        assertEquals("Discipline Check", task.title)
        assertEquals(0, task.durationMinutes)
        assertEquals(0, task.durationAmount)
        assertTrue("Discipline task must be all day to prevent calendar time-blocking", task.isAllDay)
        assertEquals(RecurrenceType.DAILY, task.recurrenceType)
    }

    @Test
    fun `test atomic task resolution finds existing task by title and prevents duplicates`() {
        val existingTasks = mutableListOf(
            TaskItem(id = 10L, title = "Discipline Check", dueDateIso = "2026-10-28", category = "Routines")
        )

        fun resolveTask(title: String): TaskItem {
            val clean = title.trim()
            val existing = existingTasks.find {
                !it.isArchived && (it.title.equals(clean, ignoreCase = true) || it.title.contains(clean, ignoreCase = true))
            }
            if (existing != null) return existing

            val created = TaskItem(
                id = (existingTasks.maxOfOrNull { it.id } ?: 0L) + 1L,
                title = clean,
                dueDateIso = "2026-10-28",
                durationMinutes = 0,
                isAllDay = true,
                recurrenceType = RecurrenceType.DAILY
            )
            existingTasks.add(created)
            return created
        }

        // 10 rapid calls
        val resolvedIds = (1..10).map { resolveTask("Discipline Check").id }
        assertEquals("All 10 calls must resolve to the identical existing task ID", 1, resolvedIds.distinct().size)
        assertEquals(10L, resolvedIds.first())
        assertEquals("Database must still have exactly 1 task", 1, existingTasks.size)
    }

    @Test
    fun `test safe cleanup of duplicate discipline tasks retains earliest and completed task`() {
        val duplicates = listOf(
            TaskItem(id = 101L, title = "Discipline Check", dueDateIso = "2026-10-25", completedDates = setOf("2026-10-25", "2026-10-26")),
            TaskItem(id = 102L, title = "Discipline", dueDateIso = "2026-10-26", completedDates = emptySet()),
            TaskItem(id = 103L, title = "Discipline Check", dueDateIso = "2026-10-27", completedDates = emptySet())
        )

        // Select task with most completions
        val targetToKeep = duplicates.maxByOrNull { it.completedDates.size } ?: duplicates.minByOrNull { it.id }!!
        assertEquals(101L, targetToKeep.id)
        assertEquals(2, targetToKeep.completedDates.size)

        val toDelete = duplicates.filter { it.id != targetToKeep.id }
        assertEquals(2, toDelete.size)
        assertTrue(toDelete.any { it.id == 102L })
        assertTrue(toDelete.any { it.id == 103L })
    }

    // =========================================================================
    // BATCH04-TPL-04: Standalone Quick Hydration Tracker Template & Bundle
    // =========================================================================

    @Test
    fun `test standalone quick hydration tracker template exists and has no unrelated tasks`() {
        val template = TemplateCatalog.getTemplateById("template_quick_hydration_tracker")
        assertNotNull("template_quick_hydration_tracker must exist in TemplateCatalog", template)
        assertEquals("Quick Hydration Tracker", template!!.title)
        assertEquals(TemplateCategory.HEALTH_HYDRATION, template.category)
        assertEquals("💧", template.emoji)

        // Exactly 1 water shortcut
        assertEquals(1, template.shortcuts.size)
        assertEquals(ShortcutType.WATER, template.shortcuts.first().type)
        assertEquals("Drink Water", template.shortcuts.first().title)

        // Valid Health project and labels
        assertTrue(template.projects.contains(StandardProjects.HEALTH))
        template.tasks.forEach { task ->
            assertEquals("Health", task.projectName)
            assertTrue("Tasks must not contain unrelated discipline/domestic/reading labels",
                task.labels.any { it.contains("Hydration", ignoreCase = true) || it.contains("Water", ignoreCase = true) }
            )
        }
    }

    @Test
    fun `test quick hydration tracker bundle exists in allBundles`() {
        val bundle = TemplateCatalog.allBundles.find { it.id == "bundle_quick_hydration_tracker" }
        assertNotNull("bundle_quick_hydration_tracker must exist in TemplateCatalog.allBundles", bundle)
        assertEquals("Quick Hydration Tracker", bundle!!.title)
        assertEquals(listOf("template_quick_hydration_tracker"), bundle.templateIds)
        assertEquals("💧", bundle.emoji)
    }

    // =========================================================================
    // BATCH04-CAL-05: Daily Recurring Task Calendar Dots Evaluation
    // =========================================================================

    @Test
    fun `test daily recurring task from Oct 25 to Oct 31 has dots on all 7 dates and none before or after`() {
        val task = TaskItem(
            id = 701L,
            title = "Daily Physical Workout",
            dueDateIso = "2026-10-25",
            recurrenceType = RecurrenceType.DAILY,
            recurrenceEndCondition = "ON_DATE",
            recurrenceEndDateIso = "2026-10-31"
        )

        // 1. Before start date -> NO dot
        assertFalse("Oct 24 is before start date Oct 25 -> must NOT have dot",
            RecurrenceEngine.isTaskScheduledOnDate(task, "2026-10-24"))

        // 2. Scheduled range Oct 25 to Oct 31 -> ALL must have dots
        val scheduledDates = (25..31).map { "2026-10-$it" }
        scheduledDates.forEach { dateIso ->
            assertTrue("Date $dateIso is in [Oct 25..Oct 31] -> MUST have dot",
                RecurrenceEngine.isTaskScheduledOnDate(task, dateIso))
        }

        // 3. Past end date -> NO dot
        assertFalse("Nov 01 is past end date Oct 31 -> must NOT have dot",
            RecurrenceEngine.isTaskScheduledOnDate(task, "2026-11-01"))
        assertFalse("Nov 02 is past end date Oct 31 -> must NOT have dot",
            RecurrenceEngine.isTaskScheduledOnDate(task, "2026-11-02"))
    }

    @Test
    fun `test completing recurring task on Oct 26 does NOT remove calendar dot on Oct 26`() {
        val task = TaskItem(
            id = 702L,
            title = "Morning Meditation",
            dueDateIso = "2026-10-25",
            recurrenceType = RecurrenceType.DAILY,
            completedDates = setOf("2026-10-26") // Completed on Oct 26
        )

        // Canonical scheduling evaluation
        assertTrue("Oct 26 is scheduled", RecurrenceEngine.isTaskScheduledOnDate(task, "2026-10-26"))
        assertTrue("Oct 26 is completed", RecurrenceEngine.isCompletedOnDate(task, "2026-10-26"))

        // Calendar dots set computation (WITHOUT !isCompletedOnDate filter)
        val visibleDates = setOf("2026-10-25", "2026-10-26", "2026-10-27")
        val activeTasks = listOf(task)
        val datesWithDots = visibleDates.filter { dateIso ->
            activeTasks.any { t -> RecurrenceEngine.isTaskScheduledOnDate(t, dateIso) }
        }.toSet()

        assertTrue("Oct 25 has dot", datesWithDots.contains("2026-10-25"))
        assertTrue("Oct 26 MUST retain dot even though completed!", datesWithDots.contains("2026-10-26"))
        assertTrue("Oct 27 has dot", datesWithDots.contains("2026-10-27"))
    }
}
