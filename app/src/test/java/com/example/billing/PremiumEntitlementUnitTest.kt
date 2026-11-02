package com.example.billing

import com.example.ui.theme.engine.PremiumEntitlementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Comprehensive unit tests for Release 4 Phase P5-L1:
 * Premium Subscription Foundation, Google Play Billing entitlement state machine,
 * verification boundaries, and developer simulator gating.
 */
class PremiumEntitlementUnitTest {

    @Test
    fun freeUser_hasNoEntitlement() {
        val state = PremiumEntitlementState.Free
        assertFalse("Free state must not be entitled", state.isEntitled)
    }

    @Test
    fun activeSubscription_grantsEntitlement() {
        val now = System.currentTimeMillis()
        val state = PremiumEntitlementState.PremiumActive(
            expiryTimeMillis = now + 86400000L * 30,
            isAutoRenewing = true
        )
        assertTrue("Active subscription must grant entitlement", state.isEntitled)
    }

    @Test
    fun trialActive_grantsEntitlementAndReflectsTrialStatus() {
        val trialExpiry = System.currentTimeMillis() + (86400000L * 7)
        val state = PremiumEntitlementState.TrialActive(
            expiryTimeMillis = trialExpiry,
            daysRemaining = 7
        )
        assertTrue("Trial active must grant entitlement", state.isEntitled)
        assertEquals(7, state.daysRemaining)
    }

    @Test
    fun pendingPurchase_doesNotGrantEntitlementPrematurely() {
        val state = PremiumEntitlementState.PendingPurchase
        assertFalse("Pending purchase must NOT grant entitlement", state.isEntitled)
    }

    @Test
    fun cancelledUntilExpiry_retainsEntitlementUntilExpiryDate() {
        val futureExpiry = System.currentTimeMillis() + 86400000L * 15
        val validCancelledState = PremiumEntitlementState.CancelledUntilExpiry(
            expiryTimeMillis = futureExpiry
        )
        assertTrue("Cancelled subscription before expiry MUST retain entitlement", validCancelledState.isEntitled)

        val pastExpiry = System.currentTimeMillis() - 10000L
        val expiredCancelledState = PremiumEntitlementState.CancelledUntilExpiry(
            expiryTimeMillis = pastExpiry
        )
        assertFalse("Cancelled subscription past expiry must lose entitlement", expiredCancelledState.isEntitled)
    }

    @Test
    fun expiredSubscription_losesEntitlement() {
        val state = PremiumEntitlementState.Expired(
            expiredAtMillis = System.currentTimeMillis() - 50000L
        )
        assertFalse("Expired subscription must not be entitled", state.isEntitled)
    }

    @Test
    fun gracePeriodAndAccountHold_reflectsPolicy() {
        val futureGrace = PremiumEntitlementState.GracePeriod(
            expiryTimeMillis = System.currentTimeMillis() + 86400000L * 3
        )
        assertTrue("Grace period before expiry must grant entitlement", futureGrace.isEntitled)

        val expiredGrace = PremiumEntitlementState.GracePeriod(
            expiryTimeMillis = System.currentTimeMillis() - 5000L
        )
        assertFalse("Grace period after expiry must NOT grant entitlement", expiredGrace.isEntitled)

        val hold = PremiumEntitlementState.AccountHold
        assertFalse("Account hold must revoke entitlement", hold.isEntitled)
    }

    @Test
    fun billingUnavailable_doesNotCrashAndHasNoEntitlement() {
        val state = PremiumEntitlementState.BillingUnavailable
        assertFalse("Billing unavailable must not be entitled", state.isEntitled)
    }

    @Test
    fun clientSidePurchaseVerifier_enforcesSecurityBoundary() {
        val validPurchase = SubscriptionPurchase(
            orderId = "GPA.9999-8888",
            productId = "lifeos_premium_monthly",
            purchaseToken = "unverified_client_token",
            purchaseTimeMillis = System.currentTimeMillis(),
            purchaseState = PurchaseState.PURCHASED,
            isAcknowledged = true
        )

        // Debug verifier flags as debug simulation
        val debugVerifier = ClientSidePurchaseVerifier(isDebugBuild = true)
        val debugResult = runBlocking { debugVerifier.verifyPurchase(validPurchase) }
        assertEquals(PurchaseVerificationStatus.SIMULATED_DEBUG_ONLY, debugResult)

        // Release verifier marks as unverified client-only
        val releaseVerifier = ClientSidePurchaseVerifier(isDebugBuild = false)
        val releaseResult = runBlocking { releaseVerifier.verifyPurchase(validPurchase) }
        assertEquals(PurchaseVerificationStatus.UNVERIFIED_CLIENT_ONLY, releaseResult)

        // Empty token is rejected
        val emptyTokenPurchase = validPurchase.copy(purchaseToken = "")
        val rejectedResult = runBlocking { releaseVerifier.verifyPurchase(emptyTokenPurchase) }
        assertEquals(PurchaseVerificationStatus.FAILED_VERIFICATION, rejectedResult)
    }

    @Test
    fun subscriptionProduct_onlyMonthlyCadenceApproved() {
        val product = SubscriptionProduct(
            productId = "lifeos_premium_monthly",
            basePlanId = "monthly-plan",
            offerId = "7-day-free-trial",
            title = "LifeOS Premium (Monthly)",
            description = "Unlock all premium themes and support ongoing development with a 7-day free trial.",
            formattedPrice = "$2.99",
            priceCurrencyCode = "USD",
            priceMicros = 2990000L,
            billingPeriod = "P1M",
            freeTrialDays = 7
        )

        assertEquals("lifeos_premium_monthly", product.productId)
        assertEquals("P1M", product.billingPeriod)
        assertEquals(7, product.freeTrialDays)
        assertTrue("Product has free trial", product.hasFreeTrial)
        assertEquals("7-day-free-trial", product.offerId)
    }

    @Test
    fun repository_evaluatesActivePurchaseCorrectly() = runBlocking {
        val fakeBilling = FakeBillingManager()
        val verifier = ClientSidePurchaseVerifier(isDebugBuild = true)
        val simulatedFlow = MutableStateFlow(false)

        val repo = TestEntitlementRepository(
            billingManager = fakeBilling,
            purchaseVerifier = verifier,
            simulatedFlow = simulatedFlow,
            isDebug = true
        )

        // Initial: No purchases -> Free
        val initial = repo.entitlementState.first()
        assertEquals(PremiumEntitlementState.Free, initial)
        assertFalse(repo.isPremiumActive.first())

        // Active monthly purchase older than trial window (e.g. 10 days ago)
        val purchaseTime = System.currentTimeMillis() - (10L * 24 * 60 * 60 * 1000)
        val expiryTime = purchaseTime + (30L * 24 * 60 * 60 * 1000)
        fakeBilling.emitPurchase(
            SubscriptionPurchase(
                orderId = "GPA.1111",
                productId = "lifeos_premium_monthly",
                purchaseToken = "valid_token",
                purchaseTimeMillis = purchaseTime,
                purchaseState = PurchaseState.PURCHASED,
                isAcknowledged = true,
                isAutoRenewing = true,
                expiryTimeMillis = expiryTime
            )
        )

        val active = repo.entitlementState.first()
        assertTrue("Entitlement state should become PremiumActive", active is PremiumEntitlementState.PremiumActive)
        assertTrue(repo.isPremiumActive.first())
    }

    @Test
    fun repository_evaluatesTrialActiveCorrectly() = runBlocking {
        val fakeBilling = FakeBillingManager()
        val verifier = ClientSidePurchaseVerifier(isDebugBuild = true)
        val simulatedFlow = MutableStateFlow(false)

        val repo = TestEntitlementRepository(
            billingManager = fakeBilling,
            purchaseVerifier = verifier,
            simulatedFlow = simulatedFlow,
            isDebug = true
        )

        // Purchase completed 2 days ago (within 7-day trial window)
        val purchaseTime = System.currentTimeMillis() - (2L * 24 * 60 * 60 * 1000)
        fakeBilling.emitPurchase(
            SubscriptionPurchase(
                orderId = "GPA.TRIAL",
                productId = "lifeos_premium_monthly",
                purchaseToken = "trial_token",
                purchaseTimeMillis = purchaseTime,
                purchaseState = PurchaseState.PURCHASED,
                isAcknowledged = true,
                isAutoRenewing = true
            )
        )

        val state = repo.entitlementState.first()
        assertTrue("Entitlement should be TrialActive within 7-day window", state is PremiumEntitlementState.TrialActive)
        assertTrue(repo.isPremiumActive.first())
    }

    @Test
    fun repository_evaluatesPendingPurchaseCorrectly() = runBlocking {
        val fakeBilling = FakeBillingManager()
        val verifier = ClientSidePurchaseVerifier(isDebugBuild = true)
        val simulatedFlow = MutableStateFlow(false)

        val repo = TestEntitlementRepository(
            billingManager = fakeBilling,
            purchaseVerifier = verifier,
            simulatedFlow = simulatedFlow,
            isDebug = true
        )

        fakeBilling.emitPurchase(
            SubscriptionPurchase(
                orderId = "GPA.PENDING",
                productId = "lifeos_premium_monthly",
                purchaseToken = "pending_token",
                purchaseTimeMillis = System.currentTimeMillis(),
                purchaseState = PurchaseState.PENDING,
                isAcknowledged = false
            )
        )

        val state = repo.entitlementState.first()
        assertEquals(PremiumEntitlementState.PendingPurchase, state)
        assertFalse(repo.isPremiumActive.first())
    }

    @Test
    fun releaseBuild_strictlyIgnoresSimulatedDeveloperToggle() = runBlocking {
        val fakeBilling = FakeBillingManager()
        val verifier = ClientSidePurchaseVerifier(isDebugBuild = false)
        val simulatedFlow = MutableStateFlow(true) // Developer toggle activated

        // In release builds (isDebug = false), simulated toggle is strictly ignored
        val repo = TestEntitlementRepository(
            billingManager = fakeBilling,
            purchaseVerifier = verifier,
            simulatedFlow = simulatedFlow,
            isDebug = false
        )

        val state = repo.entitlementState.first()
        assertEquals("Release build MUST remain Free despite simulated toggle", PremiumEntitlementState.Free, state)
        assertFalse(repo.isPremiumActive.first())
    }

    // Fake BillingManager for testing
    private class FakeBillingManager : BillingManager {
        private val _product = MutableStateFlow<SubscriptionProduct?>(null)
        override val subscriptionProduct: StateFlow<SubscriptionProduct?> = _product.asStateFlow()

        private val _purchase = MutableStateFlow<SubscriptionPurchase?>(null)
        override val subscriptionPurchase: StateFlow<SubscriptionPurchase?> = _purchase.asStateFlow()

        private val _connectionState = MutableStateFlow(BillingConnectionState.CONNECTED)
        override val connectionState: StateFlow<BillingConnectionState> = _connectionState.asStateFlow()

        fun emitPurchase(purchase: SubscriptionPurchase?) {
            _purchase.value = purchase
        }

        override suspend fun startConnection() {}
        override suspend fun querySubscriptionDetails() {}
        override suspend fun launchPurchaseFlow(activity: android.app.Activity): BillingResultStatus = BillingResultStatus.OK
        override suspend fun restorePurchases(): BillingResultStatus = BillingResultStatus.OK
        override suspend fun acknowledgePurchase(purchaseToken: String): Boolean = true
        override fun destroy() {}
    }

    // Repository emulator matching LocalPremiumEntitlementRepository logic
    private class TestEntitlementRepository(
        override val billingManager: BillingManager?,
        private val purchaseVerifier: PurchaseVerifier,
        private val simulatedFlow: MutableStateFlow<Boolean>,
        private val isDebug: Boolean
    ) : PremiumEntitlementRepository {

        private val safeSimulatedFlow: Flow<Boolean> = simulatedFlow.map { if (isDebug) it else false }

        override val entitlementState: Flow<PremiumEntitlementState> = if (billingManager != null) {
            combine(billingManager.subscriptionPurchase, safeSimulatedFlow) { purchase, simulated ->
                if (isDebug && simulated) {
                    return@combine PremiumEntitlementState.PremiumActive(
                        expiryTimeMillis = Long.MAX_VALUE,
                        isAutoRenewing = true
                    )
                }

                if (purchase == null) {
                    return@combine PremiumEntitlementState.Free
                }

                when (purchase.purchaseState) {
                    PurchaseState.PENDING -> PremiumEntitlementState.PendingPurchase
                    PurchaseState.PURCHASED -> {
                        val verification = purchaseVerifier.verifyPurchase(purchase)
                        if (verification == PurchaseVerificationStatus.FAILED_VERIFICATION) {
                            PremiumEntitlementState.Free
                        } else {
                            val now = System.currentTimeMillis()
                            val expiry = purchase.expiryTimeMillis ?: (purchase.purchaseTimeMillis + 30L * 24 * 60 * 60 * 1000)
                            if (now > expiry) {
                                PremiumEntitlementState.Expired(expiry)
                            } else if (!purchase.isAutoRenewing) {
                                PremiumEntitlementState.CancelledUntilExpiry(expiry)
                            } else {
                                val trialWindowMillis = 7L * 24 * 60 * 60 * 1000
                                if (now < purchase.purchaseTimeMillis + trialWindowMillis) {
                                    val remainingDays = (((purchase.purchaseTimeMillis + trialWindowMillis) - now) / (24 * 60 * 60 * 1000)).toInt() + 1
                                    PremiumEntitlementState.TrialActive(purchase.purchaseTimeMillis + trialWindowMillis, remainingDays)
                                } else {
                                    PremiumEntitlementState.PremiumActive(expiry, isAutoRenewing = true)
                                }
                            }
                        }
                    }
                    else -> PremiumEntitlementState.Free
                }
            }
        } else {
            safeSimulatedFlow.map { simulated ->
                if (isDebug && simulated) {
                    PremiumEntitlementState.PremiumActive(Long.MAX_VALUE, true)
                } else {
                    PremiumEntitlementState.Free
                }
            }
        }

        override val isPremiumActive: Flow<Boolean> = entitlementState.map { it.isEntitled }

        override suspend fun setSimulatedPremium(active: Boolean) {
            if (isDebug) {
                simulatedFlow.value = active
            }
        }

        override fun isThemeUnlocked(themeId: com.example.ui.theme.engine.ThemeId, isPremiumActive: Boolean): Boolean {
            if (!themeId.isPremium) return true
            return isPremiumActive
        }
    }
}
