package com.example.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entities.LabelEntity
import com.example.data.preferences.UserPreferences
import com.example.domain.model.DurationUnit
import com.example.domain.model.LabelItem
import com.example.domain.model.RecurrenceType
import com.example.domain.model.SchedulingMode
import com.example.domain.model.Subtask
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskPriority
import com.example.domain.model.TaskReminderItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TaskRepositoryUnitTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var preferences: UserPreferences
    private lateinit var taskRepository: TaskRepository
    private lateinit var lifeRepository: LifeRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        preferences = UserPreferences(context)
        taskRepository = TaskRepository(
            database = database,
            preferences = preferences,
            activeProfileIdFlow = flowOf(1L)
        )
        lifeRepository = LifeRepository(
            database = database,
            preferences = preferences,
            taskRepository = taskRepository
        )
    }

    @After
    fun tearDown() {
        taskRepository.testTransactionInterceptor = null
        database.close()
    }

    private fun createSampleTask(
        title: String = "Test Task",
        profileId: Long = 1L,
        projectId: Long? = null,
        dueDateIso: String = "2026-11-03",
        recurrenceType: RecurrenceType = RecurrenceType.NONE,
        labels: List<LabelItem> = emptyList(),
        reminders: List<TaskReminderItem> = emptyList(),
        subtasks: List<Subtask> = emptyList()
    ): TaskItem {
        return TaskItem(
            profileId = profileId,
            projectId = projectId,
            title = title,
            emoji = "📝",
            description = "Test description",
            category = "Work",
            priority = TaskPriority.HIGH,
            dueDateIso = dueDateIso,
            dueTime = "09:00",
            durationMinutes = 45,
            schedulingMode = SchedulingMode.DURATION,
            durationAmount = 45,
            durationUnit = DurationUnit.MINUTES,
            recurrenceType = recurrenceType,
            labels = labels,
            reminders = reminders,
            subtasks = subtasks
        )
    }

    @Test
    fun insertTask_and_getTaskById_standalone() = runBlocking {
        val task = createSampleTask(title = "Buy Groceries")
        val taskId = taskRepository.insertTask(task)
        assertTrue("Task ID should be positive", taskId > 0)

        val retrieved = taskRepository.getTaskById(taskId)
        assertNotNull(retrieved)
        assertEquals("Buy Groceries", retrieved?.title)
        assertEquals("Work", retrieved?.category)
        assertEquals(TaskPriority.HIGH, retrieved?.priority)
        assertEquals("09:00", retrieved?.dueTime)
        assertEquals(45, retrieved?.durationMinutes)
        assertEquals(1L, retrieved?.profileId)
    }

    @Test
    fun updateTask_standalone() = runBlocking {
        val taskId = taskRepository.insertTask(createSampleTask(title = "Initial Title"))
        val existing = taskRepository.getTaskById(taskId)
        assertNotNull(existing)

        val updated = existing!!.copy(
            title = "Updated Title",
            description = "Updated description",
            priority = TaskPriority.URGENT
        )
        taskRepository.updateTask(updated)

        val retrieved = taskRepository.getTaskById(taskId)
        assertNotNull(retrieved)
        assertEquals("Updated Title", retrieved?.title)
        assertEquals("Updated description", retrieved?.description)
        assertEquals(TaskPriority.URGENT, retrieved?.priority)
    }

    @Test
    fun deleteTask_standalone() = runBlocking {
        val taskId = taskRepository.insertTask(createSampleTask(title = "Delete Me"))
        val existing = taskRepository.getTaskById(taskId)
        assertNotNull(existing)

        taskRepository.deleteTask(taskId)
        val retrieved = taskRepository.getTaskById(taskId)
        assertNull("Task should be null after delete", retrieved)
    }

    @Test
    fun saveTaskLabels_and_reminders_persistedAndRetrieved() = runBlocking {
        val label1Id = database.labelDao().insertLabel(LabelEntity(profileId = 1, name = "Important", colorId = "red"))
        val label2Id = database.labelDao().insertLabel(LabelEntity(profileId = 1, name = "Finance", colorId = "green"))

        val task = createSampleTask(
            title = "Tax Filing",
            labels = listOf(
                LabelItem(id = label1Id, profileId = 1, name = "Important", colorId = "red"),
                LabelItem(id = label2Id, profileId = 1, name = "Finance", colorId = "green")
            ),
            reminders = listOf(
                TaskReminderItem(id = 0, taskId = 0, minutesBefore = 30, isEnabled = true)
            )
        )

        val taskId = taskRepository.insertTask(task)
        assertTrue(taskId > 0)

        val retrieved = taskRepository.getTaskById(taskId)
        assertNotNull(retrieved)
        assertEquals(2, retrieved?.labels?.size)
        assertEquals(1, retrieved?.reminders?.size)
        assertEquals(30, retrieved?.reminders?.first()?.minutesBefore)
    }

    @Test
    fun allTasksFlow_emitsActiveProfileTasks() = runBlocking {
        taskRepository.insertTask(createSampleTask(title = "Profile 1 Task", profileId = 1L))
        taskRepository.insertTask(createSampleTask(title = "Profile 2 Task", profileId = 2L))

        val tasks = taskRepository.allTasksFlow.first()
        assertEquals(1, tasks.size)
        assertEquals("Profile 1 Task", tasks[0].title)
    }

    @Test
    fun toggleTaskCompletion_invokesCallbackAndUpdatesState() = runBlocking {
        val taskId = taskRepository.insertTask(createSampleTask(title = "Daily Workout"))

        var callbackInvoked = false
        var completedState = false

        taskRepository.toggleTaskCompletion(taskId, dateIso = "2026-11-03") { _, isCompleted, _ ->
            callbackInvoked = true
            completedState = isCompleted
        }

        assertTrue("Callback should be invoked", callbackInvoked)
        assertTrue("Task should be completed", completedState)

        val afterCompletion = taskRepository.getTaskById(taskId)
        assertNotNull(afterCompletion)
        assertTrue("One time completed should be true", afterCompletion!!.isOneTimeCompleted)
        assertTrue("completedDates should contain 2026-11-03", afterCompletion.completedDates.contains("2026-11-03"))

        // Toggle back (undo completion)
        taskRepository.toggleTaskCompletion(taskId, dateIso = "2026-11-03") { _, isCompleted, _ ->
            completedState = isCompleted
        }

        assertFalse("Task should no longer be completed", completedState)
        val afterUndo = taskRepository.getTaskById(taskId)
        assertNotNull(afterUndo)
        assertFalse(afterUndo!!.isOneTimeCompleted)
        assertFalse(afterUndo.completedDates.contains("2026-11-03"))
    }

    @Test
    fun toggleSubtask_updatesSubtaskState() = runBlocking {
        val subtasks = listOf(
            Subtask(id = "sub1", title = "Step 1", isCompleted = false),
            Subtask(id = "sub2", title = "Step 2", isCompleted = false)
        )
        val taskId = taskRepository.insertTask(createSampleTask(title = "Multi-step Task", subtasks = subtasks))

        taskRepository.toggleSubtask(taskId, "sub1")

        val retrieved = taskRepository.getTaskById(taskId)
        assertNotNull(retrieved)
        assertEquals(2, retrieved?.subtasks?.size)
        assertTrue(retrieved!!.subtasks.find { it.id == "sub1" }!!.isCompleted)
        assertFalse(retrieved.subtasks.find { it.id == "sub2" }!!.isCompleted)
    }

    @Test
    fun rescheduleOccurrence_and_skipOccurrence() = runBlocking {
        val taskId = taskRepository.insertTask(
            createSampleTask(
                title = "Recurring Meeting",
                recurrenceType = RecurrenceType.DAILY
            )
        )

        // Reschedule
        taskRepository.rescheduleOccurrence(taskId, originalDateIso = "2026-11-03", newDateIso = "2026-11-04")
        val afterReschedule = taskRepository.getTaskById(taskId)!!
        assertEquals("2026-11-04", afterReschedule.rescheduledDates["2026-11-03"])

        // Skip
        taskRepository.skipOccurrence(taskId, dateIso = "2026-11-05")
        val afterSkip = taskRepository.getTaskById(taskId)!!
        assertTrue(afterSkip.skippedDates.contains("2026-11-05"))
    }

    @Test
    fun softDeleteTask_movesToTrashAndCleansUp() = runBlocking {
        val labelId = database.labelDao().insertLabel(LabelEntity(profileId = 1, name = "Temp", colorId = "gray"))
        val taskId = taskRepository.insertTask(
            createSampleTask(
                title = "Trash Me",
                labels = listOf(LabelItem(id = labelId, profileId = 1, name = "Temp", colorId = "gray")),
                reminders = listOf(TaskReminderItem(id = 0, taskId = 0, minutesBefore = 10, isEnabled = true))
            )
        )
        val task = taskRepository.getTaskById(taskId)!!

        taskRepository.softDeleteTask(task)

        // Task should no longer exist in taskDao
        assertNull(taskRepository.getTaskById(taskId))

        // Trash should have item
        val trashItems = database.trashDao().getAllTrashItemsList()
        val found = trashItems.find { it.itemType == "TASK" && it.originalId == taskId }
        assertNotNull(found)
        assertEquals("Trash Me", found?.title)

        // Labels and reminders cross references should be deleted
        assertEquals(0, database.labelDao().getLabelsForTaskList(taskId).size)
        assertEquals(0, database.taskReminderDao().getRemindersForTaskList(taskId).size)
    }

    @Test
    fun deleteAllTasksForProfile_cleansUpOnlyTargetProfile() = runBlocking {
        taskRepository.insertTask(createSampleTask(title = "Profile 1 Task", profileId = 1L))
        taskRepository.insertTask(createSampleTask(title = "Profile 2 Task", profileId = 2L))

        taskRepository.deleteAllTasksForProfile(1L)

        val p1Tasks = taskRepository.getTasksForProfileList(1L)
        val p2Tasks = taskRepository.getTasksForProfileList(2L)
        assertEquals(0, p1Tasks.size)
        assertEquals(1, p2Tasks.size)
        assertEquals("Profile 2 Task", p2Tasks[0].title)
    }

    @Test
    fun transactionRollback_onFailure() = runBlocking {
        val label1Id = database.labelDao().insertLabel(LabelEntity(profileId = 1, name = "TxLabel", colorId = "purple"))
        val task = createSampleTask(
            title = "Atomic Test Task",
            labels = listOf(LabelItem(id = label1Id, profileId = 1, name = "TxLabel", colorId = "purple"))
        )

        taskRepository.testTransactionInterceptor = { step ->
            if (step == "AFTER_INSERT_TASK") {
                throw RuntimeException("Simulated crash inside transaction")
            }
        }

        try {
            taskRepository.insertTask(task)
            fail("Expected RuntimeException was not thrown")
        } catch (e: RuntimeException) {
            assertEquals("Simulated crash inside transaction", e.message)
        }

        val allTasks = taskRepository.getAllTasksList()
        assertTrue("Database should have 0 tasks due to rollback", allTasks.isEmpty())
    }

    @Test
    fun lifeRepository_facadeDelegationParity() = runBlocking {
        val task = createSampleTask(title = "Facade Test Task")
        val taskId = lifeRepository.insertTask(task)
        assertTrue(taskId > 0)

        val retrievedFromLifeRepo = lifeRepository.getTaskById(taskId)
        val retrievedFromTaskRepo = taskRepository.getTaskById(taskId)
        assertNotNull(retrievedFromLifeRepo)
        assertEquals(retrievedFromTaskRepo?.id, retrievedFromLifeRepo?.id)
        assertEquals("Facade Test Task", retrievedFromLifeRepo?.title)

        // Update through LifeRepository
        lifeRepository.updateTask(retrievedFromLifeRepo!!.copy(title = "Facade Updated"))
        assertEquals("Facade Updated", taskRepository.getTaskById(taskId)?.title)

        // Verify allTasksFlow
        val tasks = lifeRepository.allTasksFlow.first()
        assertEquals(1, tasks.size)
        assertEquals("Facade Updated", tasks[0].title)

        // Delete through LifeRepository
        lifeRepository.deleteTask(taskId)
        assertNull(taskRepository.getTaskById(taskId))
    }
}
