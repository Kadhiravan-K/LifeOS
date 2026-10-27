package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.Alignment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.example.ui.viewmodel.LifeViewModel

@Composable
fun MainScreen(
    viewModel: LifeViewModel,
    modifier: Modifier = Modifier
) {
    var currentItem by rememberSaveable { mutableStateOf(NavigationItem.HOME) }
    var showThemeGallery by rememberSaveable { mutableStateOf(false) }

    val previewThemeId by viewModel.previewThemeId.collectAsStateWithLifecycle()
    val activeThemeDef by viewModel.activeThemeDefinition.collectAsStateWithLifecycle()

    // Intercept back button
    if (showThemeGallery) {
        BackHandler {
            viewModel.dismissThemePreview()
            showThemeGallery = false
        }
    } else if (currentItem != NavigationItem.HOME) {
        BackHandler {
            currentItem = NavigationItem.HOME
        }
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

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_navigation_bar")
            ) {
                NavigationItem.bottomNavItems.forEach { item ->
                    val isSelected = currentItem == item
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentItem = item },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label
                            )
                        },
                        label = {
                            Text(
                                text = item.label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_item_${item.route}")
                    )
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (showThemeGallery) {
            ThemeMotifLoadingTransition(label = "Theme Gallery") {
                com.example.ui.screens.themes.ThemeGalleryScreen(
                    viewModel = viewModel,
                    onNavigateBack = { showThemeGallery = false },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        } else {
            androidx.compose.foundation.layout.Column(modifier = Modifier.padding(innerPadding)) {
                // Persistent Preview Notification Banner across all tabs when in Preview Mode
                androidx.compose.animation.AnimatedVisibility(
                    visible = previewThemeId != null,
                    enter = androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.fadeOut()
                ) {
                    androidx.compose.material3.Surface(
                        color = activeThemeDef.colors.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, activeThemeDef.colors.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${activeThemeDef.decoration.motifEmoji} Previewing ${activeThemeDef.name}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = activeThemeDef.colors.onSurface
                                )
                            }
                            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                androidx.compose.material3.TextButton(
                                    onClick = { viewModel.dismissThemePreview() },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("Exit Preview", fontSize = 11.sp)
                                }
                                androidx.compose.material3.Button(
                                    onClick = {
                                        val applied = viewModel.applyPreviewedTheme()
                                        if (!applied) {
                                            showThemeGallery = true
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
                                onNavigateToTasks = { currentItem = NavigationItem.TASKS },
                                onNavigateToHabits = { currentItem = NavigationItem.HABITS },
                                onNavigateToChallenge = { currentItem = NavigationItem.CHALLENGE },
                                onNavigateToCalendar = { currentItem = NavigationItem.CALENDAR },
                                onNavigateToPlanning = { currentItem = NavigationItem.PLANNING },
                                onNavigateToGamification = { currentItem = NavigationItem.GAMIFICATION },
                                onNavigateToAnalytics = { currentItem = NavigationItem.ANALYTICS }
                            )
                        }

                        NavigationItem.TASKS -> {
                            TasksScreen(
                                viewModel = viewModel,
                                uiState = tasksState,
                                onNavigateToHabits = { currentItem = NavigationItem.HABITS },
                                onNavigateToChallenge = { currentItem = NavigationItem.CHALLENGE }
                            )
                        }

                        NavigationItem.CALENDAR -> {
                            CalendarScreen(
                                viewModel = viewModel,
                                uiState = calendarState
                            )
                        }

                        NavigationItem.HABITS -> {
                            BackHandler { currentItem = NavigationItem.TASKS }
                            HabitsScreen(
                                viewModel = viewModel,
                                uiState = habitsState
                            )
                        }

                        NavigationItem.CHALLENGE -> {
                            BackHandler { currentItem = NavigationItem.TASKS }
                            ChallengeScreen(
                                viewModel = viewModel,
                                challenge = challengeState
                            )
                        }

                        NavigationItem.PLANNING -> {
                            BackHandler { currentItem = NavigationItem.HOME }
                            PlanningScreen(
                                viewModel = viewModel,
                                onBack = { currentItem = NavigationItem.HOME }
                            )
                        }

                        NavigationItem.GAMIFICATION -> {
                            BackHandler { currentItem = NavigationItem.HOME }
                            GamificationScreen(
                                viewModel = viewModel,
                                onBack = { currentItem = NavigationItem.HOME }
                            )
                        }

                        NavigationItem.ANALYTICS -> {
                            BackHandler { currentItem = NavigationItem.HOME }
                            AnalyticsScreen(
                                viewModel = viewModel,
                                onBack = { currentItem = NavigationItem.HOME }
                            )
                        }

                        NavigationItem.MEDICATION -> {
                            BackHandler { currentItem = NavigationItem.HOME }
                            MedicationScreen(
                                viewModel = viewModel,
                                onNavigateBack = { currentItem = NavigationItem.HOME },
                                onAddNewMedication = { currentItem = NavigationItem.TASKS }
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
                                onNavigateToThemeGallery = { showThemeGallery = true },
                                onNavigateToPlanning = { currentItem = NavigationItem.PLANNING },
                                onNavigateToGamification = { currentItem = NavigationItem.GAMIFICATION },
                                onNavigateToMedication = { currentItem = NavigationItem.MEDICATION }
                            )
                        }
                    }
                }
                }
            }
        }
    }
}
