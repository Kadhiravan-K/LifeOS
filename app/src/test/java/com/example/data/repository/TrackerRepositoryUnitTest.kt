package com.example.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.preferences.UserPreferences
import com.example.domain.tracker.CycleStatus
import com.example.domain.tracker.InventoryAdjustmentItem
import com.example.domain.tracker.InventoryPolicy
import com.example.domain.tracker.QuantityUnit
import com.example.domain.tracker.TrackerItem
import com.example.domain.tracker.TrackerPurchaseItem
import com.example.domain.tracker.UsageCycleItem
import com.example.domain.tracker.UsageEntryItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TrackerRepositoryUnitTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var preferences: UserPreferences
    private lateinit var trackerRepository: TrackerRepository
    private lateinit var lifeRepository: LifeRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        preferences = UserPreferences(context)
        trackerRepository = TrackerRepository(
            database = database,
            preferences = preferences,
            activeProfileIdFlow = flowOf(1L)
        )
        lifeRepository = LifeRepository(
            database = database,
            preferences = preferences,
            trackerRepository = trackerRepository
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun createSampleTracker(
        title: String = "Engine Oil",
        profileId: Long = 1L,
        projectId: Long? = null,
        currentStock: Double = 5.0
    ): TrackerItem {
        return TrackerItem(
            profileId = profileId,
            projectId = projectId,
            title = title,
            emoji = "🛢️",
            inventoryPolicy = InventoryPolicy.CONSUMABLE,
            quantityUnit = QuantityUnit.LITERS,
            currentStock = currentStock
        )
    }

    private fun createSampleCycle(
        trackerId: Long,
        startOdometer: Double = 50000.0,
        initialQuantity: Double = 4.0
    ): UsageCycleItem {
        return UsageCycleItem(
            trackerId = trackerId,
            startDateIso = "2026-01-01",
            startOdometer = startOdometer,
            currentOdometer = startOdometer,
            initialQuantity = initialQuantity,
            remainingQuantity = initialQuantity,
            status = CycleStatus.ACTIVE
        )
    }

    @Test
    fun insertTracker_and_getTrackerById_standaloneRepository() = runBlocking {
        val sample = createSampleTracker()
        val id = trackerRepository.insertTracker(sample)
        assertTrue(id > 0)

        val retrieved = trackerRepository.getTrackerById(id)
        assertNotNull(retrieved)
        assertEquals("Engine Oil", retrieved?.title)
        assertEquals(5.0, retrieved?.currentStock ?: 0.0, 0.001)
        assertFalse(retrieved?.isArchived ?: true)
    }

    @Test
    fun allTrackersFlow_and_activeTrackersFlow_emitsTrackers() = runBlocking {
        val id1 = trackerRepository.insertTracker(createSampleTracker("Active Tracker"))
        val id2 = trackerRepository.insertTracker(createSampleTracker("Archived Tracker"))
        trackerRepository.setTrackerArchived(id2, true)

        val all = trackerRepository.allTrackersFlow.first()
        assertEquals(2, all.size)

        val active = trackerRepository.activeTrackersFlow.first()
        assertEquals(1, active.size)
        assertEquals(id1, active[0].id)
    }

    @Test
    fun updateTracker_updatesFields() = runBlocking {
        val id = trackerRepository.insertTracker(createSampleTracker("Old Name"))
        val created = trackerRepository.getTrackerById(id)!!

        val updated = created.copy(title = "New Name", currentStock = 12.0)
        trackerRepository.updateTracker(updated)

        val retrieved = trackerRepository.getTrackerById(id)
        assertEquals("New Name", retrieved?.title)
        assertEquals(12.0, retrieved?.currentStock ?: 0.0, 0.001)
    }

    @Test
    fun deleteTracker_cascadesDeletionOfTrackerAndCycles() = runBlocking {
        val trackerId = trackerRepository.insertTracker(createSampleTracker())
        val cycleId = trackerRepository.insertCycle(createSampleCycle(trackerId))

        val cyclesBefore = trackerRepository.getCyclesForTrackerFlow(trackerId).first()
        assertEquals(1, cyclesBefore.size)

        trackerRepository.deleteTracker(trackerId)

        val retrievedTracker = trackerRepository.getTrackerById(trackerId)
        assertNull(retrievedTracker)

        val cyclesAfter = trackerRepository.getCyclesForTrackerFlow(trackerId).first()
        assertEquals(0, cyclesAfter.size)
    }

    @Test
    fun deleteAllTrackersForProfile_isolatesByProfileId() = runBlocking {
        trackerRepository.insertTracker(createSampleTracker(title = "Profile 1", profileId = 1L))
        trackerRepository.insertTracker(createSampleTracker(title = "Profile 2", profileId = 2L))

        trackerRepository.deleteAllTrackersForProfile(1L)

        val t1 = trackerRepository.getTrackerById(1L)
        assertNull(t1)

        val t2 = trackerRepository.getTrackerById(2L)
        assertNotNull(t2)
        assertEquals("Profile 2", t2?.title)
    }

    @Test
    fun unassignTrackersFromProject_clearsProjectId() = runBlocking {
        val id = trackerRepository.insertTracker(createSampleTracker(projectId = 42L))
        assertEquals(42L, trackerRepository.getTrackerById(id)?.projectId)

        trackerRepository.unassignTrackersFromProject(42L)

        val retrieved = trackerRepository.getTrackerById(id)
        assertNull(retrieved?.projectId)
    }

    @Test
    fun usageCycle_and_usageEntry_operations() = runBlocking {
        val trackerId = trackerRepository.insertTracker(createSampleTracker())
        val cycleId = trackerRepository.insertCycle(createSampleCycle(trackerId, startOdometer = 10000.0, initialQuantity = 10.0))
        assertTrue(cycleId > 0)

        val cycles = trackerRepository.getCyclesForTrackerFlow(trackerId).first()
        assertEquals(1, cycles.size)
        assertEquals(10000.0, cycles[0].startOdometer ?: 0.0, 0.001)

        val entryId = trackerRepository.insertUsageEntry(
            UsageEntryItem(
                cycleId = cycleId,
                dateIso = "2026-01-05",
                odometerReading = 10500.0,
                quantityDeducted = 1.0,
                notes = "First leg"
            )
        )
        assertTrue(entryId > 0)

        val entries = trackerRepository.getEntriesForCycleFlow(cycleId).first()
        assertEquals(1, entries.size)
        assertEquals(10500.0, entries[0].odometerReading ?: 0.0, 0.001)

        trackerRepository.deleteCycle(cycleId)
        val cyclesAfterDelete = trackerRepository.getCyclesForTrackerFlow(trackerId).first()
        assertEquals(0, cyclesAfterDelete.size)
    }

    @Test
    fun purchases_and_adjustments_operations() = runBlocking {
        val trackerId = trackerRepository.insertTracker(createSampleTracker())

        val purchaseId = trackerRepository.insertPurchase(
            TrackerPurchaseItem(
                trackerId = trackerId,
                purchaseDateIso = "2026-01-02",
                quantity = 4.0,
                unitPrice = 15.0,
                totalCost = 60.0,
                notes = "Bulk oil pack"
            )
        )
        assertTrue(purchaseId > 0)

        val purchases = trackerRepository.getPurchasesForTrackerFlow(trackerId).first()
        assertEquals(1, purchases.size)
        assertEquals(60.0, purchases[0].totalCost ?: 0.0, 0.001)

        val adjustmentId = trackerRepository.insertAdjustment(
            InventoryAdjustmentItem(
                trackerId = trackerId,
                dateIso = "2026-01-03",
                deltaQuantity = -1.0,
                reason = "Spill during maintenance"
            )
        )
        assertTrue(adjustmentId > 0)

        val adjustments = trackerRepository.getAdjustmentsForTrackerFlow(trackerId).first()
        assertEquals(1, adjustments.size)
        assertEquals(-1.0, adjustments[0].deltaQuantity, 0.001)
    }

    @Test
    fun lifeRepository_facade_delegatesCorrectlyToTrackerRepository() = runBlocking {
        val sample = createSampleTracker("Facade Test")
        val id = lifeRepository.insertTracker(sample)
        assertTrue(id > 0)

        val viaLifeRepo = lifeRepository.getTrackerById(id)
        val viaTrackerRepo = trackerRepository.getTrackerById(id)
        assertNotNull(viaLifeRepo)
        assertEquals(viaLifeRepo?.id, viaTrackerRepo?.id)
        assertEquals("Facade Test", viaLifeRepo?.title)

        lifeRepository.setTrackerArchived(id, true)
        val updated = trackerRepository.getTrackerById(id)
        assertTrue(updated?.isArchived ?: false)

        lifeRepository.deleteTracker(id)
        assertNull(trackerRepository.getTrackerById(id))
    }
}
