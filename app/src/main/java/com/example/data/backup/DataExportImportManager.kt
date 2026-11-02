package com.example.data.backup

import android.content.Context
import androidx.room.withTransaction
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * LifeOS Backup & Restore System (Format v2).
 *
 * Provides complete, verifiable, transactional export and REPLACE_ALL restore
 * covering all 40 Room database entities across 18 schema versions.
 */
object DataExportImportManager {

    const val BACKUP_FORMAT_VERSION = 2
    const val APP_VERSION = "4.0.0"
    const val DATABASE_VERSION = 19
    const val MAX_BACKUP_SIZE_BYTES = 50_000_000

    /**
     * Canonical list of all 40 entity dataset keys in alphabetical order for deterministic checksumming.
     */
    val ALL_40_ENTITY_KEYS: List<String> = listOf(
        "achievements",
        "challenges",
        "gamification_profile",
        "glossary_items",
        "glossary_packs",
        "glossary_user_data",
        "habits",
        "honeycomb_shortcuts",
        "labels",
        "mcp_audit_logs",
        "mcp_servers",
        "medication_doses",
        "medications",
        "plan_milestones",
        "plan_task_cross_refs",
        "plans",
        "profile_surveys",
        "profiles",
        "project_custom_properties",
        "project_custom_statuses",
        "project_workspace_items",
        "project_workspace_views",
        "projects",
        "reward_redemptions",
        "rewards",
        "subscription_bills",
        "subscription_payments",
        "subscription_price_history",
        "subscriptions",
        "task_label_cross_ref",
        "task_reminders",
        "tasks",
        "tracker_cycles",
        "tracker_inventory_adjustments",
        "tracker_purchases",
        "tracker_usage_entries",
        "trackers",
        "trash_items",
        "water_logs",
        "xp_events"
    )

    data class BackupSummary(
        val formatVersion: Int,
        val appVersion: String,
        val databaseVersion: Int,
        val exportedAt: Long,
        val totalRecords: Int,
        val entityCounts: Map<String, Int>,
        val checksumSha256: String?,
        val isChecksumValid: Boolean?,
        val isLegacy: Boolean
    )

    sealed class ValidationResult {
        data class Valid(val summary: BackupSummary) : ValidationResult()
        data class Invalid(val error: String) : ValidationResult()
    }

    sealed class RestoreResult {
        data class Success(val restoredRecords: Int, val summary: BackupSummary) : RestoreResult()
        data class Error(val message: String, val cause: Throwable? = null) : RestoreResult()
    }

    // =========================================================================
    // EXPORT
    // =========================================================================

    suspend fun exportAllData(database: AppDatabase): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        val timestamp = System.currentTimeMillis()
        val readableDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))

        val entityCounts = mutableMapOf<String, Int>()

        // 1. Profiles
        val profiles = database.profileDao().getAllProfiles()
        entityCounts["profiles"] = profiles.size
        val profsArr = JSONArray()
        profiles.forEach { p ->
            profsArr.put(JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("emoji", p.emoji)
                put("colorHex", p.colorHex)
                put("type", p.type)
                put("orderIndex", p.orderIndex)
                put("isDefault", p.isDefault)
                put("createdAt", p.createdAt)
                put("updatedAt", p.updatedAt)
            })
        }
        root.put("profiles", profsArr)

        // 2. Profile Surveys
        val surveys = database.profileSurveyDao().getAllSurveys()
        entityCounts["profile_surveys"] = surveys.size
        val surveysArr = JSONArray()
        surveys.forEach { s ->
            surveysArr.put(JSONObject().apply {
                put("profileId", s.profileId)
                put("displayName", s.displayName ?: JSONObject.NULL)
                put("gender", s.gender ?: JSONObject.NULL)
                put("sleepingTime", s.sleepingTime ?: JSONObject.NULL)
                put("dailyWaterGoalMl", s.dailyWaterGoalMl ?: JSONObject.NULL)
                put("discoverySource", s.discoverySource ?: JSONObject.NULL)
                put("discoverySocialPlatform", s.discoverySocialPlatform ?: JSONObject.NULL)
                put("discoveryOtherDetail", s.discoveryOtherDetail ?: JSONObject.NULL)
                put("isCompleted", s.isCompleted)
                put("isSkipped", s.isSkipped)
                put("completedAt", s.completedAt)
                put("updatedAt", s.updatedAt)
            })
        }
        root.put("profile_surveys", surveysArr)

        // 3. Tasks
        val tasks = database.taskDao().getAllTasksList()
        entityCounts["tasks"] = tasks.size
        val tasksArr = JSONArray()
        tasks.forEach { t ->
            tasksArr.put(JSONObject().apply {
                put("id", t.id)
                put("title", t.title)
                put("emoji", t.emoji)
                put("description", t.description)
                put("category", t.category)
                put("priority", t.priority)
                put("dueDateIso", t.dueDateIso)
                put("dueTime", t.dueTime ?: JSONObject.NULL)
                put("isAllDay", t.isAllDay)
                put("schedulingMode", t.schedulingMode)
                put("durationAmount", t.durationAmount)
                put("durationUnit", t.durationUnit)
                put("durationMinutes", t.durationMinutes)
                put("deadlineIso", t.deadlineIso ?: JSONObject.NULL)
                put("deadlineTime", t.deadlineTime ?: JSONObject.NULL)
                put("recurrenceType", t.recurrenceType)
                put("recurrenceInterval", t.recurrenceInterval)
                put("recurrenceUnit", t.recurrenceUnit)
                put("recurrenceEndCondition", t.recurrenceEndCondition)
                put("recurrenceEndDateIso", t.recurrenceEndDateIso ?: JSONObject.NULL)
                put("recurrenceCount", t.recurrenceCount)
                put("recurrenceWindowStartDay", t.recurrenceWindowStartDay ?: JSONObject.NULL)
                put("recurrenceWindowEndDay", t.recurrenceWindowEndDay ?: JSONObject.NULL)
                put("isOneTimeCompleted", t.isOneTimeCompleted)
                put("completedDatesJson", t.completedDatesJson)
                put("skippedDatesJson", t.skippedDatesJson)
                put("rescheduledDatesJson", t.rescheduledDatesJson)
                put("subtasksJson", t.subtasksJson)
                put("notes", t.notes)
                put("isReminderEnabled", t.isReminderEnabled)
                put("reminderMinutesBefore", t.reminderMinutesBefore)
                put("isArchived", t.isArchived)
                put("isPaused", t.isPaused)
                put("isQuickReminder", t.isQuickReminder)
                put("projectId", t.projectId ?: JSONObject.NULL)
                put("profileId", t.profileId)
                put("createdAt", t.createdAt)
            })
        }
        root.put("tasks", tasksArr)

        // 4. Habits
        val habits = database.habitDao().getAllHabitsList()
        entityCounts["habits"] = habits.size
        val habitsArr = JSONArray()
        habits.forEach { h ->
            habitsArr.put(JSONObject().apply {
                put("id", h.id)
                put("title", h.title)
                put("emoji", h.emoji)
                put("description", h.description)
                put("reminderTime", h.reminderTime ?: JSONObject.NULL)
                put("colorHex", h.colorHex)
                put("completedDatesJson", h.completedDatesJson)
                put("orderIndex", h.orderIndex)
                put("profileId", h.profileId)
                put("createdAt", h.createdAt)
            })
        }
        root.put("habits", habitsArr)

        // 5. Challenges
        val challenges = database.challengeDao().getAllChallenges()
        entityCounts["challenges"] = challenges.size
        val challengesArr = JSONArray()
        challenges.forEach { ch ->
            challengesArr.put(JSONObject().apply {
                put("id", ch.id)
                put("startDateIso", ch.startDateIso)
                put("targetDays", ch.targetDays)
                put("status", ch.status)
                put("rulesJson", ch.rulesJson)
                put("dailyLogsJson", ch.dailyLogsJson)
                put("createdAt", ch.createdAt)
            })
        }
        root.put("challenges", challengesArr)

        // 6. Water Logs
        val waterLogs = database.waterLogDao().getAllWaterLogsList()
        entityCounts["water_logs"] = waterLogs.size
        val waterArr = JSONArray()
        waterLogs.forEach { w ->
            waterArr.put(JSONObject().apply {
                put("dateIso", w.dateIso)
                put("amountMl", w.amountMl)
                put("goalMl", w.goalMl)
                put("updatedAt", w.updatedAt)
            })
        }
        root.put("water_logs", waterArr)

        // 7. Honeycomb Shortcuts
        val shortcuts = database.honeycombShortcutDao().getAllShortcutsList()
        entityCounts["honeycomb_shortcuts"] = shortcuts.size
        val scArr = JSONArray()
        shortcuts.forEach { sc ->
            scArr.put(JSONObject().apply {
                put("id", sc.id)
                put("type", sc.type)
                put("targetId", sc.targetId)
                put("title", sc.title)
                put("customTileName", sc.customTileName ?: JSONObject.NULL)
                put("emoji", sc.emoji)
                put("customColorHex", sc.customColorHex ?: JSONObject.NULL)
                put("colorSource", sc.colorSource)
                put("shape", sc.shape)
                put("orderIndex", sc.orderIndex)
                put("coordQ", sc.coordQ)
                put("coordR", sc.coordR)
                put("isPinned", sc.isPinned)
                put("profileId", sc.profileId)
                put("createdAt", sc.createdAt)
            })
        }
        root.put("honeycomb_shortcuts", scArr)

        // 8. Projects
        val projects = database.projectDao().getAllProjectsList()
        entityCounts["projects"] = projects.size
        val projArr = JSONArray()
        projects.forEach { p ->
            projArr.put(JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("description", p.description)
                put("colorId", p.colorId)
                put("emoji", p.emoji)
                put("isFavorite", p.isFavorite)
                put("isArchived", p.isArchived)
                put("orderIndex", p.orderIndex)
                put("profileId", p.profileId)
                put("createdAt", p.createdAt)
            })
        }
        root.put("projects", projArr)

        // 9. Labels
        val labels = database.labelDao().getAllLabelsList()
        entityCounts["labels"] = labels.size
        val labelsArr = JSONArray()
        labels.forEach { l ->
            labelsArr.put(JSONObject().apply {
                put("id", l.id)
                put("name", l.name)
                put("colorId", l.colorId)
                put("isFavorite", l.isFavorite)
                put("isArchived", l.isArchived)
                put("orderIndex", l.orderIndex)
                put("profileId", l.profileId)
                put("createdAt", l.createdAt)
            })
        }
        root.put("labels", labelsArr)

        // 10. Task Label Cross Refs
        val taskLabelRefs = database.labelDao().getAllTaskLabelCrossRefsList()
        entityCounts["task_label_cross_ref"] = taskLabelRefs.size
        val xRefArr = JSONArray()
        taskLabelRefs.forEach { x ->
            xRefArr.put(JSONObject().apply {
                put("taskId", x.taskId)
                put("labelId", x.labelId)
            })
        }
        root.put("task_label_cross_ref", xRefArr)

        // 11. Task Reminders
        val taskReminders = database.taskReminderDao().getAllRemindersList()
        entityCounts["task_reminders"] = taskReminders.size
        val remArr = JSONArray()
        taskReminders.forEach { r ->
            remArr.put(JSONObject().apply {
                put("id", r.id)
                put("taskId", r.taskId)
                put("minutesBefore", r.minutesBefore)
                put("customTime", r.customTime ?: JSONObject.NULL)
                put("type", r.type)
                put("referenceType", r.referenceType)
                put("isEnabled", r.isEnabled)
                put("createdAt", r.createdAt)
            })
        }
        root.put("task_reminders", remArr)

        // 12. Trash Items
        val trashItems = database.trashDao().getAllTrashItemsList()
        entityCounts["trash_items"] = trashItems.size
        val trashArr = JSONArray()
        trashItems.forEach { ti ->
            trashArr.put(JSONObject().apply {
                put("id", ti.id)
                put("profileId", ti.profileId)
                put("itemType", ti.itemType)
                put("originalId", ti.originalId)
                put("title", ti.title)
                put("subtitle", ti.subtitle)
                put("originalPayloadJson", ti.originalPayloadJson)
                put("deletedAt", ti.deletedAt)
                put("expiresAt", ti.expiresAt)
            })
        }
        root.put("trash_items", trashArr)

        // 13. Plans
        val plans = database.planDao().getAllPlansList()
        entityCounts["plans"] = plans.size
        val plansArr = JSONArray()
        plans.forEach { p ->
            plansArr.put(JSONObject().apply {
                put("id", p.id)
                put("profileId", p.profileId)
                put("title", p.title)
                put("description", p.description)
                put("level", p.level)
                put("startDateIso", p.startDateIso)
                put("targetDateIso", p.targetDateIso)
                put("status", p.status)
                put("manualProgress", p.manualProgress)
                put("isAutoProgress", p.isAutoProgress)
                put("parentPlanId", p.parentPlanId ?: JSONObject.NULL)
                put("projectId", p.projectId ?: JSONObject.NULL)
                put("priority", p.priority)
                put("createdAt", p.createdAt)
                put("updatedAt", p.updatedAt)
            })
        }
        root.put("plans", plansArr)

        // 14. Plan Milestones
        val milestones = database.planDao().getAllMilestonesList()
        entityCounts["plan_milestones"] = milestones.size
        val milestonesArr = JSONArray()
        milestones.forEach { m ->
            milestonesArr.put(JSONObject().apply {
                put("id", m.id)
                put("planId", m.planId)
                put("title", m.title)
                put("isCompleted", m.isCompleted)
                put("orderIndex", m.orderIndex)
            })
        }
        root.put("plan_milestones", milestonesArr)

        // 15. Plan Task Links
        val planTaskRefs = database.planDao().getAllPlanTaskCrossRefs()
        entityCounts["plan_task_cross_refs"] = planTaskRefs.size
        val planRefsArr = JSONArray()
        planTaskRefs.forEach { r ->
            planRefsArr.put(JSONObject().apply {
                put("planId", r.planId)
                put("taskId", r.taskId)
            })
        }
        root.put("plan_task_cross_refs", planRefsArr)

        // 16. Gamification Profile
        val gamProfile = database.gamificationDao().getProfile()
        if (gamProfile != null) {
            entityCounts["gamification_profile"] = 1
            root.put("gamification_profile", JSONObject().apply {
                put("currentXp", gamProfile.currentXp)
                put("currentCoins", gamProfile.currentCoins)
                put("currentLevel", gamProfile.currentLevel)
                put("hunterRank", gamProfile.hunterRank)
            })
        } else {
            entityCounts["gamification_profile"] = 0
        }

        // 17. XP Events
        val xpEvents = database.gamificationDao().getAllXpEventsList()
        entityCounts["xp_events"] = xpEvents.size
        val xpArr = JSONArray()
        xpEvents.forEach { xe ->
            xpArr.put(JSONObject().apply {
                put("eventKey", xe.eventKey)
                put("xpAwarded", xe.xpAwarded)
                put("coinsAwarded", xe.coinsAwarded)
                put("description", xe.description)
                put("timestamp", xe.timestamp)
            })
        }
        root.put("xp_events", xpArr)

        // 18. Achievements
        val achievements = database.gamificationDao().getAllAchievementsList()
        entityCounts["achievements"] = achievements.size
        val achArr = JSONArray()
        achievements.forEach { a ->
            achArr.put(JSONObject().apply {
                put("code", a.code)
                put("title", a.title)
                put("description", a.description)
                put("tier", a.tier)
                put("xpReward", a.xpReward)
                put("coinsReward", a.coinsReward)
                put("isUnlocked", a.isUnlocked)
                put("unlockedAt", a.unlockedAt ?: JSONObject.NULL)
            })
        }
        root.put("achievements", achArr)

        // 19. Rewards
        val rewards = database.gamificationDao().getAllRewardsList()
        entityCounts["rewards"] = rewards.size
        val rewArr = JSONArray()
        rewards.forEach { rw ->
            rewArr.put(JSONObject().apply {
                put("id", rw.id)
                put("title", rw.title)
                put("description", rw.description)
                put("coinCost", rw.coinCost)
                put("iconEmoji", rw.iconEmoji)
                put("isArchived", rw.isArchived)
                put("redemptionCount", rw.redemptionCount)
                put("createdAt", rw.createdAt)
            })
        }
        root.put("rewards", rewArr)

        // 20. Reward Redemptions
        val redemptions = database.gamificationDao().getAllRewardRedemptionsList()
        entityCounts["reward_redemptions"] = redemptions.size
        val redArr = JSONArray()
        redemptions.forEach { rr ->
            redArr.put(JSONObject().apply {
                put("id", rr.id)
                put("rewardId", rr.rewardId)
                put("rewardTitle", rr.rewardTitle)
                put("coinsSpent", rr.coinsSpent)
                put("timestamp", rr.timestamp)
            })
        }
        root.put("reward_redemptions", redArr)

        // 21. Medications
        val medications = database.medicationDao().getAllMedicationsList()
        entityCounts["medications"] = medications.size
        val medsArr = JSONArray()
        medications.forEach { m ->
            medsArr.put(JSONObject().apply {
                put("id", m.id)
                put("profileId", m.profileId)
                put("taskId", m.taskId)
                put("name", m.name)
                put("form", m.form)
                put("dosageAmount", m.dosageAmount)
                put("dosageUnit", m.dosageUnit)
                put("strength", m.strength)
                put("instructions", m.instructions)
                put("scheduleType", m.scheduleType)
                put("mealPeriodsJson", m.mealPeriodsJson)
                put("fixedTimesJson", m.fixedTimesJson)
                put("intervalHours", m.intervalHours)
                put("intervalModel", m.intervalModel)
                put("intervalAnchorTime", m.intervalAnchorTime)
                put("allowOvernight", m.allowOvernight)
                put("selectedDaysOfWeek", m.selectedDaysOfWeek)
                put("startDateIso", m.startDateIso)
                put("endDateIso", m.endDateIso ?: JSONObject.NULL)
                put("courseDurationDays", m.courseDurationDays ?: JSONObject.NULL)
                put("isAsNeeded", m.isAsNeeded)
                put("isPaused", m.isPaused)
                put("reminderSound", m.reminderSound)
                put("alertStyle", m.alertStyle)
                put("snoozeMinutes", m.snoozeMinutes)
                put("followUpEnabled", m.followUpEnabled)
                put("followUpIntervalMinutes", m.followUpIntervalMinutes)
                put("maxFollowUps", m.maxFollowUps)
                put("privacySafeNotification", m.privacySafeNotification)
                put("createdAt", m.createdAt)
            })
        }
        root.put("medications", medsArr)

        // 22. Medication Doses
        val doses = database.medicationDao().getAllDosesList()
        entityCounts["medication_doses"] = doses.size
        val dosesArr = JSONArray()
        doses.forEach { d ->
            dosesArr.put(JSONObject().apply {
                put("id", d.id)
                put("medicationId", d.medicationId)
                put("taskId", d.taskId)
                put("medicationName", d.medicationName)
                put("medicationForm", d.medicationForm)
                put("dosage", d.dosage)
                put("instructions", d.instructions)
                put("scheduledDateIso", d.scheduledDateIso)
                put("scheduledTime", d.scheduledTime)
                put("status", d.status)
                put("actionTimestamp", d.actionTimestamp ?: JSONObject.NULL)
                put("snoozeCount", d.snoozeCount)
                put("followUpCount", d.followUpCount)
                put("notes", d.notes)
            })
        }
        root.put("medication_doses", dosesArr)

        // 23. Trackers
        val trackers = database.trackerDao().getAllTrackersList()
        entityCounts["trackers"] = trackers.size
        val trackersArr = JSONArray()
        trackers.forEach { tr ->
            trackersArr.put(JSONObject().apply {
                put("id", tr.id)
                put("title", tr.title)
                put("emoji", tr.emoji)
                put("description", tr.description)
                put("projectId", tr.projectId ?: JSONObject.NULL)
                put("labelIdsJson", tr.labelIdsJson)
                put("customColorHex", tr.customColorHex ?: JSONObject.NULL)
                put("trackDuration", tr.trackDuration)
                put("trackDistance", tr.trackDistance)
                put("trackQuantity", tr.trackQuantity)
                put("distanceUnit", tr.distanceUnit)
                put("quantityUnit", tr.quantityUnit)
                put("customQuantityUnitName", tr.customQuantityUnitName)
                put("inventoryPolicy", tr.inventoryPolicy)
                put("currentStock", tr.currentStock)
                put("emptyStock", tr.emptyStock)
                put("isReminderEnabled", tr.isReminderEnabled)
                put("reminderDaysInterval", tr.reminderDaysInterval)
                put("isArchived", tr.isArchived)
                put("profileId", tr.profileId)
                put("createdAt", tr.createdAt)
                put("updatedAt", tr.updatedAt)
            })
        }
        root.put("trackers", trackersArr)

        // 24. Tracker Cycles
        val cycles = database.trackerDao().getAllCyclesList()
        entityCounts["tracker_cycles"] = cycles.size
        val cyclesArr = JSONArray()
        cycles.forEach { c ->
            cyclesArr.put(JSONObject().apply {
                put("id", c.id)
                put("trackerId", c.trackerId)
                put("cycleName", c.cycleName)
                put("purchaseId", c.purchaseId ?: JSONObject.NULL)
                put("purchaseCost", c.purchaseCost)
                put("startDateIso", c.startDateIso)
                put("finishDateIso", c.finishDateIso ?: JSONObject.NULL)
                put("status", c.status)
                put("isPaused", c.isPaused)
                put("startOdometer", c.startOdometer)
                put("currentOdometer", c.currentOdometer)
                put("finishOdometer", c.finishOdometer ?: JSONObject.NULL)
                put("initialQuantity", c.initialQuantity)
                put("remainingQuantity", c.remainingQuantity)
                put("notes", c.notes)
                put("createdAt", c.createdAt)
                put("updatedAt", c.updatedAt)
            })
        }
        root.put("tracker_cycles", cyclesArr)

        // 25. Tracker Usage Entries
        val entries = database.trackerDao().getAllEntriesList()
        entityCounts["tracker_usage_entries"] = entries.size
        val entriesArr = JSONArray()
        entries.forEach { e ->
            entriesArr.put(JSONObject().apply {
                put("id", e.id)
                put("cycleId", e.cycleId)
                put("dateIso", e.dateIso)
                put("odometerReading", e.odometerReading)
                put("quantityDeducted", e.quantityDeducted)
                put("manualRemainingQuantity", e.manualRemainingQuantity ?: JSONObject.NULL)
                put("notes", e.notes)
                put("timestamp", e.timestamp)
            })
        }
        root.put("tracker_usage_entries", entriesArr)

        // 26. Tracker Purchases
        val purchases = database.trackerDao().getAllPurchasesList()
        entityCounts["tracker_purchases"] = purchases.size
        val purchasesArr = JSONArray()
        purchases.forEach { p ->
            purchasesArr.put(JSONObject().apply {
                put("id", p.id)
                put("trackerId", p.trackerId)
                put("purchaseDateIso", p.purchaseDateIso)
                put("quantity", p.quantity)
                put("unitPrice", p.unitPrice)
                put("totalCost", p.totalCost)
                put("notes", p.notes)
                put("createdAt", p.createdAt)
            })
        }
        root.put("tracker_purchases", purchasesArr)

        // 27. Tracker Inventory Adjustments
        val adjustments = database.trackerDao().getAllAdjustmentsList()
        entityCounts["tracker_inventory_adjustments"] = adjustments.size
        val adjustmentsArr = JSONArray()
        adjustments.forEach { ia ->
            adjustmentsArr.put(JSONObject().apply {
                put("id", ia.id)
                put("trackerId", ia.trackerId)
                put("dateIso", ia.dateIso)
                put("deltaQuantity", ia.deltaQuantity)
                put("isRefillExchange", ia.isRefillExchange)
                put("reason", ia.reason)
                put("timestamp", ia.timestamp)
            })
        }
        root.put("tracker_inventory_adjustments", adjustmentsArr)

        // 28. Subscriptions
        val subs = database.subscriptionDao().getAllSubscriptionsList()
        entityCounts["subscriptions"] = subs.size
        val subsArr = JSONArray()
        subs.forEach { s ->
            subsArr.put(JSONObject().apply {
                put("id", s.id)
                put("title", s.title)
                put("emoji", s.emoji)
                put("description", s.description)
                put("projectId", s.projectId ?: JSONObject.NULL)
                put("labelIdsJson", s.labelIdsJson)
                put("customColorHex", s.customColorHex ?: JSONObject.NULL)
                put("amount", s.amount)
                put("currency", s.currency)
                put("frequency", s.frequency)
                put("customIntervalCount", s.customIntervalCount)
                put("customIntervalUnit", s.customIntervalUnit)
                put("anchorDateIso", s.anchorDateIso)
                put("anchorDayOfMonth", s.anchorDayOfMonth)
                put("nextRenewalDateIso", s.nextRenewalDateIso)
                put("missingDatePolicy", s.missingDatePolicy)
                put("autoRenew", s.autoRenew)
                put("paymentMethod", s.paymentMethod)
                put("isTrial", s.isTrial)
                put("trialStartDateIso", s.trialStartDateIso ?: JSONObject.NULL)
                put("trialEndDateIso", s.trialEndDateIso ?: JSONObject.NULL)
                put("status", s.status)
                put("cancellationDateIso", s.cancellationDateIso ?: JSONObject.NULL)
                put("cancellationReason", s.cancellationReason)
                put("reminderDaysBefore", s.reminderDaysBefore)
                put("overdueReminderEnabled", s.overdueReminderEnabled)
                put("notes", s.notes)
                put("profileId", s.profileId)
                put("createdAt", s.createdAt)
                put("updatedAt", s.updatedAt)
            })
        }
        root.put("subscriptions", subsArr)

        // 29. Subscription Bills
        val bills = database.subscriptionDao().getAllBillsList()
        entityCounts["subscription_bills"] = bills.size
        val billsArr = JSONArray()
        bills.forEach { b ->
            billsArr.put(JSONObject().apply {
                put("id", b.id)
                put("subscriptionId", b.subscriptionId)
                put("dueDateIso", b.dueDateIso)
                put("expectedAmount", b.expectedAmount)
                put("currency", b.currency)
                put("status", b.status)
                put("notes", b.notes)
                put("createdAt", b.createdAt)
            })
        }
        root.put("subscription_bills", billsArr)

        // 30. Subscription Payments
        val payments = database.subscriptionDao().getAllPaymentsList()
        entityCounts["subscription_payments"] = payments.size
        val paymentsArr = JSONArray()
        payments.forEach { pay ->
            paymentsArr.put(JSONObject().apply {
                put("id", pay.id)
                put("billId", pay.billId)
                put("subscriptionId", pay.subscriptionId)
                put("amountPaid", pay.amountPaid)
                put("paymentDateIso", pay.paymentDateIso)
                put("paymentMethod", pay.paymentMethod)
                put("transactionRef", pay.transactionRef)
                put("notes", pay.notes)
                put("timestamp", pay.timestamp)
            })
        }
        root.put("subscription_payments", paymentsArr)

        // 31. Subscription Price History
        val priceHistories = database.subscriptionDao().getAllPriceHistoryList()
        entityCounts["subscription_price_history"] = priceHistories.size
        val phArr = JSONArray()
        priceHistories.forEach { ph ->
            phArr.put(JSONObject().apply {
                put("id", ph.id)
                put("subscriptionId", ph.subscriptionId)
                put("oldAmount", ph.oldAmount)
                put("newAmount", ph.newAmount)
                put("effectiveDateIso", ph.effectiveDateIso)
                put("reason", ph.reason)
                put("createdAt", ph.createdAt)
            })
        }
        root.put("subscription_price_history", phArr)

        // 32. Project Workspace Items
        val workspaceItems = database.projectWorkspaceDao().getAllItems()
        entityCounts["project_workspace_items"] = workspaceItems.size
        val wsItemsArr = JSONArray()
        workspaceItems.forEach { item ->
            wsItemsArr.put(JSONObject().apply {
                put("id", item.id)
                put("profileId", item.profileId)
                put("projectId", item.projectId)
                put("title", item.title)
                put("status", item.status)
                put("progress", item.progress)
                put("category", item.category)
                put("version", item.version)
                put("dueDateIso", item.dueDateIso ?: JSONObject.NULL)
                put("notes", item.notes)
                put("priority", item.priority)
                put("customPropertiesJson", item.customPropertiesJson)
                put("orderIndex", item.orderIndex)
                put("createdAt", item.createdAt)
                put("updatedAt", item.updatedAt)
            })
        }
        root.put("project_workspace_items", wsItemsArr)

        // 33. Project Workspace Views
        val workspaceViews = database.projectWorkspaceDao().getAllViews()
        entityCounts["project_workspace_views"] = workspaceViews.size
        val wsViewsArr = JSONArray()
        workspaceViews.forEach { v ->
            wsViewsArr.put(JSONObject().apply {
                put("id", v.id)
                put("profileId", v.profileId)
                put("projectId", v.projectId)
                put("name", v.name)
                put("viewType", v.viewType)
                put("filterStatus", v.filterStatus ?: JSONObject.NULL)
                put("filterCategory", v.filterCategory ?: JSONObject.NULL)
                put("filterVersion", v.filterVersion ?: JSONObject.NULL)
                put("sortBy", v.sortBy)
                put("sortAscending", v.sortAscending)
                put("groupBy", v.groupBy)
                put("visiblePropertiesJson", v.visiblePropertiesJson)
                put("isDefault", v.isDefault)
                put("createdAt", v.createdAt)
                put("updatedAt", v.updatedAt)
            })
        }
        root.put("project_workspace_views", wsViewsArr)

        // 34. Project Custom Statuses
        val customStatuses = database.projectWorkspaceDao().getAllCustomStatuses()
        entityCounts["project_custom_statuses"] = customStatuses.size
        val customStatusesArr = JSONArray()
        customStatuses.forEach { cs ->
            customStatusesArr.put(JSONObject().apply {
                put("id", cs.id)
                put("profileId", cs.profileId)
                put("projectId", cs.projectId)
                put("name", cs.name)
                put("colorHex", cs.colorHex)
                put("orderIndex", cs.orderIndex)
            })
        }
        root.put("project_custom_statuses", customStatusesArr)

        // 35. Project Custom Properties
        val customProperties = database.projectWorkspaceDao().getAllCustomProperties()
        entityCounts["project_custom_properties"] = customProperties.size
        val customPropsArr = JSONArray()
        customProperties.forEach { cp ->
            customPropsArr.put(JSONObject().apply {
                put("id", cp.id)
                put("profileId", cp.profileId)
                put("projectId", cp.projectId)
                put("name", cp.name)
                put("propertyType", cp.propertyType)
                put("optionsJson", cp.optionsJson)
                put("isVisible", cp.isVisible)
                put("orderIndex", cp.orderIndex)
            })
        }
        root.put("project_custom_properties", customPropsArr)

        // 36. Glossary Packs
        val glossaryPacks = database.glossaryDao().getAllPacksList()
        entityCounts["glossary_packs"] = glossaryPacks.size
        val glossaryPacksArr = JSONArray()
        glossaryPacks.forEach { gp ->
            glossaryPacksArr.put(JSONObject().apply {
                put("id", gp.id)
                put("title", gp.title)
                put("description", gp.description)
                put("category", gp.category)
                put("version", gp.version)
                put("author", gp.author)
                put("itemCount", gp.itemCount)
                put("sha256", gp.sha256)
                put("isInstalled", gp.isInstalled)
                put("installedAt", gp.installedAt)
                put("sizeBytes", gp.sizeBytes)
            })
        }
        root.put("glossary_packs", glossaryPacksArr)

        // 37. Glossary Items
        val glossaryItems = database.glossaryDao().getAllItemsList()
        entityCounts["glossary_items"] = glossaryItems.size
        val glossaryItemsArr = JSONArray()
        glossaryItems.forEach { gi ->
            glossaryItemsArr.put(JSONObject().apply {
                put("id", gi.id)
                put("packId", gi.packId)
                put("term", gi.term)
                put("phonetic", gi.phonetic ?: JSONObject.NULL)
                put("category", gi.category)
                put("definition", gi.definition)
                put("detailedExplanation", gi.detailedExplanation)
                put("examplesJson", gi.examplesJson)
                put("relatedTermsJson", gi.relatedTermsJson)
                put("tagsJson", gi.tagsJson)
                put("isBookmarked", gi.isBookmarked)
                put("createdAt", gi.createdAt)
            })
        }
        root.put("glossary_items", glossaryItemsArr)

        // 38. Glossary User Data
        val glossaryUserData = database.glossaryDao().getAllUserDataList()
        entityCounts["glossary_user_data"] = glossaryUserData.size
        val glossaryUserDataArr = JSONArray()
        glossaryUserData.forEach { ud ->
            glossaryUserDataArr.put(JSONObject().apply {
                put("id", ud.id)
                put("profileId", ud.profileId)
                put("termId", ud.termId)
                put("isBookmarked", ud.isBookmarked)
                put("personalNote", ud.personalNote)
                put("lastViewedAt", ud.lastViewedAt)
                put("updatedAt", ud.updatedAt)
            })
        }
        root.put("glossary_user_data", glossaryUserDataArr)

        // 39. MCP Servers
        val mcpServers = database.mcpDao().getAllServers()
        entityCounts["mcp_servers"] = mcpServers.size
        val mcpServersArr = JSONArray()
        mcpServers.forEach { s ->
            mcpServersArr.put(JSONObject().apply {
                put("id", s.id)
                put("profileId", s.profileId)
                put("name", s.name)
                put("endpoint", s.endpoint)
                put("transport", s.transport)
                put("authToken", s.authToken ?: JSONObject.NULL)
                put("createdAt", s.createdAt)
            })
        }
        root.put("mcp_servers", mcpServersArr)

        // 40. MCP Security Audit Logs
        val auditLogs = database.mcpDao().getAllAuditLogs()
        entityCounts["mcp_audit_logs"] = auditLogs.size
        val auditLogsArr = JSONArray()
        auditLogs.forEach { al ->
            auditLogsArr.put(JSONObject().apply {
                put("id", al.id)
                put("profileId", al.profileId)
                put("timestamp", al.timestamp)
                put("direction", al.direction)
                put("eventType", al.eventType)
                put("toolOrResource", al.toolOrResource)
                put("details", al.details)
                put("status", al.status)
            })
        }
        root.put("mcp_audit_logs", auditLogsArr)

        // Compute deterministic SHA-256 over all 40 entity datasets
        val checksumSha256 = computePayloadChecksum(root)
        val totalRecords = entityCounts.values.sum()

        // Metadata block
        val countsJson = JSONObject()
        entityCounts.forEach { (k, v) -> countsJson.put(k, v) }

        val metadata = JSONObject().apply {
            put("formatVersion", BACKUP_FORMAT_VERSION)
            put("appVersion", APP_VERSION)
            put("databaseVersion", DATABASE_VERSION)
            put("exportedAt", timestamp)
            put("totalRecords", totalRecords)
            put("checksumSha256", checksumSha256)
            put("entityCounts", countsJson)
        }

        // Top-level compatibility and metadata
        root.put("version", BACKUP_FORMAT_VERSION)
        root.put("formatVersion", BACKUP_FORMAT_VERSION)
        root.put("appVersion", APP_VERSION)
        root.put("databaseVersion", DATABASE_VERSION)
        root.put("exportedAt", timestamp)
        root.put("exportDateReadable", readableDate)
        root.put("metadata", metadata)

        return@withContext root.toString(2)
    }

    suspend fun exportToStream(database: AppDatabase, outputStream: OutputStream) = withContext(Dispatchers.IO) {
        val json = exportAllData(database)
        outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write(json)
            writer.flush()
        }
    }

    // =========================================================================
    // VALIDATION & INTEGRITY
    // =========================================================================

    fun computePayloadChecksum(root: JSONObject): String {
        val md = MessageDigest.getInstance("SHA-256")
        for (key in ALL_40_ENTITY_KEYS) {
            if (root.has(key)) {
                md.update(key.toByteArray(Charsets.UTF_8))
                md.update(root.get(key).toString().toByteArray(Charsets.UTF_8))
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    fun validateBackup(jsonStr: String): ValidationResult {
        if (jsonStr.isBlank()) {
            return ValidationResult.Invalid("Backup payload is empty.")
        }
        if (jsonStr.length > MAX_BACKUP_SIZE_BYTES) {
            return ValidationResult.Invalid("Backup payload exceeds maximum allowed size (${MAX_BACKUP_SIZE_BYTES / 1_000_000} MB).")
        }

        val root: JSONObject = try {
            JSONObject(jsonStr)
        } catch (e: Exception) {
            return ValidationResult.Invalid("Malformed JSON backup: ${e.message}")
        }

        val formatVersion = root.optInt("formatVersion", root.optInt("version", 1))
        if (formatVersion > BACKUP_FORMAT_VERSION) {
            return ValidationResult.Invalid("Unsupported backup version: $formatVersion (maximum supported version is $BACKUP_FORMAT_VERSION). Please update LifeOS.")
        }

        val metaObj = root.optJSONObject("metadata")
        val expectedChecksum = metaObj?.optString("checksumSha256")
        val isLegacy = (formatVersion < 2 || expectedChecksum.isNullOrBlank())

        var isChecksumValid: Boolean? = null
        if (!isLegacy && !expectedChecksum.isNullOrBlank()) {
            val actualChecksum = computePayloadChecksum(root)
            if (actualChecksum != expectedChecksum) {
                return ValidationResult.Invalid("Integrity validation failed: Checksum mismatch. The backup file may be corrupt or modified.")
            }
            isChecksumValid = true
        }

        val entityCounts = mutableMapOf<String, Int>()
        for (key in ALL_40_ENTITY_KEYS) {
            if (root.has(key)) {
                val optArr = root.optJSONArray(key)
                if (optArr != null) {
                    entityCounts[key] = optArr.length()
                } else if (root.optJSONObject(key) != null) {
                    entityCounts[key] = 1
                }
            }
        }
        val totalRecords = entityCounts.values.sum()

        val summary = BackupSummary(
            formatVersion = formatVersion,
            appVersion = metaObj?.optString("appVersion", root.optString("appVersion", "Unknown")) ?: "Unknown",
            databaseVersion = metaObj?.optInt("databaseVersion", root.optInt("databaseVersion", 18)) ?: 18,
            exportedAt = metaObj?.optLong("exportedAt", root.optLong("exportedAt", System.currentTimeMillis())) ?: System.currentTimeMillis(),
            totalRecords = totalRecords,
            entityCounts = entityCounts,
            checksumSha256 = expectedChecksum,
            isChecksumValid = isChecksumValid,
            isLegacy = isLegacy
        )

        return ValidationResult.Valid(summary)
    }

    // =========================================================================
    // RESTORE (REPLACE_ALL with Safety & Recovery)
    // =========================================================================

    suspend fun createPreRestoreRecovery(database: AppDatabase, context: Context): File = withContext(Dispatchers.IO) {
        val recoveryDir = File(context.filesDir, "recovery_backups")
        if (!recoveryDir.exists()) recoveryDir.mkdirs()
        val recoveryFile = File(recoveryDir, "pre_restore_recovery.json")

        val currentBackup = exportAllData(database)
        recoveryFile.writeText(currentBackup, Charsets.UTF_8)
        recoveryFile
    }

    /**
     * Performs safe, transactional REPLACE_ALL database restore.
     *
     * Invariants guaranteed:
     * 1. Pre-validation and checksum verification before any database mutation.
     * 2. Recovery backup written to app-private storage before database replacement.
     * 3. Database operations execute inside a single Room transaction (atomicity boundary).
     * 4. Reverse-dependency deletion order prevents foreign-key constraint violations.
     * 5. Forward-dependency insertion order restores parent entities before children.
     * 6. Complete rollback if any SQL operation fails.
     */
    suspend fun restoreData(
        database: AppDatabase,
        jsonStr: String,
        context: Context? = null
    ): RestoreResult = withContext(Dispatchers.IO) {
        val validation = validateBackup(jsonStr)
        if (validation is ValidationResult.Invalid) {
            return@withContext RestoreResult.Error(validation.error)
        }
        val summary = (validation as ValidationResult.Valid).summary

        // Create safety net recovery backup if context is provided
        if (context != null) {
            try {
                createPreRestoreRecovery(database, context)
            } catch (e: Exception) {
                // If writing safety recovery backup fails, abort restore to protect user
                return@withContext RestoreResult.Error("Failed to create pre-restore recovery backup: ${e.message}", e)
            }
        }

        try {
            database.withTransaction {
                val root = JSONObject(jsonStr)

                // -----------------------------------------------------------------
                // PHASE 1: DELETE ALL (Reverse-Dependency Order)
                // -----------------------------------------------------------------
                database.mcpDao().clearAllAuditLogs()
                database.mcpDao().clearAllServers()
                database.glossaryDao().clearAllUserData()
                database.glossaryDao().clearAllItems()
                database.glossaryDao().clearAllPacks()
                database.projectWorkspaceDao().clearAllCustomProperties()
                database.projectWorkspaceDao().clearAllCustomStatuses()
                database.projectWorkspaceDao().clearAllViews()
                database.projectWorkspaceDao().clearAllItems()
                database.subscriptionDao().clearAllPriceHistory()
                database.subscriptionDao().clearAllPayments()
                database.subscriptionDao().clearAllBills()
                database.subscriptionDao().clearAllSubscriptions()
                database.trackerDao().clearAllAdjustments()
                database.trackerDao().clearAllEntries()
                database.trackerDao().clearAllPurchases()
                database.trackerDao().clearAllCycles()
                database.trackerDao().clearAllTrackers()
                database.medicationDao().deleteAllDoses()
                database.medicationDao().deleteAllMedications()
                database.gamificationDao().clearAllRedemptions()
                database.gamificationDao().clearAllRewards()
                database.gamificationDao().clearAllAchievements()
                database.gamificationDao().clearAllXpEvents()
                database.gamificationDao().clearGamificationProfile()
                database.planDao().clearAllPlanTaskCrossRefs()
                database.planDao().deleteAllMilestones()
                database.planDao().deleteAllPlans()
                database.trashDao().clearTrash()
                database.taskReminderDao().deleteAllReminders()
                database.labelDao().clearAllTaskLabelCrossRefs()
                database.taskDao().deleteAllTasks()
                database.labelDao().deleteAllLabels()
                database.projectDao().deleteAllProjects()
                database.honeycombShortcutDao().clearAllShortcuts()
                database.waterLogDao().deleteAllWaterLogs()
                database.challengeDao().deleteChallenge()
                database.habitDao().deleteAllHabits()
                database.profileSurveyDao().deleteAllSurveys()
                database.profileDao().deleteAllProfiles()

                // -----------------------------------------------------------------
                // PHASE 2: INSERT ALL (Forward-Dependency Order)
                // -----------------------------------------------------------------

                // 1. Profiles
                val profsArr = root.optJSONArray("profiles")
                if (profsArr != null && profsArr.length() > 0) {
                    val list = mutableListOf<ProfileEntity>()
                    for (i in 0 until profsArr.length()) {
                        val obj = profsArr.getJSONObject(i)
                        list.add(ProfileEntity(
                            id = obj.optLong("id", 0L),
                            name = obj.getString("name"),
                            emoji = obj.optString("emoji", "📁"),
                            colorHex = obj.optLong("colorHex", 0xFF3B82F6),
                            type = obj.optString("type", ProfileEntity.TYPE_CUSTOM),
                            orderIndex = obj.optInt("orderIndex", 0),
                            isDefault = obj.optBoolean("isDefault", false),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        ))
                    }
                    database.profileDao().insertProfiles(list)
                } else {
                    // Seed default profile if absent (e.g. legacy v1 backup without profile partitioning)
                    database.profileDao().insertProfile(ProfileEntity(
                        id = 1L,
                        name = "Personal",
                        emoji = "👤",
                        colorHex = 0xFF3B82F6,
                        type = ProfileEntity.TYPE_PERSONAL,
                        isDefault = true
                    ))
                }

                // 2. Profile Surveys
                val surveysArr = root.optJSONArray("profile_surveys")
                if (surveysArr != null) {
                    val list = mutableListOf<ProfileSurveyEntity>()
                    for (i in 0 until surveysArr.length()) {
                        val obj = surveysArr.getJSONObject(i)
                        list.add(ProfileSurveyEntity(
                            profileId = obj.optLong("profileId", 1L),
                            displayName = if (obj.isNull("displayName")) null else obj.optString("displayName"),
                            gender = if (obj.isNull("gender")) null else obj.optString("gender"),
                            sleepingTime = if (obj.isNull("sleepingTime")) null else obj.optString("sleepingTime"),
                            dailyWaterGoalMl = if (obj.isNull("dailyWaterGoalMl")) null else obj.optInt("dailyWaterGoalMl"),
                            discoverySource = if (obj.isNull("discoverySource")) null else obj.optString("discoverySource"),
                            discoverySocialPlatform = if (obj.isNull("discoverySocialPlatform")) null else obj.optString("discoverySocialPlatform"),
                            discoveryOtherDetail = if (obj.isNull("discoveryOtherDetail")) null else obj.optString("discoveryOtherDetail"),
                            isCompleted = obj.optBoolean("isCompleted", false),
                            isSkipped = obj.optBoolean("isSkipped", false),
                            completedAt = obj.optLong("completedAt", System.currentTimeMillis()),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        ))
                    }
                    database.profileSurveyDao().insertSurveys(list)
                }

                // 3. Habits
                val habitsArr = root.optJSONArray("habits")
                if (habitsArr != null) {
                    val list = mutableListOf<HabitEntity>()
                    for (i in 0 until habitsArr.length()) {
                        val obj = habitsArr.getJSONObject(i)
                        list.add(HabitEntity(
                            id = obj.optLong("id", 0L),
                            title = obj.getString("title"),
                            emoji = obj.optString("emoji", "✨"),
                            description = obj.optString("description", ""),
                            reminderTime = if (obj.isNull("reminderTime")) null else obj.optString("reminderTime"),
                            colorHex = obj.optLong("colorHex", 0xFF3B82F6),
                            completedDatesJson = obj.optString("completedDatesJson", "[]"),
                            orderIndex = obj.optInt("orderIndex", 0),
                            profileId = obj.optLong("profileId", 1L),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        ))
                    }
                    database.habitDao().insertAll(list)
                }

                // 4. Challenges
                val challengesArr = root.optJSONArray("challenges")
                if (challengesArr != null) {
                    val list = mutableListOf<ChallengeEntity>()
                    for (i in 0 until challengesArr.length()) {
                        val obj = challengesArr.getJSONObject(i)
                        list.add(ChallengeEntity(
                            id = obj.optLong("id", 1L),
                            startDateIso = obj.optString("startDateIso", "2024-01-01"),
                            targetDays = obj.optInt("targetDays", 75),
                            status = obj.optString("status", "ACTIVE"),
                            rulesJson = obj.optString("rulesJson", "[]"),
                            dailyLogsJson = obj.optString("dailyLogsJson", "{}"),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        ))
                    }
                    database.challengeDao().insertChallenges(list)
                }

                // 5. Water Logs
                val waterArr = root.optJSONArray("water_logs")
                if (waterArr != null) {
                    val list = mutableListOf<WaterLogEntity>()
                    for (i in 0 until waterArr.length()) {
                        val obj = waterArr.getJSONObject(i)
                        list.add(WaterLogEntity(
                            dateIso = obj.getString("dateIso"),
                            amountMl = obj.optInt("amountMl", 0),
                            goalMl = obj.optInt("goalMl", 2000),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        ))
                    }
                    database.waterLogDao().insertWaterLogs(list)
                }

                // 6. Honeycomb Shortcuts
                val scArr = root.optJSONArray("honeycomb_shortcuts")
                if (scArr != null) {
                    val list = mutableListOf<HoneycombShortcutEntity>()
                    for (i in 0 until scArr.length()) {
                        val obj = scArr.getJSONObject(i)
                        list.add(HoneycombShortcutEntity(
                            id = obj.optLong("id", 0L),
                            type = obj.getString("type"),
                            targetId = obj.getString("targetId"),
                            title = obj.getString("title"),
                            customTileName = if (obj.isNull("customTileName")) null else obj.optString("customTileName"),
                            emoji = obj.optString("emoji", "✅"),
                            customColorHex = if (obj.isNull("customColorHex")) null else obj.optLong("customColorHex"),
                            colorSource = obj.optString("colorSource", "PROJECT"),
                            shape = obj.optString("shape", "HEXAGON"),
                            orderIndex = obj.optInt("orderIndex", 0),
                            coordQ = obj.optInt("coordQ", 0),
                            coordR = obj.optInt("coordR", 0),
                            isPinned = obj.optBoolean("isPinned", false),
                            profileId = obj.optLong("profileId", 1L),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        ))
                    }
                    database.honeycombShortcutDao().insertShortcuts(list)
                }

                // 7. Projects
                val projArr = root.optJSONArray("projects")
                if (projArr != null) {
                    val list = mutableListOf<ProjectEntity>()
                    for (i in 0 until projArr.length()) {
                        val obj = projArr.getJSONObject(i)
                        list.add(ProjectEntity(
                            id = obj.optLong("id", 0L),
                            name = obj.getString("name"),
                            description = obj.optString("description", ""),
                            colorId = obj.optString("colorId", "charcoal"),
                            emoji = obj.optString("emoji", "📁"),
                            isFavorite = obj.optBoolean("isFavorite", false),
                            isArchived = obj.optBoolean("isArchived", false),
                            orderIndex = obj.optInt("orderIndex", 0),
                            profileId = obj.optLong("profileId", 1L),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        ))
                    }
                    database.projectDao().insertProjects(list)
                }

                // 8. Labels
                val labelsArr = root.optJSONArray("labels")
                if (labelsArr != null) {
                    val list = mutableListOf<LabelEntity>()
                    for (i in 0 until labelsArr.length()) {
                        val obj = labelsArr.getJSONObject(i)
                        list.add(LabelEntity(
                            id = obj.optLong("id", 0L),
                            name = obj.getString("name"),
                            colorId = obj.optString("colorId", "charcoal"),
                            isFavorite = obj.optBoolean("isFavorite", false),
                            isArchived = obj.optBoolean("isArchived", false),
                            orderIndex = obj.optInt("orderIndex", 0),
                            profileId = obj.optLong("profileId", 1L),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        ))
                    }
                    database.labelDao().insertLabels(list)
                }

                // 9. Tasks
                val tasksArr = root.optJSONArray("tasks")
                if (tasksArr != null) {
                    val list = mutableListOf<TaskEntity>()
                    for (i in 0 until tasksArr.length()) {
                        val obj = tasksArr.getJSONObject(i)
                        list.add(TaskEntity(
                            id = obj.optLong("id", 0L),
                            title = obj.getString("title"),
                            emoji = obj.optString("emoji", ""),
                            description = obj.optString("description", ""),
                            category = obj.optString("category", "Habits & Daily Routine"),
                            priority = obj.optString("priority", "P3"),
                            dueDateIso = obj.optString("dueDateIso", ""),
                            dueTime = if (obj.isNull("dueTime")) null else obj.optString("dueTime"),
                            isAllDay = obj.optBoolean("isAllDay", true),
                            schedulingMode = obj.optString("schedulingMode", "DURATION"),
                            durationAmount = obj.optInt("durationAmount", 30),
                            durationUnit = obj.optString("durationUnit", "MINUTES"),
                            durationMinutes = obj.optInt("durationMinutes", 30),
                            deadlineIso = if (obj.isNull("deadlineIso")) null else obj.optString("deadlineIso"),
                            deadlineTime = if (obj.isNull("deadlineTime")) null else obj.optString("deadlineTime"),
                            recurrenceType = obj.optString("recurrenceType", "NONE"),
                            recurrenceInterval = obj.optInt("recurrenceInterval", 1),
                            recurrenceUnit = obj.optString("recurrenceUnit", "DAYS"),
                            recurrenceEndCondition = obj.optString("recurrenceEndCondition", "NEVER"),
                            recurrenceEndDateIso = if (obj.isNull("recurrenceEndDateIso")) null else obj.optString("recurrenceEndDateIso"),
                            recurrenceCount = obj.optInt("recurrenceCount", 1),
                            recurrenceWindowStartDay = if (obj.isNull("recurrenceWindowStartDay")) null else obj.optInt("recurrenceWindowStartDay"),
                            recurrenceWindowEndDay = if (obj.isNull("recurrenceWindowEndDay")) null else obj.optInt("recurrenceWindowEndDay"),
                            isOneTimeCompleted = obj.optBoolean("isOneTimeCompleted", false),
                            completedDatesJson = obj.optString("completedDatesJson", "[]"),
                            skippedDatesJson = obj.optString("skippedDatesJson", "[]"),
                            rescheduledDatesJson = obj.optString("rescheduledDatesJson", "{}"),
                            subtasksJson = obj.optString("subtasksJson", "[]"),
                            notes = obj.optString("notes", ""),
                            isReminderEnabled = obj.optBoolean("isReminderEnabled", false),
                            reminderMinutesBefore = obj.optInt("reminderMinutesBefore", 0),
                            isArchived = obj.optBoolean("isArchived", false),
                            isPaused = obj.optBoolean("isPaused", false),
                            isQuickReminder = obj.optBoolean("isQuickReminder", false),
                            projectId = if (obj.isNull("projectId")) null else obj.optLong("projectId"),
                            profileId = obj.optLong("profileId", 1L),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        ))
                    }
                    database.taskDao().insertTasks(list)
                }

                // 10. Task Label Cross Refs
                val xRefArr = root.optJSONArray("task_label_cross_ref")
                if (xRefArr != null) {
                    val list = mutableListOf<TaskLabelCrossRef>()
                    for (i in 0 until xRefArr.length()) {
                        val obj = xRefArr.getJSONObject(i)
                        list.add(TaskLabelCrossRef(
                            taskId = obj.getLong("taskId"),
                            labelId = obj.getLong("labelId")
                        ))
                    }
                    database.labelDao().insertOrReplaceTaskLabelCrossRefs(list)
                }

                // 11. Task Reminders
                val remArr = root.optJSONArray("task_reminders")
                if (remArr != null) {
                    val list = mutableListOf<TaskReminderEntity>()
                    for (i in 0 until remArr.length()) {
                        val obj = remArr.getJSONObject(i)
                        list.add(TaskReminderEntity(
                            id = obj.optLong("id", 0L),
                            taskId = obj.getLong("taskId"),
                            minutesBefore = obj.optInt("minutesBefore", 0),
                            customTime = if (obj.isNull("customTime")) null else obj.optString("customTime"),
                            type = obj.optString("type", "AT_DEADLINE"),
                            referenceType = obj.optString("referenceType", "DEADLINE"),
                            isEnabled = obj.optBoolean("isEnabled", true),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        ))
                    }
                    database.taskReminderDao().insertReminders(list)
                }

                // 12. Trash Items
                val trashArr = root.optJSONArray("trash_items")
                if (trashArr != null) {
                    val list = mutableListOf<TrashItemEntity>()
                    for (i in 0 until trashArr.length()) {
                        val obj = trashArr.getJSONObject(i)
                        list.add(TrashItemEntity(
                            id = obj.optLong("id", 0L),
                            profileId = obj.optLong("profileId", 1L),
                            itemType = obj.optString("itemType", "TASK"),
                            originalId = obj.optLong("originalId", 0L),
                            title = obj.optString("title", ""),
                            subtitle = obj.optString("subtitle", ""),
                            originalPayloadJson = obj.optString("originalPayloadJson", "{}"),
                            deletedAt = obj.optLong("deletedAt", System.currentTimeMillis()),
                            expiresAt = obj.optLong("expiresAt", System.currentTimeMillis() + 30L * 24 * 3600 * 1000)
                        ))
                    }
                    database.trashDao().insertTrashItems(list)
                }

                // 13. Plans
                val plansArr = root.optJSONArray("plans")
                if (plansArr != null) {
                    val list = mutableListOf<PlanEntity>()
                    for (i in 0 until plansArr.length()) {
                        val obj = plansArr.getJSONObject(i)
                        list.add(PlanEntity(
                            id = obj.optLong("id", 0L),
                            profileId = obj.optLong("profileId", 1L),
                            title = obj.getString("title"),
                            description = obj.optString("description", ""),
                            level = obj.getString("level"),
                            startDateIso = obj.getString("startDateIso"),
                            targetDateIso = obj.getString("targetDateIso"),
                            status = obj.optString("status", "ACTIVE"),
                            manualProgress = obj.optInt("manualProgress", 0),
                            isAutoProgress = obj.optBoolean("isAutoProgress", true),
                            parentPlanId = if (obj.isNull("parentPlanId")) null else obj.optLong("parentPlanId"),
                            projectId = if (obj.isNull("projectId")) null else obj.optLong("projectId"),
                            priority = obj.optString("priority", "MEDIUM"),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        ))
                    }
                    database.planDao().insertPlans(list)
                }

                // 14. Plan Milestones
                val milestonesArr = root.optJSONArray("plan_milestones")
                if (milestonesArr != null) {
                    val list = mutableListOf<PlanMilestoneEntity>()
                    for (i in 0 until milestonesArr.length()) {
                        val obj = milestonesArr.getJSONObject(i)
                        list.add(PlanMilestoneEntity(
                            id = obj.optLong("id", 0L),
                            planId = obj.getLong("planId"),
                            title = obj.getString("title"),
                            isCompleted = obj.optBoolean("isCompleted", false),
                            orderIndex = obj.optInt("orderIndex", 0)
                        ))
                    }
                    database.planDao().insertMilestones(list)
                }

                // 15. Plan Task Cross Refs
                val planRefsArr = root.optJSONArray("plan_task_cross_refs")
                if (planRefsArr != null) {
                    val list = mutableListOf<PlanTaskCrossRef>()
                    for (i in 0 until planRefsArr.length()) {
                        val obj = planRefsArr.getJSONObject(i)
                        list.add(PlanTaskCrossRef(
                            planId = obj.getLong("planId"),
                            taskId = obj.getLong("taskId")
                        ))
                    }
                    database.planDao().insertPlanTaskCrossRefs(list)
                }

                // 16. Gamification Profile
                val profObj = root.optJSONObject("gamification_profile")
                if (profObj != null) {
                    database.gamificationDao().insertOrUpdateProfile(GamificationProfileEntity(
                        id = 1,
                        currentXp = profObj.optLong("currentXp", 0L),
                        currentCoins = profObj.optInt("currentCoins", 0),
                        currentLevel = profObj.optInt("currentLevel", 1),
                        hunterRank = profObj.optString("hunterRank", "E")
                    ))
                }

                // 17. XP Events
                val xpArr = root.optJSONArray("xp_events")
                if (xpArr != null) {
                    val list = mutableListOf<XpEventEntity>()
                    for (i in 0 until xpArr.length()) {
                        val obj = xpArr.getJSONObject(i)
                        list.add(XpEventEntity(
                            eventKey = obj.getString("eventKey"),
                            xpAwarded = obj.optInt("xpAwarded", 0),
                            coinsAwarded = obj.optInt("coinsAwarded", 0),
                            description = obj.optString("description", ""),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        ))
                    }
                    database.gamificationDao().insertXpEvents(list)
                }

                // 18. Achievements
                val achArr = root.optJSONArray("achievements")
                if (achArr != null) {
                    val list = mutableListOf<AchievementEntity>()
                    for (i in 0 until achArr.length()) {
                        val obj = achArr.getJSONObject(i)
                        list.add(AchievementEntity(
                            code = obj.getString("code"),
                            title = obj.optString("title", ""),
                            description = obj.optString("description", ""),
                            tier = obj.optString("tier", "BRONZE"),
                            xpReward = obj.optInt("xpReward", 0),
                            coinsReward = obj.optInt("coinsReward", 0),
                            isUnlocked = obj.optBoolean("isUnlocked", false),
                            unlockedAt = if (obj.isNull("unlockedAt")) null else obj.optLong("unlockedAt")
                        ))
                    }
                    database.gamificationDao().insertAchievements(list)
                }

                // 19. Rewards
                val rewArr = root.optJSONArray("rewards")
                if (rewArr != null) {
                    val list = mutableListOf<RewardItemEntity>()
                    for (i in 0 until rewArr.length()) {
                        val obj = rewArr.getJSONObject(i)
                        list.add(RewardItemEntity(
                            id = obj.optLong("id", 0L),
                            title = obj.optString("title", ""),
                            description = obj.optString("description", ""),
                            coinCost = obj.optInt("coinCost", 10),
                            iconEmoji = obj.optString("iconEmoji", "🎁"),
                            isArchived = obj.optBoolean("isArchived", false),
                            redemptionCount = obj.optInt("redemptionCount", 0),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        ))
                    }
                    database.gamificationDao().insertRewards(list)
                }

                // 20. Reward Redemptions
                val redArr = root.optJSONArray("reward_redemptions")
                if (redArr != null) {
                    val list = mutableListOf<RewardRedemptionEntity>()
                    for (i in 0 until redArr.length()) {
                        val obj = redArr.getJSONObject(i)
                        list.add(RewardRedemptionEntity(
                            id = obj.optLong("id", 0L),
                            rewardId = obj.optLong("rewardId", 0L),
                            rewardTitle = obj.optString("rewardTitle", ""),
                            coinsSpent = obj.optInt("coinsSpent", 0),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        ))
                    }
                    database.gamificationDao().insertRedemptions(list)
                }

                // 21. Medications
                val medsArr = root.optJSONArray("medications")
                if (medsArr != null) {
                    val list = mutableListOf<MedicationEntity>()
                    for (i in 0 until medsArr.length()) {
                        val obj = medsArr.getJSONObject(i)
                        list.add(MedicationEntity(
                            id = obj.optLong("id", 0L),
                            profileId = obj.optLong("profileId", 1L),
                            taskId = obj.optLong("taskId", 0L),
                            name = obj.getString("name"),
                            form = obj.optString("form", "TABLET"),
                            dosageAmount = obj.optString("dosageAmount", "1"),
                            dosageUnit = obj.optString("dosageUnit", "PILL"),
                            strength = obj.optString("strength", ""),
                            instructions = obj.optString("instructions", ""),
                            scheduleType = obj.optString("scheduleType", "DAILY"),
                            mealPeriodsJson = obj.optString("mealPeriodsJson", "[]"),
                            fixedTimesJson = obj.optString("fixedTimesJson", "[]"),
                            intervalHours = obj.optInt("intervalHours", 4),
                            intervalModel = obj.optString("intervalModel", "FIXED_SCHEDULE"),
                            intervalAnchorTime = obj.optString("intervalAnchorTime", "08:00"),
                            allowOvernight = obj.optBoolean("allowOvernight", false),
                            selectedDaysOfWeek = obj.optString("selectedDaysOfWeek", ""),
                            startDateIso = obj.optString("startDateIso", ""),
                            endDateIso = if (obj.isNull("endDateIso")) null else obj.optString("endDateIso"),
                            courseDurationDays = if (obj.isNull("courseDurationDays")) null else obj.optInt("courseDurationDays"),
                            isAsNeeded = obj.optBoolean("isAsNeeded", false),
                            isPaused = obj.optBoolean("isPaused", false),
                            reminderSound = obj.optString("reminderSound", "CHIME"),
                            alertStyle = obj.optString("alertStyle", "STANDARD"),
                            snoozeMinutes = obj.optInt("snoozeMinutes", 10),
                            followUpEnabled = obj.optBoolean("followUpEnabled", true),
                            followUpIntervalMinutes = obj.optInt("followUpIntervalMinutes", 15),
                            maxFollowUps = obj.optInt("maxFollowUps", 2),
                            privacySafeNotification = obj.optBoolean("privacySafeNotification", true),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        ))
                    }
                    database.medicationDao().insertMedications(list)
                }

                // 22. Medication Doses
                val dosesArr = root.optJSONArray("medication_doses")
                if (dosesArr != null) {
                    val list = mutableListOf<MedicationDoseEntity>()
                    for (i in 0 until dosesArr.length()) {
                        val obj = dosesArr.getJSONObject(i)
                        list.add(MedicationDoseEntity(
                            id = obj.optLong("id", 0L),
                            medicationId = obj.getLong("medicationId"),
                            taskId = obj.optLong("taskId", 0L),
                            medicationName = obj.optString("medicationName", ""),
                            medicationForm = obj.optString("medicationForm", "TABLET"),
                            dosage = obj.optString("dosage", "1"),
                            instructions = obj.optString("instructions", ""),
                            scheduledDateIso = obj.optString("scheduledDateIso", ""),
                            scheduledTime = obj.optString("scheduledTime", "08:00"),
                            status = obj.optString("status", "SCHEDULED"),
                            actionTimestamp = if (obj.isNull("actionTimestamp")) null else obj.optLong("actionTimestamp"),
                            snoozeCount = obj.optInt("snoozeCount", 0),
                            followUpCount = obj.optInt("followUpCount", 0),
                            notes = obj.optString("notes", "")
                        ))
                    }
                    database.medicationDao().insertDoses(list)
                }

                // 23. Trackers
                val trackersArr = root.optJSONArray("trackers")
                if (trackersArr != null) {
                    val list = mutableListOf<TrackerEntity>()
                    for (i in 0 until trackersArr.length()) {
                        val obj = trackersArr.getJSONObject(i)
                        list.add(TrackerEntity(
                            id = obj.optLong("id", 0L),
                            title = obj.getString("title"),
                            emoji = obj.optString("emoji", "📦"),
                            description = obj.optString("description", ""),
                            projectId = if (obj.isNull("projectId")) null else obj.optLong("projectId"),
                            labelIdsJson = obj.optString("labelIdsJson", "[]"),
                            customColorHex = if (obj.isNull("customColorHex")) null else obj.optLong("customColorHex"),
                            trackDuration = obj.optBoolean("trackDuration", true),
                            trackDistance = obj.optBoolean("trackDistance", false),
                            trackQuantity = obj.optBoolean("trackQuantity", false),
                            distanceUnit = obj.optString("distanceUnit", "KILOMETERS"),
                            quantityUnit = obj.optString("quantityUnit", "PIECES"),
                            customQuantityUnitName = obj.optString("customQuantityUnitName", ""),
                            inventoryPolicy = obj.optString("inventoryPolicy", "CONSUMABLE"),
                            currentStock = obj.optDouble("currentStock", 0.0),
                            emptyStock = obj.optDouble("emptyStock", 0.0),
                            isReminderEnabled = obj.optBoolean("isReminderEnabled", false),
                            reminderDaysInterval = obj.optInt("reminderDaysInterval", 30),
                            isArchived = obj.optBoolean("isArchived", false),
                            profileId = obj.optLong("profileId", 1L),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        ))
                    }
                    database.trackerDao().insertTrackers(list)
                }

                // 24. Tracker Cycles
                val cyclesArr = root.optJSONArray("tracker_cycles")
                if (cyclesArr != null) {
                    val list = mutableListOf<UsageCycleEntity>()
                    for (i in 0 until cyclesArr.length()) {
                        val obj = cyclesArr.getJSONObject(i)
                        list.add(UsageCycleEntity(
                            id = obj.optLong("id", 0L),
                            trackerId = obj.getLong("trackerId"),
                            cycleName = obj.optString("cycleName", ""),
                            purchaseId = if (obj.isNull("purchaseId")) null else obj.optLong("purchaseId"),
                            purchaseCost = obj.optDouble("purchaseCost", 0.0),
                            startDateIso = obj.getString("startDateIso"),
                            finishDateIso = if (obj.isNull("finishDateIso")) null else obj.optString("finishDateIso"),
                            status = obj.optString("status", "ACTIVE"),
                            isPaused = obj.optBoolean("isPaused", false),
                            startOdometer = obj.optDouble("startOdometer", 0.0),
                            currentOdometer = obj.optDouble("currentOdometer", 0.0),
                            finishOdometer = if (obj.isNull("finishOdometer")) null else obj.optDouble("finishOdometer"),
                            initialQuantity = obj.optDouble("initialQuantity", 0.0),
                            remainingQuantity = obj.optDouble("remainingQuantity", 0.0),
                            notes = obj.optString("notes", ""),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        ))
                    }
                    database.trackerDao().insertCycles(list)
                }

                // 25. Tracker Usage Entries
                val entriesArr = root.optJSONArray("tracker_usage_entries")
                if (entriesArr != null) {
                    val list = mutableListOf<UsageEntryEntity>()
                    for (i in 0 until entriesArr.length()) {
                        val obj = entriesArr.getJSONObject(i)
                        list.add(UsageEntryEntity(
                            id = obj.optLong("id", 0L),
                            cycleId = obj.getLong("cycleId"),
                            dateIso = obj.getString("dateIso"),
                            odometerReading = obj.optDouble("odometerReading", 0.0),
                            quantityDeducted = obj.optDouble("quantityDeducted", 0.0),
                            manualRemainingQuantity = if (obj.isNull("manualRemainingQuantity")) null else obj.optDouble("manualRemainingQuantity"),
                            notes = obj.optString("notes", ""),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        ))
                    }
                    database.trackerDao().insertUsageEntries(list)
                }

                // 26. Tracker Purchases
                val purchasesArr = root.optJSONArray("tracker_purchases")
                if (purchasesArr != null) {
                    val list = mutableListOf<TrackerPurchaseEntity>()
                    for (i in 0 until purchasesArr.length()) {
                        val obj = purchasesArr.getJSONObject(i)
                        list.add(TrackerPurchaseEntity(
                            id = obj.optLong("id", 0L),
                            trackerId = obj.getLong("trackerId"),
                            purchaseDateIso = obj.getString("purchaseDateIso"),
                            quantity = obj.optDouble("quantity", 0.0),
                            unitPrice = obj.optDouble("unitPrice", 0.0),
                            totalCost = obj.optDouble("totalCost", 0.0),
                            notes = obj.optString("notes", ""),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        ))
                    }
                    database.trackerDao().insertPurchases(list)
                }

                // 27. Tracker Inventory Adjustments
                val adjustmentsArr = root.optJSONArray("tracker_inventory_adjustments")
                if (adjustmentsArr != null) {
                    val list = mutableListOf<InventoryAdjustmentEntity>()
                    for (i in 0 until adjustmentsArr.length()) {
                        val obj = adjustmentsArr.getJSONObject(i)
                        list.add(InventoryAdjustmentEntity(
                            id = obj.optLong("id", 0L),
                            trackerId = obj.getLong("trackerId"),
                            dateIso = obj.getString("dateIso"),
                            deltaQuantity = obj.optDouble("deltaQuantity", 0.0),
                            isRefillExchange = obj.optBoolean("isRefillExchange", false),
                            reason = obj.optString("reason", ""),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        ))
                    }
                    database.trackerDao().insertAdjustments(list)
                }

                // 28. Subscriptions
                val subsArr = root.optJSONArray("subscriptions")
                if (subsArr != null) {
                    val list = mutableListOf<SubscriptionEntity>()
                    for (i in 0 until subsArr.length()) {
                        val obj = subsArr.getJSONObject(i)
                        list.add(SubscriptionEntity(
                            id = obj.optLong("id", 0L),
                            title = obj.getString("title"),
                            emoji = obj.optString("emoji", "💳"),
                            description = obj.optString("description", ""),
                            projectId = if (obj.isNull("projectId")) null else obj.optLong("projectId"),
                            labelIdsJson = obj.optString("labelIdsJson", "[]"),
                            customColorHex = if (obj.isNull("customColorHex")) null else obj.optLong("customColorHex"),
                            amount = obj.optDouble("amount", 0.0),
                            currency = obj.optString("currency", "₹"),
                            frequency = obj.optString("frequency", "MONTHLY"),
                            customIntervalCount = obj.optInt("customIntervalCount", 1),
                            customIntervalUnit = obj.optString("customIntervalUnit", "MONTHS"),
                            anchorDateIso = obj.getString("anchorDateIso"),
                            anchorDayOfMonth = obj.optInt("anchorDayOfMonth", 1),
                            nextRenewalDateIso = obj.optString("nextRenewalDateIso", ""),
                            missingDatePolicy = obj.optString("missingDatePolicy", "LAST_VALID_DAY"),
                            autoRenew = obj.optBoolean("autoRenew", true),
                            paymentMethod = obj.optString("paymentMethod", ""),
                            isTrial = obj.optBoolean("isTrial", false),
                            trialStartDateIso = if (obj.isNull("trialStartDateIso")) null else obj.optString("trialStartDateIso"),
                            trialEndDateIso = if (obj.isNull("trialEndDateIso")) null else obj.optString("trialEndDateIso"),
                            status = obj.optString("status", "ACTIVE"),
                            cancellationDateIso = if (obj.isNull("cancellationDateIso")) null else obj.optString("cancellationDateIso"),
                            cancellationReason = obj.optString("cancellationReason", ""),
                            reminderDaysBefore = obj.optInt("reminderDaysBefore", 3),
                            overdueReminderEnabled = obj.optBoolean("overdueReminderEnabled", true),
                            notes = obj.optString("notes", ""),
                            profileId = obj.optLong("profileId", 1L),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        ))
                    }
                    database.subscriptionDao().insertSubscriptions(list)
                }

                // 29. Subscription Bills
                val billsArr = root.optJSONArray("subscription_bills")
                if (billsArr != null) {
                    val list = mutableListOf<BillingOccurrenceEntity>()
                    for (i in 0 until billsArr.length()) {
                        val obj = billsArr.getJSONObject(i)
                        list.add(BillingOccurrenceEntity(
                            id = obj.optLong("id", 0L),
                            subscriptionId = obj.getLong("subscriptionId"),
                            dueDateIso = obj.getString("dueDateIso"),
                            expectedAmount = obj.optDouble("expectedAmount", 0.0),
                            currency = obj.optString("currency", "₹"),
                            status = obj.optString("status", "UNPAID"),
                            notes = obj.optString("notes", ""),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        ))
                    }
                    database.subscriptionDao().insertBills(list)
                }

                // 30. Subscription Payments
                val paymentsArr = root.optJSONArray("subscription_payments")
                if (paymentsArr != null) {
                    val list = mutableListOf<SubscriptionPaymentEntity>()
                    for (i in 0 until paymentsArr.length()) {
                        val obj = paymentsArr.getJSONObject(i)
                        list.add(SubscriptionPaymentEntity(
                            id = obj.optLong("id", 0L),
                            billId = obj.getLong("billId"),
                            subscriptionId = obj.getLong("subscriptionId"),
                            amountPaid = obj.optDouble("amountPaid", 0.0),
                            paymentDateIso = obj.getString("paymentDateIso"),
                            paymentMethod = obj.optString("paymentMethod", ""),
                            transactionRef = obj.optString("transactionRef", ""),
                            notes = obj.optString("notes", ""),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        ))
                    }
                    database.subscriptionDao().insertPayments(list)
                }

                // 31. Subscription Price History
                val phArr = root.optJSONArray("subscription_price_history")
                if (phArr != null) {
                    val list = mutableListOf<PriceHistoryEntity>()
                    for (i in 0 until phArr.length()) {
                        val obj = phArr.getJSONObject(i)
                        list.add(PriceHistoryEntity(
                            id = obj.optLong("id", 0L),
                            subscriptionId = obj.getLong("subscriptionId"),
                            oldAmount = obj.optDouble("oldAmount", 0.0),
                            newAmount = obj.optDouble("newAmount", 0.0),
                            effectiveDateIso = obj.getString("effectiveDateIso"),
                            reason = obj.optString("reason", ""),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        ))
                    }
                    database.subscriptionDao().insertPriceHistories(list)
                }

                // 32. Project Workspace Items
                val wsItemsArr = root.optJSONArray("project_workspace_items")
                if (wsItemsArr != null) {
                    val list = mutableListOf<ProjectWorkspaceItemEntity>()
                    for (i in 0 until wsItemsArr.length()) {
                        val obj = wsItemsArr.getJSONObject(i)
                        list.add(ProjectWorkspaceItemEntity(
                            id = obj.optLong("id", 0L),
                            profileId = obj.optLong("profileId", 1L),
                            projectId = obj.getLong("projectId"),
                            title = obj.getString("title"),
                            status = obj.optString("status", "NOT_STARTED"),
                            progress = obj.optInt("progress", 0),
                            category = obj.optString("category", "Feature"),
                            version = obj.optString("version", "V1"),
                            dueDateIso = if (obj.has("dueDateIso") && !obj.isNull("dueDateIso")) obj.getString("dueDateIso") else null,
                            notes = obj.optString("notes", ""),
                            priority = obj.optString("priority", "P2"),
                            customPropertiesJson = obj.optString("customPropertiesJson", "{}"),
                            orderIndex = obj.optInt("orderIndex", 0),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        ))
                    }
                    database.projectWorkspaceDao().insertItems(list)
                }

                // 33. Project Workspace Views
                val wsViewsArr = root.optJSONArray("project_workspace_views")
                if (wsViewsArr != null) {
                    val list = mutableListOf<ProjectWorkspaceViewEntity>()
                    for (i in 0 until wsViewsArr.length()) {
                        val obj = wsViewsArr.getJSONObject(i)
                        list.add(ProjectWorkspaceViewEntity(
                            id = obj.optLong("id", 0L),
                            profileId = obj.optLong("profileId", 1L),
                            projectId = obj.getLong("projectId"),
                            name = obj.getString("name"),
                            viewType = obj.optString("viewType", "TABLE"),
                            filterStatus = if (obj.has("filterStatus") && !obj.isNull("filterStatus")) obj.getString("filterStatus") else null,
                            filterCategory = if (obj.has("filterCategory") && !obj.isNull("filterCategory")) obj.getString("filterCategory") else null,
                            filterVersion = if (obj.has("filterVersion") && !obj.isNull("filterVersion")) obj.getString("filterVersion") else null,
                            sortBy = obj.optString("sortBy", "orderIndex"),
                            sortAscending = obj.optBoolean("sortAscending", true),
                            groupBy = obj.optString("groupBy", "status"),
                            visiblePropertiesJson = obj.optString("visiblePropertiesJson", "[\"status\",\"progress\",\"category\",\"version\",\"dueDate\"]"),
                            isDefault = obj.optBoolean("isDefault", false),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        ))
                    }
                    database.projectWorkspaceDao().insertViews(list)
                }

                // 34. Project Custom Statuses
                val customStatusesArr = root.optJSONArray("project_custom_statuses")
                if (customStatusesArr != null) {
                    val list = mutableListOf<ProjectCustomStatusEntity>()
                    for (i in 0 until customStatusesArr.length()) {
                        val obj = customStatusesArr.getJSONObject(i)
                        list.add(ProjectCustomStatusEntity(
                            id = obj.optLong("id", 0L),
                            profileId = obj.optLong("profileId", 1L),
                            projectId = obj.getLong("projectId"),
                            name = obj.getString("name"),
                            colorHex = obj.optLong("colorHex", 4282168054L),
                            orderIndex = obj.optInt("orderIndex", 0)
                        ))
                    }
                    database.projectWorkspaceDao().insertCustomStatuses(list)
                }

                // 35. Project Custom Properties
                val customPropsArr = root.optJSONArray("project_custom_properties")
                if (customPropsArr != null) {
                    val list = mutableListOf<ProjectCustomPropertyEntity>()
                    for (i in 0 until customPropsArr.length()) {
                        val obj = customPropsArr.getJSONObject(i)
                        list.add(ProjectCustomPropertyEntity(
                            id = obj.optLong("id", 0L),
                            profileId = obj.optLong("profileId", 1L),
                            projectId = obj.getLong("projectId"),
                            name = obj.optString("name", "Field"),
                            propertyType = obj.optString("propertyType", "TEXT"),
                            optionsJson = obj.optString("optionsJson", "[]"),
                            isVisible = obj.optBoolean("isVisible", true),
                            orderIndex = obj.optInt("orderIndex", 0)
                        ))
                    }
                    database.projectWorkspaceDao().insertCustomProperties(list)
                }

                // 36. Glossary Packs
                val glossaryPacksArr = root.optJSONArray("glossary_packs")
                if (glossaryPacksArr != null) {
                    val list = mutableListOf<GlossaryPackEntity>()
                    for (i in 0 until glossaryPacksArr.length()) {
                        val obj = glossaryPacksArr.getJSONObject(i)
                        list.add(GlossaryPackEntity(
                            id = obj.getString("id"),
                            title = obj.getString("title"),
                            description = obj.optString("description", ""),
                            category = obj.optString("category", "General"),
                            version = obj.optInt("version", 1),
                            author = obj.optString("author", "LifeOS Community"),
                            itemCount = obj.optInt("itemCount", 0),
                            sha256 = obj.optString("sha256", ""),
                            isInstalled = obj.optBoolean("isInstalled", true),
                            installedAt = obj.optLong("installedAt", System.currentTimeMillis()),
                            sizeBytes = obj.optLong("sizeBytes", 0L)
                        ))
                    }
                    database.glossaryDao().insertPacks(list)
                }

                // 37. Glossary Items
                val glossaryItemsArr = root.optJSONArray("glossary_items")
                if (glossaryItemsArr != null) {
                    val list = mutableListOf<GlossaryItemEntity>()
                    for (i in 0 until glossaryItemsArr.length()) {
                        val obj = glossaryItemsArr.getJSONObject(i)
                        list.add(GlossaryItemEntity(
                            id = obj.optLong("id", 0L),
                            packId = obj.getString("packId"),
                            term = obj.getString("term"),
                            phonetic = if (obj.has("phonetic") && !obj.isNull("phonetic")) obj.getString("phonetic") else null,
                            category = obj.getString("category"),
                            definition = obj.getString("definition"),
                            detailedExplanation = obj.optString("detailedExplanation", ""),
                            examplesJson = obj.optString("examplesJson", "[]"),
                            relatedTermsJson = obj.optString("relatedTermsJson", "[]"),
                            tagsJson = obj.optString("tagsJson", "[]"),
                            isBookmarked = obj.optBoolean("isBookmarked", false),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        ))
                    }
                    database.glossaryDao().insertItems(list)
                }

                // 38. Glossary User Data
                val glossaryUserDataArr = root.optJSONArray("glossary_user_data")
                if (glossaryUserDataArr != null) {
                    val list = mutableListOf<GlossaryUserDataEntity>()
                    for (i in 0 until glossaryUserDataArr.length()) {
                        val obj = glossaryUserDataArr.getJSONObject(i)
                        list.add(GlossaryUserDataEntity(
                            id = obj.optLong("id", 0L),
                            profileId = obj.optLong("profileId", 1L),
                            termId = obj.getLong("termId"),
                            isBookmarked = obj.optBoolean("isBookmarked", false),
                            personalNote = obj.optString("personalNote", ""),
                            lastViewedAt = obj.optLong("lastViewedAt", 0L),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        ))
                    }
                    database.glossaryDao().insertUserDataList(list)
                }

                // 39. MCP Servers
                val mcpServersArr = root.optJSONArray("mcp_servers")
                if (mcpServersArr != null) {
                    val list = mutableListOf<McpServerEntity>()
                    for (i in 0 until mcpServersArr.length()) {
                        val obj = mcpServersArr.getJSONObject(i)
                        list.add(McpServerEntity(
                            id = obj.optLong("id", 0L),
                            profileId = obj.optLong("profileId", 1L),
                            name = obj.getString("name"),
                            endpoint = obj.getString("endpoint"),
                            transport = obj.optString("transport", "HTTP_SSE"),
                            authToken = if (obj.has("authToken") && !obj.isNull("authToken")) obj.getString("authToken") else null,
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        ))
                    }
                    database.mcpDao().insertServers(list)
                }

                // 40. MCP Security Audit Logs
                val auditLogsArr = root.optJSONArray("mcp_audit_logs")
                if (auditLogsArr != null) {
                    val list = mutableListOf<McpAuditLogEntity>()
                    for (i in 0 until auditLogsArr.length()) {
                        val obj = auditLogsArr.getJSONObject(i)
                        list.add(McpAuditLogEntity(
                            id = obj.optLong("id", 0L),
                            profileId = obj.optLong("profileId", 1L),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            direction = obj.optString("direction", "OUTBOUND"),
                            eventType = obj.optString("eventType", "TOOL_CALL"),
                            toolOrResource = obj.optString("toolOrResource", ""),
                            details = obj.optString("details", ""),
                            status = obj.optString("status", "SUCCESS")
                        ))
                    }
                    database.mcpDao().insertAuditLogs(list)
                }
            }

            return@withContext RestoreResult.Success(summary.totalRecords, summary)
        } catch (e: Exception) {
            return@withContext RestoreResult.Error("Restore transaction failed: ${e.message}", e)
        }
    }

    /**
     * Legacy import function maintained for backward-compatibility with existing tests and callers.
     */
    suspend fun importData(database: AppDatabase, jsonStr: String): Boolean = withContext(Dispatchers.IO) {
        val result = restoreData(database, jsonStr, null)
        result is RestoreResult.Success
    }

    suspend fun importFromStream(
        database: AppDatabase,
        inputStream: InputStream,
        context: Context? = null
    ): RestoreResult = withContext(Dispatchers.IO) {
        val jsonStr = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        restoreData(database, jsonStr, context)
    }

    suspend fun validateFromStream(inputStream: InputStream): ValidationResult = withContext(Dispatchers.IO) {
        val jsonStr = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        validateBackup(jsonStr)
    }
}
