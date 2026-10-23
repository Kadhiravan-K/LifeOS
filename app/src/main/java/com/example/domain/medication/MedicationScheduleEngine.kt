package com.example.domain.medication

import com.example.utils.DateUtils
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Pure deterministic engine for calculating medication schedule occurrences,
 * generating human-readable previews, and managing dose transitions.
 */
object MedicationScheduleEngine {

    /**
     * Generates a clear, user-friendly summary of the configured medication schedule.
     */
    fun formatScheduleSummary(config: MedicationScheduleConfig): String {
        if (config.isAsNeeded) {
            return "As-needed (PRN) • Log doses manually"
        }

        val baseSummary = when (config.scheduleType) {
            MedicineScheduleType.MEAL_BASED -> {
                val active = config.mealPeriods.filter { it.isEnabled }
                if (active.isEmpty()) {
                    "No meal times selected"
                } else {
                    val parts = active.joinToString(", ") { p ->
                        val relation = when (p.foodRelation) {
                            FoodRelationship.BEFORE_FOOD -> "Before ${p.period.displayName.substringBefore(" /")}"
                            FoodRelationship.WITH_FOOD -> "With ${p.period.displayName.substringBefore(" /")}"
                            FoodRelationship.AFTER_FOOD -> "After ${p.period.displayName.substringBefore(" /")}"
                            FoodRelationship.NO_FOOD -> p.period.displayName.substringBefore(" /")
                        }
                        "$relation (${p.scheduledTime})"
                    }
                    "${active.size} reminders daily: $parts"
                }
            }

            MedicineScheduleType.FIXED_TIMES -> {
                val sorted = config.fixedTimes.distinct().sorted()
                if (sorted.isEmpty()) {
                    "No times set"
                } else {
                    "${sorted.size} dose${if (sorted.size > 1) "s" else ""} daily: ${sorted.joinToString(", ")}"
                }
            }

            MedicineScheduleType.INTERVAL -> {
                val overnightText = if (config.allowOvernight) "including overnight" else "daytime only"
                when (config.intervalModel) {
                    IntervalModel.FIXED_SCHEDULE -> "Every ${config.intervalHours}h from ${config.intervalAnchorTime} ($overnightText)"
                    IntervalModel.AFTER_CONFIRMED_DOSE -> "Every ${config.intervalHours}h after each confirmed dose"
                }
            }

            MedicineScheduleType.WEEKDAYS -> {
                val days = if (config.selectedDaysOfWeek.isEmpty()) {
                    "Every day"
                } else {
                    config.selectedDaysOfWeek.joinToString(", ") {
                        it.lowercase().replaceFirstChar { char -> char.uppercase() }
                    }
                }
                val times = config.fixedTimes.distinct().sorted().joinToString(", ")
                "$days at $times"
            }

            MedicineScheduleType.AS_NEEDED -> "As-needed (PRN) • Log doses manually"
        }

        val courseText = when {
            config.courseDurationDays != null && config.courseDurationDays > 0 -> " • For ${config.courseDurationDays} days"
            !config.endDateIso.isNullOrBlank() -> " • Until ${config.endDateIso}"
            else -> ""
        }

        return baseSummary + courseText
    }

    /**
     * Calculates the list of scheduled dose times for a given date.
     * Returns empty list if date falls outside the course, paused, or as-needed.
     */
    fun calculateDailyDoseTimes(
        config: MedicationScheduleConfig,
        targetDateIso: String,
        wakeUpMinutes: Int = 360, // 06:00
        bedTimeMinutes: Int = 1380 // 23:00
    ): List<String> {
        if (config.isAsNeeded) return emptyList()

        // Check date range
        if (config.startDateIso.isNotBlank() && targetDateIso < config.startDateIso) {
            return emptyList()
        }

        // Check end date or course duration
        if (!config.endDateIso.isNullOrBlank() && targetDateIso > config.endDateIso) {
            return emptyList()
        }

        if (config.courseDurationDays != null && config.courseDurationDays > 0 && config.startDateIso.isNotBlank()) {
            val maxEndDate = DateUtils.addDays(config.startDateIso, config.courseDurationDays - 1)
            if (targetDateIso > maxEndDate) return emptyList()
        }

        // Check weekday matching
        if (config.scheduleType == MedicineScheduleType.WEEKDAYS && config.selectedDaysOfWeek.isNotEmpty()) {
            val dayOfWeek = getDayOfWeekUpper(targetDateIso)
            if (!config.selectedDaysOfWeek.contains(dayOfWeek)) {
                return emptyList()
            }
        }

        return when (config.scheduleType) {
            MedicineScheduleType.MEAL_BASED -> {
                config.mealPeriods
                    .filter { it.isEnabled && it.scheduledTime.isNotBlank() }
                    .map { it.scheduledTime }
                    .distinct()
                    .sorted()
            }

            MedicineScheduleType.FIXED_TIMES,
            MedicineScheduleType.WEEKDAYS -> {
                config.fixedTimes.filter { it.isNotBlank() }.distinct().sorted()
            }

            MedicineScheduleType.INTERVAL -> {
                if (config.intervalHours <= 0) return emptyList()
                val anchorMinutes = DateUtils.timeToMinutes(config.intervalAnchorTime)
                val intervalMins = config.intervalHours * 60
                val times = mutableListOf<String>()

                var currentMins = anchorMinutes
                while (currentMins < 24 * 60) {
                    if (config.allowOvernight || (currentMins in wakeUpMinutes..bedTimeMinutes)) {
                        times.add(DateUtils.minutesToTime(currentMins))
                    }
                    currentMins += intervalMins
                }
                times.distinct().sorted()
            }

            MedicineScheduleType.AS_NEEDED -> emptyList()
        }
    }

    /**
     * Determines the next upcoming dose from a list of scheduled dose times on or after the current time.
     */
    fun findNextDoseTime(doseTimes: List<String>, currentTime: String): String? {
        val sorted = doseTimes.sorted()
        return sorted.firstOrNull { it >= currentTime } ?: sorted.firstOrNull()
    }

    private fun getDayOfWeekUpper(dateIso: String): String {
        return try {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
            val date = LocalDate.parse(dateIso, formatter)
            date.dayOfWeek.name
        } catch (e: Exception) {
            val cal = Calendar.getInstance()
            val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateIso) ?: Date()
            cal.time = parsed
            when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> "MONDAY"
                Calendar.TUESDAY -> "TUESDAY"
                Calendar.WEDNESDAY -> "WEDNESDAY"
                Calendar.THURSDAY -> "THURSDAY"
                Calendar.FRIDAY -> "FRIDAY"
                Calendar.SATURDAY -> "SATURDAY"
                Calendar.SUNDAY -> "SUNDAY"
                else -> "MONDAY"
            }
        }
    }
}
