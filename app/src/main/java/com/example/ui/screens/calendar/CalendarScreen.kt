package com.example.ui.screens.calendar

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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.SchedulingMode
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskPriority
import com.example.domain.schedule.ScheduleConflict
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.RescheduleDialog
import com.example.ui.components.TaskAdvancedEditorSheet
import com.example.ui.components.TaskCard
import com.example.ui.components.TaskHistoryDialog
import com.example.ui.viewmodel.CalendarUiState
import com.example.ui.viewmodel.LifeViewModel
import com.example.utils.DateUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: LifeViewModel,
    uiState: CalendarUiState,
    modifier: Modifier = Modifier
) {
    var showTaskModal by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TaskItem?>(null) }
    var taskToReschedule by remember { mutableStateOf<TaskItem?>(null) }
    var historyTask by remember { mutableStateOf<TaskItem?>(null) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var isMonthExpanded by remember { mutableStateOf(true) }

    val todayStr = DateUtils.todayString()
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())

    val tasksUiState by viewModel.tasksUiState.collectAsStateWithLifecycle()
    val projects = tasksUiState.projects
    val labels = tasksUiState.labels

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingTask = null
                    showTaskModal = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_calendar_add_task")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Task to Date")
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("calendar_screen")
        ) {
            // Header: Title & Fast Jump Navigation
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.clickable { showDatePickerDialog = true }) {
                    Text(
                        text = when (uiState.viewMode) {
                            "DAY" -> DateUtils.formatDisplayDate(uiState.selectedDateIso)
                            "THREE_DAY" -> "3-Day Plan (${uiState.threeDayDates.firstOrNull() ?: ""} – ${uiState.threeDayDates.lastOrNull() ?: ""})"
                            "AGENDA" -> "Agenda View"
                            else -> uiState.monthYearTitle
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (uiState.selectedDateIso == todayStr) "Today • Tap date to jump" else "Selected: ${uiState.selectedDateIso} • Tap to jump",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.selectCalendarDate(todayStr) }) {
                        Icon(imageVector = Icons.Default.Today, contentDescription = "Jump to Today")
                    }
                    IconButton(
                        onClick = {
                            when (uiState.viewMode) {
                                "DAY" -> viewModel.previousCalendarDay()
                                "THREE_DAY" -> viewModel.selectCalendarDate(DateUtils.addDays(uiState.selectedDateIso, -3))
                                "WEEK" -> viewModel.previousCalendarWeek()
                                else -> viewModel.previousCalendarMonth()
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Previous")
                    }
                    IconButton(
                        onClick = {
                            when (uiState.viewMode) {
                                "DAY" -> viewModel.nextCalendarDay()
                                "THREE_DAY" -> viewModel.selectCalendarDate(DateUtils.addDays(uiState.selectedDateIso, 3))
                                "WEEK" -> viewModel.nextCalendarWeek()
                                else -> viewModel.nextCalendarMonth()
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Next")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // View Mode Chips: Day, 3-Day, Week, Month, Agenda
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Pair("DAY", "Day (Hourly)"),
                    Pair("THREE_DAY", "3-Day"),
                    Pair("WEEK", "Weekly"),
                    Pair("MONTH", "Monthly"),
                    Pair("AGENDA", "Agenda")
                ).forEach { (mode, label) ->
                    FilterChip(
                        selected = uiState.viewMode == mode,
                        onClick = { viewModel.setCalendarViewMode(mode) },
                        label = { Text(label, fontSize = 12.sp) },
                        modifier = Modifier.testTag("calendar_tab_$mode")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Conflict Warning Banner (Compact & Expandable)
            if (uiState.conflictsOnSelectedDate.isNotEmpty()) {
                var isAlertsExpanded by remember { mutableStateOf(false) }
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clickable { isAlertsExpanded = !isAlertsExpanded }
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.WarningAmber,
                                    contentDescription = "Conflict",
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Schedule Alert (${uiState.conflictsOnSelectedDate.size})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E)
                                )
                            }
                            Text(
                                text = if (isAlertsExpanded) "Collapse ▲" else "View ▼",
                                fontSize = 11.sp,
                                color = Color(0xFF92400E),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (isAlertsExpanded) {
                            Spacer(modifier = Modifier.height(6.dp))
                            uiState.conflictsOnSelectedDate.forEach { conflict ->
                                Text(
                                    text = "• ${conflict.message}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF78350F)
                                )
                                if (conflict.suggestedTime != null) {
                                    Text(
                                        text = "   💡 Suggested slot: ${conflict.suggestedTime}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF1E3A8A)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // View Mode Content
            when (uiState.viewMode) {
                "DAY" -> {
                    DayTimeBlockingView(
                        selectedDateIso = uiState.selectedDateIso,
                        tasks = uiState.tasksOnSelectedDate,
                        onTaskClick = { task ->
                            editingTask = task
                            showTaskModal = true
                        },
                        onToggleTask = { viewModel.toggleTaskCompletion(it.id, uiState.selectedDateIso) },
                        onReschedule = { taskToReschedule = it },
                        onHistory = { historyTask = it }
                    )
                }
                "THREE_DAY" -> {
                    ThreeDayGridView(
                        dates = uiState.threeDayDates,
                        tasks = uiState.tasksOnSelectedDate,
                        onTaskClick = { task ->
                            editingTask = task
                            showTaskModal = true
                        },
                        onToggleTask = { viewModel.toggleTaskCompletion(it.id, uiState.selectedDateIso) }
                    )
                }
                "WEEK" -> {
                    WeekStripView(
                        weekDates = uiState.weekDates,
                        selectedDate = uiState.selectedDateIso,
                        today = todayStr,
                        onSelectDate = { viewModel.selectCalendarDate(it) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    TaskListSection(
                        tasks = uiState.tasksOnSelectedDate,
                        selectedDateIso = uiState.selectedDateIso,
                        onTaskClick = { task ->
                            editingTask = task
                            showTaskModal = true
                        },
                        onToggleTask = { viewModel.toggleTaskCompletion(it.id, uiState.selectedDateIso) },
                        onReschedule = { taskToReschedule = it },
                        onHistory = { historyTask = it },
                        modifier = Modifier.weight(1f)
                    )
                }
                "AGENDA" -> {
                    AgendaView(
                        tasks = tasksUiState.allTasks,
                        today = todayStr,
                        onTaskClick = { task ->
                            editingTask = task
                            showTaskModal = true
                        },
                        onToggleTask = { task, date ->
                            viewModel.toggleTaskCompletion(task.id, date)
                        }
                    )
                }
                else -> {
                    // "MONTH"
                    if (isMonthExpanded) {
                        MonthGridView(
                            monthDates = uiState.monthGridDates,
                            selectedDate = uiState.selectedDateIso,
                            today = todayStr,
                            monthIndex = uiState.currentMonthZeroIndexed,
                            onSelectDate = { viewModel.selectCalendarDate(it) }
                        )
                    } else {
                        WeekStripView(
                            weekDates = uiState.weekDates,
                            selectedDate = uiState.selectedDateIso,
                            today = todayStr,
                            onSelectDate = { viewModel.selectCalendarDate(it) }
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isMonthExpanded = !isMonthExpanded }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isMonthExpanded) "Collapse to Week ▲" else "Expand Month ▼",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    TaskListSection(
                        tasks = uiState.tasksOnSelectedDate,
                        selectedDateIso = uiState.selectedDateIso,
                        onTaskClick = { task ->
                            editingTask = task
                            showTaskModal = true
                        },
                        onToggleTask = { viewModel.toggleTaskCompletion(it.id, uiState.selectedDateIso) },
                        onReschedule = { taskToReschedule = it },
                        onHistory = { historyTask = it },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    // Date Picker Dialog
    if (showDatePickerDialog) {
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis
                        if (millis != null) {
                            val pickedIso = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(millis))
                            viewModel.selectCalendarDate(pickedIso)
                        }
                        showDatePickerDialog = false
                    }
                ) {
                    Text("Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Task Advanced Editor Sheet
    if (showTaskModal) {
        TaskAdvancedEditorSheet(
            task = editingTask,
            projects = projects,
            labels = labels,
            onCreateProject = { proj, cb ->
                viewModel.insertProject(proj) { newId -> cb(newId) }
            },
            onCreateLabel = { lbl, cb ->
                viewModel.insertLabel(lbl) { newId -> cb(newId) }
            },
            onSaveTask = { task ->
                viewModel.saveTaskItem(task)
                showTaskModal = false
                editingTask = null
            },
            onDismiss = {
                showTaskModal = false
                editingTask = null
            }
        )
    }

    if (taskToReschedule != null) {
        RescheduleDialog(
            task = taskToReschedule!!,
            initialDateIso = uiState.selectedDateIso,
            onDismiss = { taskToReschedule = null },
            onConfirm = { newDateIso ->
                viewModel.rescheduleOccurrence(taskToReschedule!!.id, uiState.selectedDateIso, newDateIso)
                taskToReschedule = null
            }
        )
    }

    if (historyTask != null) {
        TaskHistoryDialog(
            task = historyTask!!,
            onDismiss = { historyTask = null }
        )
    }
}

/**
 * Hourly Time Blocking View from 06:00 to 22:00.
 */
@Composable
fun DayTimeBlockingView(
    selectedDateIso: String,
    tasks: List<TaskItem>,
    onTaskClick: (TaskItem) -> Unit,
    onToggleTask: (TaskItem) -> Unit,
    onReschedule: (TaskItem) -> Unit,
    onHistory: (TaskItem) -> Unit
) {
    val scrollState = rememberScrollState()
    val allDayTasks = tasks.filter { it.isAllDay || it.dueTime == null }
    val timedTasks = tasks.filter { !it.isAllDay && it.dueTime != null }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // All-day tasks section
        if (allDayTasks.isNotEmpty()) {
            Text(
                text = "All-Day Tasks (${allDayTasks.size})",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            allDayTasks.forEach { task ->
                TaskCard(
                    task = task,
                    onToggleComplete = { onToggleTask(task) },
                    onToggleSubtask = {},
                    onEdit = { onTaskClick(task) },
                    onDelete = {},
                    onReschedule = { onReschedule(task) },
                    onViewHistory = { onHistory(task) }
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }

        Text(
            text = "Hourly Schedule",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        // Hours from 06:00 to 22:00
        for (hour in 6..22) {
            val hourStr = String.format(Locale.getDefault(), "%02d:00", hour)
            val matchingTasks = timedTasks.filter {
                val t = it.dueTime ?: ""
                t.startsWith(String.format(Locale.getDefault(), "%02d", hour))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Time label
                Text(
                    text = hourStr,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.width(44.dp)
                )

                // Timeline block
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (matchingTasks.isNotEmpty()) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    if (matchingTasks.isEmpty()) {
                        Text(
                            text = "Free time slot",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                        )
                    } else {
                        matchingTasks.forEach { task ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onTaskClick(task) }
                                    .padding(vertical = 2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Text(text = task.emoji.ifBlank { "📌" }, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = task.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Surface(
                                    color = task.priority.composeColor.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "${task.durationAmount} ${task.durationUnit.singularName}",
                                        fontSize = 9.sp,
                                        color = task.priority.composeColor,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(72.dp))
    }
}

/**
 * 3-Day Grid View.
 */
@Composable
fun ThreeDayGridView(
    dates: List<String>,
    tasks: List<TaskItem>,
    onTaskClick: (TaskItem) -> Unit,
    onToggleTask: (TaskItem) -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        dates.forEach { dateIso ->
            val tasksOnDay = tasks.filter { it.dueDateIso == dateIso }
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = DateUtils.formatDisplayDate(dateIso),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${tasksOnDay.size} tasks",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (tasksOnDay.isEmpty()) {
                        Text(
                            text = "No scheduled tasks for this date",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    } else {
                        tasksOnDay.forEach { task ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onTaskClick(task) }
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(text = task.emoji.ifBlank { "✅" }, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = task.title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                if (task.dueTime != null) {
                                    Text(
                                        text = task.dueTime,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(72.dp))
    }
}

/**
 * Week Strip View.
 */
@Composable
fun WeekStripView(
    weekDates: List<String>,
    selectedDate: String,
    today: String,
    onSelectDate: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 4.dp)
        ) {
            weekDates.forEach { dateIso ->
                val isSelected = dateIso == selectedDate
                val isToday = dateIso == today
                val dayNum = dateIso.takeLast(2)
                val dayLetter = try {
                    val d = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateIso)
                    SimpleDateFormat("EEEEE", Locale.getDefault()).format(d ?: Date())
                } catch (e: Exception) {
                    "D"
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(width = 42.dp, height = 54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            when {
                                isSelected -> MaterialTheme.colorScheme.primary
                                isToday -> MaterialTheme.colorScheme.primaryContainer
                                else -> Color.Transparent
                            }
                        )
                        .clickable { onSelectDate(dateIso) }
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = dayLetter,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = dayNum,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

/**
 * Month Grid View.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MonthGridView(
    monthDates: List<String>,
    selectedDate: String,
    today: String,
    monthIndex: Int,
    onSelectDate: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceAround,
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                    Text(
                        text = day,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                maxItemsInEachRow = 7,
                horizontalArrangement = Arrangement.SpaceAround,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                monthDates.forEach { dateIso ->
                    val isSelected = dateIso == selectedDate
                    val isToday = dateIso == today
                    val dayNum = dateIso.takeLast(2)
                    val isCurrentMonth = DateUtils.getMonthZeroIndexed(dateIso) == monthIndex

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    isToday -> MaterialTheme.colorScheme.primaryContainer
                                    else -> Color.Transparent
                                }
                            )
                            .clickable { onSelectDate(dateIso) }
                    ) {
                        Text(
                            text = dayNum.toIntOrNull()?.toString() ?: dayNum,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                            color = when {
                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                                !isCurrentMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Agenda View showing chronological upcoming tasks.
 */
@Composable
fun AgendaView(
    tasks: List<TaskItem>,
    today: String,
    onTaskClick: (TaskItem) -> Unit,
    onToggleTask: (TaskItem, String) -> Unit
) {
    val sortedTasks = remember(tasks) {
        tasks.sortedWith(compareBy({ it.dueDateIso }, { it.dueTime ?: "99:99" }))
    }

    if (sortedTasks.isEmpty()) {
        EmptyStateCard(
            emoji = "📅",
            title = "No Upcoming Tasks",
            description = "Your schedule is currently clear!"
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val grouped = sortedTasks.groupBy { it.dueDateIso }
            grouped.forEach { (dateIso, itemsOnDate) ->
                item {
                    val label = when (dateIso) {
                        today -> "Today (${DateUtils.formatDisplayDate(dateIso)})"
                        DateUtils.addDays(today, 1) -> "Tomorrow (${DateUtils.formatDisplayDate(dateIso)})"
                        else -> DateUtils.formatDisplayDate(dateIso)
                    }
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                items(itemsOnDate, key = { "agenda_${it.id}" }) { task ->
                    TaskCard(
                        task = task,
                        onToggleComplete = { onToggleTask(task, dateIso) },
                        onToggleSubtask = {},
                        onEdit = { onTaskClick(task) },
                        onDelete = {},
                        onReschedule = {},
                        onViewHistory = {}
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}

/**
 * Tasks list for the selected day in Month and Week views.
 */
@Composable
fun TaskListSection(
    tasks: List<TaskItem>,
    selectedDateIso: String,
    onTaskClick: (TaskItem) -> Unit,
    onToggleTask: (TaskItem) -> Unit,
    onReschedule: (TaskItem) -> Unit,
    onHistory: (TaskItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (tasks.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth().padding(top = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            EmptyStateCard(
                emoji = "✨",
                title = "Nothing Scheduled",
                description = "Enjoy your open time or tap + to schedule a task"
            )
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tasks, key = { it.id }) { task ->
                TaskCard(
                    task = task,
                    onToggleComplete = { onToggleTask(task) },
                    onToggleSubtask = {},
                    onEdit = { onTaskClick(task) },
                    onDelete = {},
                    onReschedule = { onReschedule(task) },
                    onViewHistory = { onHistory(task) }
                )
            }
            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}
