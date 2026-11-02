package com.example.billing

/**
 * Verification contract for Google Play subscription purchases.
 *
 * CRITICAL SECURITY ARCHITECTURE NOTE:
 * Secure purchase verification requires a trusted backend that calls the Google Play Developer API
 * (`purchases.subscriptionsv2.get`) using a Google Cloud service account.
 * Because LifeOS is an offline-first application without a server or user accounts,
 * client-only verification cannot be represented as tamper-proof in production.
 */
enum class PurchaseVerificationStatus {
    VERIFIED_TRUSTED_BACKEND,
    UNVERIFIED_CLIENT_ONLY,
    FAILED_VERIFICATION,
    SIMULATED_DEBUG_ONLY
}

interface PurchaseVerifier {
    suspend fun verifyPurchase(purchase: SubscriptionPurchase): PurchaseVerificationStatus
}

class ClientSidePurchaseVerifier(
    private val isDebugBuild: Boolean
) : PurchaseVerifier {
    override suspend fun verifyPurchase(purchase: SubscriptionPurchase): PurchaseVerificationStatus {
        if (purchase.purchaseToken.isBlank()) {
            return PurchaseVerificationStatus.FAILED_VERIFICATION
        }
        return if (isDebugBuild) {
            PurchaseVerificationStatus.SIMULATED_DEBUG_ONLY
        } else {
            PurchaseVerificationStatus.UNVERIFIED_CLIENT_ONLY
        }
    }
}
