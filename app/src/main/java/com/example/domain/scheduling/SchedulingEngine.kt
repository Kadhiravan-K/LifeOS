package com.example.domain.scheduling

import com.example.domain.model.DurationUnit
import com.example.domain.model.SchedulingMode
import com.example.domain.model.TaskItem
import com.example.utils.DateUtils
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object SchedulingEngine {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)

    data class ProjectedEnd(
        val endDateIso: String,
        val endTime: String? = null,
        val displaySummary: String
    )

    /**
     * Calculates projected end date & time based on start date, time, and custom duration.
     * Uses java.time.LocalDate and LocalTime for calendar-aware precision.
     * Quarters = 3 calendar months.
     * Preserves month-ends and leap year rules automatically.
     */
    fun calculateProjectedEnd(
        startDateIso: String,
        startTime: String?,
        isAllDay: Boolean,
        amount: Int,
        unit: DurationUnit
    ): ProjectedEnd {
        val safeAmount = if (amount <= 0) 1 else amount
        return try {
            val localDate = LocalDate.parse(startDateIso, dateFormatter)
            val localTime = if (!isAllDay && !startTime.isNullOrBlank()) {
                try {
                    LocalTime.parse(startTime, timeFormatter)
                } catch (e: Exception) {
                    LocalTime.of(9, 0)
                }
            } else null

            when (unit) {
                DurationUnit.MINUTES -> {
                    if (localTime != null) {
                        val totalMinutes = localTime.toSecondOfDay() / 60 + safeAmount
                        val daysToAdd = (totalMinutes / (24 * 60)).toLong()
                        val remainderMinutes = (totalMinutes % (24 * 60)).toInt()
                        val endDate = localDate.plusDays(daysToAdd)
                        val endTime = LocalTime.ofSecondOfDay((remainderMinutes * 60).toLong())
                        val endIso = endDate.format(dateFormatter)
                        val endT = endTime.format(timeFormatter)
                        val dateDisp = if (endIso == startDateIso) "" else " on ${DateUtils.formatDisplayDate(endIso)}"
                        ProjectedEnd(endIso, endT, "Ends at $endT$dateDisp ($safeAmount mins)")
                    } else {
                        // All day or no start time: minutes remain on start day
                        val daysToAdd = (safeAmount / (24 * 60)).toLong()
                        val endDate = localDate.plusDays(daysToAdd)
                        val endIso = endDate.format(dateFormatter)
                        ProjectedEnd(endIso, null, "Duration: $safeAmount minutes")
                    }
                }

                DurationUnit.HOURS -> {
                    if (localTime != null) {
                        val totalMinutes = localTime.toSecondOfDay() / 60 + (safeAmount * 60)
                        val daysToAdd = (totalMinutes / (24 * 60)).toLong()
                        val remainderMinutes = (totalMinutes % (24 * 60)).toInt()
                        val endDate = localDate.plusDays(daysToAdd)
                        val endTime = LocalTime.ofSecondOfDay((remainderMinutes * 60).toLong())
                        val endIso = endDate.format(dateFormatter)
                        val endT = endTime.format(timeFormatter)
                        val dateDisp = if (endIso == startDateIso) "" else " on ${DateUtils.formatDisplayDate(endIso)}"
                        ProjectedEnd(endIso, endT, "Ends at $endT$dateDisp ($safeAmount hrs)")
                    } else {
                        val daysToAdd = (safeAmount / 24).toLong()
                        val endDate = localDate.plusDays(daysToAdd)
                        val endIso = endDate.format(dateFormatter)
                        val dateDisp = if (endIso == startDateIso) "Same day" else "Ends ${DateUtils.formatDisplayDate(endIso)}"
                        ProjectedEnd(endIso, null, "Duration: $safeAmount hours ($dateDisp)")
                    }
                }

                DurationUnit.DAYS -> {
                    val endDate = localDate.plusDays(safeAmount.toLong())
                    val endIso = endDate.format(dateFormatter)
                    val endT = if (!isAllDay) startTime else null
                    ProjectedEnd(endIso, endT, "Ends on ${DateUtils.formatDisplayDate(endIso)}" + if (endT != null) " at $endT" else "")
                }

                DurationUnit.WEEKS -> {
                    val endDate = localDate.plusWeeks(safeAmount.toLong())
                    val endIso = endDate.format(dateFormatter)
                    val endT = if (!isAllDay) startTime else null
                    ProjectedEnd(endIso, endT, "Ends on ${DateUtils.formatDisplayDate(endIso)}" + if (endT != null) " at $endT" else "")
                }

                DurationUnit.MONTHS -> {
                    // java.time plusMonths handles month-end clipping accurately (e.g. Jan 31 + 1 month = Feb 28 or 29)
                    val endDate = localDate.plusMonths(safeAmount.toLong())
                    val endIso = endDate.format(dateFormatter)
                    val endT = if (!isAllDay) startTime else null
                    ProjectedEnd(endIso, endT, "Ends on ${DateUtils.formatDisplayDate(endIso)}" + if (endT != null) " at $endT" else "")
                }

                DurationUnit.QUARTERS -> {
                    // Exactly 3 calendar months per quarter
                    val endDate = localDate.plusMonths((safeAmount * 3).toLong())
                    val endIso = endDate.format(dateFormatter)
                    val endT = if (!isAllDay) startTime else null
                    ProjectedEnd(endIso, endT, "Ends on ${DateUtils.formatDisplayDate(endIso)}" + if (endT != null) " at $endT" else "")
                }

                DurationUnit.YEARS -> {
                    // Handles leap year accurately (e.g. Feb 29 + 1 year = Feb 28)
                    val endDate = localDate.plusYears(safeAmount.toLong())
                    val endIso = endDate.format(dateFormatter)
                    val endT = if (!isAllDay) startTime else null
                    ProjectedEnd(endIso, endT, "Ends on ${DateUtils.formatDisplayDate(endIso)}" + if (endT != null) " at $endT" else "")
                }
            }
        } catch (e: Exception) {
            ProjectedEnd(startDateIso, startTime, "Duration: $safeAmount ${unit.displayName}")
        }
    }

    /**
     * Converts any duration amount + unit to equivalent elapsed minutes for backward compatibility.
     */
    fun toElapsedMinutes(startDateIso: String, amount: Int, unit: DurationUnit): Int {
        val safeAmount = if (amount <= 0) 1 else amount
        return when (unit) {
            DurationUnit.MINUTES -> safeAmount
            DurationUnit.HOURS -> safeAmount * 60
            DurationUnit.DAYS -> safeAmount * 24 * 60
            DurationUnit.WEEKS -> safeAmount * 7 * 24 * 60
            DurationUnit.MONTHS -> {
                try {
                    val start = LocalDate.parse(startDateIso, dateFormatter)
                    val end = start.plusMonths(safeAmount.toLong())
                    val days = java.time.temporal.ChronoUnit.DAYS.between(start, end).toInt()
                    days * 24 * 60
                } catch (e: Exception) {
                    safeAmount * 30 * 24 * 60
                }
            }
            DurationUnit.QUARTERS -> {
                try {
                    val start = LocalDate.parse(startDateIso, dateFormatter)
                    val end = start.plusMonths((safeAmount * 3).toLong())
                    val days = java.time.temporal.ChronoUnit.DAYS.between(start, end).toInt()
                    days * 24 * 60
                } catch (e: Exception) {
                    safeAmount * 90 * 24 * 60
                }
            }
            DurationUnit.YEARS -> {
                try {
                    val start = LocalDate.parse(startDateIso, dateFormatter)
                    val end = start.plusYears(safeAmount.toLong())
                    val days = java.time.temporal.ChronoUnit.DAYS.between(start, end).toInt()
                    days * 24 * 60
                } catch (e: Exception) {
                    safeAmount * 365 * 24 * 60
                }
            }
        }
    }

    /**
     * Returns a compact badge label for a task card or details screen.
     */
    fun getSchedulingBadge(task: TaskItem): String? {
        return when (task.schedulingMode) {
            SchedulingMode.DEADLINE -> {
                if (task.deadlineIso != null) {
                    val dateStr = DateUtils.formatDisplayDate(task.deadlineIso)
                    val timeStr = if (!task.deadlineTime.isNullOrBlank()) " at ${task.deadlineTime}" else ""
                    "Due $dateStr$timeStr"
                } else null
            }
            SchedulingMode.DURATION -> {
                val amount = task.durationAmount
                val unit = task.durationUnit
                val label = if (amount == 1) unit.singularName else unit.displayName
                "$amount $label"
            }
        }
    }
}
