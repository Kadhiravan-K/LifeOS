package com.example.widgets

import android.app.PendingIntent
import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entities.HabitEntity
import com.example.data.local.entities.HoneycombShortcutEntity
import com.example.data.local.entities.TaskEntity
import com.example.utils.DateUtils
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class WidgetMultiItemCompletionRegressionTest {
    private lateinit var context: Context
    private lateinit var database: AppDatabase

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        database = AppDatabase.getInstance(context)
        clearTestData()
    }

    @After
    fun tearDown() = runBlocking {
        clearTestData()
    }

    @Test
    fun `task widget actions complete only their target and honor current shortcut target`() = runBlocking {
        val today = DateUtils.todayString()
        val taskIds = listOf(91001L, 91002L, 91003L)
        database.taskDao().insertTasks(
            taskIds.mapIndexed { index, id ->
                TaskEntity(
                    id = id,
                    profileId = 1L,
                    title = "Widget task ${index + 1}",
                    category = "Work",
                    dueDateIso = today
                )
            }
        )
        database.honeycombShortcutDao().insertShortcuts(
            taskIds.mapIndexed { index, id ->
                HoneycombShortcutEntity(
                    id = 91101L + index,
                    profileId = 1L,
                    type = "TASK",
                    targetId = id.toString(),
                    title = "Widget task ${index + 1}"
                )
            }
        )

        sendTaskAction(shortcutId = 91101L, slot = 0, targetTaskId = 91001L, staleTaskId = 91002L)
        awaitCondition { database.taskDao().getTaskById(91001L)?.isOneTimeCompleted == true }
        assertTrue(database.taskDao().getTaskById(91001L)?.isOneTimeCompleted == true)
        assertFalse(database.taskDao().getTaskById(91002L)?.isOneTimeCompleted == true)
        assertFalse(database.taskDao().getTaskById(91003L)?.isOneTimeCompleted == true)

        sendTaskAction(shortcutId = 91102L, slot = 1, targetTaskId = 91002L, staleTaskId = 91002L)
        awaitCondition { database.taskDao().getTaskById(91002L)?.isOneTimeCompleted == true }
        assertTrue(database.taskDao().getTaskById(91001L)?.isOneTimeCompleted == true)
        assertTrue(database.taskDao().getTaskById(91002L)?.isOneTimeCompleted == true)
        assertFalse(database.taskDao().getTaskById(91003L)?.isOneTimeCompleted == true)
    }

    @Test
    fun `habit widget actions persist completion independently`() = runBlocking {
        val today = DateUtils.todayString()
        val habitIds = listOf(92001L, 92002L, 92003L)
        database.habitDao().insertAll(
            habitIds.mapIndexed { index, id ->
                HabitEntity(id = id, profileId = 1L, title = "Widget habit ${index + 1}")
            }
        )
        database.honeycombShortcutDao().insertShortcuts(
            habitIds.mapIndexed { index, id ->
                HoneycombShortcutEntity(
                    id = 92101L + index,
                    profileId = 1L,
                    type = "HABIT",
                    targetId = id.toString(),
                    title = "Widget habit ${index + 1}"
                )
            }
        )

        sendHabitAction(shortcutId = 92101L, slot = 0, habitId = 92001L)
        awaitCondition { isHabitComplete(92001L, today) }
        assertTrue(isHabitComplete(92001L, today))
        assertFalse(isHabitComplete(92002L, today))
        assertFalse(isHabitComplete(92003L, today))

        sendHabitAction(shortcutId = 92102L, slot = 1, habitId = 92002L)
        awaitCondition { isHabitComplete(92002L, today) }
        assertTrue(isHabitComplete(92001L, today))
        assertTrue(isHabitComplete(92002L, today))
        assertFalse(isHabitComplete(92003L, today))
    }

    @Test
    fun `habit master widget actions have stable independent identities`() = runBlocking {
        val today = DateUtils.todayString()
        val habitIds = listOf(93001L, 93002L, 93003L)
        WidgetProfileManager.saveWidgetProfileId(context, 929, 1L)
        database.habitDao().insertAll(
            habitIds.mapIndexed { index, id ->
                HabitEntity(id = id, profileId = 1L, title = "Habit master ${index + 1}")
            }
        )

        val actions = habitIds.mapIndexed { slot, habitId ->
            HabitMasterActionReceiver.createPendingIntent(
                context = context,
                appWidgetId = 929,
                slotIndex = slot,
                habitId = habitId,
                profileId = 1L
            )
        }
        assertTrue("Each habit needs a distinct PendingIntent.", actions.distinct().size == habitIds.size)
        assertTrue(
            "Recreating a tile must produce the same stable PendingIntent identity.",
            actions.first() == HabitMasterActionReceiver.createPendingIntent(
                context = context,
                appWidgetId = 929,
                slotIndex = 0,
                habitId = habitIds.first(),
                profileId = 1L
            )
        )

        actions.forEachIndexed { index, pendingIntent ->
            pendingIntent.send()
            shadowOf(Looper.getMainLooper()).idle()
            val expectedHabitId = habitIds[index]
            awaitCondition { isHabitComplete(expectedHabitId, today) }
            habitIds.forEach { habitId ->
                assertTrue(isHabitComplete(habitId, today) == (habitId == expectedHabitId ||
                    habitIds.take(index).contains(habitId)))
            }
        }

        actions.first().send()
        shadowOf(Looper.getMainLooper()).idle()
        awaitCondition { !isHabitComplete(habitIds.first(), today) }
        assertFalse(isHabitComplete(habitIds.first(), today))
        assertTrue(habitIds.drop(1).all { isHabitComplete(it, today) })

        actions.first().send()
        shadowOf(Looper.getMainLooper()).idle()
        awaitCondition { isHabitComplete(habitIds.first(), today) }
        assertTrue(habitIds.all { isHabitComplete(it, today) })
    }

    private fun sendTaskAction(
        shortcutId: Long,
        slot: Int,
        targetTaskId: Long,
        staleTaskId: Long
    ) {
        QuickRemindersActionReceiver.createPendingIntent(
            context = context,
            appWidgetId = 919,
            slotIndex = slot,
            shortcutId = shortcutId,
            type = "TASK",
            taskId = staleTaskId,
            targetId = targetTaskId.toString()
        ).send()
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun sendHabitAction(shortcutId: Long, slot: Int, habitId: Long) {
        QuickRemindersActionReceiver.createPendingIntent(
            context = context,
            appWidgetId = 920,
            slotIndex = slot,
            shortcutId = shortcutId,
            type = "HABIT",
            taskId = null,
            targetId = habitId.toString()
        ).send()
        shadowOf(Looper.getMainLooper()).idle()
    }

    private suspend fun isHabitComplete(habitId: Long, dateIso: String): Boolean {
        val dates = JSONArray(database.habitDao().getHabitById(habitId)?.completedDatesJson ?: "[]")
        return (0 until dates.length()).any { dates.optString(it) == dateIso }
    }

    private suspend fun awaitCondition(condition: suspend () -> Boolean) {
        repeat(30) {
            if (condition()) return
            Thread.sleep(100)
        }
        assertTrue("Widget action did not persist before timeout.", condition())
    }

    private suspend fun clearTestData() {
        (91001L..91003L).forEach { database.taskDao().deleteTaskById(it) }
        (92001L..92003L).forEach { database.habitDao().deleteHabitById(it) }
        (93001L..93003L).forEach { database.habitDao().deleteHabitById(it) }
        (91101L..91103L).forEach { database.honeycombShortcutDao().deleteShortcutById(it) }
        (92101L..92103L).forEach { database.honeycombShortcutDao().deleteShortcutById(it) }
        WidgetProfileManager.removeWidget(context, 929)
    }
}
