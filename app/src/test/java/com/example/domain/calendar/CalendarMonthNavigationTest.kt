package com.example.domain.calendar

import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarMonthNavigationTest {

    @Test
    fun `month changes preserve selected day and clamp month ends`() {
        assertEquals("2024-02-29", CalendarMonthNavigation.shiftSelectedDate("2024-01-31", 1))
        assertEquals("2024-01-29", CalendarMonthNavigation.shiftSelectedDate("2024-02-29", -1))
    }

    @Test
    fun `month changes cross year boundaries`() {
        assertEquals("2025-01-15", CalendarMonthNavigation.shiftSelectedDate("2024-12-15", 1))
        assertEquals("2024-12-15", CalendarMonthNavigation.shiftSelectedDate("2025-01-15", -1))
    }

    @Test
    fun `repeated month changes remain stable`() {
        var date = "2024-01-31"
        repeat(12) {
            date = CalendarMonthNavigation.shiftSelectedDate(date, 1)
        }
        assertEquals("2025-01-29", date)
    }
}
