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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material3.Button
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ColorOrderingMode
import com.example.domain.model.LabelItem
import com.example.domain.model.ProjectItem
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskPriority
import com.example.domain.model.TaskReminderItem
import com.example.domain.model.TodoistColorPalette
import com.example.ui.screens.labels.EditLabelBottomSheet
import com.example.ui.screens.projects.EditProjectBottomSheet
import com.example.utils.DateUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskQuickAddSheet(
    initialTask: TaskItem? = null,
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
    defaultDuration: Int = 30,
    onSaveTaskWithMedication: ((TaskItem, com.example.domain.medication.MedicationItem?) -> Unit)? = null,
    onExpandToAdvanced: (TaskItem) -> Unit,
    onDismiss: () -> Unit
) {
    val today = DateUtils.todayString()
    var title by remember { mutableStateOf(initialTask?.title ?: "") }
    var emoji by remember { mutableStateOf(initialTask?.emoji ?: "") }
    var description by remember { mutableStateOf(initialTask?.description ?: "") }
    var dueDateIso by remember { mutableStateOf(initialTask?.dueDateIso ?: today) }
    var priority by remember { mutableStateOf(initialTask?.priority ?: TaskPriority.P3) }
    var selectedProjectId by remember { mutableStateOf<Long?>(initialTask?.projectId) }
    var selectedLabelIds by remember { mutableStateOf<Set<Long>>(initialTask?.labels?.map { it.id }?.toSet() ?: emptySet()) }
    var isReminderEnabled by remember { mutableStateOf(initialTask?.isReminderEnabled ?: false) }
    var isQuickReminder by remember { mutableStateOf(initialTask?.isQuickReminder ?: false) }
    var isMedicineEnabled by remember { mutableStateOf(false) }
    var medicationDraft by remember { mutableStateOf<com.example.domain.medication.MedicationItem?>(null) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showEmojiDialog by remember { mutableStateOf(false) }
    var showProjectDropdown by remember { mutableStateOf(false) }
    var showLabelDropdown by remember { mutableStateOf(false) }
    var showCreateProjectSheet by remember { mutableStateOf(false) }
    var showCreateLabelSheet by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )

    fun currentTaskDraft(): TaskItem {
        val selectedLabels = labels.filter { selectedLabelIds.contains(it.id) }
        val reminders = if (isReminderEnabled) {
            listOf(TaskReminderItem(minutesBefore = 0, isEnabled = true))
        } else emptyList()

        return (initialTask ?: TaskItem(
            title = title.trim(),
            dueDateIso = dueDateIso,
            durationMinutes = defaultDuration,
            durationAmount = defaultDuration
        )).copy(
            title = title.trim(),
            emoji = emoji.trim(),
            description = description.trim(),
            dueDateIso = dueDateIso,
            priority = priority,
            projectId = selectedProjectId,
            labels = selectedLabels,
            isReminderEnabled = isReminderEnabled,
            isQuickReminder = isQuickReminder,
            reminders = reminders
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("task_quick_add_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 18.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Quick Add",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onExpandToAdvanced(currentTaskDraft()) },
                        modifier = Modifier.testTag("quick_add_expand_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInFull,
                            contentDescription = "Expand to Advanced Editor",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(20.dp))
                    }
                }
            }

            // Task title input & Emoji selector row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Interactive Emoji Button
                Surface(
                    onClick = { showEmojiDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    color = if (emoji.isNotBlank()) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (emoji.isNotBlank()) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("quick_add_emoji_btn")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (emoji.isNotBlank()) {
                            Text(text = emoji, fontSize = 22.sp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.SentimentSatisfied,
                                contentDescription = "Choose Emoji",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("What needs to be done?", fontSize = 16.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quick_add_title_input")
                )
            }

            // Optional description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = { Text("Description or details...", fontSize = 13.sp) },
                maxLines = 2,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                ),
                textStyle = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quick_add_desc_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Date Shortcuts Row
            val tomorrow = DateUtils.addDays(today, 1)
            val nextWeek = DateUtils.addDays(today, 7)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = dueDateIso == today,
                    onClick = { dueDateIso = today },
                    label = { Text("Today", fontSize = 11.sp) },
                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(12.dp)) },
                    modifier = Modifier.testTag("quick_date_today")
                )
                FilterChip(
                    selected = dueDateIso == tomorrow,
                    onClick = { dueDateIso = tomorrow },
                    label = { Text("Tomorrow", fontSize = 11.sp) },
                    modifier = Modifier.testTag("quick_date_tomorrow")
                )
                FilterChip(
                    selected = dueDateIso == nextWeek,
                    onClick = { dueDateIso = nextWeek },
                    label = { Text("Next Week", fontSize = 11.sp) },
                    modifier = Modifier.testTag("quick_date_next_week")
                )
                FilterChip(
                    selected = dueDateIso != today && dueDateIso != tomorrow && dueDateIso != nextWeek,
                    onClick = { showDatePicker = true },
                    label = { Text(if (dueDateIso != today && dueDateIso != tomorrow && dueDateIso != nextWeek) dueDateIso else "Custom...", fontSize = 11.sp) },
                    modifier = Modifier.testTag("quick_date_custom")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Metadata Row: Priority, Project, Labels, Reminders
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Priority Selector Chips (P1, P2, P3, P4)
                TaskPriority.allPriorities.forEach { p ->
                    val isSel = priority == p
                    Surface(
                        onClick = { priority = p },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSel) p.composeColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = if (isSel) androidx.compose.foundation.BorderStroke(1.5.dp, p.composeColor) else null,
                        modifier = Modifier.testTag("priority_chip_${p.code}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Flag,
                                contentDescription = p.displayName,
                                tint = p.composeColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = p.code, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = p.composeColor)
                        }
                    }
                }

                // Project selector chip with + Create New Project
                Box {
                    val activeProject = projects.find { it.id == selectedProjectId }
                    Surface(
                        onClick = { showProjectDropdown = true },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.testTag("quick_add_project_selector")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = activeProject?.emoji ?: "📁", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = activeProject?.name ?: "Inbox",
                                fontSize = 11.sp,
                                maxLines = 1,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showProjectDropdown,
                        onDismissRequest = { showProjectDropdown = false }
                    ) {
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
                            modifier = Modifier.testTag("quick_add_create_project_option")
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

                // Label selector chip with + Create New Label
                Box {
                    Surface(
                        onClick = { showLabelDropdown = true },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.testTag("quick_add_label_selector")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Label, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (selectedLabelIds.isEmpty()) "Labels" else "${selectedLabelIds.size} labels",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showLabelDropdown,
                        onDismissRequest = { showLabelDropdown = false }
                    ) {
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
                            modifier = Modifier.testTag("quick_add_create_label_option")
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
                                            Text("#${lbl.name}", color = lbl.composeColor, fontWeight = FontWeight.SemiBold)
                                            if (isChecked) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedLabelIds = if (isChecked) {
                                            selectedLabelIds - lbl.id
                                        } else {
                                            selectedLabelIds + lbl.id
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Reminder toggle shortcut
                Surface(
                    onClick = { isReminderEnabled = !isReminderEnabled },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isReminderEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.testTag("quick_add_reminder_toggle")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = "Reminder",
                            tint = if (isReminderEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isReminderEnabled) "Remind On" else "Reminder",
                            fontSize = 11.sp,
                            color = if (isReminderEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Quick Reminders tile toggle chip
                Surface(
                    onClick = { isQuickReminder = !isQuickReminder },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isQuickReminder) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isQuickReminder) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.testTag("quick_add_qr_toggle")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isQuickReminder) "⚡ In QR Tiles" else "⚡ Add to QR",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isQuickReminder) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Medicine Reminder Section (Toggle & Progressive Disclosure)
            MedicineReminderSection(
                isMedicineEnabled = isMedicineEnabled,
                onMedicineEnabledChange = { isMedicineEnabled = it },
                taskTitle = title,
                initialMedication = null,
                onMedicationDraftChanged = { medicationDraft = it }
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Action Row: Save button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            val draft = currentTaskDraft()
                            if (onSaveTaskWithMedication != null) {
                                onSaveTaskWithMedication(draft, if (isMedicineEnabled) medicationDraft else null)
                            } else {
                                onSaveTask(draft)
                            }
                            onDismiss()
                        }
                    },
                    enabled = title.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("quick_add_save_btn")
                ) {
                    Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Task")
                }
            }
        }
    }

    // Emoji Dialog
    if (showEmojiDialog) {
        EmojiInputDialog(
            currentEmoji = emoji,
            onEmojiSelected = { chosen ->
                emoji = com.example.utils.EmojiUtils.extractFirstEmoji(chosen) ?: chosen.trim().take(2)
            },
            onDismiss = { showEmojiDialog = false }
        )
    }

    // Material 3 DatePickerDialog
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
