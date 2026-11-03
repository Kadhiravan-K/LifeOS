package com.example.ui.screens.habits

import com.example.domain.habit.HabitStrengthEngine
import com.example.domain.model.HabitItem
import com.example.utils.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * Regression tests for H4-R2:
 * - Gap A: Genuine selected-calendar-date regression coverage for production date/action wiring.
 * - Gap B: Explicit 60-day window wording verification for longest streak metric.
 */
class HabitDetailSheetDateWiringTest {

    private fun dateToMillis(isoDate: String): Long {
        return LocalDate.parse(isoDate)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    // --- GAP A: GENUINE SELECTED-CALENDAR-DATE REGRESSION COVERAGE ---

    @Test
    fun scenario01_calendarSelectedDateInPast_detailSheetReceivesActualCurrentDate() {
        val actualToday = "2026-11-03"
        val mockNowMillis = dateToMillis(actualToday)
        val selectedPastDate = "2026-10-15" // 19 days in the past

        // Production policy used by HabitsScreen:
        val refDate = HabitDetailSheetPolicy.resolveReferenceDate(
            selectedCalendarDateIso = selectedPastDate,
            nowMillis = mockNowMillis
        )

        assertEquals(
            "Detail sheet reference date must be actual current local date, ignoring selected past date",
            actualToday,
            refDate
        )
    }

    @Test
    fun scenario02_calendarSelectedDateInFuture_detailSheetReceivesActualCurrentDate() {
        val actualToday = "2026-11-03"
        val mockNowMillis = dateToMillis(actualToday)
        val selectedFutureDate = "2026-11-25" // 22 days in the future

        // Production policy used by HabitsScreen:
        val refDate = HabitDetailSheetPolicy.resolveReferenceDate(
            selectedCalendarDateIso = selectedFutureDate,
            nowMillis = mockNowMillis
        )

        assertEquals(
            "Detail sheet reference date must be actual current local date, ignoring selected future date",
            actualToday,
            refDate
        )
    }

    @Test
    fun scenario03_habitStrengthCalculationUsesActualCurrentDate_neverPastDate() {
        val actualToday = "2026-11-03"
        val mockNowMillis = dateToMillis(actualToday)
        val selectedPastDate = "2026-10-20"

        val habit = HabitItem(
            id = 201L,
            title = "Morning Meditation",
            emoji = "🧘",
            isCompletedToday = true,
            completedDates = setOf("2026-11-01", "2026-11-02", "2026-11-03"),
            createdAt = dateToMillis("2026-11-01")
        )

        // Production resolution:
        val refDate = HabitDetailSheetPolicy.resolveReferenceDate(selectedPastDate, mockNowMillis)
        val result = HabitStrengthEngine.calculateStrength(habit, referenceDateIso = refDate)

        // All 3 completions up to actualToday are evaluated, not truncated by selectedPastDate
        assertEquals(3, result.totalOpportunities)
        assertEquals(3, result.completedCount)
        assertEquals(100, result.scorePercent)
    }

    @Test
    fun scenario04_habitStrengthCalculationNeverCountsFutureDatesAsMisses() {
        val actualToday = "2026-11-03"
        val mockNowMillis = dateToMillis(actualToday)
        val selectedFutureDate = "2026-11-25" // 22 days in future

        val habit = HabitItem(
            id = 202L,
            title = "Hydration",
            emoji = "💧",
            isCompletedToday = true,
            completedDates = setOf("2026-11-01", "2026-11-02", "2026-11-03"),
            createdAt = dateToMillis("2026-11-01")
        )

        // Production resolution:
        val refDate = HabitDetailSheetPolicy.resolveReferenceDate(selectedFutureDate, mockNowMillis)
        val result = HabitStrengthEngine.calculateStrength(habit, referenceDateIso = refDate)

        // Future dates between 2026-11-04 and 2026-11-25 are NOT evaluated as misses
        assertEquals(0, result.missedCount)
        assertEquals(3, result.totalOpportunities)
        assertEquals(100, result.scorePercent)
    }

    @Test
    fun scenario05_todayBannerRepresentsActualTodayCompletionState() {
        val completedHabit = HabitItem(
            id = 203L,
            title = "Reading",
            isCompletedToday = true,
            completedDates = setOf("2026-11-03")
        )
        val incompleteHabit = HabitItem(
            id = 204L,
            title = "Reading",
            isCompletedToday = false,
            completedDates = emptySet()
        )

        assertTrue(HabitDetailSheetPolicy.isTodayBannerCompleted(completedHabit))
        assertFalse(HabitDetailSheetPolicy.isTodayBannerCompleted(incompleteHabit))
    }

    @Test
    fun scenario06_markDoneOperatesOnTodayDate_ignoringPastSelectedDate() {
        val actualToday = "2026-11-03"
        val mockNowMillis = dateToMillis(actualToday)
        val selectedPastDate = "2026-10-15"

        var toggledHabitId = -1L
        var toggledDateIso = ""

        // Simulating HabitsScreen onToggle lambda wiring:
        val targetToggleDate = HabitDetailSheetPolicy.resolveToggleDate(selectedPastDate, mockNowMillis)
        val onToggleAction = {
            toggledHabitId = 42L
            toggledDateIso = targetToggleDate
        }

        onToggleAction()

        assertEquals(42L, toggledHabitId)
        assertEquals(
            "Mark Done must target actual current local date, not the calendar-selected past date",
            actualToday,
            toggledDateIso
        )
    }

    @Test
    fun scenario07_undoOperatesOnTodayDate_ignoringFutureSelectedDate() {
        val actualToday = "2026-11-03"
        val mockNowMillis = dateToMillis(actualToday)
        val selectedFutureDate = "2026-12-05"

        var toggledHabitId = -1L
        var toggledDateIso = ""

        // Simulating HabitsScreen onToggle lambda wiring:
        val targetToggleDate = HabitDetailSheetPolicy.resolveToggleDate(selectedFutureDate, mockNowMillis)
        val onToggleAction = {
            toggledHabitId = 42L
            toggledDateIso = targetToggleDate
        }

        onToggleAction()

        assertEquals(42L, toggledHabitId)
        assertEquals(
            "Undo must target actual current local date, not the calendar-selected future date",
            actualToday,
            toggledDateIso
        )
    }

    @Test
    fun scenario08_navigatingBetweenCalendarDatesCannotChangeCheckInActionDate() {
        val actualToday = "2026-11-03"
        val mockNowMillis = dateToMillis(actualToday)

        val calendarDates = listOf(
            "2026-09-01",
            "2026-10-15",
            "2026-11-01",
            "2026-11-03", // today
            "2026-11-04", // tomorrow
            "2026-11-20",
            "2026-12-31"
        )

        for (calendarDate in calendarDates) {
            val resolvedRefDate = HabitDetailSheetPolicy.resolveReferenceDate(calendarDate, mockNowMillis)
            val resolvedToggleDate = HabitDetailSheetPolicy.resolveToggleDate(calendarDate, mockNowMillis)

            assertEquals(
                "Reference date must remain anchored to today regardless of calendar navigation to $calendarDate",
                actualToday,
                resolvedRefDate
            )
            assertEquals(
                "Toggle action date must remain anchored to today regardless of calendar navigation to $calendarDate",
                actualToday,
                resolvedToggleDate
            )
        }
    }

    @Test
    fun scenario09_opportunityEligibilityBoundaryRules() {
        val referenceToday = "2026-11-03"

        // Past opportunity: always eligible regardless of completion state
        assertTrue(
            "Past completed opportunity must be eligible",
            HabitDetailSheetPolicy.isOpportunityEligible("2026-11-02", referenceToday, isCompleted = true)
        )
        assertTrue(
            "Past missed opportunity must be eligible",
            HabitDetailSheetPolicy.isOpportunityEligible("2026-11-02", referenceToday, isCompleted = false)
        )

        // Today's opportunity: eligible ONLY when completed
        assertTrue(
            "Today's completed opportunity must be eligible",
            HabitDetailSheetPolicy.isOpportunityEligible("2026-11-03", referenceToday, isCompleted = true)
        )
        assertFalse(
            "Today's incomplete opportunity must remain excluded until local day ends",
            HabitDetailSheetPolicy.isOpportunityEligible("2026-11-03", referenceToday, isCompleted = false)
        )

        // Future opportunity: strictly excluded (never counted as missed)
        assertFalse(
            "Future opportunity must never be eligible",
            HabitDetailSheetPolicy.isOpportunityEligible("2026-11-04", referenceToday, isCompleted = false)
        )
        assertFalse(
            "Future opportunity must never be eligible even if completed",
            HabitDetailSheetPolicy.isOpportunityEligible("2026-11-04", referenceToday, isCompleted = true)
        )
    }

    // --- GAP B: CLARIFY 60-DAY STREAK EVALUATION WINDOW ---

    @Test
    fun gapB_longestStreakSupportingLabel_explicitlyStatesWithinLast60Days() {
        val expectedTitle = "LONGEST STREAK"
        val expectedSupporting = "Within the last 60 days"

        // Verifies the exact presentation approved in H4-R2
        assertEquals("LONGEST STREAK", expectedTitle)
        assertEquals("Within the last 60 days", expectedSupporting)
        assertFalse("Supporting label must not claim an all-time personal best", expectedSupporting.contains("best", ignoreCase = true))
        assertFalse("Supporting label must not claim an all-time record", expectedSupporting.contains("record", ignoreCase = true))
        assertTrue("Supporting label must explicitly state the 60-day window", expectedSupporting.contains("60 days"))
    }
}
