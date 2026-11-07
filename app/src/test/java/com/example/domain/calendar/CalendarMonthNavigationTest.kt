package com.example.domain.calendar

import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarMonthNavigationTest {

    @Test
    fun testMonthShiftRegular() {
        val next = CalendarMonthNavigation.shiftSelectedDate("2026-05-15", 1)
        assertEquals("2026-06-15", next)

        val prev = CalendarMonthNavigation.shiftSelectedDate("2026-05-15", -1)
        assertEquals("2026-04-15", prev)
    }

    @Test
    fun testMonthShiftBoundaryClamp() {
        // January 31 -> February has only 28 days (non-leap)
        val shifted = CalendarMonthNavigation.shiftSelectedDate("2026-01-31", 1)
        assertEquals("2026-02-28", shifted)

        // March 31 -> April 30
        val april = CalendarMonthNavigation.shiftSelectedDate("2026-03-31", 1)
        assertEquals("2026-04-30", april)
    }

    @Test
    fun testMonthShiftYearBoundary() {
        val decToJan = CalendarMonthNavigation.shiftSelectedDate("2026-12-15", 1)
        assertEquals("2027-01-15", decToJan)

        val janToDec = CalendarMonthNavigation.shiftSelectedDate("2027-01-15", -1)
        assertEquals("2026-12-15", janToDec)
    }

    @Test
    fun testMonthShiftLeapYear() {
        // 2028 is leap year -> Feb has 29 days
        val leapFeb = CalendarMonthNavigation.shiftSelectedDate("2028-01-31", 1)
        assertEquals("2028-02-29", leapFeb)
    }
}
