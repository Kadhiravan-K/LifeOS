package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entities.ChallengeEntity
import com.example.data.local.entities.HabitEntity
import com.example.data.local.entities.HoneycombShortcutEntity
import com.example.data.local.entities.ProfileEntity
import com.example.data.local.entities.TaskEntity
import com.example.data.local.entities.WaterLogEntity
import com.example.data.preferences.UserPreferences
import com.example.data.local.entities.MedicationDoseEntity
import com.example.data.local.entities.MedicationEntity
import com.example.domain.medication.AlertStyle
import com.example.domain.medication.DoseStatus
import com.example.domain.medication.FoodRelationship
import com.example.domain.medication.IntervalModel
import com.example.domain.medication.MealPeriod
import com.example.domain.medication.MealPeriodConfig
import com.example.domain.medication.MedicationDoseItem
import com.example.domain.medication.MedicationForm
import com.example.domain.medication.MedicationItem
import com.example.domain.medication.MedicationScheduleConfig
import com.example.domain.medication.MedicationScheduleEngine
import com.example.domain.medication.MedicineScheduleType
import com.example.domain.tracker.TrackerItem
import com.example.domain.tracker.UsageCycleItem
import com.example.domain.tracker.UsageEntryItem
import com.example.domain.tracker.TrackerPurchaseItem
import com.example.domain.tracker.InventoryAdjustmentItem
import com.example.domain.subscription.SubscriptionItem
import com.example.domain.subscription.BillingOccurrenceItem
import com.example.domain.subscription.SubscriptionPaymentItem
import com.example.domain.subscription.PriceHistoryItem
import com.example.domain.subscription.SubscriptionStatus
import com.example.domain.subscription.BillStatus
import com.example.domain.subscription.SubscriptionScheduleEngine
import com.example.data.local.entities.SubscriptionEntity
import com.example.data.local.entities.BillingOccurrenceEntity
import com.example.data.local.entities.SubscriptionPaymentEntity
import com.example.data.local.entities.PriceHistoryEntity
import com.example.domain.model.ChallengeDailyLog
import com.example.domain.model.ChallengePreset
import com.example.domain.model.ChallengeRule
import com.example.domain.projectworkspace.ProjectWorkspaceItem
import com.example.domain.projectworkspace.ProjectWorkspaceView
import com.example.domain.projectworkspace.ProjectCustomStatus
import com.example.domain.projectworkspace.ProjectCustomProperty
import com.example.data.local.entities.ProjectWorkspaceItemEntity
import com.example.data.local.entities.ProjectWorkspaceViewEntity
import com.example.data.local.entities.ProjectCustomStatusEntity
import com.example.data.local.entities.ProjectCustomPropertyEntity
import com.example.domain.model.ChallengeState
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
import com.example.domain.model.TodoistColorPalette
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import androidx.room.withTransaction
import com.example.domain.gamification.GamificationEngine
import com.example.domain.gamification.StreakResult
import com.example.domain.templates.LifeTemplate
import com.example.domain.templates.TemplateImportSummary
import com.example.ui.theme.engine.PremiumEntitlementRepository

@OptIn(ExperimentalCoroutinesApi::class)
class LifeRepository(
    private val database: AppDatabase,
    private val preferences: UserPreferences,
    private val entitlementRepo: PremiumEntitlementRepository? = null,
    val subscriptionRepository: SubscriptionRepository = SubscriptionRepository(database, preferences),
    val trackerRepository: TrackerRepository = TrackerRepository(database, preferences),
    val taskRepository: TaskRepository = TaskRepository(database, preferences)
) {
    private val profileDao = database.profileDao()
    private val habitDao = database.habitDao()
    private val challengeDao = database.challengeDao()
    private val waterLogDao = database.waterLogDao()
    private val honeycombShortcutDao = database.honeycombShortcutDao()
    private val projectDao = database.projectDao()
    private val labelDao = database.labelDao()
    private val trashDao = database.trashDao()
    private val planDao = database.planDao()
    private val gamificationDao = database.gamificationDao()
    private val medicationDao = database.medicationDao()
    private val profileSurveyDao = database.profileSurveyDao()
    private val profileSecurityDao = database.profileSecurityDao()
    private val projectWorkspaceDao = database.projectWorkspaceDao()
    private val glossaryDao = database.glossaryDao()
    val shiftDao = database.shiftDao()
    val giveGetDao = database.giveGetDao()

    @androidx.annotation.VisibleForTesting
    internal var testTransactionInterceptor: ((step: String) -> Unit)?
        get() = taskRepository.testTransactionInterceptor
        set(value) {
            taskRepository.testTransactionInterceptor = value
        }

    // --- Release 3 Phase 5 Profile Isolation & Management (#9) ---
    val activeProfileIdFlow: Flow<Long> = preferences.activeProfileId
    val allProfilesFlow: Flow<List<ProfileEntity>> = profileDao.getAllProfilesFlow()

    suspend fun getActiveProfileId(): Long = preferences.activeProfileId.first()

    suspend fun setActiveProfileId(profileId: Long) {
        preferences.setActiveProfileId(profileId)
    }

    suspend fun getAllProfiles(): List<ProfileEntity> = profileDao.getAllProfiles()

    suspend fun getProfileById(id: Long): ProfileEntity? = profileDao.getProfileById(id)

    suspend fun createProfile(
        name: String,
        emoji: String = "📁",
        colorHex: Long = 0xFF3B82F6,
        type: String = ProfileEntity.TYPE_CUSTOM
    ): Long {
        val currentCount = profileDao.getProfileCount()
        val profile = ProfileEntity(
            name = name,
            emoji = emoji,
            colorHex = colorHex,
            type = type,
            orderIndex = currentCount,
            isDefault = false
        )
        return profileDao.insertProfile(profile)
    }

    suspend fun deleteProfile(id: Long): Boolean {
        if (id == ProfileEntity.DEFAULT_PERSONAL_PROFILE_ID) return false
        val active = getActiveProfileId()
        if (active == id) {
            setActiveProfileId(ProfileEntity.DEFAULT_PERSONAL_PROFILE_ID)
        }
        val deleted = profileDao.deleteCustomProfile(id)
        if (deleted > 0) {
            profileSurveyDao.deleteSurveyForProfile(id)
            taskRepository.deleteAllTasksForProfile(id)
            habitDao.deleteAllHabitsForProfile(id)
            honeycombShortcutDao.deleteAllShortcutsForProfile(id)
            projectDao.deleteAllProjectsForProfile(id)
            labelDao.deleteAllLabelsForProfile(id)
            trashDao.deleteAllTrashForProfile(id)
            planDao.deleteAllPlansForProfile(id)
            trackerRepository.deleteAllTrackersForProfile(id)
            subscriptionRepository.deleteAllSubscriptionsForProfile(id)
            medicationDao.deleteAllMedicationsForProfile(id)
            projectWorkspaceDao.deleteAllItemsForProfile(id)
            projectWorkspaceDao.deleteAllViewsForProfile(id)
            projectWorkspaceDao.deleteAllCustomStatusesForProfile(id)
            projectWorkspaceDao.deleteAllCustomPropertiesForProfile(id)
            database.glossaryDao().deleteUserDataForProfile(id)
            database.mcpDao().deleteServersForProfile(id)
            database.mcpDao().clearAuditLogs(id)
        }
        return deleted > 0
    }

    // --- Release 3 Phase 5 Batch 05C: Profile Survey (#19) ---
    fun getSurveyForProfileFlow(profileId: Long): Flow<com.example.data.local.entities.ProfileSurveyEntity?> {
        return profileSurveyDao.getSurveyForProfileFlow(profileId)
    }

    suspend fun getSurveyForProfile(profileId: Long): com.example.data.local.entities.ProfileSurveyEntity? {
        return profileSurveyDao.getSurveyForProfile(profileId)
    }

    suspend fun ensureProfileExists(profileId: Long): ProfileEntity {
        val existing = profileDao.getProfileById(profileId)
        if (existing != null) return existing

        val profile = if (profileId == ProfileEntity.DEFAULT_PERSONAL_PROFILE_ID) {
            ProfileEntity.createDefaultPersonal()
        } else {
            ProfileEntity(
                id = profileId,
                name = "Profile $profileId",
                emoji = "👤",
                colorHex = 4282168054L,
                type = ProfileEntity.TYPE_CUSTOM,
                orderIndex = profileDao.getProfileCount(),
                isDefault = false
            )
        }
        profileDao.insertProfile(profile)
        return profileDao.getProfileById(profileId) ?: profile
    }

    suspend fun ensureDefaultPersonalProfileExists() {
        ensureProfileExists(ProfileEntity.DEFAULT_PERSONAL_PROFILE_ID)
    }

    fun isProfileOnboardingCompletedFlow(profileId: Long): Flow<Boolean> {
        return combine(
            profileSurveyDao.getSurveyForProfileFlow(profileId),
            preferences.completedOnboardingProfileIds
        ) { survey, completedIds ->
            (survey != null && (survey.isCompleted || survey.isSkipped)) || completedIds.contains(profileId)
        }
    }

    val activeProfileOnboardingCompletedFlow: Flow<Boolean> = activeProfileIdFlow.flatMapLatest { profileId ->
        isProfileOnboardingCompletedFlow(profileId)
    }

    suspend fun saveSurvey(survey: com.example.data.local.entities.ProfileSurveyEntity): Long {
        ensureProfileExists(survey.profileId)
        val result = profileSurveyDao.insertOrUpdateSurvey(survey)
        preferences.recordProfileOnboardingCompleted(survey.profileId)
        return result
    }

    suspend fun skipSurvey(profileId: Long): Long {
        ensureProfileExists(profileId)
        val existing = profileSurveyDao.getSurveyForProfile(profileId)
        val skippedSurvey = existing?.copy(
            isSkipped = true,
            isCompleted = true,
            updatedAt = System.currentTimeMillis()
        ) ?: com.example.data.local.entities.ProfileSurveyEntity(
            profileId = profileId,
            isSkipped = true,
            isCompleted = true,
            completedAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val result = profileSurveyDao.insertOrUpdateSurvey(skippedSurvey)
        preferences.recordProfileOnboardingCompleted(profileId)
        return result
    }

    suspend fun updateProfile(profile: ProfileEntity) {
        profileDao.updateProfile(profile)
    }

    // --- Release 4 Collection 1: Per-Profile Security ---
    fun getProfileSecurityFlow(profileId: Long): Flow<com.example.data.local.entities.ProfileSecurityEntity?> =
        profileSecurityDao.getSecurityForProfileFlow(profileId)

    suspend fun getProfileSecurity(profileId: Long): com.example.data.local.entities.ProfileSecurityEntity? =
        profileSecurityDao.getSecurityForProfile(profileId)

    suspend fun saveProfileSecurity(security: com.example.data.local.entities.ProfileSecurityEntity) {
        profileSecurityDao.insertOrUpdateSecurity(security)
    }

    suspend fun updateFailedAttempts(profileId: Long, attempts: Int, lockoutUntil: Long) {
        profileSecurityDao.updateFailedAttempts(profileId, attempts, lockoutUntil)
    }

    suspend fun resetLockout(profileId: Long) {
        profileSecurityDao.resetLockout(profileId)
    }

    suspend fun deleteProfileSecurity(profileId: Long) {
        profileSecurityDao.deleteSecurityForProfile(profileId)
    }

    suspend fun hasAnyExistingUserData(): Boolean {
        return taskRepository.hasTasks() ||
                habitDao.getAllHabitsList().isNotEmpty() ||
                projectDao.getAllProjectsList().isNotEmpty()
    }

    // --- Release 3 Phase 6: Advanced Project Workspace (#20) ---
    fun getWorkspaceItemsForProjectFlow(projectId: Long): Flow<List<ProjectWorkspaceItem>> {
        return activeProfileIdFlow.flatMapLatest { profileId ->
            projectWorkspaceDao.getItemsForProjectFlow(projectId, profileId).map { entities ->
                entities.map { ProjectWorkspaceItem.fromEntity(it) }
            }
        }
    }

    suspend fun getWorkspaceItemsForProject(projectId: Long): List<ProjectWorkspaceItem> {
        val profileId = getActiveProfileId()
        return projectWorkspaceDao.getItemsForProjectList(projectId, profileId).map { ProjectWorkspaceItem.fromEntity(it) }
    }

    suspend fun insertWorkspaceItem(item: ProjectWorkspaceItem): Long {
        val profileId = getActiveProfileId()
        return projectWorkspaceDao.insertItem(item.copy(profileId = profileId).toEntity())
    }

    suspend fun updateWorkspaceItem(item: ProjectWorkspaceItem) {
        val profileId = getActiveProfileId()
        projectWorkspaceDao.updateItem(item.copy(profileId = profileId, updatedAt = System.currentTimeMillis()).toEntity())
    }

    suspend fun deleteWorkspaceItem(item: ProjectWorkspaceItem): Boolean {
        return projectWorkspaceDao.deleteItem(item.toEntity()) > 0
    }

    suspend fun deleteWorkspaceItemsByIds(ids: List<Long>): Int {
        return projectWorkspaceDao.deleteItemsByIds(ids)
    }

    suspend fun bulkUpdateWorkspaceItemStatus(ids: List<Long>, status: String) {
        val profileId = getActiveProfileId()
        ids.forEach { id ->
            val existing = projectWorkspaceDao.getItemById(id)
            if (existing != null && existing.profileId == profileId) {
                val progress = if (status == ProjectWorkspaceItemEntity.STATUS_DONE) 100 else if (status == ProjectWorkspaceItemEntity.STATUS_IN_PROGRESS && existing.progress == 0) 50 else existing.progress
                projectWorkspaceDao.updateItem(existing.copy(status = status, progress = progress, updatedAt = System.currentTimeMillis()))
            }
        }
    }

    suspend fun bulkUpdateWorkspaceItemVersion(ids: List<Long>, version: String) {
        val profileId = getActiveProfileId()
        ids.forEach { id ->
            val existing = projectWorkspaceDao.getItemById(id)
            if (existing != null && existing.profileId == profileId) {
                projectWorkspaceDao.updateItem(existing.copy(version = version, updatedAt = System.currentTimeMillis()))
            }
        }
    }

    suspend fun bulkUpdateWorkspaceItemCategory(ids: List<Long>, category: String) {
        val profileId = getActiveProfileId()
        ids.forEach { id ->
            val existing = projectWorkspaceDao.getItemById(id)
            if (existing != null && existing.profileId == profileId) {
                projectWorkspaceDao.updateItem(existing.copy(category = category, updatedAt = System.currentTimeMillis()))
            }
        }
    }

    fun getWorkspaceViewsForProjectFlow(projectId: Long): Flow<List<ProjectWorkspaceView>> {
        return activeProfileIdFlow.flatMapLatest { profileId ->
            projectWorkspaceDao.getViewsForProjectFlow(projectId, profileId).map { entities ->
                entities.map { ProjectWorkspaceView.fromEntity(it) }
            }
        }
    }

    suspend fun insertWorkspaceView(view: ProjectWorkspaceView): Long {
        val profileId = getActiveProfileId()
        return projectWorkspaceDao.insertView(view.copy(profileId = profileId).toEntity())
    }

    suspend fun updateWorkspaceView(view: ProjectWorkspaceView) {
        val profileId = getActiveProfileId()
        projectWorkspaceDao.updateView(view.copy(profileId = profileId, updatedAt = System.currentTimeMillis()).toEntity())
    }

    suspend fun deleteWorkspaceView(id: Long): Boolean {
        return projectWorkspaceDao.deleteViewById(id) > 0
    }

    suspend fun setDefaultWorkspaceView(projectId: Long, viewId: Long) {
        projectWorkspaceDao.clearDefaultViewsForProject(projectId)
        projectWorkspaceDao.setDefaultView(viewId)
    }

    suspend fun duplicateWorkspaceView(view: ProjectWorkspaceView): Long {
        val profileId = getActiveProfileId()
        val copy = view.copy(
            id = 0,
            profileId = profileId,
            name = "${view.name} (Copy)",
            isDefault = false,
            updatedAt = System.currentTimeMillis()
        )
        return projectWorkspaceDao.insertView(copy.toEntity())
    }

    fun getCustomStatusesForProjectFlow(projectId: Long): Flow<List<ProjectCustomStatus>> {
        return activeProfileIdFlow.flatMapLatest { profileId ->
            projectWorkspaceDao.getCustomStatusesForProjectFlow(projectId, profileId).map { entities ->
                entities.map { ProjectCustomStatus.fromEntity(it) }
            }
        }
    }

    suspend fun insertCustomStatus(status: ProjectCustomStatus): Long {
        val profileId = getActiveProfileId()
        return projectWorkspaceDao.insertCustomStatus(status.copy(profileId = profileId).toEntity())
    }

    suspend fun updateCustomStatus(status: ProjectCustomStatus) {
        val profileId = getActiveProfileId()
        projectWorkspaceDao.updateCustomStatus(status.copy(profileId = profileId).toEntity())
    }

    suspend fun deleteCustomStatus(id: Long): Boolean {
        return projectWorkspaceDao.deleteCustomStatusById(id) > 0
    }

    suspend fun deleteCustomStatusWithReassignment(
        projectId: Long,
        statusId: Long,
        deletedStatusName: String,
        replacementStatusName: String
    ): Boolean {
        projectWorkspaceDao.reassignItemStatus(projectId, deletedStatusName, replacementStatusName)
        return projectWorkspaceDao.deleteCustomStatusById(statusId) > 0
    }

    // --- Custom Properties (Release 3 Requirement #20) ---
    fun getCustomPropertiesForProjectFlow(projectId: Long): Flow<List<ProjectCustomProperty>> {
        return activeProfileIdFlow.flatMapLatest { profileId ->
            projectWorkspaceDao.getCustomPropertiesForProjectFlow(projectId, profileId).map { entities ->
                entities.map { ProjectCustomProperty.fromEntity(it) }
            }
        }
    }

    suspend fun getCustomPropertiesForProject(projectId: Long): List<ProjectCustomProperty> {
        val profileId = getActiveProfileId()
        return projectWorkspaceDao.getCustomPropertiesForProjectList(projectId, profileId).map {
            ProjectCustomProperty.fromEntity(it)
        }
    }

    suspend fun insertCustomProperty(property: ProjectCustomProperty): Long {
        val profileId = getActiveProfileId()
        return projectWorkspaceDao.insertCustomProperty(property.copy(profileId = profileId).toEntity())
    }

    suspend fun updateCustomProperty(property: ProjectCustomProperty) {
        val profileId = getActiveProfileId()
        projectWorkspaceDao.updateCustomProperty(property.copy(profileId = profileId).toEntity())
    }

    suspend fun deleteCustomProperty(id: Long): Boolean {
        return projectWorkspaceDao.deleteCustomPropertyById(id) > 0
    }

    suspend fun getAllWorkspaceItemsForExport(profileId: Long): List<ProjectWorkspaceItemEntity> {
        return projectWorkspaceDao.getAllItemsForProfile(profileId)
    }

    suspend fun getAllWorkspaceViewsForExport(profileId: Long): List<ProjectWorkspaceViewEntity> {
        return projectWorkspaceDao.getAllViewsForProfile(profileId)
    }

    suspend fun getAllCustomStatusesForExport(profileId: Long): List<ProjectCustomStatusEntity> {
        return projectWorkspaceDao.getAllCustomStatusesForProfile(profileId)
    }

    suspend fun getAllCustomPropertiesForExport(profileId: Long): List<ProjectCustomPropertyEntity> {
        return projectWorkspaceDao.getAllCustomPropertiesForProfile(profileId)
    }

    suspend fun restoreWorkspaceItems(items: List<ProjectWorkspaceItemEntity>) {
        projectWorkspaceDao.insertItems(items)
    }

    suspend fun restoreWorkspaceViews(views: List<ProjectWorkspaceViewEntity>) {
        views.forEach { projectWorkspaceDao.insertView(it) }
    }

    suspend fun restoreCustomStatuses(statuses: List<ProjectCustomStatusEntity>) {
        statuses.forEach { projectWorkspaceDao.insertCustomStatus(it) }
    }

    suspend fun restoreCustomProperties(properties: List<ProjectCustomPropertyEntity>) {
        properties.forEach { projectWorkspaceDao.insertCustomProperty(it) }
    }

    // --- Phase 3A Reactive Flows ---
    val allProjectsFlow: Flow<List<ProjectItem>> = activeProfileIdFlow.flatMapLatest { profileId ->
        combine(
            projectDao.getActiveProjectsForProfileFlow(profileId),
            taskRepository.getTasksForProfileFlow(profileId)
        ) { projects, tasks ->
            val counts = tasks.groupBy { it.projectId }
            projects.map { entity ->
                val projectTasks = counts[entity.id] ?: emptyList()
                val total = projectTasks.size
                val completed = projectTasks.count { it.isOneTimeCompleted }
                entity.toDomainModel(total, completed)
            }
        }
    }

    val archivedProjectsFlow: Flow<List<ProjectItem>> = activeProfileIdFlow.flatMapLatest { profileId ->
        projectDao.getArchivedProjectsForProfileFlow(profileId).map { list ->
            list.map { it.toDomainModel(0, 0) }
        }
    }

    val allLabelsFlow: Flow<List<LabelItem>> = activeProfileIdFlow.flatMapLatest { profileId ->
        combine(
            labelDao.getActiveLabelsForProfileFlow(profileId),
            labelDao.getAllCrossRefsFlow()
        ) { labels, crossRefs ->
            val counts = crossRefs.groupingBy { it.labelId }.eachCount()
            labels.map { it.toDomainModel(counts[it.id] ?: 0) }
        }
    }

    val archivedLabelsFlow: Flow<List<LabelItem>> = activeProfileIdFlow.flatMapLatest { profileId ->
        labelDao.getAllLabelsForProfileFlow(profileId).map { list ->
            list.filter { it.isArchived }.map { it.toDomainModel(0) }
        }
    }

    // ==========================================
    // --- TASK REPOSITORY METHODS ---
    // (Delegated to dedicated TaskRepository - P7-A4)
    // ==========================================

    val allTasksFlow: Flow<List<TaskItem>>
        get() = taskRepository.allTasksFlow

    fun getTasksForDateFlow(targetDateIso: String, includeArchived: Boolean = false): Flow<List<TaskItem>> {
        return taskRepository.getTasksForDateFlow(targetDateIso, includeArchived)
    }

    suspend fun insertTask(task: TaskItem): Long {
        return taskRepository.insertTask(task)
    }

    suspend fun updateTask(task: TaskItem) {
        taskRepository.updateTask(task)
    }

    suspend fun deleteTask(taskId: Long) {
        taskRepository.deleteTask(taskId)
    }

    suspend fun getTaskById(taskId: Long): TaskItem? {
        return taskRepository.getTaskById(taskId)
    }

    suspend fun saveTaskLabels(taskId: Long, labels: List<LabelItem>) {
        taskRepository.saveTaskLabels(taskId, labels)
    }

    suspend fun saveTaskReminders(taskId: Long, reminders: List<TaskReminderItem>) {
        taskRepository.saveTaskReminders(taskId, reminders)
    }

    suspend fun getOrCreateTaskForShortcut(
        title: String,
        emoji: String = "🔥",
        category: String = "Routines",
        recurrence: RecurrenceType = RecurrenceType.DAILY
    ): TaskItem {
        return taskRepository.getOrCreateTaskForShortcut(title, emoji, category, recurrence)
    }

    suspend fun getDuplicateDisciplineTasks(): List<TaskItem> {
        return taskRepository.getDuplicateDisciplineTasks()
    }

    suspend fun cleanupDuplicateDisciplineTasks(keepTaskId: Long? = null): Int {
        return taskRepository.cleanupDuplicateDisciplineTasks(keepTaskId)
    }

    // --- Phase 3A Project Operations ---
    suspend fun insertProject(project: ProjectItem): Long {
        val effectiveProfileId = if (project.profileId > 0) project.profileId else getActiveProfileId()
        return projectDao.insertProject(project.copy(profileId = effectiveProfileId).toEntity())
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
        // Safe deletion: move associated tasks to Inbox (projectId = null), unassign trackers & subscriptions
        projectDao.unassignTasksFromProject(projectId)
        trackerRepository.unassignTrackersFromProject(projectId)
        subscriptionRepository.unassignSubscriptionsFromProject(projectId)
        projectDao.deleteProjectById(projectId)
    }

    // --- Phase 3A Label Operations ---
    suspend fun insertLabel(label: LabelItem): Long {
        val effectiveProfileId = if (label.profileId > 0) label.profileId else getActiveProfileId()
        return labelDao.insertLabel(label.copy(profileId = effectiveProfileId).toEntity())
    }

    suspend fun getOrCreateLabel(profileId: Long, name: String): LabelItem {
        val effectiveProfileId = if (profileId > 0) profileId else getActiveProfileId()
        val trimmed = name.trim()
        val existing = labelDao.getAllLabelsForProfileList(effectiveProfileId).find { it.name.equals(trimmed, ignoreCase = true) }
        if (existing != null) {
            return existing.toDomainModel(0)
        }
        val newId = labelDao.insertLabel(
            LabelEntity(
                profileId = effectiveProfileId,
                name = trimmed,
                colorId = "blue",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
        return LabelItem(id = newId, profileId = effectiveProfileId, name = trimmed, colorId = "blue")
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
        val hasCompleted = preferences.hasCompletedOnboarding.first()
        val hasExistingData = taskRepository.hasTasks() || habitDao.getAllHabitsList().isNotEmpty()
        if (!hasCompleted && !hasExistingData) {
            // Fresh install starts with a blank canvas
            return
        }

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
        val entityBefore = taskRepository.getTaskById(taskId)
        val isCompleted = taskRepository.toggleTaskCompletion(taskId, dateIso) ?: return null

        // Gamification: Deterministic anti-farming XP reward or revocation with daily diminishing returns
        // Medication tasks are strictly excluded from gamification rewards
        val isMedicationTask = database.medicationDao().getMedicationByTaskId(taskId) != null
        if (!isMedicationTask && entityBefore != null) {
            val eventKey = GamificationEngine.createEventKey("TASK_COMPLETE", taskId.toString(), dateIso)
            if (isCompleted) {
                val dailyCount = gamificationDao.getDailyRewardedTaskCount(dateIso)
                val (xp, coins) = GamificationEngine.calculateTaskXpWithDiminishingReturns(
                    dailyCount,
                    entityBefore.priority
                )
                if (xp > 0 || coins > 0) {
                    awardXpAndCoins(eventKey, xp, coins, "Completed task: ${entityBefore.title}")
                }
                val scheduledToday = getTasksForDateFlow(dateIso).first()
                if (scheduledToday.isNotEmpty() && scheduledToday.all { it.isCompletedForDate }) {
                    checkAndUnlockAchievement("CLEAN_SLATE")
                }
            } else {
                revokeXpAndCoins(eventKey)
            }
            evaluateAchievements()
        }

        return isCompleted
    }

    suspend fun toggleSubtask(taskId: Long, subtaskId: String) {
        taskRepository.toggleSubtask(taskId, subtaskId)
    }

    /**
     * Reschedules an occurrence of a task from originalDateIso to newDateIso.
     */
    suspend fun rescheduleOccurrence(taskId: Long, originalDateIso: String, newDateIso: String) {
        taskRepository.rescheduleOccurrence(taskId, originalDateIso, newDateIso)
    }

    /**
     * Skips an occurrence of a recurring task on a specific date.
     */
    suspend fun skipOccurrence(taskId: Long, dateIso: String) {
        taskRepository.skipOccurrence(taskId, dateIso)
    }

    /**
     * Duplicates a task.
     */
    suspend fun duplicateTask(taskId: Long): Long {
        return taskRepository.duplicateTask(taskId)
    }

    /**
     * Archives or unarchives a task.
     */
    suspend fun setTaskArchived(taskId: Long, isArchived: Boolean) {
        taskRepository.setTaskArchived(taskId, isArchived)
    }

    /**
     * Pauses or resumes a recurring task.
     */
    suspend fun setTaskPaused(taskId: Long, isPaused: Boolean) {
        taskRepository.setTaskPaused(taskId, isPaused)
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
        taskRepository.bulkDeleteTasks(taskIds)
    }

    /**
     * Bulk archive tasks.
     */
    suspend fun bulkArchiveTasks(taskIds: List<Long>, isArchived: Boolean = true) {
        taskRepository.bulkArchiveTasks(taskIds, isArchived)
    }

    // --- Habits Flow ---
    val allHabitsFlow: Flow<List<HabitItem>> = activeProfileIdFlow.flatMapLatest { profileId ->
        habitDao.getHabitsForProfile(profileId).map { entities ->
            val todayStr = DateUtils.todayString()
            entities.map { it.toDomainModel(todayStr) }
        }
    }

    suspend fun insertHabit(habit: HabitItem): Long {
        val effectiveProfileId = if (habit.profileId > 0) habit.profileId else getActiveProfileId()
        return habitDao.insertHabit(habit.copy(profileId = effectiveProfileId).toEntity())
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

    suspend fun toggleHabitCompletion(habitId: Long, dateIso: String): Boolean {
        val entity = habitDao.getHabitById(habitId) ?: return false
        val completedDates = JsonMappers.parseStringList(entity.completedDatesJson).toMutableSet()
        val isNewlyCompleted = if (completedDates.contains(dateIso)) {
            completedDates.remove(dateIso)
            false
        } else {
            completedDates.add(dateIso)
            true
        }
        val updated = entity.copy(completedDatesJson = JsonMappers.serializeStringList(completedDates))
        habitDao.updateHabit(updated)

        // Gamification: Anti-farming event key & habit check-in XP
        val eventKey = GamificationEngine.createEventKey("HABIT_COMPLETE", habitId.toString(), dateIso)
        val allHabitsKey = GamificationEngine.createEventKey("HABITS_ALL_COMPLETE", "all", dateIso)
        if (isNewlyCompleted) {
            awardXpAndCoins(
                eventKey,
                GamificationEngine.XP_HABIT_CHECKIN,
                GamificationEngine.COINS_HABIT_CHECKIN,
                "Checked in habit: ${entity.title}"
            )
            val allHabits = habitDao.getAllHabitsList()
            val allDoneToday = allHabits.isNotEmpty() && allHabits.all { h ->
                if (h.id == habitId) true else JsonMappers.parseStringList(h.completedDatesJson).contains(dateIso)
            }
            if (allDoneToday) {
                awardXpAndCoins(
                    allHabitsKey,
                    GamificationEngine.XP_ALL_HABITS_BONUS,
                    GamificationEngine.COINS_ALL_HABITS_BONUS,
                    "Completed all habits for today!"
                )
            }
        } else {
            revokeXpAndCoins(eventKey)
            revokeXpAndCoins(allHabitsKey)
        }
        evaluateAchievements()

        return isNewlyCompleted
    }

    // --- 75-Day Challenge Flow ---
    val challengeStateFlow: Flow<ChallengeState> = challengeDao.getActiveChallengeFlow().map { entity ->
        if (entity == null) {
            createDefaultChallengeState()
        } else {
            entity.toDomainModel()
        }
    }

    private val challengeMutex = Mutex()

    suspend fun toggleChallengeRule(ruleId: String, dateIso: String): Boolean = challengeMutex.withLock {
        var entity = challengeDao.getActiveChallenge()
        if (entity == null) {
            val defaultState = createDefaultChallengeState()
            entity = defaultState.toEntity()
            challengeDao.insertOrUpdateChallenge(entity)
        }

        val logs = JsonMappers.parseChallengeDailyLogs(entity.dailyLogsJson).toMutableMap()
        val currentLog = logs[dateIso] ?: ChallengeDailyLog(dateIso = dateIso)
        val completedSet = currentLog.completedRuleIds.toMutableSet()

        val isNewlyCompleted = if (completedSet.contains(ruleId)) {
            completedSet.remove(ruleId)
            false
        } else {
            completedSet.add(ruleId)
            true
        }

        val updatedLog = currentLog.copy(completedRuleIds = completedSet)
        logs[dateIso] = updatedLog

        val updatedEntity = entity.copy(
            dailyLogsJson = JsonMappers.serializeChallengeDailyLogs(logs)
        )
        challengeDao.insertOrUpdateChallenge(updatedEntity)

        // Gamification: Anti-farming event key & challenge objective XP
        val ruleEventKey = GamificationEngine.createEventKey("CHALLENGE_RULE", ruleId, dateIso)
        if (isNewlyCompleted) {
            awardXpAndCoins(
                ruleEventKey,
                GamificationEngine.XP_CHALLENGE_OBJECTIVE,
                GamificationEngine.COINS_CHALLENGE_OBJECTIVE,
                "Completed challenge objective: $ruleId"
            )
        } else {
            revokeXpAndCoins(ruleEventKey)
        }
        evaluateAchievements()

        isNewlyCompleted
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

    suspend fun updateWaterGoal(goalMl: Int, dateIso: String) {
        preferences.setWaterGoalMl(goalMl)
        val current = waterLogDao.getWaterLog(dateIso)
        if (current != null) {
            waterLogDao.insertOrUpdateWaterLog(current.copy(goalMl = goalMl, updatedAt = System.currentTimeMillis()))
        } else {
            waterLogDao.insertOrUpdateWaterLog(
                WaterLogEntity(
                    dateIso = dateIso,
                    amountMl = 0,
                    goalMl = goalMl,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
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
                title = "Daily Yoga & Flexibility",
                emoji = "🧘",
                description = "Surya Namaskaram, physical postures and dynamic stretching",
                category = "Habits & Daily Routine",
                priority = TaskPriority.HIGH,
                dueDateIso = todayIso,
                dueTime = "06:30",
                durationMinutes = 30,
                recurrenceType = RecurrenceType.DAILY,
                isReminderEnabled = true
            ),
            TaskItem(
                title = "Mindful Meditation & Stillness",
                emoji = "🧘",
                description = "Mindful breathing, mental clarity and inner focus",
                category = "Habits & Daily Routine",
                priority = TaskPriority.HIGH,
                dueDateIso = todayIso,
                dueTime = "07:00",
                durationMinutes = 20,
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
            taskRepository.insertTask(task)
        }

        // 3. Initialize 75-Day Challenge
        restartChallenge(newStartDateIso = todayIso)

        preferences.setHasInstalledDefaultRoutines(true)
    }

    // --- Entity Domain Mappings ---

    private fun ProjectEntity.toDomainModel(taskCount: Int = 0, completedCount: Int = 0): ProjectItem {
        return ProjectItem(
            id = id,
            profileId = profileId,
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
            profileId = profileId,
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

    private fun LabelItem.toEntity(): LabelEntity {
        return LabelEntity(
            id = id,
            profileId = profileId,
            name = name,
            colorId = colorId,
            isFavorite = isFavorite,
            isArchived = isArchived,
            orderIndex = orderIndex,
            createdAt = createdAt,
            updatedAt = System.currentTimeMillis()
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
            profileId = profileId,
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
            profileId = profileId,
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
    val allHoneycombShortcutsFlow: Flow<List<HoneycombShortcut>> = activeProfileIdFlow.flatMapLatest { profileId ->
        honeycombShortcutDao.getShortcutsForProfile(profileId).map { list ->
            list.map { it.toDomainModel() }
        }
    }

    suspend fun getHoneycombShortcutsList(): List<HoneycombShortcut> {
        val activeProfileId = getActiveProfileId()
        return honeycombShortcutDao.getShortcutsForProfileList(activeProfileId).map { it.toDomainModel() }
    }

    suspend fun insertHoneycombShortcut(shortcut: HoneycombShortcut): Long {
        val effectiveProfileId = if (shortcut.profileId > 0) shortcut.profileId else getActiveProfileId()
        return honeycombShortcutDao.insertShortcut(shortcut.copy(profileId = effectiveProfileId).toEntity())
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
        // Do not auto-install template shortcuts on fresh install or upgrade.
        // An empty Room database must remain empty across Home and widget.
        if (existing.isNotEmpty()) {
            // Check for any duplicate / overlapping coordinates and de-overlap them
            val occupied = mutableSetOf<Pair<Int, Int>>()
            var nextR = 0
            fun findNextFree(): Pair<Int, Int> {
                while (nextR < 100) {
                    val rowLen = if (nextR % 2 == 0) 3 else 2
                    for (q in 0 until rowLen) {
                        val candidate = Pair(q, nextR)
                        if (candidate !in occupied) {
                            return candidate
                        }
                    }
                    nextR++
                }
                return Pair(0, (occupied.maxOfOrNull { it.second } ?: 0) + 1)
            }

            existing.forEach { entity ->
                val coord = Pair(entity.coordQ, entity.coordR)
                if (coord !in occupied) {
                    occupied.add(coord)
                } else {
                    val freeCoord = findNextFree()
                    occupied.add(freeCoord)
                    honeycombShortcutDao.updateCoords(entity.id, freeCoord.first, freeCoord.second, entity.orderIndex)
                }
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
        var shortcuts = honeycombShortcutDao.getAllShortcutsList()
        if (shortcuts.isEmpty()) {
            val defaultCoords = HoneycombLayoutTemplate.generateCoordinates(template, DefaultHoneycombShortcuts.list.size)
            val entities = DefaultHoneycombShortcuts.list.mapIndexed { index, item ->
                val (q, r) = defaultCoords.getOrElse(index) { Pair(index % 3, index / 3) }
                item.copy(orderIndex = index, coordQ = q, coordR = r).toEntity()
            }
            honeycombShortcutDao.insertShortcuts(entities)
            return
        }
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
    val allTrashItemsFlow: Flow<List<TrashItem>> = activeProfileIdFlow.flatMapLatest { profileId ->
        trashDao.getTrashItemsForProfile(profileId).map { list ->
            list.map { it.toDomainModel() }
        }
    }

    private suspend fun getTrashRetentionMillis(): Long {
        val days = runCatching { preferences.trashRetentionDays.first() }.getOrDefault(30).toLong().coerceIn(1L, 365L)
        return days * 24L * 60L * 60L * 1000L
    }

    suspend fun softDeleteTask(task: TaskItem) {
        taskRepository.softDeleteTask(task, getTrashRetentionMillis())
    }

    suspend fun softDeleteHabit(habit: HabitItem) {
        val effectiveProfileId = if (habit.profileId > 0) habit.profileId else getActiveProfileId()
        val payload = JSONObject().apply {
            put("id", habit.id)
            put("title", habit.title)
            put("emoji", habit.emoji)
            put("description", habit.description)
            put("reminderTime", habit.reminderTime)
            put("colorHex", habit.colorHex)
            put("profileId", effectiveProfileId)
        }.toString()
        trashDao.insertTrashItem(
            TrashItemEntity(
                profileId = effectiveProfileId,
                itemType = "HABIT",
                originalId = habit.id,
                title = habit.title,
                subtitle = "Habit item",
                originalPayloadJson = payload,
                expiresAt = System.currentTimeMillis() + getTrashRetentionMillis()
            )
        )
        habitDao.deleteHabitById(habit.id)
    }

    suspend fun softDeleteProject(project: ProjectItem) {
        val effectiveProfileId = if (project.profileId > 0) project.profileId else getActiveProfileId()
        val payload = JSONObject().apply {
            put("id", project.id)
            put("name", project.name)
            put("description", project.description)
            put("colorId", project.colorId)
            put("emoji", project.emoji)
            put("profileId", effectiveProfileId)
        }.toString()
        trashDao.insertTrashItem(
            TrashItemEntity(
                profileId = effectiveProfileId,
                itemType = "PROJECT",
                originalId = project.id,
                title = project.name,
                subtitle = "Project folder",
                originalPayloadJson = payload,
                expiresAt = System.currentTimeMillis() + getTrashRetentionMillis()
            )
        )
        projectDao.deleteProjectById(project.id)
    }

    suspend fun softDeleteLabel(label: LabelItem) {
        val effectiveProfileId = if (label.profileId > 0) label.profileId else getActiveProfileId()
        val payload = JSONObject().apply {
            put("id", label.id)
            put("name", label.name)
            put("colorId", label.colorId)
            put("profileId", effectiveProfileId)
        }.toString()
        trashDao.insertTrashItem(
            TrashItemEntity(
                profileId = effectiveProfileId,
                itemType = "LABEL",
                originalId = label.id,
                title = "#${label.name}",
                subtitle = "Label tag",
                originalPayloadJson = payload,
                expiresAt = System.currentTimeMillis() + getTrashRetentionMillis()
            )
        )
        labelDao.deleteLabelById(label.id)
    }

    suspend fun restoreTrashItem(id: Long): Boolean {
        val item = trashDao.getTrashItemById(id) ?: return false
        try {
            val json = JSONObject(item.originalPayloadJson)
            val effectiveProfileId = item.profileId
            when (item.itemType) {
                "TASK" -> {
                    taskRepository.insertTaskEntity(
                        TaskEntity(
                            id = json.optLong("id", 0L),
                            profileId = effectiveProfileId,
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
                            profileId = effectiveProfileId,
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
                            profileId = effectiveProfileId,
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
                            profileId = effectiveProfileId,
                            name = json.getString("name"),
                            colorId = json.optString("colorId", "charcoal")
                        )
                    )
                }
            }
            trashDao.deleteTrashItemById(id)
            return true
        } catch (e: Exception) {
            android.util.Log.e("LifeRepository", "Failed to restore trash item $id: ${e.message}")
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
    val allPlansFlow: Flow<List<PlanItem>> = activeProfileIdFlow.flatMapLatest { profileId ->
        planDao.getPlansForProfile(profileId).map { list ->
            list.map { planEntity ->
                val milestones = planDao.getMilestonesForPlanList(planEntity.id).map {
                    PlanMilestone(it.id, it.planId, it.title, it.isCompleted, it.orderIndex)
                }
                val linkedTaskIds = planDao.getTaskIdsForPlan(planEntity.id)
                planEntity.toDomainModel(milestones, linkedTaskIds)
            }
        }
    }

    fun getPlansByLevel(level: PlanLevel): Flow<List<PlanItem>> = activeProfileIdFlow.flatMapLatest { profileId ->
        planDao.getPlansByLevelForProfile(profileId, level.name).map { list ->
            list.map { planEntity ->
                val milestones = planDao.getMilestonesForPlanList(planEntity.id).map {
                    PlanMilestone(it.id, it.planId, it.title, it.isCompleted, it.orderIndex)
                }
                val linkedTaskIds = planDao.getTaskIdsForPlan(planEntity.id)
                planEntity.toDomainModel(milestones, linkedTaskIds)
            }
        }
    }

    suspend fun insertPlan(plan: PlanItem, milestoneTitles: List<String>, linkedTaskIds: List<Long>): Long {
        val effectiveProfileId = if (plan.profileId > 0) plan.profileId else getActiveProfileId()
        val planId = planDao.insertPlan(plan.copy(profileId = effectiveProfileId).toEntity())
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

    suspend fun updatePlan(
        plan: PlanItem,
        milestoneTitles: List<String>? = null,
        linkedTaskIds: List<Long>? = null
    ) {
        planDao.updatePlan(plan.toEntity())
        if (milestoneTitles != null) {
            planDao.deleteMilestonesForPlan(plan.id)
            val newMilestones = milestoneTitles.mapIndexed { idx, title ->
                PlanMilestoneEntity(planId = plan.id, title = title, isCompleted = false, orderIndex = idx)
            }
            planDao.insertMilestones(newMilestones)
        }
        if (linkedTaskIds != null) {
            planDao.clearTasksForPlan(plan.id)
            val refs = linkedTaskIds.map { PlanTaskCrossRef(planId = plan.id, taskId = it) }
            planDao.insertPlanTaskCrossRefs(refs)
        }
    }

    suspend fun toggleMilestoneCompletion(milestoneId: Long, isCompleted: Boolean) {
        planDao.updateMilestoneCompletion(milestoneId, isCompleted)
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

    val habitStreaksFlow: Flow<StreakResult> = habitDao.getAllHabits().map { habits ->
        val allDates = habits.flatMap { JsonMappers.parseStringList(it.completedDatesJson) }.toSet()
        GamificationEngine.calculateStreaks(allDates, DateUtils.todayString())
    }

    val taskStreaksFlow: Flow<StreakResult> get() = taskRepository.taskStreaksFlow

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

        val profile = gamificationDao.getProfile() ?: GamificationProfileEntity(1, 0, 50, 1, "E")
        val newXp = profile.currentXp + xp
        val newCoins = profile.currentCoins + coins
        val level = GamificationEngine.calculateLevelFromXp(newXp)
        val rank = GamificationEngine.calculateHunterRank(level).rankCode

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

    suspend fun revokeXpAndCoins(eventKey: String): Boolean {
        val existing = gamificationDao.getXpEvent(eventKey) ?: return false
        gamificationDao.deleteXpEvent(eventKey)
        val profile = gamificationDao.getProfile() ?: return false
        val newXp = (profile.currentXp - existing.xpAwarded).coerceAtLeast(0L)
        val newCoins = (profile.currentCoins - existing.coinsAwarded).coerceAtLeast(0)
        val level = GamificationEngine.calculateLevelFromXp(newXp)
        val rank = GamificationEngine.calculateHunterRank(level).rankCode

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

    suspend fun evaluateAchievements() {
        // 1. Task completions
        val allTasks = taskRepository.getAllTasksList()
        val totalCompletedTasks = allTasks.count { it.isOneTimeCompleted } + allTasks.sumOf {
            JsonMappers.parseStringList(it.completedDatesJson).size
        }
        if (totalCompletedTasks >= 1) checkAndUnlockAchievement("FIRST_TASK")
        if (totalCompletedTasks >= 10) checkAndUnlockAchievement("TASK_10")
        if (totalCompletedTasks >= 50) checkAndUnlockAchievement("TASK_50")
        if (totalCompletedTasks >= 100) checkAndUnlockAchievement("TASK_100")

        // 2. Habit streaks
        val allHabits = habitDao.getAllHabitsList()
        val allHabitDates = allHabits.flatMap {
            JsonMappers.parseStringList(it.completedDatesJson)
        }.toSet()
        val streakInfo = GamificationEngine.calculateStreaks(allHabitDates)
        val maxStreak = maxOf(streakInfo.currentStreak, streakInfo.longestStreak)
        if (maxStreak >= 3) checkAndUnlockAchievement("STREAK_3")
        if (maxStreak >= 7) checkAndUnlockAchievement("STREAK_7")
        if (maxStreak >= 30) checkAndUnlockAchievement("STREAK_30")

        // 3. Hydration
        val allWaterLogs = waterLogDao.getAllWaterLogsList()
        val goalMetCount = allWaterLogs.count { it.amountMl >= it.goalMl && it.goalMl > 0 }
        if (goalMetCount >= 5) checkAndUnlockAchievement("HYDRATION_HERO")

        // 4. Challenge
        val challenge = challengeDao.getActiveChallenge()
        if (challenge != null) {
            val logs = JsonMappers.parseChallengeDailyLogs(challenge.dailyLogsJson)
            val rules = JsonMappers.parseChallengeRules(challenge.rulesJson)
            val ruleIds = rules.map { it.id }.toSet()
            val successfulDays = logs.values.count { log ->
                ruleIds.isNotEmpty() && log.completedRuleIds.containsAll(ruleIds)
            }
            if (successfulDays >= 7) checkAndUnlockAchievement("CHALLENGE_DAY_7")
            if (successfulDays >= 30) checkAndUnlockAchievement("CHALLENGE_DAY_30")
            if (successfulDays >= 75) checkAndUnlockAchievement("CHALLENGE_COMPLETE")
        }

        // 5. Plans
        val planCount = planDao.getAllPlansList().size
        if (planCount >= 1) checkAndUnlockAchievement("PLANNER_INITIATE")

        // 6. Templates
        val installedTemplates = preferences.installedTemplateIds.first()
        if (installedTemplates.isNotEmpty()) checkAndUnlockAchievement("TEMPLATE_MASTER")

        // 7. Level 10
        val profile = gamificationDao.getProfile()
        if (profile != null && profile.currentLevel >= 10) checkAndUnlockAchievement("LEVEL_10")
    }

    suspend fun resetGamificationProgress() {
        gamificationDao.insertOrUpdateProfile(
            GamificationProfileEntity(
                id = 1,
                currentXp = 0L,
                currentCoins = 50,
                currentLevel = 1,
                hunterRank = "E",
                updatedAt = System.currentTimeMillis()
            )
        )
        // Reset achievements to locked state
        val defaultAchievements = DefaultAchievements.list.map {
            AchievementEntity(
                code = it.code,
                title = it.title,
                description = it.description,
                tier = it.tier,
                xpReward = it.xpReward,
                coinsReward = it.coinsReward,
                isUnlocked = false,
                unlockedAt = null
            )
        }
        gamificationDao.insertAchievements(defaultAchievements)
    }

    suspend fun installTemplate(
        template: LifeTemplate,
        selectedTaskIndices: Set<Int> = template.tasks.indices.toSet(),
        selectedHabitIndices: Set<Int> = template.habits.indices.toSet()
    ): TemplateImportSummary {
        val todayIso = DateUtils.todayString()
        val activeProfileId = getActiveProfileId()
        var tasksAdded = 0
        var habitsAdded = 0
        var projectsAdded = 0
        var shortcutsAdded = 0
        var challengeConfigured = false

        database.withTransaction {
            // 1. Projects
            val existingProjects = projectDao.getAllProjectsForProfileList(activeProfileId)
            val projectNameToId = mutableMapOf<String, Long>()
            existingProjects.forEach { p ->
                projectNameToId[p.name.trim().lowercase()] = p.id
            }

            // Ensure all projects declared in template.projects exist
            template.projects.forEach { projDef ->
                val key = projDef.name.trim().lowercase()
                if (!projectNameToId.containsKey(key)) {
                    val newId = projectDao.insertProject(
                        ProjectEntity(
                            profileId = activeProfileId,
                            name = projDef.name,
                            description = projDef.description,
                            colorId = projDef.colorId,
                            emoji = projDef.emoji
                        )
                    )
                    projectNameToId[key] = newId
                    projectsAdded++
                }
            }

            // Ensure any project referenced by tasks also exists
            template.tasks.forEach { taskDef ->
                taskDef.projectName?.let { pName ->
                    val key = pName.trim().lowercase()
                    if (key.isNotBlank() && !projectNameToId.containsKey(key)) {
                        val newId = projectDao.insertProject(
                            ProjectEntity(
                                profileId = activeProfileId,
                                name = pName.trim(),
                                description = "",
                                colorId = "blue",
                                emoji = "📁"
                            )
                        )
                        projectNameToId[key] = newId
                        projectsAdded++
                    }
                }
            }

            // 2. Labels
            val existingLabels = labelDao.getAllLabelsForProfileList(activeProfileId)
            val labelNameToId = mutableMapOf<String, Long>()
            existingLabels.forEach { l ->
                labelNameToId[l.name.trim().lowercase()] = l.id
            }

            // Ensure all labels referenced in template tasks exist
            template.tasks.forEach { taskDef ->
                taskDef.labels.forEach { labelName ->
                    val key = labelName.trim().lowercase()
                    if (key.isNotBlank() && !labelNameToId.containsKey(key)) {
                        val newLabelId = labelDao.insertLabel(
                            LabelEntity(
                                profileId = activeProfileId,
                                name = labelName.trim(),
                                colorId = "blue",
                                isFavorite = false,
                                isArchived = false,
                                orderIndex = existingLabels.size + labelNameToId.size,
                                createdAt = System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                        labelNameToId[key] = newLabelId
                    }
                }
            }

            // 3. Tasks
            val existingTasks = taskRepository.getTasksForProfileList(activeProfileId)
            template.tasks.forEachIndexed { index, taskDef ->
                if (index in selectedTaskIndices) {
                    val isDuplicate = existingTasks.any { it.title.equals(taskDef.title, ignoreCase = true) }
                    if (!isDuplicate) {
                        val targetProjectName = taskDef.projectName?.trim()
                            ?: if (template.projects.size == 1) template.projects.first().name.trim() else null
                        val taskProjectId = targetProjectName?.let { projectNameToId[it.lowercase()] }

                        val newTaskId = taskRepository.insertTaskEntity(
                            TaskEntity(
                                profileId = activeProfileId,
                                title = taskDef.title,
                                emoji = taskDef.emoji,
                                description = taskDef.description,
                                category = taskDef.category,
                                priority = taskDef.priority.name,
                                dueDateIso = todayIso,
                                dueTime = taskDef.dueTime,
                                durationMinutes = taskDef.durationMinutes,
                                schedulingMode = taskDef.schedulingMode,
                                recurrenceType = taskDef.recurrenceType.name,
                                recurrenceDaysOfWeek = taskDef.recurrenceDaysOfWeek.joinToString(","),
                                isReminderEnabled = taskDef.isReminderEnabled,
                                projectId = taskProjectId,
                                createdAt = System.currentTimeMillis()
                            )
                        )
                        tasksAdded++

                        if (taskDef.labels.isNotEmpty()) {
                            val crossRefs = taskDef.labels.mapNotNull { labelName ->
                                val key = labelName.trim().lowercase()
                                labelNameToId[key]?.let { lId ->
                                    TaskLabelCrossRef(taskId = newTaskId, labelId = lId)
                                }
                            }
                            if (crossRefs.isNotEmpty()) {
                                labelDao.insertTaskLabelCrossRefs(crossRefs)
                            }
                        }
                    }
                }
            }

            // 4. Habits
            val existingHabits = habitDao.getHabitsForProfileList(activeProfileId)
            template.habits.forEachIndexed { index, habitDef ->
                if (index in selectedHabitIndices) {
                    val isDuplicate = existingHabits.any { it.title.equals(habitDef.title, ignoreCase = true) }
                    if (!isDuplicate) {
                        habitDao.insertHabit(
                            HabitEntity(
                                profileId = activeProfileId,
                                title = habitDef.title,
                                emoji = habitDef.emoji,
                                description = habitDef.description,
                                reminderTime = habitDef.reminderTime,
                                colorHex = habitDef.colorHex,
                                completedDatesJson = "[]",
                                orderIndex = existingHabits.size + habitsAdded,
                                createdAt = System.currentTimeMillis()
                            )
                        )
                        habitsAdded++
                    }
                }
            }

            // 4. Challenge if specified
            template.challenge?.let { chalDef ->
                val rules = chalDef.rules.map { ruleDef ->
                    ChallengeRule(id = ruleDef.id, title = ruleDef.title, emoji = ruleDef.emoji, description = ruleDef.description)
                }
                restartChallenge(todayIso, rules)
                challengeConfigured = true
            }

            // 5. Honeycomb shortcuts
            val existingShortcuts = honeycombShortcutDao.getShortcutsForProfileList(activeProfileId)
            template.shortcuts.forEach { scDef ->
                val alreadyExists = existingShortcuts.any { it.title.equals(scDef.title, ignoreCase = true) }
                if (!alreadyExists && existingShortcuts.size + shortcutsAdded < 19) {
                    val nextOrder = existingShortcuts.size + shortcutsAdded
                    var resolvedTargetId = scDef.targetId
                    var projectColorLong: Long? = null
                    var isLinkedToProject = false

                    when (scDef.type) {
                        ShortcutType.TASK -> {
                            val allT = taskRepository.getTasksForProfileList(activeProfileId)
                            val matched = allT.find { it.id.toString() == scDef.targetId }
                                ?: allT.find { it.title.equals(scDef.targetId, ignoreCase = true) }
                                ?: allT.find { it.title.equals(scDef.title, ignoreCase = true) }
                                ?: allT.find { it.title.contains(scDef.targetId, ignoreCase = true) }
                                ?: allT.find { it.title.contains(scDef.title, ignoreCase = true) }
                                ?: allT.find {
                                    val words = it.title.split(Regex("[^A-Za-z0-9]")).filter { w -> w.isNotBlank() }
                                    words.any { w -> w.equals(scDef.targetId, ignoreCase = true) || w.equals(scDef.title, ignoreCase = true) }
                                }
                            if (matched != null) {
                                resolvedTargetId = matched.id.toString()
                                if (matched.projectId != null) {
                                    val project = projectDao.getProjectById(matched.projectId)
                                    if (project != null) {
                                        projectColorLong = TodoistColorPalette.getColorLong(project.colorId)
                                        isLinkedToProject = true
                                    }
                                }
                            }
                        }
                        ShortcutType.HABIT -> {
                            val allH = habitDao.getHabitsForProfileList(activeProfileId)
                            val matched = allH.find { it.id.toString() == scDef.targetId }
                                ?: allH.find { it.title.equals(scDef.targetId, ignoreCase = true) }
                                ?: allH.find { it.title.equals(scDef.title, ignoreCase = true) }
                                ?: allH.find { it.title.contains(scDef.targetId, ignoreCase = true) }
                                ?: allH.find { it.title.contains(scDef.title, ignoreCase = true) }
                            if (matched != null) {
                                resolvedTargetId = matched.id.toString()
                            }
                        }
                        ShortcutType.CHALLENGE_RULE -> {
                            val activeChallenge = challengeDao.getActiveChallenge()
                            val rules = activeChallenge?.let { JsonMappers.parseChallengeRules(it.rulesJson) } ?: emptyList()
                            val matchedRule = rules.find {
                                it.id == scDef.targetId ||
                                it.title.equals(scDef.targetId, ignoreCase = true) ||
                                it.title.equals(scDef.title, ignoreCase = true)
                            }
                            if (matchedRule != null) {
                                resolvedTargetId = matchedRule.id
                            }
                        }
                        ShortcutType.WATER -> {
                            resolvedTargetId = "water"
                        }
                        else -> {
                            resolvedTargetId = scDef.targetId
                        }
                    }

                    val hasValidCustomColor = scDef.customColorHex != null && (scDef.customColorHex and 0xFF000000L) != 0L
                    val resolvedColorSource = when {
                        scDef.colorSource != null -> scDef.colorSource
                        hasValidCustomColor -> "CUSTOM"
                        isLinkedToProject -> "PROJECT"
                        scDef.type == ShortcutType.WATER -> "CUSTOM"
                        scDef.type == ShortcutType.CHALLENGE_RULE -> "CUSTOM"
                        else -> "PROJECT"
                    }

                    val resolvedCustomColor = when {
                        hasValidCustomColor -> scDef.customColorHex
                        scDef.type == ShortcutType.WATER -> 0xFF3B82F6L
                        scDef.type == ShortcutType.CHALLENGE_RULE -> 0xFFF59E0BL
                        isLinkedToProject -> projectColorLong
                        else -> null
                    }

                    honeycombShortcutDao.insertShortcut(
                        HoneycombShortcutEntity(
                            profileId = activeProfileId,
                            type = scDef.type.name,
                            targetId = resolvedTargetId,
                            title = scDef.title,
                            emoji = scDef.emoji,
                            customColorHex = resolvedCustomColor,
                            colorSource = resolvedColorSource,
                            shape = scDef.shape,
                            orderIndex = nextOrder,
                            coordQ = scDef.coordQ,
                            coordR = scDef.coordR
                        )
                    )
                    shortcutsAdded++
                }
            }
        }

        // Record installed template ID
        preferences.addInstalledTemplateId(template.id)
        preferences.setHasCompletedOnboarding(true)

        // Award Template XP
        awardXpAndCoins(
            eventKey = "TEMPLATE_INSTALL:${template.id}",
            xp = GamificationEngine.XP_TEMPLATE_INSTALLED,
            coins = GamificationEngine.COINS_TEMPLATE_INSTALLED,
            description = "Installed template: ${template.title}"
        )

        evaluateAchievements()

        return TemplateImportSummary(
            templateId = template.id,
            templateTitle = template.title,
            tasksImported = tasksAdded,
            habitsImported = habitsAdded,
            shortcutsImported = shortcutsAdded,
            projectsImported = projectsAdded,
            challengeConfigured = challengeConfigured,
            xpAwarded = GamificationEngine.XP_TEMPLATE_INSTALLED
        )
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
        profileId = profileId,
        itemType = itemType,
        originalId = originalId,
        title = title,
        subtitle = subtitle,
        originalPayloadJson = originalPayloadJson,
        deletedAt = deletedAt,
        expiresAt = expiresAt
    )

    private fun PlanEntity.toDomainModel(
        milestones: List<PlanMilestone> = emptyList(),
        linkedTaskIds: List<Long> = emptyList()
    ) = PlanItem(
        id = id,
        profileId = profileId,
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
        milestones = milestones,
        linkedTaskIds = linkedTaskIds,
        createdAt = createdAt
    )

    private fun PlanItem.toEntity() = PlanEntity(
        id = id,
        profileId = profileId,
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

    suspend fun updateShortcutCustomTileName(id: Long, customTileName: String?) {
        honeycombShortcutDao.updateCustomTileName(id, customTileName?.trim()?.ifBlank { null })
    }

    suspend fun updateShortcutShape(id: Long, shape: String) {
        honeycombShortcutDao.updateShape(id, shape)
    }

    suspend fun updateAllShortcutShapes(shape: String) {
        honeycombShortcutDao.updateAllShapes(shape)
    }

    suspend fun updateShortcutColorSource(id: Long, colorSource: String, customColorHex: Long?) {
        honeycombShortcutDao.updateColorSource(id, customColorHex, colorSource)
    }

    suspend fun ensureTaskQuickReminderShortcut(task: TaskItem): Boolean {
        val existing = honeycombShortcutDao.getAllShortcutsList()
        val match = existing.firstOrNull { it.type == "TASK" && it.targetId == task.id.toString() }
        val projectColor = task.projectId?.let { projectDao.getProjectById(it) }?.let { TodoistColorPalette.getColorLong(it.colorId) }

        if (task.isQuickReminder) {
            if (match != null) {
                val newColor = if (match.colorSource == "PROJECT") projectColor else match.customColorHex
                honeycombShortcutDao.updateShortcut(
                    match.copy(
                        title = task.title,
                        emoji = if (task.emoji.isNotBlank()) task.emoji else match.emoji,
                        customColorHex = newColor
                    )
                )
                return true
            }
            if (existing.size >= 19) {
                return false
            }
            val occupied = existing.map { Pair(it.coordQ, it.coordR) }.toSet()
            var targetQ = 0
            var targetR = 0
            var found = false
            for (r in 0 until 50) {
                val rowLen = if (r % 2 == 0) 3 else 2
                for (q in 0 until rowLen) {
                    if (!occupied.contains(Pair(q, r))) {
                        targetQ = q
                        targetR = r
                        found = true
                        break
                    }
                }
                if (found) break
            }
            honeycombShortcutDao.insertShortcut(
                HoneycombShortcutEntity(
                    type = "TASK",
                    targetId = task.id.toString(),
                    title = task.title,
                    emoji = if (task.emoji.isNotBlank()) task.emoji else "⚡",
                    customColorHex = projectColor,
                    colorSource = if (projectColor != null) "PROJECT" else "THEME_DEFAULT",
                    shape = "HEXAGON",
                    orderIndex = existing.size,
                    coordQ = targetQ,
                    coordR = targetR,
                    profileId = task.profileId
                )
            )
            return true
        } else {
            if (match != null) {
                honeycombShortcutDao.deleteShortcutById(match.id)
            }
            return true
        }
    }

    private fun HoneycombShortcutEntity.toDomainModel(): HoneycombShortcut {
        return HoneycombShortcut(
            id = id,
            profileId = profileId,
            type = ShortcutType.fromString(type),
            targetId = targetId,
            title = title,
            customTileName = customTileName,
            emoji = emoji,
            customColorHex = customColorHex,
            colorSource = colorSource,
            shape = shape,
            orderIndex = orderIndex,
            coordQ = coordQ,
            coordR = coordR,
            isPinned = isPinned
        )
    }

    private fun HoneycombShortcut.toEntity(): HoneycombShortcutEntity {
        return HoneycombShortcutEntity(
            id = id,
            profileId = profileId,
            type = type.name,
            targetId = targetId,
            title = title,
            customTileName = customTileName,
            emoji = emoji,
            customColorHex = customColorHex,
            colorSource = colorSource,
            shape = shape,
            orderIndex = orderIndex,
            coordQ = coordQ,
            coordR = coordR,
            isPinned = isPinned
        )
    }

    // ==========================================
    // MEDICATION OPERATIONS
    // ==========================================

    fun getAllMedicationsFlow(): Flow<List<MedicationItem>> {
        return activeProfileIdFlow.flatMapLatest { profileId ->
            database.medicationDao().getAllMedicationsForProfileFlow(profileId).map { list ->
                list.map { it.toDomain() }
            }
        }
    }

    fun getMedicationForTaskFlow(taskId: Long): Flow<MedicationItem?> {
        return database.medicationDao().getMedicationByTaskIdFlow(taskId).map { it?.toDomain() }
    }

    suspend fun getMedicationForTask(taskId: Long): MedicationItem? {
        return database.medicationDao().getMedicationByTaskId(taskId)?.toDomain()
    }

    fun getMedicationDosesForDateFlow(dateIso: String): Flow<List<MedicationDoseItem>> {
        return database.medicationDao().getDosesForDateFlow(dateIso).map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun saveMedicationForTask(taskId: Long, medication: MedicationItem): Long {
        val effectiveProfileId = if (medication.profileId > 0) medication.profileId else getActiveProfileId()
        val entity = medication.copy(profileId = effectiveProfileId).toEntity(taskId)
        val medId = database.medicationDao().insertMedication(entity)
        val savedMed = entity.copy(id = medId).toDomain()

        // Generate doses for next 14 days or course length
        generateAndInsertDoses(savedMed)
        return medId
    }

    suspend fun generateAndInsertDoses(medication: MedicationItem, daysAhead: Int = 14) {
        if (medication.scheduleConfig.isAsNeeded || medication.isPaused) return

        val startDate = if (medication.scheduleConfig.startDateIso.isNotBlank()) {
            medication.scheduleConfig.startDateIso
        } else {
            DateUtils.todayString()
        }

        val limitDays = medication.scheduleConfig.courseDurationDays?.coerceAtMost(daysAhead) ?: daysAhead
        val doseEntities = mutableListOf<MedicationDoseEntity>()

        for (offset in 0 until limitDays) {
            val date = DateUtils.addDays(startDate, offset)
            val times = MedicationScheduleEngine.calculateDailyDoseTimes(medication.scheduleConfig, date)
            for (time in times) {
                doseEntities.add(
                    MedicationDoseEntity(
                        medicationId = medication.id,
                        taskId = medication.taskId,
                        medicationName = medication.name,
                        medicationForm = medication.form.name,
                        dosage = "${medication.dosageAmount} ${medication.dosageUnit}".trim(),
                        instructions = medication.instructions,
                        scheduledDateIso = date,
                        scheduledTime = time,
                        status = "SCHEDULED"
                    )
                )
            }
        }

        if (doseEntities.isNotEmpty()) {
            database.medicationDao().insertDoses(doseEntities)
        }
    }

    suspend fun recordDoseStatus(doseId: Long, status: DoseStatus, notes: String = "") {
        database.medicationDao().updateDoseStatus(
            id = doseId,
            status = status.name,
            timestamp = System.currentTimeMillis(),
            notes = notes
        )
        val dose = database.medicationDao().getDoseById(doseId) ?: return

        // If TAKEN and schedule is AFTER_CONFIRMED_DOSE, schedule next occurrence
        if (status == DoseStatus.TAKEN) {
            val med = database.medicationDao().getMedicationById(dose.medicationId)?.toDomain()
            if (med != null && med.scheduleConfig.scheduleType == MedicineScheduleType.INTERVAL &&
                med.scheduleConfig.intervalModel == IntervalModel.AFTER_CONFIRMED_DOSE) {
                val nextMins = DateUtils.timeToMinutes(DateUtils.currentTimeString()) + (med.scheduleConfig.intervalHours * 60)
                val nextDate = if (nextMins >= 24 * 60) DateUtils.addDays(dose.scheduledDateIso, 1) else dose.scheduledDateIso
                val nextTime = DateUtils.minutesToTime(nextMins % (24 * 60))

                database.medicationDao().insertDose(
                    MedicationDoseEntity(
                        medicationId = med.id,
                        taskId = med.taskId,
                        medicationName = med.name,
                        medicationForm = med.form.name,
                        dosage = "${med.dosageAmount} ${med.dosageUnit}".trim(),
                        instructions = med.instructions,
                        scheduledDateIso = nextDate,
                        scheduledTime = nextTime,
                        status = "SCHEDULED"
                    )
                )
            }
        }
    }

    suspend fun pauseMedication(medicationId: Long) {
        val med = database.medicationDao().getMedicationById(medicationId) ?: return
        database.medicationDao().updateMedication(med.copy(isPaused = true))
        database.medicationDao().deleteFutureScheduledDoses(medicationId, DateUtils.todayString())
    }

    suspend fun resumeMedication(medicationId: Long) {
        val med = database.medicationDao().getMedicationById(medicationId) ?: return
        val updated = med.copy(isPaused = false)
        database.medicationDao().updateMedication(updated)
        generateAndInsertDoses(updated.toDomain())
    }

    suspend fun logAsNeededDose(medicationId: Long, notes: String = "") {
        val med = database.medicationDao().getMedicationById(medicationId)?.toDomain() ?: return
        val today = DateUtils.todayString()
        val now = DateUtils.currentTimeString()
        val doseEntity = MedicationDoseEntity(
            medicationId = med.id,
            taskId = med.taskId,
            medicationName = med.name,
            medicationForm = med.form.name,
            dosage = "${med.dosageAmount} ${med.dosageUnit}".trim(),
            instructions = med.instructions,
            scheduledDateIso = today,
            scheduledTime = now,
            status = DoseStatus.TAKEN.name,
            actionTimestamp = System.currentTimeMillis(),
            notes = if (notes.isNotBlank()) notes else "As-needed (PRN) dose recorded"
        )
        database.medicationDao().insertDose(doseEntity)
    }

    suspend fun markTaskCompletedForDate(taskId: Long, dateIso: String) {
        taskRepository.markTaskCompletedForDate(taskId, dateIso)
    }

    // --- Mappers ---

    private fun MedicationEntity.toDomain(): MedicationItem {
        return MedicationItem(
            id = id,
            profileId = profileId,
            taskId = taskId,
            name = name,
            form = MedicationForm.fromString(form),
            dosageAmount = dosageAmount,
            dosageUnit = dosageUnit,
            strength = strength,
            instructions = instructions,
            scheduleConfig = MedicationScheduleConfig(
                scheduleType = try { MedicineScheduleType.valueOf(scheduleType) } catch (e: Exception) { MedicineScheduleType.FIXED_TIMES },
                mealPeriods = JsonMappers.parseMealPeriods(mealPeriodsJson),
                fixedTimes = JsonMappers.parseStringList(fixedTimesJson),
                intervalHours = intervalHours,
                intervalModel = try { IntervalModel.valueOf(intervalModel) } catch (e: Exception) { IntervalModel.FIXED_SCHEDULE },
                intervalAnchorTime = intervalAnchorTime,
                allowOvernight = allowOvernight,
                selectedDaysOfWeek = JsonMappers.parseStringList(selectedDaysOfWeek),
                startDateIso = startDateIso,
                endDateIso = endDateIso,
                courseDurationDays = courseDurationDays,
                isAsNeeded = isAsNeeded
            ),
            isPaused = isPaused,
            reminderSound = reminderSound,
            alertStyle = try { AlertStyle.valueOf(alertStyle) } catch (e: Exception) { AlertStyle.STANDARD },
            snoozeMinutes = snoozeMinutes,
            followUpEnabled = followUpEnabled,
            followUpIntervalMinutes = followUpIntervalMinutes,
            maxFollowUps = maxFollowUps,
            privacySafeNotification = privacySafeNotification,
            createdAt = createdAt
        )
    }

    private fun MedicationItem.toEntity(taskId: Long): MedicationEntity {
        return MedicationEntity(
            id = id,
            profileId = profileId,
            taskId = taskId,
            name = name,
            form = form.name,
            dosageAmount = dosageAmount,
            dosageUnit = dosageUnit,
            strength = strength,
            instructions = instructions,
            scheduleType = scheduleConfig.scheduleType.name,
            mealPeriodsJson = JsonMappers.serializeMealPeriods(scheduleConfig.mealPeriods),
            fixedTimesJson = JsonMappers.serializeStringList(scheduleConfig.fixedTimes),
            intervalHours = scheduleConfig.intervalHours,
            intervalModel = scheduleConfig.intervalModel.name,
            intervalAnchorTime = scheduleConfig.intervalAnchorTime,
            allowOvernight = scheduleConfig.allowOvernight,
            selectedDaysOfWeek = JsonMappers.serializeStringList(scheduleConfig.selectedDaysOfWeek),
            startDateIso = scheduleConfig.startDateIso,
            endDateIso = scheduleConfig.endDateIso,
            courseDurationDays = scheduleConfig.courseDurationDays,
            isAsNeeded = scheduleConfig.isAsNeeded,
            isPaused = isPaused,
            reminderSound = reminderSound,
            alertStyle = alertStyle.name,
            snoozeMinutes = snoozeMinutes,
            followUpEnabled = followUpEnabled,
            followUpIntervalMinutes = followUpIntervalMinutes,
            maxFollowUps = maxFollowUps,
            privacySafeNotification = privacySafeNotification,
            createdAt = createdAt
        )
    }

    private fun MedicationDoseEntity.toDomain(): MedicationDoseItem {
        return MedicationDoseItem(
            id = id,
            medicationId = medicationId,
            taskId = taskId,
            medicationName = medicationName,
            medicationForm = MedicationForm.fromString(medicationForm),
            dosage = dosage,
            instructions = instructions,
            scheduledDateIso = scheduledDateIso,
            scheduledTime = scheduledTime,
            status = try { DoseStatus.valueOf(status) } catch (e: Exception) { DoseStatus.SCHEDULED },
            actionTimestamp = actionTimestamp,
            snoozeCount = snoozeCount,
            followUpCount = followUpCount,
            notes = notes
        )
    }

    // ==========================================
    // --- SMART TRACKER REPOSITORY METHODS ---
    // (Delegated to dedicated TrackerRepository - P7-A3)
    // ==========================================

    val allTrackersFlow: Flow<List<TrackerItem>>
        get() = trackerRepository.allTrackersFlow

    val activeTrackersFlow: Flow<List<TrackerItem>>
        get() = trackerRepository.activeTrackersFlow

    suspend fun getTrackerById(id: Long): TrackerItem? {
        return trackerRepository.getTrackerById(id)
    }

    suspend fun insertTracker(tracker: TrackerItem): Long {
        return trackerRepository.insertTracker(tracker)
    }

    suspend fun updateTracker(tracker: TrackerItem) {
        trackerRepository.updateTracker(tracker)
    }

    suspend fun setTrackerArchived(id: Long, isArchived: Boolean) {
        trackerRepository.setTrackerArchived(id, isArchived)
    }

    suspend fun deleteTracker(id: Long) {
        trackerRepository.deleteTracker(id)
    }

    fun getCyclesForTrackerFlow(trackerId: Long): Flow<List<UsageCycleItem>> {
        return trackerRepository.getCyclesForTrackerFlow(trackerId)
    }

    suspend fun insertCycle(cycle: UsageCycleItem): Long {
        return trackerRepository.insertCycle(cycle)
    }

    suspend fun updateCycle(cycle: UsageCycleItem) {
        trackerRepository.updateCycle(cycle)
    }

    suspend fun deleteCycle(id: Long) {
        trackerRepository.deleteCycle(id)
    }

    fun getEntriesForCycleFlow(cycleId: Long): Flow<List<UsageEntryItem>> {
        return trackerRepository.getEntriesForCycleFlow(cycleId)
    }

    suspend fun insertUsageEntry(entry: UsageEntryItem): Long {
        return trackerRepository.insertUsageEntry(entry)
    }

    fun getPurchasesForTrackerFlow(trackerId: Long): Flow<List<TrackerPurchaseItem>> {
        return trackerRepository.getPurchasesForTrackerFlow(trackerId)
    }

    suspend fun insertPurchase(purchase: TrackerPurchaseItem): Long {
        return trackerRepository.insertPurchase(purchase)
    }

    fun getAdjustmentsForTrackerFlow(trackerId: Long): Flow<List<InventoryAdjustmentItem>> {
        return trackerRepository.getAdjustmentsForTrackerFlow(trackerId)
    }

    suspend fun insertAdjustment(adjustment: InventoryAdjustmentItem): Long {
        return trackerRepository.insertAdjustment(adjustment)
    }

    // ==========================================
    // --- SUBSCRIPTION REPOSITORY METHODS ---
    // (Delegated to dedicated SubscriptionRepository - P7-A1)
    // ==========================================

    val allSubscriptionsFlow: Flow<List<SubscriptionItem>>
        get() = subscriptionRepository.allSubscriptionsFlow

    val activeSubscriptionsFlow: Flow<List<SubscriptionItem>>
        get() = subscriptionRepository.activeSubscriptionsFlow

    suspend fun getSubscriptionById(id: Long): SubscriptionItem? =
        subscriptionRepository.getSubscriptionById(id)

    suspend fun insertSubscription(subscription: SubscriptionItem): Long =
        subscriptionRepository.insertSubscription(subscription)

    suspend fun updateSubscription(subscription: SubscriptionItem) =
        subscriptionRepository.updateSubscription(subscription)

    suspend fun setSubscriptionStatus(id: Long, status: SubscriptionStatus) =
        subscriptionRepository.setSubscriptionStatus(id, status)

    suspend fun deleteSubscription(id: Long) =
        subscriptionRepository.deleteSubscription(id)

    fun getBillsForSubscriptionFlow(subscriptionId: Long): Flow<List<BillingOccurrenceItem>> =
        subscriptionRepository.getBillsForSubscriptionFlow(subscriptionId)

    val allBillsFlow: Flow<List<BillingOccurrenceItem>>
        get() = subscriptionRepository.allBillsFlow

    suspend fun insertBill(bill: BillingOccurrenceItem): Long =
        subscriptionRepository.insertBill(bill)

    suspend fun updateBill(bill: BillingOccurrenceItem) =
        subscriptionRepository.updateBill(bill)

    suspend fun deleteBill(id: Long) =
        subscriptionRepository.deleteBill(id)

    fun getPaymentsForBillFlow(billId: Long): Flow<List<SubscriptionPaymentItem>> =
        subscriptionRepository.getPaymentsForBillFlow(billId)

    fun getPaymentsForSubscriptionFlow(subscriptionId: Long): Flow<List<SubscriptionPaymentItem>> =
        subscriptionRepository.getPaymentsForSubscriptionFlow(subscriptionId)

    suspend fun recordBillPayment(payment: SubscriptionPaymentItem, todayIso: String): Long =
        subscriptionRepository.recordBillPayment(payment, todayIso)

    fun getPriceHistoryFlow(subscriptionId: Long): Flow<List<PriceHistoryItem>> =
        subscriptionRepository.getPriceHistoryFlow(subscriptionId)

    suspend fun updateSubscriptionPrice(
        subscriptionId: Long,
        newPrice: Double,
        effectiveDateIso: String,
        reason: String
    ) = subscriptionRepository.updateSubscriptionPrice(subscriptionId, newPrice, effectiveDateIso, reason)

    // --- Release 3 Phase 8: Downloadable Feature Modules & Glossary (#2, #10) ---
    val allGlossaryPacksFlow: Flow<List<com.example.domain.glossary.GlossaryPack>> = glossaryDao.getAllPacksFlow().map { list ->
        list.map { com.example.domain.glossary.GlossaryModuleManager.run { it.toDomain() } }
    }

    val allGlossaryTermsFlow: Flow<List<com.example.domain.glossary.GlossaryTerm>> = activeProfileIdFlow.flatMapLatest { profileId ->
        combine(glossaryDao.getAllItemsFlow(), glossaryDao.getUserDataForProfileFlow(profileId)) { items, userDataList ->
            val userMap = userDataList.associateBy { it.termId }
            items.map { item ->
                val ud = userMap[item.id]
                com.example.domain.glossary.GlossaryModuleManager.run {
                    item.toDomain(
                        isBookmarkedProfile = ud?.isBookmarked ?: false,
                        personalNoteProfile = ud?.personalNote ?: "",
                        lastViewedAtProfile = ud?.lastViewedAt ?: 0L
                    )
                }
            }
        }
    }

    val allFeatureModulesFlow: Flow<List<com.example.domain.modules.FeatureModule>> = allGlossaryPacksFlow.map {
        com.example.domain.modules.FeatureModuleManager.resolveAllModules(glossaryDao)
    }

    fun getGlossaryTermsForPack(packId: String): Flow<List<com.example.domain.glossary.GlossaryTerm>> {
        return activeProfileIdFlow.flatMapLatest { profileId ->
            combine(glossaryDao.getItemsForPackFlow(packId), glossaryDao.getUserDataForProfileFlow(profileId)) { items, userDataList ->
                val userMap = userDataList.associateBy { it.termId }
                items.map { item ->
                    val ud = userMap[item.id]
                    com.example.domain.glossary.GlossaryModuleManager.run {
                        item.toDomain(
                            isBookmarkedProfile = ud?.isBookmarked ?: false,
                            personalNoteProfile = ud?.personalNote ?: "",
                            lastViewedAtProfile = ud?.lastViewedAt ?: 0L
                        )
                    }
                }
            }
        }
    }

    suspend fun setGlossaryBookmark(profileId: Long, termId: Long, isBookmarked: Boolean) {
        glossaryDao.setBookmark(profileId, termId, isBookmarked)
    }

    suspend fun setGlossaryPersonalNote(profileId: Long, termId: Long, note: String) {
        glossaryDao.setPersonalNote(profileId, termId, note)
    }

    suspend fun recordGlossaryTermView(profileId: Long, termId: Long) {
        glossaryDao.recordTermView(profileId, termId)
    }

    suspend fun installGlossaryPack(
        packJson: com.example.domain.glossary.GlossaryPackJson,
        expectedSha256: String? = null,
        rawJsonContent: String? = null
    ): Result<com.example.domain.glossary.GlossaryPack> {
        return com.example.domain.glossary.GlossaryModuleManager.installPack(
            dao = glossaryDao,
            packJson = packJson,
            expectedSha256 = expectedSha256,
            rawJsonContent = rawJsonContent
        )
    }

    suspend fun uninstallGlossaryPack(packId: String) {
        com.example.domain.glossary.GlossaryModuleManager.uninstallPack(glossaryDao, packId)
    }

    suspend fun installFeatureModule(
        manifest: com.example.domain.modules.ModuleManifest,
        packJson: com.example.domain.glossary.GlossaryPackJson,
        expectedSha256: String? = null,
        rawContent: String? = null
    ): Result<com.example.domain.modules.FeatureModule> {
        return com.example.domain.modules.FeatureModuleManager.installModule(
            dao = glossaryDao,
            manifest = manifest,
            packJson = packJson,
            expectedSha256 = expectedSha256,
            rawContent = rawContent
        )
    }

    suspend fun uninstallFeatureModule(moduleId: String): Result<Unit> {
        return com.example.domain.modules.FeatureModuleManager.uninstallModule(glossaryDao, moduleId)
    }

    // --- Release 3 Phase 9: Model Context Protocol (MCP) Bidirectional Engine (#3) ---
    private val mcpDaoInstance = database.mcpDao()
    val mcpClientEngine: com.example.mcp.client.McpClientEngine by lazy {
        com.example.mcp.client.McpClientEngine(mcpDaoInstance)
    }

    fun getAllMcpServersFlow(profileId: Long): Flow<List<com.example.data.local.entities.McpServerEntity>> {
        return mcpDaoInstance.getAllServersFlow(profileId)
    }

    suspend fun getAllMcpServersForExport(profileId: Long): List<com.example.data.local.entities.McpServerEntity> {
        return mcpDaoInstance.getAllServersList(profileId)
    }

    suspend fun insertMcpServer(server: com.example.data.local.entities.McpServerEntity): Long {
        return mcpDaoInstance.insertServer(server)
    }

    suspend fun deleteMcpServer(id: Long) {
        mcpDaoInstance.deleteServer(id)
    }

    fun getMcpAuditLogsFlow(profileId: Long): Flow<List<com.example.data.local.entities.McpAuditLogEntity>> {
        return mcpDaoInstance.getAuditLogsFlow(profileId)
    }

    suspend fun clearMcpAuditLogs(profileId: Long) {
        mcpDaoInstance.clearAuditLogs(profileId)
    }

    suspend fun insertMcpAuditLog(log: com.example.data.local.entities.McpAuditLogEntity): Long {
        return mcpDaoInstance.insertAuditLog(log)
    }

    // --- Shift-Based Life Cycle Planner ---
    fun getShiftsFlow(profileId: Long): Flow<List<com.example.data.local.entities.ShiftEntity>> =
        shiftDao.getShiftsForProfile(profileId)

    suspend fun getShiftsDirect(profileId: Long): List<com.example.data.local.entities.ShiftEntity> =
        shiftDao.getShiftsForProfileDirect(profileId)

    suspend fun saveShift(shift: com.example.data.local.entities.ShiftEntity): Long {
        return if (shift.id == 0L) shiftDao.insertShift(shift) else {
            shiftDao.updateShift(shift)
            shift.id
        }
    }

    suspend fun deleteShift(shift: com.example.data.local.entities.ShiftEntity) {
        shiftDao.deleteShift(shift)
    }

    suspend fun deleteShiftById(id: Long) {
        shiftDao.deleteShiftById(id)
    }

    fun getActiveRotationFlow(profileId: Long): Flow<com.example.data.local.entities.ShiftRotationEntity?> =
        shiftDao.getActiveRotation(profileId)

    fun getAllRotationsFlow(profileId: Long): Flow<List<com.example.data.local.entities.ShiftRotationEntity>> =
        shiftDao.getAllRotationsForProfile(profileId)

    suspend fun saveRotation(rotation: com.example.data.local.entities.ShiftRotationEntity, setAsActive: Boolean = true): Long {
        if (setAsActive) {
            shiftDao.deactivateAllRotationsForProfile(rotation.profileId)
        }
        val target = rotation.copy(isActive = setAsActive)
        return if (target.id == 0L) shiftDao.insertRotation(target) else {
            shiftDao.updateRotation(target)
            target.id
        }
    }

    suspend fun deleteRotation(rotation: com.example.data.local.entities.ShiftRotationEntity) {
        shiftDao.deleteRotation(rotation)
    }

    fun getShiftAssignmentForDateFlow(profileId: Long, dateIso: String): Flow<com.example.data.local.entities.ShiftAssignmentEntity?> =
        shiftDao.getAssignmentForDate(profileId, dateIso)

    fun getShiftAssignmentsInRangeFlow(profileId: Long, startDateIso: String, endDateIso: String): Flow<List<com.example.data.local.entities.ShiftAssignmentEntity>> =
        shiftDao.getAssignmentsInRange(profileId, startDateIso, endDateIso)

    suspend fun saveShiftAssignment(assignment: com.example.data.local.entities.ShiftAssignmentEntity): Long =
        shiftDao.insertOrUpdateAssignment(assignment)

    suspend fun deleteShiftAssignmentForDate(profileId: Long, dateIso: String) =
        shiftDao.deleteAssignmentForDate(profileId, dateIso)

    suspend fun resolveEffectiveShiftForDate(profileId: Long, dateIso: String): Pair<com.example.data.local.entities.ShiftEntity?, com.example.data.local.entities.ShiftAssignmentEntity?> {
        val directAssignment = shiftDao.getAssignmentForDateDirect(profileId, dateIso)
        if (directAssignment != null) {
            val shift = directAssignment.shiftId?.let { shiftDao.getShiftById(it) }
            return Pair(shift, directAssignment)
        }

        val activeRotation = shiftDao.getActiveRotationDirect(profileId)
        if (activeRotation != null) {
            try {
                val start = java.time.LocalDate.parse(activeRotation.startDateIso)
                val target = java.time.LocalDate.parse(dateIso)
                val daysDiff = java.time.temporal.ChronoUnit.DAYS.between(start, target).toInt()
                if (daysDiff >= 0 && activeRotation.cycleLengthDays > 0) {
                    val patternArray = org.json.JSONArray(activeRotation.patternJson)
                    if (patternArray.length() > 0) {
                        val cycleIndex = daysDiff % activeRotation.cycleLengthDays
                        if (cycleIndex < patternArray.length()) {
                            val shiftId = patternArray.optLong(cycleIndex, -1L)
                            if (shiftId > 0) {
                                val shift = shiftDao.getShiftById(shiftId)
                                return Pair(shift, null)
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                // Fallback gracefully
            }
        }
        return Pair(null, null)
    }

    // --- Give & Get Transactions ---
    fun getAllGiveGetTransactionsFlow(profileId: Long): Flow<List<com.example.data.local.entities.GiveGetTransactionEntity>> =
        giveGetDao.getAllTransactions(profileId)

    fun getGiveGetTransactionsByStatusFlow(profileId: Long, status: String): Flow<List<com.example.data.local.entities.GiveGetTransactionEntity>> =
        giveGetDao.getTransactionsByStatus(profileId, status)

    fun getDistinctGiveGetPersonsFlow(profileId: Long): Flow<List<String>> =
        giveGetDao.getDistinctPersons(profileId)

    suspend fun saveGiveGetTransaction(transaction: com.example.data.local.entities.GiveGetTransactionEntity): Long =
        if (transaction.id == 0L) giveGetDao.insertTransaction(transaction) else {
            giveGetDao.updateTransaction(transaction)
            transaction.id
        }

    suspend fun deleteGiveGetTransaction(transaction: com.example.data.local.entities.GiveGetTransactionEntity) =
        giveGetDao.deleteTransaction(transaction)

    suspend fun recordGiveGetRepayment(id: Long, repaymentDelta: Double) {
        val tx = giveGetDao.getTransactionById(id) ?: return
        val newReturned = (tx.returnedAmount + repaymentDelta).coerceAtLeast(0.0)
        val newStatus = if (newReturned >= tx.amount) "RETURNED" else "PARTIALLY_RETURNED"
        val returnDate = if (newStatus == "RETURNED") java.time.LocalDate.now().toString() else tx.actualReturnDateIso
        giveGetDao.recordRepayment(id, newReturned, newStatus, returnDate)
    }

    suspend fun markGiveGetReturned(id: Long) {
        val tx = giveGetDao.getTransactionById(id) ?: return
        val newReturned = if (tx.isMoney) tx.amount else tx.returnedAmount
        giveGetDao.recordRepayment(id, newReturned, "RETURNED", java.time.LocalDate.now().toString())
    }
}

