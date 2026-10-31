package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.backup.DataExportImportManager
import com.example.data.local.AppDatabase
import com.example.data.local.entities.HoneycombShortcutEntity
import com.example.data.local.entities.LabelEntity
import com.example.data.local.entities.ProfileEntity
import com.example.data.local.entities.ProjectEntity
import com.example.data.local.entities.TaskEntity
import com.example.data.preferences.UserPreferences
import com.example.data.repository.LifeRepository
import com.example.domain.model.LabelItem
import com.example.domain.model.ProjectItem
import com.example.domain.model.TaskItem
import kotlinx.coroutines.flow.first
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
class Release3Phase5Batch05BUnitTest {

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

        runBlocking {
            // Seed default profile
            database.profileDao().insertProfile(ProfileEntity.createDefaultPersonal())
            preferences.setActiveProfileId(ProfileEntity.DEFAULT_PERSONAL_PROFILE_ID)
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testProfileCreationAndSwitching() = runBlocking {
        // 1. Initial state has default personal profile (id = 1)
        val initialProfiles = repository.getAllProfiles()
        assertEquals(1, initialProfiles.size)
        assertEquals("Personal", initialProfiles[0].name)
        assertTrue(initialProfiles[0].isDefault)

        // 2. Create Work profile
        val workProfileId = repository.createProfile(
            name = "Work & Startup",
            emoji = "💼",
            colorHex = 0xFF8B5CF6,
            type = ProfileEntity.TYPE_WORK
        )
        assertTrue("Work profile ID must be > 1", workProfileId > 1)

        val profilesAfter = repository.getAllProfiles()
        assertEquals(2, profilesAfter.size)

        // 3. Switch active profile
        assertEquals(ProfileEntity.DEFAULT_PERSONAL_PROFILE_ID, repository.getActiveProfileId())
        repository.setActiveProfileId(workProfileId)
        assertEquals(workProfileId, repository.getActiveProfileId())
    }

    @Test
    fun testCrossProfileLeakagePreventionOnTaskInsert() = runBlocking {
        // Create Work profile (profileId = 2)
        val workProfileId = repository.createProfile(
            name = "Work",
            emoji = "💼",
            colorHex = 0xFF8B5CF6,
            type = ProfileEntity.TYPE_WORK
        )

        // Create Project belonging to Personal profile (profileId = 1)
        val personalProjectId = database.projectDao().insertProject(
            ProjectEntity(
                name = "Personal Home Renovation",
                profileId = ProfileEntity.DEFAULT_PERSONAL_PROFILE_ID
            )
        )

        // Create Label belonging to Personal profile (profileId = 1)
        val personalLabelId = database.labelDao().insertLabel(
            LabelEntity(
                name = "home_repairs",
                profileId = ProfileEntity.DEFAULT_PERSONAL_PROFILE_ID
            )
        )

        // Switch to Work profile
        repository.setActiveProfileId(workProfileId)
        assertEquals(workProfileId, repository.getActiveProfileId())

        // Insert task into Work profile while erroneously referencing Personal project and label
        val taskInWork = TaskItem(
            profileId = workProfileId,
            title = "Quarterly Sprint Review",
            dueDateIso = "2026-11-01",
            projectId = personalProjectId,
            labels = listOf(LabelItem(id = personalLabelId, name = "home_repairs", profileId = ProfileEntity.DEFAULT_PERSONAL_PROFILE_ID))
        )
        val taskId = repository.insertTask(taskInWork)

        // Verify that the task in the database has nullified cross-profile projectId and filtered cross-profile label
        val savedTask = database.taskDao().getTaskById(taskId)
        assertNotNull(savedTask)
        assertEquals(workProfileId, savedTask?.profileId)
        assertNull("Cross-profile projectId must be nullified to prevent data leakage", savedTask?.projectId)

        val taskLabels = database.labelDao().getLabelsForTaskList(taskId)
        assertTrue("Cross-profile labels must be filtered out", taskLabels.isEmpty())
    }

    @Test
    fun testProfileCascadeDeletionAndActiveFallback() = runBlocking {
        // Create Custom profile (profileId = 2)
        val customProfileId = repository.createProfile(
            name = "Side Project",
            emoji = "🚀",
            colorHex = 0xFF10B981,
            type = ProfileEntity.TYPE_CUSTOM
        )

        // Switch active profile to custom profile
        repository.setActiveProfileId(customProfileId)
        assertEquals(customProfileId, repository.getActiveProfileId())

        // Insert tasks, habits, and shortcuts for this profile
        database.taskDao().insertTask(
            TaskEntity(
                profileId = customProfileId,
                title = "Launch MVP",
                category = "Work",
                dueDateIso = "2026-11-05"
            )
        )
        database.honeycombShortcutDao().insertShortcut(
            HoneycombShortcutEntity(
                profileId = customProfileId,
                type = "CUSTOM",
                targetId = "mvp",
                title = "Launch",
                emoji = "🚀"
            )
        )

        assertEquals(1, database.taskDao().getTasksForProfileList(customProfileId).size)
        assertEquals(1, database.honeycombShortcutDao().getShortcutsForProfileList(customProfileId).size)

        // Delete the active custom profile
        val deleted = repository.deleteProfile(customProfileId)
        assertTrue("Custom profile deletion should succeed", deleted)

        // 1. Data belonging to deleted profile must be cleared
        assertEquals(0, database.taskDao().getTasksForProfileList(customProfileId).size)
        assertEquals(0, database.honeycombShortcutDao().getShortcutsForProfileList(customProfileId).size)

        // 2. Active profile must automatically fall back to default Personal profile
        assertEquals(
            "Active profile must fall back to default Personal profile",
            ProfileEntity.DEFAULT_PERSONAL_PROFILE_ID,
            repository.getActiveProfileId()
        )
    }

    @Test
    fun testDefaultProfileDeletionProtection() = runBlocking {
        // Attempt to delete default personal profile (id = 1)
        val deleteResult = repository.deleteProfile(ProfileEntity.DEFAULT_PERSONAL_PROFILE_ID)
        assertFalse("Default personal profile cannot be deleted", deleteResult)

        val profiles = repository.getAllProfiles()
        assertEquals(1, profiles.size)
        assertEquals(ProfileEntity.DEFAULT_PERSONAL_PROFILE_ID, profiles[0].id)
    }

    @Test
    fun testDataExportImportManagerProfileRoundtrip() = runBlocking {
        // 1. Create a custom profile
        val workId = database.profileDao().insertProfile(
            ProfileEntity(
                name = "Acme Corp",
                emoji = "🏢",
                colorHex = 0xFFF59E0BL,
                type = ProfileEntity.TYPE_WORK,
                orderIndex = 1,
                isDefault = false
            )
        )

        // 2. Insert records with specific profileId
        database.taskDao().insertTask(
            TaskEntity(
                profileId = workId,
                title = "Prepare Pitch Deck",
                category = "Work",
                dueDateIso = "2026-11-10"
            )
        )

        // 3. Export all data to JSON
        val exportedJson = DataExportImportManager.exportAllData(database)
        assertTrue("Exported JSON must contain profiles key", exportedJson.contains("\"profiles\""))
        assertTrue("Exported JSON must contain Acme Corp profile", exportedJson.contains("Acme Corp"))
        assertTrue("Exported JSON must contain profileId attribute", exportedJson.contains("\"profileId\": $workId"))

        // 4. Create fresh in-memory database and import JSON
        val newDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val importSuccess = DataExportImportManager.importData(newDb, exportedJson)
        assertTrue("Import must succeed", importSuccess)

        // 5. Verify profiles and tasks in new database
        val importedProfiles = newDb.profileDao().getAllProfiles()
        assertEquals(2, importedProfiles.size)
        val importedWorkProfile = importedProfiles.find { it.name == "Acme Corp" }
        assertNotNull(importedWorkProfile)
        assertEquals("🏢", importedWorkProfile?.emoji)

        val workTasks = newDb.taskDao().getTasksForProfileList(workId)
        assertEquals(1, workTasks.size)
        assertEquals("Prepare Pitch Deck", workTasks[0].title)

        newDb.close()
    }
}
