package com.example.data.backup

import com.example.data.local.AppDatabase
import com.example.data.local.entities.AchievementEntity
import com.example.data.local.entities.ChallengeEntity
import com.example.data.local.entities.GamificationProfileEntity
import com.example.data.local.entities.HabitEntity
import com.example.data.local.entities.HoneycombShortcutEntity
import com.example.data.local.entities.LabelEntity
import com.example.data.local.entities.PlanEntity
import com.example.data.local.entities.PlanMilestoneEntity
import com.example.data.local.entities.PlanTaskCrossRef
import com.example.data.local.entities.ProjectEntity
import com.example.data.local.entities.RewardItemEntity
import com.example.data.local.entities.TaskEntity
import com.example.data.local.entities.TaskLabelCrossRef
import com.example.data.local.entities.TaskReminderEntity
import com.example.data.local.entities.TrashItemEntity
import com.example.data.local.entities.TrackerEntity
import com.example.data.local.entities.UsageCycleEntity
import com.example.data.local.entities.UsageEntryEntity
import com.example.data.local.entities.TrackerPurchaseEntity
import com.example.data.local.entities.InventoryAdjustmentEntity
import com.example.data.local.entities.SubscriptionEntity
import com.example.data.local.entities.BillingOccurrenceEntity
import com.example.data.local.entities.SubscriptionPaymentEntity
import com.example.data.local.entities.PriceHistoryEntity
import com.example.data.local.entities.WaterLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DataExportImportManager {

    private const val BACKUP_VERSION = 1

    suspend fun exportAllData(database: AppDatabase): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", BACKUP_VERSION)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("exportDateReadable", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))

        // 0. Profiles
        val profiles = database.profileDao().getAllProfiles()
        val profsArr = JSONArray()
        profiles.forEach { p ->
            val obj = JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("emoji", p.emoji)
                put("colorHex", p.colorHex)
                put("type", p.type)
                put("orderIndex", p.orderIndex)
                put("isDefault", p.isDefault)
                put("createdAt", p.createdAt)
                put("updatedAt", p.updatedAt)
            }
            profsArr.put(obj)
        }
        root.put("profiles", profsArr)

        // 0b. Profile Surveys
        val surveys = database.profileSurveyDao().getAllSurveys()
        val surveysArr = JSONArray()
        surveys.forEach { s ->
            val obj = JSONObject().apply {
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
            }
            surveysArr.put(obj)
        }
        root.put("profile_surveys", surveysArr)

        // 1. Tasks
        val tasks = database.taskDao().getAllTasksList()
        val tasksArr = JSONArray()
        tasks.forEach { t ->
            val obj = JSONObject().apply {
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
            }
            tasksArr.put(obj)
        }
        root.put("tasks", tasksArr)

        // 2. Projects
        val projects = database.projectDao().getAllProjectsList()
        val projArr = JSONArray()
        projects.forEach { p ->
            val obj = JSONObject().apply {
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
            }
            projArr.put(obj)
        }
        root.put("projects", projArr)

        // 3. Labels
        val labels = database.labelDao().getAllLabelsList()
        val labelsArr = JSONArray()
        labels.forEach { l ->
            val obj = JSONObject().apply {
                put("id", l.id)
                put("name", l.name)
                put("colorId", l.colorId)
                put("isFavorite", l.isFavorite)
                put("isArchived", l.isArchived)
                put("orderIndex", l.orderIndex)
                put("profileId", l.profileId)
                put("createdAt", l.createdAt)
            }
            labelsArr.put(obj)
        }
        root.put("labels", labelsArr)

        // 4. Habits
        val habits = database.habitDao().getAllHabitsList()
        val habitsArr = JSONArray()
        habits.forEach { h ->
            val obj = JSONObject().apply {
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
            }
            habitsArr.put(obj)
        }
        root.put("habits", habitsArr)

        // 5. Honeycomb Shortcuts
        val shortcuts = database.honeycombShortcutDao().getAllShortcutsList()
        val scArr = JSONArray()
        shortcuts.forEach { sc ->
            val obj = JSONObject().apply {
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
            }
            scArr.put(obj)
        }
        root.put("honeycomb_shortcuts", scArr)

        // 6. Plans
        val plans = database.planDao().getAllPlans()
        // 7. Gamification Profile
        val profile = database.gamificationDao().getProfile()
        if (profile != null) {
            val profObj = JSONObject().apply {
                put("currentXp", profile.currentXp)
                put("currentCoins", profile.currentCoins)
                put("currentLevel", profile.currentLevel)
                put("hunterRank", profile.hunterRank)
            }
            root.put("gamification_profile", profObj)
        }

        // 8. Water Logs
        val water = database.waterLogDao().getRecentWaterLogs()

        // 9. Smart Trackers
        val trackers = database.trackerDao().getAllTrackersList()
        val trackersArr = JSONArray()
        trackers.forEach { tr ->
            val obj = JSONObject().apply {
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
            }
            trackersArr.put(obj)
        }
        root.put("trackers", trackersArr)

        val cycles = database.trackerDao().getAllCyclesList()
        val cyclesArr = JSONArray()
        cycles.forEach { c ->
            val obj = JSONObject().apply {
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
            }
            cyclesArr.put(obj)
        }
        root.put("tracker_cycles", cyclesArr)

        val purchases = database.trackerDao().getAllPurchasesList()
        val purchasesArr = JSONArray()
        purchases.forEach { p ->
            val obj = JSONObject().apply {
                put("id", p.id)
                put("trackerId", p.trackerId)
                put("purchaseDateIso", p.purchaseDateIso)
                put("quantity", p.quantity)
                put("unitPrice", p.unitPrice)
                put("totalCost", p.totalCost)
                put("notes", p.notes)
                put("createdAt", p.createdAt)
            }
            purchasesArr.put(obj)
        }
        root.put("tracker_purchases", purchasesArr)

        // 10. Subscriptions
        val subs = database.subscriptionDao().getAllSubscriptionsList()
        val subsArr = JSONArray()
        subs.forEach { s ->
            val obj = JSONObject().apply {
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
            }
            subsArr.put(obj)
        }
        root.put("subscriptions", subsArr)

        val bills = database.subscriptionDao().getAllBillsList()
        val billsArr = JSONArray()
        bills.forEach { b ->
            val obj = JSONObject().apply {
                put("id", b.id)
                put("subscriptionId", b.subscriptionId)
                put("dueDateIso", b.dueDateIso)
                put("expectedAmount", b.expectedAmount)
                put("currency", b.currency)
                put("status", b.status)
                put("notes", b.notes)
                put("createdAt", b.createdAt)
            }
            billsArr.put(obj)
        }
        root.put("subscription_bills", billsArr)

        val payments = database.subscriptionDao().getAllPaymentsList()
        val paymentsArr = JSONArray()
        payments.forEach { pay ->
            val obj = JSONObject().apply {
                put("id", pay.id)
                put("billId", pay.billId)
                put("subscriptionId", pay.subscriptionId)
                put("amountPaid", pay.amountPaid)
                put("paymentDateIso", pay.paymentDateIso)
                put("paymentMethod", pay.paymentMethod)
                put("transactionRef", pay.transactionRef)
                put("notes", pay.notes)
                put("timestamp", pay.timestamp)
            }
            paymentsArr.put(obj)
        }
        root.put("subscription_payments", paymentsArr)

        return@withContext root.toString(2)
    }

    suspend fun importData(database: AppDatabase, jsonStr: String): Boolean = withContext(Dispatchers.IO) {
        if (jsonStr.isBlank() || jsonStr.length > 50_000_000) return@withContext false
        try {
            val root = JSONObject(jsonStr)

            // Import Profiles
            val profsArr = root.optJSONArray("profiles")
            if (profsArr != null) {
                for (i in 0 until profsArr.length()) {
                    val obj = profsArr.getJSONObject(i)
                    val prof = com.example.data.local.entities.ProfileEntity(
                        id = obj.optLong("id", 0L),
                        name = obj.getString("name"),
                        emoji = obj.optString("emoji", "📁"),
                        colorHex = obj.optLong("colorHex", 0xFF3B82F6),
                        type = obj.optString("type", com.example.data.local.entities.ProfileEntity.TYPE_CUSTOM),
                        orderIndex = obj.optInt("orderIndex", 0),
                        isDefault = obj.optBoolean("isDefault", false),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                    database.profileDao().insertProfile(prof)
                }
            }

            // Import Profile Surveys
            val surveysArr = root.optJSONArray("profile_surveys")
            if (surveysArr != null) {
                for (i in 0 until surveysArr.length()) {
                    val obj = surveysArr.getJSONObject(i)
                    val survey = com.example.data.local.entities.ProfileSurveyEntity(
                        profileId = obj.getLong("profileId"),
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
                    )
                    database.profileSurveyDao().insertOrUpdateSurvey(survey)
                }
            }

            // Import Tasks
            val tasksArr = root.optJSONArray("tasks")
            if (tasksArr != null) {
                for (i in 0 until tasksArr.length()) {
                    val obj = tasksArr.getJSONObject(i)
                    val task = TaskEntity(
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
                    )
                    database.taskDao().insertTask(task)
                }
            }

            // Import Projects
            val projArr = root.optJSONArray("projects")
            if (projArr != null) {
                for (i in 0 until projArr.length()) {
                    val obj = projArr.getJSONObject(i)
                    val proj = ProjectEntity(
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
                    )
                    database.projectDao().insertProject(proj)
                }
            }

            // Import Labels
            val labelsArr = root.optJSONArray("labels")
            if (labelsArr != null) {
                for (i in 0 until labelsArr.length()) {
                    val obj = labelsArr.getJSONObject(i)
                    val label = LabelEntity(
                        id = obj.optLong("id", 0L),
                        name = obj.getString("name"),
                        colorId = obj.optString("colorId", "charcoal"),
                        isFavorite = obj.optBoolean("isFavorite", false),
                        isArchived = obj.optBoolean("isArchived", false),
                        orderIndex = obj.optInt("orderIndex", 0),
                        profileId = obj.optLong("profileId", 1L),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                    database.labelDao().insertLabel(label)
                }
            }

            // Import Habits
            val habitsArr = root.optJSONArray("habits")
            if (habitsArr != null) {
                for (i in 0 until habitsArr.length()) {
                    val obj = habitsArr.getJSONObject(i)
                    val habit = HabitEntity(
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
                    )
                    database.habitDao().insertHabit(habit)
                }
            }

            // Import Gamification
            val profObj = root.optJSONObject("gamification_profile")
            if (profObj != null) {
                database.gamificationDao().insertOrUpdateProfile(
                    GamificationProfileEntity(
                        id = 1,
                        currentXp = profObj.optLong("currentXp", 0L),
                        currentCoins = profObj.optInt("currentCoins", 0),
                        currentLevel = profObj.optInt("currentLevel", 1),
                        hunterRank = profObj.optString("hunterRank", "E")
                    )
                )
            }

            // Import Honeycomb Shortcuts
            val scArr = root.optJSONArray("honeycomb_shortcuts")
            if (scArr != null) {
                database.honeycombShortcutDao().clearAllShortcuts()
                for (i in 0 until scArr.length()) {
                    val obj = scArr.getJSONObject(i)
                    val sc = HoneycombShortcutEntity(
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
                    )
                    database.honeycombShortcutDao().insertShortcut(sc)
                }
            }

            // Import Trackers
            val trackersArr = root.optJSONArray("trackers")
            if (trackersArr != null) {
                for (i in 0 until trackersArr.length()) {
                    val obj = trackersArr.getJSONObject(i)
                    val tracker = TrackerEntity(
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
                    )
                    database.trackerDao().insertTracker(tracker)
                }
            }

            // Import Usage Cycles
            val cyclesArr = root.optJSONArray("tracker_cycles")
            if (cyclesArr != null) {
                for (i in 0 until cyclesArr.length()) {
                    val obj = cyclesArr.getJSONObject(i)
                    val cycle = UsageCycleEntity(
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
                    )
                    database.trackerDao().insertCycle(cycle)
                }
            }

            // Import Purchases
            val purchasesArr = root.optJSONArray("tracker_purchases")
            if (purchasesArr != null) {
                for (i in 0 until purchasesArr.length()) {
                    val obj = purchasesArr.getJSONObject(i)
                    val purchase = TrackerPurchaseEntity(
                        id = obj.optLong("id", 0L),
                        trackerId = obj.getLong("trackerId"),
                        purchaseDateIso = obj.getString("purchaseDateIso"),
                        quantity = obj.optDouble("quantity", 0.0),
                        unitPrice = obj.optDouble("unitPrice", 0.0),
                        totalCost = obj.optDouble("totalCost", 0.0),
                        notes = obj.optString("notes", ""),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                    database.trackerDao().insertPurchase(purchase)
                }
            }

            // Import Subscriptions
            val subsArr = root.optJSONArray("subscriptions")
            if (subsArr != null) {
                for (i in 0 until subsArr.length()) {
                    val obj = subsArr.getJSONObject(i)
                    val sub = SubscriptionEntity(
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
                    )
                    database.subscriptionDao().insertSubscription(sub)
                }
            }

            // Import Bills
            val billsArr = root.optJSONArray("subscription_bills")
            if (billsArr != null) {
                for (i in 0 until billsArr.length()) {
                    val obj = billsArr.getJSONObject(i)
                    val bill = BillingOccurrenceEntity(
                        id = obj.optLong("id", 0L),
                        subscriptionId = obj.getLong("subscriptionId"),
                        dueDateIso = obj.getString("dueDateIso"),
                        expectedAmount = obj.optDouble("expectedAmount", 0.0),
                        currency = obj.optString("currency", "₹"),
                        status = obj.optString("status", "UNPAID"),
                        notes = obj.optString("notes", ""),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                    database.subscriptionDao().insertBill(bill)
                }
            }

            // Import Payments
            val paymentsArr = root.optJSONArray("subscription_payments")
            if (paymentsArr != null) {
                for (i in 0 until paymentsArr.length()) {
                    val obj = paymentsArr.getJSONObject(i)
                    val payment = SubscriptionPaymentEntity(
                        id = obj.optLong("id", 0L),
                        billId = obj.getLong("billId"),
                        subscriptionId = obj.getLong("subscriptionId"),
                        amountPaid = obj.optDouble("amountPaid", 0.0),
                        paymentDateIso = obj.getString("paymentDateIso"),
                        paymentMethod = obj.optString("paymentMethod", ""),
                        transactionRef = obj.optString("transactionRef", ""),
                        notes = obj.optString("notes", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                    database.subscriptionDao().insertPayment(payment)
                }
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
