package com.example.domain.model

enum class PlanLevel(val displayName: String, val icon: String) {
    DAILY("Daily Plan", "☀️"),
    WEEKLY("Weekly Review & Focus", "📅"),
    MONTHLY("Monthly Objectives", "🗓️"),
    QUARTERLY("Quarterly OKRs", "🎯"),
    YEARLY("Yearly Vision", "🌟");

    companion object {
        fun fromString(value: String): PlanLevel {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: WEEKLY
        }
    }
}

enum class PlanStatus {
    ACTIVE,
    COMPLETED,
    ARCHIVED;

    companion object {
        fun fromString(value: String): PlanStatus {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: ACTIVE
        }
    }
}

data class PlanMilestone(
    val id: Long = 0,
    val planId: Long,
    val title: String,
    val isCompleted: Boolean = false,
    val orderIndex: Int = 0
)

data class PlanItem(
    val id: Long = 0,
    val profileId: Long = 1,
    val title: String,
    val description: String = "",
    val level: PlanLevel = PlanLevel.WEEKLY,
    val startDateIso: String,
    val targetDateIso: String,
    val status: PlanStatus = PlanStatus.ACTIVE,
    val manualProgress: Int = 0,
    val isAutoProgress: Boolean = true,
    val parentPlanId: Long? = null,
    val projectId: Long? = null,
    val priority: String = "MEDIUM",
    val milestones: List<PlanMilestone> = emptyList(),
    val linkedTaskIds: List<Long> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
) {
    val progressFraction: Float
        get() {
            if (status == PlanStatus.COMPLETED) return 1f
            if (!isAutoProgress) return (manualProgress.coerceIn(0, 100) / 100f)
            if (milestones.isEmpty()) return 0f
            val done = milestones.count { it.isCompleted }
            return done.toFloat() / milestones.size.toFloat()
        }
}

data class TrashItem(
    val id: Long = 0,
    val profileId: Long = 1,
    val itemType: String, // "TASK", "PROJECT", "LABEL", "HABIT", "PLAN"
    val originalId: Long,
    val title: String,
    val subtitle: String = "",
    val originalPayloadJson: String,
    val deletedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long
) {
    val daysRemaining: Int
        get() {
            val diffMs = expiresAt - System.currentTimeMillis()
            return (diffMs / (1000 * 60 * 60 * 24)).coerceAtLeast(0).toInt()
        }
}

enum class HunterRank(
    val rankCode: String,
    val title: String,
    val badgeColorHex: Long,
    val minLevel: Int
) {
    E("E", "Novice Hunter", 0xFF9CA3AF, 1),
    D("D", "Apprentice Hunter", 0xFF10B981, 6),
    C("C", "Adept Hunter", 0xFF3B82F6, 13),
    B("B", "Expert Hunter", 0xFF8B5CF6, 21),
    A("A", "Master Hunter", 0xFFF59E0B, 31),
    S("S", "Grandmaster", 0xFFEF4444, 46),
    SS("SS", "Shadow Monarch", 0xFF8B5CF6, 61),
    SSS("SSS", "Apex Ruler", 0xFFFFD700, 81);

    companion object {
        fun fromLevel(level: Int): HunterRank {
            return entries.findLast { level >= it.minLevel } ?: E
        }

        fun fromString(value: String): HunterRank {
            return entries.find { it.rankCode.equals(value, ignoreCase = true) } ?: E
        }
    }
}

data class GamificationProfile(
    val currentXp: Long = 0,
    val currentCoins: Int = 0,
    val currentLevel: Int = 1,
    val hunterRank: HunterRank = HunterRank.E
) {
    val xpForCurrentLevel: Long
        get() = (currentLevel * 100L)

    val xpProgressInLevel: Long
        get() {
            var lvl = 1
            var accumulated = 0L
            while (lvl < currentLevel) {
                accumulated += (lvl * 100L)
                lvl++
            }
            return (currentXp - accumulated).coerceAtLeast(0L)
        }

    val levelProgressFraction: Float
        get() {
            val needed = xpForCurrentLevel
            return if (needed > 0) (xpProgressInLevel.toFloat() / needed.toFloat()).coerceIn(0f, 1f) else 1f
        }
}

data class AchievementItem(
    val code: String,
    val title: String,
    val description: String,
    val tier: String, // "BRONZE", "SILVER", "GOLD", "PLATINUM"
    val xpReward: Int,
    val coinsReward: Int,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null
)

data class RewardItem(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val coinCost: Int = 50,
    val iconEmoji: String = "🎁",
    val isArchived: Boolean = false,
    val redemptionCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

object DefaultAchievements {
    val list = listOf(
        AchievementItem("FIRST_TASK", "First Blood", "Complete your very first task", "BRONZE", 50, 10),
        AchievementItem("TASK_10", "Momentum", "Complete 10 total tasks", "BRONZE", 100, 20),
        AchievementItem("TASK_50", "Centurion", "Complete 50 tasks", "SILVER", 250, 50),
        AchievementItem("TASK_100", "Century Club", "Complete 100 tasks", "GOLD", 500, 100),
        AchievementItem("STREAK_3", "Spark", "Complete habits 3 days in a row", "BRONZE", 75, 15),
        AchievementItem("STREAK_7", "Unstoppable", "Complete habits 7 days in a row", "SILVER", 200, 40),
        AchievementItem("STREAK_30", "Disciplined Mind", "Complete habits 30 days in a row", "GOLD", 600, 150),
        AchievementItem("HYDRATION_HERO", "Hydration Titan", "Reach your 2,000ml water goal 5 times", "BRONZE", 100, 20),
        AchievementItem("CHALLENGE_DAY_7", "First Week of Fire", "Survive 7 days of 75-Day Challenge", "SILVER", 250, 50),
        AchievementItem("CHALLENGE_DAY_30", "Iron Will", "Survive 30 days of 75-Day Challenge", "GOLD", 750, 150),
        AchievementItem("CHALLENGE_COMPLETE", "The Apex Champion", "Complete all 75 days of the challenge", "PLATINUM", 2000, 500),
        AchievementItem("PLANNER_INITIATE", "Master Strategist", "Create your first Weekly or Monthly Plan", "BRONZE", 100, 25),
        AchievementItem("CLEAN_SLATE", "Zero Inbox", "Clear all tasks scheduled for today", "SILVER", 150, 30),
        AchievementItem("TEMPLATE_MASTER", "Template Pioneer", "Install your first offline routine or habit template", "BRONZE", 75, 15),
        AchievementItem("LEVEL_10", "Elite Hunter", "Reach Level 10 and unlock Hunter Rank D", "SILVER", 200, 40)
    )
}

object DefaultRewards {
    val list = listOf(
        RewardItem(title = "Cheat Meal / Sweet Treat", description = "Enjoy a guilt-free delicious meal or dessert", coinCost = 40, iconEmoji = "🍔"),
        RewardItem(title = "Movie or Gaming Night", description = "Spend 2 hours watching your favorite film or gaming", coinCost = 60, iconEmoji = "🍿"),
        RewardItem(title = "Buy a New Book or Gadget", description = "Invest in something that brings you joy", coinCost = 150, iconEmoji = "📚"),
        RewardItem(title = "Guilt-free Rest Afternoon", description = "Take a full afternoon off without productivity guilt", coinCost = 80, iconEmoji = "☕"),
        RewardItem(title = "Outdoor Adventure / Trip", description = "Hike, explore a new coffee shop, or weekend roadtrip", coinCost = 200, iconEmoji = "🏕️")
    )
}
