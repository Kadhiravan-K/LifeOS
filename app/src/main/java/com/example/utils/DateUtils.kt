package com.example.utils

import android.util.Log
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.TimeZone

/**
 * Robust, thread-safe date and calendar utilities powered by java.time (DT-01, DT-02).
 *
 * Guarantees:
 * 1. 100% lock-free thread safety using immutable java.time objects.
 * 2. Complete parity with existing ISO-8601 ("yyyy-MM-dd") formats.
 * 3. Machine-timezone-independent weekday calculations for ISO dates.
 * 4. Optional Clock injection for deterministic, clock-controlled testing.
 * 5. Locale-configurable display formatting (defaulting to Locale.US).
 * 6. Zero SimpleDateFormat, Calendar, or java.util.Date usage.
 */
object DateUtils {

    private const val TAG = "DateUtils"

    private fun logWarning(msg: String) {
        try {
            Log.w(TAG, msg)
        } catch (_: Throwable) {
            // Un-mocked Log in local host JVM tests
        }
    }

    private val ISO_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
    private val DISPLAY_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US)
    private val FULL_DISPLAY_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.US)
    private val MONTH_YEAR_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)
    private val MONTH_DAY_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.US)

    private fun getDisplayFormatter(locale: Locale): DateTimeFormatter =
        if (locale == Locale.US) DISPLAY_FORMATTER else DateTimeFormatter.ofPattern("EEE, MMM d", locale)

    private fun getFullDisplayFormatter(locale: Locale): DateTimeFormatter =
        if (locale == Locale.US) FULL_DISPLAY_FORMATTER else DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", locale)

    private fun getMonthYearFormatter(locale: Locale): DateTimeFormatter =
        if (locale == Locale.US) MONTH_YEAR_FORMATTER else DateTimeFormatter.ofPattern("MMMM yyyy", locale)

    private fun getMonthDayFormatter(locale: Locale): DateTimeFormatter =
        if (locale == Locale.US) MONTH_DAY_FORMATTER else DateTimeFormatter.ofPattern("MMM d", locale)

    fun todayString(
        timeZone: TimeZone = TimeZone.getDefault(),
        nowMillis: Long? = null,
        clock: Clock = Clock.systemDefaultZone()
    ): String {
        val zoneId = timeZone.toZoneId()
        val localDate = if (nowMillis != null) {
            Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        } else {
            LocalDate.now(clock.withZone(zoneId))
        }
        return localDate.format(ISO_FORMATTER)
    }

    fun currentTimeString(clock: Clock = Clock.systemDefaultZone()): String {
        return minutesToTime(getCurrentMinuteOfDay(clock))
    }

    fun todayEpochDay(
        timeZone: TimeZone = TimeZone.getDefault(),
        nowMillis: Long? = null,
        clock: Clock = Clock.systemDefaultZone()
    ): Long {
        val zoneId = timeZone.toZoneId()
        val localDate = if (nowMillis != null) {
            Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        } else {
            LocalDate.now(clock.withZone(zoneId))
        }
        return localDate.toEpochDay()
    }

    fun epochDayToIsoString(epochDay: Long): String {
        return LocalDate.ofEpochDay(epochDay).format(ISO_FORMATTER)
    }

    fun isoStringToEpochDay(isoDate: String, clock: Clock = Clock.systemDefaultZone()): Long {
        return try {
            LocalDate.parse(isoDate.trim(), ISO_FORMATTER).toEpochDay()
        } catch (e: Exception) {
            logWarning("Failed to parse ISO date string in isoStringToEpochDay, falling back to current epoch day")
            Math.floorDiv(clock.millis(), 1000L * 60 * 60 * 24)
        }
    }

    fun formatDisplayDate(
        isoDate: String,
        displayLocale: Locale = Locale.US,
        clock: Clock = Clock.systemDefaultZone()
    ): String {
        return try {
            val date = LocalDate.parse(isoDate.trim(), ISO_FORMATTER)
            val today = LocalDate.now(clock)
            val monthDayFormatter = getMonthDayFormatter(displayLocale)
            val displayFormatter = getDisplayFormatter(displayLocale)

            when (date) {
                today -> "Today, " + date.format(monthDayFormatter)
                today.plusDays(1) -> "Tomorrow, " + date.format(monthDayFormatter)
                today.minusDays(1) -> "Yesterday, " + date.format(monthDayFormatter)
                else -> date.format(displayFormatter)
            }
        } catch (e: Exception) {
            logWarning("Failed to parse ISO date in formatDisplayDate")
            isoDate
        }
    }

    fun formatFullDisplayDate(isoDate: String, displayLocale: Locale = Locale.US): String {
        return try {
            val date = LocalDate.parse(isoDate.trim(), ISO_FORMATTER)
            date.format(getFullDisplayFormatter(displayLocale))
        } catch (e: Exception) {
            logWarning("Failed to parse ISO date in formatFullDisplayDate")
            isoDate
        }
    }

    fun formatMonthYear(year: Int, monthZeroIndexed: Int, displayLocale: Locale = Locale.US): String {
        val date = LocalDate.of(year, monthZeroIndexed + 1, 1)
        return date.format(getMonthYearFormatter(displayLocale))
    }

    fun formatMonthYear(isoDate: String, displayLocale: Locale = Locale.US): String {
        return try {
            val date = LocalDate.parse(isoDate.trim(), ISO_FORMATTER)
            date.format(getMonthYearFormatter(displayLocale))
        } catch (e: Exception) {
            logWarning("Failed to parse ISO date in formatMonthYear")
            isoDate
        }
    }

    fun getDayOfWeek(isoDate: String): String {
        return try {
            LocalDate.parse(isoDate.trim(), ISO_FORMATTER).dayOfWeek.name
        } catch (e: Exception) {
            logWarning("Failed to parse ISO date in getDayOfWeek")
            "UNKNOWN"
        }
    }

    fun addDays(isoDate: String, daysToAdd: Int): String {
        return try {
            val date = LocalDate.parse(isoDate.trim(), ISO_FORMATTER)
            date.plusDays(daysToAdd.toLong()).format(ISO_FORMATTER)
        } catch (e: Exception) {
            logWarning("Failed to parse ISO date in addDays")
            isoDate
        }
    }

    fun addMonths(isoDate: String, monthsToAdd: Int): String {
        return try {
            val date = LocalDate.parse(isoDate.trim(), ISO_FORMATTER)
            date.plusMonths(monthsToAdd.toLong()).format(ISO_FORMATTER)
        } catch (e: Exception) {
            logWarning("Failed to parse ISO date in addMonths")
            isoDate
        }
    }

    fun addYears(isoDate: String, yearsToAdd: Int): String {
        return try {
            val date = LocalDate.parse(isoDate.trim(), ISO_FORMATTER)
            date.plusYears(yearsToAdd.toLong()).format(ISO_FORMATTER)
        } catch (e: Exception) {
            logWarning("Failed to parse ISO date in addYears")
            isoDate
        }
    }

    fun daysBetween(startDateIso: String, endDateIso: String): Int {
        return try {
            val start = LocalDate.parse(startDateIso.trim(), ISO_FORMATTER)
            val end = LocalDate.parse(endDateIso.trim(), ISO_FORMATTER)
            ChronoUnit.DAYS.between(start, end).toInt()
        } catch (e: Exception) {
            logWarning("Failed to parse ISO dates in daysBetween")
            0
        }
    }

    fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }

    fun getYear(isoDate: String): Int {
        return try {
            LocalDate.parse(isoDate.trim(), ISO_FORMATTER).year
        } catch (e: Exception) {
            val parts = isoDate.trim().split("-")
            if (parts.isNotEmpty()) parts[0].toIntOrNull() ?: 2026 else 2026
        }
    }

    fun getMonthZeroIndexed(isoDate: String): Int {
        return try {
            LocalDate.parse(isoDate.trim(), ISO_FORMATTER).monthValue - 1
        } catch (e: Exception) {
            val parts = isoDate.trim().split("-")
            if (parts.size >= 2) (parts[1].toIntOrNull() ?: 1) - 1 else 0
        }
    }

    fun getDayOfMonth(isoDate: String): Int {
        return try {
            LocalDate.parse(isoDate.trim(), ISO_FORMATTER).dayOfMonth
        } catch (e: Exception) {
            val parts = isoDate.trim().split("-")
            if (parts.size >= 3) parts[2].toIntOrNull() ?: 1 else 1
        }
    }

    fun getDaysInMonth(year: Int, monthZeroIndexed: Int): Int {
        return YearMonth.of(year, monthZeroIndexed + 1).lengthOfMonth()
    }

    /**
     * Generates a complete 35-42 day calendar grid for a given month and year.
     * Takes firstDayOfWeek ("MONDAY", "SUNDAY", or "SATURDAY") into account.
     */
    fun getMonthCalendarGrid(year: Int, monthZeroIndexed: Int, firstDayOfWeek: String = "MONDAY"): List<String> {
        val firstDate = LocalDate.of(year, monthZeroIndexed + 1, 1)
        val daysInMonth = firstDate.lengthOfMonth()
        val dow = firstDate.dayOfWeek // MONDAY=1 ... SUNDAY=7

        val offset = when (firstDayOfWeek.uppercase()) {
            "SUNDAY" -> dow.value % 7 // SUNDAY(7) -> 0, MONDAY(1) -> 1, SATURDAY(6) -> 6
            "SATURDAY" -> (dow.value + 1) % 7 // SATURDAY(6) -> 0, SUNDAY(7) -> 1, MONDAY(1) -> 2
            else -> dow.value - 1 // MONDAY(1) -> 0, TUESDAY(2) -> 1, SUNDAY(7) -> 6
        }

        val startDate = firstDate.minusDays(offset.toLong())
        val remaining = (7 - ((offset + daysInMonth) % 7)) % 7
        val totalCells = offset + daysInMonth + remaining

        return (0 until totalCells).map { i ->
            startDate.plusDays(i.toLong()).format(ISO_FORMATTER)
        }
    }

    /**
     * Returns the 7 days of the week containing referenceDateIso.
     */
    fun getWeekDays(referenceDateIso: String, firstDayOfWeek: String = "MONDAY"): List<String> {
        val refDate = try {
            LocalDate.parse(referenceDateIso.trim(), ISO_FORMATTER)
        } catch (e: Exception) {
            logWarning("Failed to parse referenceDateIso in getWeekDays")
            LocalDate.now()
        }
        val dow = refDate.dayOfWeek
        val daysBeforeStart = when (firstDayOfWeek.uppercase()) {
            "SUNDAY" -> dow.value % 7
            "SATURDAY" -> (dow.value + 1) % 7
            else -> dow.value - 1
        }
        val startOfWeek = refDate.minusDays(daysBeforeStart.toLong())
        return (0L..6L).map { startOfWeek.plusDays(it).format(ISO_FORMATTER) }
    }

    fun timeToMinutes(timeStr: String?): Int {
        if (timeStr.isNullOrBlank()) return -1
        return try {
            val parts = timeStr.trim().split(":")
            val h = parts[0].toInt()
            val m = if (parts.size > 1) parts[1].toInt() else 0
            (h * 60) + m
        } catch (e: Exception) {
            -1
        }
    }

    fun minutesToTime(minutes: Int): String {
        if (minutes < 0) return ""
        val normalized = minutes % (24 * 60)
        val h = normalized / 60
        val m = normalized % 60
        return String.format(Locale.US, "%02d:%02d", h, m)
    }

    fun formatTime(timeStr: String?, is24Hour: Boolean = false): String {
        if (timeStr.isNullOrBlank()) return ""
        val mins = timeToMinutes(timeStr)
        if (mins < 0) return timeStr
        if (is24Hour) {
            return minutesToTime(mins)
        }
        val h = mins / 60
        val m = mins % 60
        val amPm = if (h < 12) "AM" else "PM"
        val h12 = if (h == 0) 12 else if (h > 12) h - 12 else h
        return String.format(Locale.US, "%d:%02d %s", h12, m, amPm)
    }

    fun doIntervalsOverlap(start1: Int, duration1: Int, start2: Int, duration2: Int): Boolean {
        if (start1 < 0 || start2 < 0) return false
        val end1 = start1 + duration1.coerceAtLeast(1)
        val end2 = start2 + duration2.coerceAtLeast(1)
        return start1 < end2 && start2 < end1
    }

    fun getGreeting(hour: Int): String {
        return when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Good night"
        }
    }

    fun getCurrentHour(clock: Clock = Clock.systemDefaultZone()): Int {
        return LocalTime.now(clock).hour
    }

    fun getCurrentMinuteOfDay(clock: Clock = Clock.systemDefaultZone()): Int {
        val now = LocalTime.now(clock)
        return (now.hour * 60) + now.minute
    }

    fun getLastNDays(n: Int, clock: Clock = Clock.systemDefaultZone()): List<String> {
        if (n <= 0) return emptyList()
        val today = LocalDate.now(clock)
        val start = today.minusDays((n - 1).toLong())
        return (0 until n).map { i ->
            start.plusDays(i.toLong()).format(ISO_FORMATTER)
        }
    }
}
