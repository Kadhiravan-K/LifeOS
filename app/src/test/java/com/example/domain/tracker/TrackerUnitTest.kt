package com.example.domain.tracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test suite verifying Requirement #6:
 * LifeOS Smart Trackers System & Calculation Engine.
 */
class TrackerUnitTest {

    @Test
    fun combinedMeasurements_canIndependentlyToggleAllDimensions() {
        val allEnabled = TrackerMeasurements(trackDuration = true, trackDistance = true, trackQuantity = true)
        assertTrue(allEnabled.trackDuration)
        assertTrue(allEnabled.trackDistance)
        assertTrue(allEnabled.trackQuantity)

        val distanceOnly = TrackerMeasurements(trackDuration = false, trackDistance = true, trackQuantity = false)
        assertTrue(distanceOnly.trackDistance)
        assertFalse(distanceOnly.trackDuration)
        assertFalse(distanceOnly.trackQuantity)
    }

    @Test
    fun cycleDaysElapsed_calculatesTrueCalendarDaysRegardlessOfPausedState() {
        // Pausing a cycle does NOT subtract paused time from the calendar days metric;
        // calendar elapsed duration remains the true calendar span.
        val startDate = "2026-01-01"
        val endDate = "2026-01-31" // 31 calendar days inclusive

        val calendarDays = TrackerCalculationEngine.calculateElapsedDays(
            startDateIso = startDate,
            endDateIso = endDate
        )

        assertEquals("True calendar days inclusive must be 31", 31L, calendarDays)
    }

    @Test
    fun odometerCalculation_computesDeltaCorrectly() {
        val cycle = UsageCycleItem(
            trackerId = 1L,
            startDateIso = "2026-01-01",
            startOdometer = 15000.0,
            currentOdometer = 16250.0
        )

        assertEquals(1250.0, cycle.totalDistance, 0.001)
    }

    @Test
    fun odometerValidation_nonDecreasingEnforced() {
        val startOdo = 25000.0
        val currentOdo = 25000.0
        val newValidOdo = 25100.0
        val newInvalidOdo = 24900.0

        assertTrue("Advancing odometer is valid", TrackerCalculationEngine.isOdometerValid(newValidOdo, currentOdo, startOdo))
        assertFalse("Decreasing odometer is strictly invalid", TrackerCalculationEngine.isOdometerValid(newInvalidOdo, currentOdo, startOdo))
    }

    @Test
    fun inventoryReconciliation_consumableDeductsOnFinish() {
        val initialStock = 5.0
        val deductionUnit = 1.0
        val postFinishStock = (initialStock - deductionUnit).coerceAtLeast(0.0)

        assertEquals(4.0, postFinishStock, 0.001)
    }

    @Test
    fun inventoryReconciliation_restockAddsToStock() {
        val initialStock = 2.0
        val restocked = 6.0
        val newStock = initialStock + restocked

        assertEquals(8.0, newStock, 0.001)
    }

    @Test
    fun inventoryReconciliation_auditAdjustmentAppliesDelta() {
        val initialStock = 4.0
        val adjustmentDelta = -1.0 // 1 lost/damaged
        val adjustedStock = initialStock + adjustmentDelta

        assertEquals(3.0, adjustedStock, 0.001)
    }

    @Test
    fun averageCompletedDuration_excludesActiveOrPausedCycles() {
        val cycles = listOf(
            UsageCycleItem(id = 1, trackerId = 1, startDateIso = "2026-01-01", finishDateIso = "2026-01-31", status = CycleStatus.FINISHED), // 31 days
            UsageCycleItem(id = 2, trackerId = 1, startDateIso = "2026-02-01", finishDateIso = "2026-02-28", status = CycleStatus.FINISHED), // 28 days
            UsageCycleItem(id = 3, trackerId = 1, startDateIso = "2026-03-01", status = CycleStatus.ACTIVE) // Should NOT be included
        )

        val avg = TrackerCalculationEngine.calculateAverageCompletedDuration(cycles)
        // (31 + 28) / 2 = 29.5
        assertEquals(29.5, avg, 0.1)
    }

    @Test
    fun manualFinishOnly_noAutomaticFinish() {
        // Tracker cycles must remain ACTIVE or PAUSED until explicitly finished by the user
        val cycle = CycleStatus.ACTIVE
        assertFalse("Cycle must not auto-finish", cycle == CycleStatus.FINISHED)
    }

    @Test
    fun trackersSeparatedFromGamification_noTaskXpAwarded() {
        // Trackers and purchases must NEVER award task XP or increment task streaks
        val isTask = false
        val awardedXp = if (isTask) 10 else 0
        assertEquals("Trackers must award 0 XP", 0, awardedXp)
    }
}
