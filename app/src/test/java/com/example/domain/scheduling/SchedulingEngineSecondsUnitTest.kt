package com.example.domain.scheduling

import com.example.domain.model.DurationUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class SchedulingEngineSecondsUnitTest {

    @Test
    fun `seconds duration preserves seconds precision across midnight`() {
        val projectedEnd = SchedulingEngine.calculateProjectedEnd(
            startDateIso = "2025-01-31",
            startTime = "23:59",
            isAllDay = false,
            amount = 70,
            unit = DurationUnit.SECONDS
        )

        assertEquals("2025-02-01", projectedEnd.endDateIso)
        assertEquals("00:00:10", projectedEnd.endTime)
    }

    @Test
    fun `seconds convert to elapsed minutes without truncating`() {
        assertEquals(1, SchedulingEngine.toElapsedMinutes("2025-01-01", 1, DurationUnit.SECONDS))
        assertEquals(1, SchedulingEngine.toElapsedMinutes("2025-01-01", 60, DurationUnit.SECONDS))
        assertEquals(2, SchedulingEngine.toElapsedMinutes("2025-01-01", 61, DurationUnit.SECONDS))
    }
}
