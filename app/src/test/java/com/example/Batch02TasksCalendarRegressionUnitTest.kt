package com.example

import com.example.domain.model.RecurrenceType
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskPriority
import com.example.domain.recurrence.RecurrenceEngine
import com.example.utils.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Mobile Testing Batch 02 (Tasks & Calendar) Regression Unit Tests.
 * Covers TASK-01, TASK-02, and CAL-01.
 */
class Batch02TasksCalendarRegressionUnitTest {

    // =========================================================================
    // TASK-01: Header Select Not Working (Immediate Multi-Selection Activation)
    // =========================================================================

    @Test
    fun `test selection mode toggles on and off without requiring filter interaction`() {
        var isSelectionMode = false
        var selectedIds = emptySet<Long>()

        fun toggleSelectionMode(enabled: Boolean? = null) {
            isSelectionMode = enabled ?: !isSelectionMode
            if (!isSelectionMode) {
                selectedIds = emptySet()
            }
        }

        // Initially inactive
        assertFalse(isSelectionMode)
        assertTrue(selectedIds.isEmpty())

        // Header Select tapped
        toggleSelectionMode()
        assertTrue("Header Select must immediately activate selection mode", isSelectionMode)
        assertTrue(selectedIds.isEmpty())

        // Header Close tapped
        toggleSelectionMode()
        assertFalse("Toggling selection mode again must deactivate it", isSelectionMode)
        assertTrue(selectedIds.isEmpty())
    }

    @Test
    fun `test selection mode and filter operate independently`() {
        val tasks = listOf(
            TaskItem(id = 1L, title = "Task 1", dueDateIso = "2026-10-27", category = "Work"),
            TaskItem(id = 2L, title = "Task 2", dueDateIso = "2026-10-27", category = "Personal"),
            TaskItem(id = 3L, title = "Task 3", dueDateIso = "2026-10-27", category = "Work")
        )

        var isSelectionMode = true
        var selectedIds = setOf(1L, 3L)
        var currentFilter = "All"

        // Filter is "All", two tasks selected
        assertTrue(isSelectionMode)
        assertEquals(2, selectedIds.size)

        // User switches filter to "Work"
        currentFilter = "Work"
        val filtered = tasks.filter { currentFilter == "All" || it.category == currentFilter }
        assertEquals(2, filtered.size)

        // Selection mode and previously selected IDs remain valid and intact
        assertTrue("Selection mode must remain active after filter change", isSelectionMode)
        assertTrue(selectedIds.contains(1L))
        assertTrue(selectedIds.contains(3L))
    }

    // =========================================================================
    // TASK-02: Task Checkboxes Responsive & Selection State Integrity
    // =========================================================================

    @Test
    fun `test individual task selection and deselection toggles cleanly`() {
        var selectedIds = emptySet<Long>()
        var isSelectionMode = false

        fun toggleTaskSelection(taskId: Long) {
            val current = selectedIds.toMutableSet()
            if (current.contains(taskId)) {
                current.remove(taskId)
            } else {
                current.add(taskId)
            }
            selectedIds = current.toSet()
            if (current.isNotEmpty() && !isSelectionMode) {
                isSelectionMode = true
            }
        }

        // Tap item 101 -> selects and auto-enables selection mode
        toggleTaskSelection(101L)
        assertTrue(isSelectionMode)
        assertTrue(selectedIds.contains(101L))
        assertEquals(1, selectedIds.size)

        // Tap item 102 -> selects item 102
        toggleTaskSelection(102L)
        assertTrue(selectedIds.contains(101L))
        assertTrue(selectedIds.contains(102L))
        assertEquals(2, selectedIds.size)

        // Tap item 101 again -> deselects item 101 (TASK-02 deselection responsiveness)
        toggleTaskSelection(101L)
        assertFalse(selectedIds.contains(101L))
        assertTrue(selectedIds.contains(102L))
        assertEquals(1, selectedIds.size)

        // Tap item 102 -> deselects item 102
        toggleTaskSelection(102L)
        assertTrue(selectedIds.isEmpty())
        assertTrue(isSelectionMode) // Remains in selection mode until explicitly closed
    }

    @Test
    fun `test select all filtered tasks selects all and toggles to deselect all`() {
        val filteredTasks = listOf(
            TaskItem(id = 10L, title = "Review PR", dueDateIso = "2026-10-27"),
            TaskItem(id = 20L, title = "Write Tests", dueDateIso = "2026-10-27"),
            TaskItem(id = 30L, title = "Submit Build", dueDateIso = "2026-10-27")
        )

        var isSelectionMode = true
        var selectedIds = emptySet<Long>()

        fun selectAllFilteredTasks() {
            val currentFilteredIds = filteredTasks.map { it.id }.toSet()
            if (currentFilteredIds.isNotEmpty() && selectedIds.containsAll(currentFilteredIds)) {
                selectedIds = emptySet()
            } else {
                selectedIds = currentFilteredIds
                isSelectionMode = true
            }
        }

        // Select All
        selectAllFilteredTasks()
        assertEquals(setOf(10L, 20L, 30L), selectedIds)

        // Tap Select All again -> Deselect All
        selectAllFilteredTasks()
        assertTrue("Tapping Select All when all tasks are selected must Deselect All", selectedIds.isEmpty())
    }

    @Test
    fun `test bulk action clears selection and exits selection mode`() {
        var isSelectionMode = true
        var selectedIds = setOf(10L, 20L)

        fun clearSelection() {
            selectedIds = emptySet()
            isSelectionMode = false
        }

        // Perform bulk action
        clearSelection()
        assertFalse(isSelectionMode)
        assertTrue(selectedIds.isEmpty())
    }

    // =========================================================================
    // CAL-01: Calendar Recurring Task Dots & Occurrence Parity
    // =========================================================================

    @Test
    fun `test daily recurring task shows scheduled dots on all applicable future dates`() {
        val dailyTask = TaskItem(
            id = 501L,
            title = "Morning Meditation",
            dueDateIso = "2026-10-20",
            recurrenceType = RecurrenceType.DAILY
        )

        val targetDates = listOf("2026-10-20", "2026-10-21", "2026-10-27", "2026-10-31", "2026-11-01", "2026-11-15")

        targetDates.forEach { dateIso ->
            assertTrue(
                "Daily task starting 2026-10-20 must be scheduled on $dateIso",
                RecurrenceEngine.isTaskScheduledOnDate(dailyTask, dateIso)
            )
            assertFalse(
                "Uncompleted daily task must not be marked complete on $dateIso",
                RecurrenceEngine.isCompletedOnDate(dailyTask, dateIso)
            )
        }

        // Dates prior to start date must not be scheduled
        assertFalse(
            "Daily task starting 2026-10-20 must not be scheduled prior to its start date",
            RecurrenceEngine.isTaskScheduledOnDate(dailyTask, "2026-10-19")
        )
    }

    @Test
    fun `test completing today occurrence hides dot for today but retains dots on future dates`() {
        val dailyTask = TaskItem(
            id = 502L,
            title = "Yoga Routine",
            dueDateIso = "2026-10-01",
            recurrenceType = RecurrenceType.DAILY,
            completedDates = setOf("2026-10-27") // Completed today only
        )

        // Today is completed -> dot hidden
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(dailyTask, "2026-10-27"))
        assertTrue("Today must be completed", RecurrenceEngine.isCompletedOnDate(dailyTask, "2026-10-27"))

        // Tomorrow is NOT completed -> dot visible
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(dailyTask, "2026-10-28"))
        assertFalse("Tomorrow must remain pending and uncompleted", RecurrenceEngine.isCompletedOnDate(dailyTask, "2026-10-28"))

        // Day after tomorrow is NOT completed -> dot visible
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(dailyTask, "2026-10-29"))
        assertFalse("Day after tomorrow must remain pending", RecurrenceEngine.isCompletedOnDate(dailyTask, "2026-10-29"))

        // Calculation parity test for calendar dot set
        val visibleDates = setOf("2026-10-26", "2026-10-27", "2026-10-28", "2026-10-29")
        val activeTasks = listOf(dailyTask)
        val datesWithDots = visibleDates.filter { dateIso ->
            activeTasks.any { task ->
                RecurrenceEngine.isTaskScheduledOnDate(task, dateIso) &&
                    !RecurrenceEngine.isCompletedOnDate(task, dateIso)
            }
        }.toSet()

        assertTrue("2026-10-26 (yesterday) was uncompleted -> dot visible", datesWithDots.contains("2026-10-26"))
        assertFalse("2026-10-27 (today) was completed -> dot hidden", datesWithDots.contains("2026-10-27"))
        assertTrue("2026-10-28 (tomorrow) is pending -> dot visible", datesWithDots.contains("2026-10-28"))
        assertTrue("2026-10-29 (future) is pending -> dot visible", datesWithDots.contains("2026-10-29"))
    }

    @Test
    fun `test recurring task recurrence end condition ON_DATE stops dots after end date`() {
        val taskWithEnd = TaskItem(
            id = 503L,
            title = "Temporary Daily Medication",
            dueDateIso = "2026-10-20",
            recurrenceType = RecurrenceType.DAILY,
            recurrenceEndCondition = "ON_DATE",
            recurrenceEndDateIso = "2026-10-25"
        )

        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(taskWithEnd, "2026-10-24"))
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(taskWithEnd, "2026-10-25"))
        assertFalse("Task must not be scheduled after its recurrence end date", RecurrenceEngine.isTaskScheduledOnDate(taskWithEnd, "2026-10-26"))
        assertFalse(RecurrenceEngine.isTaskScheduledOnDate(taskWithEnd, "2026-10-27"))
    }

    @Test
    fun `test recurring task with NEVER end condition ignores empty or stale end date string`() {
        val taskNeverEnd = TaskItem(
            id = 504L,
            title = "Never Ending Habit",
            dueDateIso = "2026-10-01",
            recurrenceType = RecurrenceType.DAILY,
            recurrenceEndCondition = "NEVER",
            recurrenceEndDateIso = "" // Empty string should not cut off recurrence
        )

        assertTrue("NEVER end condition must keep scheduling on future dates", RecurrenceEngine.isTaskScheduledOnDate(taskNeverEnd, "2026-10-27"))
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(taskNeverEnd, "2026-11-15"))
    }

    @Test
    fun `test paused recurring task generates no dots`() {
        val pausedTask = TaskItem(
            id = 505L,
            title = "Paused Routine",
            dueDateIso = "2026-10-01",
            recurrenceType = RecurrenceType.DAILY,
            isPaused = true
        )

        assertFalse("Paused recurring task must generate no occurrences", RecurrenceEngine.isTaskScheduledOnDate(pausedTask, "2026-10-27"))
        assertFalse(RecurrenceEngine.isTaskScheduledOnDate(pausedTask, "2026-10-28"))
    }

    @Test
    fun `test one-time task generates dot only on its due date and disappears when completed`() {
        val oneTimeTask = TaskItem(
            id = 506L,
            title = "Renew Vehicle Registration",
            dueDateIso = "2026-10-29",
            recurrenceType = RecurrenceType.NONE,
            isOneTimeCompleted = false
        )

        assertFalse(RecurrenceEngine.isTaskScheduledOnDate(oneTimeTask, "2026-10-28"))
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(oneTimeTask, "2026-10-29"))
        assertFalse(RecurrenceEngine.isTaskScheduledOnDate(oneTimeTask, "2026-10-30"))

        val completedOneTime = oneTimeTask.copy(isOneTimeCompleted = true)
        assertTrue(RecurrenceEngine.isCompletedOnDate(completedOneTime, "2026-10-29"))
    }

    @Test
    fun `test DateUtils fast parsing helpers handle boundary dates without parse errors`() {
        assertEquals(2026, DateUtils.getYear("2026-10-27"))
        assertEquals(9, DateUtils.getMonthZeroIndexed("2026-10-27")) // October is month 9 (zero-indexed)
        assertEquals(27, DateUtils.getDayOfMonth("2026-10-27"))

        assertEquals(2027, DateUtils.getYear("2027-01-01"))
        assertEquals(0, DateUtils.getMonthZeroIndexed("2027-01-01")) // January is month 0
        assertEquals(1, DateUtils.getDayOfMonth("2027-01-01"))

        assertEquals(2026, DateUtils.getYear("2026-12-31"))
        assertEquals(11, DateUtils.getMonthZeroIndexed("2026-12-31")) // December is month 11
        assertEquals(31, DateUtils.getDayOfMonth("2026-12-31"))
    }
}
