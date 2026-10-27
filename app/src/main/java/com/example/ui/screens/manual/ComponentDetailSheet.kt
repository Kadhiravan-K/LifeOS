package com.example.ui.screens.manual

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

        else -> {
            // Universal clean preview card for other components
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
