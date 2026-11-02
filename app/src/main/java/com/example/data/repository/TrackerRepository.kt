package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.dao.TrackerDao
import com.example.data.local.entities.InventoryAdjustmentEntity
import com.example.data.local.entities.TrackerEntity
import com.example.data.local.entities.TrackerPurchaseEntity
import com.example.data.local.entities.UsageCycleEntity
import com.example.data.local.entities.UsageEntryEntity
import com.example.data.preferences.UserPreferences
import com.example.domain.tracker.InventoryAdjustmentItem
import com.example.domain.tracker.TrackerItem
import com.example.domain.tracker.TrackerPurchaseItem
import com.example.domain.tracker.UsageCycleItem
import com.example.domain.tracker.UsageEntryItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Dedicated repository for Smart Trackers, usage cycles, consumption logs,
 * purchases, and inventory adjustments (P7-A3 incremental architecture decomposition).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TrackerRepository(
    private val database: AppDatabase,
    private val preferences: UserPreferences,
    private val trackerDao: TrackerDao = database.trackerDao(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    val activeProfileIdFlow: Flow<Long> = preferences.activeProfileId
) {

    suspend fun getActiveProfileId(): Long = activeProfileIdFlow.first()

    // --- Reactive Tracker Streams ---
    val allTrackersFlow: Flow<List<TrackerItem>> = activeProfileIdFlow.flatMapLatest { profileId ->
        trackerDao.getAllTrackersForProfileFlow(profileId).map { list ->
            list.map { it.toDomain() }
        }
    }

    val activeTrackersFlow: Flow<List<TrackerItem>> = activeProfileIdFlow.flatMapLatest { profileId ->
        trackerDao.getActiveTrackersForProfileFlow(profileId).map { list ->
            list.map { it.toDomain() }
        }
    }

    // --- Tracker CRUD ---
    suspend fun getTrackerById(id: Long): TrackerItem? = withContext(ioDispatcher) {
        trackerDao.getTrackerById(id)?.toDomain()
    }

    suspend fun insertTracker(tracker: TrackerItem): Long = withContext(ioDispatcher) {
        val effectiveProfileId = if (tracker.profileId > 0) tracker.profileId else getActiveProfileId()
        trackerDao.insertTracker(TrackerEntity.fromDomain(tracker.copy(profileId = effectiveProfileId)))
    }

    suspend fun updateTracker(tracker: TrackerItem) = withContext(ioDispatcher) {
        trackerDao.updateTracker(TrackerEntity.fromDomain(tracker))
    }

    suspend fun setTrackerArchived(id: Long, isArchived: Boolean) = withContext(ioDispatcher) {
        trackerDao.setTrackerArchived(id, isArchived)
    }

    suspend fun deleteTracker(id: Long) = withContext(ioDispatcher) {
        database.withTransaction {
            trackerDao.deleteCyclesForTracker(id)
            trackerDao.deleteTrackerById(id)
        }
    }

    suspend fun deleteAllTrackersForProfile(profileId: Long) = withContext(ioDispatcher) {
        trackerDao.deleteAllTrackersForProfile(profileId)
    }

    suspend fun unassignTrackersFromProject(projectId: Long) = withContext(ioDispatcher) {
        trackerDao.unassignTrackersFromProject(projectId)
    }

    // --- Usage Cycles ---
    fun getCyclesForTrackerFlow(trackerId: Long): Flow<List<UsageCycleItem>> {
        return trackerDao.getCyclesForTrackerFlow(trackerId).map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun insertCycle(cycle: UsageCycleItem): Long = withContext(ioDispatcher) {
        trackerDao.insertCycle(UsageCycleEntity.fromDomain(cycle))
    }

    suspend fun updateCycle(cycle: UsageCycleItem) = withContext(ioDispatcher) {
        trackerDao.updateCycle(UsageCycleEntity.fromDomain(cycle))
    }

    suspend fun deleteCycle(id: Long) = withContext(ioDispatcher) {
        trackerDao.deleteCycleById(id)
    }

    // --- Usage Entries ---
    fun getEntriesForCycleFlow(cycleId: Long): Flow<List<UsageEntryItem>> {
        return trackerDao.getEntriesForCycleFlow(cycleId).map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun insertUsageEntry(entry: UsageEntryItem): Long = withContext(ioDispatcher) {
        trackerDao.insertUsageEntry(UsageEntryEntity.fromDomain(entry))
    }

    // --- Purchases ---
    fun getPurchasesForTrackerFlow(trackerId: Long): Flow<List<TrackerPurchaseItem>> {
        return trackerDao.getPurchasesForTrackerFlow(trackerId).map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun insertPurchase(purchase: TrackerPurchaseItem): Long = withContext(ioDispatcher) {
        trackerDao.insertPurchase(TrackerPurchaseEntity.fromDomain(purchase))
    }

    // --- Inventory Adjustments ---
    fun getAdjustmentsForTrackerFlow(trackerId: Long): Flow<List<InventoryAdjustmentItem>> {
        return trackerDao.getAdjustmentsForTrackerFlow(trackerId).map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun insertAdjustment(adjustment: InventoryAdjustmentItem): Long = withContext(ioDispatcher) {
        trackerDao.insertAdjustment(InventoryAdjustmentEntity.fromDomain(adjustment))
    }
}
