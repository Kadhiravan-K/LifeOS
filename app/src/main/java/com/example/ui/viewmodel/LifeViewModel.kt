package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.UserPreferences
import com.example.data.repository.LifeRepository
import com.example.domain.model.ChallengePreset
import com.example.domain.model.ChallengeRule
import com.example.domain.model.ChallengeState
import com.example.domain.model.ColorOrderingMode
import com.example.domain.model.DefaultHoneycombShortcuts
import com.example.domain.model.HabitItem
import com.example.domain.model.HoneycombAppearanceConfig
import com.example.domain.model.HoneycombShortcut
import com.example.domain.model.LabelItem
import com.example.domain.model.ProjectItem
import com.example.domain.model.RecurrenceType
import com.example.domain.model.ShortcutType
import com.example.domain.model.Subtask
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskPriority
import com.example.domain.model.TaskReminderItem
import com.example.domain.schedule.ScheduleConflict
import com.example.domain.schedule.ScheduleConflictEngine
import com.example.notifications.NotificationHelper
import com.example.utils.DateUtils
import com.example.ui.theme.engine.ThemeDefinition
import com.example.ui.theme.engine.ThemeId
import com.example.ui.theme.engine.ThemeRegistry
import com.example.ui.theme.engine.PremiumEntitlementRepository
import com.example.ui.theme.engine.LocalPremiumEntitlementRepository
import com.example.domain.model.PlanItem
import com.example.domain.model.PlanLevel
import com.example.domain.model.PlanMilestone
import com.example.domain.model.TrashItem
import com.example.domain.model.GamificationProfile
import com.example.domain.model.HunterRank
import com.example.domain.model.AchievementItem
import com.example.domain.model.RewardItem
import com.example.domain.model.HoneycombLayoutTemplate
import com.example.data.backup.DataExportImportManager
import com.example.data.local.AppDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val userName: String = "Master",
    val greeting: String = "Good morning",
    val todayDateFormatted: String = "",
    val todayDateIso: String = "",
    val overallProgressPercentage: Float = 0f,
    val remainingTasksCount: Int = 0,
    val currentTask: TaskItem? = null,
    val nextTask: TaskItem? = null,
    val topPriorities: List<TaskItem> = emptyList(),
    val overdueTasks: List<TaskItem> = emptyList(),
    val upcomingReminders: List<TaskItem> = emptyList(),
    val morningTasks: List<TaskItem> = emptyList(),
    val afternoonTasks: List<TaskItem> = emptyList(),
    val eveningTasks: List<TaskItem> = emptyList(),
    val noTimeTasks: List<TaskItem> = emptyList(),
    val habitsCompletedTodayCount: Int = 0,
    val totalHabitsCount: Int = 0,
    val habits: List<HabitItem> = emptyList(),
    val waterCurrentMl: Int = 0,
    val waterGoalMl: Int = 2000,
    val challengeDayNumber: Int = 1,
    val challengeTargetDays: Int = 75,
    val challengeProgressPercentage: Float = 0f,
    val challengeRulesCompletedToday: Int = 0,
    val challengeRulesTotal: Int = 6,
    val hasInstalledDefaults: Boolean = false,
    val honeycombShortcuts: List<HoneycombShortcut> = emptyList(),
    val isHoneycombEditMode: Boolean = false
)

data class TasksUiState(
    val allTasks: List<TaskItem> = emptyList(),
    val filteredTasks: List<TaskItem> = emptyList(),
    val selectedFilter: String = "All", // "Inbox", "Today", "Upcoming", "Projects", "Labels", "Completed", "Archived"
    val selectedCategory: String? = null,
    val selectedProjectId: Long? = null,
    val selectedLabelId: Long? = null,
    val searchQuery: String = "",
    val selectedSort: String = "Priority", // "Priority", "Due Date", "Title"
    val selectedDateIso: String = DateUtils.todayString(),
    val isSelectionMode: Boolean = false,
    val selectedTaskIds: Set<Long> = emptySet(),
    val projects: List<ProjectItem> = emptyList(),
    val labels: List<LabelItem> = emptyList()
)

data class CalendarUiState(
    val selectedDateIso: String = DateUtils.todayString(),
    val currentMonthZeroIndexed: Int = DateUtils.getMonthZeroIndexed(DateUtils.todayString()),
    val currentYear: Int = DateUtils.getYear(DateUtils.todayString()),
    val monthYearTitle: String = "",
    val viewMode: String = "MONTH", // "DAY", "THREE_DAY", "WEEK", "MONTH", "AGENDA"
    val monthGridDates: List<String> = emptyList(),
    val weekDates: List<String> = emptyList(),
    val threeDayDates: List<String> = emptyList(),
    val tasksOnSelectedDate: List<TaskItem> = emptyList(),
    val conflictsOnSelectedDate: List<ScheduleConflict> = emptyList(),
    val selectedCategoryFilter: String? = null,
    val selectedPriorityFilter: TaskPriority? = null
)

data class HabitsUiState(
    val habits: List<HabitItem> = emptyList(),
    val completedTodayCount: Int = 0,
    val totalCount: Int = 0,
    val completionPercentage: Float = 0f,
    val selectedDateIso: String = DateUtils.todayString(),
    val heatmapDays: List<Pair<String, Float>> = emptyList()
)

private data class UserDashboardPrefs(
    val userName: String,
    val hasInstalledDefaults: Boolean,
    val is24Hour: Boolean
)

private data class TaskFilterParams(
    val filter: String,
    val category: String?,
    val projectId: Long?,
    val labelId: Long?,
    val query: String,
    val sort: String,
    val isSelectionMode: Boolean,
    val selectedIds: Set<Long>
)

class LifeViewModel(
    application: Application,
    private val repository: LifeRepository,
    private val preferences: UserPreferences
) : AndroidViewModel(application) {

    private val todayIso = DateUtils.todayString()

    init {
        viewModelScope.launch {
            repository.seedDefaultHoneycombShortcuts()
            repository.seedDefaultProjectsAndLabels()
            repository.seedDefaultGamification()
            repository.cleanupExpiredTrash()
        }
    }

    // --- Search & Filter States ---
    private val _taskSearchQuery = MutableStateFlow("")
    val taskSearchQuery: StateFlow<String> = _taskSearchQuery.asStateFlow()

    private val _taskFilter = MutableStateFlow("All")
    val taskFilter: StateFlow<String> = _taskFilter.asStateFlow()

    private val _taskProjectIdFilter = MutableStateFlow<Long?>(null)
    val taskProjectIdFilter: StateFlow<Long?> = _taskProjectIdFilter.asStateFlow()

    private val _taskLabelIdFilter = MutableStateFlow<Long?>(null)
    val taskLabelIdFilter: StateFlow<Long?> = _taskLabelIdFilter.asStateFlow()

    private val _taskCategoryFilter = MutableStateFlow<String?>(null)
    val taskCategoryFilter: StateFlow<String?> = _taskCategoryFilter.asStateFlow()

    private val _taskSort = MutableStateFlow("Priority")
    val taskSort: StateFlow<String> = _taskSort.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _selectedTaskIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedTaskIds: StateFlow<Set<Long>> = _selectedTaskIds.asStateFlow()

    // --- Calendar States ---
    private val _calendarSelectedDate = MutableStateFlow(todayIso)
    val calendarSelectedDate: StateFlow<String> = _calendarSelectedDate.asStateFlow()

    private val _calendarYear = MutableStateFlow(DateUtils.getYear(todayIso))
    private val _calendarMonth = MutableStateFlow(DateUtils.getMonthZeroIndexed(todayIso))
    private val _calendarViewMode = MutableStateFlow("MONTH") // "MONTH", "WEEK"
    private val _calendarCategoryFilter = MutableStateFlow<String?>(null)
    private val _calendarPriorityFilter = MutableStateFlow<TaskPriority?>(null)

    // --- Flows from Repository ---
    val allTasks: StateFlow<List<TaskItem>> = repository.allTasksFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val todayTasks: StateFlow<List<TaskItem>> = repository.getTasksForDateFlow(todayIso).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val habits: StateFlow<List<HabitItem>> = repository.allHabitsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val challengeState: StateFlow<ChallengeState> = repository.challengeStateFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ChallengeState(
            startDateIso = todayIso,
            rules = emptyList()
        )
    )

    val todayWaterLog = repository.getWaterLogForDate(todayIso).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = com.example.data.local.entities.WaterLogEntity(dateIso = todayIso, amountMl = 0)
    )

    // --- Projects & Labels Flows ---
    val allProjects: StateFlow<List<ProjectItem>> = repository.allProjectsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allLabels: StateFlow<List<LabelItem>> = repository.allLabelsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val archivedProjects: StateFlow<List<ProjectItem>> = repository.archivedProjectsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val archivedLabels: StateFlow<List<LabelItem>> = repository.archivedLabelsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val colorOrderingMode: StateFlow<ColorOrderingMode> = preferences.colorOrderingMode.map { modeStr: String ->
        ColorOrderingMode.fromString(modeStr)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ColorOrderingMode.AUTO
    )

    val colorUsageCounts: StateFlow<Map<String, Int>> = preferences.colorUsageCounts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    val colorCustomOrder: StateFlow<List<String>> = preferences.colorCustomOrder.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // --- Preferences Flows ---
    val userNameFlow: StateFlow<String> = preferences.userName.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "Master"
    )

    val themeModeFlow: StateFlow<String> = preferences.themeMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "SYSTEM"
    )

    val activeThemeIdFlow: StateFlow<String> = preferences.activeThemeId.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "default"
    )

    private val entitlementRepo: PremiumEntitlementRepository = LocalPremiumEntitlementRepository(application)
    val isPremiumActive: StateFlow<Boolean> = entitlementRepo.isPremiumActive.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    private val _previewThemeId = MutableStateFlow<ThemeId?>(null)
    val previewThemeId: StateFlow<ThemeId?> = _previewThemeId.asStateFlow()

    val activeThemeDefinition: StateFlow<ThemeDefinition> = combine(
        activeThemeIdFlow,
        _previewThemeId
    ) { persistedId, previewId ->
        if (previewId != null) {
            ThemeRegistry.getTheme(previewId)
        } else {
            ThemeRegistry.getTheme(persistedId)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ThemeRegistry.DEFAULT_THEME
    )

    val waterGoalFlow: StateFlow<Int> = preferences.waterGoalMl.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 2000
    )

    val hasInstalledDefaultsFlow: StateFlow<Boolean> = preferences.hasInstalledDefaultRoutines.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val notificationsEnabledFlow: StateFlow<Boolean> = preferences.notificationsEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val wakeUpTimeFlow: StateFlow<String> = preferences.wakeUpTime.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "05:30"
    )

    val bedTimeFlow: StateFlow<String> = preferences.bedTime.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "21:30"
    )

    val quietHoursStartFlow: StateFlow<String> = preferences.quietHoursStart.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "21:00"
    )

    val quietHoursEndFlow: StateFlow<String> = preferences.quietHoursEnd.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "05:30"
    )

    val clockFormatFlow: StateFlow<String> = preferences.clockFormat.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "12_HOUR"
    )

    val firstDayOfWeekFlow: StateFlow<String> = preferences.firstDayOfWeek.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "MONDAY"
    )

    val defaultTaskDurationFlow: StateFlow<Int> = preferences.defaultTaskDuration.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 30
    )

    // --- Honeycomb Appearance & Edit State ---
    private val _isHoneycombEditMode = MutableStateFlow(false)
    val isHoneycombEditMode: StateFlow<Boolean> = _isHoneycombEditMode.asStateFlow()

    val honeycombAppearance: StateFlow<HoneycombAppearanceConfig> = combine(
        preferences.honeycombDefaultColorHex,
        preferences.honeycombBorderColor,
        preferences.honeycombBorderWidthDp,
        preferences.honeycombSizeDp,
        preferences.honeycombSpacingDp
    ) { color, border, width, size, spacing ->
        HoneycombAppearanceConfig(
            defaultColorHex = color,
            borderColorType = border,
            borderWidthDp = width,
            hexagonSizeDp = size,
            spacingDp = spacing,
            iconSizeSp = 26,
            isAnimationEnabled = true
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HoneycombAppearanceConfig()
    )

    val rawHoneycombShortcuts: StateFlow<List<HoneycombShortcut>> = repository.allHoneycombShortcutsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val resolvedHoneycombShortcuts: StateFlow<List<HoneycombShortcut>> = combine(
        rawHoneycombShortcuts,
        todayTasks,
        allTasks,
        habits,
        challengeState
    ) { rawList, todayTaskList, allTaskList, habitList, challenge ->
        val effectiveList = if (rawList.isEmpty()) DefaultHoneycombShortcuts.list else rawList
        val waterCurrent = todayWaterLog.value.amountMl
        val waterGoal = waterGoalFlow.value.coerceAtLeast(1000)

        effectiveList.map { shortcut ->
            when (shortcut.type) {
                ShortcutType.TASK -> {
                    val matchedTask = todayTaskList.find { it.id.toString() == shortcut.targetId || it.title.contains(shortcut.targetId, ignoreCase = true) }
                        ?: allTaskList.find { it.id.toString() == shortcut.targetId || it.title.contains(shortcut.targetId, ignoreCase = true) }
                    val isDone = matchedTask?.isCompletedForDate ?: false
                    shortcut.copy(
                        isCompletedToday = isDone,
                        subtitle = matchedTask?.dueTime ?: matchedTask?.category ?: "",
                        isBinary = true
                    )
                }
                ShortcutType.HABIT -> {
                    val matchedHabit = habitList.find { it.id.toString() == shortcut.targetId || it.title.contains(shortcut.targetId, ignoreCase = true) }
                    val isDone = matchedHabit?.completedDates?.contains(todayIso) ?: false
                    shortcut.copy(
                        isCompletedToday = isDone,
                        subtitle = "${matchedHabit?.currentStreak ?: 0}d streak",
                        isBinary = true
                    )
                }
                ShortcutType.CHALLENGE_RULE -> {
                    val rule = challenge.rules.find { it.id == shortcut.targetId || it.title.contains(shortcut.targetId, ignoreCase = true) }
                    val todayLog = challenge.logs[todayIso]
                    val isDone = rule != null && (todayLog?.completedRuleIds?.contains(rule.id) == true)
                    shortcut.copy(
                        isCompletedToday = isDone,
                        subtitle = rule?.title ?: "",
                        isBinary = true
                    )
                }
                ShortcutType.WATER -> {
                    val isDone = waterCurrent >= waterGoal
                    shortcut.copy(
                        isCompletedToday = isDone,
                        subtitle = "$waterCurrent / $waterGoal ml",
                        progressFraction = (waterCurrent.toFloat() / waterGoal).coerceIn(0f, 1f),
                        isBinary = false
                    )
                }
                ShortcutType.CUSTOM -> {
                    shortcut.copy(isBinary = true)
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DefaultHoneycombShortcuts.list
    )

    private val userPrefsFlow = combine(userNameFlow, hasInstalledDefaultsFlow, clockFormatFlow) { name, defaults, clock ->
        UserDashboardPrefs(name, defaults, clock == "24_HOUR")
    }

    private val filterParamsFlow = combine(
        combine(_taskFilter, _taskCategoryFilter, _taskProjectIdFilter) { f, c, p -> Triple(f, c, p) },
        combine(_taskLabelIdFilter, _taskSearchQuery, _taskSort) { l, q, s -> Triple(l, q, s) }
    ) { (f, c, p), (l, q, s) ->
        TaskFilterParams(f, c, p, l, q, s, _isSelectionMode.value, _selectedTaskIds.value)
    }

    // --- Dashboard UI State ---
    private val dashboardBaseState: StateFlow<DashboardUiState> = combine(
        userPrefsFlow,
        todayTasks,
        allTasks,
        habits,
        challengeState
    ) { prefs, tasksToday, fullTaskList, habitList, challenge ->
        val completedTasksToday = tasksToday.count { it.isCompletedForDate }
        val remainingCount = tasksToday.count { !it.isCompletedForDate }

        val completedHabitsToday = habitList.count { it.isCompletedToday }
        val totalHabits = habitList.size

        val challengeTodayLog = challenge.logs[todayIso]
        val challengeCompletedRules = challengeTodayLog?.completedRuleIds?.size ?: 0
        val challengeTotalRules = challenge.rules.size.coerceAtLeast(1)

        val totalItems = tasksToday.size + totalHabits + challengeTotalRules
        val completedItems = completedTasksToday + completedHabitsToday + challengeCompletedRules
        val progress = if (totalItems > 0) completedItems.toFloat() / totalItems.toFloat() else 0f

        // Top 3 priority tasks for today
        val uncompletedPriorities = tasksToday
            .filter { !it.isCompletedForDate }
            .sortedByDescending { it.priority.level }
            .take(3)
        val priorities = uncompletedPriorities.ifEmpty {
            tasksToday.sortedByDescending { it.priority.level }.take(3)
        }

        // Current Task & Next Task determination based on current minute of day
        val nowMins = DateUtils.getCurrentMinuteOfDay()
        val timedTasksToday = tasksToday.filter { !it.dueTime.isNullOrBlank() }.sortedBy { DateUtils.timeToMinutes(it.dueTime) }
        val current = timedTasksToday.firstOrNull { task ->
            val start = DateUtils.timeToMinutes(task.dueTime)
            nowMins in start until (start + task.durationMinutes)
        }
        val next = timedTasksToday.firstOrNull { task ->
            val start = DateUtils.timeToMinutes(task.dueTime)
            start > nowMins && !task.isCompletedForDate
        }

        // Overdue tasks (due date before today and not completed)
        val overdue = fullTaskList.filter { task ->
            task.recurrenceType == RecurrenceType.NONE && task.dueDateIso < todayIso && !task.isOneTimeCompleted && !task.isArchived
        }

        // Upcoming reminders (tasks with reminder enabled today and tomorrow)
        val reminders = tasksToday.filter { it.isReminderEnabled && !it.isCompletedForDate }

        // Time groupings
        val morning = tasksToday.filter {
            val m = DateUtils.timeToMinutes(it.dueTime)
            m in 300..719 // 5:00 AM - 11:59 AM
        }
        val afternoon = tasksToday.filter {
            val m = DateUtils.timeToMinutes(it.dueTime)
            m in 720..1019 // 12:00 PM - 4:59 PM
        }
        val evening = tasksToday.filter {
            val m = DateUtils.timeToMinutes(it.dueTime)
            m in 1020..1290 // 5:00 PM - 9:30 PM
        }
        val noTime = tasksToday.filter {
            val m = DateUtils.timeToMinutes(it.dueTime)
            m < 0 || m > 1290 || m < 300
        }

        val currentHour = DateUtils.getCurrentHour()
        val greetingText = "${DateUtils.getGreeting(currentHour)}, ${prefs.userName}"

        DashboardUiState(
            userName = prefs.userName,
            greeting = greetingText,
            todayDateFormatted = DateUtils.formatDisplayDate(todayIso),
            todayDateIso = todayIso,
            overallProgressPercentage = progress,
            remainingTasksCount = remainingCount,
            currentTask = current,
            nextTask = next,
            topPriorities = priorities,
            overdueTasks = overdue,
            upcomingReminders = reminders,
            morningTasks = morning,
            afternoonTasks = afternoon,
            eveningTasks = evening,
            noTimeTasks = noTime,
            habitsCompletedTodayCount = completedHabitsToday,
            totalHabitsCount = totalHabits,
            habits = habitList,
            waterCurrentMl = todayWaterLog.value.amountMl,
            waterGoalMl = todayWaterLog.value.goalMl,
            challengeDayNumber = challenge.currentDayNumber,
            challengeTargetDays = challenge.targetDays,
            challengeProgressPercentage = challenge.progressPercentage,
            challengeRulesCompletedToday = challengeCompletedRules,
            challengeRulesTotal = challenge.rules.size,
            hasInstalledDefaults = prefs.hasInstalledDefaults
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    val dashboardUiState: StateFlow<DashboardUiState> = combine(
        dashboardBaseState,
        todayWaterLog,
        resolvedHoneycombShortcuts,
        _isHoneycombEditMode
    ) { base, water, shortcuts, editMode ->
        base.copy(
            waterCurrentMl = water.amountMl,
            waterGoalMl = water.goalMl,
            honeycombShortcuts = shortcuts,
            isHoneycombEditMode = editMode
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )


    // --- Tasks UI State ---
    val tasksUiState: StateFlow<TasksUiState> = combine(
        todayTasks,
        allTasks,
        allProjects,
        allLabels,
        filterParamsFlow
    ) { todayList, fullList, projectsList, labelsList, params ->
        val baseList = when {
            params.projectId != null -> fullList.filter { it.projectId == params.projectId && !it.isArchived }
            params.labelId != null -> fullList.filter { it.labels.any { lbl -> lbl.id == params.labelId } && !it.isArchived }
            params.filter == "Inbox" -> fullList.filter { it.projectId == null && !it.isArchived }
            params.filter == "Today" -> todayList.filter { !it.isArchived }
            params.filter == "Upcoming" -> fullList.filter { it.dueDateIso > todayIso && !it.isCompletedForDate && !it.isArchived }
            params.filter == "Completed" -> fullList.filter { (it.isCompletedForDate || it.isOneTimeCompleted) && !it.isArchived }
            params.filter == "Archived" -> fullList.filter { it.isArchived }
            else -> fullList.filter { !it.isArchived }
        }

        var filtered = baseList
        if (!params.category.isNullOrBlank()) {
            filtered = filtered.filter { it.category.equals(params.category, ignoreCase = true) }
        }
        if (params.query.isNotBlank()) {
            filtered = filtered.filter {
                it.title.contains(params.query, ignoreCase = true) ||
                it.description.contains(params.query, ignoreCase = true) ||
                (it.projectName?.contains(params.query, ignoreCase = true) == true) ||
                it.labels.any { l -> l.name.contains(params.query, ignoreCase = true) }
            }
        }

        filtered = when (params.sort) {
            "Priority" -> filtered.sortedByDescending { it.priority.level }
            "Due Date" -> filtered.sortedBy { it.dueDateIso }
            "Title" -> filtered.sortedBy { it.title.lowercase() }
            else -> filtered
        }

        TasksUiState(
            allTasks = fullList,
            filteredTasks = filtered,
            selectedFilter = params.filter,
            selectedCategory = params.category,
            selectedProjectId = params.projectId,
            selectedLabelId = params.labelId,
            searchQuery = params.query,
            selectedSort = params.sort,
            selectedDateIso = todayIso,
            isSelectionMode = params.isSelectionMode,
            selectedTaskIds = params.selectedIds,
            projects = projectsList,
            labels = labelsList
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TasksUiState()
    )

    // --- Calendar UI State ---
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val calendarUiState: StateFlow<CalendarUiState> = combine(
        _calendarSelectedDate,
        _calendarYear,
        _calendarMonth,
        _calendarViewMode,
        firstDayOfWeekFlow
    ) { selDate, year, month, viewMode, firstDay ->
        val monthTitle = DateUtils.formatMonthYear(year, month)
        val monthGrid = DateUtils.getMonthCalendarGrid(year, month, firstDay)
        val weekGrid = DateUtils.getWeekDays(selDate, firstDay)
        val threeDays = listOf(selDate, DateUtils.addDays(selDate, 1), DateUtils.addDays(selDate, 2))
        Triple(monthTitle, Pair(monthGrid, weekGrid), threeDays)
    }.flatMapLatest { (title, grids, threeDays) ->
        val (monthGrid, weekGrid) = grids
        val selDate = _calendarSelectedDate.value
        repository.getTasksForDateFlow(selDate).map { tasksOnDate ->
            val wakeUp = DateUtils.timeToMinutes(wakeUpTimeFlow.value).coerceAtLeast(330)
            val bedTime = DateUtils.timeToMinutes(bedTimeFlow.value).coerceAtLeast(1290)

            // Compute conflicts on selected date
            val allConflicts = mutableListOf<ScheduleConflict>()
            tasksOnDate.forEach { task ->
                val taskConflicts = ScheduleConflictEngine.detectConflicts(
                    task = task,
                    existingTasksOnDate = tasksOnDate,
                    targetDateIso = selDate,
                    wakeUpMinutes = wakeUp,
                    bedTimeMinutes = bedTime
                )
                allConflicts.addAll(taskConflicts)
            }

            var filtered = tasksOnDate
            if (_calendarCategoryFilter.value != null) {
                filtered = filtered.filter { it.category.equals(_calendarCategoryFilter.value, ignoreCase = true) }
            }
            if (_calendarPriorityFilter.value != null) {
                filtered = filtered.filter { it.priority == _calendarPriorityFilter.value }
            }

            CalendarUiState(
                selectedDateIso = selDate,
                currentMonthZeroIndexed = _calendarMonth.value,
                currentYear = _calendarYear.value,
                monthYearTitle = title,
                viewMode = _calendarViewMode.value,
                monthGridDates = monthGrid,
                weekDates = weekGrid,
                threeDayDates = threeDays,
                tasksOnSelectedDate = filtered,
                conflictsOnSelectedDate = allConflicts.distinctBy { it.message },
                selectedCategoryFilter = _calendarCategoryFilter.value,
                selectedPriorityFilter = _calendarPriorityFilter.value
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CalendarUiState()
    )

    // --- Habits UI State ---
    val habitsUiState: StateFlow<HabitsUiState> = habits.map { habitList ->
        val completedToday = habitList.count { it.isCompletedToday }
        val total = habitList.size
        val percentage = if (total > 0) completedToday.toFloat() / total.toFloat() else 0f

        val past35Days = DateUtils.getLastNDays(35)
        val heatmap = past35Days.map { dateIso ->
            val countOnDate = habitList.count { it.completedDates.contains(dateIso) }
            val ratio = if (total > 0) countOnDate.toFloat() / total.toFloat() else 0f
            Pair(dateIso, ratio)
        }

        HabitsUiState(
            habits = habitList,
            completedTodayCount = completedToday,
            totalCount = total,
            completionPercentage = percentage,
            selectedDateIso = todayIso,
            heatmapDays = heatmap
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HabitsUiState()
    )

    // --- Task Actions ---
    fun toggleTaskCompletion(taskId: Long, dateIso: String = todayIso) {
        viewModelScope.launch {
            repository.toggleTaskCompletion(taskId, dateIso)
        }
    }

    fun toggleTask(taskId: Long, dateIso: String = todayIso) {
        toggleTaskCompletion(taskId, dateIso)
    }

    fun toggleSubtask(taskId: Long, subtaskId: String) {
        viewModelScope.launch {
            repository.toggleSubtask(taskId, subtaskId)
        }
    }

    fun saveTask(
        id: Long = 0,
        title: String,
        emoji: String,
        description: String,
        category: String,
        priority: TaskPriority,
        dueDateIso: String,
        dueTime: String?,
        durationMinutes: Int = 30,
        recurrenceType: RecurrenceType = RecurrenceType.NONE,
        recurrenceInterval: Int = 1,
        recurrenceDaysOfWeek: List<String> = emptyList(),
        recurrenceEndDateIso: String? = null,
        subtasks: List<Subtask> = emptyList(),
        notes: String = "",
        isReminderEnabled: Boolean = false,
        reminderMinutesBefore: Int = 0
    ) {
        viewModelScope.launch {
            val task = TaskItem(
                id = id,
                title = title,
                emoji = emoji,
                description = description,
                category = category,
                priority = priority,
                dueDateIso = dueDateIso,
                dueTime = dueTime,
                durationMinutes = durationMinutes,
                recurrenceType = recurrenceType,
                recurrenceInterval = recurrenceInterval,
                recurrenceDaysOfWeek = recurrenceDaysOfWeek,
                recurrenceEndDateIso = recurrenceEndDateIso,
                subtasks = subtasks,
                notes = notes,
                isReminderEnabled = isReminderEnabled,
                reminderMinutesBefore = reminderMinutesBefore
            )

            val savedId = if (id == 0L) {
                repository.insertTask(task)
            } else {
                repository.updateTask(task)
                id
            }

            // Schedule or cancel alarm
            val fullTask = task.copy(id = savedId)
            if (isReminderEnabled) {
                val qStart = DateUtils.timeToMinutes(quietHoursStartFlow.value).coerceAtLeast(1260)
                val qEnd = DateUtils.timeToMinutes(quietHoursEndFlow.value).coerceAtLeast(330)
                NotificationHelper.scheduleTaskAlarm(
                    context = getApplication(),
                    task = fullTask,
                    dateIso = dueDateIso,
                    quietHoursStartMins = qStart,
                    quietHoursEndMins = qEnd
                )
            } else {
                NotificationHelper.cancelTaskAlarm(getApplication(), savedId, dueDateIso)
            }
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            NotificationHelper.cancelTaskAlarm(getApplication(), taskId)
            repository.deleteTask(taskId)
        }
    }

    fun editOccurrenceOnly(
        parentTaskId: Long,
        occurrenceDateIso: String,
        title: String,
        emoji: String,
        description: String,
        category: String,
        priority: TaskPriority,
        dueTime: String?,
        durationMinutes: Int,
        subtasks: List<Subtask>,
        notes: String,
        isReminderEnabled: Boolean,
        reminderMinutesBefore: Int
    ) {
        viewModelScope.launch {
            repository.skipOccurrence(parentTaskId, occurrenceDateIso)
            val standaloneTask = TaskItem(
                id = 0,
                title = title,
                emoji = emoji,
                description = description,
                category = category,
                priority = priority,
                dueDateIso = occurrenceDateIso,
                dueTime = dueTime,
                durationMinutes = durationMinutes,
                recurrenceType = RecurrenceType.NONE,
                subtasks = subtasks,
                notes = notes,
                isReminderEnabled = isReminderEnabled,
                reminderMinutesBefore = reminderMinutesBefore
            )
            val newId = repository.insertTask(standaloneTask)
            if (isReminderEnabled) {
                NotificationHelper.scheduleTaskAlarm(
                    context = getApplication(),
                    task = standaloneTask.copy(id = newId),
                    dateIso = occurrenceDateIso
                )
            }
        }
    }

    fun rescheduleOccurrence(taskId: Long, originalDateIso: String, newDateIso: String) {
        viewModelScope.launch {
            repository.rescheduleOccurrence(taskId, originalDateIso, newDateIso)
        }
    }

    fun skipOccurrence(taskId: Long, dateIso: String) {
        viewModelScope.launch {
            repository.skipOccurrence(taskId, dateIso)
        }
    }

    fun duplicateTask(taskId: Long) {
        viewModelScope.launch {
            repository.duplicateTask(taskId)
        }
    }

    fun setTaskArchived(taskId: Long, isArchived: Boolean) {
        viewModelScope.launch {
            repository.setTaskArchived(taskId, isArchived)
        }
    }

    fun setTaskPaused(taskId: Long, isPaused: Boolean) {
        viewModelScope.launch {
            repository.setTaskPaused(taskId, isPaused)
        }
    }

    // --- Bulk Selection Actions ---
    fun toggleSelectionMode(enabled: Boolean? = null) {
        _isSelectionMode.value = enabled ?: !_isSelectionMode.value
        if (!_isSelectionMode.value) {
            _selectedTaskIds.value = emptySet()
        }
    }

    fun toggleTaskSelection(taskId: Long) {
        val current = _selectedTaskIds.value.toMutableSet()
        if (current.contains(taskId)) {
            current.remove(taskId)
        } else {
            current.add(taskId)
        }
        _selectedTaskIds.value = current
        if (current.isNotEmpty() && !_isSelectionMode.value) {
            _isSelectionMode.value = true
        }
    }

    fun selectAllFilteredTasks() {
        _selectedTaskIds.value = tasksUiState.value.filteredTasks.map { it.id }.toSet()
        _isSelectionMode.value = true
    }

    fun clearSelection() {
        _selectedTaskIds.value = emptySet()
        _isSelectionMode.value = false
    }

    fun bulkCompleteSelected() {
        viewModelScope.launch {
            repository.bulkCompleteTasks(_selectedTaskIds.value.toList(), todayIso)
            clearSelection()
        }
    }

    fun bulkArchiveSelected() {
        viewModelScope.launch {
            repository.bulkArchiveTasks(_selectedTaskIds.value.toList(), true)
            clearSelection()
        }
    }

    fun bulkDeleteSelected() {
        viewModelScope.launch {
            repository.bulkDeleteTasks(_selectedTaskIds.value.toList())
            clearSelection()
        }
    }

    // --- Calendar Actions ---
    fun selectCalendarDate(dateIso: String) {
        _calendarSelectedDate.value = dateIso
    }

    fun previousCalendarMonth() {
        var m = _calendarMonth.value - 1
        var y = _calendarYear.value
        if (m < 0) {
            m = 11
            y -= 1
        }
        _calendarMonth.value = m
        _calendarYear.value = y
    }

    fun nextCalendarMonth() {
        var m = _calendarMonth.value + 1
        var y = _calendarYear.value
        if (m > 11) {
            m = 0
            y += 1
        }
        _calendarMonth.value = m
        _calendarYear.value = y
    }

    fun setCalendarViewMode(mode: String) {
        _calendarViewMode.value = mode
    }

    fun setCalendarCategoryFilter(category: String?) {
        _calendarCategoryFilter.value = category
    }

    fun setCalendarPriorityFilter(priority: TaskPriority?) {
        _calendarPriorityFilter.value = priority
    }

    fun setTaskSearchQuery(query: String) {
        _taskSearchQuery.value = query
    }

    fun setTaskFilter(filter: String, projectId: Long? = null, labelId: Long? = null) {
        _taskFilter.value = filter
        _taskProjectIdFilter.value = projectId
        _taskLabelIdFilter.value = labelId
    }

    fun selectProject(projectId: Long?) {
        _taskProjectIdFilter.value = projectId
        _taskLabelIdFilter.value = null
        _taskFilter.value = if (projectId != null) "Projects" else "All"
    }

    fun selectLabel(labelId: Long?) {
        _taskLabelIdFilter.value = labelId
        _taskProjectIdFilter.value = null
        _taskFilter.value = if (labelId != null) "Labels" else "All"
    }

    // --- Phase 3A Project CRUD Operations ---
    fun insertProject(project: ProjectItem, onComplete: ((Long) -> Unit)? = null) {
        viewModelScope.launch {
            val id = repository.insertProject(project)
            onComplete?.invoke(id)
        }
    }

    fun updateProject(project: ProjectItem) {
        viewModelScope.launch {
            repository.updateProject(project)
        }
    }

    fun setProjectArchived(projectId: Long, isArchived: Boolean) {
        viewModelScope.launch {
            repository.setProjectArchived(projectId, isArchived)
        }
    }

    fun setProjectFavorite(projectId: Long, isFavorite: Boolean) {
        viewModelScope.launch {
            repository.setProjectFavorite(projectId, isFavorite)
        }
    }

    fun deleteProject(projectId: Long) {
        viewModelScope.launch {
            if (_taskProjectIdFilter.value == projectId) {
                _taskProjectIdFilter.value = null
                _taskFilter.value = "All"
            }
            repository.deleteProject(projectId)
        }
    }

    // --- Phase 3A Label CRUD Operations ---
    fun insertLabel(label: LabelItem, onComplete: ((Long) -> Unit)? = null) {
        viewModelScope.launch {
            val id = repository.insertLabel(label)
            onComplete?.invoke(id)
        }
    }

    fun updateLabel(label: LabelItem) {
        viewModelScope.launch {
            repository.updateLabel(label)
        }
    }

    fun setLabelFavorite(labelId: Long, isFavorite: Boolean) {
        viewModelScope.launch {
            repository.setLabelFavorite(labelId, isFavorite)
        }
    }

    fun deleteLabel(labelId: Long) {
        viewModelScope.launch {
            if (_taskLabelIdFilter.value == labelId) {
                _taskLabelIdFilter.value = null
                _taskFilter.value = "All"
            }
            repository.deleteLabel(labelId)
        }
    }

    // --- Phase 3A Color Palette & Ordering Operations ---
    fun recordColorUsage(colorId: String) {
        viewModelScope.launch {
            preferences.recordColorUsage(colorId)
        }
    }

    fun setColorOrderingMode(mode: ColorOrderingMode) {
        viewModelScope.launch {
            preferences.setColorOrderingMode(mode.name)
        }
    }

    fun saveCustomColorOrder(order: List<String>) {
        viewModelScope.launch {
            preferences.saveCustomColorOrder(order)
        }
    }

    fun resetCustomColorOrder() {
        viewModelScope.launch {
            preferences.resetCustomColorOrder()
        }
    }

    fun saveTaskItem(task: TaskItem) {
        viewModelScope.launch {
            val savedId = if (task.id == 0L) {
                repository.insertTask(task)
            } else {
                repository.updateTask(task)
                task.id
            }
            val fullTask = task.copy(id = savedId)
            if (fullTask.isReminderEnabled) {
                val qStart = DateUtils.timeToMinutes(quietHoursStartFlow.value).coerceAtLeast(1260)
                val qEnd = DateUtils.timeToMinutes(quietHoursEndFlow.value).coerceAtLeast(330)
                NotificationHelper.scheduleTaskAlarm(
                    context = getApplication(),
                    task = fullTask,
                    dateIso = fullTask.dueDateIso,
                    quietHoursStartMins = qStart,
                    quietHoursEndMins = qEnd
                )
            } else {
                NotificationHelper.cancelTaskAlarm(getApplication(), savedId, fullTask.dueDateIso)
            }
        }
    }

    fun setTaskCategoryFilter(category: String?) {
        _taskCategoryFilter.value = category
    }

    fun setTaskSort(sort: String) {
        _taskSort.value = sort
    }

    // --- Habit Actions ---
    fun toggleHabit(habitId: Long, dateIso: String = todayIso) {
        viewModelScope.launch {
            repository.toggleHabitCompletion(habitId, dateIso)
        }
    }

    fun saveHabit(
        id: Long = 0,
        title: String,
        emoji: String,
        description: String,
        reminderTime: String?,
        colorHex: Long = 0xFF3B82F6
    ) {
        viewModelScope.launch {
            val habit = HabitItem(
                id = id,
                title = title,
                emoji = emoji,
                description = description,
                reminderTime = reminderTime,
                colorHex = colorHex
            )
            if (id == 0L) {
                repository.insertHabit(habit)
            } else {
                repository.updateHabit(habit)
            }
        }
    }

    fun deleteHabit(habitId: Long) {
        viewModelScope.launch {
            repository.deleteHabit(habitId)
        }
    }

    // --- 75-Day Challenge Actions ---
    fun toggleChallengeRule(ruleId: String, dateIso: String = todayIso) {
        viewModelScope.launch {
            repository.toggleChallengeRule(ruleId, dateIso)
        }
    }

    fun restartChallenge(startDateIso: String = todayIso) {
        viewModelScope.launch {
            repository.restartChallenge(startDateIso)
        }
    }

    // --- Water Tracking Actions ---
    fun addWater(deltaMl: Int) {
        viewModelScope.launch {
            val currentGoal = waterGoalFlow.value
            repository.addWater(deltaMl, todayIso, currentGoal)
        }
    }

    fun resetWater() {
        viewModelScope.launch {
            repository.resetWater(todayIso)
        }
    }

    // --- Routine Installation Action ---
    fun installDefaultRoutines(force: Boolean = false) {
        viewModelScope.launch {
            repository.installDefaultRoutinesAndHabits(force)
        }
    }

    // --- Preferences Actions ---
    fun updateUserName(name: String) {
        viewModelScope.launch { preferences.setUserName(name) }
    }

    fun updateThemeMode(mode: String) {
        viewModelScope.launch { preferences.setThemeMode(mode) }
    }

    fun selectTheme(themeId: ThemeId): Boolean {
        val isUnlocked = entitlementRepo.isThemeUnlocked(themeId, isPremiumActive.value)
        if (!isUnlocked) {
            return false
        }
        viewModelScope.launch {
            preferences.setActiveThemeId(themeId.idString)
            _previewThemeId.value = null
        }
        return true
    }

    fun startThemePreview(themeId: ThemeId) {
        _previewThemeId.value = themeId
    }

    fun applyPreviewedTheme(): Boolean {
        val preview = _previewThemeId.value ?: return false
        return selectTheme(preview)
    }

    fun dismissThemePreview() {
        _previewThemeId.value = null
    }

    fun toggleSimulatedPremium(active: Boolean) {
        viewModelScope.launch {
            entitlementRepo.setSimulatedPremium(active)
        }
    }

    fun isThemeUnlocked(themeId: ThemeId): Boolean {
        return entitlementRepo.isThemeUnlocked(themeId, isPremiumActive.value)
    }

    fun updateWaterGoal(goalMl: Int) {
        viewModelScope.launch { preferences.setWaterGoalMl(goalMl) }
    }

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setNotificationsEnabled(enabled)
            if (enabled) {
                NotificationHelper.scheduleDailyReminder(getApplication(), 8, 0)
            }
        }
    }

    fun updateWakeUpTime(time: String) {
        viewModelScope.launch { preferences.setWakeUpTime(time) }
    }

    fun updateBedTime(time: String) {
        viewModelScope.launch { preferences.setBedTime(time) }
    }

    fun updateQuietHours(start: String, end: String) {
        viewModelScope.launch { preferences.setQuietHours(start, end) }
    }

    fun updateClockFormat(format: String) {
        viewModelScope.launch { preferences.setClockFormat(format) }
    }

    fun updateFirstDayOfWeek(day: String) {
        viewModelScope.launch { preferences.setFirstDayOfWeek(day) }
    }

    fun updateDefaultTaskDuration(duration: Int) {
        viewModelScope.launch { preferences.setDefaultTaskDuration(duration) }
    }

    // --- Honeycomb Actions ---
    fun setHoneycombEditMode(enabled: Boolean) {
        _isHoneycombEditMode.value = enabled
    }

    fun toggleHoneycombShortcut(shortcut: HoneycombShortcut) {
        viewModelScope.launch {
            when (shortcut.type) {
                ShortcutType.TASK -> {
                    val taskId = shortcut.targetId.toLongOrNull()
                    val targetTask = if (taskId != null) {
                        repository.getTaskById(taskId)
                    } else {
                        allTasks.value.find { it.title.contains(shortcut.targetId, ignoreCase = true) }
                    }
                    if (targetTask != null) {
                        toggleTaskCompletion(targetTask.id, todayIso)
                    }
                }
                ShortcutType.HABIT -> {
                    val habitId = shortcut.targetId.toLongOrNull()
                    val targetHabit = if (habitId != null) {
                        repository.getHabitById(habitId)
                    } else {
                        habits.value.find { it.title.contains(shortcut.targetId, ignoreCase = true) }
                    }
                    if (targetHabit != null) {
                        toggleHabit(targetHabit.id, todayIso)
                    }
                }
                ShortcutType.CHALLENGE_RULE -> {
                    val rule = challengeState.value.rules.find {
                        it.id == shortcut.targetId || it.title.contains(shortcut.targetId, ignoreCase = true)
                    }
                    if (rule != null) {
                        toggleChallengeRule(rule.id, todayIso)
                    }
                }
                ShortcutType.WATER -> {
                    addWater(250)
                }
                ShortcutType.CUSTOM -> {
                    // Custom non-binary toggle
                }
            }
        }
    }

    fun reorderHoneycombShortcuts(newList: List<HoneycombShortcut>) {
        viewModelScope.launch {
            repository.saveHoneycombShortcutsOrder(newList)
        }
    }

    fun moveHoneycombShortcut(shortcutId: Long, delta: Int) {
        viewModelScope.launch {
            val currentList = resolvedHoneycombShortcuts.value.toMutableList()
            val index = currentList.indexOfFirst { it.id == shortcutId }
            if (index != -1) {
                val targetIndex = (index + delta).coerceIn(0, currentList.size - 1)
                if (targetIndex != index) {
                    val item = currentList.removeAt(index)
                    currentList.add(targetIndex, item)
                    repository.saveHoneycombShortcutsOrder(currentList)
                }
            }
        }
    }

    fun addHoneycombShortcut(shortcut: HoneycombShortcut) {
        viewModelScope.launch {
            val maxOrder = resolvedHoneycombShortcuts.value.maxOfOrNull { it.orderIndex } ?: 0
            repository.insertHoneycombShortcut(shortcut.copy(orderIndex = maxOrder + 1))
        }
    }

    fun removeHoneycombShortcut(shortcutId: Long) {
        viewModelScope.launch {
            repository.deleteHoneycombShortcut(shortcutId)
        }
    }

    fun updateHoneycombShortcutIcon(shortcutId: Long, newEmoji: String) {
        viewModelScope.launch {
            val item = resolvedHoneycombShortcuts.value.find { it.id == shortcutId } ?: return@launch
            repository.updateHoneycombShortcut(item.copy(emoji = newEmoji))
        }
    }

    fun updateHoneycombShortcutColor(shortcutId: Long, newColorHex: Long?) {
        viewModelScope.launch {
            val item = resolvedHoneycombShortcuts.value.find { it.id == shortcutId } ?: return@launch
            repository.updateHoneycombShortcut(item.copy(customColorHex = newColorHex))
        }
    }

    fun resetHoneycombLayoutToDefault() {
        viewModelScope.launch {
            repository.resetHoneycombShortcutsToDefault()
            _isHoneycombEditMode.value = false
        }
    }

    // --- Honeycomb Appearance Settings ---
    fun setHoneycombDefaultColor(hex: Long) = viewModelScope.launch { preferences.setHoneycombDefaultColor(hex) }
    fun setHoneycombBorderColor(type: String) = viewModelScope.launch { preferences.setHoneycombBorderColor(type) }
    fun setHoneycombBorderWidth(widthDp: Int) = viewModelScope.launch { preferences.setHoneycombBorderWidth(widthDp) }
    fun setHoneycombSize(sizeDp: Int) = viewModelScope.launch { preferences.setHoneycombSize(sizeDp) }
    fun setHoneycombSpacing(spacingDp: Int) = viewModelScope.launch { preferences.setHoneycombSpacing(spacingDp) }
    fun setHoneycombIconSize(sizeSp: Int) = viewModelScope.launch { preferences.setHoneycombIconSize(sizeSp) }
    fun setHoneycombAnimationEnabled(enabled: Boolean) = viewModelScope.launch { preferences.setHoneycombAnimationEnabled(enabled) }
    fun resetHoneycombAppearance() = viewModelScope.launch { preferences.resetHoneycombAppearance() }

    // --- 75 Hard Customization Actions ---
    fun updateChallengeRules(rules: List<ChallengeRule>, resetProgress: Boolean = false) {
        viewModelScope.launch {
            repository.updateChallengeRules(rules, resetProgress)
        }
    }

    fun applyChallengePreset(preset: ChallengePreset, resetProgress: Boolean = false) {
        viewModelScope.launch {
            repository.applyChallengePreset(preset, resetProgress)
        }
    }

    fun addCustomChallengeRule(title: String, emoji: String, description: String) {
        viewModelScope.launch {
            repository.addCustomChallengeRule(title, emoji, description)
        }
    }

    fun removeChallengeRule(ruleId: String) {
        viewModelScope.launch {
            repository.removeChallengeRule(ruleId)
        }
    }

    fun editChallengeRule(ruleId: String, title: String, emoji: String, description: String) {
        viewModelScope.launch {
            repository.editChallengeRule(ruleId, title, emoji, description)
        }
    }

    // --- Phase 3 Master: Trash Bin ---
    val trashItemsFlow: StateFlow<List<TrashItem>> = repository.allTrashItemsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun softDeleteTask(task: TaskItem) = viewModelScope.launch { repository.softDeleteTask(task) }
    fun softDeleteHabit(habit: HabitItem) = viewModelScope.launch { repository.softDeleteHabit(habit) }
    fun softDeleteProject(project: ProjectItem) = viewModelScope.launch { repository.softDeleteProject(project) }
    fun softDeleteLabel(label: LabelItem) = viewModelScope.launch { repository.softDeleteLabel(label) }
    fun restoreTrashItem(id: Long) = viewModelScope.launch { repository.restoreTrashItem(id) }
    fun deleteTrashItemPermanently(id: Long) = viewModelScope.launch { repository.deleteTrashItemPermanently(id) }
    fun clearTrash() = viewModelScope.launch { repository.clearTrash() }

    // --- Phase 3 Master: Planning Engine ---
    val allPlansFlow: StateFlow<List<PlanItem>> = repository.allPlansFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun insertPlan(plan: PlanItem, milestoneTitles: List<String>, linkedTaskIds: List<Long>, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.insertPlan(plan, milestoneTitles, linkedTaskIds)
            repository.awardXpAndCoins("PLAN_${System.currentTimeMillis()}", 25, 5, "Strategic plan created: ${plan.title}")
            repository.checkAndUnlockAchievement("PLANNER_INITIATE")
            onComplete()
        }
    }

    fun updatePlan(plan: PlanItem) = viewModelScope.launch { repository.updatePlan(plan) }
    fun deletePlan(id: Long) = viewModelScope.launch { repository.deletePlan(id) }
    fun getMilestonesForPlan(planId: Long) = repository.getMilestonesForPlan(planId)

    // --- Phase 3 Master: Gamification & Rewards ---
    val gamificationProfile: StateFlow<GamificationProfile> = repository.gamificationProfileFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GamificationProfile()
    )

    val allAchievements: StateFlow<List<AchievementItem>> = repository.allAchievementsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val activeRewards: StateFlow<List<RewardItem>> = repository.activeRewardsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun redeemReward(rewardId: Long, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val success = repository.redeemReward(rewardId)
            onResult(success)
        }
    }

    fun insertReward(title: String, description: String, cost: Int, emoji: String) {
        viewModelScope.launch {
            repository.insertReward(RewardItem(title = title, description = description, coinCost = cost, iconEmoji = emoji))
        }
    }

    // --- Honeycomb Custom Layouts ---
    fun applyHoneycombTemplate(template: HoneycombLayoutTemplate) {
        viewModelScope.launch {
            repository.applyHoneycombTemplate(template)
        }
    }

    fun updateShortcutCoords(id: Long, q: Int, r: Int, orderIndex: Int) {
        viewModelScope.launch {
            repository.updateShortcutCoords(id, q, r, orderIndex)
        }
    }

    // --- Calendar Date Navigation ---
    fun previousCalendarDay() {
        val prev = DateUtils.addDays(_calendarSelectedDate.value, -1)
        selectCalendarDate(prev)
    }

    fun nextCalendarDay() {
        val next = DateUtils.addDays(_calendarSelectedDate.value, 1)
        selectCalendarDate(next)
    }

    fun previousCalendarWeek() {
        val prev = DateUtils.addDays(_calendarSelectedDate.value, -7)
        selectCalendarDate(prev)
    }

    fun nextCalendarWeek() {
        val next = DateUtils.addDays(_calendarSelectedDate.value, 7)
        selectCalendarDate(next)
    }

    // --- Backup & Restore ---
    suspend fun exportDataJson(): String {
        return DataExportImportManager.exportAllData(AppDatabase.getInstance(getApplication()))
    }

    suspend fun importDataJson(jsonStr: String): Boolean {
        return DataExportImportManager.importData(AppDatabase.getInstance(getApplication()), jsonStr)
    }
}

class LifeViewModelFactory(
    private val application: Application,
    private val repository: LifeRepository,
    private val preferences: UserPreferences
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LifeViewModel::class.java)) {
            return LifeViewModel(application, repository, preferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
