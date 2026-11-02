package com.example.billing

import com.example.ui.theme.engine.PremiumEntitlementRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Comprehensive unit tests for Release 4 Phase P5-L3:
 * Subscription Lifecycle, Purchase Verification, Offline Trust Policy,
 * Idempotent Recovery, and Failure Modes.
 */
class PremiumEntitlementUnitTest {

    @Test
    fun freeUser_hasNoEntitlement() {
        val state = PremiumEntitlementState.Free
        assertFalse("Free state must not be entitled", state.isEntitled)
    }

    @Test
    fun purchaseInitiated_hasNoEntitlementYet() {
        val state = PremiumEntitlementState.PurchaseInitiated
        assertFalse("PurchaseInitiated must not be entitled prematurely", state.isEntitled)
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
    fun verificationFailed_revokesEntitlement() {
        val state = PremiumEntitlementState.VerificationFailed("Tampered token")
        assertFalse("Verification failed must not be entitled", state.isEntitled)
        assertEquals("Tampered token", state.reason)
    }

    @Test
    fun offlineTrustExpired_revokesEntitlement() {
        val state = PremiumEntitlementState.OfflineTrustExpired(System.currentTimeMillis() - 86400000L * 5)
        assertFalse("Offline trust expired must revoke entitlement", state.isEntitled)
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
    fun tokenRedaction_masksRawTokenForPrivacy() {
        val purchase = SubscriptionPurchase(
            orderId = "GPA.1234",
            productId = "lifeos_premium_monthly",
            purchaseToken = "inapp_billing_secret_token_abcdef123456",
            purchaseTimeMillis = System.currentTimeMillis(),
            purchaseState = PurchaseState.PURCHASED,
            isAcknowledged = true
        )

        val redacted = purchase.redactedToken
        assertFalse("Redacted token must NOT contain full secret", redacted.contains("secret_token"))
        assertTrue("Redacted token must start with prefix", redacted.startsWith("inap..."))
        assertNotEquals("inapp_billing_secret_token_abcdef123456", redacted)
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
                expiryTimeMillis = expiryTime,
                lastVerifiedTimeMillis = System.currentTimeMillis()
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
                isAutoRenewing = true,
                isTrial = true,
                lastVerifiedTimeMillis = System.currentTimeMillis()
            )
        )

        val state = repo.entitlementState.first()
        assertTrue("Entitlement should be TrialActive within 7-day window", state is PremiumEntitlementState.TrialActive)
        assertTrue(repo.isPremiumActive.first())
    }

    @Test
    fun repository_evaluatesIneligibleTrialUser_transitionsToStandardPaid() = runBlocking {
        val fakeBilling = FakeBillingManager()
        val verifier = ClientSidePurchaseVerifier(isDebugBuild = true)
        val simulatedFlow = MutableStateFlow(false)

        val repo = TestEntitlementRepository(
            billingManager = fakeBilling,
            purchaseVerifier = verifier,
            simulatedFlow = simulatedFlow,
            isDebug = true
        )

        // User is ineligible for trial (isTrial = false, isEligibleForTrial = false)
        val purchaseTime = System.currentTimeMillis() - (1L * 24 * 60 * 60 * 1000)
        val expiryTime = purchaseTime + (30L * 24 * 60 * 60 * 1000)
        fakeBilling.emitPurchase(
            SubscriptionPurchase(
                orderId = "GPA.STANDARD",
                productId = "lifeos_premium_monthly",
                purchaseToken = "standard_token",
                purchaseTimeMillis = purchaseTime,
                purchaseState = PurchaseState.PURCHASED,
                isAcknowledged = true,
                isAutoRenewing = true,
                isTrial = false,
                isEligibleForTrial = false,
                expiryTimeMillis = expiryTime,
                lastVerifiedTimeMillis = System.currentTimeMillis()
            )
        )

        val state = repo.entitlementState.first()
        assertTrue("Ineligible trial user immediately receives standard PremiumActive without trial promise", state is PremiumEntitlementState.PremiumActive)
        assertTrue(repo.isPremiumActive.first())
    }

    @Test
    fun repository_evaluatesSubscriptionRenewal_preservesEntitlement() = runBlocking {
        val fakeBilling = FakeBillingManager()
        val verifier = ClientSidePurchaseVerifier(isDebugBuild = true)
        val simulatedFlow = MutableStateFlow(false)

        val repo = TestEntitlementRepository(
            billingManager = fakeBilling,
            purchaseVerifier = verifier,
            simulatedFlow = simulatedFlow,
            isDebug = true
        )

        // Month 1 purchase
        val month1Time = System.currentTimeMillis() - (25L * 24 * 60 * 60 * 1000)
        val month1Expiry = month1Time + (30L * 24 * 60 * 60 * 1000)
        fakeBilling.emitPurchase(
            SubscriptionPurchase(
                orderId = "GPA.M1",
                productId = "lifeos_premium_monthly",
                purchaseToken = "renew_token",
                purchaseTimeMillis = month1Time,
                purchaseState = PurchaseState.PURCHASED,
                isAcknowledged = true,
                isAutoRenewing = true,
                expiryTimeMillis = month1Expiry,
                lastVerifiedTimeMillis = System.currentTimeMillis()
            )
        )

        val state1 = repo.entitlementState.first()
        assertTrue(state1.isEntitled)

        // Month 2 Renewal event from Google Play: expiry extended by 30 days
        val month2Expiry = month1Expiry + (30L * 24 * 60 * 60 * 1000)
        fakeBilling.emitPurchase(
            SubscriptionPurchase(
                orderId = "GPA.M2",
                productId = "lifeos_premium_monthly",
                purchaseToken = "renew_token",
                purchaseTimeMillis = month1Time,
                purchaseState = PurchaseState.PURCHASED,
                isAcknowledged = true,
                isAutoRenewing = true,
                expiryTimeMillis = month2Expiry,
                lastVerifiedTimeMillis = System.currentTimeMillis()
            )
        )

        val state2 = repo.entitlementState.first()
        assertTrue("Renewed subscription must maintain entitlement with updated expiry", state2 is PremiumEntitlementState.PremiumActive)
        assertEquals(month2Expiry, (state2 as PremiumEntitlementState.PremiumActive).expiryTimeMillis)
        assertTrue(state2.isEntitled)
    }

    @Test
    fun repository_evaluatesGracePeriodAndAccountHold() = runBlocking {
        val fakeBilling = FakeBillingManager()
        val verifier = ClientSidePurchaseVerifier(isDebugBuild = true)
        val simulatedFlow = MutableStateFlow(false)

        val repo = TestEntitlementRepository(
            billingManager = fakeBilling,
            purchaseVerifier = verifier,
            simulatedFlow = simulatedFlow,
            isDebug = true
        )

        // Grace period: payment failed but in grace window -> retains entitlement
        val graceExpiry = System.currentTimeMillis() + (3L * 24 * 60 * 60 * 1000)
        fakeBilling.emitPurchase(
            SubscriptionPurchase(
                orderId = "GPA.GRACE",
                productId = "lifeos_premium_monthly",
                purchaseToken = "grace_token",
                purchaseTimeMillis = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000),
                purchaseState = PurchaseState.PURCHASED,
                isAcknowledged = true,
                isInGracePeriod = true,
                expiryTimeMillis = graceExpiry,
                lastVerifiedTimeMillis = System.currentTimeMillis()
            )
        )

        val graceState = repo.entitlementState.first()
        assertTrue("Grace period should retain entitlement", graceState is PremiumEntitlementState.GracePeriod)
        assertTrue(graceState.isEntitled)

        // Account hold: payment unresolved, access suspended -> revokes entitlement
        fakeBilling.emitPurchase(
            SubscriptionPurchase(
                orderId = "GPA.HOLD",
                productId = "lifeos_premium_monthly",
                purchaseToken = "hold_token",
                purchaseTimeMillis = System.currentTimeMillis() - (35L * 24 * 60 * 60 * 1000),
                purchaseState = PurchaseState.PURCHASED,
                isAcknowledged = true,
                isInAccountHold = true,
                lastVerifiedTimeMillis = System.currentTimeMillis()
            )
        )

        val holdState = repo.entitlementState.first()
        assertEquals(PremiumEntitlementState.AccountHold, holdState)
        assertFalse("Account hold must revoke entitlement", holdState.isEntitled)
    }

    @Test
    fun repository_evaluatesOfflineTrustPolicy() = runBlocking {
        val fakeBilling = FakeBillingManager()
        val verifier = ClientSidePurchaseVerifier(isDebugBuild = true)
        val simulatedFlow = MutableStateFlow(false)

        // Test with 72-hour offline trust window
        val trustDuration = 72L * 60 * 60 * 1000L
        val repo = TestEntitlementRepository(
            billingManager = fakeBilling,
            purchaseVerifier = verifier,
            simulatedFlow = simulatedFlow,
            isDebug = true,
            offlineTrustDurationMillis = trustDuration
        )

        val now = System.currentTimeMillis()

        // Case 1: Verified 24 hours ago (well within 72-hour offline trust) -> Entitled
        fakeBilling.emitPurchase(
            SubscriptionPurchase(
                orderId = "GPA.OFFLINE1",
                productId = "lifeos_premium_monthly",
                purchaseToken = "offline_token",
                purchaseTimeMillis = now - (10L * 24 * 60 * 60 * 1000),
                purchaseState = PurchaseState.PURCHASED,
                isAcknowledged = true,
                expiryTimeMillis = now + (20L * 24 * 60 * 60 * 1000),
                lastVerifiedTimeMillis = now - (24L * 60 * 60 * 1000)
            )
        )

        val stateOnline = repo.entitlementState.first()
        assertTrue("Within 72h offline trust window, entitlement is retained", stateOnline.isEntitled)

        // Case 2: Subscription expired AND offline without verification for > 72 hours -> OfflineTrustExpired
        val expiredTime = now - (5L * 60 * 60 * 1000)
        val staleVerificationTime = now - (80L * 60 * 60 * 1000) // 80 hours ago
        fakeBilling.emitPurchase(
            SubscriptionPurchase(
                orderId = "GPA.OFFLINE2",
                productId = "lifeos_premium_monthly",
                purchaseToken = "offline_stale_token",
                purchaseTimeMillis = now - (35L * 24 * 60 * 60 * 1000),
                purchaseState = PurchaseState.PURCHASED,
                isAcknowledged = true,
                expiryTimeMillis = expiredTime,
                lastVerifiedTimeMillis = staleVerificationTime
            )
        )

        val stateExpired = repo.entitlementState.first()
        assertTrue("Past offline trust window and past expiry, state must be OfflineTrustExpired", stateExpired is PremiumEntitlementState.OfflineTrustExpired)
        assertFalse(stateExpired.isEntitled)
    }

    @Test
    fun purchaseAcknowledgement_orchestratedPostVerification() = runBlocking {
        val fakeBilling = FakeBillingManager()
        val verifier = ClientSidePurchaseVerifier(isDebugBuild = true)
        val simulatedFlow = MutableStateFlow(false)

        val repo = TestEntitlementRepository(
            billingManager = fakeBilling,
            purchaseVerifier = verifier,
            simulatedFlow = simulatedFlow,
            isDebug = true
        )

        // New purchase: unacknowledged
        fakeBilling.emitPurchase(
            SubscriptionPurchase(
                orderId = "GPA.ACK",
                productId = "lifeos_premium_monthly",
                purchaseToken = "unack_token",
                purchaseTimeMillis = System.currentTimeMillis(),
                purchaseState = PurchaseState.PURCHASED,
                isAcknowledged = false,
                isAutoRenewing = true,
                lastVerifiedTimeMillis = System.currentTimeMillis()
            )
        )

        val state = repo.entitlementState.first()
        assertTrue(state.isEntitled)

        // Verify that acknowledgment was triggered after verification
        assertTrue("Purchase acknowledgement must be invoked post-verification", fakeBilling.acknowledgedTokens.contains("unack_token"))
    }

    @Test
    fun failedVerification_doesNotAcknowledgePurchase() = runBlocking {
        val fakeBilling = FakeBillingManager()
        // Verifier that rejects empty or invalid tokens
        val verifier = ClientSidePurchaseVerifier(isDebugBuild = false)
        val simulatedFlow = MutableStateFlow(false)

        val repo = TestEntitlementRepository(
            billingManager = fakeBilling,
            purchaseVerifier = verifier,
            simulatedFlow = simulatedFlow,
            isDebug = false
        )

        // Purchase with empty token (fails verification)
        fakeBilling.emitPurchase(
            SubscriptionPurchase(
                orderId = "GPA.FAIL",
                productId = "lifeos_premium_monthly",
                purchaseToken = "",
                purchaseTimeMillis = System.currentTimeMillis(),
                purchaseState = PurchaseState.PURCHASED,
                isAcknowledged = false,
                lastVerifiedTimeMillis = System.currentTimeMillis()
            )
        )

        val state = repo.entitlementState.first()
        assertTrue("Failed verification yields VerificationFailed state", state is PremiumEntitlementState.VerificationFailed)
        assertFalse(state.isEntitled)
        assertFalse("Unverified purchase must NEVER be acknowledged", fakeBilling.acknowledgedTokens.contains(""))
    }

    @Test
    fun duplicateNotificationAndReconciliation_isIdempotent() = runBlocking {
        val fakeBilling = FakeBillingManager()
        val verifier = ClientSidePurchaseVerifier(isDebugBuild = true)
        val simulatedFlow = MutableStateFlow(false)

        val repo = TestEntitlementRepository(
            billingManager = fakeBilling,
            purchaseVerifier = verifier,
            simulatedFlow = simulatedFlow,
            isDebug = true
        )

        val purchase = SubscriptionPurchase(
            orderId = "GPA.IDEMPOTENT",
            productId = "lifeos_premium_monthly",
            purchaseToken = "idempotent_token",
            purchaseTimeMillis = System.currentTimeMillis(),
            purchaseState = PurchaseState.PURCHASED,
            isAcknowledged = true,
            isAutoRenewing = true,
            expiryTimeMillis = System.currentTimeMillis() + 86400000L * 30,
            lastVerifiedTimeMillis = System.currentTimeMillis()
        )

        // First emission
        fakeBilling.emitPurchase(purchase)
        val state1 = repo.entitlementState.first()
        assertTrue(state1.isEntitled)

        // Duplicate emission (e.g. repeated broadcast or multiple query callbacks)
        fakeBilling.emitPurchase(purchase)
        val state2 = repo.entitlementState.first()
        assertEquals(state1, state2)
    }

    @Test
    fun restorePurchases_onFreshInstall_restoresEntitlement() = runBlocking {
        val fakeBilling = FakeBillingManager()
        val verifier = ClientSidePurchaseVerifier(isDebugBuild = true)
        val simulatedFlow = MutableStateFlow(false)

        val repo = TestEntitlementRepository(
            billingManager = fakeBilling,
            purchaseVerifier = verifier,
            simulatedFlow = simulatedFlow,
            isDebug = true
        )

        // App starts fresh: Free
        assertEquals(PremiumEntitlementState.Free, repo.entitlementState.first())

        // User triggers "Restore Purchases", Google Play queries active purchase
        val status = fakeBilling.restorePurchases()
        assertEquals(BillingResultStatus.OK, status)

        fakeBilling.emitPurchase(
            SubscriptionPurchase(
                orderId = "GPA.RESTORE",
                productId = "lifeos_premium_monthly",
                purchaseToken = "restored_token",
                purchaseTimeMillis = System.currentTimeMillis() - 86400000L * 5,
                purchaseState = PurchaseState.PURCHASED,
                isAcknowledged = true,
                isAutoRenewing = true,
                expiryTimeMillis = System.currentTimeMillis() + 86400000L * 25,
                lastVerifiedTimeMillis = System.currentTimeMillis()
            )
        )

        val restoredState = repo.entitlementState.first()
        assertTrue("Restored purchase successfully reactivates entitlement on fresh install", restoredState.isEntitled)
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

    // Fake BillingManager for lifecycle testing
    private class FakeBillingManager : BillingManager {
        private val _product = MutableStateFlow<SubscriptionProduct?>(null)
        override val subscriptionProduct: StateFlow<SubscriptionProduct?> = _product.asStateFlow()

        private val _purchase = MutableStateFlow<SubscriptionPurchase?>(null)
        override val subscriptionPurchase: StateFlow<SubscriptionPurchase?> = _purchase.asStateFlow()

        private val _connectionState = MutableStateFlow(BillingConnectionState.CONNECTED)
        override val connectionState: StateFlow<BillingConnectionState> = _connectionState.asStateFlow()

        val acknowledgedTokens = mutableSetOf<String>()

        fun emitPurchase(purchase: SubscriptionPurchase?) {
            _purchase.value = purchase
        }

        override suspend fun startConnection() {}
        override suspend fun querySubscriptionDetails() {}
        override suspend fun launchPurchaseFlow(activity: android.app.Activity): BillingResultStatus = BillingResultStatus.OK
        override suspend fun restorePurchases(): BillingResultStatus = BillingResultStatus.OK
        override suspend fun acknowledgePurchase(purchaseToken: String): Boolean {
            acknowledgedTokens.add(purchaseToken)
            return true
        }
        override fun destroy() {}
    }

    // Repository emulator matching LocalPremiumEntitlementRepository logic
    private class TestEntitlementRepository(
        override val billingManager: BillingManager?,
        private val purchaseVerifier: PurchaseVerifier,
        private val simulatedFlow: MutableStateFlow<Boolean>,
        private val isDebug: Boolean,
        private val offlineTrustDurationMillis: Long = 72L * 60 * 60 * 1000L,
        private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
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
                            PremiumEntitlementState.VerificationFailed("Purchase verification rejected or invalid token")
                        } else {
                            // Acknowledge verified purchase if unacknowledged
                            if (!purchase.isAcknowledged) {
                                coroutineScope.launch {
                                    billingManager.acknowledgePurchase(purchase.purchaseToken)
                                }
                            }

                            if (purchase.isInAccountHold) {
                                return@combine PremiumEntitlementState.AccountHold
                            }

                            val now = System.currentTimeMillis()
                            val expiry = purchase.expiryTimeMillis ?: (purchase.purchaseTimeMillis + 30L * 24 * 60 * 60 * 1000)

                            if (purchase.isInGracePeriod && now < expiry) {
                                return@combine PremiumEntitlementState.GracePeriod(expiry)
                            }

                            val lastVerified = purchase.lastVerifiedTimeMillis
                            if (lastVerified != null && (now - lastVerified > offlineTrustDurationMillis) && now > expiry) {
                                return@combine PremiumEntitlementState.OfflineTrustExpired(lastVerified)
                            }

                            if (now > expiry) {
                                PremiumEntitlementState.Expired(expiry)
                            } else if (!purchase.isAutoRenewing) {
                                PremiumEntitlementState.CancelledUntilExpiry(expiry)
                            } else {
                                val trialWindowMillis = 7L * 24 * 60 * 60 * 1000
                                if (purchase.isTrial || (purchase.isEligibleForTrial && now < purchase.purchaseTimeMillis + trialWindowMillis)) {
                                    val remainingDays = (((purchase.purchaseTimeMillis + trialWindowMillis) - now) / (24 * 60 * 60 * 1000)).toInt() + 1
                                    PremiumEntitlementState.TrialActive(purchase.purchaseTimeMillis + trialWindowMillis, remainingDays.coerceAtLeast(1))
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
