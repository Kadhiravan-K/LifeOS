package com.example.notifications

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(RobolectricTestRunner::class)
class MaintenanceWorkerUnitTest {

    private lateinit var context: Context
    private val schedulerInvoked = AtomicBoolean(false)
    private var throwSimulatedError = false

    private val testScheduler = object : ReminderScheduler {
        override suspend fun rescheduleAll(horizonDays: Int): RescheduleResult {
            if (throwSimulatedError) {
                throw IllegalStateException("Simulated database failure during maintenance")
            }
            schedulerInvoked.set(true)
            return RescheduleResult(
                horizonDays = horizonDays,
                tasksScheduled = 3,
                dosesScheduled = 1,
                totalScheduled = 5,
                cappedByLimit = false
            )
        }
    }

    private fun createTestWorker(): MaintenanceWorker {
        val unsafeField = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe").apply { isAccessible = true }
        val unsafe = unsafeField.get(null) as sun.misc.Unsafe
        val workerParams = unsafe.allocateInstance(WorkerParameters::class.java) as WorkerParameters
        return MaintenanceWorker(context, workerParams)
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        schedulerInvoked.set(false)
        throwSimulatedError = false
        ReminderScheduler.setInstanceForTesting(testScheduler)
    }

    @After
    fun tearDown() {
        ReminderScheduler.setInstanceForTesting(null)
    }

    @Test
    fun testMaintenanceWorkerInvokesCentralizedScheduler() = runBlocking {
        val worker = createTestWorker()
        val result = worker.doWork()

        assertTrue("doWork() must succeed", result is ListenableWorker.Result.Success)
        assertTrue("Centralized ReminderScheduler must be invoked", schedulerInvoked.get())
    }

    @Test
    fun testMaintenanceWorkerRetriesOnFailure() = runBlocking {
        throwSimulatedError = true
        val worker = createTestWorker()
        val result = worker.doWork()

        assertTrue("doWork() must return Retry on exception", result is ListenableWorker.Result.Retry)
    }

    @Test
    fun testMaintenanceWorkerUniqueWorkName() {
        assertEquals("lifeos_maintenance_worker", MaintenanceWorker.WORK_NAME)
    }
}
