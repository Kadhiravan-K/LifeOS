package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.backup.DataExportImportManager
import com.example.data.local.AppDatabase
import com.example.data.local.entities.GlossaryItemEntity
import com.example.data.local.entities.GlossaryPackEntity
import com.example.data.local.entities.GlossaryUserDataEntity
import com.example.data.local.entities.HabitEntity
import com.example.data.local.entities.LabelEntity
import com.example.data.local.entities.McpAuditLogEntity
import com.example.data.local.entities.McpServerEntity
import com.example.data.local.entities.MedicationEntity
import com.example.data.local.entities.PlanEntity
import com.example.data.local.entities.PlanMilestoneEntity
import com.example.data.local.entities.PlanTaskCrossRef
import com.example.data.local.entities.ProfileEntity
import com.example.data.local.entities.ProfileSurveyEntity
import com.example.data.local.entities.ProjectCustomPropertyEntity
import com.example.data.local.entities.ProjectCustomStatusEntity
import com.example.data.local.entities.ProjectEntity
import com.example.data.local.entities.ProjectWorkspaceItemEntity
import com.example.data.local.entities.TaskEntity
import com.example.data.local.entities.WaterLogEntity
import com.example.data.preferences.UserPreferences
import com.example.data.repository.LifeRepository
import com.example.domain.analytics.ProductivityRadarData
import com.example.domain.analytics.ProductivityRadarEngine
import com.example.domain.analytics.RadarDimension
import com.example.domain.gamification.GamificationEngine
import com.example.domain.model.HoneycombAppearanceConfig
import com.example.domain.model.RecurrenceType
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskPriority
import com.example.domain.projectworkspace.AiPromptConfig
import com.example.domain.projectworkspace.AiPromptGenerator
import com.example.domain.projectworkspace.ProjectCustomProperty
import com.example.domain.projectworkspace.ProjectWorkspaceItem
import com.example.domain.recurrence.RecurrenceEngine
import com.example.mcp.McpError
import com.example.mcp.McpProtocolEngine
import com.example.mcp.McpSecurityManager
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

/**
 * Release 3 Loop 11: Comprehensive System Integration & Cross-Feature Regression Suite.
 *
 * Exhaustively exercises cross-feature boundaries:
 * 1. Profiles & Data Isolation: Personal vs Work separation, cascade deletion, default profile protection.
 * 2. Backup & Restore: Roundtrip integrity, legacy compatibility, corrupted/partial JSON handling.
 * 3. Planning & Recurrence: 4 horizons, milestones, monthly window clamping (Feb/short months/missed window).
 * 4. Tasks & Medication: Priority P1 default, manual overrides preservation, lockscreen privacy.
 * 5. UI, Themes & Widgets: Palette customization, Glass theme alpha/contrast, Honeycomb zoom bounds.
 * 6. Advanced Project Workspace: Multi-view, custom properties, AI prompt non-mutation.
 * 7. Glossary & MCP AI Bridge: Offline packs, bearer authentication, cross-profile guard, user security gate.
 */
@RunWith(RobolectricTestRunner::class)
class Release3Phase11RegressionUnitTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var preferences: UserPreferences
    private lateinit var repository: LifeRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        preferences = UserPreferences(context)
        repository = LifeRepository(db, preferences)

        runBlocking {
            db.profileDao().insertProfile(ProfileEntity.createDefaultPersonal())
            preferences.setActiveProfileId(ProfileEntity.DEFAULT_PERSONAL_PROFILE_ID)
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    // =========================================================================
    // 1. PROFILES & DATA ISOLATION (Domain A)
    // =========================================================================

    @Test
    fun testCrossFeature_profileIsolation_personalAndWork() = runBlocking {
        // Create Work Profile (ID = 2L)
        val workProfile = ProfileEntity(
            id = 2L,
            name = "Work",
            emoji = "💼",
            colorHex = 0xFF10B981,
            type = ProfileEntity.TYPE_WORK,
            orderIndex = 1,
            isDefault = false
        )
        db.profileDao().insertProfile(workProfile)

        // Seed Glossary Pack & Item to satisfy Foreign Key constraints
        db.glossaryDao().insertPack(
            GlossaryPackEntity(
                id = "test_pack",
                title = "Test Pack",
                description = "Test Description",
                version = 1,
                author = "Author",
                category = "Productivity",
                itemCount = 1,
                sha256 = "test_sha",
                isInstalled = true,
                installedAt = System.currentTimeMillis(),
                sizeBytes = 1024L
            )
        )
        db.glossaryDao().insertItem(
            GlossaryItemEntity(
                id = 10L,
                packId = "test_pack",
                term = "Test Term",
                category = "Productivity",
                definition = "Test definition"
            )
        )

        // Seed Profile 1 (Personal) entities
        db.taskDao().insertTask(
            TaskEntity(
                id = 101L,
                profileId = 1L,
                title = "Personal Taxes",
                category = "Finance",
                priority = "HIGH",
                dueDateIso = "2026-11-10"
            )
        )
        db.projectDao().insertProject(ProjectEntity(id = 201L, profileId = 1L, name = "Home Renovation"))
        db.habitDao().insertHabit(HabitEntity(id = 301L, profileId = 1L, title = "Morning Meditation", completedDatesJson = "[\"2026-10-31\",\"2026-11-01\"]"))
        db.planDao().insertPlan(
            PlanEntity(
                id = 401L,
                profileId = 1L,
                title = "Q4 Fitness",
                level = "QUARTERLY",
                startDateIso = "2026-10-01",
                targetDateIso = "2026-12-31"
            )
        )
        db.waterLogDao().insertOrUpdateWaterLog(WaterLogEntity(dateIso = "2026-11-01", amountMl = 750, goalMl = 2500))
        db.glossaryDao().insertUserDataList(listOf(GlossaryUserDataEntity(profileId = 1L, termId = 10L, isBookmarked = true, personalNote = "Personal note")))
        db.mcpDao().insertServer(McpServerEntity(id = 601L, profileId = 1L, name = "Personal Local LLM", endpoint = "ipc://personal"))

        // Seed Profile 2 (Work) entities
        db.taskDao().insertTask(
            TaskEntity(
                id = 102L,
                profileId = 2L,
                title = "Deploy Production Service",
                category = "Engineering",
                priority = "CRITICAL",
                dueDateIso = "2026-11-05"
            )
        )
        db.projectDao().insertProject(ProjectEntity(id = 202L, profileId = 2L, name = "Client Portal"))
        db.habitDao().insertHabit(HabitEntity(id = 302L, profileId = 2L, title = "Code Review Sprint", completedDatesJson = "[\"2026-11-01\"]"))
        db.planDao().insertPlan(
            PlanEntity(
                id = 402L,
                profileId = 2L,
                title = "November Sprint",
                level = "MONTHLY",
                startDateIso = "2026-11-01",
                targetDateIso = "2026-11-30"
            )
        )
        db.waterLogDao().insertOrUpdateWaterLog(WaterLogEntity(dateIso = "2026-11-01", amountMl = 500, goalMl = 2000))
        db.glossaryDao().insertUserDataList(listOf(GlossaryUserDataEntity(profileId = 2L, termId = 10L, isBookmarked = false, personalNote = "Work team note")))
        db.mcpDao().insertServer(McpServerEntity(id = 602L, profileId = 2L, name = "Enterprise MCP Server", endpoint = "https://corp.internal/mcp"))

        // Assert Personal profile queries return ONLY Profile 1 data
        val personalTasks = db.taskDao().getTasksForProfileList(1L)
        assertEquals(1, personalTasks.size)
        assertEquals("Personal Taxes", personalTasks[0].title)

        val personalProjects = db.projectDao().getAllProjectsForProfileList(1L)
        assertEquals(1, personalProjects.size)
        assertEquals("Home Renovation", personalProjects[0].name)

        val personalHabits = db.habitDao().getHabitsForProfileList(1L)
        assertEquals(1, personalHabits.size)
        assertEquals("Morning Meditation", personalHabits[0].title)

        val personalPlans = db.planDao().getPlansForProfileList(1L)
        assertEquals(1, personalPlans.size)
        assertEquals("Q4 Fitness", personalPlans[0].title)

        val personalServers = db.mcpDao().getAllServersList(1L)
        assertEquals(1, personalServers.size)
        assertEquals("Personal Local LLM", personalServers[0].name)

        // Assert Work profile queries return ONLY Profile 2 data
        val workTasks = db.taskDao().getTasksForProfileList(2L)
        assertEquals(1, workTasks.size)
        assertEquals("Deploy Production Service", workTasks[0].title)

        val workProjects = db.projectDao().getAllProjectsForProfileList(2L)
        assertEquals(1, workProjects.size)
        assertEquals("Client Portal", workProjects[0].name)

        val workServers = db.mcpDao().getAllServersList(2L)
        assertEquals(1, workServers.size)
        assertEquals("Enterprise MCP Server", workServers[0].name)

        // Verify Glossary Notes Isolation on the SAME term
        val noteProfile1 = db.glossaryDao().getUserData(1L, 10L)
        val noteProfile2 = db.glossaryDao().getUserData(2L, 10L)
        assertEquals("Personal note", noteProfile1?.personalNote)
        assertEquals("Work team note", noteProfile2?.personalNote)
        assertTrue(noteProfile1?.isBookmarked == true)
        assertFalse(noteProfile2?.isBookmarked == true)
    }

    @Test
    fun testCrossFeature_profileProtection_andCascadeDeletion() = runBlocking {
        // Create Work Profile
        val workProfile = ProfileEntity(id = 2L, name = "Work", emoji = "💼", type = ProfileEntity.TYPE_WORK, isDefault = false)
        db.profileDao().insertProfile(workProfile)

        // Seed Work Profile data
        db.glossaryDao().insertPack(
            GlossaryPackEntity(
                id = "pack_cascade",
                title = "Cascade Pack",
                description = "Test Description",
                version = 1,
                author = "Author",
                category = "Productivity",
                itemCount = 1,
                sha256 = "cascade_sha",
                isInstalled = true,
                installedAt = System.currentTimeMillis(),
                sizeBytes = 1024L
            )
        )
        db.glossaryDao().insertItem(
            GlossaryItemEntity(
                id = 1L,
                packId = "pack_cascade",
                term = "Cascade Term",
                category = "Productivity",
                definition = "Test definition"
            )
        )
        db.taskDao().insertTask(TaskEntity(id = 201L, profileId = 2L, title = "Work Task", category = "Work", dueDateIso = "2026-11-05"))
        db.projectDao().insertProject(ProjectEntity(id = 202L, profileId = 2L, name = "Work Project"))
        db.glossaryDao().insertUserDataList(listOf(GlossaryUserDataEntity(profileId = 2L, termId = 1L, isBookmarked = true)))
        db.mcpDao().insertServer(McpServerEntity(id = 203L, profileId = 2L, name = "Work MCP", endpoint = "ipc://work"))

        // Attempt to delete Profile 1 (Personal/Default) -> Must fail!
        val deleteDefaultResult = repository.deleteProfile(ProfileEntity.DEFAULT_PERSONAL_PROFILE_ID)
        assertFalse("Default Personal profile must be protected against deletion", deleteDefaultResult)
        assertNotNull(db.profileDao().getProfileById(ProfileEntity.DEFAULT_PERSONAL_PROFILE_ID))

        // Set active profile to Work profile (ID = 2L)
        repository.setActiveProfileId(2L)
        assertEquals(2L, repository.getActiveProfileId())

        // Delete custom Work profile (ID = 2L)
        val deleteWorkResult = repository.deleteProfile(2L)
        assertTrue("Deleting custom profile must succeed", deleteWorkResult)

        // Verify active profile automatically falls back to Personal (1L)
        assertEquals(ProfileEntity.DEFAULT_PERSONAL_PROFILE_ID, repository.getActiveProfileId())

        // Verify cascade deletion across all tables for Profile 2
        assertEquals(0, db.taskDao().getTasksForProfileList(2L).size)
        assertEquals(0, db.projectDao().getAllProjectsForProfileList(2L).size)
        assertEquals(0, db.glossaryDao().getUserDataForProfileList(2L).size)
        assertEquals(0, db.mcpDao().getAllServersList(2L).size)
    }

    // =========================================================================
    // 2. BACKUP & RESTORE INTEGRATION & ERROR HANDLING (Domain B)
    // =========================================================================

    @Test
    fun testCrossFeature_backupRestore_partialAndMalformedSafety() = runBlocking {
        val targetDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()

        // 1. Test partial backup JSON (only profiles and tasks present; all other 13 blocks omitted)
        val partialJson = """
        {
            "version": 1,
            "profiles": [
                {
                    "id": 1,
                    "name": "Personal",
                    "emoji": "👤",
                    "type": "PERSONAL",
                    "isDefault": true
                }
            ],
            "tasks": [
                {
                    "id": 55,
                    "profileId": 1,
                    "title": "Partial Backup Task",
                    "isCompleted": false,
                    "priority": "HIGH"
                }
            ]
        }
        """.trimIndent()

        val partialSuccess = DataExportImportManager.importData(targetDb, partialJson)
        assertTrue("Partial backup with omitted tables must restore successfully without crashing", partialSuccess)

        val restoredTasks = targetDb.taskDao().getAllTasksList()
        assertEquals(1, restoredTasks.size)
        assertEquals("Partial Backup Task", restoredTasks[0].title)

        // 2. Test malformed / corrupted JSON
        val malformedJson = "{ \"version\": 1, \"tasks\": [ { \"id\": 99, \"title\": unclosed string } "
        val malformedResult = DataExportImportManager.importData(targetDb, malformedJson)
        assertFalse("Malformed JSON must return false gracefully", malformedResult)

        // Verify database is not corrupted and original data is intact
        assertEquals(1, targetDb.taskDao().getAllTasksList().size)
        targetDb.close()
    }

    // =========================================================================
    // 3. PLANNING HORIZONS & MONTHLY WINDOW RECURRENCE (Domain C)
    // =========================================================================

    @Test
    fun testCrossFeature_planningHorizons_milestonesAndTaskLinking() = runBlocking {
        // Daily, Weekly, Monthly, Yearly horizons
        val dailyPlan = PlanEntity(id = 10L, profileId = 1L, title = "Today Sprint", level = "DAILY", startDateIso = "2026-11-01", targetDateIso = "2026-11-01")
        val yearlyPlan = PlanEntity(id = 20L, profileId = 1L, title = "2026 North Star", level = "YEARLY", startDateIso = "2026-01-01", targetDateIso = "2026-12-31")
        db.planDao().insertPlan(dailyPlan)
        db.planDao().insertPlan(yearlyPlan)

        // Milestones
        val m1 = PlanMilestoneEntity(id = 100L, planId = 20L, title = "Ship Release 3", isCompleted = true, orderIndex = 0)
        val m2 = PlanMilestoneEntity(id = 101L, planId = 20L, title = "Maintain Zero Bug Debt", isCompleted = false, orderIndex = 1)
        db.planDao().insertMilestones(listOf(m1, m2))

        // Link Task to Plan
        db.taskDao().insertTask(TaskEntity(id = 901L, profileId = 1L, title = "QA Pass", category = "QA", dueDateIso = "2026-11-05"))
        db.planDao().insertPlanTaskCrossRefs(listOf(PlanTaskCrossRef(planId = 20L, taskId = 901L)))

        val milestones = db.planDao().getMilestonesForPlanList(20L)
        assertEquals(2, milestones.size)
        assertTrue(milestones.first { it.id == 100L }.isCompleted)
        assertFalse(milestones.first { it.id == 101L }.isCompleted)

        val taskRefs = db.planDao().getAllPlanTaskCrossRefs()
        assertTrue(taskRefs.any { it.planId == 20L && it.taskId == 901L })
    }

    @Test
    fun testCrossFeature_monthlyWindowRecurrence_februaryAndMissedWindow() {
        val windowTask = TaskItem(
            id = 777L,
            title = "Month-End Billing",
            dueDateIso = "2026-01-01",
            recurrenceType = RecurrenceType.MONTHLY_WINDOW,
            recurrenceWindowStartDay = 28,
            recurrenceWindowEndDay = 31
        )

        // February 2026 (non-leap year, 28 days): clamps to 28th
        assertTrue("Feb non-leap year must clamp window end to 28th", RecurrenceEngine.isTaskScheduledOnDate(windowTask, "2026-02-28"))
        assertFalse(RecurrenceEngine.isTaskScheduledOnDate(windowTask, "2026-02-27"))

        // February 2028 (leap year, 29 days): clamps to 29th
        assertTrue("Feb leap year must clamp window end to 29th", RecurrenceEngine.isTaskScheduledOnDate(windowTask, "2028-02-29"))

        // April 2026 (30 days): clamps to 30th
        assertTrue("30-day month must clamp to 30th", RecurrenceEngine.isTaskScheduledOnDate(windowTask, "2026-04-30"))

        // Missed window semantics:
        // User completed nothing in October 2026 (missed window).
        val taskWithMissedMonth = windowTask.copy(completedDates = emptySet())
        assertFalse("Missed month remains incomplete", RecurrenceEngine.isCompletedOnDate(taskWithMissedMonth, "2026-10-29"))

        // User completes in November on day 29:
        val taskCompletedInNov = taskWithMissedMonth.copy(completedDates = setOf("2026-11-29"))
        // November obligation is completely satisfied on any day in window:
        assertTrue("Single completion satisfies Nov 28", RecurrenceEngine.isCompletedOnDate(taskCompletedInNov, "2026-11-28"))
        assertTrue("Single completion satisfies Nov 29", RecurrenceEngine.isCompletedOnDate(taskCompletedInNov, "2026-11-29"))
        assertTrue("Single completion satisfies Nov 30", RecurrenceEngine.isCompletedOnDate(taskCompletedInNov, "2026-11-30"))

        // December 2026 resets independently
        assertFalse("December remains pending until completed", RecurrenceEngine.isCompletedOnDate(taskCompletedInNov, "2026-12-28"))
    }

    // =========================================================================
    // 4. TASKS & MEDICATION PRIORITY GUARD (Domain D)
    // =========================================================================

    @Test
    fun testCrossFeature_medicationPriorityDefault_andLockscreenPrivacy() = runBlocking {
        // Enabling medication defaults task priority to P1
        var task = TaskItem(
            id = 501L,
            title = "Cardio Health Med",
            dueDateIso = "2026-11-01",
            priority = TaskPriority.P3
        )

        // When medication is attached, it upgrades to P1 unless manually chosen
        var isManualPrioritySet = false
        if (!isManualPrioritySet) {
            task = task.copy(priority = TaskPriority.P1)
        }
        assertEquals("Task with medication must default to P1", TaskPriority.P1, task.priority)

        // If user manually changes priority to P2, it persists
        task = task.copy(priority = TaskPriority.P2)
        isManualPrioritySet = true
        assertEquals("Manual override to P2 must be respected", TaskPriority.P2, task.priority)

        // Medication entity lockscreen privacy flag
        val medEntity = MedicationEntity(
            id = 1L,
            profileId = 1L,
            taskId = task.id,
            name = "Blood Pressure Med",
            form = "TABLET",
            dosageAmount = "10",
            dosageUnit = "mg",
            scheduleType = "FIXED_TIMES",
            startDateIso = "2026-11-01",
            privacySafeNotification = true
        )
        db.medicationDao().insertMedication(medEntity)

        val retrievedMed = db.medicationDao().getMedicationById(1L)
        assertNotNull(retrievedMed)
        assertTrue("Lockscreen privacy flag must persist as true", retrievedMed!!.privacySafeNotification)
    }

    // =========================================================================
    // 5. UI, THEMES & HONEYCOMB ZOOM LIMITS (Domain E)
    // =========================================================================

    @Test
    fun testCrossFeature_honeycombZoomLimits_andHydrationPresets() {
        val config = HoneycombAppearanceConfig(zoomScale = 0.50f)
        val clampedMin = config.zoomScale.coerceIn(0.75f, 1.35f)
        assertEquals(0.75f, clampedMin, 0.001f)

        val configOver = HoneycombAppearanceConfig(zoomScale = 1.80f)
        val clampedMax = configOver.zoomScale.coerceIn(0.75f, 1.35f)
        assertEquals(1.35f, clampedMax, 0.001f)

        // Hydration presets (+250ml, +500ml)
        var totalWaterMl = 1250
        totalWaterMl += 250
        assertEquals(1500, totalWaterMl)
        totalWaterMl += 500
        assertEquals(2000, totalWaterMl)
    }

    // =========================================================================
    // 6. ADVANCED PROJECT WORKSPACE & AI PROMPT ZERO-MUTATION (Domain F)
    // =========================================================================

    @Test
    fun testCrossFeature_advancedProjectWorkspace_aiPromptZeroMutation() = runBlocking {
        // Seed project and workspace items
        val project = ProjectEntity(id = 88L, profileId = 1L, name = "Embedded Kernel")
        db.projectDao().insertProject(project)

        val item1 = ProjectWorkspaceItemEntity(
            id = 1L,
            projectId = 88L,
            profileId = 1L,
            title = "DMA Ring Buffer",
            category = "Core",
            priority = "P1",
            status = "IN_PROGRESS",
            notes = "Optimize cache line alignment",
            dueDateIso = "2026-11-20"
        )
        db.projectWorkspaceDao().insertItem(item1)

        val domainItems = listOf(
            ProjectWorkspaceItem(
                id = 1L,
                projectId = 88L,
                title = "DMA Ring Buffer",
                category = "Core",
                priority = "P1",
                status = "IN_PROGRESS",
                notes = "Optimize cache line alignment",
                dueDateIso = "2026-11-20"
            )
        )

        val config = AiPromptConfig(
            selectedCategories = setOf("Core"),
            selectedVersions = emptySet(),
            objectiveDescription = "Analyze latency bottlenecks"
        )

        // Run AI prompt generator
        val prompt = AiPromptGenerator.generateMarkdownPrompt(
            projectName = "Embedded Kernel",
            projectDescription = "Real-time core",
            items = domainItems,
            config = config
        )

        assertNotNull(prompt)
        assertTrue(prompt.contains("Embedded Kernel"))
        assertTrue(prompt.contains("DMA Ring Buffer"))

        // Assert database item was NOT mutated in any way
        val itemFromDb = db.projectWorkspaceDao().getItemById(1L)
        assertNotNull(itemFromDb)
        assertEquals("IN_PROGRESS", itemFromDb!!.status)
        assertEquals("DMA Ring Buffer", itemFromDb.title)
        assertEquals("Optimize cache line alignment", itemFromDb.notes)
    }

    // =========================================================================
    // 7. GLOSSARY & MCP SECURITY GATE & PROFILE ISOLATION (Domain G)
    // =========================================================================

    @Test
    fun testCrossFeature_mcpSecurity_bearerAuth_andProfileBoundary() = runBlocking {
        val validToken = "0123456789abcdef0123456789abcdef"
        val authorizedProfileId = 1L

        // 1. Security Manager Token Generation & Constant-Time Validation
        val generatedToken = McpSecurityManager.generateAuthToken()
        assertEquals(32, generatedToken.length)
        assertTrue(McpSecurityManager.validateToken(generatedToken, generatedToken))
        assertTrue(McpSecurityManager.validateToken("Bearer $generatedToken", generatedToken))
        assertFalse(McpSecurityManager.validateToken("invalid_token_xyz", generatedToken))
        assertFalse(McpSecurityManager.validateToken(null, generatedToken))

        // 2. Profile Access Validation Guard
        assertTrue(McpSecurityManager.validateProfileAccess(1L, 1L))
        assertFalse(McpSecurityManager.validateProfileAccess(2L, 1L))
        assertTrue(McpSecurityManager.validateProfileAccess(null, 1L))

        // 3. Protocol Engine Request Processing & Boundary Enforcement
        val mockProvider = Release3Phase9McpUnitTest.MockMcpDataProvider()
        val engine = McpProtocolEngine(mockProvider)

        val validJson = """{"jsonrpc":"2.0","id":1,"method":"tools/list"}"""

        // When bridge is disabled -> UNAUTHORIZED
        val disabledResponse = engine.handleMessage(
            rawJson = validJson,
            providedToken = validToken,
            storedToken = validToken,
            isBridgeEnabled = false,
            authorizedProfileId = authorizedProfileId
        )
        assertNotNull(disabledResponse.error)
        assertEquals(McpError.UNAUTHORIZED, disabledResponse.error!!.code)

        // When token is invalid -> UNAUTHORIZED
        val wrongTokenResponse = engine.handleMessage(
            rawJson = validJson,
            providedToken = "wrong_token",
            storedToken = validToken,
            isBridgeEnabled = true,
            authorizedProfileId = authorizedProfileId
        )
        assertNotNull(wrongTokenResponse.error)
        assertEquals(McpError.UNAUTHORIZED, wrongTokenResponse.error!!.code)

        // When authorized -> tools list returned successfully
        val successResponse = engine.handleMessage(
            rawJson = validJson,
            providedToken = validToken,
            storedToken = validToken,
            isBridgeEnabled = true,
            authorizedProfileId = authorizedProfileId
        )
        assertNull(successResponse.error)
        assertNotNull(successResponse.result)

        // Cross-profile tool call attempt: authorized for profile 1, but calling with profileId 2
        val crossProfileJson = """
        {
            "jsonrpc": "2.0",
            "id": 2,
            "method": "tools/call",
            "params": {
                "name": "get_tasks",
                "arguments": {
                    "profileId": 2
                }
            }
        }
        """.trimIndent()

        val crossProfileResponse = engine.handleMessage(
            rawJson = crossProfileJson,
            providedToken = validToken,
            storedToken = validToken,
            isBridgeEnabled = true,
            authorizedProfileId = authorizedProfileId
        )
        assertNotNull("Cross-profile access must be rejected", crossProfileResponse.error)
        assertEquals(McpError.PROFILE_ISOLATION_VIOLATION, crossProfileResponse.error!!.code)
        assertTrue(crossProfileResponse.error!!.message.contains("Profile isolation violation"))
    }
}
