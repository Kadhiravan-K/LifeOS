package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entities.ChallengeEntity
import com.example.data.local.entities.HabitEntity
import com.example.data.local.entities.TaskEntity
import com.example.data.local.entities.WaterLogEntity
import com.example.data.preferences.UserPreferences
import com.example.domain.model.ChallengeDailyLog
import com.example.domain.model.ChallengeRule
import com.example.domain.model.ChallengeState
import com.example.domain.model.ChallengeStatus
import com.example.domain.model.DefaultChallengeRules
import com.example.domain.model.DefaultHabits
import com.example.domain.model.HabitItem
import com.example.domain.model.RecurrenceType
import com.example.domain.model.Subtask
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskPriority
import com.example.domain.recurrence.RecurrenceEngine
import com.example.utils.DateUtils
import kotlinx.coroutines.flow.Flow
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

    // --- Tasks Flow ---
    val allTasksFlow: Flow<List<TaskItem>> = taskDao.getAllTasks().map { entities ->
        entities.map { it.toDomainModel() }
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
        return taskDao.insertTask(task.toEntity())
    }

    suspend fun updateTask(task: TaskItem) {
        taskDao.updateTask(task.toEntity())
    }

    suspend fun deleteTask(taskId: Long) {
        taskDao.deleteTaskById(taskId)
    }

    suspend fun getTaskById(taskId: Long): TaskItem? {
        return taskDao.getTaskById(taskId)?.toDomainModel()
    }

    /**
     * Toggles completion for a task on a specific date.
     * Completing today does NOT complete future occurrences of recurring tasks.
     */
    suspend fun toggleTaskCompletion(taskId: Long, dateIso: String) {
        val entity = taskDao.getTaskById(taskId) ?: return
        val currentCompletedDates = JsonMappers.parseStringList(entity.completedDatesJson).toMutableSet()
        val recurrenceType = RecurrenceType.fromString(entity.recurrenceType)

        val newOneTimeCompleted: Boolean
        if (recurrenceType == RecurrenceType.NONE) {
            if (entity.isOneTimeCompleted) {
                newOneTimeCompleted = false
                currentCompletedDates.remove(dateIso)
            } else {
                newOneTimeCompleted = true
                currentCompletedDates.add(dateIso)
            }
        } else {
            newOneTimeCompleted = false
            if (currentCompletedDates.contains(dateIso)) {
                currentCompletedDates.remove(dateIso)
            } else {
                currentCompletedDates.add(dateIso)
            }
        }

        val updatedEntity = entity.copy(
            isOneTimeCompleted = newOneTimeCompleted,
            completedDatesJson = JsonMappers.serializeStringList(currentCompletedDates)
        )
        taskDao.updateTask(updatedEntity)
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
    private fun TaskEntity.toDomainModel(): TaskItem {
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
            recurrenceType = RecurrenceType.fromString(recurrenceType),
            recurrenceInterval = recurrenceInterval,
            recurrenceDaysOfWeek = recurrenceDays,
            recurrenceEndDateIso = recurrenceEndDateIso,
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
            recurrenceType = recurrenceType.name,
            recurrenceInterval = recurrenceInterval,
            recurrenceDaysOfWeek = recurrenceDaysOfWeek.joinToString(","),
            recurrenceEndDateIso = recurrenceEndDateIso,
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
}
