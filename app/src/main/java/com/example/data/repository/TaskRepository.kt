package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.dao.HoneycombShortcutDao
import com.example.data.local.dao.LabelDao
import com.example.data.local.dao.MedicationDao
import com.example.data.local.dao.ProjectDao
import com.example.data.local.dao.TaskDao
import com.example.data.local.dao.TaskReminderDao
import com.example.data.local.dao.TrashDao
import com.example.data.local.entities.LabelEntity
import com.example.data.local.entities.ProjectEntity
import com.example.data.local.entities.TaskEntity
import com.example.data.local.entities.TaskLabelCrossRef
import com.example.data.local.entities.TaskReminderEntity
import com.example.data.local.entities.TrashItemEntity
import com.example.data.preferences.UserPreferences
import com.example.domain.gamification.GamificationEngine
import com.example.domain.gamification.StreakResult
import com.example.domain.model.DurationUnit
import com.example.domain.model.LabelItem
import com.example.domain.model.RecurrenceType
import com.example.domain.model.SchedulingMode
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskPriority
import com.example.domain.model.TaskReminderItem
import com.example.domain.recurrence.RecurrenceEngine
import com.example.utils.DateUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Dedicated repository for the Task domain (P7-A4 incremental architecture decomposition).
 * Encapsulates task CRUD, completion, labels, reminders, recurrence, reordering,
 * soft deletion/trash, discipline deduplication, and profile scoping.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TaskRepository(
    private val database: AppDatabase,
    private val preferences: UserPreferences,
    private val taskDao: TaskDao = database.taskDao(),
    private val projectDao: ProjectDao = database.projectDao(),
    private val labelDao: LabelDao = database.labelDao(),
    private val taskReminderDao: TaskReminderDao = database.taskReminderDao(),
    private val honeycombShortcutDao: HoneycombShortcutDao = database.honeycombShortcutDao(),
    private val medicationDao: MedicationDao = database.medicationDao(),
    private val trashDao: TrashDao = database.trashDao(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    val activeProfileIdFlow: Flow<Long> = preferences.activeProfileId
) {

    @androidx.annotation.VisibleForTesting
    var testTransactionInterceptor: ((step: String) -> Unit)? = null

    suspend fun getActiveProfileId(): Long = activeProfileIdFlow.first()

    // ==========================================
    // --- REACTIVE TASK STREAMS ---
    // ==========================================

    /**
     * Unified Tasks Flow enriched with associated Project, Labels, and Reminders.
     */
    val allTasksFlow: Flow<List<TaskItem>> = activeProfileIdFlow.flatMapLatest { profileId ->
        combine(
            taskDao.getTasksForProfile(profileId),
            projectDao.getAllProjectsForProfileFlow(profileId),
            labelDao.getAllLabelsForProfileFlow(profileId),
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

    /**
     * Reactive task streaks stream based on completion dates.
     */
    val taskStreaksFlow: Flow<StreakResult> = taskDao.getAllTasks().map { tasks ->
        val allDates = tasks.flatMap { JsonMappers.parseStringList(it.completedDatesJson) }.toSet()
        GamificationEngine.calculateStreaks(allDates, DateUtils.todayString())
    }

    fun getTasksForProfileFlow(profileId: Long): Flow<List<TaskEntity>> = taskDao.getTasksForProfile(profileId)

    fun getAllTaskEntitiesFlow(): Flow<List<TaskEntity>> = taskDao.getAllTasks()

    // ==========================================
    // --- TASK CRUD & TRANSACTIONAL OPERATIONS ---
    // ==========================================

    suspend fun getTaskById(taskId: Long): TaskItem? = withContext(ioDispatcher) {
        val entity = taskDao.getTaskById(taskId) ?: return@withContext null
        val project = entity.projectId?.let { projectDao.getProjectById(it) }
        val labelEntities = labelDao.getLabelsForTaskList(taskId)
        val reminderEntities = taskReminderDao.getRemindersForTaskList(taskId)
        entity.toDomainModel(project, labelEntities.map { it.toDomainModel() }, reminderEntities.map { it.toDomainModel() })
    }

    suspend fun insertTask(task: TaskItem): Long = withContext(ioDispatcher) {
        val effectiveProfileId = if (task.profileId > 0) task.profileId else getActiveProfileId()
        val validatedProjectId = task.projectId?.let { pId ->
            val project = projectDao.getProjectById(pId)
            if (project != null && project.profileId == effectiveProfileId) pId else null
        }
        val validatedLabels = task.labels.filter { label ->
            val dbLabel = labelDao.getLabelById(label.id)
            dbLabel == null || dbLabel.profileId == effectiveProfileId
        }
        val safeTask = task.copy(
            profileId = effectiveProfileId,
            projectId = validatedProjectId,
            labels = validatedLabels
        )
        database.withTransaction {
            val taskId = taskDao.insertTask(safeTask.toEntity())
            testTransactionInterceptor?.invoke("AFTER_INSERT_TASK")
            saveTaskLabelsInternal(taskId, safeTask.labels)
            testTransactionInterceptor?.invoke("AFTER_SAVE_LABELS")
            saveTaskRemindersInternal(taskId, safeTask.reminders)
            testTransactionInterceptor?.invoke("AFTER_SAVE_REMINDERS")
            taskId
        }
    }

    suspend fun insertTaskEntity(entity: TaskEntity): Long = withContext(ioDispatcher) {
        taskDao.insertTask(entity)
    }

    suspend fun updateTask(task: TaskItem) = withContext(ioDispatcher) {
        val effectiveProfileId = if (task.profileId > 0) task.profileId else getActiveProfileId()
        val validatedProjectId = task.projectId?.let { pId ->
            val project = projectDao.getProjectById(pId)
            if (project != null && project.profileId == effectiveProfileId) pId else null
        }
        val validatedLabels = task.labels.filter { label ->
            val dbLabel = labelDao.getLabelById(label.id)
            dbLabel == null || dbLabel.profileId == effectiveProfileId
        }
        val safeTask = task.copy(
            profileId = effectiveProfileId,
            projectId = validatedProjectId,
            labels = validatedLabels
        )
        database.withTransaction {
            taskDao.updateTask(safeTask.toEntity())
            testTransactionInterceptor?.invoke("AFTER_UPDATE_TASK")
            saveTaskLabelsInternal(safeTask.id, safeTask.labels)
            testTransactionInterceptor?.invoke("AFTER_UPDATE_LABELS")
            saveTaskRemindersInternal(safeTask.id, safeTask.reminders)
            testTransactionInterceptor?.invoke("AFTER_UPDATE_REMINDERS")
        }
    }

    suspend fun deleteTask(taskId: Long) = withContext(ioDispatcher) {
        database.withTransaction {
            labelDao.clearLabelsForTask(taskId)
            taskReminderDao.deleteRemindersForTask(taskId)
            honeycombShortcutDao.deleteShortcutByTarget("TASK", taskId.toString())
            medicationDao.deleteMedicationByTaskId(taskId)
            medicationDao.deleteDosesByTaskId(taskId)
            testTransactionInterceptor?.invoke("BEFORE_DELETE_TASK_ROW")
            taskDao.deleteTaskById(taskId)
            testTransactionInterceptor?.invoke("AFTER_DELETE_TASK_ROW")
        }
    }

    suspend fun softDeleteTask(task: TaskItem, retentionMillis: Long = 30L * 24 * 60 * 60 * 1000L) = withContext(ioDispatcher) {
        val effectiveProfileId = if (task.profileId > 0) task.profileId else getActiveProfileId()
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
            put("profileId", effectiveProfileId)
        }.toString()
        database.withTransaction {
            trashDao.insertTrashItem(
                TrashItemEntity(
                    profileId = effectiveProfileId,
                    itemType = "TASK",
                    originalId = task.id,
                    title = task.title,
                    subtitle = "Scheduled for ${task.dueDateIso}",
                    originalPayloadJson = payload,
                    expiresAt = System.currentTimeMillis() + retentionMillis
                )
            )
            labelDao.clearLabelsForTask(task.id)
            taskReminderDao.deleteRemindersForTask(task.id)
            honeycombShortcutDao.deleteShortcutByTarget("TASK", task.id.toString())
            taskDao.deleteTaskById(task.id)
        }
    }

    suspend fun deleteAllTasksForProfile(profileId: Long) = withContext(ioDispatcher) {
        taskDao.deleteAllTasksForProfile(profileId)
    }

    suspend fun getAllTasksList(): List<TaskEntity> = withContext(ioDispatcher) {
        taskDao.getAllTasksList()
    }

    suspend fun getTasksForProfileList(profileId: Long): List<TaskEntity> = withContext(ioDispatcher) {
        taskDao.getTasksForProfileList(profileId)
    }

    suspend fun hasTasks(): Boolean = withContext(ioDispatcher) {
        taskDao.getAllTasksList().isNotEmpty()
    }

    // ==========================================
    // --- TASK LABELS & REMINDERS ---
    // ==========================================

    suspend fun saveTaskLabels(taskId: Long, labels: List<LabelItem>) = withContext(ioDispatcher) {
        database.withTransaction {
            saveTaskLabelsInternal(taskId, labels)
        }
    }

    private suspend fun saveTaskLabelsInternal(taskId: Long, labels: List<LabelItem>) {
        labelDao.clearLabelsForTask(taskId)
        val refs = labels.map { TaskLabelCrossRef(taskId = taskId, labelId = it.id) }
        labelDao.insertTaskLabelCrossRefs(refs)
    }

    suspend fun saveTaskReminders(taskId: Long, reminders: List<TaskReminderItem>) = withContext(ioDispatcher) {
        database.withTransaction {
            saveTaskRemindersInternal(taskId, reminders)
        }
    }

    private suspend fun saveTaskRemindersInternal(taskId: Long, reminders: List<TaskReminderItem>) {
        taskReminderDao.deleteRemindersForTask(taskId)
        val entities = reminders.map { it.toEntity(taskId) }
        taskReminderDao.insertReminders(entities)
    }

    // ==========================================
    // --- TASK COMPLETION & RECURRENCE ---
    // ==========================================

    /**
     * Toggles completion for a task on a specific date.
     * Invokes optional [onCompletionChanged] callback when completion state updates.
     */
    suspend fun toggleTaskCompletion(
        taskId: Long,
        dateIso: String,
        onCompletionChanged: (suspend (taskEntity: TaskEntity, isNowCompleted: Boolean, dateIso: String) -> Unit)? = null
    ): Boolean? = withContext(ioDispatcher) {
        val entity = taskDao.getTaskById(taskId) ?: return@withContext null
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
        } else if (recurrenceType == RecurrenceType.MONTHLY_WINDOW) {
            newOneTimeCompleted = false
            val targetYearMonth = if (dateIso.length >= 7) dateIso.substring(0, 7) else dateIso
            val completedInMonth = currentCompletedDates.filter { it.startsWith(targetYearMonth) }
            if (completedInMonth.isNotEmpty()) {
                isNowCompleted = false
                currentCompletedDates.removeAll(completedInMonth.toSet())
            } else {
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

        onCompletionChanged?.invoke(entity, isNowCompleted, dateIso)

        isNowCompleted
    }

    suspend fun toggleSubtask(taskId: Long, subtaskId: String) = withContext(ioDispatcher) {
        val entity = taskDao.getTaskById(taskId) ?: return@withContext
        val subtasks = JsonMappers.parseSubtasks(entity.subtasksJson).map {
            if (it.id == subtaskId) it.copy(isCompleted = !it.isCompleted) else it
        }
        val updated = entity.copy(subtasksJson = JsonMappers.serializeSubtasks(subtasks))
        taskDao.updateTask(updated)
    }

    suspend fun markTaskCompletedForDate(taskId: Long, dateIso: String) = withContext(ioDispatcher) {
        val entity = taskDao.getTaskById(taskId) ?: return@withContext
        val currentCompleted = JsonMappers.parseStringList(entity.completedDatesJson).toMutableSet()
        currentCompleted.add(dateIso)
        val updated = entity.copy(
            isOneTimeCompleted = entity.recurrenceType == "NONE",
            completedDatesJson = JsonMappers.serializeStringList(currentCompleted)
        )
        taskDao.updateTask(updated)
    }

    suspend fun rescheduleOccurrence(taskId: Long, originalDateIso: String, newDateIso: String) = withContext(ioDispatcher) {
        val entity = taskDao.getTaskById(taskId) ?: return@withContext
        val recurrenceType = RecurrenceType.fromString(entity.recurrenceType)

        if (recurrenceType == RecurrenceType.NONE) {
            val updated = entity.copy(dueDateIso = newDateIso)
            taskDao.updateTask(updated)
        } else {
            val map = JsonMappers.parseStringMap(entity.rescheduledDatesJson).toMutableMap()
            map[originalDateIso] = newDateIso
            val updated = entity.copy(rescheduledDatesJson = JsonMappers.serializeStringMap(map))
            taskDao.updateTask(updated)
        }
    }

    suspend fun skipOccurrence(taskId: Long, dateIso: String) = withContext(ioDispatcher) {
        val entity = taskDao.getTaskById(taskId) ?: return@withContext
        val skipped = JsonMappers.parseStringList(entity.skippedDatesJson).toMutableSet()
        skipped.add(dateIso)
        val updated = entity.copy(skippedDatesJson = JsonMappers.serializeStringList(skipped))
        taskDao.updateTask(updated)
    }

    suspend fun duplicateTask(taskId: Long): Long = withContext(ioDispatcher) {
        val original = taskDao.getTaskById(taskId) ?: return@withContext -1L
        val duplicate = original.copy(
            id = 0,
            title = "${original.title} (Copy)",
            isOneTimeCompleted = false,
            completedDatesJson = "[]",
            skippedDatesJson = "[]",
            rescheduledDatesJson = "{}",
            createdAt = System.currentTimeMillis()
        )
        taskDao.insertTask(duplicate)
    }

    suspend fun setTaskArchived(taskId: Long, isArchived: Boolean) = withContext(ioDispatcher) {
        val entity = taskDao.getTaskById(taskId) ?: return@withContext
        taskDao.updateTask(entity.copy(isArchived = isArchived))
    }

    suspend fun setTaskPaused(taskId: Long, isPaused: Boolean) = withContext(ioDispatcher) {
        val entity = taskDao.getTaskById(taskId) ?: return@withContext
        taskDao.updateTask(entity.copy(isPaused = isPaused))
    }

    suspend fun bulkCompleteTasks(
        taskIds: List<Long>,
        dateIso: String,
        onCompletionChanged: (suspend (taskEntity: TaskEntity, isNowCompleted: Boolean, dateIso: String) -> Unit)? = null
    ) = withContext(ioDispatcher) {
        taskIds.forEach { taskId ->
            toggleTaskCompletion(taskId, dateIso, onCompletionChanged)
        }
    }

    suspend fun bulkDeleteTasks(taskIds: List<Long>) = withContext(ioDispatcher) {
        taskIds.forEach { taskId ->
            taskDao.deleteTaskById(taskId)
        }
    }

    suspend fun bulkArchiveTasks(taskIds: List<Long>, isArchived: Boolean = true) = withContext(ioDispatcher) {
        taskIds.forEach { taskId ->
            setTaskArchived(taskId, isArchived)
        }
    }

    // ==========================================
    // --- QUICK REMINDERS & DEDUPLICATION ---
    // ==========================================

    suspend fun getOrCreateTaskForShortcut(
        title: String,
        emoji: String = "🔥",
        category: String = "Routines",
        recurrence: RecurrenceType = RecurrenceType.DAILY
    ): TaskItem {
        val cleanTitle = title.trim()
        val activeProfileId = getActiveProfileId()
        val allTasks = taskDao.getTasksForProfileList(activeProfileId)
        val existing = allTasks.find {
            !it.isArchived && (
                it.title.equals(cleanTitle, ignoreCase = true) ||
                it.title.contains(cleanTitle, ignoreCase = true) ||
                cleanTitle.contains(it.title, ignoreCase = true)
            )
        }
        if (existing != null) {
            return getTaskById(existing.id) ?: existing.toDomainModel()
        }

        val todayIso = DateUtils.todayString()
        val newTaskId = insertTask(
            TaskItem(
                profileId = activeProfileId,
                title = cleanTitle,
                emoji = emoji,
                description = "Daily Quick Reminder Routine",
                category = category,
                priority = TaskPriority.HIGH,
                dueDateIso = todayIso,
                durationMinutes = 0,
                durationAmount = 0,
                schedulingMode = SchedulingMode.DURATION,
                isAllDay = true,
                recurrenceType = recurrence,
                isQuickReminder = true,
                createdAt = System.currentTimeMillis()
            )
        )
        return getTaskById(newTaskId) ?: TaskItem(
            id = newTaskId,
            title = cleanTitle,
            emoji = emoji,
            category = category,
            priority = TaskPriority.HIGH,
            dueDateIso = todayIso,
            durationMinutes = 0,
            isAllDay = true,
            recurrenceType = recurrence
        )
    }

    suspend fun getDuplicateDisciplineTasks(): List<TaskItem> {
        val allEntities = taskDao.getAllTasksList()
        val disciplineEntities = allEntities.filter {
            !it.isArchived && (
                it.title.equals("Discipline", ignoreCase = true) ||
                it.title.equals("Discipline Check", ignoreCase = true) ||
                it.title.startsWith("Discipline", ignoreCase = true)
            )
        }
        return disciplineEntities.map { entity ->
            getTaskById(entity.id) ?: entity.toDomainModel()
        }
    }

    suspend fun cleanupDuplicateDisciplineTasks(keepTaskId: Long? = null): Int = withContext(ioDispatcher) {
        val duplicates = getDuplicateDisciplineTasks()
        if (duplicates.size <= 1) return@withContext 0

        val targetToKeep = if (keepTaskId != null) {
            duplicates.find { it.id == keepTaskId } ?: duplicates.minByOrNull { it.id }!!
        } else {
            duplicates.maxByOrNull { it.completedDates.size } ?: duplicates.minByOrNull { it.id }!!
        }

        var deletedCount = 0
        duplicates.forEach { task ->
            if (task.id != targetToKeep.id) {
                deleteTask(task.id)
                deletedCount++
            }
        }
        deletedCount
    }

    // ==========================================
    // --- MAPPERS ---
    // ==========================================

    fun TaskEntity.toDomainModel(
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
            profileId = profileId,
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
            recurrenceWindowStartDay = recurrenceWindowStartDay,
            recurrenceWindowEndDay = recurrenceWindowEndDay,
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
            isQuickReminder = isQuickReminder,
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

    fun TaskItem.toEntity(): TaskEntity {
        return TaskEntity(
            id = id,
            profileId = profileId,
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
            recurrenceWindowStartDay = recurrenceWindowStartDay,
            recurrenceWindowEndDay = recurrenceWindowEndDay,
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
            isQuickReminder = isQuickReminder,
            projectId = projectId,
            createdAt = createdAt
        )
    }

    fun LabelEntity.toDomainModel(taskCount: Int = 0): LabelItem {
        return LabelItem(
            id = id,
            profileId = profileId,
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

    fun TaskReminderEntity.toDomainModel(): TaskReminderItem {
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

    fun TaskReminderItem.toEntity(overrideTaskId: Long = taskId): TaskReminderEntity {
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
}
