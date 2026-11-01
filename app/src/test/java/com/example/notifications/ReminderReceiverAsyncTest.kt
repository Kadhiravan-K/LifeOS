package com.example.notifications

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class ReminderReceiverAsyncTest {

    private class FakePendingResult : AsyncPendingResult {
        val finishCount = AtomicInteger(0)

        override fun finish() {
            finishCount.incrementAndGet()
        }
    }

    @Test
    fun testFinishCalledOnSuccess() = runBlocking {
        val fakeResult = FakePendingResult()
        val executed = AtomicBoolean(false)

        val job = runAsyncInternal(
            pendingResult = fakeResult,
            timeoutMs = 1_000L,
            dispatcher = Dispatchers.Unconfined
        ) {
            executed.set(true)
        }
        job.join()

        assertTrue("Coroutine block must be executed", executed.get())
        assertEquals("finish() must be called exactly once on success", 1, fakeResult.finishCount.get())
    }

    @Test
    fun testFinishCalledOnException() = runBlocking {
        val fakeResult = FakePendingResult()
        var caughtException: Throwable? = null

        val job = runAsyncInternal(
            pendingResult = fakeResult,
            timeoutMs = 1_000L,
            dispatcher = Dispatchers.Unconfined,
            onException = { caughtException = it }
        ) {
            throw IllegalStateException("Simulated database failure")
        }
        job.join()

        assertNotNull("Exception must be caught and logged", caughtException)
        assertTrue(caughtException is IllegalStateException)
        assertEquals("finish() must be called exactly once despite exception", 1, fakeResult.finishCount.get())
    }

    @Test
    fun testFinishCalledOnTimeout() = runBlocking {
        val fakeResult = FakePendingResult()
        var caughtTimeout: Throwable? = null

        val job = runAsyncInternal(
            pendingResult = fakeResult,
            timeoutMs = 50L, // 50ms short timeout
            dispatcher = Dispatchers.IO,
            onException = { caughtTimeout = it }
        ) {
            delay(500L) // Exceeds timeout
        }
        job.join()

        assertNotNull("CancellationException must be triggered on timeout", caughtTimeout)
        assertTrue(caughtTimeout is CancellationException)
        assertEquals("finish() must be called exactly once on timeout", 1, fakeResult.finishCount.get())
    }

    @Test
    fun testMultipleBranchesFinishExactlyOnce() = runBlocking {
        // Test 10 consecutive executions simulating different receiver branches
        for (i in 1..10) {
            val fakeResult = FakePendingResult()
            val job = runAsyncInternal(
                pendingResult = fakeResult,
                timeoutMs = 1_000L,
                dispatcher = Dispatchers.Unconfined
            ) {
                // simulate branch work
            }
            job.join()
            assertEquals("Branch $i must finish exactly once", 1, fakeResult.finishCount.get())
        }
    }
}
