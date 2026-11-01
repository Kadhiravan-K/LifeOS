package com.example.notifications

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
class BootRescheduleReceiverRobolectricTest {

    private lateinit var context: Context
    private val rescheduleCallCount = AtomicInteger(0)

    private val testScheduler = object : ReminderScheduler {
        override suspend fun rescheduleAll(horizonDays: Int): RescheduleResult {
            rescheduleCallCount.incrementAndGet()
            return RescheduleResult(
                horizonDays = horizonDays,
                tasksScheduled = 5,
                dosesScheduled = 2,
                totalScheduled = 8,
                cappedByLimit = false
            )
        }
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        rescheduleCallCount.set(0)
        ReminderScheduler.setInstanceForTesting(testScheduler)
    }

    @After
    fun tearDown() {
        ReminderScheduler.setInstanceForTesting(null)
    }

    @Test
    fun testBootCompletedDelegatesToScheduler() = runBlocking {
        val receiver = BootCompletedReceiver()
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)

        receiver.onReceive(context, intent)
        delay(200L) // Wait for runAsync to complete

        assertEquals("BOOT_COMPLETED must delegate to ReminderScheduler", 1, rescheduleCallCount.get())
    }

    @Test
    fun testPackageReplacedDelegatesToScheduler() = runBlocking {
        val receiver = BootCompletedReceiver()
        val intent = Intent(Intent.ACTION_MY_PACKAGE_REPLACED)

        receiver.onReceive(context, intent)
        delay(200L)

        assertEquals("MY_PACKAGE_REPLACED must delegate to ReminderScheduler", 1, rescheduleCallCount.get())
    }

    @Test
    fun testTimeChangedDelegatesToScheduler() = runBlocking {
        val receiver = BootCompletedReceiver()
        val intent = Intent(Intent.ACTION_TIME_CHANGED)

        receiver.onReceive(context, intent)
        delay(200L)

        assertEquals("TIME_CHANGED must delegate to ReminderScheduler", 1, rescheduleCallCount.get())
    }

    @Test
    fun testTimezoneChangedDelegatesToScheduler() = runBlocking {
        val receiver = BootCompletedReceiver()
        val intent = Intent(Intent.ACTION_TIMEZONE_CHANGED)

        receiver.onReceive(context, intent)
        delay(200L)

        assertEquals("TIMEZONE_CHANGED must delegate to ReminderScheduler", 1, rescheduleCallCount.get())
    }

    @Test
    fun testExactAlarmPermissionStateChangedDelegatesToScheduler() = runBlocking {
        val receiver = BootCompletedReceiver()
        val intent = Intent(AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)

        receiver.onReceive(context, intent)
        delay(200L)

        assertEquals("SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED must delegate to ReminderScheduler", 1, rescheduleCallCount.get())
    }

    @Test
    fun testUnexpectedForeignActionRejected() = runBlocking {
        val receiver = BootCompletedReceiver()
        val intent = Intent("android.intent.action.AIRPLANE_MODE")

        receiver.onReceive(context, intent)
        delay(100L)

        assertEquals("Foreign action must be rejected immediately without delegating to scheduler", 0, rescheduleCallCount.get())
    }

    @Test
    fun testNullActionRejected() = runBlocking {
        val receiver = BootCompletedReceiver()
        val intent = Intent() // null action

        receiver.onReceive(context, intent)
        delay(100L)

        assertEquals("Null action must be rejected immediately without delegating to scheduler", 0, rescheduleCallCount.get())
    }

    @Test
    fun testRepeatedBroadcastsAreIdempotent() = runBlocking {
        val receiver = BootCompletedReceiver()

        receiver.onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))
        receiver.onReceive(context, Intent(Intent.ACTION_TIME_CHANGED))
        receiver.onReceive(context, Intent(Intent.ACTION_TIMEZONE_CHANGED))
        delay(300L)

        assertEquals("All 3 valid broadcasts must invoke centralized scheduler", 3, rescheduleCallCount.get())
    }
}
