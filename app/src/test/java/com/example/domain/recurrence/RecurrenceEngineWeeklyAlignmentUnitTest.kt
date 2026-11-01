package com.example.domain.recurrence

import com.example.domain.model.RecurrenceType
import com.example.domain.model.TaskItem
import com.example.utils.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class RecurrenceEngineWeeklyAlignmentUnitTest {

    /**
     * Independent weekly oracle implemented strictly from specifications using java.time.
     * Computes calendar-week alignment without touching RecurrenceEngine.
     */
    private object IndependentWeeklyOracle {
        fun matches(
            dueIso: String,
            targetIso: String,
            interval: Int,
            selectedWeekdays: List<String>,
            firstDayOfWeek: String = "MONDAY"
        ): Boolean {
            val dueDate = LocalDate.parse(dueIso)
            val targetDate = LocalDate.parse(targetIso)
            if (targetDate.isBefore(dueDate)) return false

            val dow = targetDate.dayOfWeek.name
            val effectiveSelected = if (selectedWeekdays.isNotEmpty()) {
                selectedWeekdays.map { it.uppercase() }.toSet()
            } else {
                setOf(dueDate.dayOfWeek.name)
            }
            if (!effectiveSelected.contains(dow)) return false

            val dueStart = getStartOfWeek(dueDate, firstDayOfWeek)
            val targetStart = getStartOfWeek(targetDate, firstDayOfWeek)
            val weeksBetween = ChronoUnit.WEEKS.between(dueStart, targetStart)
            if (weeksBetween < 0) return false

            val effectiveInterval = if (interval > 0) interval else 1
            return weeksBetween % effectiveInterval == 0L
        }

        private fun getStartOfWeek(date: LocalDate, firstDayOfWeek: String): LocalDate {
            val dow = date.dayOfWeek
            val daysBefore = when (firstDayOfWeek.uppercase()) {
                "SUNDAY" -> dow.value % 7
                "SATURDAY" -> (dow.value + 1) % 7
                else -> dow.value - 1
            }
            return date.minusDays(daysBefore.toLong())
        }
    }

    /**
     * Independent reference occurrence counter using straightforward daily stepping.
     */
    private object ReferenceOccurrenceCounter {
        fun countUpTo(task: TaskItem, targetDateIso: String, firstDayOfWeek: String = "MONDAY"): Int {
            var count = 0
            val startIso = if (task.dueDateIso.isNotBlank()) task.dueDateIso else DateUtils.todayString()
            if (targetDateIso < startIso) return 0
            val days = DateUtils.daysBetween(startIso, targetDateIso)
            for (i in 0..days) {
                val d = DateUtils.addDays(startIso, i)
                if (RecurrenceEngine.isTaskScheduledOnDate(task, d, firstDayOfWeek)) {
                    count++
                    if (d == targetDateIso) return count
                }
            }
            return count
        }
    }

    // =========================================================================
    // 1. Bug Reproduction Test (Prompt C1 Specification)
    // =========================================================================

    @Test
    fun testReproduceWeeklyRecurrenceBug_WednesdayJan1EveryTwoWeeks() {
        val task = TaskItem(
            id = 101,
            title = "Bi-weekly Workouts",
            dueDateIso = "2025-01-01", // Wednesday
            recurrenceType = RecurrenceType.EVERY_N_WEEKS,
            recurrenceInterval = 2,
            recurrenceDaysOfWeek = listOf("MONDAY", "FRIDAY")
        )

        // Week 0 (Dec 30 2024 - Jan 5 2025):
        assertFalse("Wednesday Jan 1 is not in selected weekdays", RecurrenceEngine.isTaskScheduledOnDate(task, "2025-01-01"))
        assertTrue("Friday Jan 3 is in week 0 and selected weekdays -> must match", RecurrenceEngine.isTaskScheduledOnDate(task, "2025-01-03"))

        // Week 1 (Jan 6 - Jan 12): should NOT match because interval = 2
        assertFalse(
            "Monday Jan 6 is in week 1 and must NOT match bi-weekly recurrence",
            RecurrenceEngine.isTaskScheduledOnDate(task, "2025-01-06")
        )
        assertFalse(
            "Friday Jan 10 is in week 1 and must NOT match bi-weekly recurrence",
            RecurrenceEngine.isTaskScheduledOnDate(task, "2025-01-10")
        )

        // Week 2 (Jan 13 - Jan 19): matches
        assertTrue(
            "Monday Jan 13 is in week 2 and MUST match bi-weekly recurrence",
            RecurrenceEngine.isTaskScheduledOnDate(task, "2025-01-13")
        )
        assertTrue(
            "Friday Jan 17 is in week 2 and MUST match bi-weekly recurrence",
            RecurrenceEngine.isTaskScheduledOnDate(task, "2025-01-17")
        )

        // Verify with CUSTOM WEEKS as well
        val customTask = task.copy(recurrenceType = RecurrenceType.CUSTOM, recurrenceUnit = "WEEKS")
        assertFalse(RecurrenceEngine.isTaskScheduledOnDate(customTask, "2025-01-06"))
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(customTask, "2025-01-13"))
    }

    // =========================================================================
    // 2. Table-Driven Tests Across Intervals, Weekdays, Boundaries & Oracles
    // =========================================================================

    @Test
    fun testTableDrivenWeeklyRecurrenceAcrossAllDueWeekdaysAndIntervals() {
        val intervals = listOf(1, 2, 3, 4)
        val selectedDayCombos = listOf(
            emptyList(), // Empty -> defaults to due day
            listOf("MONDAY"),
            listOf("WEDNESDAY", "FRIDAY"),
            listOf("SATURDAY", "SUNDAY"),
            listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY")
        )

        // 7 consecutive days starting 2025-01-06 (Monday) through 2025-01-12 (Sunday)
        val dueDates = (0..6).map { DateUtils.addDays("2025-01-06", it) }

        for (interval in intervals) {
            for (selectedDays in selectedDayCombos) {
                for (dueIso in dueDates) {
                    val task = TaskItem(
                        id = 200,
                        title = "Test Weekly",
                        dueDateIso = dueIso,
                        recurrenceType = RecurrenceType.EVERY_N_WEEKS,
                        recurrenceInterval = interval,
                        recurrenceDaysOfWeek = selectedDays
                    )

                    // Test targets: 14 days before due to 56 days after due (8 weeks)
                    for (offset in -14..56) {
                        val targetIso = DateUtils.addDays(dueIso, offset)
                        val expected = IndependentWeeklyOracle.matches(dueIso, targetIso, interval, selectedDays)
                        val actual = RecurrenceEngine.isTaskScheduledOnDate(task, targetIso)

                        assertEquals(
                            "Discrepancy for due=$dueIso, target=$targetIso (offset=$offset), interval=$interval, days=$selectedDays",
                            expected,
                            actual
                        )
                    }
                }
            }
        }
    }

    @Test
    fun testMonthAndYearBoundaryTransitions() {
        // Due on New Year's Eve: 2024-12-31 (Tuesday)
        val task = TaskItem(
            id = 201,
            title = "Boundary Weekly",
            dueDateIso = "2024-12-31",
            recurrenceType = RecurrenceType.EVERY_N_WEEKS,
            recurrenceInterval = 2,
            recurrenceDaysOfWeek = listOf("TUESDAY", "THURSDAY")
        )

        // Week 0: Dec 30 2024 - Jan 5 2025
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(task, "2024-12-31")) // Tuesday
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(task, "2025-01-02")) // Thursday

        // Week 1: Jan 6 - Jan 12 2025 (Skip)
        assertFalse(RecurrenceEngine.isTaskScheduledOnDate(task, "2025-01-07")) // Tuesday
        assertFalse(RecurrenceEngine.isTaskScheduledOnDate(task, "2025-01-09")) // Thursday

        // Week 2: Jan 13 - Jan 19 2025 (Match)
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(task, "2025-01-14")) // Tuesday
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(task, "2025-01-16")) // Thursday
    }

    @Test
    fun testLeapYearBoundaryTransitions() {
        // Leap year boundary: 2024-02-28 (Wednesday)
        val task = TaskItem(
            id = 202,
            title = "Leap Weekly",
            dueDateIso = "2024-02-28",
            recurrenceType = RecurrenceType.EVERY_N_WEEKS,
            recurrenceInterval = 2,
            recurrenceDaysOfWeek = listOf("THURSDAY")
        )

        // Week 0: Feb 26 - Mar 3 2024. Thursday is Feb 29 (leap day)!
        assertTrue(
            "Feb 29 2024 is in week 0 Thursday -> must match",
            RecurrenceEngine.isTaskScheduledOnDate(task, "2024-02-29")
        )

        // Week 1: Mar 4 - Mar 10 2024. Thursday is Mar 7 -> skip
        assertFalse(RecurrenceEngine.isTaskScheduledOnDate(task, "2024-03-07"))

        // Week 2: Mar 11 - Mar 17 2024. Thursday is Mar 14 -> match
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(task, "2024-03-14"))
    }

    @Test
    fun testConfigurableFirstDayOfWeek() {
        // Due: 2025-01-05 (Sunday). Selected: Sunday and Monday. Interval: 2
        val task = TaskItem(
            id = 203,
            title = "Sunday Start",
            dueDateIso = "2025-01-05", // Sunday
            recurrenceType = RecurrenceType.EVERY_N_WEEKS,
            recurrenceInterval = 2,
            recurrenceDaysOfWeek = listOf("SUNDAY", "MONDAY")
        )

        // With firstDayOfWeek = "SUNDAY":
        // Week 0 starts on Sunday Jan 5, 2025.
        // Sunday Jan 5 is week 0 (Match).
        // Monday Jan 6 is week 0 (Match).
        // Next Sunday Jan 12 is week 1 (Skip).
        // Sunday Jan 19 is week 2 (Match).
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(task, "2025-01-05", firstDayOfWeek = "SUNDAY"))
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(task, "2025-01-06", firstDayOfWeek = "SUNDAY"))
        assertFalse(RecurrenceEngine.isTaskScheduledOnDate(task, "2025-01-12", firstDayOfWeek = "SUNDAY"))
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(task, "2025-01-19", firstDayOfWeek = "SUNDAY"))

        // With firstDayOfWeek = "MONDAY":
        // Week 0 for due date (Sunday Jan 5) started on Monday Dec 30, 2024.
        // Sunday Jan 5 is the LAST day of week 0.
        // Monday Jan 6 is week 1! So with Monday start, Jan 6 is skipped!
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(task, "2025-01-05", firstDayOfWeek = "MONDAY"))
        assertFalse(RecurrenceEngine.isTaskScheduledOnDate(task, "2025-01-06", firstDayOfWeek = "MONDAY"))
    }

    // =========================================================================
    // 3. Monthly and Yearly Edge-Case Investigation (Prompt C1 Section 7)
    // =========================================================================

    @Test
    fun testMonthlyClampingEdgeCasesDay29_30_31() {
        // Day 31: Should clamp to last day of shorter months (Apr 30, Feb 28/29)
        val taskDay31 = TaskItem(
            id = 301,
            title = "Monthly Day 31",
            dueDateIso = "2024-01-31", // 2024 is leap year
            recurrenceType = RecurrenceType.MONTHLY
        )

        // Jan 31: Matches
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(taskDay31, "2024-01-31"))

        // Feb 2024 (leap year, 29 days): clamps to Feb 29
        assertTrue("Day 31 in leap Feb clamps to Feb 29", RecurrenceEngine.isTaskScheduledOnDate(taskDay31, "2024-02-29"))
        assertFalse(RecurrenceEngine.isTaskScheduledOnDate(taskDay31, "2024-02-28"))

        // Apr 2024 (30 days): clamps to Apr 30
        assertTrue("Day 31 in April clamps to Apr 30", RecurrenceEngine.isTaskScheduledOnDate(taskDay31, "2024-04-30"))
        assertFalse(RecurrenceEngine.isTaskScheduledOnDate(taskDay31, "2024-04-29"))

        // Non-leap Feb 2025: clamps to Feb 28
        val taskNonLeap = taskDay31.copy(dueDateIso = "2025-01-31")
        assertTrue("Day 31 in non-leap Feb clamps to Feb 28", RecurrenceEngine.isTaskScheduledOnDate(taskNonLeap, "2025-02-28"))
        assertFalse(RecurrenceEngine.isTaskScheduledOnDate(taskNonLeap, "2025-02-27"))

        // Day 30 in Feb: clamps to Feb 28 (non-leap) / Feb 29 (leap)
        val taskDay30 = TaskItem(
            id = 302,
            title = "Monthly Day 30",
            dueDateIso = "2025-01-30",
            recurrenceType = RecurrenceType.MONTHLY
        )
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(taskDay30, "2025-01-30"))
        assertTrue("Day 30 in non-leap Feb clamps to Feb 28", RecurrenceEngine.isTaskScheduledOnDate(taskDay30, "2025-02-28"))
        assertFalse(RecurrenceEngine.isTaskScheduledOnDate(taskDay30, "2025-02-27"))

        // Day 29 in non-leap Feb: clamps to Feb 28
        val taskDay29 = TaskItem(
            id = 303,
            title = "Monthly Day 29",
            dueDateIso = "2025-01-29",
            recurrenceType = RecurrenceType.MONTHLY
        )
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(taskDay29, "2025-01-29"))
        assertTrue("Day 29 in non-leap Feb clamps to Feb 28", RecurrenceEngine.isTaskScheduledOnDate(taskDay29, "2025-02-28"))
    }

    @Test
    fun testYearlyFeb29LeapYearEdgeCases() {
        val leapYearlyTask = TaskItem(
            id = 304,
            title = "Leap Yearly Task",
            dueDateIso = "2024-02-29", // Leap day
            recurrenceType = RecurrenceType.YEARLY
        )

        // 2024 (Leap year): matches Feb 29
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(leapYearlyTask, "2024-02-29"))

        // 2025 (Non-leap): clamps to Feb 28
        assertTrue("Feb 29 in non-leap 2025 clamps to Feb 28", RecurrenceEngine.isTaskScheduledOnDate(leapYearlyTask, "2025-02-28"))
        assertFalse(RecurrenceEngine.isTaskScheduledOnDate(leapYearlyTask, "2025-02-27"))

        // 2026 (Non-leap): clamps to Feb 28
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(leapYearlyTask, "2026-02-28"))

        // 2028 (Leap year): matches Feb 29
        assertTrue("Feb 29 in leap 2028 matches Feb 29", RecurrenceEngine.isTaskScheduledOnDate(leapYearlyTask, "2028-02-29"))
        assertFalse(RecurrenceEngine.isTaskScheduledOnDate(leapYearlyTask, "2028-02-28"))
    }

    // =========================================================================
    // 4. Occurrence Count Optimization & Equivalence Tests (Prompt C1 Section 8)
    // =========================================================================

    @Test
    fun testOccurrenceCountEquivalenceAcrossRecurrenceTypes() {
        val testCases = listOf(
            TaskItem(id = 1, title = "Daily", dueDateIso = "2025-01-01", recurrenceType = RecurrenceType.DAILY),
            TaskItem(id = 2, title = "Every 3 Days", dueDateIso = "2025-01-01", recurrenceType = RecurrenceType.EVERY_N_DAYS, recurrenceInterval = 3),
            TaskItem(id = 3, title = "Weekdays", dueDateIso = "2025-01-01", recurrenceType = RecurrenceType.WEEKDAYS),
            TaskItem(id = 4, title = "Selected Mon/Fri", dueDateIso = "2025-01-01", recurrenceType = RecurrenceType.SELECTED_WEEKDAYS, recurrenceDaysOfWeek = listOf("MONDAY", "FRIDAY")),
            TaskItem(id = 5, title = "Every 2 Weeks Mon/Fri", dueDateIso = "2025-01-01", recurrenceType = RecurrenceType.EVERY_N_WEEKS, recurrenceInterval = 2, recurrenceDaysOfWeek = listOf("MONDAY", "FRIDAY")),
            TaskItem(id = 6, title = "Every 3 Weeks Wed", dueDateIso = "2025-01-01", recurrenceType = RecurrenceType.EVERY_N_WEEKS, recurrenceInterval = 3, recurrenceDaysOfWeek = listOf("WEDNESDAY")),
            TaskItem(id = 7, title = "Monthly Jan 31", dueDateIso = "2024-01-31", recurrenceType = RecurrenceType.MONTHLY),
            TaskItem(id = 8, title = "Every 3 Months", dueDateIso = "2024-01-15", recurrenceType = RecurrenceType.EVERY_N_MONTHS, recurrenceInterval = 3),
            TaskItem(id = 9, title = "Yearly Feb 29", dueDateIso = "2024-02-29", recurrenceType = RecurrenceType.YEARLY)
        )

        for (task in testCases) {
            // Check equivalence across 120 consecutive dates
            for (offset in 0..120) {
                val targetIso = DateUtils.addDays(task.dueDateIso, offset)
                val expectedCount = ReferenceOccurrenceCounter.countUpTo(task, targetIso)
                val optimizedCount = RecurrenceEngine.countOccurrencesUpTo(task, targetIso)

                assertEquals(
                    "Count mismatch for task ${task.title} at target=$targetIso (offset=$offset)",
                    expectedCount,
                    optimizedCount
                )
            }
        }
    }

    @Test
    fun testOccurrenceCountMultiYearPerformanceBenchmark() {
        val task = TaskItem(
            id = 401,
            title = "Bi-weekly Workouts",
            dueDateIso = "2020-01-01",
            recurrenceType = RecurrenceType.EVERY_N_WEEKS,
            recurrenceInterval = 2,
            recurrenceDaysOfWeek = listOf("MONDAY", "WEDNESDAY", "FRIDAY")
        )

        // Benchmark counting 1,826 days (5 full years) using optimized countOccurrencesUpTo
        val startTime = System.nanoTime()
        val totalCalls = 1000
        for (i in 0 until totalCalls) {
            val count = RecurrenceEngine.countOccurrencesUpTo(task, "2025-01-01")
            assertTrue("Count must be positive", count > 0)
        }
        val elapsedMs = (System.nanoTime() - startTime) / 1_000_000

        // 1,000 multi-year counts should complete in well under 100ms with O(1) arithmetic
        assertTrue("1,000 5-year occurrence counts completed in ${elapsedMs}ms (expected < 100ms)", elapsedMs < 100)
    }

    // =========================================================================
    // 5. End Condition AFTER_COUNT Integration
    // =========================================================================

    @Test
    fun testAfterCountTerminationWithWeeklyRecurrence() {
        val task = TaskItem(
            id = 501,
            title = "3 Sessions Only",
            dueDateIso = "2025-01-01", // Wednesday
            recurrenceType = RecurrenceType.EVERY_N_WEEKS,
            recurrenceInterval = 2,
            recurrenceDaysOfWeek = listOf("MONDAY", "FRIDAY"),
            recurrenceEndCondition = "AFTER_COUNT",
            recurrenceCount = 3
        )

        // Occurrence 1: Friday Jan 3 (Week 0)
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(task, "2025-01-03"))

        // Occurrence 2: Monday Jan 13 (Week 2)
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(task, "2025-01-13"))

        // Occurrence 3: Friday Jan 17 (Week 2)
        assertTrue(RecurrenceEngine.isTaskScheduledOnDate(task, "2025-01-17"))

        // Occurrence 4: Monday Jan 27 (Week 4) -> should be CUT OFF because count > 3
        assertFalse("4th occurrence must be cut off after count=3", RecurrenceEngine.isTaskScheduledOnDate(task, "2025-01-27"))
    }
}
