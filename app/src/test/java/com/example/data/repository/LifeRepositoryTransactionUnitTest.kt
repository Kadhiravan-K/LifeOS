package com.example.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entities.HoneycombShortcutEntity
import com.example.data.local.entities.LabelEntity
import com.example.data.local.entities.MedicationDoseEntity
import com.example.data.local.entities.MedicationEntity
import com.example.data.preferences.UserPreferences
import com.example.domain.model.LabelItem
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskPriority
import com.example.domain.model.TaskReminderItem
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LifeRepositoryTransactionUnitTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var preferences: UserPreferences
    private lateinit var repository: LifeRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        preferences = UserPreferences(context)
        repository = LifeRepository(database, preferences)
    }

    @After
    fun tearDown() {
        repository.testTransactionInterceptor = null
        database.close()
    }

    @Test
    fun testSuccessfulTaskInsertWithLabelsAndReminders() = runBlocking {
        val label1Id = database.labelDao().insertLabel(LabelEntity(profileId = 1, name = "Urgent", colorId = "red"))
        val label2Id = database.labelDao().insertLabel(LabelEntity(profileId = 1, name = "Work", colorId = "blue"))

        val task = TaskItem(
            id = 0,
            profileId = 1,
            title = "Complete Transaction Fix",
            dueDateIso = "2026-11-02",
            priority = TaskPriority.HIGH,
            labels = listOf(
                LabelItem(id = label1Id, profileId = 1, name = "Urgent", colorId = "red"),
                LabelItem(id = label2Id, profileId = 1, name = "Work", colorId = "blue")
            ),
            reminders = listOf(
                TaskReminderItem(id = 0, taskId = 0, minutesBefore = 15, isEnabled = true)
            )
        )

        val taskId = repository.insertTask(task)
        assertTrue(taskId > 0)

        val retrieved = repository.getTaskById(taskId)
        assertNotNull(retrieved)
        assertEquals("Complete Transaction Fix", retrieved?.title)
        assertEquals(2, retrieved?.labels?.size)
        assertEquals(1, retrieved?.reminders?.size)
    }

    @Test
    fun testTaskInsertRollbackOnFailure() = runBlocking {
        val label1Id = database.labelDao().insertLabel(LabelEntity(profileId = 1, name = "Urgent", colorId = "red"))

        val task = TaskItem(
            id = 0,
            profileId = 1,
            title = "Orphaned Task If No Transaction",
            dueDateIso = "2026-11-02",
            priority = TaskPriority.HIGH,
            labels = listOf(
                LabelItem(id = label1Id, profileId = 1, name = "Urgent", colorId = "red")
            ),
            reminders = listOf(
                TaskReminderItem(id = 0, taskId = 0, minutesBefore = 10, isEnabled = true)
            )
        )

        // Inject failure right after task insertion, before labels/reminders are persisted
        repository.testTransactionInterceptor = { step ->
            if (step == "AFTER_INSERT_TASK") {
                throw IllegalStateException("Simulated crash after inserting task row")
            }
        }

        try {
            repository.insertTask(task)
            fail("Expected IllegalStateException")
        } catch (e: IllegalStateException) {
            assertEquals("Simulated crash after inserting task row", e.message)
        }

        // When transactional, rollback ensures NO task row remains in the database!
        val allTasks = database.taskDao().getAllTasksList()
        assertTrue("Task row must be rolled back on failure (was: ${allTasks.size} tasks)", allTasks.isEmpty())

        val allReminders = database.taskReminderDao().getAllRemindersList()
        assertTrue("Reminders must be empty", allReminders.isEmpty())
    }

    @Test
    fun testTaskUpdateRollbackOnFailure() = runBlocking {
        val label1Id = database.labelDao().insertLabel(LabelEntity(profileId = 1, name = "Initial", colorId = "green"))
        val label2Id = database.labelDao().insertLabel(LabelEntity(profileId = 1, name = "Updated", colorId = "red"))

        val initialTask = TaskItem(
            id = 0,
            profileId = 1,
            title = "Initial Task Title",
            dueDateIso = "2026-11-02",
            priority = TaskPriority.LOW,
            labels = listOf(LabelItem(id = label1Id, profileId = 1, name = "Initial", colorId = "green")),
            reminders = listOf(TaskReminderItem(id = 0, taskId = 0, minutesBefore = 30, isEnabled = true))
        )

        val taskId = repository.insertTask(initialTask)
        assertTrue(taskId > 0)

        // Attempt to update task, but inject failure during reminder saving
        val updatedTask = repository.getTaskById(taskId)!!.copy(
            title = "Mutated Task Title",
            labels = listOf(LabelItem(id = label2Id, profileId = 1, name = "Updated", colorId = "red")),
            reminders = listOf(TaskReminderItem(id = 0, taskId = taskId, minutesBefore = 5, isEnabled = true))
        )

        repository.testTransactionInterceptor = { step ->
            if (step == "AFTER_UPDATE_LABELS") {
                throw IllegalStateException("Simulated crash during update after labels")
            }
        }

        try {
            repository.updateTask(updatedTask)
            fail("Expected IllegalStateException")
        } catch (e: IllegalStateException) {
            assertEquals("Simulated crash during update after labels", e.message)
        }

        // When transactional, rollback restores original task title, labels, and reminders!
        val currentTask = repository.getTaskById(taskId)
        assertNotNull(currentTask)
        assertEquals("Initial Task Title", currentTask?.title)
        assertEquals(1, currentTask?.labels?.size)
        assertEquals("Initial", currentTask?.labels?.first()?.name)
        assertEquals(1, currentTask?.reminders?.size)
        assertEquals(30, currentTask?.reminders?.first()?.minutesBefore)
    }

    @Test
    fun testTaskDeleteRollbackOnFailure() = runBlocking {
        val labelId = database.labelDao().insertLabel(LabelEntity(profileId = 1, name = "ToDelete", colorId = "orange"))

        val task = TaskItem(
            id = 0,
            profileId = 1,
            title = "Task To Delete",
            dueDateIso = "2026-11-02",
            priority = TaskPriority.MEDIUM,
            labels = listOf(LabelItem(id = labelId, profileId = 1, name = "ToDelete", colorId = "orange")),
            reminders = listOf(TaskReminderItem(id = 0, taskId = 0, minutesBefore = 15, isEnabled = true))
        )

        val taskId = repository.insertTask(task)
        assertTrue(taskId > 0)

        // Add honeycomb shortcut and medication tied to this task
        database.honeycombShortcutDao().insertShortcut(
            HoneycombShortcutEntity(
                profileId = 1,
                type = "TASK",
                targetId = taskId.toString(),
                title = "Task Shortcut"
            )
        )
        val medId = database.medicationDao().insertMedication(
            MedicationEntity(
                profileId = 1,
                taskId = taskId,
                name = "Aspirin",
                form = "TABLET",
                dosageAmount = "100",
                dosageUnit = "mg",
                scheduleType = "DAILY",
                startDateIso = "2026-11-02"
            )
        )
        database.medicationDao().insertDose(
            MedicationDoseEntity(
                medicationId = medId,
                taskId = taskId,
                medicationName = "Aspirin",
                medicationForm = "TABLET",
                dosage = "100mg",
                scheduledDateIso = "2026-11-02",
                scheduledTime = "08:00"
            )
        )

        // Inject failure right before deleting the task row from `tasks` table
        repository.testTransactionInterceptor = { step ->
            if (step == "BEFORE_DELETE_TASK_ROW") {
                throw IllegalStateException("Simulated crash right before deleting task row")
            }
        }

        try {
            repository.deleteTask(taskId)
            fail("Expected IllegalStateException")
        } catch (e: IllegalStateException) {
            assertEquals("Simulated crash right before deleting task row", e.message)
        }

        // When transactional, rollback ensures labels, reminders, shortcuts, and meds are NOT destroyed!
        val retrieved = repository.getTaskById(taskId)
        assertNotNull("Task row must still exist after rollback", retrieved)
        assertEquals(1, retrieved?.labels?.size)
        assertEquals(1, retrieved?.reminders?.size)

        val shortcuts = database.honeycombShortcutDao().getShortcutsForProfileList(1)
        assertTrue("Shortcut must not be lost", shortcuts.any { it.targetId == taskId.toString() })

        val med = database.medicationDao().getMedicationById(medId)
        assertNotNull("Medication must not be lost", med)

        val doses = database.medicationDao().getDosesForMedication(medId)
        assertEquals("Doses must not be lost", 1, doses.size)
    }
}
