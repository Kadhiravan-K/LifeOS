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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ColorOrderingMode
import com.example.domain.model.LabelItem
import com.example.domain.model.ProjectItem
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskPriority
import com.example.domain.model.TaskReminderItem
import com.example.domain.model.TaskDateRange
import com.example.domain.model.TaskDateRangePolicy
import com.example.domain.model.MedicinePrioritySelection
import com.example.domain.model.TodoistColorPalette
import com.example.ui.screens.labels.EditLabelBottomSheet
import com.example.ui.screens.projects.EditProjectBottomSheet
import com.example.utils.DateUtils
import android.widget.Toast

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
    onSaveTaskWithResult: ((TaskItem, com.example.domain.medication.MedicationItem?, (Result<com.example.ui.viewmodel.TaskSaveReceipt>) -> Unit) -> Unit)? = null,
    onExpandToAdvanced: (TaskItem) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val today = DateUtils.todayString()
    var title by remember { mutableStateOf(initialTask?.title ?: "") }
    var emoji by remember { mutableStateOf(initialTask?.emoji ?: "") }
    var isEmojiManuallySelected by remember { mutableStateOf(!initialTask?.emoji.isNullOrBlank()) }
    val suggestedEmojis by remember(title, isEmojiManuallySelected) {
        androidx.compose.runtime.derivedStateOf {
            if (isEmojiManuallySelected) emptyList()
            else com.example.domain.emoji.TaskEmojiSuggestionEngine.suggestEmojis(title)
        }
    }

    var description by remember { mutableStateOf(initialTask?.description ?: "") }
    var dueDateIso by remember { mutableStateOf(initialTask?.dueDateIso ?: today) }
    var deadlineIso by remember { mutableStateOf<String?>(initialTask?.deadlineIso) }
    var medicinePriority by remember(initialTask?.id) {
        mutableStateOf(MedicinePrioritySelection(initialTask?.priority ?: TaskPriority.P3, false))
    }
    val priority = medicinePriority.effectivePriority

    var selectedProjectId by remember { mutableStateOf<Long?>(initialTask?.projectId) }
    var selectedLabelIds by remember { mutableStateOf<Set<Long>>(initialTask?.labels?.map { it.id }?.toSet() ?: emptySet()) }
    var isReminderEnabled by remember { mutableStateOf(initialTask?.isReminderEnabled ?: false) }
    var isQuickReminder by remember { mutableStateOf(initialTask?.isQuickReminder ?: false) }
    var isMedicineEnabled by remember { mutableStateOf(false) }
    var medicationDraft by remember { mutableStateOf<com.example.domain.medication.MedicationItem?>(null) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showEmojiDialog by remember { mutableStateOf(false) }
    var showProjectDropdown by remember { mutableStateOf(false) }
    var showMultiLabelSheet by remember { mutableStateOf(false) }
    var showCreateProjectSheet by remember { mutableStateOf(false) }
    var showCreateLabelSheet by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }

    fun updateDueDate(newDateIso: String) {
        val updated = TaskDateRangePolicy.updateStartDate(
            TaskDateRange(dueDateIso, deadlineIso),
            newDateIso
        )
        dueDateIso = updated.startDateIso
        deadlineIso = updated.endDateIso
    }

    fun currentTaskDraft(): TaskItem {
        val selectedLabels = labels.filter { selectedLabelIds.contains(it.id) }
        val reminders = if (isReminderEnabled) {
            listOf(TaskReminderItem(minutesBefore = 0, isEnabled = true))
        } else emptyList()

        return (initialTask ?: TaskItem(
            title = title.trim(),
            dueDateIso = dueDateIso,
            deadlineIso = deadlineIso,
            durationMinutes = defaultDuration,
            durationAmount = defaultDuration
        )).copy(
            title = title.trim(),
            emoji = emoji.trim(),
            description = description.trim(),
            dueDateIso = dueDateIso,
            deadlineIso = deadlineIso,
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
                    onValueChange = { newTitle ->
                        title = newTitle
                        if (!isEmojiManuallySelected) {
                            val leadingEmoji = com.example.utils.EmojiUtils.extractLeadingEmoji(newTitle)
                            if (leadingEmoji != null) {
                                emoji = leadingEmoji
                            }
                        }
                    },
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

            // Requirement #5: Responsive horizontal strip of offline suggested emojis
            if (!isEmojiManuallySelected && suggestedEmojis.isNotEmpty()) {
                EmojiSuggestionRow(
                    suggestions = suggestedEmojis,
                    currentEmoji = emoji,
                    onEmojiSelected = { chosen ->
                        emoji = chosen
                        isEmojiManuallySelected = true
                    },
                    modifier = Modifier.padding(vertical = 2.dp)
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
                    onClick = { updateDueDate(today) },
                    label = { Text("Today", fontSize = 11.sp) },
                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(12.dp)) },
                    modifier = Modifier.testTag("quick_date_today")
                )
                FilterChip(
                    selected = dueDateIso == tomorrow,
                    onClick = { updateDueDate(tomorrow) },
                    label = { Text("Tomorrow", fontSize = 11.sp) },
                    modifier = Modifier.testTag("quick_date_tomorrow")
                )
                FilterChip(
                    selected = dueDateIso == nextWeek,
                    onClick = { updateDueDate(nextWeek) },
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
                        onClick = {
                            medicinePriority = medicinePriority.selectPriority(p)
                        },
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

                // Requirement #6: Multi-Label Trigger Chip
                Surface(
                    onClick = { showMultiLabelSheet = true },
                    shape = RoundedCornerShape(8.dp),
                    color = if (selectedLabelIds.isNotEmpty()) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = if (selectedLabelIds.isNotEmpty()) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
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

            // Requirement #6: Selected Label Chips with Remove Action
            val currentSelectedLabels = labels.filter { selectedLabelIds.contains(it.id) }
            if (currentSelectedLabels.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                SelectedLabelChips(
                    selectedLabels = currentSelectedLabels,
                    onRemoveLabel = { removedId ->
                        selectedLabelIds = selectedLabelIds - removedId
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Requirement #18: Always-visible optional deadline field
            LifeDatePickerField(
                selectedDateIso = deadlineIso ?: "",
                onDateSelected = { deadlineIso = it },
                label = "Deadline (Optional)",
                minDateIso = dueDateIso,
                isNullable = true,
                onClear = { deadlineIso = null },
                modifier = Modifier.testTag("quick_add_deadline_field")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Medicine Reminder Section (Toggle & Progressive Disclosure)
            MedicineReminderSection(
                isMedicineEnabled = isMedicineEnabled,
                onMedicineEnabledChange = { enabled ->
                    isMedicineEnabled = enabled
                    medicinePriority = medicinePriority.setMedicineReminderEnabled(enabled)
                },
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
                        if (title.isNotBlank() && !isSaving) {
                            saveError = null
                            val draft = currentTaskDraft()
                            val medication = if (isMedicineEnabled) medicationDraft else null
                            if (onSaveTaskWithResult != null) {
                                isSaving = true
                                onSaveTaskWithResult(draft, medication) { result ->
                                    isSaving = false
                                    result.fold(
                                        onSuccess = { receipt ->
                                            receipt.warning?.let {
                                                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                                            }
                                            onDismiss()
                                        },
                                        onFailure = { error ->
                                            saveError = error.message ?: "Task could not be saved. Please try again."
                                        }
                                    )
                                }
                            } else {
                                if (onSaveTaskWithMedication != null) {
                                    onSaveTaskWithMedication(draft, medication)
                                } else {
                                    onSaveTask(draft)
                                }
                                onDismiss()
                            }
                        }
                    },
                    enabled = title.isNotBlank() && !isSaving,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("quick_add_save_btn")
                ) {
                    Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isSaving) "Saving..." else "Add Task")
                }
            }
            if (saveError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = saveError.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.testTag("quick_add_save_error")
                )
            }
        }
    }

    // Emoji Dialog
    if (showEmojiDialog) {
        EmojiInputDialog(
            currentEmoji = emoji,
            onEmojiSelected = { chosen ->
                emoji = com.example.utils.EmojiUtils.extractFirstEmoji(chosen) ?: chosen.trim()
                isEmojiManuallySelected = true
            },
            onDismiss = { showEmojiDialog = false }
        )
    }

    // Requirement #6: Multi-Label Selection Bottom Sheet
    if (showMultiLabelSheet) {
        MultiLabelSelectorBottomSheet(
            availableLabels = labels,
            selectedLabelIds = selectedLabelIds,
            onToggleLabel = { id ->
                selectedLabelIds = if (selectedLabelIds.contains(id)) {
                    selectedLabelIds - id
                } else {
                    selectedLabelIds + id
                }
            },
            onCreateNewLabel = {
                showMultiLabelSheet = false
                showCreateLabelSheet = true
            },
            onDismiss = { showMultiLabelSheet = false }
        )
    }

    // The shared Material 3 calendar scrolls vertically through months.
    if (showDatePicker) {
        LifeDatePickerDialog(
            initialDateIso = dueDateIso,
            title = "Select Start Date",
            onConfirm = {
                updateDueDate(it)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
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
