package com.example.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Robust date and calendar utilities compatible with all Android API levels.
 */
object DateUtils {
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val displayFormat = SimpleDateFormat("EEE, MMM d", Locale.US)
    private val fullDisplayFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US)
    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.US)

    fun todayString(): String {
        return isoFormat.format(Date())
    }

    fun currentTimeString(): String {
        return minutesToTime(getCurrentMinuteOfDay())
    }

    fun todayEpochDay(): Long {
        return System.currentTimeMillis() / (1000L * 60 * 60 * 24)
    }

    fun epochDayToIsoString(epochDay: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = epochDay * (1000L * 60 * 60 * 24)
        return isoFormat.format(calendar.time)
    }

    fun isoStringToEpochDay(isoDate: String): Long {
        return try {
            val date = isoFormat.parse(isoDate) ?: Date()
            date.time / (1000L * 60 * 60 * 24)
        } catch (e: Exception) {
            todayEpochDay()
        }
    }

    fun formatDisplayDate(isoDate: String): String {
        return try {
            val date = isoFormat.parse(isoDate) ?: Date()
            val todayStr = todayString()
            if (isoDate == todayStr) return "Today, " + SimpleDateFormat("MMM d", Locale.US).format(date)

            val calToday = Calendar.getInstance()
            calToday.add(Calendar.DAY_OF_YEAR, 1)
            if (isoDate == isoFormat.format(calToday.time)) return "Tomorrow, " + SimpleDateFormat("MMM d", Locale.US).format(date)

            calToday.add(Calendar.DAY_OF_YEAR, -2)
            if (isoDate == isoFormat.format(calToday.time)) return "Yesterday, " + SimpleDateFormat("MMM d", Locale.US).format(date)

            displayFormat.format(date)
        } catch (e: Exception) {
            isoDate
        }
    }

    fun formatMonthYear(year: Int, monthZeroIndexed: Int): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroIndexed)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return monthYearFormat.format(cal.time)
    }

    fun getDayOfWeek(isoDate: String): String {
        return try {
            val date = isoFormat.parse(isoDate) ?: Date()
            val cal = Calendar.getInstance()
            cal.time = date
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
            val date = isoFormat.parse(isoDate) ?: Date()
            val cal = Calendar.getInstance()
            cal.time = date
            cal.add(Calendar.DAY_OF_YEAR, daysToAdd)
            isoFormat.format(cal.time)
        } catch (e: Exception) {
            isoDate
        }
    }

    fun daysBetween(startDateIso: String, endDateIso: String): Int {
        return try {
            val d1 = isoFormat.parse(startDateIso) ?: Date()
            val d2 = isoFormat.parse(endDateIso) ?: Date()
            val diff = d2.time - d1.time
            (diff / (1000L * 60 * 60 * 24)).toInt()
        } catch (e: Exception) {
            0
        }
    }

    fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }

    fun getYear(isoDate: String): Int {
        return try {
            val date = isoFormat.parse(isoDate) ?: Date()
            val cal = Calendar.getInstance().apply { time = date }
            cal.get(Calendar.YEAR)
        } catch (e: Exception) {
            2026
        }
    }

    fun getMonthZeroIndexed(isoDate: String): Int {
        return try {
            val date = isoFormat.parse(isoDate) ?: Date()
            val cal = Calendar.getInstance().apply { time = date }
            cal.get(Calendar.MONTH)
        } catch (e: Exception) {
            0
        }
    }

    fun getDayOfMonth(isoDate: String): Int {
        return try {
            val date = isoFormat.parse(isoDate) ?: Date()
            val cal = Calendar.getInstance().apply { time = date }
            cal.get(Calendar.DAY_OF_MONTH)
        } catch (e: Exception) {
            1
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

    /**
     * Generates a complete 35-42 day calendar grid for a given month and year.
     * Takes firstDayOfWeek ("MONDAY" or "SUNDAY") into account.
     */
    fun getMonthCalendarGrid(year: Int, monthZeroIndexed: Int, firstDayOfWeek: String = "MONDAY"): List<String> {
        val daysInMonth = getDaysInMonth(year, monthZeroIndexed)
        val firstDayCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroIndexed)
            set(Calendar.DAY_OF_MONTH, 1)
        }

        val firstDayOfWeekCal = firstDayCal.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon...
        val offset = if (firstDayOfWeek == "SUNDAY") {
            firstDayOfWeekCal - Calendar.SUNDAY
        } else {
            // Monday start: Sun=6, Mon=0, Tue=1...
            if (firstDayOfWeekCal == Calendar.SUNDAY) 6 else firstDayOfWeekCal - Calendar.MONDAY
        }

        val grid = mutableListOf<String>()
        // Backfill previous month
        for (i in offset downTo 1) {
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, monthZeroIndexed)
                set(Calendar.DAY_OF_MONTH, 1)
                add(Calendar.DAY_OF_YEAR, -i)
            }
            grid.add(isoFormat.format(cal.time))
        }

        // Current month
        for (d in 1..daysInMonth) {
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, monthZeroIndexed)
                set(Calendar.DAY_OF_MONTH, d)
            }
            grid.add(isoFormat.format(cal.time))
        }

        // Fill remaining days to complete 5 or 6 rows of 7
        val remaining = (7 - (grid.size % 7)) % 7
        for (i in 1..remaining) {
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, monthZeroIndexed)
                set(Calendar.DAY_OF_MONTH, daysInMonth)
                add(Calendar.DAY_OF_YEAR, i)
            }
            grid.add(isoFormat.format(cal.time))
        }

        return grid
    }

    /**
     * Returns the 7 days of the week containing referenceDateIso.
     */
    fun getWeekDays(referenceDateIso: String, firstDayOfWeek: String = "MONDAY"): List<String> {
        val date = try { isoFormat.parse(referenceDateIso) ?: Date() } catch (e: Exception) { Date() }
        val cal = Calendar.getInstance().apply { time = date }
        val currentDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)

        val daysBeforeStart = if (firstDayOfWeek == "SUNDAY") {
            currentDayOfWeek - Calendar.SUNDAY
        } else {
            if (currentDayOfWeek == Calendar.SUNDAY) 6 else currentDayOfWeek - Calendar.MONDAY
        }

        cal.add(Calendar.DAY_OF_YEAR, -daysBeforeStart)
        val weekList = mutableListOf<String>()
        for (i in 0..6) {
            weekList.add(isoFormat.format(cal.time))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return weekList
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

    fun getCurrentHour(): Int {
        return Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    }

    fun getCurrentMinuteOfDay(): Int {
        val cal = Calendar.getInstance()
        return (cal.get(Calendar.HOUR_OF_DAY) * 60) + cal.get(Calendar.MINUTE)
    }

    fun getLastNDays(n: Int): List<String> {
        val list = mutableListOf<String>()
        val cal = Calendar.getInstance()
        for (i in 0 until n) {
            list.add(0, isoFormat.format(cal.time))
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return list
    }
}
