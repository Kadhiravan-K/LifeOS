package com.example.notifications

import android.content.BroadcastReceiver
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private fun safeLogE(tag: String, msg: String, tr: Throwable? = null) {
    try {
        Log.e(tag, msg, tr)
    } catch (_: Throwable) {
        // Ignored in unit test environments without mocked android.util.Log
    }
}

private fun safeLogW(tag: String, msg: String, tr: Throwable? = null) {
    try {
        Log.w(tag, msg, tr)
    } catch (_: Throwable) {
        // Ignored in unit test environments without mocked android.util.Log
    }
}

/**
 * Interface wrapping PendingResult.finish() to enable deterministic unit testing and mocking.
 */
interface AsyncPendingResult {
    fun finish()
}

/**
 * Core asynchronous runner logic for broadcast receivers.
 *
 * Guarantees:
 * 1. Synchronous execution on Dispatchers.IO.
 * 2. Bounded execution via withTimeout(timeoutMs) (default 9,000ms, well below the 10s ANR threshold).
 * 3. Guaranteed pendingResult.finish() invocation in the finally block.
 * 4. Exceptions and cancellations are caught, logged, reported to onException callback, and safely managed without unhandled process termination.
 */
fun runAsyncInternal(
    pendingResult: AsyncPendingResult?,
    timeoutMs: Long = 9_000L,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    onException: ((Throwable) -> Unit)? = null,
    block: suspend () -> Unit
): Job {
    val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        safeLogE("BroadcastReceiver", "Unhandled coroutine exception in runAsync", throwable)
        onException?.invoke(throwable)
    }
    val scope = CoroutineScope(dispatcher + SupervisorJob() + exceptionHandler)
    return scope.launch {
        try {
            withTimeout(timeoutMs) {
                block()
            }
        } catch (ce: CancellationException) {
            safeLogW("BroadcastReceiver", "Receiver async work timed out after ${timeoutMs}ms or was cancelled", ce)
            onException?.invoke(ce)
        } catch (t: Throwable) {
            safeLogE("BroadcastReceiver", "Receiver async work failed with exception", t)
            onException?.invoke(t)
        } finally {
            try {
                pendingResult?.finish()
            } catch (e: Exception) {
                safeLogE("BroadcastReceiver", "Failed calling PendingResult.finish()", e)
            }
        }
    }
}

/**
 * Reusable helper for BroadcastReceiver instances:
 * Calls goAsync() synchronously before launching Dispatchers.IO coroutine work,
 * bounded by a strict timeout (default 9,000ms), and guarantees pendingResult.finish() in finally.
 */
fun BroadcastReceiver.runAsync(
    timeoutMs: Long = 9_000L,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    onException: ((Throwable) -> Unit)? = null,
    block: suspend () -> Unit
): Job {
    val pr = try {
        goAsync()
    } catch (e: Exception) {
        safeLogW("BroadcastReceiver", "goAsync() unavailable or threw exception", e)
        null
    }

    val wrapped = pr?.let {
        object : AsyncPendingResult {
            override fun finish() = it.finish()
        }
    }
    return runAsyncInternal(wrapped, timeoutMs, dispatcher, onException, block)
}
