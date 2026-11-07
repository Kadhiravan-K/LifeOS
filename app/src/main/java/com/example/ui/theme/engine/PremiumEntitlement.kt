package com.example.ui.theme.engine

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.example.BuildConfig
import com.example.billing.BillingManager
import com.example.billing.ClientSidePurchaseVerifier
import com.example.billing.PlayBillingManager
import com.example.billing.PremiumEntitlementState
import com.example.billing.PurchaseState
import com.example.billing.PurchaseVerificationStatus
import com.example.billing.PurchaseVerifier
import com.example.data.preferences.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.entitlementDataStore by preferencesDataStore(name = "lifeos_premium_entitlement")

/**
 * Repository interface for managing user entitlement state.
 * Core task management, habits, reminders, and accessibility features are never gated.
 * Premium access is strictly cosmetic (themes, particle animations, custom engravings).
 */
interface PremiumEntitlementRepository {
    val entitlementState: Flow<PremiumEntitlementState>
    val isPremiumActive: Flow<Boolean>
    val billingManager: BillingManager?
    suspend fun setSimulatedPremium(active: Boolean)
    fun isThemeUnlocked(themeId: ThemeId, isPremiumActive: Boolean): Boolean
}

class LocalPremiumEntitlementRepository(
    private val context: Context,
    override val billingManager: BillingManager? = try {
        PlayBillingManager(context.applicationContext)
    } catch (t: Throwable) {
        android.util.Log.w("PremiumEntitlement", "PlayBillingManager initialization deferred or unavailable: ${t.message}")
        null
    },
    private val purchaseVerifier: PurchaseVerifier = ClientSidePurchaseVerifier(BuildConfig.DEBUG),
    private val coroutineScope: kotlinx.coroutines.CoroutineScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default),
    private val offlineTrustDurationMillis: Long = 72L * 60 * 60 * 1000L, // Recommended 72-hour offline trust duration
    private val userPreferences: UserPreferences = UserPreferences(context.applicationContext)
) : PremiumEntitlementRepository {

    companion object {
        // Legacy simulated key: kept purely for debug testing; strictly ignored in release builds.
        val KEY_SIMULATED_PREMIUM = booleanPreferencesKey("simulated_developer_premium")
    }

    private val simulatedPremiumFlow: Flow<Boolean> = context.entitlementDataStore.data.map { prefs ->
        if (BuildConfig.DEBUG) {
            prefs[KEY_SIMULATED_PREMIUM] ?: false
        } else {
            // CRITICAL: Production release builds NEVER trust a local simulated DataStore flag.
            false
        }
    }

    override val entitlementState: Flow<PremiumEntitlementState> = if (billingManager != null) {
        combine(
            billingManager.subscriptionPurchase,
            simulatedPremiumFlow,
            userPreferences.isLifetimePremiumActive,
            userPreferences.temporaryPremiumExpiry
        ) { purchase, simulatedDebug, isLifetime, tempExpiry ->
            if (isLifetime) {
                return@combine PremiumEntitlementState.PremiumActive(
                    expiryTimeMillis = Long.MAX_VALUE,
                    isAutoRenewing = false
                )
            }

            val now = System.currentTimeMillis()
            if (tempExpiry > now) {
                return@combine PremiumEntitlementState.PremiumActive(
                    expiryTimeMillis = tempExpiry,
                    isAutoRenewing = false
                )
            }

            if (BuildConfig.DEBUG && simulatedDebug) {
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
                        // Acknowledge verified purchase if unacknowledged (non-blocking)
                        if (!purchase.isAcknowledged) {
                            coroutineScope.launch {
                                billingManager.acknowledgePurchase(purchase.purchaseToken)
                            }
                        }

                        if (purchase.isInAccountHold) {
                            return@combine PremiumEntitlementState.AccountHold
                        }

                        val expiry = purchase.expiryTimeMillis ?: (purchase.purchaseTimeMillis + 30L * 24 * 60 * 60 * 1000)

                        if (purchase.isInGracePeriod && now < expiry) {
                            return@combine PremiumEntitlementState.GracePeriod(expiry)
                        }

                        // Evaluate offline trust window if purchase has a last verified timestamp
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
                            if (purchase.isTrial || (now < purchase.purchaseTimeMillis + trialWindowMillis)) {
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
        combine(
            simulatedPremiumFlow,
            userPreferences.isLifetimePremiumActive,
            userPreferences.temporaryPremiumExpiry
        ) { simulated, isLifetime, tempExpiry ->
            if (isLifetime) {
                PremiumEntitlementState.PremiumActive(Long.MAX_VALUE, false)
            } else if (tempExpiry > System.currentTimeMillis()) {
                PremiumEntitlementState.PremiumActive(tempExpiry, false)
            } else if (BuildConfig.DEBUG && simulated) {
                PremiumEntitlementState.PremiumActive(Long.MAX_VALUE, true)
            } else {
                PremiumEntitlementState.Free
            }
        }
    }

    override val isPremiumActive: Flow<Boolean> = entitlementState.map { it.isEntitled }

    override suspend fun setSimulatedPremium(active: Boolean) {
        // Enforce security safeguard: simulated toggle can ONLY be modified in debug builds.
        if (!BuildConfig.DEBUG) return

        context.entitlementDataStore.edit { prefs ->
            prefs[KEY_SIMULATED_PREMIUM] = active
        }
    }

    override fun isThemeUnlocked(themeId: ThemeId, isPremiumActive: Boolean): Boolean {
        // Free theme is always unlocked for everyone.
        if (!themeId.isPremium) return true
        // Premium themes are unlocked when user has active entitlement.
        return isPremiumActive
    }
}
