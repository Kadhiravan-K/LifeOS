package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
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
import com.example.ui.screens.calendar.CalendarScreen
import com.example.ui.screens.challenge.ChallengeScreen
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.habits.HabitsScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.tasks.TasksScreen
import com.example.ui.viewmodel.LifeViewModel

@Composable
fun MainScreen(
    viewModel: LifeViewModel,
    modifier: Modifier = Modifier
) {
    var currentItem by rememberSaveable { mutableStateOf(NavigationItem.HOME) }

    // Intercept back button on secondary screens to return to Home
    if (currentItem != NavigationItem.HOME) {
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
                NavigationItem.entries.forEach { item ->
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
        Crossfade(
            targetState = currentItem,
            label = "screen_transition",
            modifier = Modifier.padding(innerPadding)
        ) { destination ->
            when (destination) {
                NavigationItem.HOME -> {
                    DashboardScreen(
                        viewModel = viewModel,
                        uiState = dashboardState,
                        onNavigateToTasks = { currentItem = NavigationItem.TASKS },
                        onNavigateToHabits = { currentItem = NavigationItem.HABITS },
                        onNavigateToChallenge = { currentItem = NavigationItem.CHALLENGE },
                        onNavigateToCalendar = { currentItem = NavigationItem.CALENDAR }
                    )
                }

                NavigationItem.TASKS -> {
                    TasksScreen(
                        viewModel = viewModel,
                        uiState = tasksState
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
                        defaultDuration = defaultDuration
                    )
                }
            }
        }
    }
}
