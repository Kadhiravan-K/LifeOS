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
     * Incorporates end dates, paused status, skipped dates, and rescheduled dates.
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
        if (task.recurrenceEndDateIso != null && targetDateIso > task.recurrenceEndDateIso) {
            return false
        }

        val daysDiff = DateUtils.daysBetween(task.dueDateIso, targetDateIso)
        if (daysDiff < 0) {
            // Task has not started yet
            return false
        }

        return when (task.recurrenceType) {
            RecurrenceType.NONE -> {
                // One-time task scheduled on due date, or overdue on today if not completed
                if (task.dueDateIso == targetDateIso) {
                    true
                } else if (targetDateIso == DateUtils.todayString() && !task.isOneTimeCompleted) {
                    // Overdue tasks show on today
                    true
                } else {
                    false
                }
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

            RecurrenceType.CUSTOM -> {
                val interval = if (task.recurrenceInterval > 0) task.recurrenceInterval else 1
                if (task.recurrenceDaysOfWeek.isNotEmpty()) {
                    val targetDayOfWeek = DateUtils.getDayOfWeek(targetDateIso)
                    task.recurrenceDaysOfWeek.contains(targetDayOfWeek) && (daysDiff % interval == 0)
                } else {
                    daysDiff % interval == 0
                }
            }
        }
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
        for (i in 0..730) { // Check up to 2 years ahead
            val date = DateUtils.addDays(fromDateIso, i)
            if (isTaskScheduledOnDate(task, date)) {
                return date
            }
        }
        return null
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
        return try {
            val dStart = isoFormat.parse(startDateIso) ?: Date()
            val dTarget = isoFormat.parse(targetDateIso) ?: Date()
            val calStart = Calendar.getInstance().apply { time = dStart }
            val calTarget = Calendar.getInstance().apply { time = dTarget }

            if (calTarget.get(Calendar.YEAR) < calStart.get(Calendar.YEAR)) {
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
