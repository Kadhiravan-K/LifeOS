package com.example.widgets

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entities.HoneycombShortcutEntity
import com.example.data.local.entities.TaskEntity
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
class WidgetTileResolutionTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var receiver: QuickRemindersActionReceiver

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = AppDatabase.getInstance(context)
        receiver = QuickRemindersActionReceiver()
        runBlocking {
            database.taskDao().deleteAllTasks()
            database.honeycombShortcutDao().clearAllShortcuts()
        }
    }

    @After
    fun tearDown() {
        runBlocking {
            database.taskDao().deleteAllTasks()
            database.honeycombShortcutDao().clearAllShortcuts()
        }
    }

    @Test
    fun testNumericIdResolvesCorrectTask() = runBlocking {
        val task1 = TaskEntity(id = 101L, profileId = 1L, title = "Task 101", category = "Work", dueDateIso = "2026-11-01", isOneTimeCompleted = false)
        val task2 = TaskEntity(id = 102L, profileId = 1L, title = "Task 102", category = "Work", dueDateIso = "2026-11-01", isOneTimeCompleted = false)
        database.taskDao().insertTasks(listOf(task1, task2))

        val shortcut = HoneycombShortcutEntity(
            id = 1L,
            profileId = 1L,
            type = "TASK",
            targetId = "101",
            title = "Task 101"
        )
        database.honeycombShortcutDao().insertShortcuts(listOf(shortcut))

        val intent = QuickRemindersActionReceiver.createPendingIntent(
            context = context,
            appWidgetId = 1,
            slotIndex = 0,
            shortcutId = 1L,
            type = "TASK",
            taskId = 101L,
            targetId = "101"
        )

        // Broadcast to receiver
        receiver.onReceive(context, Intent(QuickRemindersActionReceiver.ACTION_TOGGLE_SHORTCUT).apply {
            data = Uri.parse("lifeos://widget/qr/1/0/1")
            putExtra(QuickRemindersActionReceiver.EXTRA_SHORTCUT_ID, 1L)
            putExtra(QuickRemindersActionReceiver.EXTRA_TYPE, "TASK")
            putExtra(QuickRemindersActionReceiver.EXTRA_TASK_ID, 101L)
            putExtra(QuickRemindersActionReceiver.EXTRA_TARGET_ID, "101")
        })

        // Give coroutine a moment to complete on Dispatchers.IO
        kotlinx.coroutines.delay(200L)

        val updatedTask1 = database.taskDao().getTaskById(101L)
        val updatedTask2 = database.taskDao().getTaskById(102L)

        assertNotNull(updatedTask1)
        assertTrue("Task 101 must be toggled", updatedTask1!!.isOneTimeCompleted || updatedTask1.completedDatesJson.contains("2026-11-01"))
        assertFalse("Task 102 must remain untoggled", updatedTask2!!.isOneTimeCompleted)
    }

    @Test
    fun testLegacyCaseInsensitiveUniqueExactTitleMatch_resolvesCorrectly() = runBlocking {
        val task = TaskEntity(id = 201L, profileId = 1L, title = "Morning Run", category = "Fitness", dueDateIso = "2026-11-01", isOneTimeCompleted = false)
        database.taskDao().insertTasks(listOf(task))

        val shortcut = HoneycombShortcutEntity(
            id = 2L,
            profileId = 1L,
            type = "TASK",
            targetId = "morning run", // Lowercase legacy title target
            title = "Morning Run"
        )
        database.honeycombShortcutDao().insertShortcuts(listOf(shortcut))

        receiver.onReceive(context, Intent(QuickRemindersActionReceiver.ACTION_TOGGLE_SHORTCUT).apply {
            data = Uri.parse("lifeos://widget/qr/1/0/2")
            putExtra(QuickRemindersActionReceiver.EXTRA_SHORTCUT_ID, 2L)
            putExtra(QuickRemindersActionReceiver.EXTRA_TYPE, "TASK")
            putExtra(QuickRemindersActionReceiver.EXTRA_TASK_ID, -1L) // Legacy had no numeric taskId in extra
            putExtra(QuickRemindersActionReceiver.EXTRA_TARGET_ID, "morning run")
        })

        kotlinx.coroutines.delay(200L)

        val updatedTask = database.taskDao().getTaskById(201L)
        assertNotNull(updatedTask)
        assertTrue("Legacy task matched by unique exact title must be toggled", updatedTask!!.isOneTimeCompleted || updatedTask.completedDatesJson.contains("2026-11-01"))
    }

    @Test
    fun testDuplicateTitles_returnUnlinkedAndDoNotToggle() = runBlocking {
        // Two tasks with the exact same title
        val task1 = TaskEntity(id = 301L, profileId = 1L, title = "Read Book", category = "Habits", dueDateIso = "2026-11-01", isOneTimeCompleted = false)
        val task2 = TaskEntity(id = 302L, profileId = 1L, title = "Read Book", category = "Habits", dueDateIso = "2026-11-01", isOneTimeCompleted = false)
        database.taskDao().insertTasks(listOf(task1, task2))

        val shortcut = HoneycombShortcutEntity(
            id = 3L,
            profileId = 1L,
            type = "TASK",
            targetId = "Read Book",
            title = "Read Book"
        )
        database.honeycombShortcutDao().insertShortcuts(listOf(shortcut))

        receiver.onReceive(context, Intent(QuickRemindersActionReceiver.ACTION_TOGGLE_SHORTCUT).apply {
            data = Uri.parse("lifeos://widget/qr/1/0/3")
            putExtra(QuickRemindersActionReceiver.EXTRA_SHORTCUT_ID, 3L)
            putExtra(QuickRemindersActionReceiver.EXTRA_TYPE, "TASK")
            putExtra(QuickRemindersActionReceiver.EXTRA_TASK_ID, -1L)
            putExtra(QuickRemindersActionReceiver.EXTRA_TARGET_ID, "Read Book")
        })

        kotlinx.coroutines.delay(200L)

        val updatedTask1 = database.taskDao().getTaskById(301L)
        val updatedTask2 = database.taskDao().getTaskById(302L)
        assertFalse("Ambiguous duplicate title task 1 must NOT be toggled", updatedTask1!!.isOneTimeCompleted)
        assertFalse("Ambiguous duplicate title task 2 must NOT be toggled", updatedTask2!!.isOneTimeCompleted)
    }

    @Test
    fun testSubstringTitles_Run_and_RunPayroll_neverCrossToggle() = runBlocking {
        val taskShort = TaskEntity(id = 401L, profileId = 1L, title = "Run", category = "Health", dueDateIso = "2026-11-01", isOneTimeCompleted = false)
        val taskLong = TaskEntity(id = 402L, profileId = 1L, title = "Run payroll", category = "Finance", dueDateIso = "2026-11-01", isOneTimeCompleted = false)
        database.taskDao().insertTasks(listOf(taskShort, taskLong))

        // Shortcut targeted specifically to "Run"
        val shortcut = HoneycombShortcutEntity(
            id = 4L,
            profileId = 1L,
            type = "TASK",
            targetId = "Run",
            title = "Run"
        )
        database.honeycombShortcutDao().insertShortcuts(listOf(shortcut))

        receiver.onReceive(context, Intent(QuickRemindersActionReceiver.ACTION_TOGGLE_SHORTCUT).apply {
            data = Uri.parse("lifeos://widget/qr/1/0/4")
            putExtra(QuickRemindersActionReceiver.EXTRA_SHORTCUT_ID, 4L)
            putExtra(QuickRemindersActionReceiver.EXTRA_TYPE, "TASK")
            putExtra(QuickRemindersActionReceiver.EXTRA_TASK_ID, -1L)
            putExtra(QuickRemindersActionReceiver.EXTRA_TARGET_ID, "Run")
        })

        kotlinx.coroutines.delay(200L)

        val updatedShort = database.taskDao().getTaskById(401L)
        val updatedLong = database.taskDao().getTaskById(402L)

        assertTrue("Exact match 'Run' must be toggled", updatedShort!!.isOneTimeCompleted || updatedShort.completedDatesJson.contains("2026-11-01"))
        assertFalse("Substring match 'Run payroll' must NEVER be cross-toggled", updatedLong!!.isOneTimeCompleted)
    }

    @Test
    fun testMissingOrUnknownId_neverCreatesUnintendedTasks() = runBlocking {
        val initialTaskCount = database.taskDao().getAllTasksList().size

        // Shortcut pointing to non-existent task and no title match
        val shortcut = HoneycombShortcutEntity(
            id = 5L,
            profileId = 1L,
            type = "TASK",
            targetId = "NonExistentTask999",
            title = "Ghost Task"
        )
        database.honeycombShortcutDao().insertShortcuts(listOf(shortcut))

        receiver.onReceive(context, Intent(QuickRemindersActionReceiver.ACTION_TOGGLE_SHORTCUT).apply {
            data = Uri.parse("lifeos://widget/qr/1/0/5")
            putExtra(QuickRemindersActionReceiver.EXTRA_SHORTCUT_ID, 5L)
            putExtra(QuickRemindersActionReceiver.EXTRA_TYPE, "TASK")
            putExtra(QuickRemindersActionReceiver.EXTRA_TASK_ID, -1L)
            putExtra(QuickRemindersActionReceiver.EXTRA_TARGET_ID, "NonExistentTask999")
        })

        kotlinx.coroutines.delay(200L)

        val finalTaskCount = database.taskDao().getAllTasksList().size
        assertEquals("Database must NOT create a new task for an unresolvable shortcut", initialTaskCount, finalTaskCount)
    }
}
