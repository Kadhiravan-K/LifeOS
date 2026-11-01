package com.example.data.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entities.AchievementEntity
import com.example.data.local.entities.BillingOccurrenceEntity
import com.example.data.local.entities.ChallengeEntity
import com.example.data.local.entities.GamificationProfileEntity
import com.example.data.local.entities.GlossaryItemEntity
import com.example.data.local.entities.GlossaryPackEntity
import com.example.data.local.entities.GlossaryUserDataEntity
import com.example.data.local.entities.HabitEntity
import com.example.data.local.entities.HoneycombShortcutEntity
import com.example.data.local.entities.InventoryAdjustmentEntity
import com.example.data.local.entities.LabelEntity
import com.example.data.local.entities.McpAuditLogEntity
import com.example.data.local.entities.McpServerEntity
import com.example.data.local.entities.MedicationDoseEntity
import com.example.data.local.entities.MedicationEntity
import com.example.data.local.entities.PlanEntity
import com.example.data.local.entities.PlanMilestoneEntity
import com.example.data.local.entities.PlanTaskCrossRef
import com.example.data.local.entities.PriceHistoryEntity
import com.example.data.local.entities.ProfileEntity
import com.example.data.local.entities.ProfileSurveyEntity
import com.example.data.local.entities.ProjectCustomPropertyEntity
import com.example.data.local.entities.ProjectCustomStatusEntity
import com.example.data.local.entities.ProjectEntity
import com.example.data.local.entities.ProjectWorkspaceItemEntity
import com.example.data.local.entities.ProjectWorkspaceViewEntity
import com.example.data.local.entities.RewardItemEntity
import com.example.data.local.entities.RewardRedemptionEntity
import com.example.data.local.entities.SubscriptionEntity
import com.example.data.local.entities.SubscriptionPaymentEntity
import com.example.data.local.entities.TaskEntity
import com.example.data.local.entities.TaskLabelCrossRef
import com.example.data.local.entities.TaskReminderEntity
import com.example.data.local.entities.TrackerEntity
import com.example.data.local.entities.TrackerPurchaseEntity
import com.example.data.local.entities.TrashItemEntity
import com.example.data.local.entities.UsageCycleEntity
import com.example.data.local.entities.UsageEntryEntity
import com.example.data.local.entities.WaterLogEntity
import com.example.data.local.entities.XpEventEntity
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
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(RobolectricTestRunner::class)
class Release4BackupRestoreV2UnitTest {

    private lateinit var context: Context
    private lateinit var sourceDb: AppDatabase
    private lateinit var targetDb: AppDatabase

    @Before
    fun setUp() {
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
    fun testAll40EntitiesRoundTripWithFieldPreservation() = runBlocking {
        // 1. Seed source DB with instances across all 40 entities
        seedCoreEntities(sourceDb)
        seedPlanningAndGamificationEntities(sourceDb)
        seedHealthAndTrackerEntities(sourceDb)
        seedSubscriptionAndWorkspaceEntities(sourceDb)
        seedGlossaryAndMcpEntities(sourceDb)

        // 2. Export Database to Backup v2
        val exportedJson = DataExportImportManager.exportAllData(sourceDb)
        assertNotNull(exportedJson)

        // Verify JSON root fields
        val root = JSONObject(exportedJson)
        assertEquals(2, root.getInt("version"))
        assertEquals(2, root.getInt("formatVersion"))
        assertEquals("4.0.0", root.getString("appVersion"))
        assertEquals(18, root.getInt("databaseVersion"))
        assertTrue("Export must contain metadata block", root.has("metadata"))

        val metadata = root.getJSONObject("metadata")
        assertEquals(2, metadata.getInt("formatVersion"))
        val checksum = metadata.getString("checksumSha256")
        assertTrue("Checksum must be 64-char SHA-256 hex string", checksum.length == 64)

        // 3. Validate backup before restore
        val validation = DataExportImportManager.validateBackup(exportedJson)
        assertTrue("Validation must succeed", validation is DataExportImportManager.ValidationResult.Valid)
        val summary = (validation as DataExportImportManager.ValidationResult.Valid).summary
        assertEquals(true, summary.isChecksumValid)
        assertEquals(false, summary.isLegacy)

        // Verify all 40 entities counted in summary
        DataExportImportManager.ALL_40_ENTITY_KEYS.forEach { key ->
            assertTrue("Entity count for '$key' must exist in summary", summary.entityCounts.containsKey(key))
            assertTrue("Entity count for '$key' must be > 0", summary.entityCounts[key]!! > 0)
        }

        // 4. Restore into empty target DB
        val restoreResult = DataExportImportManager.restoreData(targetDb, exportedJson, context)
        assertTrue("Restore must succeed", restoreResult is DataExportImportManager.RestoreResult.Success)

        // 5. Field-by-field verification across all 40 tables in targetDb
        verifyCoreEntities(targetDb)
        verifyPlanningAndGamificationEntities(targetDb)
        verifyHealthAndTrackerEntities(targetDb)
        verifySubscriptionAndWorkspaceEntities(targetDb)
        verifyGlossaryAndMcpEntities(targetDb)
    }

    private suspend fun seedCoreEntities(sourceDb: AppDatabase) {
        // Profiles (1)
        val profile1 = ProfileEntity(id = 1L, name = "Personal 👤", emoji = "👤", colorHex = 0xFF3B82F6, type = ProfileEntity.TYPE_PERSONAL, isDefault = true)
        val profile2 = ProfileEntity(id = 2L, name = "Work 💼", emoji = "💼", colorHex = 0xFF10B981, type = ProfileEntity.TYPE_WORK, isDefault = false)
        sourceDb.profileDao().insertProfiles(listOf(profile1, profile2))

        // Profile Surveys (2)
        val survey = ProfileSurveyEntity(
            profileId = 1L,
            displayName = "Kadhir 🚀",
            gender = "Male",
            sleepingTime = "23:00",
            dailyWaterGoalMl = 2500,
            discoverySource = "PlayStore",
            discoverySocialPlatform = "Twitter",
            discoveryOtherDetail = null,
            isCompleted = true,
            isSkipped = false
        )
        sourceDb.profileSurveyDao().insertSurveys(listOf(survey))

        // Habits (3)
        val habit = HabitEntity(
            id = 10L,
            profileId = 1L,
            title = "Morning Meditation 🧘",
            emoji = "🧘",
            description = "10 minutes mindfulness",
            reminderTime = "07:00",
            colorHex = 0xFF10B981
        )
        sourceDb.habitDao().insertHabit(habit)

        // Challenges (4)
        val challenge = ChallengeEntity(
            id = 1L,
            startDateIso = "2026-11-01",
            targetDays = 75,
            status = "ACTIVE",
            rulesJson = "[\"Workout 45m\",\"Read 10 pages\"]",
            dailyLogsJson = "{\"2026-11-01\":true}"
        )
        sourceDb.challengeDao().insertChallenges(listOf(challenge))

        // Water Logs (5)
        val water = WaterLogEntity(dateIso = "2026-11-01", amountMl = 1500, goalMl = 2500)
        sourceDb.waterLogDao().insertWaterLogs(listOf(water))

        // Projects (6)
        val project = ProjectEntity(
            id = 100L,
            profileId = 1L,
            name = "LifeOS Architecture 📱",
            description = "Offline-first productivity system",
            colorId = "blue",
            emoji = "📱"
        )
        sourceDb.projectDao().insertProjects(listOf(project))

        // Labels (7)
        val label = LabelEntity(
            id = 200L,
            profileId = 1L,
            name = "Urgent 🔥",
            colorId = "red"
        )
        sourceDb.labelDao().insertLabels(listOf(label))

        // Tasks (8)
        val task = TaskEntity(
            id = 300L,
            profileId = 1L,
            projectId = 100L,
            title = "Finalize Release 4 Phase P1 🎯",
            emoji = "🎯",
            description = "Complete 40-entity backup/restore v2",
            category = "Engineering",
            priority = "P1",
            dueDateIso = "2026-11-02",
            dueTime = "18:00",
            durationMinutes = 60,
            deadlineIso = "2026-11-03",
            recurrenceType = "DAILY",
            recurrenceInterval = 1,
            recurrenceWindowStartDay = 1,
            recurrenceWindowEndDay = 5
        )
        sourceDb.taskDao().insertTasks(listOf(task))

        // Task-Label Cross Ref (9)
        val crossRef = TaskLabelCrossRef(taskId = 300L, labelId = 200L)
        sourceDb.labelDao().insertOrReplaceTaskLabelCrossRefs(listOf(crossRef))

        // Task Reminders (10)
        val reminder = TaskReminderEntity(
            id = 400L,
            taskId = 300L,
            minutesBefore = 15,
            customTime = null,
            type = "BEFORE_DUE",
            referenceType = "DUE_TIME",
            isEnabled = true
        )
        sourceDb.taskReminderDao().insertReminders(listOf(reminder))
    }

    private suspend fun seedPlanningAndGamificationEntities(sourceDb: AppDatabase) {
        // Honeycomb Shortcuts (11)
        val shortcut = HoneycombShortcutEntity(
            id = 500L,
            profileId = 1L,
            type = "TASK",
            targetId = "300",
            title = "Shortcut to Task 300 ⚡",
            emoji = "⚡",
            coordQ = 1,
            coordR = 2
        )
        sourceDb.honeycombShortcutDao().insertShortcuts(listOf(shortcut))

        // Trash Items (12)
        val trash = TrashItemEntity(
            id = 600L,
            profileId = 1L,
            itemType = "TASK",
            originalId = 299L,
            title = "Deleted Old Task 🗑️",
            subtitle = "Archived yesterday",
            originalPayloadJson = "{\"title\":\"Old Task\"}",
            expiresAt = 1733000000000L
        )
        sourceDb.trashDao().insertTrashItems(listOf(trash))

        // Plans (13)
        val plan = PlanEntity(
            id = 700L,
            profileId = 1L,
            projectId = 100L,
            title = "Q4 Goals 🏆",
            description = "Quarterly strategic roadmap",
            level = "QUARTERLY",
            startDateIso = "2026-10-01",
            targetDateIso = "2026-12-31"
        )
        sourceDb.planDao().insertPlans(listOf(plan))

        // Plan Milestones (14)
        val milestone = PlanMilestoneEntity(
            id = 800L,
            planId = 700L,
            title = "Deliver Phase P1",
            isCompleted = true,
            orderIndex = 0
        )
        sourceDb.planDao().insertMilestones(listOf(milestone))

        // Plan Task Cross Refs (15)
        val planRef = PlanTaskCrossRef(planId = 700L, taskId = 300L)
        sourceDb.planDao().insertPlanTaskCrossRefs(listOf(planRef))

        // Gamification Profile (16)
        val gamProfile = GamificationProfileEntity(
            id = 1,
            currentXp = 1250L,
            currentCoins = 450,
            currentLevel = 5,
            hunterRank = "B"
        )
        sourceDb.gamificationDao().insertOrUpdateProfile(gamProfile)

        // XP Events (17)
        val xpEvent = XpEventEntity(
            eventKey = "TASK_COMPLETE:300:2026-11-01",
            xpAwarded = 50,
            coinsAwarded = 10,
            description = "Completed task 300"
        )
        sourceDb.gamificationDao().insertXpEvents(listOf(xpEvent))

        // Achievements (18)
        val achievement = AchievementEntity(
            code = "SPEED_DEMON",
            title = "Speed Demon ⚡",
            description = "Complete 5 tasks in 1 hour",
            tier = "GOLD",
            xpReward = 100,
            coinsReward = 25,
            isUnlocked = true,
            unlockedAt = 1730467200000L
        )
        sourceDb.gamificationDao().insertAchievements(listOf(achievement))

        // Rewards (19)
        val reward = RewardItemEntity(
            id = 900L,
            title = "Coffee Break ☕",
            description = "Enjoy an espresso",
            coinCost = 50,
            iconEmoji = "☕"
        )
        sourceDb.gamificationDao().insertRewards(listOf(reward))

        // Reward Redemptions (20)
        val redemption = RewardRedemptionEntity(
            id = 1000L,
            rewardId = 900L,
            rewardTitle = "Coffee Break ☕",
            coinsSpent = 50
        )
        sourceDb.gamificationDao().insertRedemptions(listOf(redemption))
    }

    private suspend fun seedHealthAndTrackerEntities(sourceDb: AppDatabase) {
        // Medications (21)
        val med = MedicationEntity(
            id = 1100L,
            profileId = 1L,
            taskId = 300L,
            name = "Vitamin D3 💊",
            form = "CAPSULE",
            dosageAmount = "1",
            dosageUnit = "CAPSULE",
            strength = "2000 IU",
            scheduleType = "DAILY",
            intervalHours = 24,
            startDateIso = "2026-11-01"
        )
        sourceDb.medicationDao().insertMedications(listOf(med))

        // Medication Doses (22)
        val dose = MedicationDoseEntity(
            id = 1200L,
            medicationId = 1100L,
            taskId = 300L,
            medicationName = "Vitamin D3 💊",
            medicationForm = "CAPSULE",
            dosage = "1",
            scheduledDateIso = "2026-11-01",
            scheduledTime = "08:00",
            status = "TAKEN"
        )
        sourceDb.medicationDao().insertDoses(listOf(dose))

        // Trackers (23)
        val tracker = TrackerEntity(
            id = 1300L,
            profileId = 1L,
            projectId = 100L,
            title = "A4 Printer Paper 📄",
            emoji = "📄",
            inventoryPolicy = "CONSUMABLE",
            currentStock = 500.0,
            emptyStock = 50.0
        )
        sourceDb.trackerDao().insertTrackers(listOf(tracker))

        // Tracker Cycles (24)
        val cycle = UsageCycleEntity(
            id = 1400L,
            trackerId = 1300L,
            cycleName = "November Ream",
            startDateIso = "2026-11-01",
            initialQuantity = 500.0,
            remainingQuantity = 450.0
        )
        sourceDb.trackerDao().insertCycles(listOf(cycle))

        // Tracker Usage Entries (25)
        val entry = UsageEntryEntity(
            id = 1500L,
            cycleId = 1400L,
            dateIso = "2026-11-01",
            quantityDeducted = 50.0,
            notes = "Printed Release 4 Roadmap"
        )
        sourceDb.trackerDao().insertUsageEntries(listOf(entry))

        // Tracker Purchases (26)
        val purchase = TrackerPurchaseEntity(
            id = 1600L,
            trackerId = 1300L,
            purchaseDateIso = "2026-10-25",
            quantity = 500.0,
            unitPrice = 0.5,
            totalCost = 250.0
        )
        sourceDb.trackerDao().insertPurchases(listOf(purchase))

        // Tracker Inventory Adjustments (27)
        val adjustment = InventoryAdjustmentEntity(
            id = 1700L,
            trackerId = 1300L,
            dateIso = "2026-11-01",
            deltaQuantity = -50.0,
            reason = "Printing sprint"
        )
        sourceDb.trackerDao().insertAdjustments(listOf(adjustment))
    }

    private suspend fun seedSubscriptionAndWorkspaceEntities(sourceDb: AppDatabase) {
        // Subscriptions (28)
        val subscription = SubscriptionEntity(
            id = 1800L,
            profileId = 1L,
            projectId = 100L,
            title = "Cloud Domain 🌐",
            amount = 12.99,
            currency = "$",
            frequency = "YEARLY",
            anchorDateIso = "2026-01-15",
            nextRenewalDateIso = "2027-01-15",
            status = "ACTIVE"
        )
        sourceDb.subscriptionDao().insertSubscriptions(listOf(subscription))

        // Subscription Bills (29)
        val bill = BillingOccurrenceEntity(
            id = 1900L,
            subscriptionId = 1800L,
            dueDateIso = "2026-01-15",
            expectedAmount = 12.99,
            status = "PAID"
        )
        sourceDb.subscriptionDao().insertBills(listOf(bill))

        // Subscription Payments (30)
        val payment = SubscriptionPaymentEntity(
            id = 2000L,
            billId = 1900L,
            subscriptionId = 1800L,
            amountPaid = 12.99,
            paymentDateIso = "2026-01-15",
            paymentMethod = "Credit Card",
            transactionRef = "TXN_998877"
        )
        sourceDb.subscriptionDao().insertPayments(listOf(payment))

        // Subscription Price History (31)
        val priceHistory = PriceHistoryEntity(
            id = 2100L,
            subscriptionId = 1800L,
            oldAmount = 9.99,
            newAmount = 12.99,
            effectiveDateIso = "2026-01-15",
            reason = "Price increase"
        )
        sourceDb.subscriptionDao().insertPriceHistories(listOf(priceHistory))

        // Project Workspace Items (32)
        val wsItem = ProjectWorkspaceItemEntity(
            id = 2200L,
            profileId = 1L,
            projectId = 100L,
            title = "Workspace Kanban Card 📌",
            status = "IN_PROGRESS",
            progress = 50,
            priority = "HIGH"
        )
        sourceDb.projectWorkspaceDao().insertItems(listOf(wsItem))

        // Project Workspace Views (33)
        val wsView = ProjectWorkspaceViewEntity(
            id = 2300L,
            profileId = 1L,
            projectId = 100L,
            name = "Sprint Board 📋",
            viewType = "KANBAN",
            isDefault = true
        )
        sourceDb.projectWorkspaceDao().insertViews(listOf(wsView))

        // Project Custom Statuses (34)
        val customStatus = ProjectCustomStatusEntity(
            id = 2400L,
            profileId = 1L,
            projectId = 100L,
            name = "In Code Review 🔍",
            colorHex = 4282168054L,
            orderIndex = 2
        )
        sourceDb.projectWorkspaceDao().insertCustomStatuses(listOf(customStatus))

        // Project Custom Properties (35)
        val customProperty = ProjectCustomPropertyEntity(
            id = 2500L,
            profileId = 1L,
            projectId = 100L,
            name = "Story Points 🔢",
            propertyType = "NUMBER",
            optionsJson = "[]"
        )
        sourceDb.projectWorkspaceDao().insertCustomProperties(listOf(customProperty))
    }

    private suspend fun seedGlossaryAndMcpEntities(sourceDb: AppDatabase) {
        // Glossary Packs (36)
        val pack = GlossaryPackEntity(
            id = "custom_pack_1",
            title = "Agile Glossary 📚",
            description = "Agile engineering terms",
            category = "Engineering",
            version = 1,
            author = "Engineering Team",
            itemCount = 1
        )
        sourceDb.glossaryDao().insertPacks(listOf(pack))

        // Glossary Items (37)
        val glossaryItem = GlossaryItemEntity(
            id = 2600L,
            packId = "custom_pack_1",
            term = "CI/CD",
            category = "DevOps",
            definition = "Continuous Integration & Deployment",
            detailedExplanation = "Automated test and build pipelines"
        )
        sourceDb.glossaryDao().insertItems(listOf(glossaryItem))

        // Glossary User Data (38)
        val userData = GlossaryUserDataEntity(
            id = 2700L,
            profileId = 1L,
            termId = 2600L,
            isBookmarked = true,
            personalNote = "Important for Release 4"
        )
        sourceDb.glossaryDao().insertUserDataList(listOf(userData))

        // MCP Servers (39)
        val mcpServer = McpServerEntity(
            id = 2800L,
            profileId = 1L,
            name = "Local Analysis MCP 🤖",
            endpoint = "http://127.0.0.1:8080/mcp",
            transport = "HTTP_SSE"
        )
        sourceDb.mcpDao().insertServers(listOf(mcpServer))

        // MCP Security Audit Logs (40)
        val auditLog = McpAuditLogEntity(
            id = 2900L,
            profileId = 1L,
            timestamp = 1730467200000L,
            direction = "OUTBOUND",
            eventType = "TOOL_CALL",
            toolOrResource = "query_logs",
            details = "Params: {}",
            status = "SUCCESS"
        )
        sourceDb.mcpDao().insertAuditLogs(listOf(auditLog))
    }

    private suspend fun verifyCoreEntities(targetDb: AppDatabase) {
        // Profiles
        val restoredProfiles = targetDb.profileDao().getAllProfiles()
        assertEquals(2, restoredProfiles.size)
        assertEquals("Personal 👤", restoredProfiles[0].name)
        assertEquals("Work 💼", restoredProfiles[1].name)

        // Profile Survey
        val restoredSurvey = targetDb.profileSurveyDao().getSurveyForProfile(1L)
        assertNotNull(restoredSurvey)
        assertEquals("Kadhir 🚀", restoredSurvey!!.displayName)
        assertEquals("Male", restoredSurvey.gender)

        // Habits
        val restoredHabit = targetDb.habitDao().getHabitById(10L)
        assertNotNull(restoredHabit)
        assertEquals("Morning Meditation 🧘", restoredHabit!!.title)
        assertEquals("07:00", restoredHabit.reminderTime)

        // Challenges
        val restoredChallenges = targetDb.challengeDao().getAllChallenges()
        assertEquals(1, restoredChallenges.size)
        assertEquals(75, restoredChallenges[0].targetDays)

        // Water Logs
        val restoredWater = targetDb.waterLogDao().getWaterLog("2026-11-01")
        assertNotNull(restoredWater)
        assertEquals(1500, restoredWater!!.amountMl)

        // Projects
        val restoredProject = targetDb.projectDao().getProjectById(100L)
        assertNotNull(restoredProject)
        assertEquals("LifeOS Architecture 📱", restoredProject!!.name)

        // Labels
        val restoredLabel = targetDb.labelDao().getLabelById(200L)
        assertNotNull(restoredLabel)
        assertEquals("Urgent 🔥", restoredLabel!!.name)

        // Tasks
        val restoredTask = targetDb.taskDao().getTaskById(300L)
        assertNotNull(restoredTask)
        assertEquals("Finalize Release 4 Phase P1 🎯", restoredTask!!.title)
        assertEquals("DAILY", restoredTask.recurrenceType)
        assertEquals(1, restoredTask.recurrenceWindowStartDay)
        assertEquals(5, restoredTask.recurrenceWindowEndDay)

        // Task Label Cross Ref
        val restoredCrossRefs = targetDb.labelDao().getAllTaskLabelCrossRefsList()
        assertEquals(1, restoredCrossRefs.size)
        assertEquals(300L, restoredCrossRefs[0].taskId)
        assertEquals(200L, restoredCrossRefs[0].labelId)

        // Task Reminders
        val restoredReminders = targetDb.taskReminderDao().getAllRemindersList()
        assertEquals(1, restoredReminders.size)
        assertEquals(15, restoredReminders[0].minutesBefore)
    }

    private suspend fun verifyPlanningAndGamificationEntities(targetDb: AppDatabase) {
        // Honeycomb Shortcuts
        val restoredShortcuts = targetDb.honeycombShortcutDao().getAllShortcutsList()
        assertEquals(1, restoredShortcuts.size)
        assertEquals("Shortcut to Task 300 ⚡", restoredShortcuts[0].title)

        // Trash Items
        val restoredTrash = targetDb.trashDao().getAllTrashItemsList()
        assertEquals(1, restoredTrash.size)
        assertEquals("Deleted Old Task 🗑️", restoredTrash[0].title)

        // Plans
        val restoredPlan = targetDb.planDao().getPlanById(700L)
        assertNotNull(restoredPlan)
        assertEquals("Q4 Goals 🏆", restoredPlan!!.title)

        // Plan Milestones
        val restoredMilestones = targetDb.planDao().getAllMilestonesList()
        assertEquals(1, restoredMilestones.size)
        assertEquals("Deliver Phase P1", restoredMilestones[0].title)

        // Plan Task Links
        val restoredPlanRefs = targetDb.planDao().getAllPlanTaskCrossRefs()
        assertEquals(1, restoredPlanRefs.size)
        assertEquals(700L, restoredPlanRefs[0].planId)

        // Gamification
        val restoredGam = targetDb.gamificationDao().getProfile()
        assertNotNull(restoredGam)
        assertEquals(1250L, restoredGam!!.currentXp)
        assertEquals(5, restoredGam.currentLevel)

        // XP Events
        val restoredXpEvents = targetDb.gamificationDao().getAllXpEventsList()
        assertEquals(1, restoredXpEvents.size)
        assertEquals("TASK_COMPLETE:300:2026-11-01", restoredXpEvents[0].eventKey)

        // Achievements
        val restoredAchievements = targetDb.gamificationDao().getAllAchievementsList()
        assertEquals(1, restoredAchievements.size)
        assertEquals("SPEED_DEMON", restoredAchievements[0].code)

        // Rewards
        val restoredRewards = targetDb.gamificationDao().getAllRewardsList()
        assertEquals(1, restoredRewards.size)
        assertEquals("Coffee Break ☕", restoredRewards[0].title)

        // Reward Redemptions
        val restoredRedemptions = targetDb.gamificationDao().getAllRewardRedemptionsList()
        assertEquals(1, restoredRedemptions.size)
        assertEquals(50, restoredRedemptions[0].coinsSpent)
    }

    private suspend fun verifyHealthAndTrackerEntities(targetDb: AppDatabase) {
        // Medications
        val restoredMed = targetDb.medicationDao().getMedicationById(1100L)
        assertNotNull(restoredMed)
        assertEquals("Vitamin D3 💊", restoredMed!!.name)
        assertEquals("2000 IU", restoredMed.strength)

        // Medication Doses
        val restoredDoses = targetDb.medicationDao().getAllDosesList()
        assertEquals(1, restoredDoses.size)
        assertEquals("TAKEN", restoredDoses[0].status)

        // Trackers
        val restoredTracker = targetDb.trackerDao().getTrackerById(1300L)
        assertNotNull(restoredTracker)
        assertEquals("A4 Printer Paper 📄", restoredTracker!!.title)
        assertEquals(500.0, restoredTracker.currentStock, 0.01)

        // Tracker Cycles
        val restoredCycles = targetDb.trackerDao().getAllCyclesList()
        assertEquals(1, restoredCycles.size)
        assertEquals("November Ream", restoredCycles[0].cycleName)

        // Tracker Usage Entries
        val restoredEntries = targetDb.trackerDao().getAllEntriesList()
        assertEquals(1, restoredEntries.size)
        assertEquals(50.0, restoredEntries[0].quantityDeducted!!, 0.01)

        // Tracker Purchases
        val restoredPurchases = targetDb.trackerDao().getAllPurchasesList()
        assertEquals(1, restoredPurchases.size)
        assertEquals(250.0, restoredPurchases[0].totalCost, 0.01)

        // Tracker Inventory Adjustments
        val restoredAdjustments = targetDb.trackerDao().getAllAdjustmentsList()
        assertEquals(1, restoredAdjustments.size)
        assertEquals(-50.0, restoredAdjustments[0].deltaQuantity, 0.01)
    }

    private suspend fun verifySubscriptionAndWorkspaceEntities(targetDb: AppDatabase) {
        // Subscriptions
        val restoredSub = targetDb.subscriptionDao().getSubscriptionById(1800L)
        assertNotNull(restoredSub)
        assertEquals("Cloud Domain 🌐", restoredSub!!.title)
        assertEquals(12.99, restoredSub.amount, 0.01)

        // Subscription Bills
        val restoredBills = targetDb.subscriptionDao().getAllBillsList()
        assertEquals(1, restoredBills.size)
        assertEquals("PAID", restoredBills[0].status)

        // Subscription Payments
        val restoredPayments = targetDb.subscriptionDao().getAllPaymentsList()
        assertEquals(1, restoredPayments.size)
        assertEquals("TXN_998877", restoredPayments[0].transactionRef)

        // Subscription Price History
        val restoredPh = targetDb.subscriptionDao().getAllPriceHistoryList()
        assertEquals(1, restoredPh.size)
        assertEquals(9.99, restoredPh[0].oldAmount, 0.01)

        // Project Workspace Items
        val restoredWsItems = targetDb.projectWorkspaceDao().getAllItems()
        assertEquals(1, restoredWsItems.size)
        assertEquals("Workspace Kanban Card 📌", restoredWsItems[0].title)

        // Project Workspace Views
        val restoredWsViews = targetDb.projectWorkspaceDao().getAllViews()
        assertEquals(1, restoredWsViews.size)
        assertEquals("Sprint Board 📋", restoredWsViews[0].name)

        // Project Custom Statuses
        val restoredCustomStatuses = targetDb.projectWorkspaceDao().getAllCustomStatuses()
        assertEquals(1, restoredCustomStatuses.size)
        assertEquals("In Code Review 🔍", restoredCustomStatuses[0].name)

        // Project Custom Properties
        val restoredCustomProperties = targetDb.projectWorkspaceDao().getAllCustomProperties()
        assertEquals(1, restoredCustomProperties.size)
        assertEquals("Story Points 🔢", restoredCustomProperties[0].name)
    }

    private suspend fun verifyGlossaryAndMcpEntities(targetDb: AppDatabase) {
        // Glossary Packs
        val restoredPacks = targetDb.glossaryDao().getAllPacksList()
        assertEquals(1, restoredPacks.size)
        assertEquals("Agile Glossary 📚", restoredPacks[0].title)

        // Glossary Items
        val restoredGlossaryItems = targetDb.glossaryDao().getAllItemsList()
        assertEquals(1, restoredGlossaryItems.size)
        assertEquals("CI/CD", restoredGlossaryItems[0].term)

        // Glossary User Data
        val restoredUserData = targetDb.glossaryDao().getAllUserDataList()
        assertEquals(1, restoredUserData.size)
        assertEquals(true, restoredUserData[0].isBookmarked)

        // MCP Servers
        val restoredServers = targetDb.mcpDao().getAllServers()
        assertEquals(1, restoredServers.size)
        assertEquals("Local Analysis MCP 🤖", restoredServers[0].name)

        // MCP Security Audit Logs
        val restoredAuditLogs = targetDb.mcpDao().getAllAuditLogs()
        assertEquals(1, restoredAuditLogs.size)
        assertEquals("TOOL_CALL", restoredAuditLogs[0].eventType)
    }

    @Test
    fun testChecksumTamperingDetectedAndRejected() = runBlocking {
        // Seed simple database and export
        sourceDb.profileDao().insertProfile(ProfileEntity(id = 1L, name = "Personal", emoji = "👤", colorHex = 0xFF3B82F6, type = ProfileEntity.TYPE_PERSONAL, isDefault = true))
        sourceDb.taskDao().insertTask(TaskEntity(id = 10L, profileId = 1L, title = "Original Task", category = "Personal", dueDateIso = "2026-11-01"))

        val exportedJson = DataExportImportManager.exportAllData(sourceDb)
        assertTrue(DataExportImportManager.validateBackup(exportedJson) is DataExportImportManager.ValidationResult.Valid)

        // Tamper with the task title in the JSON payload
        val tamperedJson = exportedJson.replace("Original Task", "Malicious Injected Task")

        val validation = DataExportImportManager.validateBackup(tamperedJson)
        assertTrue("Validation must fail when payload is tampered", validation is DataExportImportManager.ValidationResult.Invalid)
        val errorMessage = (validation as DataExportImportManager.ValidationResult.Invalid).error
        assertTrue(errorMessage.contains("Checksum mismatch") || errorMessage.contains("corrupt"))

        // Attempting to restore tampered backup must return RestoreResult.Error and leave DB empty
        val result = DataExportImportManager.restoreData(targetDb, tamperedJson, context)
        assertTrue("Restore must fail on tampered backup", result is DataExportImportManager.RestoreResult.Error)
        assertEquals(0, targetDb.taskDao().getAllTasksList().size)
    }

    @Test
    fun testUnsupportedFutureVersionRejected() = runBlocking {
        val root = JSONObject().apply {
            put("version", 99)
            put("formatVersion", 99)
            put("appVersion", "99.0.0")
            put("tasks", org.json.JSONArray())
        }
        val futureJson = root.toString()

        val validation = DataExportImportManager.validateBackup(futureJson)
        assertTrue("Validation must reject future version", validation is DataExportImportManager.ValidationResult.Invalid)
        val error = (validation as DataExportImportManager.ValidationResult.Invalid).error
        assertTrue(error.contains("Unsupported backup version: 99"))

        val result = DataExportImportManager.restoreData(targetDb, futureJson, context)
        assertTrue("Restore must reject future version", result is DataExportImportManager.RestoreResult.Error)
    }

    @Test
    fun testBackwardCompatibilityWithLegacyV1Backup() = runBlocking {
        // Reconstructed legacy v1 backup (no metadata, no checksum, missing profileId)
        val legacyJson = """
            {
                "version": 1,
                "exportedAt": 1700000000000,
                "tasks": [
                    {
                        "id": 1,
                        "title": "Legacy V1 Task",
                        "category": "Work",
                        "priority": "P2",
                        "isCompleted": false
                    }
                ],
                "habits": [
                    {
                        "id": 1,
                        "title": "Legacy V1 Habit",
                        "emoji": "🏃"
                    }
                ],
                "water_logs": [
                    {
                        "dateIso": "2024-01-01",
                        "amountMl": 1000,
                        "goalMl": 2000
                    }
                ]
            }
        """.trimIndent()

        val validation = DataExportImportManager.validateBackup(legacyJson)
        assertTrue("Legacy v1 backup must validate successfully", validation is DataExportImportManager.ValidationResult.Valid)
        val summary = (validation as DataExportImportManager.ValidationResult.Valid).summary
        assertTrue("Must be marked as legacy", summary.isLegacy)
        assertEquals(1, summary.formatVersion)

        val result = DataExportImportManager.restoreData(targetDb, legacyJson, context)
        assertTrue("Legacy restore must succeed", result is DataExportImportManager.RestoreResult.Success)

        // Verify default profile was created and legacy tasks/habits assigned to profileId = 1
        val profiles = targetDb.profileDao().getAllProfiles()
        assertEquals(1, profiles.size)
        assertEquals("Personal", profiles[0].name)

        val tasks = targetDb.taskDao().getAllTasksList()
        assertEquals(1, tasks.size)
        assertEquals("Legacy V1 Task", tasks[0].title)
        assertEquals(1L, tasks[0].profileId)

        val habits = targetDb.habitDao().getAllHabitsList()
        assertEquals(1, habits.size)
        assertEquals("Legacy V1 Habit", habits[0].title)
        assertEquals(1L, habits[0].profileId)
    }

    @Test
    fun testReplaceAllSemanticsAndOrphanRemoval() = runBlocking {
        // 1. Target DB already contains existing records
        targetDb.profileDao().insertProfile(ProfileEntity(id = 1L, name = "Old Profile", emoji = "👤", colorHex = 0xFF000000, type = ProfileEntity.TYPE_PERSONAL, isDefault = true))
        targetDb.taskDao().insertTask(TaskEntity(id = 999L, profileId = 1L, title = "Old Obsolete Task", category = "Work", dueDateIso = "2020-01-01"))
        targetDb.habitDao().insertHabit(HabitEntity(id = 888L, profileId = 1L, title = "Old Obsolete Habit"))
        targetDb.waterLogDao().insertWaterLogs(listOf(WaterLogEntity(dateIso = "2020-01-01", amountMl = 100)))

        assertEquals(1, targetDb.taskDao().getAllTasksList().size)
        assertEquals(1, targetDb.habitDao().getAllHabitsList().size)

        // 2. Source DB has completely different records
        sourceDb.profileDao().insertProfile(ProfileEntity(id = 1L, name = "New Profile", emoji = "🌟", colorHex = 0xFF3B82F6, type = ProfileEntity.TYPE_PERSONAL, isDefault = true))
        sourceDb.taskDao().insertTask(TaskEntity(id = 10L, profileId = 1L, title = "Fresh New Task", category = "Personal", dueDateIso = "2026-11-01"))

        val newBackup = DataExportImportManager.exportAllData(sourceDb)

        // 3. Restore with REPLACE_ALL
        val result = DataExportImportManager.restoreData(targetDb, newBackup, context)
        assertTrue(result is DataExportImportManager.RestoreResult.Success)

        // 4. Assert obsolete records were completely wiped and replaced
        val tasks = targetDb.taskDao().getAllTasksList()
        assertEquals(1, tasks.size)
        assertEquals("Fresh New Task", tasks[0].title)
        assertFalse("Old task 999 must NOT exist after REPLACE_ALL", tasks.any { it.id == 999L })

        val habits = targetDb.habitDao().getAllHabitsList()
        assertEquals(0, habits.size) // Source had no habits, so target habits must be 0

        val waterLogs = targetDb.waterLogDao().getAllWaterLogsList()
        assertEquals(0, waterLogs.size) // Source had no water logs
    }

    @Test
    fun testPreRestoreRecoveryBackupCreated() = runBlocking {
        // Seed target DB with existing data before restore
        targetDb.profileDao().insertProfile(ProfileEntity(id = 1L, name = "Current Data", emoji = "👤", colorHex = 0xFF3B82F6, type = ProfileEntity.TYPE_PERSONAL, isDefault = true))
        targetDb.taskDao().insertTask(TaskEntity(id = 55L, profileId = 1L, title = "Crucial User Data Before Restore", category = "Important", dueDateIso = "2026-11-01"))

        // Source has new data
        sourceDb.profileDao().insertProfile(ProfileEntity(id = 1L, name = "Imported Profile", emoji = "📁", colorHex = 0xFF10B981, type = ProfileEntity.TYPE_PERSONAL, isDefault = true))
        sourceDb.taskDao().insertTask(TaskEntity(id = 77L, profileId = 1L, title = "Imported Task", category = "Personal", dueDateIso = "2026-11-01"))

        val backupJson = DataExportImportManager.exportAllData(sourceDb)

        val result = DataExportImportManager.restoreData(targetDb, backupJson, context)
        assertTrue(result is DataExportImportManager.RestoreResult.Success)

        // Verify recovery backup file was written to app storage
        val recoveryFile = File(context.filesDir, "recovery_backups/pre_restore_recovery.json")
        assertTrue("Recovery backup file must exist", recoveryFile.exists())
        val recoveryContent = recoveryFile.readText()
        assertTrue("Recovery file must contain data from before the restore", recoveryContent.contains("Crucial User Data Before Restore"))
    }

    @Test
    fun testTransactionalRollbackOnCorruptedJsonMidway() = runBlocking {
        // Target DB has existing data
        targetDb.profileDao().insertProfile(ProfileEntity(id = 1L, name = "Safe Profile", emoji = "👤", colorHex = 0xFF3B82F6, type = ProfileEntity.TYPE_PERSONAL, isDefault = true))
        targetDb.taskDao().insertTask(TaskEntity(id = 100L, profileId = 1L, title = "Protected Task", category = "Personal", dueDateIso = "2026-11-01"))

        // Invalidate JSON structure midway
        val corruptedJson = "{ \"version\": 2, \"tasks\": [ { \"id\": 1, \"title\": " // truncated syntax error

        val result = DataExportImportManager.restoreData(targetDb, corruptedJson, context)
        assertTrue("Restore must fail on corrupted JSON", result is DataExportImportManager.RestoreResult.Error)

        // Verify target DB data is completely untouched
        val tasks = targetDb.taskDao().getAllTasksList()
        assertEquals(1, tasks.size)
        assertEquals("Protected Task", tasks[0].title)
    }

    @Test
    fun testStreamExportAndImportRoundTrip() = runBlocking {
        sourceDb.profileDao().insertProfile(ProfileEntity(id = 1L, name = "Stream Profile", emoji = "🌊", colorHex = 0xFF3B82F6, type = ProfileEntity.TYPE_PERSONAL, isDefault = true))
        sourceDb.taskDao().insertTask(TaskEntity(id = 101L, profileId = 1L, title = "Stream Task 🌊", category = "Personal", dueDateIso = "2026-11-01"))

        // Export to OutputStream (simulating SAF)
        val outputStream = ByteArrayOutputStream()
        DataExportImportManager.exportToStream(sourceDb, outputStream)
        val bytes = outputStream.toByteArray()
        assertTrue("Exported bytes must be non-empty", bytes.isNotEmpty())

        // Import from InputStream (simulating SAF)
        val inputStream = ByteArrayInputStream(bytes)
        val result = DataExportImportManager.importFromStream(targetDb, inputStream, context)
        assertTrue(result is DataExportImportManager.RestoreResult.Success)

        val tasks = targetDb.taskDao().getAllTasksList()
        assertEquals(1, tasks.size)
        assertEquals("Stream Task 🌊", tasks[0].title)
    }
}
