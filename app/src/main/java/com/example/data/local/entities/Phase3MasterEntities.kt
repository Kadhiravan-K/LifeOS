package com.example.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Phase 3 Master Entities: Trash, Planning & Gamification.
 */

@Entity(
    tableName = "trash_items",
    indices = [
        Index("profileId"),
        Index("itemType"),
        Index("expiresAt")
    ]
)
data class TrashItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val profileId: Long = 1,
    val itemType: String, // "TASK", "PROJECT", "LABEL", "HABIT", "CHALLENGE", "PLAN"
    val originalId: Long,
    val title: String,
    val subtitle: String = "",
    val originalPayloadJson: String,
    val deletedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long // Timestamp after which it's permanently eligible for cleanup
)

@Entity(
    tableName = "plans",
    indices = [
        Index("profileId"),
        Index("level"),
        Index("status"),
        Index("parentPlanId"),
        Index("projectId")
    ]
)
data class PlanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val profileId: Long = 1,
    val title: String,
    val description: String = "",
    val level: String, // "DAILY", "WEEKLY", "MONTHLY", "QUARTERLY", "YEARLY"
    val startDateIso: String,
    val targetDateIso: String,
    val status: String = "ACTIVE", // "ACTIVE", "COMPLETED", "ARCHIVED"
    val manualProgress: Int = 0, // 0..100
    val isAutoProgress: Boolean = true,
    val parentPlanId: Long? = null,
    val projectId: Long? = null,
    val priority: String = "MEDIUM",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "plan_milestones",
    indices = [Index("planId")]
)
data class PlanMilestoneEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val planId: Long,
    val title: String,
    val isCompleted: Boolean = false,
    val orderIndex: Int = 0
)

@Entity(
    tableName = "plan_task_cross_refs",
    primaryKeys = ["planId", "taskId"],
    indices = [Index("taskId")]
)
data class PlanTaskCrossRef(
    val planId: Long,
    val taskId: Long
)

@Entity(tableName = "gamification_profile")
data class GamificationProfileEntity(
    @PrimaryKey
    val id: Long = 1,
    val currentXp: Long = 0,
    val currentCoins: Int = 0,
    val currentLevel: Int = 1,
    val hunterRank: String = "E", // E, D, C, B, A, S, SS, SSS
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "xp_events")
data class XpEventEntity(
    @PrimaryKey
    val eventKey: String, // Unique key e.g. "TASK_COMPLETE_10_2026-10-17"
    val xpAwarded: Int,
    val coinsAwarded: Int,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey
    val code: String, // "FIRST_TASK", "STREAK_7_DAYS", etc.
    val title: String,
    val description: String,
    val tier: String, // "BRONZE", "SILVER", "GOLD", "PLATINUM"
    val xpReward: Int,
    val coinsReward: Int,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null
)

@Entity(tableName = "rewards")
data class RewardItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val coinCost: Int = 50,
    val iconEmoji: String = "🎁",
    val isArchived: Boolean = false,
    val redemptionCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "reward_redemptions")
data class RewardRedemptionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val rewardId: Long,
    val rewardTitle: String,
    val coinsSpent: Int,
    val timestamp: Long = System.currentTimeMillis()
)
