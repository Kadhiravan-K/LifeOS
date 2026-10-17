package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entities.ChallengeEntity
import com.example.data.local.entities.HabitEntity
import com.example.data.local.entities.HoneycombShortcutEntity
import com.example.data.local.entities.TaskEntity
import com.example.data.local.entities.WaterLogEntity
import com.example.data.preferences.UserPreferences
import com.example.domain.model.ChallengeDailyLog
import com.example.domain.model.ChallengePreset
import com.example.domain.model.ChallengeRule
import com.example.domain.model.ChallengeState
import com.example.domain.model.ChallengeStatus
import com.example.domain.model.DefaultChallengeRules
import com.example.domain.model.DefaultHabits
import com.example.domain.model.DefaultHoneycombShortcuts
import com.example.domain.model.HabitItem
import com.example.domain.model.HoneycombShortcut
import com.example.domain.model.RecurrenceType
import com.example.domain.model.ShortcutType
import com.example.domain.model.Subtask
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskPriority
import com.example.domain.model.SchedulingMode
import com.example.domain.model.DurationUnit
import com.example.domain.recurrence.RecurrenceEngine
import com.example.utils.DateUtils
import com.example.data.local.entities.LabelEntity
import com.example.data.local.entities.ProjectEntity
import com.example.data.local.entities.TaskLabelCrossRef
import com.example.data.local.entities.TaskReminderEntity
import com.example.domain.model.LabelItem
import com.example.domain.model.ProjectItem
import com.example.domain.model.TaskReminderItem
import com.example.domain.model.PlanItem
import com.example.domain.model.PlanLevel
import com.example.domain.model.PlanMilestone
import com.example.domain.model.PlanStatus
import com.example.domain.model.TrashItem
import com.example.domain.model.GamificationProfile
import com.example.domain.model.HunterRank
import com.example.domain.model.AchievementItem
import com.example.domain.model.RewardItem
import com.example.domain.model.DefaultAchievements
import com.example.domain.model.DefaultRewards
import com.example.domain.model.HoneycombLayoutTemplate
import com.example.data.local.entities.PlanEntity
import com.example.data.local.entities.PlanMilestoneEntity
import com.example.data.local.entities.PlanTaskCrossRef
import com.example.data.local.entities.TrashItemEntity
import com.example.data.local.entities.GamificationProfileEntity
import com.example.data.local.entities.XpEventEntity
import com.example.data.local.entities.AchievementEntity
import com.example.data.local.entities.RewardItemEntity
import com.example.data.local.entities.RewardRedemptionEntity
import org.json.JSONObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class LifeRepository(
    private val database: AppDatabase,
    private val preferences: UserPreferences
) {
    private val taskDao = database.taskDao()
    private val habitDao = database.habitDao()
    private val challengeDao = database.challengeDao()
    private val waterLogDao = database.waterLogDao()
    private val honeycombShortcutDao = database.honeycombShortcutDao()
    private val projectDao = database.projectDao()
    private val labelDao = database.labelDao()
    private val taskReminderDao = database.taskReminderDao()
    private val trashDao = database.trashDao()
    private val planDao = database.planDao()
    private val gamificationDao = database.gamificationDao()

    // --- Phase 3A Reactive Flows ---
    val allProjectsFlow: Flow<List<ProjectItem>> = combine(
        projectDao.getActiveProjectsFlow(),
        taskDao.getAllTasks()
    ) { projects, tasks ->
        val counts = tasks.groupBy { it.projectId }
        projects.map { entity ->
            val projectTasks = counts[entity.id] ?: emptyList()
            val total = projectTasks.size
            val completed = projectTasks.count { it.isOneTimeCompleted }
            entity.toDomainModel(total, completed)
        }
    }

    val archivedProjectsFlow: Flow<List<ProjectItem>> = projectDao.getArchivedProjectsFlow().map { list ->
        list.map { it.toDomainModel(0, 0) }
    }

    val allLabelsFlow: Flow<List<LabelItem>> = combine(
        labelDao.getActiveLabelsFlow(),
        labelDao.getAllCrossRefsFlow()
    ) { labels, crossRefs ->
        val counts = crossRefs.groupingBy { it.labelId }.eachCount()
        labels.map { it.toDomainModel(counts[it.id] ?: 0) }
    }

    val archivedLabelsFlow: Flow<List<LabelItem>> = labelDao.getAllLabelsFlow().map { list ->
        list.filter { it.isArchived }.map { it.toDomainModel(0) }
    }

    // --- Unified Tasks Flow (enriched with Project, Labels, and Reminders) ---
    val allTasksFlow: Flow<List<TaskItem>> = combine(
        taskDao.getAllTasks(),
        projectDao.getAllProjectsFlow(),
        labelDao.getAllLabelsFlow(),
        labelDao.getAllCrossRefsFlow(),
        taskReminderDao.getAllActiveRemindersFlow()
    ) { tasks, projects, labels, crossRefs, reminders ->
        val projectMap = projects.associateBy { it.id }
        val labelMap = labels.associateBy { it.id }
        val labelsByTask = crossRefs.groupBy({ it.taskId }, { it.labelId })
        val remindersByTask = reminders.groupBy { it.taskId }

        tasks.map { entity ->
            val project = entity.projectId?.let { projectMap[it] }
            val taskLabels = (labelsByTask[entity.id] ?: emptyList()).mapNotNull { labelMap[it]?.toDomainModel() }
            val taskReminders = (remindersByTask[entity.id] ?: emptyList()).map { it.toDomainModel() }
            entity.toDomainModel(project, taskLabels, taskReminders)
        }
    }

    /**
     * Returns scheduled active tasks for a given date, with accurate completion status for that date.
     * Excludes archived tasks by default.
     */
    fun getTasksForDateFlow(targetDateIso: String, includeArchived: Boolean = false): Flow<List<TaskItem>> {
        return allTasksFlow.map { allTasks ->
            allTasks
                .filter { task -> (includeArchived || !task.isArchived) && RecurrenceEngine.isTaskScheduledOnDate(task, targetDateIso) }
                .map { task ->
                    val isCompleted = RecurrenceEngine.isCompletedOnDate(task, targetDateIso)
                    task.copy(isCompletedForDate = isCompleted)
                }
        }
    }

    suspend fun insertTask(task: TaskItem): Long {
        val taskId = taskDao.insertTask(task.toEntity())
        saveTaskLabels(taskId, task.labels)
        saveTaskReminders(taskId, task.reminders)
        return taskId
    }

    suspend fun updateTask(task: TaskItem) {
        taskDao.updateTask(task.toEntity())
        saveTaskLabels(task.id, task.labels)
        saveTaskReminders(task.id, task.reminders)
    }

    suspend fun deleteTask(taskId: Long) {
        labelDao.clearLabelsForTask(taskId)
        taskReminderDao.deleteRemindersForTask(taskId)
        taskDao.deleteTaskById(taskId)
    }

    suspend fun getTaskById(taskId: Long): TaskItem? {
        val entity = taskDao.getTaskById(taskId) ?: return null
        val project = entity.projectId?.let { projectDao.getProjectById(it) }
        val labelEntities = labelDao.getLabelsForTaskList(taskId)
        val reminderEntities = taskReminderDao.getRemindersForTaskList(taskId)
        return entity.toDomainModel(project, labelEntities.map { it.toDomainModel() }, reminderEntities.map { it.toDomainModel() })
    }

    suspend fun saveTaskLabels(taskId: Long, labels: List<LabelItem>) {
        labelDao.clearLabelsForTask(taskId)
        val refs = labels.map { TaskLabelCrossRef(taskId = taskId, labelId = it.id) }
        labelDao.insertTaskLabelCrossRefs(refs)
    }

    suspend fun saveTaskReminders(taskId: Long, reminders: List<TaskReminderItem>) {
        taskReminderDao.deleteRemindersForTask(taskId)
        val entities = reminders.map { it.toEntity(taskId) }
        taskReminderDao.insertReminders(entities)
    }

    // --- Phase 3A Project Operations ---
    suspend fun insertProject(project: ProjectItem): Long {
        return projectDao.insertProject(project.toEntity())
    }

    suspend fun updateProject(project: ProjectItem) {
        projectDao.updateProject(project.toEntity())
    }

    suspend fun setProjectArchived(projectId: Long, isArchived: Boolean) {
        projectDao.setProjectArchived(projectId, isArchived)
    }

    suspend fun setProjectFavorite(projectId: Long, isFavorite: Boolean) {
        projectDao.setProjectFavorite(projectId, isFavorite)
    }

    suspend fun deleteProject(projectId: Long) {
        // Safe deletion: move associated tasks to Inbox (projectId = null)
        projectDao.unassignTasksFromProject(projectId)
        projectDao.deleteProjectById(projectId)
    }

    // --- Phase 3A Label Operations ---
    suspend fun insertLabel(label: LabelItem): Long {
        return labelDao.insertLabel(label.toEntity())
    }

    suspend fun updateLabel(label: LabelItem) {
        labelDao.updateLabel(label.toEntity())
    }

    suspend fun setLabelFavorite(labelId: Long, isFavorite: Boolean) {
        labelDao.setLabelFavorite(labelId, isFavorite)
    }

    suspend fun deleteLabel(labelId: Long) {
        // Safe deletion: remove task associations, preserve tasks
        labelDao.clearTaskRefsForLabel(labelId)
        labelDao.deleteLabelById(labelId)
    }

    // --- Seed Default Projects and Labels if empty ---
    suspend fun seedDefaultProjectsAndLabels() {
        val existingProjects = projectDao.getAllProjectsList()
        if (existingProjects.isEmpty()) {
            val defaults = listOf(
                ProjectItem(name = "Work & Career", emoji = "💼", colorId = "blue", orderIndex = 0),
                ProjectItem(name = "Personal", emoji = "👤", colorId = "green", orderIndex = 1),
                ProjectItem(name = "Health & Fitness", emoji = "🏃", colorId = "orange", orderIndex = 2)
            )
            projectDao.insertProjects(defaults.map { it.toEntity() })
        }

        val existingLabels = labelDao.getAllLabelsList()
        if (existingLabels.isEmpty()) {
            val defaults = listOf(
                LabelItem(name = "important", colorId = "red", orderIndex = 0, isFavorite = true),
                LabelItem(name = "learning", colorId = "blue", orderIndex = 1),
                LabelItem(name = "routine", colorId = "mint_green", orderIndex = 2)
            )
            labelDao.insertLabels(defaults.map { it.toEntity() })
        }
    }

    /**
     * Toggles completion for a task on a specific date.
     * Completing today does NOT complete future occurrences of recurring tasks.
     */
    suspend fun toggleTaskCompletion(taskId: Long, dateIso: String): Boolean? {
        val entity = taskDao.getTaskById(taskId) ?: return null
        val currentCompletedDates = JsonMappers.parseStringList(entity.completedDatesJson).toMutableSet()
        val recurrenceType = RecurrenceType.fromString(entity.recurrenceType)

        val newOneTimeCompleted: Boolean
        val isNowCompleted: Boolean
        if (recurrenceType == RecurrenceType.NONE) {
            if (entity.isOneTimeCompleted) {
                newOneTimeCompleted = false
                isNowCompleted = false
                currentCompletedDates.remove(dateIso)
            } else {
                newOneTimeCompleted = true
                isNowCompleted = true
                currentCompletedDates.add(dateIso)
            }
        } else {
            newOneTimeCompleted = false
            if (currentCompletedDates.contains(dateIso)) {
                isNowCompleted = false
                currentCompletedDates.remove(dateIso)
            } else {
                isNowCompleted = true
                currentCompletedDates.add(dateIso)
            }
        }

        val updatedEntity = entity.copy(
            isOneTimeCompleted = newOneTimeCompleted,
            completedDatesJson = JsonMappers.serializeStringList(currentCompletedDates)
        )
        taskDao.updateTask(updatedEntity)
        return isNowCompleted
    }

    suspend fun toggleSubtask(taskId: Long, subtaskId: String) {
        val entity = taskDao.getTaskById(taskId) ?: return
        val subtasks = JsonMappers.parseSubtasks(entity.subtasksJson).map {
            if (it.id == subtaskId) it.copy(isCompleted = !it.isCompleted) else it
        }
        val updated = entity.copy(subtasksJson = JsonMappers.serializeSubtasks(subtasks))
        taskDao.updateTask(updated)
    }

    /**
     * Reschedules an occurrence of a task from originalDateIso to newDateIso.
     */
    suspend fun rescheduleOccurrence(taskId: Long, originalDateIso: String, newDateIso: String) {
        val entity = taskDao.getTaskById(taskId) ?: return
        val recurrenceType = RecurrenceType.fromString(entity.recurrenceType)

        if (recurrenceType == RecurrenceType.NONE) {
            // For one-time tasks, simply update the dueDateIso
            val updated = entity.copy(dueDateIso = newDateIso)
            taskDao.updateTask(updated)
        } else {
            // For recurring tasks, map the original date to the new date
            val map = JsonMappers.parseStringMap(entity.rescheduledDatesJson).toMutableMap()
            map[originalDateIso] = newDateIso
            val updated = entity.copy(rescheduledDatesJson = JsonMappers.serializeStringMap(map))
            taskDao.updateTask(updated)
        }
    }

    /**
     * Skips an occurrence of a recurring task on a specific date.
     */
    suspend fun skipOccurrence(taskId: Long, dateIso: String) {
        val entity = taskDao.getTaskById(taskId) ?: return
        val skipped = JsonMappers.parseStringList(entity.skippedDatesJson).toMutableSet()
        skipped.add(dateIso)
        val updated = entity.copy(skippedDatesJson = JsonMappers.serializeStringList(skipped))
        taskDao.updateTask(updated)
    }

    /**
     * Duplicates a task.
     */
    suspend fun duplicateTask(taskId: Long): Long {
        val original = taskDao.getTaskById(taskId) ?: return -1L
        val duplicate = original.copy(
            id = 0,
            title = "${original.title} (Copy)",
            isOneTimeCompleted = false,
            completedDatesJson = "[]",
            skippedDatesJson = "[]",
            rescheduledDatesJson = "{}",
            createdAt = System.currentTimeMillis()
        )
        return taskDao.insertTask(duplicate)
    }

    /**
     * Archives or unarchives a task.
     */
    suspend fun setTaskArchived(taskId: Long, isArchived: Boolean) {
        val entity = taskDao.getTaskById(taskId) ?: return
        taskDao.updateTask(entity.copy(isArchived = isArchived))
    }

    /**
     * Pauses or resumes a recurring task.
     */
    suspend fun setTaskPaused(taskId: Long, isPaused: Boolean) {
        val entity = taskDao.getTaskById(taskId) ?: return
        taskDao.updateTask(entity.copy(isPaused = isPaused))
    }

    /**
     * Bulk complete tasks for a date.
     */
    suspend fun bulkCompleteTasks(taskIds: List<Long>, dateIso: String) {
        taskIds.forEach { taskId ->
            toggleTaskCompletion(taskId, dateIso)
        }
    }

    /**
     * Bulk delete tasks.
     */
    suspend fun bulkDeleteTasks(taskIds: List<Long>) {
        taskIds.forEach { taskId ->
            taskDao.deleteTaskById(taskId)
        }
    }

    /**
     * Bulk archive tasks.
     */
    suspend fun bulkArchiveTasks(taskIds: List<Long>, isArchived: Boolean = true) {
        taskIds.forEach { taskId ->
            setTaskArchived(taskId, isArchived)
        }
    }

    // --- Habits Flow ---
    val allHabitsFlow: Flow<List<HabitItem>> = habitDao.getAllHabits().map { entities ->
        val todayStr = DateUtils.todayString()
        entities.map { it.toDomainModel(todayStr) }
    }

    suspend fun insertHabit(habit: HabitItem): Long {
        return habitDao.insertHabit(habit.toEntity())
    }

    suspend fun updateHabit(habit: HabitItem) {
        habitDao.updateHabit(habit.toEntity())
    }

    suspend fun deleteHabit(habitId: Long) {
        habitDao.deleteHabitById(habitId)
    }

    suspend fun getHabitById(habitId: Long): HabitItem? {
        val todayStr = DateUtils.todayString()
        return habitDao.getHabitById(habitId)?.toDomainModel(todayStr)
    }

    suspend fun toggleHabitCompletion(habitId: Long, dateIso: String) {
        val entity = habitDao.getHabitById(habitId) ?: return
        val completedDates = JsonMappers.parseStringList(entity.completedDatesJson).toMutableSet()
        if (completedDates.contains(dateIso)) {
            completedDates.remove(dateIso)
        } else {
            completedDates.add(dateIso)
        }
        val updated = entity.copy(completedDatesJson = JsonMappers.serializeStringList(completedDates))
        habitDao.updateHabit(updated)
    }

    // --- 75-Day Challenge Flow ---
    val challengeStateFlow: Flow<ChallengeState> = challengeDao.getActiveChallengeFlow().map { entity ->
        if (entity == null) {
            createDefaultChallengeState()
        } else {
            entity.toDomainModel()
        }
    }

    suspend fun toggleChallengeRule(ruleId: String, dateIso: String) {
        var entity = challengeDao.getActiveChallenge()
        if (entity == null) {
            val defaultState = createDefaultChallengeState()
            entity = defaultState.toEntity()
            challengeDao.insertOrUpdateChallenge(entity)
        }

        val logs = JsonMappers.parseChallengeDailyLogs(entity.dailyLogsJson).toMutableMap()
        val currentLog = logs[dateIso] ?: ChallengeDailyLog(dateIso = dateIso)
        val completedSet = currentLog.completedRuleIds.toMutableSet()

        if (completedSet.contains(ruleId)) {
            completedSet.remove(ruleId)
        } else {
            completedSet.add(ruleId)
        }

        val updatedLog = currentLog.copy(completedRuleIds = completedSet)
        logs[dateIso] = updatedLog

        val updatedEntity = entity.copy(
            dailyLogsJson = JsonMappers.serializeChallengeDailyLogs(logs)
        )
        challengeDao.insertOrUpdateChallenge(updatedEntity)
    }

    suspend fun restartChallenge(newStartDateIso: String, customRules: List<ChallengeRule>? = null) {
        val rulesToUse = customRules ?: DefaultChallengeRules.list
        val newEntity = ChallengeEntity(
            id = 1,
            startDateIso = newStartDateIso,
            targetDays = 75,
            status = "ACTIVE",
            rulesJson = JsonMappers.serializeChallengeRules(rulesToUse),
            dailyLogsJson = "{}",
            createdAt = System.currentTimeMillis()
        )
        challengeDao.insertOrUpdateChallenge(newEntity)
    }

    // --- Water Log Flow ---
    fun getWaterLogForDate(dateIso: String): Flow<WaterLogEntity> {
        return waterLogDao.getWaterLogFlow(dateIso).map { log ->
            log ?: WaterLogEntity(dateIso = dateIso, amountMl = 0, goalMl = 2000)
        }
    }

    suspend fun addWater(amountDelta: Int, dateIso: String, goalMl: Int = 2000) {
        val current = waterLogDao.getWaterLog(dateIso)
        val currentAmount = current?.amountMl ?: 0
        val newAmount = (currentAmount + amountDelta).coerceAtLeast(0)
        val updated = WaterLogEntity(
            dateIso = dateIso,
            amountMl = newAmount,
            goalMl = current?.goalMl ?: goalMl,
            updatedAt = System.currentTimeMillis()
        )
        waterLogDao.insertOrUpdateWaterLog(updated)
    }

    suspend fun resetWater(dateIso: String) {
        val current = waterLogDao.getWaterLog(dateIso) ?: return
        waterLogDao.insertOrUpdateWaterLog(current.copy(amountMl = 0, updatedAt = System.currentTimeMillis()))
    }

    // --- Default Routine Installation ---
    suspend fun installDefaultRoutinesAndHabits(force: Boolean = false) {
        val alreadyInstalled = preferences.hasInstalledDefaultRoutines.first()
        if (alreadyInstalled && !force) return

        val todayIso = DateUtils.todayString()

        // 1. Install Default Habits if none exist
        DefaultHabits.list.forEach { habit ->
            val habitEntity = HabitEntity(
                title = habit.title,
                emoji = habit.emoji,
                description = habit.description,
                reminderTime = habit.reminderTime,
                colorHex = habit.colorHex,
                completedDatesJson = "[]",
                orderIndex = habit.orderIndex,
                createdAt = System.currentTimeMillis()
            )
            habitDao.insertHabit(habitEntity)
        }

        // 2. Install Default Schedule Tasks
        val defaultTasks = listOf(
            TaskItem(
                title = "Sleep & Full Recovery (9:30 PM – 5:30 AM)",
                emoji = "😴",
                description = "Consistent circadian rhythm: wind down at 9:00 PM, lights off at 9:30 PM",
                category = "Habits & Daily Routine",
                priority = TaskPriority.HIGH,
                dueDateIso = todayIso,
                dueTime = "21:30",
                durationMinutes = 480, // 8 hours
                recurrenceType = RecurrenceType.DAILY,
                isReminderEnabled = true,
                reminderMinutesBefore = 30
            ),
            TaskItem(
                title = "Laundry Routine",
                emoji = "🧺",
                description = "Wash, dry, fold and organize clothes",
                category = "Home & Cleaning",
                priority = TaskPriority.MEDIUM,
                dueDateIso = todayIso,
                dueTime = "18:00",
                durationMinutes = 60,
                recurrenceType = RecurrenceType.SELECTED_WEEKDAYS,
                recurrenceDaysOfWeek = listOf("TUESDAY", "THURSDAY", "SUNDAY"),
                isReminderEnabled = true
            ),
            TaskItem(
                title = "Light Room Cleaning & Declutter",
                emoji = "🧹",
                description = "Dust surfaces, clear desk, empty trash",
                category = "Home & Cleaning",
                priority = TaskPriority.LOW,
                dueDateIso = todayIso,
                dueTime = "19:30",
                durationMinutes = 30,
                recurrenceType = RecurrenceType.SELECTED_WEEKDAYS,
                recurrenceDaysOfWeek = listOf("MONDAY", "WEDNESDAY", "FRIDAY", "SATURDAY")
            ),
            TaskItem(
                title = "Sunday Deep Cleaning",
                emoji = "🧽",
                description = "Mop floors, sanitize bathroom, deep vacuum and sheet change",
                category = "Home & Cleaning",
                priority = TaskPriority.HIGH,
                dueDateIso = todayIso,
                dueTime = "10:00",
                durationMinutes = 90,
                recurrenceType = RecurrenceType.SELECTED_WEEKDAYS,
                recurrenceDaysOfWeek = listOf("SUNDAY"),
                isReminderEnabled = true
            ),
            TaskItem(
                title = "Wednesday Head Bath",
                emoji = "🚿",
                description = "Mid-week scalp cleanse and hair wash",
                category = "Personal & Self-Care",
                priority = TaskPriority.MEDIUM,
                dueDateIso = todayIso,
                dueTime = "07:00",
                durationMinutes = 30,
                recurrenceType = RecurrenceType.SELECTED_WEEKDAYS,
                recurrenceDaysOfWeek = listOf("WEDNESDAY")
            ),
            TaskItem(
                title = "Saturday Herbal Oil Head Bath",
                emoji = "💆‍♂️",
                description = "Traditional warm oil scalp massage and deep conditioning",
                category = "Personal & Self-Care",
                priority = TaskPriority.MEDIUM,
                dueDateIso = todayIso,
                dueTime = "08:00",
                durationMinutes = 45,
                recurrenceType = RecurrenceType.SELECTED_WEEKDAYS,
                recurrenceDaysOfWeek = listOf("SATURDAY")
            ),
            TaskItem(
                title = "Replace Toothbrush",
                emoji = "🪥",
                description = "Dental hygiene: replace toothbrush head every 6 months",
                category = "Hospital & Family Health",
                priority = TaskPriority.MEDIUM,
                dueDateIso = todayIso,
                dueTime = "09:00",
                durationMinutes = 15,
                recurrenceType = RecurrenceType.EVERY_6_MONTHS,
                isReminderEnabled = true
            ),
            TaskItem(
                title = "Daily 2-Liter Hydration Goal",
                emoji = "💧",
                description = "Drink pure water consistently across morning, afternoon and evening",
                category = "Personal & Self-Care",
                priority = TaskPriority.HIGH,
                dueDateIso = todayIso,
                dueTime = "12:00",
                durationMinutes = 15,
                recurrenceType = RecurrenceType.DAILY
            ),
            TaskItem(
                title = "Daily Yoga, Exercise & Meditation",
                emoji = "🧘",
                description = "Surya Namaskaram, focused core workouts and mindful stillness",
                category = "Habits & Daily Routine",
                priority = TaskPriority.URGENT,
                dueDateIso = todayIso,
                dueTime = "06:30",
                durationMinutes = 60,
                recurrenceType = RecurrenceType.DAILY,
                isReminderEnabled = true
            ),
            TaskItem(
                title = "Weekly Review & Next Week Planning",
                emoji = "📊",
                description = "Audit habit streaks, review finance expenses, schedule priorities",
                category = "Planning & Reviews",
                priority = TaskPriority.HIGH,
                dueDateIso = todayIso,
                dueTime = "16:00",
                durationMinutes = 60,
                recurrenceType = RecurrenceType.SELECTED_WEEKDAYS,
                recurrenceDaysOfWeek = listOf("SUNDAY"),
                isReminderEnabled = true
            ),
            TaskItem(
                title = "Monthly Life & Financial Audit",
                emoji = "🎯",
                description = "Review budget, update investment targets, check annual goals",
                category = "Planning & Reviews",
                priority = TaskPriority.HIGH,
                dueDateIso = todayIso,
                dueTime = "10:00",
                durationMinutes = 60,
                recurrenceType = RecurrenceType.MONTHLY,
                isReminderEnabled = true
            )
        )

        defaultTasks.forEach { task ->
            taskDao.insertTask(task.toEntity())
        }

        // 3. Initialize 75-Day Challenge
        restartChallenge(newStartDateIso = todayIso)

        preferences.setHasInstalledDefaultRoutines(true)
    }

    // --- Entity Domain Mappings ---
    private fun TaskEntity.toDomainModel(
        project: ProjectEntity? = null,
        labels: List<LabelItem> = emptyList(),
        reminders: List<TaskReminderItem> = emptyList()
    ): TaskItem {
        val completedSet = JsonMappers.parseStringList(completedDatesJson).toSet()
        val skippedSet = JsonMappers.parseStringList(skippedDatesJson).toSet()
        val rescheduledMap = JsonMappers.parseStringMap(rescheduledDatesJson)
        val subtasks = JsonMappers.parseSubtasks(subtasksJson)
        val recurrenceDays = if (recurrenceDaysOfWeek.isBlank()) emptyList() else recurrenceDaysOfWeek.split(",")

        return TaskItem(
            id = id,
            title = title,
            emoji = emoji,
            description = description,
            category = category,
            priority = TaskPriority.fromString(priority),
            dueDateIso = dueDateIso,
            dueTime = dueTime,
            durationMinutes = durationMinutes,
            schedulingMode = SchedulingMode.fromString(schedulingMode),
            durationAmount = durationAmount,
            durationUnit = DurationUnit.fromString(durationUnit),
            recurrenceType = RecurrenceType.fromString(recurrenceType),
            recurrenceInterval = recurrenceInterval,
            recurrenceUnit = recurrenceUnit,
            recurrenceDaysOfWeek = recurrenceDays,
            recurrenceEndCondition = recurrenceEndCondition,
            recurrenceEndDateIso = recurrenceEndDateIso,
            recurrenceCount = recurrenceCount,
            isOneTimeCompleted = isOneTimeCompleted,
            completedDates = completedSet,
            skippedDates = skippedSet,
            rescheduledDates = rescheduledMap,
            subtasks = subtasks,
            notes = notes,
            isReminderEnabled = isReminderEnabled,
            reminderMinutesBefore = reminderMinutesBefore,
            isArchived = isArchived,
            isPaused = isPaused,
            projectId = projectId,
            projectName = project?.name,
            projectColorId = project?.colorId,
            deadlineIso = deadlineIso,
            deadlineTime = deadlineTime,
            isAllDay = isAllDay,
            labels = labels,
            reminders = reminders,
            createdAt = createdAt
        )
    }

    private fun TaskItem.toEntity(): TaskEntity {
        return TaskEntity(
            id = id,
            title = title,
            emoji = emoji,
            description = description,
            category = category,
            priority = priority.name,
            dueDateIso = dueDateIso,
            dueTime = dueTime,
            durationMinutes = durationMinutes,
            schedulingMode = schedulingMode.name,
            durationAmount = durationAmount,
            durationUnit = durationUnit.name,
            deadlineIso = deadlineIso,
            deadlineTime = deadlineTime,
            isAllDay = isAllDay,
            recurrenceType = recurrenceType.name,
            recurrenceInterval = recurrenceInterval,
            recurrenceUnit = recurrenceUnit,
            recurrenceDaysOfWeek = recurrenceDaysOfWeek.joinToString(","),
            recurrenceEndCondition = recurrenceEndCondition,
            recurrenceEndDateIso = recurrenceEndDateIso,
            recurrenceCount = recurrenceCount,
            isOneTimeCompleted = isOneTimeCompleted,
            completedDatesJson = JsonMappers.serializeStringList(completedDates),
            skippedDatesJson = JsonMappers.serializeStringList(skippedDates),
            rescheduledDatesJson = JsonMappers.serializeStringMap(rescheduledDates),
            subtasksJson = JsonMappers.serializeSubtasks(subtasks),
            notes = notes,
            isReminderEnabled = isReminderEnabled,
            reminderMinutesBefore = reminderMinutesBefore,
            isArchived = isArchived,
            isPaused = isPaused,
            projectId = projectId,
            createdAt = createdAt
        )
    }

    private fun ProjectEntity.toDomainModel(taskCount: Int = 0, completedCount: Int = 0): ProjectItem {
        return ProjectItem(
            id = id,
            name = name,
            description = description,
            colorId = colorId,
            emoji = emoji,
            isFavorite = isFavorite,
            isArchived = isArchived,
            orderIndex = orderIndex,
            taskCount = taskCount,
            completedCount = completedCount,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun ProjectItem.toEntity(): ProjectEntity {
        return ProjectEntity(
            id = id,
            name = name,
            description = description,
            colorId = colorId,
            emoji = emoji,
            isFavorite = isFavorite,
            isArchived = isArchived,
            orderIndex = orderIndex,
            createdAt = createdAt,
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun LabelEntity.toDomainModel(taskCount: Int = 0): LabelItem {
        return LabelItem(
            id = id,
            name = name,
            colorId = colorId,
            isFavorite = isFavorite,
            isArchived = isArchived,
            orderIndex = orderIndex,
            taskCount = taskCount,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun LabelItem.toEntity(): LabelEntity {
        return LabelEntity(
            id = id,
            name = name,
            colorId = colorId,
            isFavorite = isFavorite,
            isArchived = isArchived,
            orderIndex = orderIndex,
            createdAt = createdAt,
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun TaskReminderEntity.toDomainModel(): TaskReminderItem {
        return TaskReminderItem(
            id = id,
            taskId = taskId,
            minutesBefore = minutesBefore,
            customTime = customTime,
            type = type,
            referenceType = referenceType,
            isEnabled = isEnabled,
            createdAt = createdAt
        )
    }

    private fun TaskReminderItem.toEntity(overrideTaskId: Long = taskId): TaskReminderEntity {
        return TaskReminderEntity(
            id = id,
            taskId = overrideTaskId,
            minutesBefore = minutesBefore,
            customTime = customTime,
            type = type,
            referenceType = referenceType,
            isEnabled = isEnabled,
            createdAt = createdAt
        )
    }

    private fun HabitEntity.toDomainModel(todayIso: String): HabitItem {
        val completedSet = JsonMappers.parseStringList(completedDatesJson).toSet()
        val isCompletedToday = completedSet.contains(todayIso)

        var currentStreak = 0
        var longestStreak = 0
        var tempStreak = 0

        val last7Days = DateUtils.getLastNDays(7)
        var weeklyCompletions = 0
        for (day in last7Days) {
            if (completedSet.contains(day)) {
                weeklyCompletions++
            }
        }
        val weeklyRate = weeklyCompletions.toFloat() / 7f

        var checkDate = todayIso
        if (!completedSet.contains(checkDate)) {
            checkDate = DateUtils.addDays(todayIso, -1)
        }

        while (completedSet.contains(checkDate)) {
            currentStreak++
            checkDate = DateUtils.addDays(checkDate, -1)
        }

        val historyDays = DateUtils.getLastNDays(60)
        for (day in historyDays) {
            if (completedSet.contains(day)) {
                tempStreak++
                if (tempStreak > longestStreak) longestStreak = tempStreak
            } else {
                tempStreak = 0
            }
        }
        if (currentStreak > longestStreak) longestStreak = currentStreak

        return HabitItem(
            id = id,
            title = title,
            emoji = emoji,
            description = description,
            reminderTime = reminderTime,
            colorHex = colorHex,
            completedDates = completedSet,
            isCompletedToday = isCompletedToday,
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            weeklyCompletionRate = weeklyRate,
            orderIndex = orderIndex,
            createdAt = createdAt
        )
    }

    private fun HabitItem.toEntity(): HabitEntity {
        return HabitEntity(
            id = id,
            title = title,
            emoji = emoji,
            description = description,
            reminderTime = reminderTime,
            colorHex = colorHex,
            completedDatesJson = JsonMappers.serializeStringList(completedDates),
            orderIndex = orderIndex,
            createdAt = createdAt
        )
    }

    private fun ChallengeEntity.toDomainModel(): ChallengeState {
        val rules = JsonMappers.parseChallengeRules(rulesJson).ifEmpty { DefaultChallengeRules.list }
        val logs = JsonMappers.parseChallengeDailyLogs(dailyLogsJson)

        val todayIso = DateUtils.todayString()
        val daysSinceStart = DateUtils.daysBetween(startDateIso, todayIso)
        val currentDay = (daysSinceStart + 1).coerceIn(1, targetDays)

        var completedDays = 0
        var missedDays = 0
        var currentStreak = 0

        for (i in 0 until daysSinceStart) {
            val date = DateUtils.addDays(startDateIso, i)
            val log = logs[date]
            val allRulesDone = log != null && rules.all { log.completedRuleIds.contains(it.id) }
            if (allRulesDone) {
                completedDays++
                currentStreak++
            } else {
                missedDays++
                currentStreak = 0
            }
        }

        val todayLog = logs[todayIso]
        val todayAllDone = todayLog != null && rules.all { todayLog.completedRuleIds.contains(it.id) }
        if (todayAllDone) {
            completedDays++
            currentStreak++
        }

        val progress = (completedDays.toFloat() / targetDays.toFloat()).coerceIn(0f, 1f)

        return ChallengeState(
            id = id,
            startDateIso = startDateIso,
            targetDays = targetDays,
            status = when (status) {
                "COMPLETED" -> ChallengeStatus.COMPLETED
                "RESTARTED" -> ChallengeStatus.RESTARTED
                else -> ChallengeStatus.ACTIVE
            },
            rules = rules,
            logs = logs,
            currentDayNumber = currentDay,
            completedDaysCount = completedDays,
            missedDaysCount = missedDays,
            progressPercentage = progress,
            currentStreak = currentStreak,
            createdAt = createdAt
        )
    }

    private fun ChallengeState.toEntity(): ChallengeEntity {
        return ChallengeEntity(
            id = id,
            startDateIso = startDateIso,
            targetDays = targetDays,
            status = status.name,
            rulesJson = JsonMappers.serializeChallengeRules(rules),
            dailyLogsJson = JsonMappers.serializeChallengeDailyLogs(logs),
            createdAt = createdAt
        )
    }

    private fun createDefaultChallengeState(): ChallengeState {
        val todayIso = DateUtils.todayString()
        return ChallengeState(
            id = 1,
            startDateIso = todayIso,
            targetDays = 75,
            status = ChallengeStatus.ACTIVE,
            rules = DefaultChallengeRules.list,
            logs = emptyMap(),
            currentDayNumber = 1,
            completedDaysCount = 0,
            missedDaysCount = 0,
            progressPercentage = 0f,
            currentStreak = 0
        )
    }

    // --- Challenge Customization & Presets ---
    suspend fun updateChallengeRules(newRules: List<ChallengeRule>, resetProgress: Boolean = false) {
        val current = challengeDao.getActiveChallenge()?.toDomainModel() ?: createDefaultChallengeState()
        val updated = if (resetProgress) {
            current.copy(
                startDateIso = DateUtils.todayString(),
                rules = newRules,
                logs = emptyMap(),
                currentDayNumber = 1,
                completedDaysCount = 0,
                missedDaysCount = 0,
                progressPercentage = 0f,
                currentStreak = 0
            )
        } else {
            current.copy(rules = newRules)
        }
        challengeDao.insertOrUpdateChallenge(updated.toEntity())
    }

    suspend fun applyChallengePreset(preset: ChallengePreset, resetProgress: Boolean = false) {
        updateChallengeRules(preset.rules, resetProgress)
        preferences.setWaterGoalMl(preset.waterTargetMl)
    }

    suspend fun addCustomChallengeRule(title: String, emoji: String, description: String) {
        val current = challengeDao.getActiveChallenge()?.toDomainModel() ?: createDefaultChallengeState()
        val newRule = ChallengeRule(
            id = "custom_rule_${System.currentTimeMillis()}",
            title = title,
            emoji = emoji.ifBlank { "🎯" },
            description = description
        )
        val updatedRules = current.rules + newRule
        challengeDao.insertOrUpdateChallenge(current.copy(rules = updatedRules).toEntity())
    }

    suspend fun removeChallengeRule(ruleId: String) {
        val current = challengeDao.getActiveChallenge()?.toDomainModel() ?: return
        val updatedRules = current.rules.filterNot { it.id == ruleId }
        challengeDao.insertOrUpdateChallenge(current.copy(rules = updatedRules).toEntity())
    }

    suspend fun editChallengeRule(ruleId: String, title: String, emoji: String, description: String) {
        val current = challengeDao.getActiveChallenge()?.toDomainModel() ?: return
        val updatedRules = current.rules.map {
            if (it.id == ruleId) it.copy(title = title, emoji = emoji.ifBlank { it.emoji }, description = description) else it
        }
        challengeDao.insertOrUpdateChallenge(current.copy(rules = updatedRules).toEntity())
    }

    // --- Honeycomb Shortcuts Flow & CRUD ---
    val allHoneycombShortcutsFlow: Flow<List<HoneycombShortcut>> = honeycombShortcutDao.getAllShortcuts().map { list ->
        list.map { it.toDomainModel() }
    }

    suspend fun getHoneycombShortcutsList(): List<HoneycombShortcut> {
        return honeycombShortcutDao.getAllShortcutsList().map { it.toDomainModel() }
    }

    suspend fun insertHoneycombShortcut(shortcut: HoneycombShortcut): Long {
        return honeycombShortcutDao.insertShortcut(shortcut.toEntity())
    }

    suspend fun updateHoneycombShortcut(shortcut: HoneycombShortcut) {
        honeycombShortcutDao.insertShortcut(shortcut.toEntity())
    }

    suspend fun deleteHoneycombShortcut(id: Long) {
        honeycombShortcutDao.deleteShortcutById(id)
    }

    suspend fun saveHoneycombShortcutsOrder(orderedList: List<HoneycombShortcut>) {
        val entities = orderedList.mapIndexed { index, item ->
            item.copy(orderIndex = index).toEntity()
        }
        honeycombShortcutDao.insertShortcuts(entities)
    }

    suspend fun seedDefaultHoneycombShortcuts() {
        val existing = honeycombShortcutDao.getAllShortcutsList()
        if (existing.isEmpty()) {
            val coords = HoneycombLayoutTemplate.generateCoordinates(HoneycombLayoutTemplate.STANDARD, DefaultHoneycombShortcuts.list.size)
            val entities = DefaultHoneycombShortcuts.list.mapIndexed { index, item ->
                val (q, r) = coords.getOrElse(index) { Pair(index % 3, index / 3) }
                item.copy(orderIndex = index, coordQ = q, coordR = r).toEntity()
            }
            honeycombShortcutDao.insertShortcuts(entities)
        } else if (existing.size > 1 && existing.all { it.coordQ == 0 && it.coordR == 0 }) {
            val coords = HoneycombLayoutTemplate.generateCoordinates(HoneycombLayoutTemplate.STANDARD, existing.size)
            existing.forEachIndexed { index, entity ->
                val (q, r) = coords.getOrElse(index) { Pair(index % 3, index / 3) }
                honeycombShortcutDao.updateCoords(entity.id, q, r, entity.orderIndex)
            }
        }
    }

    suspend fun resetHoneycombShortcutsToDefault() {
        honeycombShortcutDao.clearAllShortcuts()
        val coords = HoneycombLayoutTemplate.generateCoordinates(HoneycombLayoutTemplate.STANDARD, DefaultHoneycombShortcuts.list.size)
        val entities = DefaultHoneycombShortcuts.list.mapIndexed { index, item ->
            val (q, r) = coords.getOrElse(index) { Pair(index % 3, index / 3) }
            item.copy(orderIndex = index, coordQ = q, coordR = r).toEntity()
        }
        honeycombShortcutDao.insertShortcuts(entities)
    }

    suspend fun applyHoneycombTemplate(template: HoneycombLayoutTemplate) {
        val shortcuts = honeycombShortcutDao.getAllShortcutsList()
        val coords = HoneycombLayoutTemplate.generateCoordinates(template, shortcuts.size)
        shortcuts.forEachIndexed { index, entity ->
            val coord = coords.getOrElse(index) { Pair(index % 3, index / 3) }
            honeycombShortcutDao.updateCoords(entity.id, coord.first, coord.second, index)
        }
    }

    suspend fun updateShortcutCoords(id: Long, q: Int, r: Int, orderIndex: Int) {
        honeycombShortcutDao.updateCoords(id, q, r, orderIndex)
    }

    // --- TRASH BIN REPOSITORY ---
    val allTrashItemsFlow: Flow<List<TrashItem>> = trashDao.getAllTrashItems().map { list ->
        list.map { it.toDomainModel() }
    }

    suspend fun softDeleteTask(task: TaskItem) {
        val payload = JSONObject().apply {
            put("id", task.id)
            put("title", task.title)
            put("emoji", task.emoji)
            put("description", task.description)
            put("category", task.category)
            put("priority", task.priority.code)
            put("dueDateIso", task.dueDateIso)
            put("dueTime", task.dueTime)
            put("projectId", task.projectId)
        }.toString()
        trashDao.insertTrashItem(
            TrashItemEntity(
                itemType = "TASK",
                originalId = task.id,
                title = task.title,
                subtitle = "Scheduled for ${task.dueDateIso}",
                originalPayloadJson = payload,
                expiresAt = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)
            )
        )
        taskDao.deleteTaskById(task.id)
    }

    suspend fun softDeleteHabit(habit: HabitItem) {
        val payload = JSONObject().apply {
            put("id", habit.id)
            put("title", habit.title)
            put("emoji", habit.emoji)
            put("description", habit.description)
            put("reminderTime", habit.reminderTime)
            put("colorHex", habit.colorHex)
        }.toString()
        trashDao.insertTrashItem(
            TrashItemEntity(
                itemType = "HABIT",
                originalId = habit.id,
                title = habit.title,
                subtitle = "Habit item",
                originalPayloadJson = payload,
                expiresAt = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)
            )
        )
        habitDao.deleteHabitById(habit.id)
    }

    suspend fun softDeleteProject(project: ProjectItem) {
        val payload = JSONObject().apply {
            put("id", project.id)
            put("name", project.name)
            put("description", project.description)
            put("colorId", project.colorId)
            put("emoji", project.emoji)
        }.toString()
        trashDao.insertTrashItem(
            TrashItemEntity(
                itemType = "PROJECT",
                originalId = project.id,
                title = project.name,
                subtitle = "Project folder",
                originalPayloadJson = payload,
                expiresAt = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)
            )
        )
        projectDao.deleteProjectById(project.id)
    }

    suspend fun softDeleteLabel(label: LabelItem) {
        val payload = JSONObject().apply {
            put("id", label.id)
            put("name", label.name)
            put("colorId", label.colorId)
        }.toString()
        trashDao.insertTrashItem(
            TrashItemEntity(
                itemType = "LABEL",
                originalId = label.id,
                title = "#${label.name}",
                subtitle = "Label tag",
                originalPayloadJson = payload,
                expiresAt = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)
            )
        )
        labelDao.deleteLabelById(label.id)
    }

    suspend fun restoreTrashItem(id: Long): Boolean {
        val item = trashDao.getTrashItemById(id) ?: return false
        try {
            val json = JSONObject(item.originalPayloadJson)
            when (item.itemType) {
                "TASK" -> {
                    taskDao.insertTask(
                        TaskEntity(
                            id = json.optLong("id", 0L),
                            title = json.getString("title"),
                            emoji = json.optString("emoji", ""),
                            description = json.optString("description", ""),
                            category = json.optString("category", "Habits & Daily Routine"),
                            priority = json.optString("priority", "P3"),
                            dueDateIso = json.optString("dueDateIso", DateUtils.todayString()),
                            dueTime = if (json.isNull("dueTime")) null else json.optString("dueTime"),
                            projectId = if (json.isNull("projectId")) null else json.optLong("projectId")
                        )
                    )
                }
                "HABIT" -> {
                    habitDao.insertHabit(
                        HabitEntity(
                            id = json.optLong("id", 0L),
                            title = json.getString("title"),
                            emoji = json.optString("emoji", "✨"),
                            description = json.optString("description", ""),
                            reminderTime = if (json.isNull("reminderTime")) null else json.optString("reminderTime"),
                            colorHex = json.optLong("colorHex", 0xFF3B82F6)
                        )
                    )
                }
                "PROJECT" -> {
                    projectDao.insertProject(
                        ProjectEntity(
                            id = json.optLong("id", 0L),
                            name = json.getString("name"),
                            description = json.optString("description", ""),
                            colorId = json.optString("colorId", "charcoal"),
                            emoji = json.optString("emoji", "📁")
                        )
                    )
                }
                "LABEL" -> {
                    labelDao.insertLabel(
                        LabelEntity(
                            id = json.optLong("id", 0L),
                            name = json.getString("name"),
                            colorId = json.optString("colorId", "charcoal")
                        )
                    )
                }
            }
            trashDao.deleteTrashItemById(id)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    suspend fun deleteTrashItemPermanently(id: Long) {
        trashDao.deleteTrashItemById(id)
    }

    suspend fun clearTrash() {
        trashDao.clearTrash()
    }

    suspend fun cleanupExpiredTrash(): Int {
        return trashDao.cleanupExpiredTrash(System.currentTimeMillis())
    }

    // --- PLANNING REPOSITORY ---
    val allPlansFlow: Flow<List<PlanItem>> = planDao.getAllPlans().map { list ->
        list.map { it.toDomainModel() }
    }

    fun getPlansByLevel(level: PlanLevel): Flow<List<PlanItem>> = planDao.getPlansByLevel(level.name).map { list ->
        list.map { it.toDomainModel() }
    }

    suspend fun insertPlan(plan: PlanItem, milestoneTitles: List<String>, linkedTaskIds: List<Long>): Long {
        val planId = planDao.insertPlan(plan.toEntity())
        if (milestoneTitles.isNotEmpty()) {
            val milestones = milestoneTitles.mapIndexed { idx, title ->
                PlanMilestoneEntity(planId = planId, title = title, isCompleted = false, orderIndex = idx)
            }
            planDao.insertMilestones(milestones)
        }
        if (linkedTaskIds.isNotEmpty()) {
            val refs = linkedTaskIds.map { PlanTaskCrossRef(planId = planId, taskId = it) }
            planDao.insertPlanTaskCrossRefs(refs)
        }
        return planId
    }

    suspend fun updatePlan(plan: PlanItem) {
        planDao.updatePlan(plan.toEntity())
    }

    suspend fun deletePlan(id: Long) {
        planDao.deletePlanById(id)
    }

    fun getMilestonesForPlan(planId: Long): Flow<List<PlanMilestone>> = planDao.getMilestonesForPlan(planId).map { list ->
        list.map { PlanMilestone(it.id, it.planId, it.title, it.isCompleted, it.orderIndex) }
    }

    // --- GAMIFICATION REPOSITORY ---
    val gamificationProfileFlow: Flow<GamificationProfile> = gamificationDao.getProfileFlow().map { entity ->
        if (entity != null) {
            GamificationProfile(
                currentXp = entity.currentXp,
                currentCoins = entity.currentCoins,
                currentLevel = entity.currentLevel,
                hunterRank = HunterRank.fromString(entity.hunterRank)
            )
        } else {
            GamificationProfile()
        }
    }

    val allAchievementsFlow: Flow<List<AchievementItem>> = gamificationDao.getAllAchievements().map { list ->
        list.map { it.toDomainModel() }
    }

    val activeRewardsFlow: Flow<List<RewardItem>> = gamificationDao.getActiveRewards().map { list ->
        list.map { it.toDomainModel() }
    }

    suspend fun seedDefaultGamification() {
        val prof = gamificationDao.getProfile()
        if (prof == null) {
            gamificationDao.insertOrUpdateProfile(
                GamificationProfileEntity(
                    id = 1,
                    currentXp = 0L,
                    currentCoins = 50,
                    currentLevel = 1,
                    hunterRank = "E"
                )
            )
        }
        val achievements = DefaultAchievements.list.map {
            AchievementEntity(
                code = it.code,
                title = it.title,
                description = it.description,
                tier = it.tier,
                xpReward = it.xpReward,
                coinsReward = it.coinsReward
            )
        }
        gamificationDao.insertAchievements(achievements)

        val existingRewards = gamificationDao.getActiveRewards().first()
        if (existingRewards.isEmpty()) {
            DefaultRewards.list.forEach { r ->
                gamificationDao.insertReward(
                    RewardItemEntity(
                        title = r.title,
                        description = r.description,
                        coinCost = r.coinCost,
                        iconEmoji = r.iconEmoji
                    )
                )
            }
        }
    }

    suspend fun awardXpAndCoins(eventKey: String, xp: Int, coins: Int, description: String): Boolean {
        val existing = gamificationDao.getXpEvent(eventKey)
        if (existing != null) return false

        gamificationDao.insertXpEvent(
            XpEventEntity(
                eventKey = eventKey,
                xpAwarded = xp,
                coinsAwarded = coins,
                description = description
            )
        )

        val profile = gamificationDao.getProfile() ?: GamificationProfileEntity(1, 0, 0, 1, "E")
        val newXp = profile.currentXp + xp
        val newCoins = profile.currentCoins + coins

        var level = 1
        var needed = 100L
        var totalNeeded = 100L
        while (newXp >= totalNeeded && level < 100) {
            level++
            needed = level * 100L
            totalNeeded += needed
        }

        val rank = HunterRank.fromLevel(level).rankCode
        gamificationDao.insertOrUpdateProfile(
            profile.copy(
                currentXp = newXp,
                currentCoins = newCoins,
                currentLevel = level,
                hunterRank = rank,
                updatedAt = System.currentTimeMillis()
            )
        )
        return true
    }

    suspend fun redeemReward(rewardId: Long): Boolean {
        val reward = gamificationDao.getRewardById(rewardId) ?: return false
        val profile = gamificationDao.getProfile() ?: return false
        if (profile.currentCoins < reward.coinCost) return false

        gamificationDao.insertOrUpdateProfile(
            profile.copy(currentCoins = profile.currentCoins - reward.coinCost)
        )
        gamificationDao.updateReward(reward.copy(redemptionCount = reward.redemptionCount + 1))
        gamificationDao.insertRedemption(
            RewardRedemptionEntity(
                rewardId = reward.id,
                rewardTitle = reward.title,
                coinsSpent = reward.coinCost
            )
        )
        return true
    }

    suspend fun insertReward(reward: RewardItem): Long {
        return gamificationDao.insertReward(
            RewardItemEntity(
                title = reward.title,
                description = reward.description,
                coinCost = reward.coinCost,
                iconEmoji = reward.iconEmoji
            )
        )
    }

    suspend fun checkAndUnlockAchievement(code: String): Boolean {
        val achievement = gamificationDao.getAchievement(code) ?: return false
        if (achievement.isUnlocked) return false
        gamificationDao.updateAchievement(
            achievement.copy(
                isUnlocked = true,
                unlockedAt = System.currentTimeMillis()
            )
        )
        awardXpAndCoins(
            eventKey = "ACHIEVEMENT_$code",
            xp = achievement.xpReward,
            coins = achievement.coinsReward,
            description = "Unlocked achievement: ${achievement.title}"
        )
        return true
    }

    private fun TrashItemEntity.toDomainModel() = TrashItem(
        id = id,
        itemType = itemType,
        originalId = originalId,
        title = title,
        subtitle = subtitle,
        originalPayloadJson = originalPayloadJson,
        deletedAt = deletedAt,
        expiresAt = expiresAt
    )

    private fun PlanEntity.toDomainModel() = PlanItem(
        id = id,
        title = title,
        description = description,
        level = PlanLevel.fromString(level),
        startDateIso = startDateIso,
        targetDateIso = targetDateIso,
        status = PlanStatus.fromString(status),
        manualProgress = manualProgress,
        isAutoProgress = isAutoProgress,
        parentPlanId = parentPlanId,
        projectId = projectId,
        priority = priority,
        createdAt = createdAt
    )

    private fun PlanItem.toEntity() = PlanEntity(
        id = id,
        title = title,
        description = description,
        level = level.name,
        startDateIso = startDateIso,
        targetDateIso = targetDateIso,
        status = status.name,
        manualProgress = manualProgress,
        isAutoProgress = isAutoProgress,
        parentPlanId = parentPlanId,
        projectId = projectId,
        priority = priority,
        createdAt = createdAt,
        updatedAt = System.currentTimeMillis()
    )

    private fun AchievementEntity.toDomainModel() = AchievementItem(
        code = code,
        title = title,
        description = description,
        tier = tier,
        xpReward = xpReward,
        coinsReward = coinsReward,
        isUnlocked = isUnlocked,
        unlockedAt = unlockedAt
    )

    private fun RewardItemEntity.toDomainModel() = RewardItem(
        id = id,
        title = title,
        description = description,
        coinCost = coinCost,
        iconEmoji = iconEmoji,
        isArchived = isArchived,
        redemptionCount = redemptionCount,
        createdAt = createdAt
    )

    private fun HoneycombShortcutEntity.toDomainModel(): HoneycombShortcut {
        return HoneycombShortcut(
            id = id,
            type = ShortcutType.fromString(type),
            targetId = targetId,
            title = title,
            emoji = emoji,
            customColorHex = customColorHex,
            orderIndex = orderIndex,
            coordQ = coordQ,
            coordR = coordR,
            isPinned = isPinned
        )
    }

    private fun HoneycombShortcut.toEntity(): HoneycombShortcutEntity {
        return HoneycombShortcutEntity(
            id = id,
            type = type.name,
            targetId = targetId,
            title = title,
            emoji = emoji,
            customColorHex = customColorHex,
            orderIndex = orderIndex,
            coordQ = coordQ,
            coordR = coordR,
            isPinned = isPinned
        )
    }
}

