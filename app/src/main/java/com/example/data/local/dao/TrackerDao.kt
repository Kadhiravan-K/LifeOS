package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.InventoryAdjustmentEntity
import com.example.data.local.entities.TrackerEntity
import com.example.data.local.entities.TrackerPurchaseEntity
import com.example.data.local.entities.UsageCycleEntity
import com.example.data.local.entities.UsageEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackerDao {

    // --- Tracker CRUD ---
    @Query("SELECT * FROM trackers WHERE isArchived = 0 ORDER BY updatedAt DESC")
    fun getActiveTrackersFlow(): Flow<List<TrackerEntity>>

    @Query("SELECT * FROM trackers WHERE profileId = :profileId AND isArchived = 0 ORDER BY updatedAt DESC")
    fun getActiveTrackersForProfileFlow(profileId: Long): Flow<List<TrackerEntity>>

    @Query("SELECT * FROM trackers ORDER BY isArchived ASC, updatedAt DESC")
    fun getAllTrackersFlow(): Flow<List<TrackerEntity>>

    @Query("SELECT * FROM trackers WHERE profileId = :profileId ORDER BY isArchived ASC, updatedAt DESC")
    fun getAllTrackersForProfileFlow(profileId: Long): Flow<List<TrackerEntity>>

    @Query("SELECT * FROM trackers WHERE id = :id LIMIT 1")
    suspend fun getTrackerById(id: Long): TrackerEntity?

    @Query("SELECT * FROM trackers ORDER BY id ASC")
    suspend fun getAllTrackersList(): List<TrackerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracker(tracker: TrackerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackers(trackers: List<TrackerEntity>)

    @Update
    suspend fun updateTracker(tracker: TrackerEntity)

    @Query("UPDATE trackers SET isArchived = :isArchived, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setTrackerArchived(id: Long, isArchived: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM trackers WHERE id = :id")
    suspend fun deleteTrackerById(id: Long)

    @Query("DELETE FROM trackers WHERE profileId = :profileId")
    suspend fun deleteAllTrackersForProfile(profileId: Long)

    @Query("UPDATE trackers SET projectId = NULL WHERE projectId = :projectId")
    suspend fun unassignTrackersFromProject(projectId: Long)

    // --- Usage Cycles ---
    @Query("SELECT * FROM tracker_cycles WHERE trackerId = :trackerId ORDER BY startDateIso DESC, id DESC")
    fun getCyclesForTrackerFlow(trackerId: Long): Flow<List<UsageCycleEntity>>

    @Query("SELECT * FROM tracker_cycles WHERE trackerId = :trackerId ORDER BY startDateIso DESC, id DESC")
    suspend fun getCyclesForTrackerList(trackerId: Long): List<UsageCycleEntity>

    @Query("SELECT * FROM tracker_cycles ORDER BY id ASC")
    suspend fun getAllCyclesList(): List<UsageCycleEntity>

    @Query("SELECT * FROM tracker_cycles WHERE id = :id LIMIT 1")
    suspend fun getCycleById(id: Long): UsageCycleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCycle(cycle: UsageCycleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCycles(cycles: List<UsageCycleEntity>)

    @Update
    suspend fun updateCycle(cycle: UsageCycleEntity)

    @Query("DELETE FROM tracker_cycles WHERE id = :id")
    suspend fun deleteCycleById(id: Long)

    @Query("DELETE FROM tracker_cycles WHERE trackerId = :trackerId")
    suspend fun deleteCyclesForTracker(trackerId: Long)

    // --- Usage Entries ---
    @Query("SELECT * FROM tracker_usage_entries WHERE cycleId = :cycleId ORDER BY dateIso ASC, timestamp ASC")
    fun getEntriesForCycleFlow(cycleId: Long): Flow<List<UsageEntryEntity>>

    @Query("SELECT * FROM tracker_usage_entries WHERE cycleId = :cycleId ORDER BY dateIso ASC, timestamp ASC")
    suspend fun getEntriesForCycleList(cycleId: Long): List<UsageEntryEntity>

    @Query("SELECT * FROM tracker_usage_entries ORDER BY id ASC")
    suspend fun getAllEntriesList(): List<UsageEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsageEntry(entry: UsageEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsageEntries(entries: List<UsageEntryEntity>)

    @Query("DELETE FROM tracker_usage_entries WHERE id = :id")
    suspend fun deleteUsageEntryById(id: Long)

    // --- Purchases ---
    @Query("SELECT * FROM tracker_purchases WHERE trackerId = :trackerId ORDER BY purchaseDateIso DESC")
    fun getPurchasesForTrackerFlow(trackerId: Long): Flow<List<TrackerPurchaseEntity>>

    @Query("SELECT * FROM tracker_purchases WHERE trackerId = :trackerId ORDER BY purchaseDateIso DESC")
    suspend fun getPurchasesForTrackerList(trackerId: Long): List<TrackerPurchaseEntity>

    @Query("SELECT * FROM tracker_purchases ORDER BY id ASC")
    suspend fun getAllPurchasesList(): List<TrackerPurchaseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchase(purchase: TrackerPurchaseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchases(purchases: List<TrackerPurchaseEntity>)

    @Query("DELETE FROM tracker_purchases WHERE id = :id")
    suspend fun deletePurchaseById(id: Long)

    // --- Inventory Adjustments ---
    @Query("SELECT * FROM tracker_inventory_adjustments WHERE trackerId = :trackerId ORDER BY dateIso DESC, timestamp DESC")
    fun getAdjustmentsForTrackerFlow(trackerId: Long): Flow<List<InventoryAdjustmentEntity>>

    @Query("SELECT * FROM tracker_inventory_adjustments WHERE trackerId = :trackerId ORDER BY dateIso DESC, timestamp DESC")
    suspend fun getAdjustmentsForTrackerList(trackerId: Long): List<InventoryAdjustmentEntity>

    @Query("SELECT * FROM tracker_inventory_adjustments ORDER BY id ASC")
    suspend fun getAllAdjustmentsList(): List<InventoryAdjustmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdjustment(adjustment: InventoryAdjustmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdjustments(adjustments: List<InventoryAdjustmentEntity>)

    @Query("DELETE FROM tracker_inventory_adjustments WHERE id = :id")
    suspend fun deleteAdjustmentById(id: Long)
}
