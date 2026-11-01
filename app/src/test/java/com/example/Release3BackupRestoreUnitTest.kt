package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.backup.DataExportImportManager
import com.example.data.local.AppDatabase
import com.example.data.local.entities.GlossaryItemEntity
import com.example.data.local.entities.GlossaryPackEntity
import com.example.data.local.entities.HabitEntity
import com.example.data.local.entities.LabelEntity
import com.example.data.local.entities.MedicationEntity
import com.example.data.local.entities.PlanEntity
import com.example.data.local.entities.PlanMilestoneEntity
import com.example.data.local.entities.PlanTaskCrossRef
import com.example.data.local.entities.ProfileEntity
import com.example.data.local.entities.ProfileSurveyEntity
import com.example.data.local.entities.ProjectEntity
import com.example.data.local.entities.ProjectWorkspaceItemEntity
import com.example.data.local.entities.TaskEntity
import com.example.data.local.entities.TaskLabelCrossRef
import com.example.data.local.entities.WaterLogEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
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
class Release3BackupRestoreUnitTest {

    private lateinit var context: Context
    private lateinit var sourceDb: AppDatabase
    private lateinit var targetDb: AppDatabase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        sourceDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        targetDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        sourceDb.close()
        targetDb.close()
    }

    @Test
    fun testComprehensiveExportAndImportRoundtrip() = runBlocking {
        // 1. Seed source DB with Release 3 entities
        val personalProfile = ProfileEntity.createDefaultPersonal()
        val workProfile = ProfileEntity(
            id = 2L,
            name = "Work",
            emoji = "💼",
            colorHex = 0xFF10B981,
            type = ProfileEntity.TYPE_WORK,
            orderIndex = 1,
            isDefault = false,
            createdAt = 1730000000000L,
            updatedAt = 1730000000000L
        )
        sourceDb.profileDao().insertProfile(personalProfile)
        sourceDb.profileDao().insertProfile(workProfile)

        val survey = ProfileSurveyEntity(
            profileId = 1L,
            displayName = "Alex",
            gender = "Non-binary",
            sleepingTime = "23:00",
            dailyWaterGoalMl = 2500,
            discoverySource = "Search",
            discoverySocialPlatform = null,
            discoveryOtherDetail = null,
            isCompleted = true,
            isSkipped = false,
            completedAt = 1730000100000L,
            updatedAt = 1730000100000L
        )
        sourceDb.profileSurveyDao().insertOrUpdateSurvey(survey)

        val project = ProjectEntity(
            id = 101L,
            profileId = 1L,
            name = "Release 3 QA",
            colorId = "blue",
            createdAt = 1730000000000L,
            updatedAt = 1730000000000L
        )
        sourceDb.projectDao().insertProject(project)

        val task = TaskEntity(
            id = 201L,
            profileId = 1L,
            title = "Validate Backup Integrity",
            emoji = "📦",
            description = "Test all Release 3 tables",
            category = "Testing",
            priority = "HIGH",
            dueDateIso = "2026-11-05",
            dueTime = "14:30",
            deadlineIso = "2026-11-10",
            projectId = 101L,
            recurrenceType = "MONTHLY",
            recurrenceInterval = 1,
            recurrenceWindowStartDay = 1,
            recurrenceWindowEndDay = 5,
            notes = "Test notes",
            createdAt = 1730000000000L
        )
        sourceDb.taskDao().insertTask(task)

        val label = LabelEntity(
            id = 301L,
            profileId = 1L,
            name = "Critical",
            colorId = "red",
            createdAt = 1730000000000L,
            updatedAt = 1730000000000L
        )
        sourceDb.labelDao().insertLabel(label)
        sourceDb.labelDao().insertTaskLabelCrossRef(TaskLabelCrossRef(taskId = 201L, labelId = 301L))

        val workspaceItem = ProjectWorkspaceItemEntity(
            id = 401L,
            profileId = 1L,
            projectId = 101L,
            title = "Test Checklist",
            category = "Feature",
            version = "V1",
            notes = "Checklist notes",
            status = "NOT_STARTED",
            progress = 0
        )
        sourceDb.projectWorkspaceDao().insertItem(workspaceItem)

        val habit = HabitEntity(
            id = 501L,
            profileId = 1L,
            title = "Morning Hydration",
            emoji = "💧",
            description = "Drink water",
            colorHex = 0xFF06B6D4,
            orderIndex = 0,
            createdAt = 1730000000000L
        )
        sourceDb.habitDao().insertHabit(habit)

        val plan = PlanEntity(
            id = 601L,
            profileId = 1L,
            title = "Quarterly Health Sprint",
            description = "Hydration & Routine",
            level = "MONTHLY",
            startDateIso = "2026-11-01",
            targetDateIso = "2026-11-30",
            status = "IN_PROGRESS",
            manualProgress = 40,
            isAutoProgress = false,
            parentPlanId = null,
            projectId = 101L,
            priority = "HIGH",
            createdAt = 1730000000000L,
            updatedAt = 1730000000000L
        )
        sourceDb.planDao().insertPlan(plan)

        val milestone = PlanMilestoneEntity(
            id = 701L,
            planId = 601L,
            title = "Reach 2.5L daily avg",
            isCompleted = true,
            orderIndex = 0
        )
        sourceDb.planDao().insertMilestones(listOf(milestone))

        sourceDb.planDao().insertPlanTaskCrossRefs(listOf(PlanTaskCrossRef(planId = 601L, taskId = 201L)))

        val medication = MedicationEntity(
            id = 801L,
            profileId = 1L,
            taskId = 201L,
            name = "Vitamin D3",
            form = "CAPSULE",
            dosageAmount = "1",
            dosageUnit = "PILL",
            strength = "2000 IU",
            instructions = "Take after breakfast",
            scheduleType = "DAILY",
            mealPeriodsJson = "[\"BREAKFAST\"]",
            fixedTimesJson = "[\"09:00\"]",
            intervalHours = 24,
            intervalModel = "FIXED_SCHEDULE",
            intervalAnchorTime = "09:00",
            allowOvernight = false,
            selectedDaysOfWeek = "1,2,3,4,5,6,7",
            startDateIso = "2026-11-01",
            endDateIso = null,
            courseDurationDays = null,
            isAsNeeded = false,
            isPaused = false,
            reminderSound = "CHIME",
            alertStyle = "HIGH_PRIORITY",
            snoozeMinutes = 10,
            followUpEnabled = true,
            followUpIntervalMinutes = 15,
            maxFollowUps = 2,
            privacySafeNotification = true,
            createdAt = 1730000000000L
        )
        sourceDb.medicationDao().insertMedication(medication)

        val waterLog = WaterLogEntity(
            dateIso = "2026-11-01",
            amountMl = 1500,
            goalMl = 2500,
            updatedAt = 1730000000000L
        )
        sourceDb.waterLogDao().insertOrUpdateWaterLog(waterLog)

        val pack = GlossaryPackEntity(
            id = "pack_custom",
            title = "Custom Productivity",
            description = "Custom terms",
            version = 1,
            author = "User",
            category = "Productivity",
            itemCount = 1,
            sha256 = "dummy_sha",
            isInstalled = true,
            installedAt = 1730000000000L,
            sizeBytes = 1024L
        )
        val term = GlossaryItemEntity(
            id = 1L,
            packId = "pack_custom",
            term = "Timeboxing",
            phonetic = null,
            category = "Productivity",
            definition = "Allocating fixed time blocks",
            detailedExplanation = "Block 30 min",
            examplesJson = "[]",
            relatedTermsJson = "[]",
            tagsJson = "[]",
            isBookmarked = false,
            createdAt = 1730000000000L
        )
        sourceDb.glossaryDao().insertPack(pack)
        sourceDb.glossaryDao().insertItems(listOf(term))

        // 2. Export all data to JSON
        val exportedJson = DataExportImportManager.exportAllData(sourceDb)
        assertNotNull(exportedJson)
        val root = JSONObject(exportedJson)

        assertTrue(root.has("profiles"))
        assertTrue(root.has("profile_surveys"))
        assertTrue(root.has("tasks"))
        assertTrue(root.has("projects"))
        assertTrue(root.has("project_workspace_items"))
        assertTrue(root.has("labels"))
        assertTrue(root.has("habits"))
        assertTrue(root.has("plans"))
        assertTrue(root.has("plan_milestones"))
        assertTrue(root.has("plan_task_cross_refs"))
        assertTrue(root.has("medications"))
        assertTrue(root.has("water_logs"))
        assertTrue(root.has("glossary_packs"))
        assertTrue(root.has("glossary_items"))

        // 3. Import JSON into fresh target database
        val importSuccess = DataExportImportManager.importData(targetDb, exportedJson)
        assertTrue(importSuccess)

        // 4. Verify entities in target database
        val restoredProfiles = targetDb.profileDao().getAllProfiles()
        assertEquals(2, restoredProfiles.size)

        val restoredSurvey = targetDb.profileSurveyDao().getSurveyForProfile(1L)
        assertNotNull(restoredSurvey)
        assertEquals("Alex", restoredSurvey?.displayName)
        assertEquals(2500, restoredSurvey?.dailyWaterGoalMl)

        val restoredTasks = targetDb.taskDao().getAllTasksList()
        assertEquals(1, restoredTasks.size)
        val restoredTask = restoredTasks[0]
        assertEquals("Validate Backup Integrity", restoredTask.title)
        assertEquals(1, restoredTask.recurrenceWindowStartDay)
        assertEquals(5, restoredTask.recurrenceWindowEndDay)
        assertEquals("2026-11-10", restoredTask.deadlineIso)
        assertEquals("📦", restoredTask.emoji)

        val restoredPlans = targetDb.planDao().getAllPlansList()
        assertEquals(1, restoredPlans.size)
        assertEquals("Quarterly Health Sprint", restoredPlans[0].title)

        val restoredMilestones = targetDb.planDao().getAllMilestonesList()
        assertEquals(1, restoredMilestones.size)
        assertEquals("Reach 2.5L daily avg", restoredMilestones[0].title)
        assertTrue(restoredMilestones[0].isCompleted)

        val restoredPlanRefs = targetDb.planDao().getAllPlanTaskCrossRefs()
        assertEquals(1, restoredPlanRefs.size)
        assertEquals(601L, restoredPlanRefs[0].planId)
        assertEquals(201L, restoredPlanRefs[0].taskId)

        val restoredMeds = targetDb.medicationDao().getAllMedicationsList()
        assertEquals(1, restoredMeds.size)
        assertEquals("Vitamin D3", restoredMeds[0].name)
        assertEquals("HIGH_PRIORITY", restoredMeds[0].alertStyle)
        assertEquals(15, restoredMeds[0].followUpIntervalMinutes)

        val restoredWaterLogs = targetDb.waterLogDao().getAllWaterLogsList()
        assertEquals(1, restoredWaterLogs.size)
        assertEquals(1500, restoredWaterLogs[0].amountMl)

        val restoredPacks = targetDb.glossaryDao().getAllPacksFlow().first()
        assertEquals(1, restoredPacks.size)
        assertEquals("pack_custom", restoredPacks[0].id)
    }

    @Test
    fun testLegacyBackupImportCompatibility() = runBlocking {
        // Pre-Release 3 backup JSON where profileId was not present and new tables were omitted
        val legacyJson = """
        {
            "version": 1,
            "exportedAt": 1700000000000,
            "tasks": [
                {
                    "id": 99,
                    "title": "Legacy Task without profileId",
                    "notes": "",
                    "isCompleted": false,
                    "priority": "MEDIUM",
                    "dueDateIso": "2026-11-05",
                    "category": "General",
                    "recurrenceType": "NONE",
                    "createdAt": 1700000000000
                }
            ],
            "projects": [
                {
                    "id": 88,
                    "name": "Legacy Project",
                    "colorId": "blue",
                    "createdAt": 1700000000000,
                    "updatedAt": 1700000000000
                }
            ]
        }
        """.trimIndent()

        val success = DataExportImportManager.importData(targetDb, legacyJson)
        assertTrue(success)

        val tasks = targetDb.taskDao().getAllTasksList()
        assertEquals(1, tasks.size)
        assertEquals("Legacy Task without profileId", tasks[0].title)
        // Should default profileId to 1L
        assertEquals(ProfileEntity.DEFAULT_PERSONAL_PROFILE_ID, tasks[0].profileId)
    }

    @Test
    fun testCorruptedJsonImportHandling() = runBlocking {
        val corruptedJson = "{ not a valid json at all "
        val result = DataExportImportManager.importData(targetDb, corruptedJson)
        assertFalse(result)
    }
}
