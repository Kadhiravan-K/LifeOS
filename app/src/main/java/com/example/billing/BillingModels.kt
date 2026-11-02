package com.example.billing

/**
 * Approved commercial subscription models for LifeOS Release 4 (MON-01).
 * Supports monthly subscription with an optional 7-day free trial offer.
 * No annual, lifetime, or one-time purchase models.
 */
data class SubscriptionProduct(
    val productId: String,
    val basePlanId: String,
    val offerId: String? = null,
    val title: String = "",
    val description: String = "",
    val formattedPrice: String,
    val priceCurrencyCode: String,
    val priceMicros: Long,
    val billingPeriod: String, // e.g., "P1M"
    val freeTrialPeriod: String? = null, // e.g., "P7D"
    val freeTrialDays: Int = 0,
    val hasFreeTrial: Boolean = freeTrialDays > 0
) {
    val priceFormatted: String get() = formattedPrice
    val trialPeriodDays: Int get() = freeTrialDays
}

enum class PurchaseState {
    UNSPECIFIED,
    PURCHASED,
    PENDING
}

data class SubscriptionPurchase(
    val orderId: String?,
    val purchaseToken: String,
    val productId: String,
    val purchaseTimeMillis: Long,
    val purchaseState: PurchaseState,
    val isAcknowledged: Boolean,
    val isAutoRenewing: Boolean = true,
    val expiryTimeMillis: Long? = null,
    val isTrial: Boolean = false
)

enum class BillingResultStatus {
    OK,
    USER_CANCELED,
    SERVICE_UNAVAILABLE,
    BILLING_UNAVAILABLE,
    ITEM_UNAVAILABLE,
    DEVELOPER_ERROR,
    ERROR,
    ITEM_ALREADY_OWNED,
    ITEM_NOT_OWNED,
    NETWORK_ERROR
}

enum class BillingConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    CLOSED
}
