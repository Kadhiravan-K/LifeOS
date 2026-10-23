package com.example.domain.subscription

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test suite verifying Requirement #7:
 * LifeOS Subscription Manager, Anchor Day Preservation, and Billing Engine.
 */
class SubscriptionUnitTest {

    @Test
    fun monthlySchedule_advancesOneMonth() {
        val nextDate = SubscriptionScheduleEngine.calculateNextBillingDate(
            currentDateIso = "2026-01-15",
            anchorDayOfMonth = 15,
            frequency = BillingFrequency.MONTHLY,
            missingDatePolicy = MissingDatePolicy.LAST_VALID_DAY
        )

        assertEquals("2026-02-15", nextDate)
    }

    @Test
    fun anchorDayPreservation_jan31ToFeb_lastValidDay_returnsFeb28() {
        val nextDate = SubscriptionScheduleEngine.calculateNextBillingDate(
            currentDateIso = "2026-01-31",
            anchorDayOfMonth = 31,
            frequency = BillingFrequency.MONTHLY,
            missingDatePolicy = MissingDatePolicy.LAST_VALID_DAY
        )

        // 2026 is not a leap year, February has 28 days
        assertEquals("2026-02-28", nextDate)
    }

    @Test
    fun anchorDayPreservation_feb28ToMarch_anchorPreserved_returnsMarch31() {
        // Even though February billed on the 28th, the anchorDayOfMonth is 31,
        // advancing to March MUST restore March 31st!
        val nextDate = SubscriptionScheduleEngine.calculateNextBillingDate(
            currentDateIso = "2026-02-28",
            anchorDayOfMonth = 31,
            frequency = BillingFrequency.MONTHLY,
            missingDatePolicy = MissingDatePolicy.LAST_VALID_DAY
        )

        assertEquals("2026-03-31", nextDate)
    }

    @Test
    fun anchorDayPreservation_firstDayNextMonthPolicy_returnsMarch1() {
        val nextDate = SubscriptionScheduleEngine.calculateNextBillingDate(
            currentDateIso = "2026-01-31",
            anchorDayOfMonth = 31,
            frequency = BillingFrequency.MONTHLY,
            missingDatePolicy = MissingDatePolicy.FIRST_DAY_NEXT_MONTH
        )

        // Under FIRST_DAY_NEXT_MONTH, missing Feb 31 lands on March 1st
        assertEquals("2026-03-01", nextDate)
    }

    @Test
    fun weeklySchedule_advances7Days() {
        val nextDate = SubscriptionScheduleEngine.calculateNextBillingDate(
            currentDateIso = "2026-03-10",
            anchorDayOfMonth = 10,
            frequency = BillingFrequency.WEEKLY
        )

        assertEquals("2026-03-17", nextDate)
    }

    @Test
    fun yearlySchedule_advancesOneYear() {
        val nextDate = SubscriptionScheduleEngine.calculateNextBillingDate(
            currentDateIso = "2026-05-20",
            anchorDayOfMonth = 20,
            frequency = BillingFrequency.YEARLY
        )

        assertEquals("2027-05-20", nextDate)
    }

    @Test
    fun customInterval_advancesExactDays() {
        val nextDate = SubscriptionScheduleEngine.calculateNextBillingDate(
            currentDateIso = "2026-06-01",
            anchorDayOfMonth = 1,
            frequency = BillingFrequency.CUSTOM,
            customIntervalCount = 45,
            customIntervalUnit = CustomIntervalUnit.DAYS
        )

        assertEquals("2026-07-16", nextDate)
    }

    @Test
    fun costCalculations_annualAndMonthlyEquivalents() {
        // Monthly 15.00
        val annualMonthly = SubscriptionScheduleEngine.calculateYearlyEquivalent(15.0, BillingFrequency.MONTHLY)
        assertEquals(180.0, annualMonthly, 0.001)

        val monthlyFromMonthly = SubscriptionScheduleEngine.calculateMonthlyEquivalent(15.0, BillingFrequency.MONTHLY)
        assertEquals(15.0, monthlyFromMonthly, 0.001)

        // Yearly 120.00
        val monthlyFromYearly = SubscriptionScheduleEngine.calculateMonthlyEquivalent(120.0, BillingFrequency.YEARLY)
        assertEquals(10.0, monthlyFromYearly, 0.001)

        // Weekly 10.00 -> 520.00 annual
        val annualFromWeekly = SubscriptionScheduleEngine.calculateYearlyEquivalent(10.0, BillingFrequency.WEEKLY)
        assertEquals(520.0, annualFromWeekly, 0.001)
    }

    @Test
    fun partialPayment_updatesStatusToPartiallyPaidUntilFullyPaid() {
        val billAmount = 100.0
        val payments1 = listOf(
            SubscriptionPaymentItem(billId = 1, subscriptionId = 1, amountPaid = 40.0, paymentDateIso = "2026-03-01")
        )

        val status1 = SubscriptionScheduleEngine.evaluateBillStatus(
            expectedAmount = billAmount,
            payments = payments1,
            dueDateIso = "2026-03-15",
            todayIso = "2026-03-02",
            currentStatus = BillStatus.UNPAID
        )
        assertEquals(BillStatus.PARTIALLY_PAID, status1)

        val payments2 = listOf(
            SubscriptionPaymentItem(billId = 1, subscriptionId = 1, amountPaid = 40.0, paymentDateIso = "2026-03-01"),
            SubscriptionPaymentItem(billId = 1, subscriptionId = 1, amountPaid = 60.0, paymentDateIso = "2026-03-05")
        )

        val status2 = SubscriptionScheduleEngine.evaluateBillStatus(
            expectedAmount = billAmount,
            payments = payments2,
            dueDateIso = "2026-03-15",
            todayIso = "2026-03-06",
            currentStatus = BillStatus.PARTIALLY_PAID
        )
        assertEquals(BillStatus.PAID, status2)
    }

    @Test
    fun overdueDetection_marksBillOverdueWhenPastDueDate() {
        val status = SubscriptionScheduleEngine.evaluateBillStatus(
            expectedAmount = 50.0,
            payments = emptyList(),
            dueDateIso = "2026-03-01",
            todayIso = "2026-03-02",
            currentStatus = BillStatus.UNPAID
        )
        assertEquals(BillStatus.OVERDUE, status)
    }

    @Test
    fun priceChangePreservesPastAuditHistory() {
        val originalAmount = 14.99
        val newAmount = 19.99

        val priceHistory = PriceHistoryItem(
            subscriptionId = 1L,
            oldAmount = originalAmount,
            newAmount = newAmount,
            effectiveDateIso = "2026-04-01",
            reason = "Annual price adjustment"
        )

        assertEquals(14.99, priceHistory.oldAmount, 0.001)
        assertEquals(19.99, priceHistory.newAmount, 0.001)
        assertEquals("2026-04-01", priceHistory.effectiveDateIso)
    }
}
