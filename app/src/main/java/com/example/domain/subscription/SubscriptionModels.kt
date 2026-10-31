package com.example.domain.subscription

import androidx.compose.ui.graphics.Color
import com.example.domain.model.TodoistColorPalette

enum class BillingFrequency(val displayName: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    QUARTERLY("Quarterly"),
    YEARLY("Yearly"),
    CUSTOM("Custom Interval");

    companion object {
        fun fromString(value: String?): BillingFrequency {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: MONTHLY
        }
    }
}

enum class CustomIntervalUnit(val displayName: String) {
    DAYS("Days"),
    WEEKS("Weeks"),
    MONTHS("Months"),
    YEARS("Years");

    companion object {
        fun fromString(value: String?): CustomIntervalUnit {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: MONTHS
        }
    }
}

enum class MissingDatePolicy(val displayName: String, val description: String) {
    LAST_VALID_DAY("Last Valid Day of Month", "For anchors like the 31st, bills on Feb 28/29, then recovers to 31st for March."),
    FIRST_DAY_NEXT_MONTH("First Day of Next Month", "For anchors like the 31st, bills on March 1, then recovers to 31st for March.");

    companion object {
        fun fromString(value: String?): MissingDatePolicy {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: LAST_VALID_DAY
        }
    }
}

enum class SubscriptionStatus {
    ACTIVE,
    PAUSED,
    CANCELLED,
    TRIAL,
    ARCHIVED;

    companion object {
        fun fromString(value: String?): SubscriptionStatus {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: ACTIVE
        }
    }
}

enum class BillStatus {
    UNPAID,
    PARTIALLY_PAID,
    PAID,
    OVERDUE,
    SKIPPED,
    WAIVED;

    companion object {
        fun fromString(value: String?): BillStatus {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: UNPAID
        }
    }
}

/**
 * Domain model representing a Subscription definition.
 */
data class SubscriptionItem(
    val id: Long = 0,
    val profileId: Long = 1,
    val title: String,
    val emoji: String = "💳",
    val description: String = "",
    val projectId: Long? = null,
    val labelIds: List<Long> = emptyList(),
    val customColorHex: Long? = null,
    val amount: Double,
    val currency: String = "₹",
    val frequency: BillingFrequency = BillingFrequency.MONTHLY,
    val customIntervalCount: Int = 1,
    val customIntervalUnit: CustomIntervalUnit = CustomIntervalUnit.MONTHS,
    val anchorDateIso: String, // Initial start / billing anchor date (e.g. 2026-01-31)
    val anchorDayOfMonth: Int = 1, // 1 to 31, preserved across short months
    val nextRenewalDateIso: String,
    val missingDatePolicy: MissingDatePolicy = MissingDatePolicy.LAST_VALID_DAY,
    val autoRenew: Boolean = true,
    val paymentMethod: String = "",
    val isTrial: Boolean = false,
    val trialStartDateIso: String? = null,
    val trialEndDateIso: String? = null,
    val status: SubscriptionStatus = SubscriptionStatus.ACTIVE,
    val cancellationDateIso: String? = null,
    val cancellationReason: String = "",
    val reminderDaysBefore: Int = 3, // Presets: 1, 3, 7 days before
    val overdueReminderEnabled: Boolean = true,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun resolveColor(projectColorHex: Long?): Color {
        return when {
            customColorHex != null -> Color(customColorHex)
            projectColorHex != null -> Color(projectColorHex)
            else -> Color(0xFF10B981) // default emerald
        }
    }
}

/**
 * Domain model representing a single billing occurrence (Bill).
 */
data class BillingOccurrenceItem(
    val id: Long = 0,
    val subscriptionId: Long,
    val dueDateIso: String,
    val expectedAmount: Double,
    val currency: String = "₹",
    val status: BillStatus = BillStatus.UNPAID,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Domain model representing an individual payment towards a bill (supporting partial payments).
 */
data class SubscriptionPaymentItem(
    val id: Long = 0,
    val billId: Long,
    val subscriptionId: Long,
    val amountPaid: Double,
    val paymentDateIso: String,
    val paymentMethod: String = "",
    val transactionRef: String = "",
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Price change history for a subscription.
 */
data class PriceHistoryItem(
    val id: Long = 0,
    val subscriptionId: Long,
    val oldAmount: Double,
    val newAmount: Double,
    val effectiveDateIso: String,
    val reason: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
