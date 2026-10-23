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

        return@withContext root.toString(2)
    }

    suspend fun importData(database: AppDatabase, jsonStr: String): Boolean = withContext(Dispatchers.IO) {
        if (jsonStr.isBlank() || jsonStr.length > 50_000_000) return@withContext false
        try {
            val root = JSONObject(jsonStr)

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
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                    database.honeycombShortcutDao().insertShortcut(sc)
                }
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
