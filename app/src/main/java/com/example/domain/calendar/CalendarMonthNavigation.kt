package com.example.domain.calendar

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object CalendarMonthNavigation {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun shiftSelectedDate(dateIso: String, months: Long): String {
        val selectedDate = LocalDate.parse(dateIso, dateFormatter)
        val targetMonth = selectedDate.withDayOfMonth(1).plusMonths(months)
        val targetDate = targetMonth.withDayOfMonth(
            selectedDate.dayOfMonth.coerceAtMost(targetMonth.lengthOfMonth())
        )
        return targetDate.format(dateFormatter)
    }
}
