package com.example.domain.scheduling

import com.example.domain.model.DurationUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class SchedulingEngineSecondsUnitTest {

    @Test
    fun testSecondsToElapsedMinutesConversion() {
        assertEquals(1, SchedulingEngine.toElapsedMinutes("2026-11-07", 1, DurationUnit.SECONDS))
        assertEquals(1, SchedulingEngine.toElapsedMinutes("2026-11-07", 30, DurationUnit.SECONDS))
        assertEquals(1, SchedulingEngine.toElapsedMinutes("2026-11-07", 60, DurationUnit.SECONDS))
        assertEquals(2, SchedulingEngine.toElapsedMinutes("2026-11-07", 61, DurationUnit.SECONDS))
        assertEquals(2, SchedulingEngine.toElapsedMinutes("2026-11-07", 120, DurationUnit.SECONDS))
        assertEquals(3, SchedulingEngine.toElapsedMinutes("2026-11-07", 150, DurationUnit.SECONDS))
    }

    @Test
    fun testSecondsProjectedEndWithStartTime() {
        val proj = SchedulingEngine.calculateProjectedEnd(
            startDateIso = "2026-11-07",
            startTime = "10:00",
            isAllDay = false,
            amount = 90,
            unit = DurationUnit.SECONDS
        )
        assertEquals("2026-11-07", proj.endDateIso)
        assertEquals("10:01:30", proj.endTime)
        assertEquals("Ends at 10:01:30 (90 sec)", proj.displaySummary)
    }

    @Test
    fun testSecondsProjectedEndAllDay() {
        val proj = SchedulingEngine.calculateProjectedEnd(
            startDateIso = "2026-11-07",
            startTime = null,
            isAllDay = true,
            amount = 45,
            unit = DurationUnit.SECONDS
        )
        assertEquals("2026-11-07", proj.endDateIso)
        assertEquals("Duration: 45 seconds", proj.displaySummary)
    }
}
