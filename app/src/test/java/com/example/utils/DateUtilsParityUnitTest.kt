package com.example.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Independent legacy oracle implementing the legacy SimpleDateFormat and Calendar
 * algorithms from the original DateUtils.kt.
 *
 * Used as an independent reference to ensure 100% mathematical and behavioral parity
 * across 2000-01-01 through 2040-12-31 (14,976 days).
 */
object DateUtilsLegacyOracle {

    fun todayString(timeZone: TimeZone = TimeZone.getDefault(), nowMillis: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance(timeZone).apply {
            timeInMillis = nowMillis
        }
        return String.format(
            Locale.US,
            "%04d-%02d-%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun epochDayToIsoString(epochDay: Long): String {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            timeInMillis = epochDay * (1000L * 60 * 60 * 24)
        }
        return String.format(
            Locale.US,
            "%04d-%02d-%02d",
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun isoStringToEpochDay(isoDate: String): Long {
        return try {
            val parts = isoDate.split("-")
            if (parts.size == 3) {
                val year = parts[0].toInt()
                val month = parts[1].toInt()
                val day = parts[2].toInt()
                val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                    clear()
                    set(year, month - 1, day)
                }
                Math.floorDiv(cal.timeInMillis, 1000L * 60 * 60 * 24)
            } else {
                Math.floorDiv(System.currentTimeMillis(), 1000L * 60 * 60 * 24)
            }
        } catch (e: Exception) {
            Math.floorDiv(System.currentTimeMillis(), 1000L * 60 * 60 * 24)
        }
    }

    fun getDayOfWeek(isoDate: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = sdf.parse(isoDate) ?: Date()
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                time = date
            }
            when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.SUNDAY -> "SUNDAY"
                Calendar.MONDAY -> "MONDAY"
                Calendar.TUESDAY -> "TUESDAY"
                Calendar.WEDNESDAY -> "WEDNESDAY"
                Calendar.THURSDAY -> "THURSDAY"
                Calendar.FRIDAY -> "FRIDAY"
                Calendar.SATURDAY -> "SATURDAY"
                else -> "UNKNOWN"
            }
        } catch (e: Exception) {
            "UNKNOWN"
        }
    }

    fun addDays(isoDate: String, daysToAdd: Int): String {
        return try {
            epochDayToIsoString(isoStringToEpochDay(isoDate) + daysToAdd)
        } catch (e: Exception) {
            isoDate
        }
    }

    fun daysBetween(startDateIso: String, endDateIso: String): Int {
        return try {
            (isoStringToEpochDay(endDateIso) - isoStringToEpochDay(startDateIso)).toInt()
        } catch (e: Exception) {
            0
        }
    }

    fun getDaysInMonth(year: Int, monthZeroIndexed: Int): Int {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroIndexed)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }
}

/**
 * Test suite verifying 100% parity and robustness for the java.time refactoring in DateUtils.kt.
 */
class DateUtilsParityUnitTest {

    @Test
    fun testExhaustiveParityAcross40Years() {
        // Comprehensive check from 2000-01-01 to 2040-12-31 (14,976 consecutive dates)
        var current = LocalDate.of(2000, 1, 1)
        val end = LocalDate.of(2040, 12, 31)

        var totalDaysChecked = 0

        while (!current.isAfter(end)) {
            val isoDate = current.toString() // yyyy-MM-dd

            // 1. epochDay round-trip parity
            val legacyEpoch = DateUtilsLegacyOracle.isoStringToEpochDay(isoDate)
            val actualEpoch = DateUtils.isoStringToEpochDay(isoDate)
            assertEquals("Epoch day mismatch for $isoDate", legacyEpoch, actualEpoch)

            val legacyIsoFromEpoch = DateUtilsLegacyOracle.epochDayToIsoString(actualEpoch)
            val actualIsoFromEpoch = DateUtils.epochDayToIsoString(actualEpoch)
            assertEquals("Epoch to ISO mismatch for $isoDate", legacyIsoFromEpoch, actualIsoFromEpoch)
            assertEquals("Round-trip mismatch for $isoDate", isoDate, actualIsoFromEpoch)

            // 2. getDayOfWeek parity
            val legacyDow = DateUtilsLegacyOracle.getDayOfWeek(isoDate)
            val actualDow = DateUtils.getDayOfWeek(isoDate)
            assertEquals("Day of week mismatch for $isoDate", legacyDow, actualDow)

            // 3. addDays parity (+1, +7, +30, -1, -7, -30)
            for (offset in intArrayOf(1, 7, 30, -1, -7, -30)) {
                val legacyAdd = DateUtilsLegacyOracle.addDays(isoDate, offset)
                val actualAdd = DateUtils.addDays(isoDate, offset)
                assertEquals("addDays($isoDate, $offset) mismatch", legacyAdd, actualAdd)
            }

            // 4. daysBetween parity
            val futureIso = current.plusDays(14).toString()
            val legacyBetween = DateUtilsLegacyOracle.daysBetween(isoDate, futureIso)
            val actualBetween = DateUtils.daysBetween(isoDate, futureIso)
            assertEquals("daysBetween($isoDate, $futureIso) mismatch", legacyBetween, actualBetween)
            assertEquals("daysBetween should be 14", 14, actualBetween)

            current = current.plusDays(1)
            totalDaysChecked++
        }

        assertEquals("Total dates checked must be 14,976", 14976, totalDaysChecked)
    }

    @Test
    fun testGetDaysInMonthParity() {
        for (year in 2000..2040) {
            for (month in 0..11) {
                val legacyDays = DateUtilsLegacyOracle.getDaysInMonth(year, month)
                val actualDays = DateUtils.getDaysInMonth(year, month)
                assertEquals("Days in month mismatch for $year-$month", legacyDays, actualDays)
            }
        }
    }

    @Test
    fun testLeapYearParity() {
        for (year in 1900..2100) {
            val legacyLeap = DateUtilsLegacyOracle.isLeapYear(year)
            val actualLeap = DateUtils.isLeapYear(year)
            assertEquals("Leap year mismatch for year $year", legacyLeap, actualLeap)
        }
        // Specific boundary cases
        assertTrue(DateUtils.isLeapYear(2000)) // Leap century
        assertFalse(DateUtils.isLeapYear(1900)) // Non-leap century
        assertFalse(DateUtils.isLeapYear(2100)) // Non-leap century
        assertTrue(DateUtils.isLeapYear(2024)) // Regular leap year
        assertFalse(DateUtils.isLeapYear(2025)) // Non-leap year
    }

    @Test
    fun testMultithreadedStress8Threads10000Calls() {
        val numThreads = 8
        val callsPerThread = 10000
        val executor = Executors.newFixedThreadPool(numThreads)
        val latch = CountDownLatch(numThreads)
        val errorCount = AtomicInteger(0)

        val testDates = listOf(
            "2026-11-01", "2024-02-29", "2000-01-01", "2040-12-31",
            "2023-07-15", "2025-10-31", "2026-01-01", "2026-08-20"
        )

        for (t in 0 until numThreads) {
            executor.submit {
                try {
                    for (i in 0 until callsPerThread) {
                        val date = testDates[i % testDates.size]
                        val dow = DateUtils.getDayOfWeek(date)
                        if (dow == "UNKNOWN") errorCount.incrementAndGet()

                        val formatted = DateUtils.formatDisplayDate(date)
                        if (formatted.isBlank()) errorCount.incrementAndGet()

                        val nextDay = DateUtils.addDays(date, 1)
                        if (nextDay.isBlank()) errorCount.incrementAndGet()

                        val epoch = DateUtils.isoStringToEpochDay(date)
                        val roundTrip = DateUtils.epochDayToIsoString(epoch)
                        if (roundTrip != date) errorCount.incrementAndGet()
                    }
                } catch (e: Exception) {
                    errorCount.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }

        val completed = latch.await(60, TimeUnit.SECONDS)
        executor.shutdown()
        assertTrue("Multithreaded stress test timed out", completed)
        assertEquals("Multithreaded stress test encountered exceptions or corrupted state", 0, errorCount.get())
    }

    @Test
    fun testDstTransitionDates() {
        // US Spring Forward 2026: March 8, 2026
        assertEquals("SUNDAY", DateUtils.getDayOfWeek("2026-03-08"))
        assertEquals("2026-03-09", DateUtils.addDays("2026-03-08", 1))
        assertEquals("2026-03-07", DateUtils.addDays("2026-03-08", -1))

        // US Fall Back 2026: November 1, 2026
        assertEquals("SUNDAY", DateUtils.getDayOfWeek("2026-11-01"))
        assertEquals("2026-11-02", DateUtils.addDays("2026-11-01", 1))
        assertEquals("2026-10-31", DateUtils.addDays("2026-11-01", -1))

        // European DST Spring 2026: March 29, 2026
        assertEquals("SUNDAY", DateUtils.getDayOfWeek("2026-03-29"))
        assertEquals("2026-03-30", DateUtils.addDays("2026-03-29", 1))

        // European DST Autumn 2026: October 25, 2026
        assertEquals("SUNDAY", DateUtils.getDayOfWeek("2026-10-25"))
        assertEquals("2026-10-26", DateUtils.addDays("2026-10-25", 1))
    }

    @Test
    fun testInvalidInputFallbacks() {
        // Invalid ISO strings in getDayOfWeek
        assertEquals("UNKNOWN", DateUtils.getDayOfWeek("invalid-date"))
        assertEquals("UNKNOWN", DateUtils.getDayOfWeek(""))
        assertEquals("UNKNOWN", DateUtils.getDayOfWeek("2026-13-45"))

        // Invalid ISO in addDays
        assertEquals("not-a-date", DateUtils.addDays("not-a-date", 3))
        assertEquals("", DateUtils.addDays("", 3))

        // Invalid in daysBetween
        assertEquals(0, DateUtils.daysBetween("bad-start", "2026-11-01"))
        assertEquals(0, DateUtils.daysBetween("2026-11-01", "bad-end"))

        // Invalid in formatDisplayDate
        assertEquals("invalid-input", DateUtils.formatDisplayDate("invalid-input"))
        assertEquals("", DateUtils.formatDisplayDate(""))

        // Invalid in formatMonthYear
        assertEquals("invalid-date", DateUtils.formatMonthYear("invalid-date"))
    }

    @Test
    fun testClockInjection() {
        val fixedInstant = Instant.parse("2026-11-01T14:30:00Z")
        val fixedClock = Clock.fixed(fixedInstant, ZoneId.of("UTC"))

        // todayString with injected clock
        val todayUtc = DateUtils.todayString(timeZone = TimeZone.getTimeZone("UTC"), clock = fixedClock)
        assertEquals("2026-11-01", todayUtc)

        // Today epoch day
        val epochDay = DateUtils.todayEpochDay(timeZone = TimeZone.getTimeZone("UTC"), clock = fixedClock)
        assertEquals(LocalDate.of(2026, 11, 1).toEpochDay(), epochDay)

        // getCurrentHour and minute with clock
        assertEquals(14, DateUtils.getCurrentHour(fixedClock))
        assertEquals((14 * 60) + 30, DateUtils.getCurrentMinuteOfDay(fixedClock))

        // formatDisplayDate relative to injected clock
        assertEquals("Today, Nov 1", DateUtils.formatDisplayDate("2026-11-01", clock = fixedClock))
        assertEquals("Tomorrow, Nov 2", DateUtils.formatDisplayDate("2026-11-02", clock = fixedClock))
        assertEquals("Yesterday, Oct 31", DateUtils.formatDisplayDate("2026-10-31", clock = fixedClock))
        assertEquals("Fri, Oct 30", DateUtils.formatDisplayDate("2026-10-30", clock = fixedClock))

        // getLastNDays with clock
        val last3Days = DateUtils.getLastNDays(3, clock = fixedClock)
        assertEquals(listOf("2026-10-30", "2026-10-31", "2026-11-01"), last3Days)
    }

    @Test
    fun testIsoDateSerializationCompliance() {
        val regex = Regex("^\\d{4}-\\d{2}-\\d{2}$")

        for (epoch in 0L..25000L step 37L) {
            val iso = DateUtils.epochDayToIsoString(epoch)
            assertTrue("ISO format check failed for epoch $epoch: $iso", regex.matches(iso))
            val roundTrip = DateUtils.isoStringToEpochDay(iso)
            assertEquals("Round trip failed for epoch $epoch", epoch, roundTrip)
        }
    }
}
