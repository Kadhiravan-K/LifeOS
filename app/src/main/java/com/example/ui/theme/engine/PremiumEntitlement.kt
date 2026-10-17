package com.example.ui.theme.engine

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.entitlementDataStore by preferencesDataStore(name = "lifeos_premium_entitlement")

/**
 * Repository interface for managing user entitlement state.
 * Core task management, habits, reminders, and accessibility features are never gated.
 * Premium access is strictly cosmetic (themes, particle animations, custom engravings).
 */
interface PremiumEntitlementRepository {
    val isPremiumActive: Flow<Boolean>
    suspend fun setSimulatedPremium(active: Boolean)
    fun isThemeUnlocked(themeId: ThemeId, isPremiumActive: Boolean): Boolean
}

class LocalPremiumEntitlementRepository(
    private val context: Context
) : PremiumEntitlementRepository {

    companion object {
        private val KEY_SIMULATED_PREMIUM = booleanPreferencesKey("simulated_developer_premium")
    }

    override val isPremiumActive: Flow<Boolean> = context.entitlementDataStore.data.map { prefs ->
        // In local development simulator mode, reads local simulated toggle.
        prefs[KEY_SIMULATED_PREMIUM] ?: false
    }

    override suspend fun setSimulatedPremium(active: Boolean) {
        context.entitlementDataStore.edit { prefs ->
            prefs[KEY_SIMULATED_PREMIUM] = active
        }
    }

    override fun isThemeUnlocked(themeId: ThemeId, isPremiumActive: Boolean): Boolean {
        // Free theme is always unlocked for everyone.
        if (!themeId.isPremium) return true
        // Premium themes are unlocked when user has active entitlement (or developer simulation).
        return isPremiumActive
    }
}
