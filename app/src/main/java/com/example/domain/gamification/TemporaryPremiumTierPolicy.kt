package com.example.domain.gamification

/**
 * Business policy and tiered duration calculator for purchasing Temporary Premium Access
 * via the Gamification Rewards Shop.
 *
 * Tiered duration rules based on total lifetime redemption count:
 * - 1st purchase (count == 0): 3 days (72 hours)
 * - 2nd to 10th purchase (count in 1..9): 1 day (24 hours)
 * - 11th to 20th purchase (count in 10..19): 18 hours
 * - 21st+ purchase (count >= 20): 12 hours
 */
object TemporaryPremiumTierPolicy {
    const val DEFAULT_COIN_COST = 50

    fun calculateDurationMillis(redemptionCount: Int): Long {
        return when {
            redemptionCount <= 0 -> 72L * 60L * 60L * 1000L // 3 days
            redemptionCount in 1..9 -> 24L * 60L * 60L * 1000L // 1 day
            redemptionCount in 10..19 -> 18L * 60L * 60L * 1000L // 18 hours
            else -> 12L * 60L * 60L * 1000L // 12 hours
        }
    }

    fun getDurationLabel(redemptionCount: Int): String {
        return when {
            redemptionCount <= 0 -> "3 Days (72h)"
            redemptionCount in 1..9 -> "1 Day (24h)"
            redemptionCount in 10..19 -> "18 Hours"
            else -> "12 Hours"
        }
    }

    fun getTierDescription(redemptionCount: Int): String {
        return when {
            redemptionCount <= 0 -> "1st Purchase Bonus: 3 Full Days"
            redemptionCount in 1..9 -> "Tier 2 (2nd-10th): 1 Full Day"
            redemptionCount in 10..19 -> "Tier 3 (11th-20th): 18 Hours"
            else -> "Tier 4 (21st+): 12 Hours"
        }
    }
}
