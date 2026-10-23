package com.example.domain.subscription

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Deterministic recurrence, schedule calculation, and missing-date resolution engine
 * for subscriptions and billing occurrences.
 */
object SubscriptionScheduleEngine {

    private val ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE

    /**
     * Calculates the next billing date following a given current billing date,
     * maintaining the original anchor day of month and applying the selected missing-date policy.
     */
    fun calculateNextBillingDate(
        currentDateIso: String,
        anchorDayOfMonth: Int,
        frequency: BillingFrequency,
        customIntervalCount: Int = 1,
        customIntervalUnit: CustomIntervalUnit = CustomIntervalUnit.MONTHS,
        missingDatePolicy: MissingDatePolicy = MissingDatePolicy.LAST_VALID_DAY
    ): String {
        return try {
            val current = LocalDate.parse(currentDateIso, ISO_FORMATTER)
            val nextDate = when (frequency) {
                BillingFrequency.DAILY -> current.plusDays(1)
                BillingFrequency.WEEKLY -> current.plusWeeks(1)
                BillingFrequency.MONTHLY -> advanceMonths(current, 1, anchorDayOfMonth, missingDatePolicy)
                BillingFrequency.QUARTERLY -> advanceMonths(current, 3, anchorDayOfMonth, missingDatePolicy)
                BillingFrequency.YEARLY -> advanceYears(current, 1, anchorDayOfMonth, missingDatePolicy)
                BillingFrequency.CUSTOM -> {
                    val count = customIntervalCount.coerceAtLeast(1).toLong()
                    when (customIntervalUnit) {
                        CustomIntervalUnit.DAYS -> current.plusDays(count)
                        CustomIntervalUnit.WEEKS -> current.plusWeeks(count)
                        CustomIntervalUnit.MONTHS -> advanceMonths(current, count.toInt(), anchorDayOfMonth, missingDatePolicy)
                        CustomIntervalUnit.YEARS -> advanceYears(current, count.toInt(), anchorDayOfMonth, missingDatePolicy)
                    }
                }
            }
            nextDate.format(ISO_FORMATTER)
        } catch (e: Exception) {
            currentDateIso
        }
    }

    private fun advanceMonths(
        current: LocalDate,
        monthsToAdd: Int,
        anchorDayOfMonth: Int,
        policy: MissingDatePolicy
    ): LocalDate {
        val targetYearMonth = YearMonth.from(current).plusMonths(monthsToAdd.toLong())
        val daysInMonth = targetYearMonth.lengthOfMonth()

        return if (anchorDayOfMonth <= daysInMonth) {
            targetYearMonth.atDay(anchorDayOfMonth)
        } else {
            when (policy) {
                MissingDatePolicy.LAST_VALID_DAY -> targetYearMonth.atDay(daysInMonth)
                MissingDatePolicy.FIRST_DAY_NEXT_MONTH -> targetYearMonth.plusMonths(1).atDay(1)
            }
        }
    }

    private fun advanceYears(
        current: LocalDate,
        yearsToAdd: Int,
        anchorDayOfMonth: Int,
        policy: MissingDatePolicy
    ): LocalDate {
        val targetYear = current.year + yearsToAdd
        val targetMonth = current.month
        val targetYearMonth = YearMonth.of(targetYear, targetMonth)
        val daysInMonth = targetYearMonth.lengthOfMonth()

        return if (anchorDayOfMonth <= daysInMonth) {
            targetYearMonth.atDay(anchorDayOfMonth)
        } else {
            when (policy) {
                MissingDatePolicy.LAST_VALID_DAY -> targetYearMonth.atDay(daysInMonth)
                MissingDatePolicy.FIRST_DAY_NEXT_MONTH -> targetYearMonth.plusMonths(1).atDay(1)
            }
        }
    }

    /**
     * Determines the status of a bill based on payments and current date.
     */
    fun evaluateBillStatus(
        expectedAmount: Double,
        payments: List<SubscriptionPaymentItem>,
        dueDateIso: String,
        todayIso: String,
        currentStatus: BillStatus
    ): BillStatus {
        if (currentStatus == BillStatus.SKIPPED || currentStatus == BillStatus.WAIVED) {
            return currentStatus
        }

        val totalPaid = payments.sumOf { it.amountPaid }
        val isPastDue = try {
            val due = LocalDate.parse(dueDateIso, ISO_FORMATTER)
            val today = LocalDate.parse(todayIso, ISO_FORMATTER)
            today.isAfter(due)
        } catch (e: Exception) {
            false
        }

        return when {
            totalPaid >= (expectedAmount - 0.001) -> BillStatus.PAID
            totalPaid > 0.0 -> if (isPastDue) BillStatus.OVERDUE else BillStatus.PARTIALLY_PAID
            isPastDue -> BillStatus.OVERDUE
            else -> BillStatus.UNPAID
        }
    }

    /**
     * Computes the normalized monthly-equivalent cost for analytics.
     */
    fun calculateMonthlyEquivalent(
        amount: Double,
        frequency: BillingFrequency,
        customIntervalCount: Int = 1,
        customIntervalUnit: CustomIntervalUnit = CustomIntervalUnit.MONTHS
    ): Double {
        if (amount <= 0.0) return 0.0
        val raw = when (frequency) {
            BillingFrequency.DAILY -> amount * 30.4375
            BillingFrequency.WEEKLY -> amount * (52.0 / 12.0)
            BillingFrequency.MONTHLY -> amount
            BillingFrequency.QUARTERLY -> amount / 3.0
            BillingFrequency.YEARLY -> amount / 12.0
            BillingFrequency.CUSTOM -> {
                val count = customIntervalCount.coerceAtLeast(1).toDouble()
                when (customIntervalUnit) {
                    CustomIntervalUnit.DAYS -> (amount / count) * 30.4375
                    CustomIntervalUnit.WEEKS -> (amount / count) * (52.0 / 12.0)
                    CustomIntervalUnit.MONTHS -> amount / count
                    CustomIntervalUnit.YEARS -> (amount / count) / 12.0
                }
            }
        }
        return Math.round(raw * 100.0) / 100.0
    }

    /**
     * Computes the normalized yearly-equivalent cost for analytics.
     */
    fun calculateYearlyEquivalent(
        amount: Double,
        frequency: BillingFrequency,
        customIntervalCount: Int = 1,
        customIntervalUnit: CustomIntervalUnit = CustomIntervalUnit.MONTHS
    ): Double {
        if (amount <= 0.0) return 0.0
        val raw = when (frequency) {
            BillingFrequency.DAILY -> amount * 365.0
            BillingFrequency.WEEKLY -> amount * 52.0
            BillingFrequency.MONTHLY -> amount * 12.0
            BillingFrequency.QUARTERLY -> amount * 4.0
            BillingFrequency.YEARLY -> amount
            BillingFrequency.CUSTOM -> {
                val count = customIntervalCount.coerceAtLeast(1).toDouble()
                when (customIntervalUnit) {
                    CustomIntervalUnit.DAYS -> (amount / count) * 365.0
                    CustomIntervalUnit.WEEKS -> (amount / count) * 52.0
                    CustomIntervalUnit.MONTHS -> (amount / count) * 12.0
                    CustomIntervalUnit.YEARS -> amount / count
                }
            }
        }
        return Math.round(raw * 100.0) / 100.0
    }
}
