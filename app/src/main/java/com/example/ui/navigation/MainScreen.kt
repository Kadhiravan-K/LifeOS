package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.ProjectItem
import com.example.domain.model.TaskItem
import com.example.ui.components.HabitEditModal
import com.example.ui.components.TaskAdvancedEditorSheet
import com.example.ui.components.TaskQuickAddSheet
import com.example.ui.screens.analytics.AnalyticsScreen
import com.example.ui.screens.calendar.CalendarScreen
import com.example.ui.screens.challenge.ChallengeScreen
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.gamification.GamificationScreen
import com.example.ui.screens.habits.HabitsScreen
import com.example.ui.screens.medication.MedicationScreen
import com.example.ui.screens.planning.PlanningScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.tasks.TasksScreen
import com.example.ui.theme.engine.ThemeId
import com.example.ui.viewmodel.LifeViewModel

@Composable
fun MainScreen(
    viewModel: LifeViewModel,
    modifier: Modifier = Modifier,
    initialDestination: NavigationItem = NavigationItem.HOME,
    initialOpenQuickAdd: Boolean = false
) {
    val navManager = rememberNavigationStateManager(initialDestination = initialDestination)
    val currentItem = navManager.currentDestination

    var showQuickAddMenuSheet by rememberSaveable { mutableStateOf(false) }
    var showTaskQuickAddSheet by rememberSaveable { mutableStateOf(initialOpenQuickAdd) }
    var showTaskAdvancedEditorSheet by rememberSaveable { mutableStateOf(false) }
    var taskDraftForAdvancedEditor by remember { mutableStateOf<TaskItem?>(null) }
    var showCreateHabitModal by rememberSaveable { mutableStateOf(false) }
    var showCreateProjectDialog by rememberSaveable { mutableStateOf(false) }
    var newProjectName by rememberSaveable { mutableStateOf("") }

    val previewThemeId by viewModel.previewThemeId.collectAsStateWithLifecycle()
    val activeThemeDef by viewModel.activeThemeDefinition.collectAsStateWithLifecycle()

    // Central Back-Stack Traversal
    BackHandler(enabled = navManager.backStack.size > 1) {
        if (navManager.currentDestination == NavigationItem.THEMES) {
            viewModel.dismissThemePreview()
        }
        navManager.popBack()
    }

    val dashboardState by viewModel.dashboardUiState.collectAsStateWithLifecycle()
    val tasksState by viewModel.tasksUiState.collectAsStateWithLifecycle()
    val calendarState by viewModel.calendarUiState.collectAsStateWithLifecycle()
    val habitsState by viewModel.habitsUiState.collectAsStateWithLifecycle()
    val challengeState by viewModel.challengeState.collectAsStateWithLifecycle()
    val userName by viewModel.userNameFlow.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeModeFlow.collectAsStateWithLifecycle()
    val waterGoal by viewModel.waterGoalFlow.collectAsStateWithLifecycle()
    val notificationsEnabled by viewModel.notificationsEnabledFlow.collectAsStateWithLifecycle()
    val hasInstalledDefaults by viewModel.hasInstalledDefaultsFlow.collectAsStateWithLifecycle()

    val wakeUpTime by viewModel.wakeUpTimeFlow.collectAsStateWithLifecycle()
    val bedTime by viewModel.bedTimeFlow.collectAsStateWithLifecycle()
    val quietHoursStart by viewModel.quietHoursStartFlow.collectAsStateWithLifecycle()
    val quietHoursEnd by viewModel.quietHoursEndFlow.collectAsStateWithLifecycle()
    val clockFormat by viewModel.clockFormatFlow.collectAsStateWithLifecycle()
    val firstDayOfWeek by viewModel.firstDayOfWeekFlow.collectAsStateWithLifecycle()
    val defaultDuration by viewModel.defaultTaskDurationFlow.collectAsStateWithLifecycle()
    val colorOrderingMode by viewModel.colorOrderingMode.collectAsStateWithLifecycle()
    val colorUsageCounts by viewModel.colorUsageCounts.collectAsStateWithLifecycle()
    val colorCustomOrder by viewModel.colorCustomOrder.collectAsStateWithLifecycle()

    // Release 4 Collection 1: Configurable Bottom Navigation Slots
    val rawSlot2 by viewModel.bottomNavSlot2Flow.collectAsStateWithLifecycle()
    val rawSlot4 by viewModel.bottomNavSlot4Flow.collectAsStateWithLifecycle()
    val slot2Item = remember(rawSlot2) { NavigationItem.fromSlotKey(rawSlot2, NavigationItem.TASKS) }
    val slot4Item = remember(rawSlot4) { NavigationItem.fromSlotKey(rawSlot4, NavigationItem.CALENDAR) }

    // Release 4 Collection 1: Per-Theme Quick Add Animation Preferences
    val animArcane by viewModel.animArcaneEnabled.collectAsStateWithLifecycle()
    val animCyber by viewModel.animCyberEnabled.collectAsStateWithLifecycle()
    val animAdventure by viewModel.animAdventureEnabled.collectAsStateWithLifecycle()
    val animCosmic by viewModel.animCosmicEnabled.collectAsStateWithLifecycle()
    val animGlass by viewModel.animGlassEnabled.collectAsStateWithLifecycle()

    val isThemeAnimEnabled = when (activeThemeDef.id) {
        ThemeId.ARCANE_ACADEMY -> animArcane
        ThemeId.CYBER_NEXUS -> animCyber
        ThemeId.ADVENTURE_REALM -> animAdventure
        ThemeId.COSMIC_EXPLORER -> animCosmic
        ThemeId.GLASS -> animGlass
        else -> true
    }

    Scaffold(
        bottomBar = {
            LifeOSBottomNavigation(
                currentDestination = currentItem,
                slot2Item = slot2Item,
                slot4Item = slot4Item,
                activeTheme = activeThemeDef,
                isThemeAnimationEnabled = isThemeAnimEnabled,
                onItemSelected = { item ->
                    navManager.onBottomNavSelected(item)
                },
                onQuickAddClick = {
                    showQuickAddMenuSheet = true
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            // Persistent Preview Notification Banner across all tabs when in Preview Mode
            androidx.compose.animation.AnimatedVisibility(
                visible = previewThemeId != null,
                enter = androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.fadeOut()
            ) {
                Surface(
                    color = activeThemeDef.colors.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, activeThemeDef.colors.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${activeThemeDef.decoration.motifEmoji} Previewing ${activeThemeDef.name}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = activeThemeDef.colors.onSurface
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TextButton(
                                onClick = {
                                    viewModel.dismissThemePreview()
                                    if (currentItem == NavigationItem.THEMES) {
                                        navManager.popBack()
                                    }
                                },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Exit Preview", fontSize = 11.sp)
                            }
                            Button(
                                onClick = {
                                    val applied = viewModel.applyPreviewedTheme()
                                    if (!applied) {
                                        navManager.navigateTo(NavigationItem.THEMES)
                                    }
                                },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                            ) {
                                Text("Apply", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            MainNavigationTransition(
                targetItem = currentItem,
                modifier = Modifier.fillMaxSize()
            ) {
                Crossfade(
                    targetState = currentItem,
                    label = "screen_transition",
                    modifier = Modifier.fillMaxSize()
                ) { destination ->
                    when (destination) {
                        NavigationItem.HOME -> {
                            DashboardScreen(
                                viewModel = viewModel,
                                uiState = dashboardState,
                                onNavigateToTasks = { navManager.navigateTo(NavigationItem.TASKS) },
                                onNavigateToHabits = { navManager.navigateTo(NavigationItem.HABITS) },
                                onNavigateToChallenge = { navManager.navigateTo(NavigationItem.CHALLENGE) },
                                onNavigateToCalendar = { navManager.navigateTo(NavigationItem.CALENDAR) },
                                onNavigateToPlanning = { navManager.navigateTo(NavigationItem.PLANNING) },
                                onNavigateToGamification = { navManager.navigateTo(NavigationItem.GAMIFICATION) },
                                onNavigateToAnalytics = { navManager.navigateTo(NavigationItem.ANALYTICS) }
                            )
                        }

                        NavigationItem.TASKS -> {
                            TasksScreen(
                                viewModel = viewModel,
                                uiState = tasksState,
                                onNavigateToHabits = { navManager.navigateTo(NavigationItem.HABITS) },
                                onNavigateToChallenge = { navManager.navigateTo(NavigationItem.CHALLENGE) }
                            )
                        }

                        NavigationItem.CALENDAR -> {
                            CalendarScreen(
                                viewModel = viewModel,
                                uiState = calendarState
                            )
                        }

                        NavigationItem.HABITS -> {
                            HabitsScreen(
                                viewModel = viewModel,
                                uiState = habitsState
                            )
                        }

                        NavigationItem.CHALLENGE -> {
                            ChallengeScreen(
                                viewModel = viewModel,
                                challenge = challengeState
                            )
                        }

                        NavigationItem.PLANNING -> {
                            PlanningScreen(
                                viewModel = viewModel,
                                onBack = { navManager.popBack() }
                            )
                        }

                        NavigationItem.GAMIFICATION -> {
                            GamificationScreen(
                                viewModel = viewModel,
                                onBack = { navManager.popBack() }
                            )
                        }

                        NavigationItem.ANALYTICS -> {
                            AnalyticsScreen(
                                viewModel = viewModel,
                                onBack = { navManager.popBack() }
                            )
                        }

                        NavigationItem.MEDICATION -> {
                            MedicationScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navManager.popBack() },
                                onAddNewMedication = { navManager.navigateTo(NavigationItem.TASKS) }
                            )
                        }

                        NavigationItem.THEMES -> {
                            ThemeMotifLoadingTransition(label = "Theme Gallery") {
                                com.example.ui.screens.themes.ThemeGalleryScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = {
                                        viewModel.dismissThemePreview()
                                        navManager.popBack()
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        NavigationItem.SHIFTS -> {
                            com.example.ui.screens.shifts.ShiftPlannerScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navManager.popBack() },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        NavigationItem.GIVE_GET -> {
                            com.example.ui.screens.giveget.GiveGetScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navManager.popBack() },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        NavigationItem.SETTINGS -> {
                            SettingsScreen(
                                viewModel = viewModel,
                                userName = userName,
                                themeMode = themeMode,
                                waterGoalMl = waterGoal,
                                notificationsEnabled = notificationsEnabled,
                                hasInstalledDefaults = hasInstalledDefaults,
                                wakeUpTime = wakeUpTime,
                                bedTime = bedTime,
                                quietHoursStart = quietHoursStart,
                                quietHoursEnd = quietHoursEnd,
                                clockFormat = clockFormat,
                                firstDayOfWeek = firstDayOfWeek,
                                defaultDuration = defaultDuration,
                                resetTrigger = navManager.settingsResetTrigger,
                                onNavigateToThemeGallery = { navManager.navigateTo(NavigationItem.THEMES) },
                                onNavigateToPlanning = { navManager.navigateTo(NavigationItem.PLANNING) },
                                onNavigateToGamification = { navManager.navigateTo(NavigationItem.GAMIFICATION) },
                                onNavigateToMedication = { navManager.navigateTo(NavigationItem.MEDICATION) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Quick Add Menu Action Sheet
    if (showQuickAddMenuSheet) {
        QuickAddMenuSheet(
            onDismiss = { showQuickAddMenuSheet = false },
            onAddTask = {
                showQuickAddMenuSheet = false
                showTaskQuickAddSheet = true
            },
            onAddHabit = {
                showQuickAddMenuSheet = false
                showCreateHabitModal = true
            },
            onAddReminder = {
                showQuickAddMenuSheet = false
                showTaskQuickAddSheet = true
            },
            onAddProject = {
                showQuickAddMenuSheet = false
                showCreateProjectDialog = true
            },
            onAddMedication = {
                showQuickAddMenuSheet = false
                navManager.navigateTo(NavigationItem.MEDICATION)
            },
            onAddGiveGet = {
                showQuickAddMenuSheet = false
                navManager.navigateTo(NavigationItem.GIVE_GET)
            },
            onNavigateToShifts = {
                showQuickAddMenuSheet = false
                navManager.navigateTo(NavigationItem.SHIFTS)
            }
        )
    }

    // Task Quick Add Sheet
    if (showTaskQuickAddSheet) {
        TaskQuickAddSheet(
            projects = tasksState.projects,
            labels = tasksState.labels,
            colorOrderingMode = colorOrderingMode,
            colorUsageCounts = colorUsageCounts,
            colorCustomOrder = colorCustomOrder,
            defaultDuration = defaultDuration,
            onCreateProject = { proj, cb -> viewModel.insertProject(proj) { newId -> cb(newId) } },
            onCreateLabel = { lbl, cb -> viewModel.insertLabel(lbl) { newId -> cb(newId) } },
            onColorUsed = { viewModel.recordColorUsage(it) },
            onModeChanged = { viewModel.setColorOrderingMode(it) },
            onSaveCustomOrder = { viewModel.saveCustomColorOrder(it) },
            onResetCustomOrder = { viewModel.resetCustomColorOrder() },
            onSaveTask = { task ->
                viewModel.saveTaskItem(task)
                showTaskQuickAddSheet = false
            },
            onSaveTaskWithMedication = { task, med ->
                viewModel.saveTaskItem(task, med)
                showTaskQuickAddSheet = false
            },
            onExpandToAdvanced = { draft ->
                taskDraftForAdvancedEditor = draft
                showTaskQuickAddSheet = false
                showTaskAdvancedEditorSheet = true
                navManager.navigateTo(NavigationItem.TASKS)
            },
            onDismiss = { showTaskQuickAddSheet = false }
        )
    }

    // Task Advanced Editor Sheet (opened from Quick Add expand)
    if (showTaskAdvancedEditorSheet) {
        TaskAdvancedEditorSheet(
            task = taskDraftForAdvancedEditor,
            projects = tasksState.projects,
            labels = tasksState.labels,
            colorOrderingMode = colorOrderingMode,
            colorUsageCounts = colorUsageCounts,
            colorCustomOrder = colorCustomOrder,
            defaultDuration = defaultDuration,
            onCreateProject = { proj, cb -> viewModel.insertProject(proj) { newId -> cb(newId) } },
            onCreateLabel = { lbl, cb -> viewModel.insertLabel(lbl) { newId -> cb(newId) } },
            onColorUsed = { viewModel.recordColorUsage(it) },
            onModeChanged = { viewModel.setColorOrderingMode(it) },
            onSaveCustomOrder = { viewModel.saveCustomColorOrder(it) },
            onResetCustomOrder = { viewModel.resetCustomColorOrder() },
            onSaveTask = { task ->
                viewModel.saveTaskItem(task)
                showTaskAdvancedEditorSheet = false
                taskDraftForAdvancedEditor = null
            },
            onSaveTaskWithMedication = { task, med ->
                viewModel.saveTaskItem(task, med)
                showTaskAdvancedEditorSheet = false
                taskDraftForAdvancedEditor = null
            },
            onDismiss = {
                showTaskAdvancedEditorSheet = false
                taskDraftForAdvancedEditor = null
            }
        )
    }

    // Create Habit Modal
    if (showCreateHabitModal) {
        HabitEditModal(
            initialHabit = null,
            onDismiss = { showCreateHabitModal = false },
            onSave = { title, emoji, description, reminderTime ->
                viewModel.saveHabit(
                    title = title,
                    emoji = emoji,
                    description = description,
                    reminderTime = reminderTime
                )
                showCreateHabitModal = false
            }
        )
    }

    // Create Project Dialog
    if (showCreateProjectDialog) {
        AlertDialog(
            onDismissRequest = { showCreateProjectDialog = false },
            title = { Text("New Project") },
            text = {
                OutlinedTextField(
                    value = newProjectName,
                    onValueChange = { newProjectName = it },
                    label = { Text("Project Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("quick_add_project_name_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newProjectName.isNotBlank()) {
                            viewModel.insertProject(
                                ProjectItem(
                                    name = newProjectName.trim(),
                                    colorId = "blue"
                                )
                            ) {
                                newProjectName = ""
                                showCreateProjectDialog = false
                            }
                        }
                    },
                    enabled = newProjectName.isNotBlank(),
                    modifier = Modifier.testTag("quick_add_project_confirm")
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    newProjectName = ""
                    showCreateProjectDialog = false
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}
