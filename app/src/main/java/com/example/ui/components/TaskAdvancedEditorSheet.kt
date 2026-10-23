package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ColorOrderingMode
import com.example.domain.model.DefaultTaskCategories
import com.example.domain.model.DurationUnit
import com.example.domain.model.LabelItem
import com.example.domain.model.ProjectItem
import com.example.domain.model.RecurrenceType
import com.example.domain.model.SchedulingMode
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskPriority
import com.example.domain.model.TaskReminderItem
import com.example.domain.model.TodoistColorPalette
import com.example.domain.recurrence.RecurrenceEngine
import com.example.domain.scheduling.SchedulingEngine
import com.example.ui.screens.labels.EditLabelBottomSheet
import com.example.ui.screens.projects.EditProjectBottomSheet
import com.example.utils.DateUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskAdvancedEditorSheet(
    task: TaskItem?,
    projects: List<ProjectItem>,
    labels: List<LabelItem>,
    colorOrderingMode: ColorOrderingMode = ColorOrderingMode.AUTO,
    colorUsageCounts: Map<String, Int> = emptyMap(),
    colorCustomOrder: List<String> = emptyList(),
    onCreateProject: ((ProjectItem, (Long) -> Unit) -> Unit)? = null,
    onCreateLabel: ((LabelItem, (Long) -> Unit) -> Unit)? = null,
    onColorUsed: (String) -> Unit = {},
    onModeChanged: (ColorOrderingMode) -> Unit = {},
    onSaveCustomOrder: (List<String>) -> Unit = {},
    onResetCustomOrder: () -> Unit = {},
    onSaveTask: (TaskItem) -> Unit,
    onSaveTaskWithMedication: ((TaskItem, com.example.domain.medication.MedicationItem?) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val today = DateUtils.todayString()

    // 1. Basic Info
    var title by remember { mutableStateOf(task?.title ?: "") }
    var emoji by remember { mutableStateOf(task?.emoji ?: "") }
    var description by remember { mutableStateOf(task?.description ?: "") }
    var category by remember { mutableStateOf(task?.category ?: "Habits & Daily Routine") }
    var priority by remember { mutableStateOf(task?.priority ?: TaskPriority.P3) }
    var selectedProjectId by remember { mutableStateOf<Long?>(task?.projectId) }
    var selectedLabelIds by remember { mutableStateOf<Set<Long>>(task?.labels?.map { it.id }?.toSet() ?: emptySet()) }
    var isQuickReminder by remember { mutableStateOf(task?.isQuickReminder ?: false) }
    var isMedicineEnabled by remember { mutableStateOf(false) }
    var medicationDraft by remember { mutableStateOf<com.example.domain.medication.MedicationItem?>(null) }

    // 2. Schedule Date & Time
    var dueDateIso by remember { mutableStateOf(task?.dueDateIso ?: today) }
    var draftDueTime by remember { mutableStateOf(task?.dueTime ?: "09:00") }
    var isAllDay by remember { mutableStateOf(task?.isAllDay ?: (task?.dueTime == null)) }

    // 3. Scheduling Mode: Duration vs Deadline (mutually exclusive)
    var schedulingMode by remember {
        mutableStateOf(task?.schedulingMode ?: if (task?.deadlineIso != null) SchedulingMode.DEADLINE else SchedulingMode.DURATION)
    }

    // Duration draft state
    var durationAmount by remember { mutableIntStateOf(task?.durationAmount ?: (task?.durationMinutes ?: 30)) }
    var durationUnit by remember { mutableStateOf(task?.durationUnit ?: DurationUnit.MINUTES) }

    // Deadline draft state
    var draftDeadlineIso by remember { mutableStateOf(task?.deadlineIso ?: DateUtils.addDays(today, 1)) }
    var draftDeadlineTime by remember { mutableStateOf(task?.deadlineTime) }

    // 4. Recurrence State
    var recurrenceType by remember { mutableStateOf(task?.recurrenceType ?: RecurrenceType.NONE) }
    var recurrenceInterval by remember { mutableIntStateOf(if (task?.recurrenceInterval != null && task.recurrenceInterval > 0) task.recurrenceInterval else 1) }
    var recurrenceUnit by remember { mutableStateOf(task?.recurrenceUnit ?: "DAYS") }
    var recurrenceDaysOfWeek by remember { mutableStateOf(task?.recurrenceDaysOfWeek ?: emptyList()) }
    var recurrenceEndCondition by remember { mutableStateOf(task?.recurrenceEndCondition ?: "NEVER") }
    var recurrenceEndDateIso by remember { mutableStateOf(task?.recurrenceEndDateIso) }
    var recurrenceCount by remember { mutableIntStateOf(if (task?.recurrenceCount != null && task.recurrenceCount > 0) task.recurrenceCount else 10) }

    // 5. Reminders List
    var remindersList by remember {
        mutableStateOf<List<TaskReminderItem>>(task?.reminders ?: emptyList())
    }

    // Dialog & Picker visibility states
    var showEmojiDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDeadlineDatePicker by remember { mutableStateOf(false) }
    var showDeadlineTimePicker by remember { mutableStateOf(false) }
    var showDurationUnitMenu by remember { mutableStateOf(false) }
    var showRecurrenceMenu by remember { mutableStateOf(false) }
    var showCustomRecurrenceDialog by remember { mutableStateOf(false) }
    var showAddReminderDialog by remember { mutableStateOf(false) }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var showProjectDropdown by remember { mutableStateOf(false) }
    var showLabelDropdown by remember { mutableStateOf(false) }
    var showCreateProjectSheet by remember { mutableStateOf(false) }
    var showCreateLabelSheet by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )
    val deadlineDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )

    val timePickerState = rememberTimePickerState(
        initialHour = draftDueTime.split(":").getOrNull(0)?.toIntOrNull() ?: 9,
        initialMinute = draftDueTime.split(":").getOrNull(1)?.toIntOrNull() ?: 0,
        is24Hour = false
    )

    val deadlineTimePickerState = rememberTimePickerState(
        initialHour = draftDeadlineTime?.split(":")?.getOrNull(0)?.toIntOrNull() ?: 17,
        initialMinute = draftDeadlineTime?.split(":")?.getOrNull(1)?.toIntOrNull() ?: 0,
        is24Hour = false
    )

    // Projected Duration calculation
    val projectedEnd by remember(dueDateIso, draftDueTime, isAllDay, durationAmount, durationUnit) {
        derivedStateOf {
            SchedulingEngine.calculateProjectedEnd(
                startDateIso = dueDateIso,
                startTime = if (isAllDay) null else draftDueTime,
                isAllDay = isAllDay,
                amount = durationAmount,
                unit = durationUnit
            )
        }
    }

    // Recurrence preview computation
    val dummyTaskForRecurrence by remember(
        dueDateIso,
        recurrenceType,
        recurrenceInterval,
        recurrenceUnit,
        recurrenceDaysOfWeek,
        recurrenceEndCondition,
        recurrenceEndDateIso,
        recurrenceCount
    ) {
        derivedStateOf {
            TaskItem(
                title = "Draft",
                dueDateIso = dueDateIso,
                recurrenceType = recurrenceType,
                recurrenceInterval = recurrenceInterval,
                recurrenceUnit = recurrenceUnit,
                recurrenceDaysOfWeek = recurrenceDaysOfWeek,
                recurrenceEndCondition = recurrenceEndCondition,
                recurrenceEndDateIso = recurrenceEndDateIso,
                recurrenceCount = recurrenceCount
            )
        }
    }

    val recurrencePreview by remember(dummyTaskForRecurrence) {
        derivedStateOf {
            RecurrenceEngine.formatRecurrencePreview(dummyTaskForRecurrence)
        }
    }

    val nextOccurrences by remember(dummyTaskForRecurrence) {
        derivedStateOf {
            RecurrenceEngine.getNextNOccurrences(dummyTaskForRecurrence, count = 3, fromDateIso = dueDateIso)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("task_advanced_editor_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 40.dp)
        ) {
            // Top App Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("adv_editor_close_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                    Text(
                        text = if (task == null || task.id == 0L) "New Task" else "Edit Task",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = {
                        val trimmed = title.trim()
                        if (trimmed.isNotBlank()) {
                            val selectedLabels = labels.filter { selectedLabelIds.contains(it.id) }
                            val elapsedMinutes = SchedulingEngine.toElapsedMinutes(dueDateIso, durationAmount, durationUnit)

                            val updatedTask = (task ?: TaskItem(
                                title = trimmed,
                                dueDateIso = dueDateIso
                            )).copy(
                                title = trimmed,
                                emoji = emoji.trim(),
                                description = description.trim(),
                                category = category,
                                priority = priority,
                                dueDateIso = dueDateIso,
                                dueTime = if (isAllDay) null else draftDueTime,
                                isAllDay = isAllDay,
                                schedulingMode = schedulingMode,
                                durationAmount = durationAmount,
                                durationUnit = durationUnit,
                                durationMinutes = elapsedMinutes,
                                deadlineIso = if (schedulingMode == SchedulingMode.DEADLINE) draftDeadlineIso else null,
                                deadlineTime = if (schedulingMode == SchedulingMode.DEADLINE) draftDeadlineTime else null,
                                recurrenceType = recurrenceType,
                                recurrenceInterval = recurrenceInterval,
                                recurrenceUnit = recurrenceUnit,
                                recurrenceDaysOfWeek = recurrenceDaysOfWeek,
                                recurrenceEndCondition = recurrenceEndCondition,
                                recurrenceEndDateIso = recurrenceEndDateIso,
                                recurrenceCount = recurrenceCount,
                                projectId = selectedProjectId,
                                labels = selectedLabels,
                                isReminderEnabled = remindersList.isNotEmpty(),
                                isQuickReminder = isQuickReminder,
                                reminders = remindersList
                            )
                            if (onSaveTaskWithMedication != null) {
                                onSaveTaskWithMedication(updatedTask, if (isMedicineEnabled) medicationDraft else null)
                            } else {
                                onSaveTask(updatedTask)
                            }
                            onDismiss()
                        }
                    },
                    enabled = title.isNotBlank(),
                    modifier = Modifier.testTag("adv_editor_save_btn")
                ) {
                    Text("Save")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Emoji & Title Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = { showEmojiDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    color = if (emoji.isNotBlank()) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        if (emoji.isNotBlank()) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .size(54.dp)
                        .testTag("adv_task_emoji_btn")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (emoji.isNotBlank()) {
                            Text(text = emoji, fontSize = 28.sp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.SentimentSatisfied,
                                contentDescription = "Add Emoji",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title *") },
                    placeholder = { Text("What needs to be done?") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("adv_task_title_input")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description & Notes") },
                placeholder = { Text("Add subtasks, links, or context...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("adv_task_desc_input"),
                minLines = 2,
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Priority Section
            Text("Priority", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TaskPriority.allPriorities.forEach { p ->
                    val isSel = priority == p
                    Surface(
                        onClick = { priority = p },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSel) p.composeColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = if (isSel) androidx.compose.foundation.BorderStroke(1.5.dp, p.composeColor) else null,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("adv_priority_${p.code}")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Flag, contentDescription = null, tint = p.composeColor, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(p.code, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = p.composeColor)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Project Selector with + Create New Project
            Box {
                val currentProject = projects.find { it.id == selectedProjectId }
                Surface(
                    onClick = { showProjectDropdown = true },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("adv_project_row")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Project", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = if (currentProject != null) "${currentProject.emoji} ${currentProject.name}" else "📁 Inbox (No Project)",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        TextButton(onClick = { showProjectDropdown = true }) {
                            Text("Change")
                        }
                    }
                }

                DropdownMenu(expanded = showProjectDropdown, onDismissRequest = { showProjectDropdown = false }) {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Create New Project", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        },
                        onClick = {
                            showProjectDropdown = false
                            showCreateProjectSheet = true
                        },
                        modifier = Modifier.testTag("adv_create_project_option")
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("📁 Inbox (No Project)") },
                        onClick = {
                            selectedProjectId = null
                            showProjectDropdown = false
                        }
                    )
                    projects.forEach { proj ->
                        DropdownMenuItem(
                            text = { Text("${proj.emoji} ${proj.name}") },
                            onClick = {
                                selectedProjectId = proj.id
                                showProjectDropdown = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Labels Multi-select Row with + Create New Label
            Box {
                Surface(
                    onClick = { showLabelDropdown = true },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("adv_labels_row")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Label, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Labels", fontWeight = FontWeight.SemiBold)
                            }
                            TextButton(onClick = { showLabelDropdown = true }) {
                                Text(if (selectedLabelIds.isEmpty()) "Add Labels" else "Edit")
                            }
                        }

                        if (selectedLabelIds.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                labels.filter { selectedLabelIds.contains(it.id) }.forEach { lbl ->
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = lbl.composeColor.copy(alpha = 0.15f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, lbl.composeColor.copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = "#${lbl.name}",
                                            color = lbl.composeColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                DropdownMenu(expanded = showLabelDropdown, onDismissRequest = { showLabelDropdown = false }) {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Create New Label", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        },
                        onClick = {
                            showLabelDropdown = false
                            showCreateLabelSheet = true
                        },
                        modifier = Modifier.testTag("adv_create_label_option")
                    )
                    HorizontalDivider()
                    if (labels.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No labels yet", color = MaterialTheme.colorScheme.outline) },
                            onClick = { showLabelDropdown = false }
                        )
                    } else {
                        labels.forEach { lbl ->
                            val isChecked = selectedLabelIds.contains(lbl.id)
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("#${lbl.name}", color = lbl.composeColor, fontWeight = FontWeight.Bold)
                                        if (isChecked) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                },
                                onClick = {
                                    selectedLabelIds = if (isChecked) selectedLabelIds - lbl.id else selectedLabelIds + lbl.id
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SECTION: Date, Time & All-Day
            Text("Date & Time Schedule", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Start Date
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Start Date", style = MaterialTheme.typography.labelSmall)
                                Text(DateUtils.formatDisplayDate(dueDateIso), fontWeight = FontWeight.SemiBold)
                            }
                        }
                        TextButton(onClick = { showDatePicker = true }) {
                            Text("Change")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // All-day switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("All-day task", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(
                                text = if (isAllDay) "No specific start time" else "Specific start time enabled",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isAllDay,
                            onCheckedChange = { isAllDay = it },
                            modifier = Modifier.testTag("adv_all_day_switch")
                        )
                    }

                    // Start Time row (visible when NOT all day)
                    if (!isAllDay) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Start Time", style = MaterialTheme.typography.labelSmall)
                                    Text(draftDueTime, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            TextButton(onClick = { showTimePicker = true }) {
                                Text("Pick Time")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SECTION: Scheduling Method (Mutually Exclusive: Duration vs Deadline)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Planning Method", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(2.dp)
                ) {
                    Surface(
                        onClick = { schedulingMode = SchedulingMode.DURATION },
                        shape = RoundedCornerShape(6.dp),
                        color = if (schedulingMode == SchedulingMode.DURATION) MaterialTheme.colorScheme.primary else Color.Transparent,
                        modifier = Modifier.testTag("sched_mode_duration_tab")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Timer,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (schedulingMode == SchedulingMode.DURATION) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Duration",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (schedulingMode == SchedulingMode.DURATION) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        onClick = { schedulingMode = SchedulingMode.DEADLINE },
                        shape = RoundedCornerShape(6.dp),
                        color = if (schedulingMode == SchedulingMode.DEADLINE) MaterialTheme.colorScheme.primary else Color.Transparent,
                        modifier = Modifier.testTag("sched_mode_deadline_tab")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.HourglassBottom,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (schedulingMode == SchedulingMode.DEADLINE) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Deadline",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (schedulingMode == SchedulingMode.DEADLINE) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    if (schedulingMode == SchedulingMode.DURATION) {
                        // MODE A: Custom Duration with custom units
                        Text("Expected Duration", style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = if (durationAmount > 0) durationAmount.toString() else "",
                                onValueChange = { str ->
                                    val n = str.filter { it.isDigit() }.toIntOrNull()
                                    if (n != null && n in 1..9999) {
                                        durationAmount = n
                                    } else if (str.isEmpty()) {
                                        durationAmount = 1
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .width(85.dp)
                                    .testTag("adv_duration_amount_input")
                            )

                            Box(modifier = Modifier.weight(1f)) {
                                Surface(
                                    onClick = { showDurationUnitMenu = true },
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (durationAmount == 1) durationUnit.singularName.replaceFirstChar { it.uppercase() } else durationUnit.displayName.replaceFirstChar { it.uppercase() },
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text("▼", fontSize = 10.sp)
                                    }
                                }

                                DropdownMenu(
                                    expanded = showDurationUnitMenu,
                                    onDismissRequest = { showDurationUnitMenu = false }
                                ) {
                                    DurationUnit.entries.forEach { u ->
                                        DropdownMenuItem(
                                            text = { Text(u.displayName.replaceFirstChar { it.uppercase() }) },
                                            onClick = {
                                                durationUnit = u
                                                showDurationUnitMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Preset shortcuts
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                Triple(15, DurationUnit.MINUTES, "15m"),
                                Triple(30, DurationUnit.MINUTES, "30m"),
                                Triple(1, DurationUnit.HOURS, "1h"),
                                Triple(2, DurationUnit.HOURS, "2h"),
                                Triple(1, DurationUnit.DAYS, "1d"),
                                Triple(1, DurationUnit.WEEKS, "1w"),
                                Triple(1, DurationUnit.MONTHS, "1mo"),
                                Triple(1, DurationUnit.QUARTERS, "1q"),
                                Triple(1, DurationUnit.YEARS, "1y")
                            ).forEach { (amt, u, label) ->
                                FilterChip(
                                    selected = durationAmount == amt && durationUnit == u,
                                    onClick = {
                                        durationAmount = amt
                                        durationUnit = u
                                    },
                                    label = { Text(label, fontSize = 11.sp) }
                                )
                            }
                        }

                        // Calendar-aware projected end display
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = projectedEnd.displaySummary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    } else {
                        // MODE B: Deadline Date & Optional Time
                        Text("Complete By (Deadline)", style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.HourglassBottom, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Deadline Date", style = MaterialTheme.typography.labelSmall)
                                    Text(DateUtils.formatDisplayDate(draftDeadlineIso), fontWeight = FontWeight.SemiBold)
                                }
                            }
                            TextButton(onClick = { showDeadlineDatePicker = true }) {
                                Text("Change Date")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Deadline Time (Optional)", style = MaterialTheme.typography.labelSmall)
                                    Text(draftDeadlineTime ?: "End of day", fontWeight = FontWeight.SemiBold)
                                }
                            }
                            Row {
                                if (draftDeadlineTime != null) {
                                    TextButton(onClick = { draftDeadlineTime = null }) {
                                        Text("Clear")
                                    }
                                }
                                TextButton(onClick = { showDeadlineTimePicker = true }) {
                                    Text("Pick Time")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SECTION: Recurrence
            Text("Recurrence Rules", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Box {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Repeat, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Repeat Pattern", style = MaterialTheme.typography.labelSmall)
                                    Text(recurrenceType.displayName, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            TextButton(onClick = { showRecurrenceMenu = true }) {
                                Text("Change")
                            }
                        }

                        DropdownMenu(expanded = showRecurrenceMenu, onDismissRequest = { showRecurrenceMenu = false }) {
                            RecurrenceType.entries.forEach { r ->
                                DropdownMenuItem(
                                    text = { Text(r.displayName) },
                                    onClick = {
                                        recurrenceType = r
                                        showRecurrenceMenu = false
                                        if (r == RecurrenceType.CUSTOM) {
                                            showCustomRecurrenceDialog = true
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Conditional inputs for specific recurrence selections
                    when (recurrenceType) {
                        RecurrenceType.EVERY_N_DAYS -> {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Repeat every", fontSize = 13.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedTextField(
                                    value = if (recurrenceInterval > 0) recurrenceInterval.toString() else "",
                                    onValueChange = { str ->
                                        val n = str.filter { it.isDigit() }.toIntOrNull()
                                        if (n != null && n in 1..999) recurrenceInterval = n
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.width(70.dp).height(50.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("days", fontSize = 13.sp)
                            }
                        }

                        RecurrenceType.EVERY_N_WEEKS -> {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Repeat every", fontSize = 13.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedTextField(
                                    value = if (recurrenceInterval > 0) recurrenceInterval.toString() else "",
                                    onValueChange = { str ->
                                        val n = str.filter { it.isDigit() }.toIntOrNull()
                                        if (n != null && n in 1..999) recurrenceInterval = n
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.width(70.dp).height(50.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("weeks", fontSize = 13.sp)
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text("On days", style = MaterialTheme.typography.labelSmall)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                listOf("MONDAY" to "M", "TUESDAY" to "T", "WEDNESDAY" to "W", "THURSDAY" to "T", "FRIDAY" to "F", "SATURDAY" to "S", "SUNDAY" to "S").forEach { (dKey, dInit) ->
                                    val isSel = recurrenceDaysOfWeek.contains(dKey)
                                    Surface(
                                        onClick = {
                                            recurrenceDaysOfWeek = if (isSel) recurrenceDaysOfWeek - dKey else recurrenceDaysOfWeek + dKey
                                        },
                                        shape = CircleShape,
                                        color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(dInit, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                }
                            }
                        }

                        RecurrenceType.EVERY_N_MONTHS -> {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Repeat every", fontSize = 13.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedTextField(
                                    value = if (recurrenceInterval > 0) recurrenceInterval.toString() else "",
                                    onValueChange = { str ->
                                        val n = str.filter { it.isDigit() }.toIntOrNull()
                                        if (n != null && n in 1..999) recurrenceInterval = n
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.width(70.dp).height(50.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("months", fontSize = 13.sp)
                            }
                        }

                        RecurrenceType.EVERY_N_YEARS -> {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Repeat every", fontSize = 13.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedTextField(
                                    value = if (recurrenceInterval > 0) recurrenceInterval.toString() else "",
                                    onValueChange = { str ->
                                        val n = str.filter { it.isDigit() }.toIntOrNull()
                                        if (n != null && n in 1..999) recurrenceInterval = n
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.width(70.dp).height(50.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("years", fontSize = 13.sp)
                            }
                        }

                        RecurrenceType.SELECTED_WEEKDAYS -> {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Select Weekdays", style = MaterialTheme.typography.labelSmall)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                listOf("MONDAY" to "Mon", "TUESDAY" to "Tue", "WEDNESDAY" to "Wed", "THURSDAY" to "Thu", "FRIDAY" to "Fri", "SATURDAY" to "Sat", "SUNDAY" to "Sun").forEach { (dKey, dInit) ->
                                    val isSel = recurrenceDaysOfWeek.contains(dKey)
                                    FilterChip(
                                        selected = isSel,
                                        onClick = {
                                            recurrenceDaysOfWeek = if (isSel) recurrenceDaysOfWeek - dKey else recurrenceDaysOfWeek + dKey
                                        },
                                        label = { Text(dInit, fontSize = 10.sp) }
                                    )
                                }
                            }
                        }

                        RecurrenceType.CUSTOM -> {
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { showCustomRecurrenceDialog = true }) {
                                Icon(Icons.Default.Repeat, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Open Custom Recurrence Editor")
                            }
                        }

                        else -> {}
                    }

                    // Live Recurrence Preview Card
                    if (recurrenceType != RecurrenceType.NONE) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = recurrencePreview,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (nextOccurrences.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Next: " + nextOccurrences.joinToString(", ") { DateUtils.formatDisplayDate(it) },
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SECTION: Multiple Reminders
            Text("Reminders", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    if (remindersList.isEmpty()) {
                        Text(
                            text = "No reminders scheduled",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        remindersList.forEachIndexed { index, rem ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(rem.displayLabel, style = MaterialTheme.typography.bodyMedium)
                                }
                                IconButton(
                                    onClick = {
                                        val mutable = remindersList.toMutableList()
                                        mutable.removeAt(index)
                                        remindersList = mutable
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove reminder", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = { showAddReminderDialog = true },
                        modifier = Modifier.testTag("adv_add_reminder_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Custom Reminder")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Reminders Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "⚡ Add to Quick Reminders",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Automatically place this task on your Home Dashboard QR grid for one-tap execution",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    androidx.compose.material3.Switch(
                        checked = isQuickReminder,
                        onCheckedChange = { isQuickReminder = it },
                        modifier = Modifier.testTag("adv_editor_qr_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Medicine Reminder Section
            MedicineReminderSection(
                isMedicineEnabled = isMedicineEnabled,
                onMedicineEnabledChange = { isMedicineEnabled = it },
                taskTitle = title,
                initialMedication = null,
                onMedicationDraftChanged = { medicationDraft = it }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Emoji Selection Dialog
    if (showEmojiDialog) {
        EmojiInputDialog(
            currentEmoji = emoji,
            onEmojiSelected = { chosen ->
                emoji = chosen
            },
            onDismiss = { showEmojiDialog = false }
        )
    }

    // Custom Recurrence Dialog
    if (showCustomRecurrenceDialog) {
        CustomRecurrenceDialog(
            startDateIso = dueDateIso,
            initialInterval = recurrenceInterval,
            initialUnit = recurrenceUnit,
            initialDaysOfWeek = recurrenceDaysOfWeek,
            initialEndCondition = recurrenceEndCondition,
            initialEndDateIso = recurrenceEndDateIso,
            initialCount = recurrenceCount,
            onApply = { interval, unit, days, endCond, endDate, count ->
                recurrenceType = RecurrenceType.CUSTOM
                recurrenceInterval = interval
                recurrenceUnit = unit
                recurrenceDaysOfWeek = days
                recurrenceEndCondition = endCond
                recurrenceEndDateIso = endDate
                recurrenceCount = count
                showCustomRecurrenceDialog = false
            },
            onDismiss = { showCustomRecurrenceDialog = false }
        )
    }

    // Custom Reminder Dialog
    if (showAddReminderDialog) {
        CustomReminderDialog(
            schedulingMode = schedulingMode,
            onAddReminder = { reminderItem ->
                remindersList = remindersList + reminderItem
                showAddReminderDialog = false
            },
            onDismiss = { showAddReminderDialog = false }
        )
    }

    // Material 3 Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { ms ->
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                        dueDateIso = sdf.format(Date(ms))
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Deadline Date Picker Dialog
    if (showDeadlineDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDeadlineDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    deadlineDatePickerState.selectedDateMillis?.let { ms ->
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                        draftDeadlineIso = sdf.format(Date(ms))
                    }
                    showDeadlineDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeadlineDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = deadlineDatePickerState)
        }
    }

    // Time Picker Modal Dialog
    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val h = timePickerState.hour.toString().padStart(2, '0')
                    val m = timePickerState.minute.toString().padStart(2, '0')
                    draftDueTime = "$h:$m"
                    showTimePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }

    // Deadline Time Picker Modal Dialog
    if (showDeadlineTimePicker) {
        AlertDialog(
            onDismissRequest = { showDeadlineTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val h = deadlineTimePickerState.hour.toString().padStart(2, '0')
                    val m = deadlineTimePickerState.minute.toString().padStart(2, '0')
                    draftDeadlineTime = "$h:$m"
                    showDeadlineTimePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeadlineTimePicker = false }) {
                    Text("Cancel")
                }
            },
            text = {
                TimePicker(state = deadlineTimePickerState)
            }
        )
    }

    // Inline Create Project Bottom Sheet
    if (showCreateProjectSheet && onCreateProject != null) {
        EditProjectBottomSheet(
            project = null,
            colorOrderingMode = colorOrderingMode,
            colorUsageCounts = colorUsageCounts,
            colorCustomOrder = colorCustomOrder,
            onSave = { name, desc, projEmoji, colorId, isFav ->
                val newProject = ProjectItem(
                    name = name,
                    description = desc,
                    emoji = projEmoji,
                    colorId = colorId,
                    isFavorite = isFav
                )
                onCreateProject(newProject) { newId ->
                    selectedProjectId = newId
                    showCreateProjectSheet = false
                }
            },
            onColorUsed = onColorUsed,
            onModeChanged = onModeChanged,
            onSaveCustomOrder = onSaveCustomOrder,
            onResetCustomOrder = onResetCustomOrder,
            onDismiss = { showCreateProjectSheet = false }
        )
    }

    // Inline Create Label Bottom Sheet
    if (showCreateLabelSheet && onCreateLabel != null) {
        EditLabelBottomSheet(
            label = null,
            colorOrderingMode = colorOrderingMode,
            colorUsageCounts = colorUsageCounts,
            colorCustomOrder = colorCustomOrder,
            onSave = { name, colorId, isFav ->
                val newLabel = LabelItem(
                    name = name,
                    colorId = colorId,
                    isFavorite = isFav
                )
                onCreateLabel(newLabel) { newId ->
                    selectedLabelIds = selectedLabelIds + newId
                    showCreateLabelSheet = false
                }
            },
            onColorUsed = onColorUsed,
            onModeChanged = onModeChanged,
            onSaveCustomOrder = onSaveCustomOrder,
            onResetCustomOrder = onResetCustomOrder,
            onDismiss = { showCreateLabelSheet = false }
        )
    }
}
