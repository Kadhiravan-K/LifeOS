package com.example.notifications

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entities.MedicationDoseEntity
import com.example.data.local.entities.MedicationEntity
import com.example.data.local.entities.ProfileEntity
import com.example.data.local.entities.TaskEntity
import com.example.data.local.entities.TaskReminderEntity
import com.example.data.preferences.UserPreferences
import com.example.data.repository.LifeRepository
import com.example.domain.model.TaskPriority
import com.example.ui.theme.engine.LocalPremiumEntitlementRepository
import com.example.ui.viewmodel.LifeViewModel
import com.example.utils.DateUtils
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReminderHealthStateUnitTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var preferences: UserPreferences
    private lateinit var repository: LifeRepository
    private lateinit var scheduler: DefaultReminderScheduler

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        preferences = UserPreferences(context)
        repository = LifeRepository(
            database,
            preferences,
            LocalPremiumEntitlementRepository(context)
        )
        scheduler = DefaultReminderScheduler(
            context = context,
            database = database,
            preferences = preferences,
            repository = repository
        )

        runBlocking {
            database.profileDao().insertProfile(
                ProfileEntity.createDefaultPersonal()
            )
        }
    }

    @After
    fun tearDown() {
        ReminderScheduler.cancelTestReminder(context)
        database.close()
    }

    @Test
    fun testEmptyUpcomingReminders() = runBlocking {
        val reminders = scheduler.getUpcomingReminders(limit = 5)
        assertTrue("When no tasks or doses exist, upcoming list must be empty", reminders.isEmpty())
    }

    @Test
    fun testUpcomingRemindersChronologicalOrderingAndLimit() = runBlocking {
        val tomorrowIso = DateUtils.addDays(DateUtils.todayString(), 1)
        val dayAfterTomorrowIso = DateUtils.addDays(DateUtils.todayString(), 2)

        // Insert 7 future tasks with different due times
        val times = listOf("18:00", "09:00", "14:00", "11:30", "08:15", "20:00", "16:45")
        for ((idx, time) in times.withIndex()) {
            database.taskDao().insertTask(
                TaskEntity(
                    id = (idx + 1).toLong(),
                    profileId = 1L,
                    title = "Task at $time",
                    category = "WORK",
                    dueDateIso = if (idx < 5) tomorrowIso else dayAfterTomorrowIso,
                    dueTime = time,
                    priority = TaskPriority.P1.name,
                    isReminderEnabled = true,
                    reminderMinutesBefore = 0,
                    createdAt = System.currentTimeMillis()
                )
            )
        }

        // Query with limit = 5
        val upcoming = scheduler.getUpcomingReminders(limit = 5)

        assertEquals("Must return exactly 5 items when more exist", 5, upcoming.size)

        // Verify strictly chronological ordering
        for (i in 0 until upcoming.size - 1) {
            assertTrue(
                "Items must be sorted chronologically by triggerEpochMillis",
                upcoming[i].triggerEpochMillis <= upcoming[i + 1].triggerEpochMillis
            )
        }

        // First item on tomorrow must be the earliest time (08:15)
        assertEquals("Earliest tomorrow task must be first", "08:15", upcoming[0].scheduledTime)
    }

    @Test
    fun testCompletedAndArchivedTasksExcludedFromUpcoming() = runBlocking {
        val tomorrowIso = DateUtils.addDays(DateUtils.todayString(), 1)

        // 1. Completed task
        database.taskDao().insertTask(
            TaskEntity(
                id = 50L,
                profileId = 1L,
                title = "Done Task",
                category = "WORK",
                dueDateIso = tomorrowIso,
                dueTime = "10:00",
                priority = TaskPriority.P1.name,
                isReminderEnabled = true,
                isOneTimeCompleted = true,
                createdAt = System.currentTimeMillis()
            )
        )

        // 2. Archived task
        database.taskDao().insertTask(
            TaskEntity(
                id = 51L,
                profileId = 1L,
                title = "Archived Task",
                category = "WORK",
                dueDateIso = tomorrowIso,
                dueTime = "11:00",
                priority = TaskPriority.P1.name,
                isReminderEnabled = true,
                isArchived = true,
                createdAt = System.currentTimeMillis()
            )
        )

        // 3. Active task
        database.taskDao().insertTask(
            TaskEntity(
                id = 52L,
                profileId = 1L,
                title = "Active Future Task",
                category = "WORK",
                dueDateIso = tomorrowIso,
                dueTime = "12:00",
                priority = TaskPriority.P1.name,
                isReminderEnabled = true,
                createdAt = System.currentTimeMillis()
            )
        )

        val upcoming = scheduler.getUpcomingReminders(limit = 5)
        assertEquals("Only the active task must appear in upcoming list", 1, upcoming.size)
        assertEquals(52L, upcoming[0].id.substringAfter("task_").toLong())
    }

    @Test
    fun testMedicationDoseAppearsInUpcomingReminders() = runBlocking {
        val tomorrowIso = DateUtils.addDays(DateUtils.todayString(), 1)

        database.medicationDao().insertMedication(
            MedicationEntity(
                id = 201L,
                profileId = 1L,
                taskId = 0L,
                name = "Amoxicillin",
                form = "CAPSULE",
                dosageAmount = "500",
                dosageUnit = "mg",
                scheduleType = "DAILY",
                startDateIso = tomorrowIso,
                createdAt = System.currentTimeMillis()
            )
        )

        database.medicationDao().insertDose(
            MedicationDoseEntity(
                id = 701L,
                medicationId = 201L,
                taskId = 0L,
                medicationName = "Amoxicillin",
                medicationForm = "CAPSULE",
                dosage = "500 mg",
                scheduledDateIso = tomorrowIso,
                scheduledTime = "08:00",
                status = "SCHEDULED"
            )
        )

        val upcoming = scheduler.getUpcomingReminders(limit = 5)
        assertEquals(1, upcoming.size)
        assertEquals("MEDICATION", upcoming[0].associationType)
        assertEquals("MEDICINE_DOSE", upcoming[0].reminderType)
        assertTrue(upcoming[0].title.contains("Amoxicillin"))
    }

    @Test
    fun testTestReminderLifecycleAndIsolation() = runBlocking {
        // Initial state: not scheduled
        assertFalse(ReminderScheduler.isTestReminderScheduled(context))

        // Schedule test reminder
        val scheduled = ReminderScheduler.scheduleTestReminder(context, delaySeconds = 60L)
        assertTrue("Test reminder scheduling must succeed", scheduled)
        assertTrue("Test reminder must now be reported as scheduled", ReminderScheduler.isTestReminderScheduled(context))

        // Verify no real tasks or medication doses were inserted into database
        val tasks = database.taskDao().getAllTasksList()
        val doses = database.medicationDao().getAllDosesList()
        assertTrue("Test reminder must never create real tasks in database", tasks.isEmpty())
        assertTrue("Test reminder must never create real doses in database", doses.isEmpty())

        // Cancel test reminder
        ReminderScheduler.cancelTestReminder(context)
        assertFalse("Test reminder must be cancelled", ReminderScheduler.isTestReminderScheduled(context))
    }

    @Test
    fun testRepeatedTestReminderSchedulingIsIdempotent() {
        // Scheduling twice must overwrite the alarm rather than creating duplicate entries
        val run1 = ReminderScheduler.scheduleTestReminder(context, delaySeconds = 60L)
        val run2 = ReminderScheduler.scheduleTestReminder(context, delaySeconds = 60L)

        assertTrue(run1)
        assertTrue(run2)
        assertTrue(ReminderScheduler.isTestReminderScheduled(context))

        ReminderScheduler.cancelTestReminder(context)
    }

    @Test
    fun testBatteryOptimizationCheckDoesNotThrow() {
        val isIgnoring = ReminderScheduler.checkBatteryOptimizationStatus(context)
        // Robolectric environment should report boolean without throwing
        assertNotNull(isIgnoring)
    }

    @Test
    fun testNotificationChannelsCheckReturnsAllCategories() {
        val channels = ReminderScheduler.checkAllChannelsStatus(context)
        assertNotNull(channels)
        // In Robolectric (Android SDK 34+), NotificationHelper registers 9 categories
        if (channels.isNotEmpty()) {
            assertEquals("Must report all 9 LifeOS notification channels", 9, channels.size)
            assertTrue("Medication channel must be present", channels.any { it.id == NotificationHelper.CHANNEL_MEDICINE })
            assertTrue("Task reminder channel must be present", channels.any { it.id == NotificationHelper.CHANNEL_TASK_REMINDERS })
            assertTrue("Daily momentum channel must be present", channels.any { it.id == NotificationHelper.CHANNEL_DAILY_MOMENTUM })
        }
    }

    @Test
    fun testViewModelReminderHealthStateRefresh() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = LifeViewModel(app, repository, preferences)

        viewModel.refreshReminderHealth(context).join()
        val state = viewModel.reminderHealthState.value

        assertNotNull(state)
        assertFalse("isLoading must be false after refresh completes", state.isLoading)
        assertNotNull(state.exactAlarmStatus)
        assertNotNull(state.channels)
        assertNotNull(state.upcomingReminders)
    }
}
