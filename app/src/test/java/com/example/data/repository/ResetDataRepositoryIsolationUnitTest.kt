package com.example.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entities.HabitEntity
import com.example.data.local.entities.ProfileEntity
import com.example.data.local.entities.ProfileSecurityEntity
import com.example.data.local.entities.ShiftEntity
import com.example.data.preferences.UserPreferences
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskPriority
import com.example.domain.reset.ResetCategory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * LifeOS v4.0.1 — Issue 05: Reset Data Isolation & Persistence Unit Tests.
 * Verifies that:
 * 1. Resets are strictly isolated to the specified profile.
 * 2. Unselected categories for the target profile remain intact.
 * 3. Other profiles' data is never touched or affected.
 * 4. Profile entity and security lock credentials are never deleted by reset operations.
 */
@RunWith(RobolectricTestRunner::class)
class ResetDataRepositoryIsolationUnitTest {

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
        database.close()
    }

    @Test
    fun testSelectiveCategoryReset_strictProfileIsolation() = runBlocking {
        val profileAId = 1L
        val profileBId = 2L

        // Set up profiles
        database.profileDao().insertProfile(
            ProfileEntity(id = profileAId, name = "Profile A", emoji = "💼")
        )
        database.profileDao().insertProfile(
            ProfileEntity(id = profileBId, name = "Profile B", emoji = "🏠")
        )

        // Set up security lock for Profile A
        database.profileSecurityDao().insertOrUpdateSecurity(
            ProfileSecurityEntity(
                profileId = profileAId,
                isLockEnabled = true,
                authMethod = ProfileSecurityEntity.AUTH_METHOD_PIN,
                credentialHash = "sample_hash",
                salt = "sample_salt"
            )
        )

        // Insert Tasks for Profile A & B
        val taskRepo = repository.taskRepository
        taskRepo.insertTask(
            TaskItem(id = 0, profileId = profileAId, title = "Task A1", dueDateIso = "2026-11-10", priority = TaskPriority.HIGH)
        )
        taskRepo.insertTask(
            TaskItem(id = 0, profileId = profileAId, title = "Task A2", dueDateIso = "2026-11-10", priority = TaskPriority.MEDIUM)
        )
        taskRepo.insertTask(
            TaskItem(id = 0, profileId = profileBId, title = "Task B1", dueDateIso = "2026-11-10", priority = TaskPriority.LOW)
        )

        // Insert Habits for Profile A & B
        database.habitDao().insertHabit(
            HabitEntity(id = 0, profileId = profileAId, title = "Habit A")
        )
        database.habitDao().insertHabit(
            HabitEntity(id = 0, profileId = profileBId, title = "Habit B")
        )

        // Insert Shifts for Profile A
        database.shiftDao().insertShift(
            ShiftEntity(
                id = 0,
                profileId = profileAId,
                name = "Morning Shift",
                startTime = "08:00",
                endTime = "16:00",
                wakeUpTime = "06:30",
                sleepTime = "23:00",
                colorHex = "#FF0000"
            )
        )

        // Verify initial state
        assertEquals(2, taskRepo.getTasksForProfileFlow(profileAId).first().size)
        assertEquals(1, taskRepo.getTasksForProfileFlow(profileBId).first().size)
        assertEquals(1, database.habitDao().getHabitsForProfile(profileAId).first().size)
        assertEquals(1, database.habitDao().getHabitsForProfile(profileBId).first().size)
        assertEquals(1, database.shiftDao().getShiftsForProfileDirect(profileAId).size)

        // Action: Reset ONLY TASKS and SHIFTS for Profile A
        val result = repository.resetCategoriesForProfile(
            profileId = profileAId,
            categories = setOf(ResetCategory.TASKS, ResetCategory.SHIFTS)
        )

        // Verify ResetExecutionResult
        assertTrue(result.isCompleteSuccess)
        assertEquals(2, result.successfulCategories.size)
        assertTrue(result.successfulCategories.contains(ResetCategory.TASKS))
        assertTrue(result.successfulCategories.contains(ResetCategory.SHIFTS))

        // Profile A: Tasks and Shifts are deleted
        assertEquals(0, taskRepo.getTasksForProfileFlow(profileAId).first().size)
        assertEquals(0, database.shiftDao().getShiftsForProfileDirect(profileAId).size)

        // Profile A: Unselected category (HABITS) is strictly preserved
        val profileAHabits = database.habitDao().getHabitsForProfile(profileAId).first()
        assertEquals(1, profileAHabits.size)
        assertEquals("Habit A", profileAHabits[0].title)

        // Profile A: Profile identity & security entities are strictly preserved
        val profileAEntity = database.profileDao().getProfileById(profileAId)
        assertNotNull(profileAEntity)
        assertEquals("Profile A", profileAEntity?.name)
        val profileASecurity = database.profileSecurityDao().getSecurityForProfile(profileAId)
        assertNotNull(profileASecurity)
        assertTrue(profileASecurity?.isLockEnabled == true)

        // Profile B: Data is completely untouched
        val profileBTasks = taskRepo.getTasksForProfileFlow(profileBId).first()
        assertEquals(1, profileBTasks.size)
        assertEquals("Task B1", profileBTasks[0].title)

        val profileBHabits = database.habitDao().getHabitsForProfile(profileBId).first()
        assertEquals(1, profileBHabits.size)
        assertEquals("Habit B", profileBHabits[0].title)
    }
}
