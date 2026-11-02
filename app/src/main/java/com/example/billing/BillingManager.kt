package com.example.billing

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface abstracting Google Play Billing operations.
 */
interface BillingManager {
    val subscriptionProduct: StateFlow<SubscriptionProduct?>
    val subscriptionPurchase: StateFlow<SubscriptionPurchase?>
    val connectionState: StateFlow<BillingConnectionState>

    suspend fun startConnection()
    suspend fun querySubscriptionDetails()
    suspend fun launchPurchaseFlow(activity: Activity): BillingResultStatus
    suspend fun restorePurchases(): BillingResultStatus
    suspend fun acknowledgePurchase(purchaseToken: String): Boolean
    fun destroy()
}
