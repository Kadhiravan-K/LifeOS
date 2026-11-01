package com.example.domain.recurrence

import com.example.domain.model.RecurrenceType
import com.example.domain.model.TaskItem
import com.example.utils.DateUtils
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

object RecurrenceEngine {

    /**
     * Calculates the start-of-week date for a given [date] respecting [firstDayOfWeek]
     * ("MONDAY", "SUNDAY", or "SATURDAY"). Default is "MONDAY".
     */
    fun getStartOfWeek(date: LocalDate, firstDayOfWeek: String = "MONDAY"): LocalDate {
        val dow = date.dayOfWeek // MONDAY=1 ... SUNDAY=7
        val daysBeforeStart = when (firstDayOfWeek.uppercase()) {
            "SUNDAY" -> dow.value % 7 // Sun(7)->0, Mon(1)->1, ..., Sat(6)->6
            "SATURDAY" -> (dow.value + 1) % 7 // Sat(6)->0, Sun(7)->1, ..., Fri(5)->6
            else -> dow.value - 1 // Mon(1)->0, Tue(2)->1, ..., Sun(7)->6
        }
        return date.minusDays(daysBeforeStart.toLong())
    }

    private val ISO_FORMATTER: java.time.format.DateTimeFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd", java.util.Locale.US)

    private fun parseIsoDate(iso: String): LocalDate? {
        if (iso.isBlank()) return null
        return try {
            LocalDate.parse(iso.trim(), ISO_FORMATTER)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Checks if a task is scheduled to appear on a specific target date.
     * Incorporates end dates, end conditions (ON_DATE, AFTER_COUNT), paused status, skipped dates, and rescheduled dates.
     */
    fun isTaskScheduledOnDate(
        task: TaskItem,
        targetDateIso: String,
        firstDayOfWeek: String = "MONDAY"
    ): Boolean {
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
        if (!matchesBasePattern(task, targetDateIso, daysDiff, firstDayOfWeek)) {
            return false
        }

        // 6. If recurrence end condition is AFTER_COUNT, check if within limit
        if (task.recurrenceType != RecurrenceType.NONE &&
            task.recurrenceEndCondition == "AFTER_COUNT" &&
            task.recurrenceCount > 0
        ) {
            val count = countOccurrencesUpTo(task, targetDateIso, firstDayOfWeek)
            if (count > task.recurrenceCount) {
                return false
            }
        }

        return true
    }

    private fun matchesBasePattern(
        task: TaskItem,
        targetDateIso: String,
        daysDiff: Int = DateUtils.daysBetween(task.dueDateIso, targetDateIso),
        firstDayOfWeek: String = "MONDAY"
    ): Boolean {
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

                val dueLocalDate = parseIsoDate(task.dueDateIso) ?: return false
                val targetLocalDate = parseIsoDate(targetDateIso) ?: return false
                if (targetLocalDate.isBefore(dueLocalDate)) return false

                val startOfWeekDue = getStartOfWeek(dueLocalDate, firstDayOfWeek)
                val startOfWeekTarget = getStartOfWeek(targetLocalDate, firstDayOfWeek)
                val weekIndex = (startOfWeekTarget.toEpochDay() - startOfWeekDue.toEpochDay()) / 7
                if (weekIndex < 0L) return false

                weekIndex % interval == 0L
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

            RecurrenceType.MONTHLY_WINDOW -> {
                matchesMonthlyWindow(task, targetDateIso)
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

                        val dueLocalDate = parseIsoDate(task.dueDateIso) ?: return false
                        val targetLocalDate = parseIsoDate(targetDateIso) ?: return false
                        if (targetLocalDate.isBefore(dueLocalDate)) return false

                        val startOfWeekDue = getStartOfWeek(dueLocalDate, firstDayOfWeek)
                        val startOfWeekTarget = getStartOfWeek(targetLocalDate, firstDayOfWeek)
                        val weekIndex = (startOfWeekTarget.toEpochDay() - startOfWeekDue.toEpochDay()) / 7
                        if (weekIndex < 0L) return false

                        weekIndex % interval == 0L
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
     * Uses arithmetic calculation for O(1) performance across multi-year date ranges.
     */
    fun countOccurrencesUpTo(
        task: TaskItem,
        targetDateIso: String,
        firstDayOfWeek: String = "MONDAY"
    ): Int {
        val startIso = if (task.dueDateIso.isNotBlank()) task.dueDateIso else DateUtils.todayString()
        if (targetDateIso < startIso) return 0

        val startDate = parseIsoDate(startIso) ?: return 0
        val targetDate = parseIsoDate(targetDateIso) ?: return 0
        if (targetDate.isBefore(startDate)) return 0

        val daysDiff = ChronoUnit.DAYS.between(startDate, targetDate).toInt()

        return when (task.recurrenceType) {
            RecurrenceType.NONE -> if (targetDate == startDate) 1 else 0

            RecurrenceType.DAILY -> daysDiff + 1

            RecurrenceType.EVERY_N_DAYS -> {
                val interval = if (task.recurrenceInterval > 0) task.recurrenceInterval else 1
                (daysDiff / interval) + 1
            }

            RecurrenceType.WEEKDAYS -> {
                val fullWeeks = daysDiff / 7
                var count = fullWeeks * 5
                val remainderDays = daysDiff % 7
                val remainderStart = startDate.plusDays((fullWeeks * 7).toLong())
                for (d in 0..remainderDays) {
                    val dow = remainderStart.plusDays(d.toLong()).dayOfWeek
                    if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                        count++
                    }
                }
                count
            }

            RecurrenceType.WEEKLY,
            RecurrenceType.SELECTED_WEEKDAYS -> {
                val selectedDays = if (task.recurrenceDaysOfWeek.isNotEmpty()) {
                    task.recurrenceDaysOfWeek.map { it.uppercase() }.toSet()
                } else {
                    setOf(startDate.dayOfWeek.name)
                }
                val fullWeeks = daysDiff / 7
                var count = fullWeeks * selectedDays.size
                val remainderDays = daysDiff % 7
                val remainderStart = startDate.plusDays((fullWeeks * 7).toLong())
                for (d in 0..remainderDays) {
                    val dowName = remainderStart.plusDays(d.toLong()).dayOfWeek.name
                    if (selectedDays.contains(dowName)) {
                        count++
                    }
                }
                count
            }

            RecurrenceType.EVERY_N_WEEKS -> {
                val interval = if (task.recurrenceInterval > 0) task.recurrenceInterval else 1
                countWeeklyOccurrences(task, startDate, targetDate, interval, firstDayOfWeek)
            }

            RecurrenceType.CUSTOM -> {
                val interval = if (task.recurrenceInterval > 0) task.recurrenceInterval else 1
                when (task.recurrenceUnit.uppercase()) {
                    "WEEKS" -> countWeeklyOccurrences(task, startDate, targetDate, interval, firstDayOfWeek)
                    "MONTHS" -> countMonthlyOccurrences(startDate, targetDate, interval)
                    "YEARS" -> countYearlyOccurrences(startDate, targetDate, interval)
                    else -> (daysDiff / interval) + 1 // DAYS
                }
            }

            RecurrenceType.MONTHLY -> countMonthlyOccurrences(startDate, targetDate, 1)
            RecurrenceType.EVERY_N_MONTHS -> {
                val interval = if (task.recurrenceInterval > 0) task.recurrenceInterval else 1
                countMonthlyOccurrences(startDate, targetDate, interval)
            }
            RecurrenceType.EVERY_6_MONTHS -> countMonthlyOccurrences(startDate, targetDate, 6)

            RecurrenceType.YEARLY -> countYearlyOccurrences(startDate, targetDate, 1)
            RecurrenceType.EVERY_N_YEARS -> {
                val interval = if (task.recurrenceInterval > 0) task.recurrenceInterval else 1
                countYearlyOccurrences(startDate, targetDate, interval)
            }

            RecurrenceType.MONTHLY_WINDOW -> {
                countMonthlyWindowOccurrences(task, startDate, targetDate)
            }
        }
    }

    private fun countWeeklyOccurrences(
        task: TaskItem,
        startDate: LocalDate,
        targetDate: LocalDate,
        interval: Int,
        firstDayOfWeek: String
    ): Int {
        val selectedDays = if (task.recurrenceDaysOfWeek.isNotEmpty()) {
            task.recurrenceDaysOfWeek.map { it.uppercase() }.toSet()
        } else {
            setOf(startDate.dayOfWeek.name)
        }
        val startOfWeekDue = getStartOfWeek(startDate, firstDayOfWeek)
        val startOfWeekTarget = getStartOfWeek(targetDate, firstDayOfWeek)
        val weekIndex = (startOfWeekTarget.toEpochDay() - startOfWeekDue.toEpochDay()) / 7

        if (weekIndex == 0L) {
            var count = 0
            var curr = startDate
            while (!curr.isAfter(targetDate)) {
                if (selectedDays.contains(curr.dayOfWeek.name)) {
                    count++
                }
                curr = curr.plusDays(1)
            }
            return count
        }

        // Occurrences in week 0 from startDate to end of week 0:
        val endOfWeek0 = startOfWeekDue.plusDays(6)
        var countWeek0 = 0
        var curr = startDate
        while (!curr.isAfter(endOfWeek0)) {
            if (selectedDays.contains(curr.dayOfWeek.name)) {
                countWeek0++
            }
            curr = curr.plusDays(1)
        }

        // Full active weeks strictly between week 0 and weekIndex:
        val fullActiveWeeksBetween = ((weekIndex - 1) / interval).toInt()
        val countIntermediate = fullActiveWeeksBetween * selectedDays.size

        // Occurrences in the target week if weekIndex is an active week:
        var countTargetWeek = 0
        if (weekIndex % interval == 0L) {
            var wCurr = startOfWeekTarget
            while (!wCurr.isAfter(targetDate)) {
                if (selectedDays.contains(wCurr.dayOfWeek.name)) {
                    countTargetWeek++
                }
                wCurr = wCurr.plusDays(1)
            }
        }

        return countWeek0 + countIntermediate + countTargetWeek
    }

    private fun countMonthlyOccurrences(
        startDate: LocalDate,
        targetDate: LocalDate,
        interval: Int
    ): Int {
        val yearDiff = targetDate.year - startDate.year
        val monthDiff = (yearDiff * 12) + (targetDate.monthValue - startDate.monthValue)
        if (monthDiff < 0) return 0
        if (monthDiff == 0) return 1

        val fullActiveMonthsBetween = (monthDiff - 1) / interval
        var count = 1 + fullActiveMonthsBetween

        if (monthDiff % interval == 0) {
            val startDay = startDate.dayOfMonth
            val maxDaysInTargetMonth = targetDate.lengthOfMonth()
            val effectiveDay = if (startDay > maxDaysInTargetMonth) maxDaysInTargetMonth else startDay
            if (targetDate.dayOfMonth >= effectiveDay) {
                count++
            }
        }
        return count
    }

    private fun countYearlyOccurrences(
        startDate: LocalDate,
        targetDate: LocalDate,
        interval: Int
    ): Int {
        val yearDiff = targetDate.year - startDate.year
        if (yearDiff < 0) return 0
        if (yearDiff == 0) return 1

        val fullActiveYearsBetween = (yearDiff - 1) / interval
        var count = 1 + fullActiveYearsBetween

        if (yearDiff % interval == 0) {
            val startDay = startDate.dayOfMonth
            val effectiveDay = if (startDate.monthValue == 2 && startDay == 29 && !targetDate.isLeapYear) {
                28
            } else {
                startDay
            }
            val hasOccurredThisYear = if (targetDate.monthValue > startDate.monthValue) {
                true
            } else if (targetDate.monthValue == startDate.monthValue) {
                targetDate.dayOfMonth >= effectiveDay
            } else {
                false
            }
            if (hasOccurredThisYear) {
                count++
            }
        }
        return count
    }

    private fun countMonthlyWindowOccurrences(
        task: TaskItem,
        startDate: LocalDate,
        targetDate: LocalDate
    ): Int {
        val yearDiff = targetDate.year - startDate.year
        val monthDiff = (yearDiff * 12) + (targetDate.monthValue - startDate.monthValue)
        if (monthDiff < 0) return 0

        var count = 0
        var ym = YearMonth.from(startDate)
        val targetYm = YearMonth.from(targetDate)

        while (!ym.isAfter(targetYm)) {
            val maxDays = ym.lengthOfMonth()
            val rawStart = task.recurrenceWindowStartDay ?: startDate.dayOfMonth
            val rawEnd = task.recurrenceWindowEndDay ?: rawStart
            val minDay = minOf(rawStart, rawEnd).coerceIn(1, 31)
            val maxDay = maxOf(rawStart, rawEnd).coerceIn(1, 31)
            val effStart = minOf(minOf(minDay, maxDay), maxDays)
            val effEnd = minOf(maxOf(minDay, maxDay), maxDays)

            if (ym == targetYm) {
                val targetDay = targetDate.dayOfMonth
                if (targetDay >= effStart) {
                    count += minOf(targetDay, effEnd) - effStart + 1
                }
            } else {
                count += effEnd - effStart + 1
            }
            ym = ym.plusMonths(1)
        }
        return count
    }

    /**
     * Determines whether the occurrence on targetDateIso has been completed.
     * Completing today's occurrence never completes future occurrences.
     */
    fun isCompletedOnDate(task: TaskItem, targetDateIso: String): Boolean {
        return when (task.recurrenceType) {
            RecurrenceType.NONE -> {
                task.isOneTimeCompleted || task.completedDates.contains(targetDateIso)
            }
            RecurrenceType.MONTHLY_WINDOW -> {
                val targetYearMonth = if (targetDateIso.length >= 7) targetDateIso.substring(0, 7) else ""
                task.completedDates.any { it.startsWith(targetYearMonth) }
            }
            else -> {
                task.completedDates.contains(targetDateIso)
            }
        }
    }

    /**
     * Finds next upcoming occurrence date from referenceDateIso.
     */
    fun getNextOccurrenceDate(
        task: TaskItem,
        fromDateIso: String = DateUtils.todayString(),
        firstDayOfWeek: String = "MONDAY"
    ): String? {
        val nexts = getNextNOccurrences(task, count = 1, fromDateIso = fromDateIso, firstDayOfWeek = firstDayOfWeek)
        return nexts.firstOrNull()
    }

    /**
     * Returns up to [count] scheduled upcoming occurrence dates.
     */
    fun getNextNOccurrences(
        task: TaskItem,
        count: Int = 3,
        fromDateIso: String = DateUtils.todayString(),
        firstDayOfWeek: String = "MONDAY"
    ): List<String> {
        if (task.recurrenceType == RecurrenceType.NONE) return emptyList()
        val results = mutableListOf<String>()
        val start = if (fromDateIso < task.dueDateIso) task.dueDateIso else fromDateIso
        val seenMonths = mutableSetOf<String>()
        for (i in 0..730) { // Check up to 2 years ahead
            val date = DateUtils.addDays(start, i)
            if (isTaskScheduledOnDate(task, date, firstDayOfWeek)) {
                if (task.recurrenceType == RecurrenceType.MONTHLY_WINDOW) {
                    val ym = if (date.length >= 7) date.substring(0, 7) else date
                    if (seenMonths.add(ym)) {
                        results.add(date)
                    }
                } else {
                    results.add(date)
                }
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
            RecurrenceType.MONTHLY_WINDOW -> {
                val start = task.recurrenceWindowStartDay ?: 1
                val end = task.recurrenceWindowEndDay ?: start
                val minDay = minOf(start, end).coerceIn(1, 31)
                val maxDay = maxOf(start, end).coerceIn(1, 31)
                if (minDay == maxDay) {
                    "Monthly on the ${formatOrdinal(minDay)}"
                } else {
                    "Monthly between the ${formatOrdinal(minDay)} and ${formatOrdinal(maxDay)}"
                }
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
        val startDate = parseIsoDate(startDateIso) ?: return false
        val targetDate = parseIsoDate(targetDateIso) ?: return false

        val yearDiff = targetDate.year - startDate.year
        val monthDiff = (yearDiff * 12) + (targetDate.monthValue - startDate.monthValue)

        if (monthDiff < 0 || monthDiff % intervalMonths != 0) {
            return false
        }

        val startDay = startDate.dayOfMonth
        val targetDay = targetDate.dayOfMonth
        val maxDaysInTargetMonth = targetDate.lengthOfMonth()

        // If startDay > maxDaysInTargetMonth (e.g. Jan 31 in Feb), fires on last day of Feb (28/29)
        return if (startDay > maxDaysInTargetMonth) {
            targetDay == maxDaysInTargetMonth
        } else {
            targetDay == startDay
        }
    }

    private fun matchesYearly(startDateIso: String, targetDateIso: String): Boolean {
        return matchesYearlyInterval(startDateIso, targetDateIso, intervalYears = 1)
    }

    private fun matchesYearlyInterval(startDateIso: String, targetDateIso: String, intervalYears: Int): Boolean {
        val startDate = parseIsoDate(startDateIso) ?: return false
        val targetDate = parseIsoDate(targetDateIso) ?: return false

        val yearDiff = targetDate.year - startDate.year
        if (yearDiff < 0 || yearDiff % intervalYears != 0) {
            return false
        }

        if (startDate.monthValue != targetDate.monthValue) return false

        val startDay = startDate.dayOfMonth
        val targetDay = targetDate.dayOfMonth

        // Handle Leap Year edge case: Feb 29 on non-leap years
        if (startDate.monthValue == 2 && startDay == 29) {
            if (!targetDate.isLeapYear) {
                // Matches Feb 28 on non-leap year
                return targetDay == 28
            }
        }

        return targetDay == startDay
    }

    private fun matchesMonthlyWindow(task: TaskItem, targetDateIso: String): Boolean {
        val targetDate = parseIsoDate(targetDateIso) ?: return false
        val startIso = if (task.dueDateIso.isNotBlank()) task.dueDateIso else targetDateIso
        val startDate = parseIsoDate(startIso) ?: return false

        val yearDiff = targetDate.year - startDate.year
        val monthDiff = (yearDiff * 12) + (targetDate.monthValue - startDate.monthValue)
        if (monthDiff < 0) return false

        val maxDaysInMonth = targetDate.lengthOfMonth()
        val rawStart = task.recurrenceWindowStartDay ?: startDate.dayOfMonth
        val rawEnd = task.recurrenceWindowEndDay ?: rawStart

        val minDay = minOf(rawStart, rawEnd).coerceIn(1, 31)
        val maxDay = maxOf(rawStart, rawEnd).coerceIn(1, 31)

        val clampedStart = minOf(minDay, maxDaysInMonth)
        val clampedEnd = minOf(maxDay, maxDaysInMonth)

        val effectiveStart = minOf(clampedStart, clampedEnd)
        val effectiveEnd = maxOf(clampedStart, clampedEnd)

        val targetDay = targetDate.dayOfMonth
        return targetDay in effectiveStart..effectiveEnd
    }
}

