package com.example.domain.recurrence

import com.example.domain.model.RecurrenceType
import com.example.domain.model.TaskItem
import com.example.utils.DateUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object RecurrenceEngine {

    private val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /**
     * Checks if a task is scheduled to appear on a specific target date.
     * Incorporates end dates, end conditions (ON_DATE, AFTER_COUNT), paused status, skipped dates, and rescheduled dates.
     */
    fun isTaskScheduledOnDate(task: TaskItem, targetDateIso: String): Boolean {
        // 1. Paused recurring tasks never generate occurrences
        if (task.isPaused && task.recurrenceType != RecurrenceType.NONE) {
            return false
        }

        // 2. If this occurrence was specifically rescheduled TO this date
        if (task.rescheduledDates.containsValue(targetDateIso)) {
            return true
        }

        // 3. If the occurrence for this date was moved to another date
        if (task.rescheduledDates.containsKey(targetDateIso)) {
            return false
        }

        // 4. If this date was specifically skipped
        if (task.skippedDates.contains(targetDateIso)) {
            return false
        }

        // 5. If target date is past the optional recurrence end date
        if (task.recurrenceEndCondition == "ON_DATE" && !task.recurrenceEndDateIso.isNullOrBlank()) {
            if (targetDateIso > task.recurrenceEndDateIso) {
                return false
            }
        }

        val startIso = if (task.dueDateIso.isNotBlank()) task.dueDateIso else DateUtils.todayString()
        val daysDiff = DateUtils.daysBetween(startIso, targetDateIso)
        if (daysDiff < 0) {
            // Task has not started yet
            return false
        }

        // Check base pattern match
        if (!matchesBasePattern(task, targetDateIso, daysDiff)) {
            return false
        }

        // 6. If recurrence end condition is AFTER_COUNT, check if within limit
        if (task.recurrenceType != RecurrenceType.NONE &&
            task.recurrenceEndCondition == "AFTER_COUNT" &&
            task.recurrenceCount > 0
        ) {
            val count = countOccurrencesUpTo(task, targetDateIso)
            if (count > task.recurrenceCount) {
                return false
            }
        }

        return true
    }

    private fun matchesBasePattern(task: TaskItem, targetDateIso: String, daysDiff: Int = DateUtils.daysBetween(task.dueDateIso, targetDateIso)): Boolean {
        if (daysDiff < 0) return false

        return when (task.recurrenceType) {
            RecurrenceType.NONE -> {
                task.dueDateIso == targetDateIso
            }

            RecurrenceType.DAILY -> true

            RecurrenceType.EVERY_N_DAYS -> {
                val interval = if (task.recurrenceInterval > 0) task.recurrenceInterval else 1
                daysDiff % interval == 0
            }

            RecurrenceType.WEEKDAYS -> {
                val dayOfWeek = DateUtils.getDayOfWeek(targetDateIso)
                dayOfWeek != "SATURDAY" && dayOfWeek != "SUNDAY"
            }

            RecurrenceType.SELECTED_WEEKDAYS -> {
                val targetDayOfWeek = DateUtils.getDayOfWeek(targetDateIso)
                if (task.recurrenceDaysOfWeek.isNotEmpty()) {
                    task.recurrenceDaysOfWeek.contains(targetDayOfWeek)
                } else {
                    DateUtils.getDayOfWeek(task.dueDateIso) == targetDayOfWeek
                }
            }

            RecurrenceType.WEEKLY -> {
                val targetDayOfWeek = DateUtils.getDayOfWeek(targetDateIso)
                if (task.recurrenceDaysOfWeek.isNotEmpty()) {
                    task.recurrenceDaysOfWeek.contains(targetDayOfWeek)
                } else {
                    DateUtils.getDayOfWeek(task.dueDateIso) == targetDayOfWeek
                }
            }

            RecurrenceType.EVERY_N_WEEKS -> {
                val interval = if (task.recurrenceInterval > 0) task.recurrenceInterval else 1
                val targetDayOfWeek = DateUtils.getDayOfWeek(targetDateIso)
                val dayMatches = if (task.recurrenceDaysOfWeek.isNotEmpty()) {
                    task.recurrenceDaysOfWeek.contains(targetDayOfWeek)
                } else {
                    DateUtils.getDayOfWeek(task.dueDateIso) == targetDayOfWeek
                }
                if (!dayMatches) return false

                val weeksDiff = daysDiff / 7
                weeksDiff % interval == 0
            }

            RecurrenceType.MONTHLY -> {
                matchesMonthDay(task.dueDateIso, targetDateIso, intervalMonths = 1)
            }

            RecurrenceType.EVERY_N_MONTHS -> {
                val interval = if (task.recurrenceInterval > 0) task.recurrenceInterval else 1
                matchesMonthDay(task.dueDateIso, targetDateIso, intervalMonths = interval)
            }

            RecurrenceType.EVERY_6_MONTHS -> {
                matchesMonthDay(task.dueDateIso, targetDateIso, intervalMonths = 6)
            }

            RecurrenceType.YEARLY -> {
                matchesYearly(task.dueDateIso, targetDateIso)
            }

            RecurrenceType.EVERY_N_YEARS -> {
                val interval = if (task.recurrenceInterval > 0) task.recurrenceInterval else 1
                matchesYearlyInterval(task.dueDateIso, targetDateIso, intervalYears = interval)
            }

            RecurrenceType.CUSTOM -> {
                val interval = if (task.recurrenceInterval > 0) task.recurrenceInterval else 1
                when (task.recurrenceUnit.uppercase()) {
                    "WEEKS" -> {
                        val targetDayOfWeek = DateUtils.getDayOfWeek(targetDateIso)
                        val dayMatches = if (task.recurrenceDaysOfWeek.isNotEmpty()) {
                            task.recurrenceDaysOfWeek.contains(targetDayOfWeek)
                        } else {
                            DateUtils.getDayOfWeek(task.dueDateIso) == targetDayOfWeek
                        }
                        if (!dayMatches) return false
                        val weeksDiff = daysDiff / 7
                        weeksDiff % interval == 0
                    }
                    "MONTHS" -> matchesMonthDay(task.dueDateIso, targetDateIso, intervalMonths = interval)
                    "YEARS" -> matchesYearlyInterval(task.dueDateIso, targetDateIso, intervalYears = interval)
                    else -> daysDiff % interval == 0 // DAYS
                }
            }
        }
    }

    /**
     * Counts how many occurrences match between task.dueDateIso and targetDateIso.
     */
    private fun countOccurrencesUpTo(task: TaskItem, targetDateIso: String): Int {
        var count = 0
        val startIso = if (task.dueDateIso.isNotBlank()) task.dueDateIso else DateUtils.todayString()
        val maxDays = minOf(1460, DateUtils.daysBetween(startIso, targetDateIso))
        for (i in 0..maxDays) {
            val date = DateUtils.addDays(startIso, i)
            if (matchesBasePattern(task, date, i)) {
                count++
                if (date == targetDateIso) return count
                if (count > task.recurrenceCount) return count
            }
        }
        return count
    }

    /**
     * Determines whether the occurrence on targetDateIso has been completed.
     * Completing today's occurrence never completes future occurrences.
     */
    fun isCompletedOnDate(task: TaskItem, targetDateIso: String): Boolean {
        return if (task.recurrenceType == RecurrenceType.NONE) {
            task.isOneTimeCompleted || task.completedDates.contains(targetDateIso)
        } else {
            task.completedDates.contains(targetDateIso)
        }
    }

    /**
     * Finds next upcoming occurrence date from referenceDateIso.
     */
    fun getNextOccurrenceDate(task: TaskItem, fromDateIso: String = DateUtils.todayString()): String? {
        val nexts = getNextNOccurrences(task, count = 1, fromDateIso = fromDateIso)
        return nexts.firstOrNull()
    }

    /**
     * Returns up to [count] scheduled upcoming occurrence dates.
     */
    fun getNextNOccurrences(task: TaskItem, count: Int = 3, fromDateIso: String = DateUtils.todayString()): List<String> {
        if (task.recurrenceType == RecurrenceType.NONE) return emptyList()
        val results = mutableListOf<String>()
        val start = if (fromDateIso < task.dueDateIso) task.dueDateIso else fromDateIso
        for (i in 0..730) { // Check up to 2 years ahead
            val date = DateUtils.addDays(start, i)
            if (isTaskScheduledOnDate(task, date)) {
                results.add(date)
                if (results.size >= count) break
            }
        }
        return results
    }

    /**
     * Formats a comprehensive human-readable preview of the recurrence rules.
     */
    fun formatRecurrencePreview(task: TaskItem): String {
        val base = when (task.recurrenceType) {
            RecurrenceType.NONE -> "Does not repeat"
            RecurrenceType.DAILY -> "Every day"
            RecurrenceType.EVERY_N_DAYS -> if (task.recurrenceInterval <= 1) "Every day" else "Every ${task.recurrenceInterval} days"
            RecurrenceType.WEEKDAYS -> "Every weekday (Mon–Fri)"
            RecurrenceType.SELECTED_WEEKDAYS -> {
                val days = if (task.recurrenceDaysOfWeek.isEmpty()) "selected days" else task.recurrenceDaysOfWeek.joinToString(", ") { formatDayName(it) }
                "Every week on $days"
            }
            RecurrenceType.WEEKLY -> {
                val dayName = formatDayName(DateUtils.getDayOfWeek(task.dueDateIso))
                "Every week on $dayName"
            }
            RecurrenceType.EVERY_N_WEEKS -> {
                val days = if (task.recurrenceDaysOfWeek.isEmpty()) formatDayName(DateUtils.getDayOfWeek(task.dueDateIso)) else task.recurrenceDaysOfWeek.joinToString(", ") { formatDayName(it) }
                if (task.recurrenceInterval <= 1) "Every week on $days" else "Every ${task.recurrenceInterval} weeks on $days"
            }
            RecurrenceType.MONTHLY -> {
                val dayOfMonth = DateUtils.getDayOfMonth(task.dueDateIso)
                "Every month on the ${formatOrdinal(dayOfMonth)}"
            }
            RecurrenceType.EVERY_N_MONTHS -> {
                val dayOfMonth = DateUtils.getDayOfMonth(task.dueDateIso)
                if (task.recurrenceInterval <= 1) "Every month on the ${formatOrdinal(dayOfMonth)}" else "Every ${task.recurrenceInterval} months on the ${formatOrdinal(dayOfMonth)}"
            }
            RecurrenceType.EVERY_6_MONTHS -> {
                val dayOfMonth = DateUtils.getDayOfMonth(task.dueDateIso)
                "Every 6 months on the ${formatOrdinal(dayOfMonth)}"
            }
            RecurrenceType.YEARLY -> {
                val d = DateUtils.formatDisplayDate(task.dueDateIso)
                "Every year on $d"
            }
            RecurrenceType.EVERY_N_YEARS -> {
                val d = DateUtils.formatDisplayDate(task.dueDateIso)
                if (task.recurrenceInterval <= 1) "Every year on $d" else "Every ${task.recurrenceInterval} years on $d"
            }
            RecurrenceType.CUSTOM -> {
                val unit = task.recurrenceUnit.lowercase()
                val interval = if (task.recurrenceInterval > 0) task.recurrenceInterval else 1
                val intervalStr = if (interval <= 1) "Every $unit" else "Every $interval $unit"
                if (task.recurrenceUnit.equals("WEEKS", ignoreCase = true) && task.recurrenceDaysOfWeek.isNotEmpty()) {
                    val days = task.recurrenceDaysOfWeek.joinToString(", ") { formatDayName(it) }
                    "$intervalStr on $days"
                } else {
                    intervalStr
                }
            }
        }

        val end = when (task.recurrenceEndCondition) {
            "ON_DATE" -> if (task.recurrenceEndDateIso != null) " • Until ${DateUtils.formatDisplayDate(task.recurrenceEndDateIso)}" else ""
            "AFTER_COUNT" -> " • Ends after ${task.recurrenceCount} occurrences"
            else -> ""
        }

        return base + end
    }

    private fun formatDayName(day: String): String {
        return when (day.uppercase()) {
            "MONDAY" -> "Mon"
            "TUESDAY" -> "Tue"
            "WEDNESDAY" -> "Wed"
            "THURSDAY" -> "Thu"
            "FRIDAY" -> "Fri"
            "SATURDAY" -> "Sat"
            "SUNDAY" -> "Sun"
            else -> day
        }
    }

    private fun formatOrdinal(n: Int): String {
        val suffix = when {
            n in 11..13 -> "th"
            n % 10 == 1 -> "st"
            n % 10 == 2 -> "nd"
            n % 10 == 3 -> "rd"
            else -> "th"
        }
        return "$n$suffix"
    }

    private fun matchesMonthDay(startDateIso: String, targetDateIso: String, intervalMonths: Int): Boolean {
        return try {
            val dStart = isoFormat.parse(startDateIso) ?: Date()
            val dTarget = isoFormat.parse(targetDateIso) ?: Date()
            val calStart = Calendar.getInstance().apply { time = dStart }
            val calTarget = Calendar.getInstance().apply { time = dTarget }

            val yearDiff = calTarget.get(Calendar.YEAR) - calStart.get(Calendar.YEAR)
            val monthDiff = (yearDiff * 12) + (calTarget.get(Calendar.MONTH) - calStart.get(Calendar.MONTH))

            if (monthDiff < 0 || monthDiff % intervalMonths != 0) {
                return false
            }

            val startDay = calStart.get(Calendar.DAY_OF_MONTH)
            val targetDay = calTarget.get(Calendar.DAY_OF_MONTH)
            val maxDaysInTargetMonth = calTarget.getActualMaximum(Calendar.DAY_OF_MONTH)

            // If startDay > maxDaysInTargetMonth (e.g. Jan 31 in Feb), fires on last day of Feb (28/29)
            if (startDay > maxDaysInTargetMonth) {
                targetDay == maxDaysInTargetMonth
            } else {
                targetDay == startDay
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun matchesYearly(startDateIso: String, targetDateIso: String): Boolean {
        return matchesYearlyInterval(startDateIso, targetDateIso, intervalYears = 1)
    }

    private fun matchesYearlyInterval(startDateIso: String, targetDateIso: String, intervalYears: Int): Boolean {
        return try {
            val dStart = isoFormat.parse(startDateIso) ?: Date()
            val dTarget = isoFormat.parse(targetDateIso) ?: Date()
            val calStart = Calendar.getInstance().apply { time = dStart }
            val calTarget = Calendar.getInstance().apply { time = dTarget }

            val yearDiff = calTarget.get(Calendar.YEAR) - calStart.get(Calendar.YEAR)
            if (yearDiff < 0 || yearDiff % intervalYears != 0) {
                return false
            }

            val startMonth = calStart.get(Calendar.MONTH)
            val targetMonth = calTarget.get(Calendar.MONTH)
            if (startMonth != targetMonth) return false

            val startDay = calStart.get(Calendar.DAY_OF_MONTH)
            val targetDay = calTarget.get(Calendar.DAY_OF_MONTH)

            // Handle Leap Year edge case: Feb 29 on non-leap years
            if (startMonth == Calendar.FEBRUARY && startDay == 29) {
                val targetYear = calTarget.get(Calendar.YEAR)
                if (!DateUtils.isLeapYear(targetYear)) {
                    // Matches Feb 28 on non-leap year
                    return targetDay == 28
                }
            }

            targetDay == startDay
        } catch (e: Exception) {
            false
        }
    }
}
