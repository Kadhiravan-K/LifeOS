package com.example.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying unified date/time input parsing, formatting, duration calculations,
 * and overnight shift range validation.
 */
class UnifiedDateTimeInputUnitTest {

    @Test
    fun parseTimeToHourMinute_validInputs() {
        assertEquals(8 to 30, parseTimeToHourMinute("08:30"))
        assertEquals(0 to 0, parseTimeToHourMinute("00:00"))
        assertEquals(23 to 59, parseTimeToHourMinute("23:59"))
        assertEquals(12 to 0, parseTimeToHourMinute("12:00"))
    }

    @Test
    fun parseTimeToHourMinute_invalidInputs_returnsDefault() {
        assertEquals(9 to 0, parseTimeToHourMinute(null))
        assertEquals(9 to 0, parseTimeToHourMinute(""))
        assertEquals(9 to 0, parseTimeToHourMinute("invalid"))
        assertEquals(9 to 0, parseTimeToHourMinute("not:number"))
    }

    @Test
    fun parseTimeToHourMinute_outOfRange_clamped() {
        assertEquals(23 to 59, parseTimeToHourMinute("25:99"))
        assertEquals(23 to 0, parseTimeToHourMinute("30:00"))
        assertEquals(0 to 59, parseTimeToHourMinute("-5:100"))
    }

    @Test
    fun formatTimeDisplay_24HourMode() {
        assertEquals("08:30", formatTimeDisplay("08:30", is24Hour = true))
        assertEquals("00:00", formatTimeDisplay("00:00", is24Hour = true))
        assertEquals("14:45", formatTimeDisplay("14:45", is24Hour = true))
        assertEquals("23:59", formatTimeDisplay("23:59", is24Hour = true))
    }

    @Test
    fun formatTimeDisplay_12HourMode() {
        assertEquals("8:30 AM", formatTimeDisplay("08:30", is24Hour = false))
        assertEquals("12:00 AM", formatTimeDisplay("00:00", is24Hour = false))
        assertEquals("12:00 PM", formatTimeDisplay("12:00", is24Hour = false))
        assertEquals("2:45 PM", formatTimeDisplay("14:45", is24Hour = false))
        assertEquals("11:59 PM", formatTimeDisplay("23:59", is24Hour = false))
    }

    @Test
    fun isTimeRangeCrossMidnight_detection() {
        // Normal daytime range
        assertFalse(isTimeRangeCrossMidnight("08:00", "16:00"))
        assertFalse(isTimeRangeCrossMidnight("09:00", "17:00"))
        assertFalse(isTimeRangeCrossMidnight("12:00", "12:00"))

        // Crosses midnight (night shift)
        assertTrue(isTimeRangeCrossMidnight("22:00", "06:00"))
        assertTrue(isTimeRangeCrossMidnight("20:00", "04:00"))
        assertTrue(isTimeRangeCrossMidnight("23:30", "07:30"))
        assertTrue(isTimeRangeCrossMidnight("23:45", "00:15"))
    }

    @Test
    fun calculateTimeDurationMinutes_sameDayShift() {
        // 8:00 to 16:00 = 8 hours = 480 minutes
        val duration = calculateTimeDurationMinutes("08:00", "16:00", isOvernight = false)
        assertEquals(480, duration)
        assertEquals("8h", formatDurationMinutesDisplay(duration))

        // 9:15 to 17:45 = 8 hours 30 mins = 510 minutes
        val durationHalf = calculateTimeDurationMinutes("09:15", "17:45", isOvernight = false)
        assertEquals(510, durationHalf)
        assertEquals("8h 30m", formatDurationMinutesDisplay(durationHalf))
    }

    @Test
    fun calculateTimeDurationMinutes_overnightShift() {
        // 22:00 to 06:00 (crosses midnight) = 8 hours = 480 minutes
        val duration = calculateTimeDurationMinutes("22:00", "06:00", isOvernight = true)
        assertEquals(480, duration)
        assertEquals("8h", formatDurationMinutesDisplay(duration))

        // Auto-detected overnight when endTime < startTime even if isOvernight flag is false
        val autoOvernight = calculateTimeDurationMinutes("22:00", "06:00", isOvernight = false)
        assertEquals(480, autoOvernight)

        // 23:30 to 00:30 (crosses midnight) = 1 hour = 60 minutes
        val shortOvernight = calculateTimeDurationMinutes("23:30", "00:30", isOvernight = true)
        assertEquals(60, shortOvernight)
        assertEquals("1h", formatDurationMinutesDisplay(shortOvernight))

        // 20:00 to 08:30 = 12 hours 30 minutes = 750 minutes
        val longShift = calculateTimeDurationMinutes("20:00", "08:30", isOvernight = true)
        assertEquals(750, longShift)
        assertEquals("12h 30m", formatDurationMinutesDisplay(longShift))
    }

    @Test
    fun formatDurationMinutesDisplay_formatsCorrectly() {
        assertEquals("45m", formatDurationMinutesDisplay(45))
        assertEquals("1h", formatDurationMinutesDisplay(60))
        assertEquals("1h 15m", formatDurationMinutesDisplay(75))
        assertEquals("8h", formatDurationMinutesDisplay(480))
        assertEquals("8h 05m", formatDurationMinutesDisplay(485))
    }

    @Test
    fun calculateTimeDurationMinutes_handlesEmptyOrNull() {
        assertEquals(0, calculateTimeDurationMinutes(null, "08:00"))
        assertEquals(0, calculateTimeDurationMinutes("08:00", null))
        assertEquals(0, calculateTimeDurationMinutes("", ""))
    }
}
