package com.example.ui.screens.planning

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.HabitItem
import com.example.domain.model.PlanItem
import com.example.domain.model.PlanLevel
import com.example.domain.model.PlanMilestone
import com.example.domain.model.PlanStatus
import com.example.domain.model.RecurrenceType
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskPriority
import com.example.domain.recurrence.RecurrenceEngine
import com.example.ui.components.EmptyStateCard
import com.example.ui.viewmodel.LifeViewModel
import com.example.utils.DateUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Requirement #10: Four Distinct Planning Boards (Daily, Weekly, Monthly, Yearly).
 * Features independent planning content, active profile isolation, full CRUD, task linking,
 * completion progress metrics, empty states, and period navigation.
 */
enum class PlanningBoardTab(val title: String, val emoji: String, val level: PlanLevel) {
    DAILY("Daily", "☀️", PlanLevel.DAILY),
    WEEKLY("Weekly", "📅", PlanLevel.WEEKLY),
    MONTHLY("Monthly", "🗓️", PlanLevel.MONTHLY),
    YEARLY("Yearly", "🌟", PlanLevel.YEARLY);
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanningScreen(
    viewModel: LifeViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val plans by viewModel.allPlansFlow.collectAsStateWithLifecycle()
    val allTasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val habits by viewModel.habits.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(PlanningBoardTab.DAILY) }
    var selectedDateIso by remember { mutableStateOf(DateUtils.todayString()) }

    var planToEdit by remember { mutableStateOf<PlanItem?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    val todayIso = remember { DateUtils.todayString() }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    planToEdit = null
                    showCreateDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_create_plan")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Goal / Plan")
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("planning_screen")
        ) {
            // Top App Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "🎯 LifeOS Planning",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Four-Horizon Execution Boards",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Period Navigator
            PlanningPeriodNavigator(
                activeTab = activeTab,
                selectedDateIso = selectedDateIso,
                todayIso = todayIso,
                onPrevious = {
                    selectedDateIso = when (activeTab) {
                        PlanningBoardTab.DAILY -> DateUtils.addDays(selectedDateIso, -1)
                        PlanningBoardTab.WEEKLY -> DateUtils.addDays(selectedDateIso, -7)
                        PlanningBoardTab.MONTHLY -> DateUtils.addMonths(selectedDateIso, -1)
                        PlanningBoardTab.YEARLY -> DateUtils.addYears(selectedDateIso, -1)
                    }
                },
                onNext = {
                    selectedDateIso = when (activeTab) {
                        PlanningBoardTab.DAILY -> DateUtils.addDays(selectedDateIso, 1)
                        PlanningBoardTab.WEEKLY -> DateUtils.addDays(selectedDateIso, 7)
                        PlanningBoardTab.MONTHLY -> DateUtils.addMonths(selectedDateIso, 1)
                        PlanningBoardTab.YEARLY -> DateUtils.addYears(selectedDateIso, 1)
                    }
                },
                onJumpToCurrent = {
                    selectedDateIso = todayIso
                }
            )

            // Board Tab Switcher
            TabRow(
                selectedTabIndex = activeTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth().testTag("planning_tabs")
            ) {
                PlanningBoardTab.entries.forEach { tab ->
                    Tab(
                        selected = activeTab == tab,
                        onClick = { activeTab = tab },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(tab.emoji)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(tab.title, fontWeight = if (activeTab == tab) FontWeight.Bold else FontWeight.Normal)
                            }
                        },
                        modifier = Modifier.testTag("planning_tab_${tab.name.lowercase()}")
                    )
                }
            }

            // Board Content
            Box(modifier = Modifier.weight(1f)) {
                when (activeTab) {
                    PlanningBoardTab.DAILY -> {
                        DailyPlanningBoard(
                            selectedDateIso = selectedDateIso,
                            todayIso = todayIso,
                            plans = plans,
                            allTasks = allTasks,
                            habits = habits,
                            onToggleTask = { taskId ->
                                viewModel.toggleTaskCompletion(taskId, selectedDateIso)
                            },
                            onToggleHabit = { habitId ->
                                viewModel.toggleHabit(habitId, selectedDateIso)
                            },
                            onTogglePlanStatus = { plan ->
                                val nextStatus = if (plan.status == PlanStatus.COMPLETED) PlanStatus.ACTIVE else PlanStatus.COMPLETED
                                viewModel.updatePlan(plan.copy(status = nextStatus))
                            },
                            onToggleMilestone = { milestoneId, isDone ->
                                viewModel.toggleMilestoneCompletion(milestoneId, isDone)
                            },
                            onEditPlan = { plan ->
                                planToEdit = plan
                                showCreateDialog = true
                            },
                            onDeletePlan = { planId ->
                                viewModel.deletePlan(planId)
                            },
                            onQuickCaptureTask = { title ->
                                val newTask = TaskItem(
                                    title = title,
                                    dueDateIso = selectedDateIso,
                                    priority = TaskPriority.HIGH,
                                    isAllDay = true
                                )
                                viewModel.saveTaskItem(newTask)
                            }
                        )
                    }

                    PlanningBoardTab.WEEKLY -> {
                        WeeklyPlanningBoard(
                            selectedDateIso = selectedDateIso,
                            todayIso = todayIso,
                            plans = plans,
                            allTasks = allTasks,
                            onSelectDay = { dateIso ->
                                selectedDateIso = dateIso
                                activeTab = PlanningBoardTab.DAILY
                            },
                            onToggleTask = { taskId, dateIso ->
                                viewModel.toggleTaskCompletion(taskId, dateIso)
                            },
                            onTogglePlanStatus = { plan ->
                                val nextStatus = if (plan.status == PlanStatus.COMPLETED) PlanStatus.ACTIVE else PlanStatus.COMPLETED
                                viewModel.updatePlan(plan.copy(status = nextStatus))
                            },
                            onToggleMilestone = { milestoneId, isDone ->
                                viewModel.toggleMilestoneCompletion(milestoneId, isDone)
                            },
                            onEditPlan = { plan ->
                                planToEdit = plan
                                showCreateDialog = true
                            },
                            onDeletePlan = { planId ->
                                viewModel.deletePlan(planId)
                            }
                        )
                    }

                    PlanningBoardTab.MONTHLY -> {
                        MonthlyPlanningBoard(
                            selectedDateIso = selectedDateIso,
                            todayIso = todayIso,
                            plans = plans,
                            allTasks = allTasks,
                            onToggleTask = { taskId, dateIso ->
                                viewModel.toggleTaskCompletion(taskId, dateIso)
                            },
                            onTogglePlanStatus = { plan ->
                                val nextStatus = if (plan.status == PlanStatus.COMPLETED) PlanStatus.ACTIVE else PlanStatus.COMPLETED
                                viewModel.updatePlan(plan.copy(status = nextStatus))
                            },
                            onToggleMilestone = { milestoneId, isDone ->
                                viewModel.toggleMilestoneCompletion(milestoneId, isDone)
                            },
                            onEditPlan = { plan ->
                                planToEdit = plan
                                showCreateDialog = true
                            },
                            onDeletePlan = { planId ->
                                viewModel.deletePlan(planId)
                            }
                        )
                    }

                    PlanningBoardTab.YEARLY -> {
                        YearlyPlanningBoard(
                            selectedDateIso = selectedDateIso,
                            plans = plans,
                            onTogglePlanStatus = { plan ->
                                val nextStatus = if (plan.status == PlanStatus.COMPLETED) PlanStatus.ACTIVE else PlanStatus.COMPLETED
                                viewModel.updatePlan(plan.copy(status = nextStatus))
                            },
                            onToggleMilestone = { milestoneId, isDone ->
                                viewModel.toggleMilestoneCompletion(milestoneId, isDone)
                            },
                            onEditPlan = { plan ->
                                planToEdit = plan
                                showCreateDialog = true
                            },
                            onDeletePlan = { planId ->
                                viewModel.deletePlan(planId)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateEditPlanDialog(
            initialPlan = planToEdit,
            defaultLevel = activeTab.level,
            defaultTargetDate = selectedDateIso,
            allTasks = allTasks,
            onDismiss = {
                showCreateDialog = false
                planToEdit = null
            },
            onSave = { title, desc, level, startIso, targetIso, milestones, linkedTaskIds ->
                if (planToEdit != null) {
                    val updated = planToEdit!!.copy(
                        title = title,
                        description = desc,
                        level = level,
                        startDateIso = startIso,
                        targetDateIso = targetIso
                    )
                    viewModel.updatePlan(
                        plan = updated,
                        milestoneTitles = milestones,
                        linkedTaskIds = linkedTaskIds
                    ) {
                        showCreateDialog = false
                        planToEdit = null
                    }
                } else {
                    viewModel.insertPlan(
                        plan = PlanItem(
                            title = title,
                            description = desc,
                            level = level,
                            startDateIso = startIso,
                            targetDateIso = targetIso,
                            status = PlanStatus.ACTIVE
                        ),
                        milestoneTitles = milestones,
                        linkedTaskIds = linkedTaskIds
                    ) {
                        showCreateDialog = false
                        planToEdit = null
                    }
                }
            }
        )
    }
}

/**
 * Period navigation header with prev/next period and jump-to-current button.
 */
@Composable
fun PlanningPeriodNavigator(
    activeTab: PlanningBoardTab,
    selectedDateIso: String,
    todayIso: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onJumpToCurrent: () -> Unit,
    modifier: Modifier = Modifier
) {
    val periodLabel = remember(activeTab, selectedDateIso) {
        when (activeTab) {
            PlanningBoardTab.DAILY -> {
                DateUtils.formatDisplayDate(selectedDateIso)
            }
            PlanningBoardTab.WEEKLY -> {
                val week = DateUtils.getWeekDays(selectedDateIso, "MONDAY")
                val start = if (week.isNotEmpty()) DateUtils.formatDisplayDate(week.first()) else selectedDateIso
                val end = if (week.isNotEmpty()) DateUtils.formatDisplayDate(week.last()) else selectedDateIso
                "$start – $end"
            }
            PlanningBoardTab.MONTHLY -> {
                DateUtils.formatMonthYear(selectedDateIso)
            }
            PlanningBoardTab.YEARLY -> {
                "Year ${DateUtils.getYear(selectedDateIso)}"
            }
        }
    }

    val isCurrent = remember(activeTab, selectedDateIso, todayIso) {
        when (activeTab) {
            PlanningBoardTab.DAILY -> selectedDateIso == todayIso
            PlanningBoardTab.WEEKLY -> {
                val week = DateUtils.getWeekDays(selectedDateIso, "MONDAY")
                week.contains(todayIso)
            }
            PlanningBoardTab.MONTHLY -> {
                selectedDateIso.take(7) == todayIso.take(7)
            }
            PlanningBoardTab.YEARLY -> {
                selectedDateIso.take(4) == todayIso.take(4)
            }
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrevious, modifier = Modifier.size(34.dp).testTag("nav_period_prev")) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Period")
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = periodLabel,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${activeTab.title} Planning Horizon",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onNext, modifier = Modifier.size(34.dp).testTag("nav_period_next")) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Period")
                }
            }

            if (!isCurrent) {
                Button(
                    onClick = onJumpToCurrent,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(30.dp).testTag("nav_jump_current")
                ) {
                    Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = when (activeTab) {
                            PlanningBoardTab.DAILY -> "Today"
                            PlanningBoardTab.WEEKLY -> "This Week"
                            PlanningBoardTab.MONTHLY -> "This Month"
                            PlanningBoardTab.YEARLY -> "This Year"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * DAILY PLANNING BOARD:
 * Today's priorities, tasks and habits, daily goals, completion progress, quick capture.
 */
@Composable
fun DailyPlanningBoard(
    selectedDateIso: String,
    todayIso: String,
    plans: List<PlanItem>,
    allTasks: List<TaskItem>,
    habits: List<HabitItem>,
    onToggleTask: (taskId: Long) -> Unit,
    onToggleHabit: (habitId: Long) -> Unit,
    onTogglePlanStatus: (PlanItem) -> Unit,
    onToggleMilestone: (milestoneId: Long, isDone: Boolean) -> Unit,
    onEditPlan: (PlanItem) -> Unit,
    onDeletePlan: (planId: Long) -> Unit,
    onQuickCaptureTask: (title: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var quickCaptureText by remember { mutableStateOf("") }

    val dailyPlans = remember(plans, selectedDateIso) {
        plans.filter {
            it.level == PlanLevel.DAILY &&
            (it.targetDateIso == selectedDateIso || it.startDateIso == selectedDateIso || (it.startDateIso <= selectedDateIso && it.targetDateIso >= selectedDateIso))
        }
    }

    val scheduledTasks = remember(allTasks, selectedDateIso) {
        allTasks.filter { task ->
            !task.isArchived && RecurrenceEngine.isTaskScheduledOnDate(task, selectedDateIso)
        }
    }

    val priorities = remember(scheduledTasks) {
        scheduledTasks.filter { it.priority == TaskPriority.URGENT || it.priority == TaskPriority.HIGH }
    }

    val totalGoals = dailyPlans.size
    val completedGoals = dailyPlans.count { it.status == PlanStatus.COMPLETED }

    val totalTasks = scheduledTasks.size
    val completedTasks = scheduledTasks.count { RecurrenceEngine.isCompletedOnDate(it, selectedDateIso) }

    val totalHabits = habits.size
    val completedHabits = habits.count { it.completedDates.contains(selectedDateIso) }

    val totalItems = totalGoals + totalTasks + totalHabits
    val completedItems = completedGoals + completedTasks + completedHabits
    val progressFraction = if (totalItems > 0) completedItems.toFloat() / totalItems.toFloat() else 0f

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("daily_planning_board"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Daily Progress Summary Card
        item {
            PlanningProgressCard(
                title = "Daily Execution Progress",
                subtitle = "${DateUtils.formatDisplayDate(selectedDateIso)} • $completedItems of $totalItems completed",
                progressFraction = progressFraction,
                breakdown = listOf(
                    "Goals" to "$completedGoals/$totalGoals",
                    "Tasks" to "$completedTasks/$totalTasks",
                    "Habits" to "$completedHabits/$totalHabits"
                )
            )
        }

        // Quick Capture Input
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(8.dp)
                ) {
                    OutlinedTextField(
                        value = quickCaptureText,
                        onValueChange = { quickCaptureText = it },
                        placeholder = { Text("⚡ Quick capture priority or task...", fontSize = 13.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("daily_quick_capture_input")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (quickCaptureText.isNotBlank()) {
                                onQuickCaptureTask(quickCaptureText.trim())
                                quickCaptureText = ""
                            }
                        },
                        enabled = quickCaptureText.isNotBlank(),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("daily_quick_capture_btn")
                    ) {
                        Text("Add", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section: Today's Priorities (P1 & P2)
        if (priorities.isNotEmpty()) {
            item {
                SectionHeader(title = "🔥 Daily Priorities (${priorities.size})", badge = "Focus")
            }
            items(priorities, key = { "priority_${it.id}" }) { task ->
                val isCompleted = RecurrenceEngine.isCompletedOnDate(task, selectedDateIso)
                TaskRowCard(
                    task = task,
                    isCompleted = isCompleted,
                    onToggle = { onToggleTask(task.id) }
                )
            }
        }

        // Section: Daily Goals (Plans)
        item {
            SectionHeader(
                title = "🎯 Daily Objectives & Goals (${dailyPlans.size})",
                badge = if (dailyPlans.isEmpty()) "Empty" else "$completedGoals done"
            )
        }
        if (dailyPlans.isEmpty()) {
            item {
                EmptyStateCard(
                    emoji = "☀️",
                    title = "No Daily Goals Scheduled",
                    description = "Tap + to add a daily target or focal objective for today!"
                )
            }
        } else {
            items(dailyPlans, key = { "plan_${it.id}" }) { plan ->
                PlanCard(
                    plan = plan,
                    onToggleStatus = { onTogglePlanStatus(plan) },
                    onToggleMilestone = onToggleMilestone,
                    onEdit = { onEditPlan(plan) },
                    onDelete = { onDeletePlan(plan.id) }
                )
            }
        }

        // Section: Tasks & Habits
        item {
            SectionHeader(title = "✅ Tasks & Habits", badge = "$completedTasks/$totalTasks tasks • $completedHabits/$totalHabits habits")
        }

        if (scheduledTasks.isEmpty() && habits.isEmpty()) {
            item {
                EmptyStateCard(
                    emoji = "✨",
                    title = "No Tasks or Habits Today",
                    description = "Your day is clear! Capture a priority above or enjoy your downtime."
                )
            }
        } else {
            // Scheduled Tasks (non-priorities or all)
            val otherTasks = scheduledTasks.filter { it.priority != TaskPriority.URGENT && it.priority != TaskPriority.HIGH }
            if (otherTasks.isNotEmpty()) {
                items(otherTasks, key = { "task_${it.id}" }) { task ->
                    val isCompleted = RecurrenceEngine.isCompletedOnDate(task, selectedDateIso)
                    TaskRowCard(
                        task = task,
                        isCompleted = isCompleted,
                        onToggle = { onToggleTask(task.id) }
                    )
                }
            }

            // Habits
            if (habits.isNotEmpty()) {
                items(habits, key = { "habit_${it.id}" }) { habit ->
                    val isDone = habit.completedDates.contains(selectedDateIso)
                    HabitRowCard(
                        habit = habit,
                        isDone = isDone,
                        onToggle = { onToggleHabit(habit.id) }
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

/**
 * WEEKLY PLANNING BOARD:
 * Weekly priorities, 7-day overview, task distribution across the week, weekly goals & progress, overdue/unfinished items.
 */
@Composable
fun WeeklyPlanningBoard(
    selectedDateIso: String,
    todayIso: String,
    plans: List<PlanItem>,
    allTasks: List<TaskItem>,
    onSelectDay: (String) -> Unit,
    onToggleTask: (taskId: Long, dateIso: String) -> Unit,
    onTogglePlanStatus: (PlanItem) -> Unit,
    onToggleMilestone: (milestoneId: Long, isDone: Boolean) -> Unit,
    onEditPlan: (PlanItem) -> Unit,
    onDeletePlan: (planId: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val weekDays = remember(selectedDateIso) {
        DateUtils.getWeekDays(selectedDateIso, "MONDAY")
    }

    val weeklyPlans = remember(plans, weekDays) {
        val weekStart = weekDays.firstOrNull() ?: selectedDateIso
        val weekEnd = weekDays.lastOrNull() ?: selectedDateIso
        plans.filter {
            it.level == PlanLevel.WEEKLY &&
            ((it.startDateIso in weekStart..weekEnd) || (it.targetDateIso in weekStart..weekEnd) || (it.startDateIso <= weekStart && it.targetDateIso >= weekEnd))
        }
    }

    // Weekly Priorities: P1/P2 across the 7 days
    val weeklyPriorities = remember(allTasks, weekDays) {
        val weekSet = weekDays.toSet()
        allTasks.filter { task ->
            !task.isArchived &&
            (task.priority == TaskPriority.URGENT || task.priority == TaskPriority.HIGH) &&
            weekSet.any { dateIso -> RecurrenceEngine.isTaskScheduledOnDate(task, dateIso) }
        }
    }

    // Overdue incomplete tasks
    val overdueTasks = remember(allTasks, todayIso) {
        allTasks.filter {
            !it.isArchived &&
            it.recurrenceType == RecurrenceType.NONE &&
            it.dueDateIso < todayIso &&
            !it.isOneTimeCompleted
        }
    }

    val totalGoals = weeklyPlans.size
    val completedGoals = weeklyPlans.count { it.status == PlanStatus.COMPLETED }
    val progressFraction = if (totalGoals > 0) completedGoals.toFloat() / totalGoals.toFloat() else 0f

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("weekly_planning_board"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Weekly Progress Overview
        item {
            PlanningProgressCard(
                title = "Weekly Review & Objectives",
                subtitle = "Week of ${DateUtils.formatDisplayDate(weekDays.firstOrNull() ?: selectedDateIso)} • $completedGoals of $totalGoals goals completed",
                progressFraction = progressFraction,
                breakdown = listOf(
                    "Weekly Goals" to "$completedGoals/$totalGoals",
                    "Priorities" to "${weeklyPriorities.size} active",
                    "Overdue" to "${overdueTasks.size} items"
                )
            )
        }

        // Seven-Day Overview & Task Distribution
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "📅 7-Day Overview & Task Distribution",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        weekDays.forEach { dayIso ->
                            val scheduledCount = allTasks.count { !it.isArchived && RecurrenceEngine.isTaskScheduledOnDate(it, dayIso) }
                            val completedCount = allTasks.count { !it.isArchived && RecurrenceEngine.isTaskScheduledOnDate(it, dayIso) && RecurrenceEngine.isCompletedOnDate(it, dayIso) }
                            val dayName = DateUtils.getDayOfWeek(dayIso).take(3)
                            val dayNum = dayIso.takeLast(2)
                            val isToday = dayIso == todayIso

                            Surface(
                                onClick = { onSelectDay(dayIso) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isToday) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = if (isToday) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 2.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    Text(dayName, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(dayNum.toIntOrNull()?.toString() ?: dayNum, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Surface(
                                        shape = CircleShape,
                                        color = if (completedCount > 0 && completedCount == scheduledCount) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        modifier = Modifier.size(16.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(scheduledCount.toString(), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Overdue & Unfinished Items (Attention required)
        if (overdueTasks.isNotEmpty()) {
            item {
                SectionHeader(title = "⚠️ Overdue & Unfinished Items (${overdueTasks.size})", badge = "Action Needed")
            }
            items(overdueTasks, key = { "overdue_${it.id}" }) { task ->
                val daysOverdue = DateUtils.daysBetween(task.dueDateIso, todayIso)
                TaskRowCard(
                    task = task,
                    isCompleted = false,
                    onToggle = { onToggleTask(task.id, task.dueDateIso) },
                    overdueNotice = "$daysOverdue days overdue"
                )
            }
        }

        // Weekly Priorities
        if (weeklyPriorities.isNotEmpty()) {
            item {
                SectionHeader(title = "🔥 Weekly Priorities (${weeklyPriorities.size})", badge = "High Impact")
            }
            items(weeklyPriorities, key = { "wp_${it.id}" }) { task ->
                val isCompleted = RecurrenceEngine.isCompletedOnDate(task, selectedDateIso)
                TaskRowCard(
                    task = task,
                    isCompleted = isCompleted,
                    onToggle = { onToggleTask(task.id, selectedDateIso) }
                )
            }
        }

        // Weekly Goals
        item {
            SectionHeader(
                title = "🎯 Weekly Goals & Key Results (${weeklyPlans.size})",
                badge = if (weeklyPlans.isEmpty()) "Empty" else "$completedGoals completed"
            )
        }

        if (weeklyPlans.isEmpty()) {
            item {
                EmptyStateCard(
                    emoji = "📅",
                    title = "No Goals for this Week",
                    description = "Tap + to define your weekly commitments, sprint milestones, or review targets!"
                )
            }
        } else {
            items(weeklyPlans, key = { "plan_${it.id}" }) { plan ->
                PlanCard(
                    plan = plan,
                    onToggleStatus = { onTogglePlanStatus(plan) },
                    onToggleMilestone = onToggleMilestone,
                    onEdit = { onEditPlan(plan) },
                    onDelete = { onDeletePlan(plan.id) }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

/**
 * MONTHLY PLANNING BOARD:
 * Monthly goals, important dates and deadlines, recurring obligations (including MONTHLY_WINDOW), monthly progress.
 */
@Composable
fun MonthlyPlanningBoard(
    selectedDateIso: String,
    todayIso: String,
    plans: List<PlanItem>,
    allTasks: List<TaskItem>,
    onToggleTask: (taskId: Long, dateIso: String) -> Unit,
    onTogglePlanStatus: (PlanItem) -> Unit,
    onToggleMilestone: (milestoneId: Long, isDone: Boolean) -> Unit,
    onEditPlan: (PlanItem) -> Unit,
    onDeletePlan: (planId: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentMonthPrefix = remember(selectedDateIso) {
        if (selectedDateIso.length >= 7) selectedDateIso.substring(0, 7) else selectedDateIso
    }

    val monthlyPlans = remember(plans, currentMonthPrefix) {
        plans.filter {
            it.level == PlanLevel.MONTHLY &&
            (it.targetDateIso.startsWith(currentMonthPrefix) || it.startDateIso.startsWith(currentMonthPrefix))
        }
    }

    // Important dates & deadlines in this month
    val deadlineTasks = remember(allTasks, currentMonthPrefix) {
        allTasks.filter {
            !it.isArchived &&
            ((it.deadlineIso != null && it.deadlineIso.startsWith(currentMonthPrefix)) ||
             (it.deadlineIso == null && it.dueDateIso.startsWith(currentMonthPrefix)))
        }.sortedBy { it.deadlineIso ?: it.dueDateIso }
    }

    // Recurring Obligations (including flexible monthly windows)
    val recurringObligations = remember(allTasks, currentMonthPrefix) {
        allTasks.filter { task ->
            !task.isArchived && task.recurrenceType != RecurrenceType.NONE
        }
    }

    val totalGoals = monthlyPlans.size
    val completedGoals = monthlyPlans.count { it.status == PlanStatus.COMPLETED }
    val progressFraction = if (totalGoals > 0) completedGoals.toFloat() / totalGoals.toFloat() else 0f

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("monthly_planning_board"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Monthly Progress Card
        item {
            PlanningProgressCard(
                title = "Monthly Strategy & Milestones",
                subtitle = "${DateUtils.formatMonthYear(selectedDateIso)} • $completedGoals of $totalGoals objectives met",
                progressFraction = progressFraction,
                breakdown = listOf(
                    "Monthly Goals" to "$completedGoals/$totalGoals",
                    "Deadlines" to "${deadlineTasks.size} dates",
                    "Recurring" to "${recurringObligations.size} rules"
                )
            )
        }

        // Monthly Goals
        item {
            SectionHeader(
                title = "🎯 Monthly Objectives & Goals (${monthlyPlans.size})",
                badge = if (monthlyPlans.isEmpty()) "Empty" else "$completedGoals done"
            )
        }
        if (monthlyPlans.isEmpty()) {
            item {
                EmptyStateCard(
                    emoji = "🗓️",
                    title = "No Monthly Goals for ${DateUtils.formatMonthYear(selectedDateIso)}",
                    description = "Set overarching goals for this month with key measurable milestones!"
                )
            }
        } else {
            items(monthlyPlans, key = { "mplan_${it.id}" }) { plan ->
                PlanCard(
                    plan = plan,
                    onToggleStatus = { onTogglePlanStatus(plan) },
                    onToggleMilestone = onToggleMilestone,
                    onEdit = { onEditPlan(plan) },
                    onDelete = { onDeletePlan(plan.id) }
                )
            }
        }

        // Recurring Obligations & Flexible Monthly Windows
        item {
            SectionHeader(
                title = "🔄 Recurring Obligations & Windows (${recurringObligations.size})",
                badge = "Monthly Cycles"
            )
        }
        if (recurringObligations.isEmpty()) {
            item {
                EmptyStateCard(
                    emoji = "🔄",
                    title = "No Recurring Obligations",
                    description = "Add recurring routines, rent, subscriptions, or flexible monthly window tasks."
                )
            }
        } else {
            items(recurringObligations, key = { "rec_${it.id}" }) { task ->
                val isWindow = task.recurrenceType == RecurrenceType.MONTHLY_WINDOW
                val isSatisfiedThisMonth = task.completedDates.any { it.startsWith(currentMonthPrefix) }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isWindow)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        else
                            MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Checkbox(
                            checked = isSatisfiedThisMonth,
                            onCheckedChange = {
                                onToggleTask(task.id, "$currentMonthPrefix-01")
                            },
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (task.emoji.isNotBlank()) "${task.emoji} ${task.title}" else task.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = if (isSatisfiedThisMonth) TextDecoration.LineThrough else null
                                )
                                if (isWindow) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "WINDOW",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = RecurrenceEngine.formatRecurrencePreview(task),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSatisfiedThisMonth) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = if (isSatisfiedThisMonth) "Satisfied" else "Pending",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSatisfiedThisMonth) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }

        // Important Dates & Deadlines
        item {
            SectionHeader(
                title = "📌 Important Dates & Deadlines (${deadlineTasks.size})",
                badge = "Milestones"
            )
        }
        if (deadlineTasks.isEmpty()) {
            item {
                EmptyStateCard(
                    emoji = "📌",
                    title = "No Major Deadlines This Month",
                    description = "Tasks with hard deadlines or scheduled due dates in this month will appear here."
                )
            }
        } else {
            items(deadlineTasks, key = { "deadline_${it.id}" }) { task ->
                val isCompleted = task.isOneTimeCompleted || task.completedDates.any { it.startsWith(currentMonthPrefix) }
                val targetDate = task.deadlineIso ?: task.dueDateIso
                TaskRowCard(
                    task = task,
                    isCompleted = isCompleted,
                    onToggle = { onToggleTask(task.id, targetDate) },
                    overdueNotice = if (task.deadlineIso != null) "Deadline: ${DateUtils.formatDisplayDate(task.deadlineIso)}" else null
                )
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

/**
 * YEARLY PLANNING BOARD:
 * Annual goals, milestones, quarterly overview, yearly progress.
 */
@Composable
fun YearlyPlanningBoard(
    selectedDateIso: String,
    plans: List<PlanItem>,
    onTogglePlanStatus: (PlanItem) -> Unit,
    onToggleMilestone: (milestoneId: Long, isDone: Boolean) -> Unit,
    onEditPlan: (PlanItem) -> Unit,
    onDeletePlan: (planId: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentYear = remember(selectedDateIso) { DateUtils.getYear(selectedDateIso) }

    val yearlyPlans = remember(plans, currentYear) {
        plans.filter {
            it.level == PlanLevel.YEARLY &&
            (DateUtils.getYear(it.targetDateIso) == currentYear || DateUtils.getYear(it.startDateIso) == currentYear)
        }
    }

    // All milestones across yearly goals
    val allYearlyMilestones = remember(yearlyPlans) {
        yearlyPlans.flatMap { it.milestones }
    }

    val totalGoals = yearlyPlans.size
    val completedGoals = yearlyPlans.count { it.status == PlanStatus.COMPLETED }
    val progressFraction = if (totalGoals > 0) completedGoals.toFloat() / totalGoals.toFloat() else 0f

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("yearly_planning_board"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Yearly Progress Metric
        item {
            PlanningProgressCard(
                title = "Year $currentYear Vision & Grand Outcomes",
                subtitle = "$completedGoals of $totalGoals annual goals accomplished",
                progressFraction = progressFraction,
                breakdown = listOf(
                    "Annual Goals" to "$completedGoals/$totalGoals",
                    "Milestones" to "${allYearlyMilestones.count { it.isCompleted }}/${allYearlyMilestones.size}",
                    "Completion" to "${(progressFraction * 100).toInt()}%"
                )
            )
        }

        // Quarterly Overview (Q1 - Q4)
        item {
            SectionHeader(title = "🧭 Quarterly Overview (Q1 – Q4)", badge = "Year $currentYear Breakdown")
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    QuarterInfo("Q1 (Jan – Mar)", "$currentYear-01-01", "$currentYear-03-31", "🌱 Foundation & Launch"),
                    QuarterInfo("Q2 (Apr – Jun)", "$currentYear-04-01", "$currentYear-06-30", "🚀 Build & Accelerate"),
                    QuarterInfo("Q3 (Jul – Sep)", "$currentYear-07-01", "$currentYear-09-30", "⚡ Scale & Optimize"),
                    QuarterInfo("Q4 (Oct – Dec)", "$currentYear-10-01", "$currentYear-12-31", "🏆 Harvest & Reflect")
                ).forEach { q ->
                    val qPlans = yearlyPlans.filter {
                        (it.targetDateIso in q.startDate..q.endDate) || (it.startDateIso in q.startDate..q.endDate)
                    }
                    QuarterCard(quarter = q, plans = qPlans)
                }
            }
        }

        // Annual Goals
        item {
            SectionHeader(
                title = "🌟 Annual Goals & Strategic Vision (${yearlyPlans.size})",
                badge = if (yearlyPlans.isEmpty()) "Empty" else "$completedGoals completed"
            )
        }

        if (yearlyPlans.isEmpty()) {
            item {
                EmptyStateCard(
                    emoji = "🌟",
                    title = "No Annual Goals for $currentYear",
                    description = "Define high-level objectives, life pillars, or North Star visions for this year!"
                )
            }
        } else {
            items(yearlyPlans, key = { "yplan_${it.id}" }) { plan ->
                PlanCard(
                    plan = plan,
                    onToggleStatus = { onTogglePlanStatus(plan) },
                    onToggleMilestone = onToggleMilestone,
                    onEdit = { onEditPlan(plan) },
                    onDelete = { onDeletePlan(plan.id) }
                )
            }
        }

        // Aggregated Milestones Checklist
        if (allYearlyMilestones.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "🎯 Key Annual Milestones (${allYearlyMilestones.size})",
                    badge = "${allYearlyMilestones.count { it.isCompleted }} done"
                )
            }
            items(allYearlyMilestones, key = { "yms_${it.id}" }) { ms ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Checkbox(
                            checked = ms.isCompleted,
                            onCheckedChange = { onToggleMilestone(ms.id, it) },
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = ms.title,
                            style = MaterialTheme.typography.bodyMedium,
                            textDecoration = if (ms.isCompleted) TextDecoration.LineThrough else null,
                            color = if (ms.isCompleted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

data class QuarterInfo(
    val title: String,
    val startDate: String,
    val endDate: String,
    val focusTheme: String
)

@Composable
fun QuarterCard(
    quarter: QuarterInfo,
    plans: List<PlanItem>,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = quarter.title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${plans.size} Goals",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = quarter.focusTheme,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (plans.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                plans.forEach { p ->
                    Text(
                        text = "• ${p.title} (${((if (p.status == PlanStatus.COMPLETED) 1f else p.progressFraction) * 100).toInt()}%)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Common progress summary card with progress bar and 3-way breakdown chips.
 */
@Composable
fun PlanningProgressCard(
    title: String,
    subtitle: String,
    progressFraction: Float,
    breakdown: List<Pair<String, String>>,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    text = "${(progressFraction * 100).toInt()}%",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { progressFraction.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                breakdown.forEach { (label, value) ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        modifier = Modifier.weight(1f).padding(horizontal = 3.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 6.dp)
                        ) {
                            Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    badge: String? = null,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 2.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        if (badge != null) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = badge,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

/**
 * Task Row Card with completion toggle, priority color badge, and overdue warning.
 */
@Composable
fun TaskRowCard(
    task: TaskItem,
    isCompleted: Boolean,
    onToggle: () -> Unit,
    overdueNotice: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Checkbox(
                checked = isCompleted,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.width(6.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (task.emoji.isNotBlank()) "${task.emoji} ${task.title}" else task.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else null,
                        color = if (isCompleted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = task.priority.composeColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = task.priority.code,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = task.priority.composeColor,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                if (overdueNotice != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "⚠️ $overdueNotice",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (!task.dueTime.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "🕒 ${task.dueTime}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

/**
 * Habit Row Card with streak indicator and toggle.
 */
@Composable
fun HabitRowCard(
    habit: HabitItem,
    isDone: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDone)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Checkbox(
                checked = isDone,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${habit.emoji} ${habit.title}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (isDone) TextDecoration.LineThrough else null
                )
                Text(
                    text = "🔥 ${habit.currentStreak} day streak",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Reusable Plan Card with milestone checklist, edit and delete actions.
 */
@Composable
fun PlanCard(
    plan: PlanItem,
    onToggleStatus: () -> Unit,
    onToggleMilestone: (milestoneId: Long, isDone: Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMilestones by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (plan.status == PlanStatus.COMPLETED)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth().testTag("plan_card_${plan.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${plan.level.icon} ${plan.level.displayName}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Target: ${DateUtils.formatDisplayDate(plan.targetDateIso)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleStatus, modifier = Modifier.size(28.dp).testTag("plan_toggle_status_${plan.id}")) {
                        Icon(
                            imageVector = if (plan.status == PlanStatus.COMPLETED) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = "Toggle Complete",
                            tint = if (plan.status == PlanStatus.COMPLETED) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp).testTag("plan_edit_${plan.id}")) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Plan",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp).testTag("plan_delete_${plan.id}")) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = plan.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (plan.status == PlanStatus.COMPLETED) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
            )

            if (plan.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = plan.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Bar
            val effectiveProgress = if (plan.status == PlanStatus.COMPLETED) 1f else plan.progressFraction
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { effectiveProgress },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${(effectiveProgress * 100).toInt()}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Milestones Expandable List
            if (plan.milestones.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showMilestones = !showMilestones }
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "Milestones (${plan.milestones.count { it.isCompleted }}/${plan.milestones.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (showMilestones) "▲ Hide" else "▼ Show",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                AnimatedVisibility(visible = showMilestones) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        plan.milestones.forEach { ms ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Checkbox(
                                    checked = ms.isCompleted,
                                    onCheckedChange = { onToggleMilestone(ms.id, it) },
                                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = ms.title,
                                    fontSize = 11.sp,
                                    textDecoration = if (ms.isCompleted) TextDecoration.LineThrough else null,
                                    color = if (ms.isCompleted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Create or Edit Plan Dialog with Milestones and Task Linking.
 */
@Composable
fun CreateEditPlanDialog(
    initialPlan: PlanItem? = null,
    defaultLevel: PlanLevel = PlanLevel.WEEKLY,
    defaultTargetDate: String = DateUtils.todayString(),
    allTasks: List<TaskItem> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        desc: String,
        level: PlanLevel,
        startIso: String,
        targetIso: String,
        milestones: List<String>,
        linkedTaskIds: List<Long>
    ) -> Unit
) {
    var title by remember { mutableStateOf(initialPlan?.title ?: "") }
    var description by remember { mutableStateOf(initialPlan?.description ?: "") }
    var level by remember { mutableStateOf(initialPlan?.level ?: defaultLevel) }
    var targetDateIso by remember {
        mutableStateOf(
            initialPlan?.targetDateIso ?: when (defaultLevel) {
                PlanLevel.DAILY -> defaultTargetDate
                PlanLevel.WEEKLY -> DateUtils.addDays(defaultTargetDate, 7)
                PlanLevel.MONTHLY -> DateUtils.addMonths(defaultTargetDate, 1)
                PlanLevel.YEARLY -> DateUtils.addYears(defaultTargetDate, 1)
                PlanLevel.QUARTERLY -> DateUtils.addDays(defaultTargetDate, 90)
            }
        )
    }

    var milestoneInput by remember { mutableStateOf("") }
    val milestones = remember {
        mutableStateOf(initialPlan?.milestones?.map { it.title } ?: emptyList())
    }

    var selectedTaskIds by remember {
        mutableStateOf((initialPlan?.linkedTaskIds ?: emptyList()).toSet())
    }

    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initialPlan != null) "Edit Strategic Plan" else "Create Strategic Plan",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Planning Horizon", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PlanLevel.entries.forEach { lvl ->
                        FilterChip(
                            selected = level == lvl,
                            onClick = {
                                level = lvl
                                if (initialPlan == null) {
                                    targetDateIso = when (lvl) {
                                        PlanLevel.DAILY -> defaultTargetDate
                                        PlanLevel.WEEKLY -> DateUtils.addDays(defaultTargetDate, 7)
                                        PlanLevel.MONTHLY -> DateUtils.addMonths(defaultTargetDate, 1)
                                        PlanLevel.YEARLY -> DateUtils.addYears(defaultTargetDate, 1)
                                        PlanLevel.QUARTERLY -> DateUtils.addDays(defaultTargetDate, 90)
                                    }
                                }
                            },
                            label = { Text("${lvl.icon} ${lvl.displayName}", fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (it.isNotBlank()) isError = false
                    },
                    label = { Text("Objective / Goal Title *") },
                    isError = isError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("plan_dialog_title")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Why this matters / Desired outcome") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = targetDateIso,
                    onValueChange = { targetDateIso = it },
                    label = { Text("Target Completion Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Milestones Builder
                Text("Milestones / Key Results", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = milestoneInput,
                        onValueChange = { milestoneInput = it },
                        placeholder = { Text("Add key milestone...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = {
                            if (milestoneInput.isNotBlank()) {
                                milestones.value = milestones.value + milestoneInput.trim()
                                milestoneInput = ""
                            }
                        }
                    ) {
                        Text("Add")
                    }
                }

                milestones.value.forEachIndexed { idx, ms ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Text("• $ms", fontSize = 12.sp, modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = {
                                milestones.value = milestones.value.filterIndexed { i, _ -> i != idx }
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // Task Linking
                if (allTasks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Link Tasks from Workspace", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    allTasks.take(15).forEach { task ->
                        val isLinked = selectedTaskIds.contains(task.id)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedTaskIds = if (isLinked) selectedTaskIds - task.id else selectedTaskIds + task.id
                                }
                                .padding(vertical = 2.dp)
                        ) {
                            Checkbox(
                                checked = isLinked,
                                onCheckedChange = {
                                    selectedTaskIds = if (it) selectedTaskIds + task.id else selectedTaskIds - task.id
                                }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(task.title, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        isError = true
                        return@Button
                    }
                    onSave(
                        title.trim(),
                        description.trim(),
                        level,
                        initialPlan?.startDateIso ?: DateUtils.todayString(),
                        targetDateIso.trim(),
                        milestones.value,
                        selectedTaskIds.toList()
                    )
                },
                modifier = Modifier.testTag("plan_dialog_save_btn")
            ) {
                Text(if (initialPlan != null) "Save Changes" else "Create Plan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
