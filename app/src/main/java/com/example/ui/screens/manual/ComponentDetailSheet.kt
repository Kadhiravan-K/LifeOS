package com.example.ui.screens.manual

import com.example.domain.emoji.TaskEmojiSuggestionEngine
import com.example.ui.components.EmojiSuggestionRow
import com.example.ui.components.SelectedLabelChips
import com.example.ui.components.MultiLabelSelectorBottomSheet
import com.example.ui.components.MedicineReminderSection
import com.example.ui.screens.medication.MedicationCourseCard
import com.example.ui.screens.medication.MedicationDetailsBottomSheet
import com.example.ui.screens.calendar.MonthGridView
import com.example.domain.medication.MedicationItem
import com.example.domain.medication.MedicationScheduleConfig
import com.example.domain.medication.MedicationDoseItem
import com.example.domain.medication.DoseStatus
import com.example.domain.model.LabelItem

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.foundation.clickable
import androidx.compose.material3.Switch
import androidx.compose.material3.Slider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import com.example.ui.components.PremiumCard
import com.example.ui.components.PremiumCardStyle
import com.example.ui.components.PremiumButton
import com.example.ui.components.PremiumButtonStyle
import com.example.ui.theme.engine.AnimationSpeed
import com.example.ui.theme.engine.LocalLifeAnimationSpeed
import com.example.ui.theme.engine.ThemedLoadingIndicator
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ColorOrderingMode
import com.example.domain.model.DurationUnit
import com.example.domain.model.TaskPriority
import com.example.domain.model.TodoistColorPalette
import com.example.ui.components.ColorBadge
import com.example.ui.components.ColorDot
import com.example.ui.components.DateRangeSelection
import com.example.ui.components.LifeDateField
import com.example.ui.components.LifeDateRangeField
import com.example.ui.components.LifeDurationPicker
import com.example.ui.components.LifeEmojiField
import com.example.ui.components.LifeEmojiPickerRow
import com.example.ui.components.LifeExposedDropdown
import com.example.ui.components.LifeSafetyDialog
import com.example.ui.components.LabeledLinksInput
import com.example.ui.components.CategoryChip
import com.example.ui.components.DurationInput
import com.example.ui.components.EmojiInputDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.LabeledLinkItem
import com.example.ui.components.LabeledUrlEditor
import com.example.ui.components.LifeDatePickerField
import com.example.ui.components.LifeDropdownField
import com.example.ui.components.LifeTimePickerField
import com.example.ui.components.PriorityBadge
import com.example.ui.components.PrioritySelectorGroup
import com.example.ui.components.SecureConfirmationDialog
import com.example.ui.components.TaskCompletionCircle
import com.example.ui.components.TodoistColorPickerSheet
import com.example.utils.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComponentDetailSheet(
    component: ComponentEntry,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var isCodeCopied by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("component_detail_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = component.category.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = component.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = component.category.displayName,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            StatusBadge(component.status)
                        }
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // =========================================================================
            // 1. OVERVIEW SECTION
            // =========================================================================
            SectionTitle("OVERVIEW & PURPOSE")
            Text(
                text = component.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "When to Use",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = component.whenToUse,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // =========================================================================
            // 2. LIVE INTERACTIVE PREVIEW (Safe Sandbox)
            // =========================================================================
            SectionTitle("LIVE INTERACTIVE PREVIEW")
            Text(
                text = "Interactive sandbox isolated from database state. Changes made here will not mutate real app records.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("interactive_preview_container")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    RenderComponentLivePreview(component.id)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // =========================================================================
            // 3. DEVELOPER REFERENCE & API CONTRACT
            // =========================================================================
            SectionTitle("DEVELOPER REFERENCE")

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    ReferenceRow("Source File", component.sourceFile)
                    Spacer(modifier = Modifier.height(6.dp))
                    ReferenceRow("Composable", component.composableName)
                    Spacer(modifier = Modifier.height(6.dp))
                    ReferenceRow("Screens Reusing This", component.usageLocations.joinToString(", "))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Key Parameters Table
            if (component.parameters.isNotEmpty()) {
                Text(
                    text = "Key Parameters & State Callbacks:",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    component.parameters.forEach { param ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = param.name,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = param.type,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = param.description,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Copyable Code Snippet
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Kotlin / Compose Usage Example:",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                FilledTonalButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(component.codeExample))
                        isCodeCopied = true
                        Toast.makeText(context, "Code copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("copy_code_snippet_btn")
                ) {
                    Icon(
                        imageVector = if (isCodeCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isCodeCopied) "Copied!" else "Copy Code", fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = component.codeExample,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // =========================================================================
            // 4. REUSE RECOMMENDATIONS
            // =========================================================================
            SectionTitle("REUSE RECOMMENDATIONS & ROADMAP")
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = component.reuseRecommendations,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RenderComponentLivePreview(componentId: String) {
    when (componentId) {
        "clock_time_input" -> {
            var sampleTime by remember { mutableStateOf("14:30") }
            Column {
                Text("Sample Time Field with Quick Presets & Analog/Digital Modes:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                LifeTimePickerField(
                    time = sampleTime,
                    onTimeSelected = { sampleTime = it },
                    label = "Preview Reminder Time",
                    onClear = { sampleTime = "" }
                )
            }
        }

        "date_trigger_field" -> {
            var sampleDate by remember { mutableStateOf(DateUtils.todayString()) }
            var range by remember { mutableStateOf(DateRangeSelection(DateUtils.todayString(), DateUtils.addDays(DateUtils.todayString(), 7))) }
            Column {
                Text("1. Standard Date Field with Today/Tomorrow Presets:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                LifeDateField(
                    selectedDateIso = sampleDate,
                    onDateSelected = { sampleDate = it },
                    label = "Single Due Date",
                    minDateIso = DateUtils.todayString()
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text("2. Date Range Field with Next 7d/30d/Quarterly Presets:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                LifeDateRangeField(
                    startDateIso = range.startDateIso,
                    endDateIso = range.endDateIso,
                    onRangeSelected = { range = it }
                )
            }
        }

        "emoji_icon_selector" -> {
            var selectedEmoji by remember { mutableStateOf("ðŸš€") }
            Column {
                Text("1. Compact Carousel with 'Custom...' Modal Trigger:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                LifeEmojiPickerRow(
                    selectedEmoji = selectedEmoji,
                    onEmojiSelected = { selectedEmoji = it }
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text("2. Standard Clickable Icon/Emoji Field:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                LifeEmojiField(
                    emoji = selectedEmoji,
                    onEmojiSelected = { selectedEmoji = it }
                )
            }
        }

        "duration_stepper_input" -> {
            var amount by remember { mutableIntStateOf(45) }
            var unit by remember { mutableStateOf(DurationUnit.MINUTES) }
            var isAllDay by remember { mutableStateOf(false) }
            Column {
                Text("Interactive Duration Stepper [-] / [+], Presets (15m, 30m, 45m, 1h, 2h) & All-Day Mode:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                LifeDurationPicker(
                    durationAmount = amount,
                    durationUnit = unit,
                    onDurationAmountChanged = { amount = it },
                    onDurationUnitChanged = { unit = it },
                    isAllDay = isAllDay,
                    onAllDayToggle = { isAllDay = it }
                )
            }
        }

        "priority_selector_group" -> {
            var priority by remember { mutableStateOf(TaskPriority.P1) }
            Column {
                Text("Single-Select Segmented Priority Group (P1-P4):", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                PrioritySelectorGroup(
                    selectedPriority = priority,
                    onPrioritySelected = { priority = it }
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Resulting Badge: ", fontSize = 12.sp)
                    PriorityBadge(priority = priority)
                }
            }
        }

        "type_safe_dropdown" -> {
            val categories = listOf("Habits & Daily Routine", "Work & Career", "Health & Fitness", "Personal Finance", "Learning")
            var selected by remember { mutableStateOf(categories[0]) }
            Column {
                Text("Generic Type-Safe Dropdown Field (LifeExposedDropdown):", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                LifeExposedDropdown(
                    label = "Task Category",
                    items = categories,
                    selectedItem = selected,
                    onItemSelected = { selected = it }
                )
            }
        }

        "labeled_links_editor" -> {
            var links by remember {
                mutableStateOf(
                    listOf(
                        LabeledLinkItem(label = "Location Map", url = "https://maps.google.com/sample"),
                        LabeledLinkItem(label = "Meeting Link", url = "https://meet.google.com/abc-defg-hij")
                    )
                )
            }
            Column {
                Text("Multi-Link Manager with Type Icons (Maps/Video/Web) & One-Tap Tester:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                LabeledLinksInput(
                    links = links,
                    onLinksChanged = { links = it }
                )
            }
        }

        "secure_confirmation_dialog" -> {
            var showModal by remember { mutableStateOf(false) }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("Audition LifeSafetyDialog with Consequence Alert, 3s Countdown & Phrase Match:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { showModal = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Trigger Safety Dialog Demo")
                }

                if (showModal) {
                    LifeSafetyDialog(
                        title = "Sample Purge Operation",
                        message = "This modal demonstrates high-safety destruction prevention with a 3-second countdown and phrase verification.",
                        consequenceText = "Sandbox mode active: No actual application data will be modified.",
                        requiredConfirmationPhrase = "PURGE",
                        countdownSeconds = 3,
                        confirmLabel = "Purge Safely",
                        isDestructive = true,
                        onConfirm = { showModal = false },
                        onDismiss = { showModal = false }
                    )
                }
            }
        }

        "color_picker_palette" -> {
            var selectedColorId by remember { mutableStateOf("sky") }
            var showSheet by remember { mutableStateOf(false) }
            val colorLong = TodoistColorPalette.getColorLong(selectedColorId)

            Column {
                Text("Color Palette Swatch, ColorDot, ColorBadge & Sheet Launcher:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ColorDot(color = Color(colorLong), size = 28.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    ColorBadge(
                        color = Color(colorLong),
                        label = selectedColorId.replaceFirstChar { it.uppercase() },
                        hexCode = "#" + java.lang.Long.toHexString(colorLong).uppercase()
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(onClick = { showSheet = true }) {
                    Text("Open Todoist Color Picker Sheet")
                }

                if (showSheet) {
                    TodoistColorPickerSheet(
                        selectedColorId = selectedColorId,
                        orderingMode = ColorOrderingMode.AUTO,
                        usageCounts = emptyMap(),
                        customOrder = emptyList(),
                        onColorSelected = {
                            selectedColorId = it.id
                            showSheet = false
                        },
                        onModeChanged = {},
                        onSaveCustomOrder = {},
                        onResetCustomOrder = {},
                        onDismiss = { showSheet = false }
                    )
                }
            }
        }

        "task_completion_circle" -> {
            var isChecked by remember { mutableStateOf(false) }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("Tap circle to toggle completion state:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TaskCompletionCircle(
                        isCompleted = isChecked,
                        activeColor = Color(0xFFEF4444),
                        onToggle = { isChecked = !isChecked }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isChecked) "Completed! (Strike-through active)" else "Pending Task (Tap to complete)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        "empty_state_card" -> {
            EmptyStateCard(
                emoji = "\u2728",
                title = "Zero Tasks Today",
                description = "All your habits and scheduled tasks are complete. Take a well-deserved rest!",
                actionButtonText = "Add Bonus Task",
                onActionClick = {}
            )
        }

        "category_chip" -> {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CategoryChip(categoryName = "Personal & Self-Care")
                CategoryChip(categoryName = "Work & Career")
                CategoryChip(categoryName = "Health & Routine")
            }
        }

        "search_bar_field" -> {
            var searchQuery by remember { mutableStateOf("Release 2 Verification") }
            Column {
                Text("Pill-shaped live search bar with instant clear button:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search tasks, settings, components...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (searchQuery.isBlank()) "Type to filter results in real time" else "Filtering results for: \"$searchQuery\"",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        "filter_chips_row" -> {
            val filterOptions = listOf("All Items", "Urgent P1", "Today", "Daily Habits", "Completed")
            var activeFilter by remember { mutableStateOf("All Items") }
            Column {
                Text("Horizontal scrolling FilterChips row with active indicator:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filterOptions.forEach { option ->
                        FilterChip(
                            selected = activeFilter == option,
                            onClick = { activeFilter = option },
                            label = { Text(option, fontSize = 12.sp) },
                            leadingIcon = if (activeFilter == option) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text("Active selection: $activeFilter", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
            }
        }

        "sort_selector_menu" -> {
            val sortOptions = listOf("Priority (P1 → P4)", "Due Date (Earliest First)", "Alphabetical (A-Z)", "Created Date")
            var selectedSort by remember { mutableStateOf(sortOptions[0]) }
            var isAscending by remember { mutableStateOf(true) }
            Column {
                Text("List sort ordering selector with directional toggle:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledTonalButton(
                        onClick = {
                            val nextIndex = (sortOptions.indexOf(selectedSort) + 1) % sortOptions.size
                            selectedSort = sortOptions[nextIndex]
                        }
                    ) {
                        Icon(Icons.Default.Sort, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(selectedSort, fontSize = 12.sp)
                    }
                    IconButton(onClick = { isAscending = !isAscending }) {
                        Icon(
                            imageVector = if (isAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                            contentDescription = "Toggle sort direction",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Sorted by: $selectedSort (${if (isAscending) "Ascending" else "Descending"})",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        "grid_list_toggle" -> {
            var isGridMode by remember { mutableStateOf(false) }
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isGridMode) "Active: 2-Column Grid Layout" else "Active: 1-Column List Layout",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    FilledTonalIconButton(onClick = { isGridMode = !isGridMode }) {
                        Icon(
                            imageVector = if (isGridMode) Icons.Default.ViewList else Icons.Default.GridView,
                            contentDescription = "Toggle Grid/List View"
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                if (isGridMode) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Card(shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f)) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Grid Card 1", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("P1 • 09:00", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Card(shape = RoundedCornerShape(10.dp), modifier = Modifier.weight(1f)) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Grid Card 2", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("P2 • 14:00", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                } else {
                    Card(shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("List Item Row", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Single-column high-detail layout with full metadata", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        "habit_streak_indicator" -> {
            var streakCount by remember { mutableIntStateOf(14) }
            Column {
                Text("Habit streak badge with 7-day mini completion history:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🔥", fontSize = 18.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("$streakCount Days Streak!", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFF59E0B))
                            Text("Best: 28 Days • Top 5%", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Button(onClick = { streakCount++ }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))) {
                        Text("+1 Day", fontSize = 11.sp)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                    listOf("M", "T", "W", "T", "F", "S", "S").forEachIndexed { idx, day ->
                        val isDone = idx < 6
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(day, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(if (isDone) Color(0xFF10B981) else MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isDone) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            }
        }

        "honeycomb_tile" -> {
            var selectedShape by remember { mutableStateOf("Hexagon") }
            var isTileDone by remember { mutableStateOf(false) }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("Interactive Honeycomb Quick Reminder Tile Preview:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Hexagon" to "⬡ Hexagon", "Circle" to "○ Circle", "Square" to "□ Square").forEach { (shapeKey, label) ->
                        FilterChip(
                            selected = selectedShape == shapeKey,
                            onClick = { selectedShape = shapeKey },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                val tileShape = when (selectedShape) {
                    "Circle" -> CircleShape
                    "Square" -> RoundedCornerShape(12.dp)
                    else -> RoundedCornerShape(18.dp)
                }
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(tileShape)
                        .background(if (isTileDone) Color(0xFF10B981) else MaterialTheme.colorScheme.primaryContainer)
                        .clickable { isTileDone = !isTileDone },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (isTileDone) "✅" else "⚡", fontSize = 28.sp)
                        Text(if (isTileDone) "Done" else "Deep Work", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isTileDone) Color.White else MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text("Tap tile to toggle completion state", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        "numeric_unit_input" -> {
            var numericValue by remember { mutableStateOf("2500") }
            var selectedUnit by remember { mutableStateOf("ml") }
            Column {
                Text("Numeric input with stepper buttons & unit conversion:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalIconButton(onClick = {
                        val curr = numericValue.toIntOrNull() ?: 0
                        numericValue = (curr - 250).coerceAtLeast(0).toString()
                    }) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease")
                    }
                    OutlinedTextField(
                        value = numericValue,
                        onValueChange = { numericValue = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Quantity ($selectedUnit)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    FilledTonalIconButton(onClick = {
                        val curr = numericValue.toIntOrNull() ?: 0
                        numericValue = (curr + 250).toString()
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Increase")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("ml", "L", "oz").forEach { unit ->
                        FilterChip(
                            selected = selectedUnit == unit,
                            onClick = { selectedUnit = unit },
                            label = { Text(unit) }
                        )
                    }
                }
            }
        }

        "medication_schedule_selector" -> {
            var mealRel by remember { mutableStateOf("Before Meal") }
            var mealPeriod by remember { mutableStateOf("Breakfast") }
            Column {
                Text("Meal relationship & medication schedule slot selector:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Text("1. Relationship to Food:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Before Meal", "With Meal", "After Meal").forEach { rel ->
                        FilterChip(
                            selected = mealRel == rel,
                            onClick = { mealRel = rel },
                            label = { Text(rel, fontSize = 11.sp) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text("2. Routine Period Slot:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Breakfast (08:00)", "Lunch (13:00)", "Dinner (20:00)", "Bedtime (22:30)").forEach { period ->
                        FilterChip(
                            selected = mealPeriod == period,
                            onClick = { mealPeriod = period },
                            label = { Text(period, fontSize = 11.sp) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), modifier = Modifier.fillMaxWidth()) {
                    Text("💊 Schedule: Take 1 capsule $mealRel at $mealPeriod", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(10.dp))
                }
            }
        }

        "recurrence_rule_selector" -> {
            var freq by remember { mutableStateOf("Weekly") }
            var selectedDays by remember { mutableStateOf(setOf("Mon", "Wed", "Fri")) }
            Column {
                Text("Recurrence rule selector with interactive weekday bitmask:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Daily", "Weekly", "Monthly").forEach { f ->
                        FilterChip(selected = freq == f, onClick = { freq = f }, label = { Text(f, fontSize = 12.sp) })
                    }
                }
                if (freq == "Weekly") {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Repeat On Weekdays:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { day ->
                            val active = selectedDays.contains(day)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        selectedDays = if (active) selectedDays - day else selectedDays + day
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(day.take(1), fontWeight = FontWeight.Bold, color = if (active) Color.White else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Rule summary: Repeats $freq on ${selectedDays.sorted().joinToString(", ")}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
            }
        }

        "notification_reminder_config" -> {
            var isP1Alarm by remember { mutableStateOf(true) }
            var snoozeMins by remember { mutableIntStateOf(10) }
            Column {
                Text("Notification & smart alarm reminder configuration:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("P1 Critical Smart Alarm", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Overrides Do-Not-Disturb & quiet hours", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = isP1Alarm, onCheckedChange = { isP1Alarm = it })
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text("Snooze Interval:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 10, 15, 20).forEach { mins ->
                        FilterChip(
                            selected = snoozeMins == mins,
                            onClick = { snoozeMins = mins },
                            label = { Text("$mins min") }
                        )
                    }
                }
            }
        }

        "loading_skeleton" -> {
            Column {
                Text("Cold-start loading skeleton preview (Zero text flash):", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(10.dp))
                repeat(2) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Box(modifier = Modifier.fillMaxWidth(0.6f).height(12.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceVariant))
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(modifier = Modifier.fillMaxWidth(0.4f).height(10.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)))
                            }
                        }
                    }
                }
            }
        }

        "feedback_snackbar" -> {
            var showSnackbar by remember { mutableStateOf(false) }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("Task completion snackbar with instant undo action:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(10.dp))
                Button(onClick = { showSnackbar = true }) {
                    Text("Trigger Feedback Toast")
                }
                Spacer(modifier = Modifier.height(12.dp))
                AnimatedVisibility(visible = showSnackbar) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.inverseSurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Task completed! (+10 XP)", color = MaterialTheme.colorScheme.inverseOnSurface, fontSize = 12.sp)
                            TextButton(onClick = { showSnackbar = false }) {
                                Text("UNDO", color = MaterialTheme.colorScheme.inversePrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        "progress_indicators" -> {
            var progressVal by remember { mutableStateOf(0.70f) }
            Column {
                Text("Dynamic linear and circular progress indicators:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Progress: ${(progressVal * 100).toInt()}%", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    CircularProgressIndicator(progress = { progressVal }, modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(progress = { progressVal }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)))
                Spacer(modifier = Modifier.height(8.dp))
                Slider(value = progressVal, onValueChange = { progressVal = it }, valueRange = 0f..1f)
            }
        }

        "settings_navigation_card" -> {
            var clickCount by remember { mutableIntStateOf(0) }
            Column {
                Text("Settings navigation card with highlighted container and chevron:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().clickable { clickCount++ }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(36.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Appearance & Themes", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("10 curated themes, dark mode, animations", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text("Tapped: $clickCount times", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
            }
        }

        "bottom_sheet_container" -> {
            var showInnerSheet by remember { mutableStateOf(false) }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("Standardized ModalBottomSheet editor container:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(10.dp))
                Button(onClick = { showInnerSheet = true }) {
                    Text("Trigger Demo Bottom Sheet")
                }
                if (showInnerSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { showInnerSheet = false },
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
                            Text("Sample Modal Bottom Sheet", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Keyboard-safe container with drag handle and responsive form inputs.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(onClick = { showInnerSheet = false }, modifier = Modifier.fillMaxWidth()) {
                                Text("Dismiss Sheet")
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }

        "expandable_card" -> {
            var isCardExpanded by remember { mutableStateOf(false) }
            Column {
                Text("Progressive disclosure accordion card:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth().clickable { isCardExpanded = !isCardExpanded }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Advanced Recurrence & Sound Rules", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Icon(
                                imageVector = if (isCardExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        AnimatedVisibility(visible = isCardExpanded) {
                            Column(modifier = Modifier.padding(top = 10.dp)) {
                                Text("• Subtask dependencies enabled", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("• Auto-reschedule overdue occurrences to next weekday", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("• Custom Tibetan Bell completion audio assigned", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        "toggle_switch_row" -> {
            var switchA by remember { mutableStateOf(true) }
            var switchB by remember { mutableStateOf(false) }
            Column {
                Text("Standardized setting toggle switch row:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Completion Haptic Vibration", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Tactile feedback on task completion", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = switchA, onCheckedChange = { switchA = it })
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Quiet Hours Mute", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Silence non-critical alerts from 22:00 to 07:00", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = switchB, onCheckedChange = { switchB = it })
                }
            }
        }

        "validation_error_message" -> {
            var textInput by remember { mutableStateOf("") }
            val hasError = textInput.isBlank()
            Column {
                Text("Input validation callout with real-time error state:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    label = { Text("Task Title *") },
                    isError = hasError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (hasError) {
                    Row(modifier = Modifier.padding(top = 4.dp, start = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Title cannot be blank. Enter at least 1 character.", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                    }
                }
            }
        }

        "sound_preview_selector" -> {
            val sounds = listOf("Tibetan Bell" to "🔔", "Crisp Pop" to "🫧", "Celestial Harp" to "🎵", "Victory Marimba" to "🎶")
            var activeSound by remember { mutableStateOf("Tibetan Bell") }
            var isPlaying by remember { mutableStateOf(false) }
            Column {
                Text("Sound effect selector with audition play button:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    sounds.forEach { (name, emoji) ->
                        FilterChip(
                            selected = activeSound == name,
                            onClick = { activeSound = name },
                            label = { Text("$emoji $name", fontSize = 12.sp) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Selected: $activeSound", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    FilledTonalButton(onClick = { isPlaying = !isPlaying }) {
                        Icon(if (isPlaying) Icons.Default.VolumeUp else Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isPlaying) "Playing..." else "Audition", fontSize = 11.sp)
                    }
                }
            }
        }

        "calendar_day_indicator" -> {
            var selectedDayIndex by remember { mutableIntStateOf(3) }
            val days = listOf("24", "25", "26", "27", "28", "29", "30")
            val weekdays = listOf("M", "T", "W", "T", "F", "S", "S")
            Column {
                Text("Calendar day indicator strip with dot recurrence markers:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    days.forEachIndexed { idx, dayNum ->
                        val isSelected = selectedDayIndex == idx
                        val isToday = idx == 3
                        val hasTasks = idx % 2 == 1
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .clickable { selectedDayIndex = idx }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(weekdays[idx], fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(dayNum, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (isToday) Color.White else MaterialTheme.colorScheme.onSurface)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            if (hasTasks) {
                                Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(Color(0xFFEF4444)))
                            } else {
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }
        }

        "file_picker_field" -> {
            var chosenFile by remember { mutableStateOf("lifeos_backup_20261028.json") }
            Column {
                Text("Storage Access Framework (SAF) file selector trigger:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.AttachFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(chosenFile, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("48.6 KB • Application/JSON", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        OutlinedButton(onClick = { chosenFile = "custom_bell_sound.wav" }) {
                            Text("Browse", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        "section_header" -> {
            Column {
                Text("Prominent uppercase section header with badge & action:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "SCHEDULE & REMINDERS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                            Text("4 Active", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    Text("See All", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        "success_feedback_animation" -> {
            var celebrated by remember { mutableStateOf(false) }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("Milestone celebration motif & XP award feedback:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(10.dp))
                Button(onClick = { celebrated = !celebrated }) {
                    Text(if (celebrated) "Reset Animation" else "Trigger Celebration Win 🎉")
                }
                Spacer(modifier = Modifier.height(12.dp))
                AnimatedVisibility(visible = celebrated) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🏆 VICTORY ACHIEVED!", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("+75 XP Earned • Level 4 Discipline Unlocked", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                        }
                    }
                }
            }
        }

        "backup_restore_dialog" -> {
            Column {
                Text("Safe offline backup & restore status card:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("100% Offline Encrypted Backup", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Zero network telemetry. All database records export into a secure JSON archive on local device storage.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {}, modifier = Modifier.weight(1f)) {
                                Text("Export Backup", fontSize = 11.sp)
                            }
                            OutlinedButton(onClick = {}, modifier = Modifier.weight(1f)) {
                                Text("Restore Backup", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        "premium_card", "premium_button", "glass_theme_panel", "animation_speed_system" -> {
            RenderPremiumThemeLivePreview(componentId)
        }

        "honeycomb_profile_selector", "profile_tile" -> {
            RenderProfileLivePreview(componentId)
        }

        "app_entry_profile_selector", "onboarding_survey_screen", "survey_time_picker", "survey_water_goal", "survey_discovery_selector" -> {
            RenderSurveyAndEntryLivePreview(componentId)
        }

        else -> {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Interactive Demonstration", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "This component is integrated into the active LifeOS architecture. See the Developer Reference below for source file paths, parameter signatures, and copyable integration code.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RenderPremiumThemeLivePreview(componentId: String) {
    when (componentId) {
        "premium_card" -> {
            var selectedStyle by remember { mutableStateOf(PremiumCardStyle.THEME_BESPOKE) }
            var isCardSelected by remember { mutableStateOf(false) }
            var isCardCompleted by remember { mutableStateOf(false) }
            var isExpanded by remember { mutableStateOf(false) }

            Column {
                Text("Select Style Tier & Interactive Flags:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PremiumCardStyle.values().forEach { style ->
                        FilterChip(
                            selected = selectedStyle == style,
                            onClick = { selectedStyle = style },
                            label = { Text(style.name, fontSize = 11.sp) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isCardSelected,
                        onClick = { isCardSelected = !isCardSelected },
                        label = { Text("Selected", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = isCardCompleted,
                        onClick = { isCardCompleted = !isCardCompleted },
                        label = { Text("Completed", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = isExpanded,
                        onClick = { isExpanded = !isExpanded },
                        label = { Text("Expanded", fontSize = 11.sp) }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                PremiumCard(
                    headerTitle = "Interactive Premium Card",
                    headerEmoji = "✨",
                    badgeText = "Live Preview",
                    style = selectedStyle,
                    isSelected = isCardSelected,
                    isCompleted = isCardCompleted,
                    onClick = { isExpanded = !isExpanded }
                ) {
                    Text(
                        text = "Tap to toggle expand/collapse! Features tactile spring press feedback, animated border transitions, and theme-bespoke surfaces.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isExpanded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Expanded content pane with dynamic spring physics and theme-aware typography tokens.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        "premium_button" -> {
            var isButtonLoading by remember { mutableStateOf(false) }
            var isButtonSuccess by remember { mutableStateOf(false) }
            val coroutineScope = rememberCoroutineScope()

            Column {
                Text("Interactive Tactile Spring Bounce & Feedback Transitions:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(10.dp))
                PremiumButton(
                    text = "Tap for Micro-Spinner & Success",
                    style = PremiumButtonStyle.PRIMARY,
                    isLoading = isButtonLoading,
                    isSuccess = isButtonSuccess,
                    onClick = {
                        isButtonLoading = true
                        coroutineScope.launch {
                            kotlinx.coroutines.delay(1200)
                            isButtonLoading = false
                            isButtonSuccess = true
                            kotlinx.coroutines.delay(1500)
                            isButtonSuccess = false
                        }
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PremiumButton(
                        text = "Outlined",
                        style = PremiumButtonStyle.OUTLINED,
                        onClick = {},
                        modifier = Modifier.weight(1f)
                    )
                    PremiumButton(
                        text = "Glass",
                        style = PremiumButtonStyle.GLASS,
                        onClick = {},
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PremiumButton(
                        text = "Destructive",
                        style = PremiumButtonStyle.DESTRUCTIVE,
                        onClick = {},
                        modifier = Modifier.weight(1f)
                    )
                    PremiumButton(
                        text = "Disabled",
                        style = PremiumButtonStyle.SECONDARY,
                        enabled = false,
                        onClick = {},
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        "glass_theme_panel" -> {
            Column {
                Text("Frosted Glass Surface with Specular Highlights & Prism Glyph:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(10.dp))
                PremiumCard(
                    style = PremiumCardStyle.GLASS,
                    headerTitle = "Glassmorphic Focus Pane",
                    headerEmoji = "💎",
                    badgeText = "Frosted Specular"
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Layered translucent gradient background (alpha 0.55 - 0.75) with specular white highlight border outline.",
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        ThemedLoadingIndicator(size = 48.dp)
                    }
                }
            }
        }

        "animation_speed_system" -> {
            var previewSpeed by remember { mutableStateOf(AnimationSpeed.NORMAL) }

            Column {
                Text("Select Speed Multiplier to observe real-time velocity changes:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        AnimationSpeed.OFF to "Off (0×)",
                        AnimationSpeed.SLOW to "0.5×",
                        AnimationSpeed.NORMAL to "1.0×",
                        AnimationSpeed.FAST to "1.5×",
                        AnimationSpeed.VERY_FAST to "2.0×"
                    ).forEach { (speedTier, label) ->
                        FilterChip(
                            selected = previewSpeed == speedTier,
                            onClick = { previewSpeed = speedTier },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                CompositionLocalProvider(LocalLifeAnimationSpeed provides previewSpeed) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Current Velocity: ${previewSpeed.multiplier}×",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = if (previewSpeed == AnimationSpeed.OFF) "Static non-animated state" else "Duration: ${previewSpeed.calculateDuration(1000)}ms",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            ThemedLoadingIndicator(size = 36.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RenderProfileLivePreview(componentId: String) {
    when (componentId) {
        "honeycomb_profile_selector" -> {
            var selectedId by remember { mutableStateOf(1L) }
            val sampleProfiles = remember {
                listOf(
                    com.example.data.local.entities.ProfileEntity.createDefaultPersonal(),
                    com.example.data.local.entities.ProfileEntity(id = 2, name = "Work", emoji = "💼", colorHex = 0xFF8B5CF6, type = com.example.data.local.entities.ProfileEntity.TYPE_WORK, isDefault = false),
                    com.example.data.local.entities.ProfileEntity(id = 3, name = "Startup", emoji = "🚀", colorHex = 0xFF10B981, type = com.example.data.local.entities.ProfileEntity.TYPE_CUSTOM, isDefault = false)
                )
            }
            Column {
                Text("Tap workspace tiles to switch active profile reactively:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                com.example.ui.components.HoneycombProfileSelector(
                    profiles = sampleProfiles,
                    activeProfileId = selectedId,
                    onProfileSelected = { selectedId = it },
                    onAddProfileClick = { /* demo */ }
                )
            }
        }

        "profile_tile" -> {
            var activeState by remember { mutableStateOf(true) }
            val sampleProf = remember {
                com.example.data.local.entities.ProfileEntity(id = 2, name = "Engineering", emoji = "💻", colorHex = 0xFF3B82F6, type = com.example.data.local.entities.ProfileEntity.TYPE_WORK, isDefault = false)
            }
            Column {
                Text("Interactive Honeycomb Profile Tile (tap to toggle active state):", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                com.example.ui.components.HoneycombProfileTile(
                    profile = sampleProf,
                    isActive = activeState,
                    onClick = { activeState = !activeState }
                )
            }
        }
    }
}

@Composable
private fun RenderSurveyAndEntryLivePreview(componentId: String) {
    when (componentId) {
        "app_entry_profile_selector" -> {
            var selectedId by remember { mutableStateOf(1L) }
            val sampleProfiles = remember {
                listOf(
                    com.example.data.local.entities.ProfileEntity.createDefaultPersonal(),
                    com.example.data.local.entities.ProfileEntity(id = 2, name = "Work", emoji = "💼", colorHex = 0xFF8B5CF6, type = com.example.data.local.entities.ProfileEntity.TYPE_WORK, isDefault = false)
                )
            }
            Column {
                Text("App-Entry Honeycomb Profile Selection Live Preview:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    sampleProfiles.forEach { profile ->
                        com.example.ui.screens.profile.HoneycombEntryProfileCard(
                            profile = profile,
                            isSelected = profile.id == selectedId,
                            isPersistedActive = profile.id == 1L,
                            onClick = { selectedId = profile.id },
                            onDoubleClickOrEnter = { selectedId = profile.id }
                        )
                    }
                }
            }
        }

        "onboarding_survey_screen" -> {
            var nameState by remember { mutableStateOf("Sam") }
            Column {
                Text("Onboarding Survey Form Preview (Display Name Step):", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(10.dp))
                com.example.ui.screens.onboarding.DisplayNameStep(
                    displayName = nameState,
                    onDisplayNameChange = { nameState = it }
                )
            }
        }

        "survey_time_picker" -> {
            var sleepTime by remember { mutableStateOf("22:30") }
            Column {
                Text("Sleeping Time Picker Preview:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(10.dp))
                com.example.ui.screens.onboarding.SleepingTimeStep(
                    sleepingTime = sleepTime,
                    onSleepingTimeSelected = { sleepTime = it }
                )
            }
        }

        "survey_water_goal" -> {
            var waterText by remember { mutableStateOf("2500") }
            Column {
                Text("Hydration Target Input & Stepper:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(10.dp))
                com.example.ui.screens.onboarding.WaterGoalStep(
                    waterGoalText = waterText,
                    onWaterGoalChange = { waterText = it },
                    errorMessage = null
                )
            }
        }

        "survey_discovery_selector" -> {
            var source by remember { mutableStateOf("Social Media") }
            var platform by remember { mutableStateOf("Reddit") }
            var detail by remember { mutableStateOf("") }
            Column {
                Text("Discovery Source Attribution Selector:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(10.dp))
                com.example.ui.screens.onboarding.DiscoverySourceStep(
                    selectedSource = source,
                    onSourceSelected = { source = it },
                    selectedPlatform = platform,
                    onPlatformSelected = { platform = it },
                    otherDetail = detail,
                    onDetailChange = { detail = it }
                )
            }
        }

        "task_emoji_suggester" -> {
            var taskTitle by remember { mutableStateOf("Buy groceries and milk") }
            var chosenEmoji by remember { mutableStateOf("🛒") }
            val suggestions = remember(taskTitle) {
                TaskEmojiSuggestionEngine.suggestEmojis(taskTitle)
            }

            Column {
                Text("Interactive Title-based Emoji Suggester:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.material3.OutlinedTextField(
                    value = taskTitle,
                    onValueChange = { taskTitle = it },
                    label = { Text("Task Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                EmojiSuggestionRow(
                    suggestions = suggestions,
                    currentEmoji = chosenEmoji,
                    onEmojiSelected = { chosenEmoji = it }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Selected Emoji: $chosenEmoji  |  Matches: ${suggestions.joinToString(" ")}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }

        "multi_label_checkbox_selector" -> {
            val sampleLabels = remember {
                listOf(
                    LabelItem(id = 1L, name = "Urgent", colorId = "Berry Red"),
                    LabelItem(id = 2L, name = "Health", colorId = "Green"),
                    LabelItem(id = 3L, name = "Work", colorId = "Blue"),
                    LabelItem(id = 4L, name = "Finance", colorId = "Yellow")
                )
            }
            var selectedIds by remember { mutableStateOf(setOf(1L, 2L)) }
            var showSheet by remember { mutableStateOf(false) }

            Column {
                Text("Multi-Label Checkbox Selection & Chips:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                SelectedLabelChips(
                    selectedLabels = sampleLabels.filter { selectedIds.contains(it.id) },
                    onRemoveLabel = { id -> selectedIds = selectedIds - id }
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(onClick = { showSheet = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Select Labels (${selectedIds.size})")
                }
                if (showSheet) {
                    MultiLabelSelectorBottomSheet(
                        availableLabels = sampleLabels,
                        selectedLabelIds = selectedIds,
                        onToggleLabel = { id ->
                            selectedIds = if (selectedIds.contains(id)) selectedIds - id else selectedIds + id
                        },
                        onCreateNewLabel = { showSheet = false },
                        onDismiss = { showSheet = false }
                    )
                }
            }
        }

        "medication_priority_timing_control" -> {
            var isMedEnabled by remember { mutableStateOf(true) }
            Column {
                Text("Medication Priority Defaults & Timing Controls:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                MedicineReminderSection(
                    isMedicineEnabled = isMedEnabled,
                    onMedicineEnabledChange = { isMedEnabled = it },
                    taskTitle = "Amoxicillin 500mg",
                    wakeUpTime = "06:30",
                    bedTime = "22:30",
                    onMedicationDraftChanged = {}
                )
            }
        }

        "medication_detail_card" -> {
            val sampleMed = remember {
                MedicationItem(
                    id = 101L,
                    taskId = 1L,
                    name = "Amoxicillin",
                    form = com.example.domain.medication.MedicationForm.CAPSULE,
                    dosageAmount = "1",
                    dosageUnit = "Capsule",
                    strength = "500mg",
                    instructions = "Take after breakfast with a full glass of water",
                    scheduleConfig = MedicationScheduleConfig(
                        scheduleType = com.example.domain.medication.MedicineScheduleType.MEAL_BASED,
                        courseDurationDays = 7
                    )
                )
            }
            val sampleDoses = remember {
                listOf(
                    MedicationDoseItem(
                        id = 1L,
                        medicationId = 101L,
                        taskId = 1L,
                        medicationName = "Amoxicillin",
                        dosage = "1 Capsule (500mg)",
                        scheduledDateIso = com.example.utils.DateUtils.todayString(),
                        scheduledTime = "08:30",
                        status = DoseStatus.TAKEN,
                        actionTimestamp = System.currentTimeMillis() - 7200000
                    ),
                    MedicationDoseItem(
                        id = 2L,
                        medicationId = 101L,
                        taskId = 1L,
                        medicationName = "Amoxicillin",
                        dosage = "1 Capsule (500mg)",
                        scheduledDateIso = com.example.utils.DateUtils.todayString(),
                        scheduledTime = "13:30",
                        status = DoseStatus.SCHEDULED
                    )
                )
            }
            var showDetailsSheet by remember { mutableStateOf(false) }

            Column {
                Text("Medication Course Card (Tap for Details Sheet):", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                MedicationCourseCard(
                    medication = sampleMed,
                    onTogglePause = {},
                    onLogPrnDose = {},
                    onClick = { showDetailsSheet = true }
                )
                if (showDetailsSheet) {
                    MedicationDetailsBottomSheet(
                        medication = sampleMed,
                        doses = sampleDoses,
                        onDismiss = { showDetailsSheet = false },
                        onMarkTaken = {},
                        onSkip = {},
                        onTogglePause = {},
                        onLogPrnDose = {}
                    )
                }
            }
        }

        "calendar_deadline_range_marker" -> {
            val today = com.example.utils.DateUtils.todayString()
            val monthDates = remember(today) {
                (-5..15).map { com.example.utils.DateUtils.addDays(today, it) }
            }
            var selectedDate by remember { mutableStateOf(today) }
            val rangeStart = remember(today) { com.example.utils.DateUtils.addDays(today, 1) }
            val rangeEnd = remember(today) { com.example.utils.DateUtils.addDays(today, 5) }
            val datesWithTasks = remember(today) { setOf(today, com.example.utils.DateUtils.addDays(today, 2), com.example.utils.DateUtils.addDays(today, 6)) }
            val datesWithRecurring = remember(today) { setOf(com.example.utils.DateUtils.addDays(today, 2), com.example.utils.DateUtils.addDays(today, 6)) }

            Column {
                Text("Dotted Range Boundaries & Recurrence Indicators:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Range: $rangeStart → $rangeEnd (Intermediate dotted circles)", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                MonthGridView(
                    monthDates = monthDates,
                    selectedDate = selectedDate,
                    today = today,
                    monthIndex = com.example.utils.DateUtils.getMonthZeroIndexed(today),
                    datesWithTasks = datesWithTasks,
                    datesWithRecurringTasks = datesWithRecurring,
                    rangeStartDate = rangeStart,
                    rangeEndDate = rangeEnd,
                    onSelectDate = { selectedDate = it }
                )
            }
        }

        "planning_boards" -> {
            var demoTab by remember { mutableStateOf(com.example.ui.screens.planning.PlanningBoardTab.DAILY) }
            var demoDate by remember { mutableStateOf(com.example.utils.DateUtils.todayString()) }

            Column {
                Text("Interactive Horizon Navigator:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                com.example.ui.screens.planning.PlanningPeriodNavigator(
                    activeTab = demoTab,
                    selectedDateIso = demoDate,
                    todayIso = com.example.utils.DateUtils.todayString(),
                    onPrevious = { demoDate = com.example.utils.DateUtils.addDays(demoDate, -1) },
                    onNext = { demoDate = com.example.utils.DateUtils.addDays(demoDate, 1) },
                    onJumpToCurrent = { demoDate = com.example.utils.DateUtils.todayString() }
                )
            }
        }

        "monthly_window_recurrence" -> {
            val sampleWindowTask = remember {
                com.example.domain.model.TaskItem(
                    title = "Monthly Rent & Utility Bill",
                    dueDateIso = "2026-11-25",
                    recurrenceType = com.example.domain.model.RecurrenceType.MONTHLY_WINDOW,
                    recurrenceWindowStartDay = 25,
                    recurrenceWindowEndDay = 30
                )
            }
            val preview = remember(sampleWindowTask) {
                com.example.domain.recurrence.RecurrenceEngine.formatRecurrencePreview(sampleWindowTask)
            }

            Column {
                Text("Flexible Monthly Recurrence Window:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(sampleWindowTask.title, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(preview, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Window: 25th–30th of each month (1 obligation/month, satisfies on any day)", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }

        "advanced_project_workspace" -> {
            Column {
                Text("Advanced Project Database Views Preview:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📁 Embedded Systems", fontWeight = FontWeight.Bold)
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text("Table • Board • Calendar • List • Gallery", fontSize = 10.sp, modifier = Modifier.padding(4.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF10B981).copy(alpha = 0.2f)) {
                                Text("✓ SPI Driver [DONE]", fontSize = 11.sp, color = Color(0xFF059669), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                            Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF3B82F6).copy(alpha = 0.2f)) {
                                Text("⚙ DMA Buffers [IN PROGRESS]", fontSize = 11.sp, color = Color(0xFF2563EB), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 1.sp
    )
    Spacer(modifier = Modifier.height(4.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReferenceRow(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurface)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusBadge(status: ComponentStatus) {
    val (bgColor, textColor) = when (status) {
        ComponentStatus.EXISTING -> Color(0xFF10B981).copy(alpha = 0.15f) to Color(0xFF10B981)
        ComponentStatus.PARTIAL -> Color(0xFFF59E0B).copy(alpha = 0.15f) to Color(0xFFF59E0B)
        ComponentStatus.PROPOSED -> Color(0xFF3B82F6).copy(alpha = 0.15f) to Color(0xFF3B82F6)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, textColor.copy(alpha = 0.4f))
    ) {
        Text(
            text = status.label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
