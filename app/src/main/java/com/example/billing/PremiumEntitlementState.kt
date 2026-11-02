package com.example.billing

/**
 * State machine representing user's Premium entitlement.
 * Separates Google Play purchase mechanics from application feature gating.
 */
sealed interface PremiumEntitlementState {
    object Free : PremiumEntitlementState

    object PurchaseInitiated : PremiumEntitlementState

    data class TrialActive(
        val expiryTimeMillis: Long,
        val daysRemaining: Int
    ) : PremiumEntitlementState

    data class PremiumActive(
        val expiryTimeMillis: Long,
        val isAutoRenewing: Boolean
    ) : PremiumEntitlementState

    object PendingPurchase : PremiumEntitlementState

    object BillingUnavailable : PremiumEntitlementState

    data class Expired(
        val expiredAtMillis: Long
    ) : PremiumEntitlementState

    data class CancelledUntilExpiry(
        val expiryTimeMillis: Long
    ) : PremiumEntitlementState

    data class GracePeriod(
        val expiryTimeMillis: Long
    ) : PremiumEntitlementState

    object AccountHold : PremiumEntitlementState

    data class VerificationFailed(
        val reason: String
    ) : PremiumEntitlementState

    data class OfflineTrustExpired(
        val lastVerifiedMillis: Long
    ) : PremiumEntitlementState

    data class Unknown(
        val reason: String
    ) : PremiumEntitlementState

    val isEntitled: Boolean
        get() = when (this) {
            is PremiumActive -> true
            is TrialActive -> true
            is CancelledUntilExpiry -> System.currentTimeMillis() < expiryTimeMillis
            is GracePeriod -> System.currentTimeMillis() < expiryTimeMillis
            else -> false
        }
}
