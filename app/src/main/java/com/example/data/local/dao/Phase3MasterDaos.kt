package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entities.AchievementEntity
import com.example.data.local.entities.GamificationProfileEntity
import com.example.data.local.entities.PlanEntity
import com.example.data.local.entities.PlanMilestoneEntity
import com.example.data.local.entities.PlanTaskCrossRef
import com.example.data.local.entities.RewardItemEntity
import com.example.data.local.entities.RewardRedemptionEntity
import com.example.data.local.entities.TrashItemEntity
import com.example.data.local.entities.XpEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrashDao {
    @Query("SELECT * FROM trash_items ORDER BY deletedAt DESC")
    fun getAllTrashItems(): Flow<List<TrashItemEntity>>

    @Query("SELECT * FROM trash_items ORDER BY deletedAt DESC")
    suspend fun getAllTrashItemsList(): List<TrashItemEntity>

    @Query("SELECT * FROM trash_items WHERE id = :id LIMIT 1")
    suspend fun getTrashItemById(id: Long): TrashItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrashItem(item: TrashItemEntity): Long

    @Query("DELETE FROM trash_items WHERE id = :id")
    suspend fun deleteTrashItemById(id: Long)

    @Query("DELETE FROM trash_items")
    suspend fun clearTrash()

    @Query("DELETE FROM trash_items WHERE expiresAt > 0 AND expiresAt < :currentTimestamp")
    suspend fun cleanupExpiredTrash(currentTimestamp: Long): Int
}

@Dao
interface PlanDao {
    @Query("SELECT * FROM plans ORDER BY createdAt DESC")
    fun getAllPlans(): Flow<List<PlanEntity>>

    @Query("SELECT * FROM plans ORDER BY createdAt DESC")
    suspend fun getAllPlansList(): List<PlanEntity>

    @Query("SELECT * FROM plans WHERE level = :level ORDER BY createdAt DESC")
    fun getPlansByLevel(level: String): Flow<List<PlanEntity>>

    @Query("SELECT * FROM plans WHERE id = :id LIMIT 1")
    suspend fun getPlanById(id: Long): PlanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: PlanEntity): Long

    @Update
    suspend fun updatePlan(plan: PlanEntity)

    @Query("DELETE FROM plans WHERE id = :id")
    suspend fun deletePlanById(id: Long)

    // Milestones
    @Query("SELECT * FROM plan_milestones WHERE planId = :planId ORDER BY orderIndex ASC")
    fun getMilestonesForPlan(planId: Long): Flow<List<PlanMilestoneEntity>>

    @Query("SELECT * FROM plan_milestones WHERE planId = :planId ORDER BY orderIndex ASC")
    suspend fun getMilestonesForPlanList(planId: Long): List<PlanMilestoneEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMilestones(milestones: List<PlanMilestoneEntity>)

    @Query("DELETE FROM plan_milestones WHERE planId = :planId")
    suspend fun deleteMilestonesForPlan(planId: Long)

    // Linked Tasks
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlanTaskCrossRefs(refs: List<PlanTaskCrossRef>)

    @Query("DELETE FROM plan_task_cross_refs WHERE planId = :planId")
    suspend fun clearTasksForPlan(planId: Long)

    @Query("SELECT taskId FROM plan_task_cross_refs WHERE planId = :planId")
    suspend fun getTaskIdsForPlan(planId: Long): List<Long>
}

@Dao
interface GamificationDao {
    @Query("SELECT * FROM gamification_profile WHERE id = 1 LIMIT 1")
    fun getProfileFlow(): Flow<GamificationProfileEntity?>

    @Query("SELECT * FROM gamification_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfile(): GamificationProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: GamificationProfileEntity)

    // XP Events (Anti-abuse idempotency)
    @Query("SELECT * FROM xp_events WHERE eventKey = :eventKey LIMIT 1")
    suspend fun getXpEvent(eventKey: String): XpEventEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertXpEvent(event: XpEventEntity): Long

    @Query("DELETE FROM xp_events WHERE eventKey = :eventKey")
    suspend fun deleteXpEvent(eventKey: String): Int

    @Query("SELECT * FROM xp_events ORDER BY timestamp DESC LIMIT 50")
    fun getRecentXpEvents(): Flow<List<XpEventEntity>>

    // Achievements
    @Query("SELECT * FROM achievements")
    fun getAllAchievements(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE code = :code LIMIT 1")
    suspend fun getAchievement(code: String): AchievementEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAchievements(list: List<AchievementEntity>)

    @Update
    suspend fun updateAchievement(achievement: AchievementEntity)

    // Rewards
    @Query("SELECT * FROM rewards WHERE isArchived = 0 ORDER BY createdAt DESC")
    fun getActiveRewards(): Flow<List<RewardItemEntity>>

    @Query("SELECT * FROM rewards WHERE id = :id LIMIT 1")
    suspend fun getRewardById(id: Long): RewardItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReward(reward: RewardItemEntity): Long

    @Update
    suspend fun updateReward(reward: RewardItemEntity)

    @Query("DELETE FROM rewards WHERE id = :id")
    suspend fun deleteRewardById(id: Long)

    // Redemptions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRedemption(redemption: RewardRedemptionEntity): Long

    @Query("SELECT * FROM reward_redemptions ORDER BY timestamp DESC LIMIT 50")
    fun getRecentRedemptions(): Flow<List<RewardRedemptionEntity>>
}
