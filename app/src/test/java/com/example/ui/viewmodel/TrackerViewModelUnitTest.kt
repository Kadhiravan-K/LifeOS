package com.example.ui.viewmodel

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.preferences.UserPreferences
import com.example.data.repository.TrackerRepository
import com.example.domain.tracker.CycleStatus
import com.example.domain.tracker.InventoryAdjustmentItem
import com.example.domain.tracker.InventoryPolicy
import com.example.domain.tracker.QuantityUnit
import com.example.domain.tracker.TrackerItem
import com.example.domain.tracker.TrackerPurchaseItem
import com.example.domain.tracker.UsageCycleItem
import com.example.domain.tracker.UsageEntryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class TrackerViewModelUnitTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var preferences: UserPreferences
    private lateinit var trackerRepository: TrackerRepository
    private lateinit var trackerViewModel: TrackerViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        preferences = UserPreferences(context)
        trackerRepository = TrackerRepository(
            database = database,
            preferences = preferences,
            ioDispatcher = Dispatchers.Unconfined,
            activeProfileIdFlow = flowOf(1L)
        )
        trackerViewModel = TrackerViewModel(trackerRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        database.close()
    }

    private fun createSampleTracker(
        title: String = "Tire Tread",
        currentStock: Double = 4.0
    ): TrackerItem {
        return TrackerItem(
            profileId = 1L,
            title = title,
            emoji = "🛞",
            inventoryPolicy = InventoryPolicy.REUSABLE_REPLACEABLE,
            quantityUnit = QuantityUnit.PIECES,
            currentStock = currentStock
        )
    }

    private fun createSampleCycle(
        trackerId: Long,
        startOdometer: Double = 20000.0,
        initialQuantity: Double = 8.0
    ): UsageCycleItem {
        return UsageCycleItem(
            trackerId = trackerId,
            startDateIso = "2026-02-01",
            startOdometer = startOdometer,
            currentOdometer = startOdometer,
            initialQuantity = initialQuantity,
            remainingQuantity = initialQuantity,
            status = CycleStatus.ACTIVE
        )
    }

    @Test
    fun initialState_isExpected() = runTest {
        assertFalse(trackerViewModel.isLoading.value)
        assertNull(trackerViewModel.errorMessage.value)
        assertEquals(0, trackerViewModel.allTrackers.value.size)
        assertEquals(0, trackerViewModel.activeTrackers.value.size)
    }

    @Test
    fun saveTracker_insertsAndUpdates_withCallback() = runTest {
        val sample = createSampleTracker("Chain Lubricant", 2.0)
        var savedId = 0L
        val latch = CountDownLatch(1)

        trackerViewModel.saveTracker(sample) { id ->
            savedId = id
            latch.countDown()
        }

        assertTrue(latch.await(3, TimeUnit.SECONDS))
        assertTrue(savedId > 0)

        val retrieved = trackerRepository.getTrackerById(savedId)
        assertNotNull(retrieved)
        assertEquals("Chain Lubricant", retrieved?.title)

        // Update
        val latchUpdate = CountDownLatch(1)
        val toUpdate = retrieved!!.copy(title = "Ceramic Chain Lube")
        trackerViewModel.saveTracker(toUpdate) {
            latchUpdate.countDown()
        }
        assertTrue(latchUpdate.await(3, TimeUnit.SECONDS))

        val updated = trackerRepository.getTrackerById(savedId)
        assertEquals("Ceramic Chain Lube", updated?.title)
    }

    @Test
    fun setTrackerArchived_updatesArchiveStatus() = runTest {
        val trackerId = trackerRepository.insertTracker(createSampleTracker())
        assertFalse(trackerRepository.getTrackerById(trackerId)?.isArchived ?: true)

        val latch = CountDownLatch(1)
        trackerViewModel.setTrackerArchived(trackerId, true) {
            latch.countDown()
        }
        assertTrue(latch.await(3, TimeUnit.SECONDS))

        val retrieved = trackerRepository.getTrackerById(trackerId)
        assertTrue(retrieved?.isArchived ?: false)
    }

    @Test
    fun deleteTracker_removesTracker_withCallback() = runTest {
        val trackerId = trackerRepository.insertTracker(createSampleTracker())
        val latch = CountDownLatch(1)

        trackerViewModel.deleteTracker(trackerId) {
            latch.countDown()
        }

        assertTrue(latch.await(3, TimeUnit.SECONDS))
        assertNull(trackerRepository.getTrackerById(trackerId))
    }

    @Test
    fun saveCycle_insertsAndUpdatesCycle() = runTest {
        val trackerId = trackerRepository.insertTracker(createSampleTracker())
        var savedCycleId = 0L
        val latch = CountDownLatch(1)

        val cycle = createSampleCycle(trackerId)
        trackerViewModel.saveCycle(cycle) { id ->
            savedCycleId = id
            latch.countDown()
        }

        assertTrue(latch.await(3, TimeUnit.SECONDS))
        assertTrue(savedCycleId > 0)

        val cycles = trackerViewModel.getCyclesForTracker(trackerId).first()
        assertEquals(1, cycles.size)
        assertEquals(20000.0, cycles[0].startOdometer ?: 0.0, 0.001)

        // Update cycle
        val latchUpdate = CountDownLatch(1)
        val cycleToUpdate = cycles[0].copy(notes = "Winter cycle")
        trackerViewModel.saveCycle(cycleToUpdate) {
            latchUpdate.countDown()
        }
        assertTrue(latchUpdate.await(3, TimeUnit.SECONDS))

        val updatedCycles = trackerViewModel.getCyclesForTracker(trackerId).first()
        assertEquals("Winter cycle", updatedCycles[0].notes)
    }

    @Test
    fun pauseResumeCycle_togglesPausedState() = runTest {
        val trackerId = trackerRepository.insertTracker(createSampleTracker())
        val cycleId = trackerRepository.insertCycle(createSampleCycle(trackerId))
        val cycle = trackerRepository.getCyclesForTrackerFlow(trackerId).first()[0]
        assertEquals(CycleStatus.ACTIVE, cycle.status)
        assertFalse(cycle.isPaused)

        val latchPause = CountDownLatch(1)
        trackerViewModel.pauseResumeCycle(cycle, isPaused = true) {
            latchPause.countDown()
        }
        assertTrue(latchPause.await(3, TimeUnit.SECONDS))

        val paused = trackerRepository.getCyclesForTrackerFlow(trackerId).first()[0]
        assertEquals(CycleStatus.PAUSED, paused.status)
        assertTrue(paused.isPaused)

        val latchResume = CountDownLatch(1)
        trackerViewModel.pauseResumeCycle(paused, isPaused = false) {
            latchResume.countDown()
        }
        assertTrue(latchResume.await(3, TimeUnit.SECONDS))

        val resumed = trackerRepository.getCyclesForTrackerFlow(trackerId).first()[0]
        assertEquals(CycleStatus.ACTIVE, resumed.status)
        assertFalse(resumed.isPaused)
    }

    @Test
    fun logUsageEntry_advancesOdometerAndDeductsRemainingQuantity() = runTest {
        val trackerId = trackerRepository.insertTracker(createSampleTracker())
        val cycleId = trackerRepository.insertCycle(
            createSampleCycle(trackerId, startOdometer = 1000.0, initialQuantity = 10.0)
        )
        val cycle = trackerRepository.getCyclesForTrackerFlow(trackerId).first()[0]

        val entry = UsageEntryItem(
            cycleId = cycleId,
            dateIso = "2026-02-10",
            odometerReading = 1250.0,
            quantityDeducted = 2.5
        )

        val latch = CountDownLatch(1)
        trackerViewModel.logUsageEntry(entry, cycle) {
            latch.countDown()
        }
        assertTrue(latch.await(3, TimeUnit.SECONDS))

        val updatedCycle = trackerRepository.getCyclesForTrackerFlow(trackerId).first()[0]
        assertEquals(1250.0, updatedCycle.currentOdometer ?: 0.0, 0.001)
        assertEquals(7.5, updatedCycle.remainingQuantity, 0.001)

        val entries = trackerRepository.getEntriesForCycleFlow(cycleId).first()
        assertEquals(1, entries.size)
        assertEquals(1250.0, entries[0].odometerReading ?: 0.0, 0.001)
    }

    @Test
    fun finishCycle_updatesStatusAndFinalValues() = runTest {
        val trackerId = trackerRepository.insertTracker(createSampleTracker())
        val cycleId = trackerRepository.insertCycle(createSampleCycle(trackerId, startOdometer = 5000.0, initialQuantity = 5.0))
        val cycle = trackerRepository.getCyclesForTrackerFlow(trackerId).first()[0]

        val latch = CountDownLatch(1)
        trackerViewModel.finishCycle(
            cycle = cycle,
            finishDateIso = "2026-03-01",
            finishOdometer = 9500.0,
            finalRemainingQuantity = 0.5,
            notes = "Completed normal service"
        ) {
            latch.countDown()
        }
        assertTrue(latch.await(3, TimeUnit.SECONDS))

        val finished = trackerRepository.getCyclesForTrackerFlow(trackerId).first()[0]
        assertEquals(CycleStatus.FINISHED, finished.status)
        assertEquals("2026-03-01", finished.finishDateIso)
        assertEquals(9500.0, finished.finishOdometer ?: 0.0, 0.001)
        assertEquals(0.5, finished.remainingQuantity, 0.001)
        assertTrue(finished.notes.contains("Completed normal service"))
    }

    @Test
    fun recordPurchase_and_recordStockAdjustment() = runTest {
        val trackerId = trackerRepository.insertTracker(createSampleTracker())

        val latchPurchase = CountDownLatch(1)
        trackerViewModel.recordPurchase(
            TrackerPurchaseItem(
                trackerId = trackerId,
                purchaseDateIso = "2026-02-15",
                quantity = 2.0,
                unitPrice = 25.0,
                totalCost = 50.0
            )
        ) {
            latchPurchase.countDown()
        }
        assertTrue(latchPurchase.await(3, TimeUnit.SECONDS))

        val purchases = trackerViewModel.getPurchasesForTracker(trackerId).first()
        assertEquals(1, purchases.size)
        assertEquals(50.0, purchases[0].totalCost ?: 0.0, 0.001)

        val latchAdj = CountDownLatch(1)
        trackerViewModel.recordStockAdjustment(
            InventoryAdjustmentItem(
                trackerId = trackerId,
                dateIso = "2026-02-16",
                deltaQuantity = 1.0,
                reason = "Inventory correction"
            )
        ) {
            latchAdj.countDown()
        }
        assertTrue(latchAdj.await(3, TimeUnit.SECONDS))

        val adjustments = trackerViewModel.getAdjustmentsForTracker(trackerId).first()
        assertEquals(1, adjustments.size)
        assertEquals(1.0, adjustments[0].deltaQuantity, 0.001)
    }

    @Test
    fun clearError_resetsErrorMessage() = runTest {
        trackerViewModel.clearError()
        assertNull(trackerViewModel.errorMessage.value)
    }

    @Test
    fun trackerViewModelFactory_createsInstance() {
        val factory = TrackerViewModelFactory(trackerRepository)
        val vm = factory.create(TrackerViewModel::class.java)
        assertNotNull(vm)
    }

    @Test(expected = IllegalArgumentException::class)
    fun trackerViewModelFactory_throwsOnUnknownViewModel() {
        val factory = TrackerViewModelFactory(trackerRepository)
        factory.create(LifeViewModel::class.java)
    }
}
