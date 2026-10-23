package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.medication.AlertStyle
import com.example.domain.medication.FoodRelationship
import com.example.domain.medication.IntervalModel
import com.example.domain.medication.MealPeriod
import com.example.domain.medication.MealPeriodConfig
import com.example.domain.medication.MedicationForm
import com.example.domain.medication.MedicationItem
import com.example.domain.medication.MedicationScheduleConfig
import com.example.domain.medication.MedicationScheduleEngine
import com.example.domain.medication.MedicineScheduleType
import com.example.utils.DateUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MedicineReminderSection(
    isMedicineEnabled: Boolean,
    onMedicineEnabledChange: (Boolean) -> Unit,
    taskTitle: String,
    initialMedication: MedicationItem? = null,
    onMedicationDraftChanged: (MedicationItem?) -> Unit,
    modifier: Modifier = Modifier
) {
    // Medication Fields
    var medName by remember(taskTitle) {
        mutableStateOf(initialMedication?.name ?: taskTitle.ifBlank { "" })
    }
    var selectedForm by remember {
        mutableStateOf(initialMedication?.form ?: MedicationForm.TABLET)
    }
    var dosageAmount by remember {
        mutableStateOf(initialMedication?.dosageAmount ?: "1")
    }
    var dosageUnit by remember(selectedForm) {
        mutableStateOf(initialMedication?.dosageUnit ?: selectedForm.defaultUnit)
    }
    var strength by remember {
        mutableStateOf(initialMedication?.strength ?: "")
    }
    var instructions by remember {
        mutableStateOf(initialMedication?.instructions ?: "")
    }

    // Schedule Mode
    var scheduleType by remember {
        mutableStateOf(initialMedication?.scheduleConfig?.scheduleType ?: MedicineScheduleType.MEAL_BASED)
    }

    // Meal-based state
    var mealConfigs by remember {
        mutableStateOf(
            if (initialMedication?.scheduleConfig?.mealPeriods?.isNotEmpty() == true) {
                initialMedication.scheduleConfig.mealPeriods
            } else {
                listOf(
                    MealPeriodConfig(MealPeriod.MORNING, "08:00", FoodRelationship.BEFORE_FOOD),
                    MealPeriodConfig(MealPeriod.LUNCH, "13:00", FoodRelationship.AFTER_FOOD),
                    MealPeriodConfig(MealPeriod.EVENING, "17:30", FoodRelationship.WITH_FOOD, isEnabled = false),
                    MealPeriodConfig(MealPeriod.DINNER, "20:30", FoodRelationship.AFTER_FOOD)
                )
            }
        )
    }

    // Fixed Times state
    var fixedTimes by remember {
        mutableStateOf(initialMedication?.scheduleConfig?.fixedTimes ?: listOf("08:00", "14:00", "20:00"))
    }
    var newTimeInput by remember { mutableStateOf("12:00") }

    // Repeating Intervals state
    var intervalHours by remember {
        mutableIntStateOf(initialMedication?.scheduleConfig?.intervalHours ?: 4)
    }
    var intervalModel by remember {
        mutableStateOf(initialMedication?.scheduleConfig?.intervalModel ?: IntervalModel.FIXED_SCHEDULE)
    }
    var intervalAnchorTime by remember {
        mutableStateOf(initialMedication?.scheduleConfig?.intervalAnchorTime ?: "08:00")
    }
    var allowOvernight by remember {
        mutableStateOf(initialMedication?.scheduleConfig?.allowOvernight ?: false)
    }

    // Weekdays state
    var selectedDays by remember {
        mutableStateOf(initialMedication?.scheduleConfig?.selectedDaysOfWeek ?: listOf("MONDAY", "WEDNESDAY", "FRIDAY"))
    }

    // Course Duration state
    var startDateIso by remember {
        mutableStateOf(initialMedication?.scheduleConfig?.startDateIso ?: DateUtils.todayString())
    }
    var endDateIso by remember {
        mutableStateOf(initialMedication?.scheduleConfig?.endDateIso)
    }
    var courseDurationDays by remember {
        mutableStateOf(initialMedication?.scheduleConfig?.courseDurationDays?.toString() ?: "")
    }
    var isAsNeeded by remember {
        mutableStateOf(initialMedication?.scheduleConfig?.isAsNeeded ?: false)
    }

    // Notification & Alert Preferences
    var alertStyle by remember {
        mutableStateOf(initialMedication?.alertStyle ?: AlertStyle.STANDARD)
    }
    var soundChoice by remember {
        mutableStateOf(initialMedication?.reminderSound ?: "CHIME")
    }
    var snoozeMinutes by remember {
        mutableIntStateOf(initialMedication?.snoozeMinutes ?: 10)
    }
    var followUpEnabled by remember {
        mutableStateOf(initialMedication?.followUpEnabled ?: true)
    }
    var maxFollowUps by remember {
        mutableIntStateOf(initialMedication?.maxFollowUps ?: 2)
    }
    var privacySafe by remember {
        mutableStateOf(initialMedication?.privacySafeNotification ?: true)
    }

    var showFormDropdown by remember { mutableStateOf(false) }

    // Derived Schedule Config & Preview
    val currentScheduleConfig by remember {
        derivedStateOf {
            MedicationScheduleConfig(
                scheduleType = scheduleType,
                mealPeriods = mealConfigs,
                fixedTimes = fixedTimes,
                intervalHours = intervalHours,
                intervalModel = intervalModel,
                intervalAnchorTime = intervalAnchorTime,
                allowOvernight = allowOvernight,
                selectedDaysOfWeek = selectedDays,
                startDateIso = startDateIso,
                endDateIso = endDateIso,
                courseDurationDays = courseDurationDays.toIntOrNull(),
                isAsNeeded = isAsNeeded
            )
        }
    }

    val scheduleSummaryText by remember {
        derivedStateOf {
            MedicationScheduleEngine.formatScheduleSummary(currentScheduleConfig)
        }
    }

    // Sync draft up to parent
    val currentMedicationDraft by remember {
        derivedStateOf {
            if (!isMedicineEnabled) null
            else MedicationItem(
                id = initialMedication?.id ?: 0L,
                taskId = initialMedication?.taskId ?: 0L,
                name = medName.ifBlank { taskTitle.ifBlank { "Prescribed Medication" } },
                form = selectedForm,
                dosageAmount = dosageAmount.ifBlank { "1" },
                dosageUnit = dosageUnit.ifBlank { selectedForm.defaultUnit },
                strength = strength.trim(),
                instructions = instructions.trim(),
                scheduleConfig = currentScheduleConfig,
                reminderSound = soundChoice,
                alertStyle = alertStyle,
                snoozeMinutes = snoozeMinutes,
                followUpEnabled = followUpEnabled,
                followUpIntervalMinutes = 15,
                maxFollowUps = maxFollowUps,
                privacySafeNotification = privacySafe
            )
        }
    }

    androidx.compose.runtime.LaunchedEffect(currentMedicationDraft, isMedicineEnabled) {
        onMedicationDraftChanged(if (isMedicineEnabled) currentMedicationDraft else null)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Toggle Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isMedicineEnabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ),
            border = if (isMedicineEnabled) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onMedicineEnabledChange(!isMedicineEnabled) }
                .testTag("medicine_reminder_toggle_card")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "💊", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Medicine Reminder",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isMedicineEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                            ) {
                                Text(
                                    text = "FREE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Enable medication scheduling and dose tracking.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = isMedicineEnabled,
                    onCheckedChange = onMedicineEnabledChange,
                    modifier = Modifier.testTag("medicine_reminder_switch")
                )
            }
        }

        // Expanded Medication Fields (Progressive Disclosure)
        AnimatedVisibility(
            visible = isMedicineEnabled,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(14.dp)
            ) {
                // Section 1: Medication Name & Form
                Text(
                    text = "1. Medication Details",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = medName,
                    onValueChange = { medName = it },
                    label = { Text("Medication Name (e.g. Amoxicillin, Insulin)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("med_name_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Form & Unit Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1.2f)) {
                        OutlinedTextField(
                            value = "${selectedForm.emoji} ${selectedForm.displayName}",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Form") },
                            trailingIcon = {
                                TextButton(onClick = { showFormDropdown = true }) {
                                    Text("Change", fontSize = 11.sp)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showFormDropdown = true }
                                .testTag("med_form_picker")
                        )

                        DropdownMenu(
                            expanded = showFormDropdown,
                            onDismissRequest = { showFormDropdown = false }
                        ) {
                            MedicationForm.entries.forEach { form ->
                                DropdownMenuItem(
                                    text = { Text("${form.emoji} ${form.displayName}") },
                                    onClick = {
                                        selectedForm = form
                                        dosageUnit = form.defaultUnit
                                        showFormDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = dosageAmount,
                        onValueChange = { dosageAmount = it },
                        label = { Text("Dose") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(0.8f)
                            .testTag("med_dose_amount")
                    )

                    OutlinedTextField(
                        value = dosageUnit,
                        onValueChange = { dosageUnit = it },
                        label = { Text("Unit") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(0.8f)
                            .testTag("med_dose_unit")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Strength & Instructions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = strength,
                        onValueChange = { strength = it },
                        label = { Text("Strength (e.g. 500mg)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = instructions,
                        onValueChange = { instructions = it },
                        label = { Text("Instructions (e.g. with water)") },
                        singleLine = true,
                        modifier = Modifier.weight(1.2f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(10.dp))

                // Section 2: Schedule Mode
                Text(
                    text = "2. Scheduling Mode",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MedicineScheduleType.entries.forEach { mode ->
                        FilterChip(
                            selected = scheduleType == mode,
                            onClick = {
                                scheduleType = mode
                                isAsNeeded = (mode == MedicineScheduleType.AS_NEEDED)
                            },
                            label = { Text(mode.displayName, fontSize = 11.sp) },
                            modifier = Modifier.testTag("med_schedule_mode_${mode.name}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Mode-specific configuration views
                when (scheduleType) {
                    MedicineScheduleType.MEAL_BASED -> {
                        Text(
                            text = "Daily Meal Periods & Food Timing",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        mealConfigs.forEachIndexed { index, config ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(
                                        color = if (config.isEnabled) MaterialTheme.colorScheme.surface
                                        else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Switch(
                                        checked = config.isEnabled,
                                        onCheckedChange = { checked ->
                                            mealConfigs = mealConfigs.toMutableList().also { list ->
                                                list[index] = config.copy(isEnabled = checked)
                                            }
                                        },
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = config.period.displayName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                if (config.isEnabled) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        // Food relationship selector chip
                                        Surface(
                                            onClick = {
                                                val nextRel = when (config.foodRelation) {
                                                    FoodRelationship.BEFORE_FOOD -> FoodRelationship.WITH_FOOD
                                                    FoodRelationship.WITH_FOOD -> FoodRelationship.AFTER_FOOD
                                                    FoodRelationship.AFTER_FOOD -> FoodRelationship.NO_FOOD
                                                    FoodRelationship.NO_FOOD -> FoodRelationship.BEFORE_FOOD
                                                }
                                                mealConfigs = mealConfigs.toMutableList().also {
                                                    it[index] = config.copy(foodRelation = nextRel)
                                                }
                                            },
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                        ) {
                                            Text(
                                                text = config.foodRelation.displayName,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                            )
                                        }

                                        // Time chip
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                text = config.scheduledTime,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    MedicineScheduleType.FIXED_TIMES -> {
                        Text(
                            text = "Specific Daily Times",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            fixedTimes.forEach { time ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 2.dp, bottom = 2.dp)
                                    ) {
                                        Text(text = "⏰ $time", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        IconButton(
                                            onClick = { fixedTimes = fixedTimes - time },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newTimeInput,
                                onValueChange = { newTimeInput = it },
                                label = { Text("Add Time (HH:mm)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    val trimmed = newTimeInput.trim()
                                    if (trimmed.matches(Regex("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$")) && !fixedTimes.contains(trimmed)) {
                                        fixedTimes = (fixedTimes + trimmed).sorted()
                                    }
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add", fontSize = 12.sp)
                            }
                        }
                    }

                    MedicineScheduleType.INTERVAL -> {
                        Text(
                            text = "Repeating Interval Model",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                        ) {
                            listOf(1, 2, 4, 6, 8, 12, 24).forEach { hours ->
                                FilterChip(
                                    selected = intervalHours == hours,
                                    onClick = { intervalHours = hours },
                                    label = { Text("Every ${hours}h", fontSize = 11.sp) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Anchor Start Time", fontSize = 12.sp)
                            OutlinedTextField(
                                value = intervalAnchorTime,
                                onValueChange = { intervalAnchorTime = it },
                                singleLine = true,
                                modifier = Modifier.width(100.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "Include Overnight", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text(text = "Alerts during sleep hours", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = allowOvernight,
                                onCheckedChange = { allowOvernight = it }
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = intervalModel == IntervalModel.FIXED_SCHEDULE,
                                onClick = { intervalModel = IntervalModel.FIXED_SCHEDULE },
                                label = { Text("Fixed from anchor", fontSize = 10.sp) }
                            )
                            FilterChip(
                                selected = intervalModel == IntervalModel.AFTER_CONFIRMED_DOSE,
                                onClick = { intervalModel = IntervalModel.AFTER_CONFIRMED_DOSE },
                                label = { Text("After confirmed dose", fontSize = 10.sp) }
                            )
                        }
                    }

                    MedicineScheduleType.WEEKDAYS -> {
                        Text(
                            text = "Selected Days of Week",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        val days = listOf(
                            "MONDAY" to "Mon",
                            "TUESDAY" to "Tue",
                            "WEDNESDAY" to "Wed",
                            "THURSDAY" to "Thu",
                            "FRIDAY" to "Fri",
                            "SATURDAY" to "Sat",
                            "SUNDAY" to "Sun"
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            days.forEach { (key, label) ->
                                val isSel = selectedDays.contains(key)
                                FilterChip(
                                    selected = isSel,
                                    onClick = {
                                        selectedDays = if (isSel) selectedDays - key else selectedDays + key
                                    },
                                    label = { Text(label, fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    MedicineScheduleType.AS_NEEDED -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "As-needed (PRN) mode logs doses manually without recurring automated alarm interrupts.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(10.dp))

                // Section 3: Smart Alerts, Snooze & Privacy
                Text(
                    text = "3. Smart Alerts & Privacy",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Alert Style Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AlertStyle.entries.forEach { style ->
                        FilterChip(
                            selected = alertStyle == style,
                            onClick = { alertStyle = style },
                            label = { Text(style.name, fontSize = 10.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Snooze & Follow-up Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Snooze Duration", fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(5, 10, 15, 30).forEach { mins ->
                            FilterChip(
                                selected = snoozeMinutes == mins,
                                onClick = { snoozeMinutes = mins },
                                label = { Text("${mins}m", fontSize = 10.sp) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Privacy Safe Notification Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Discreet Lock-Screen Alert", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                        Text(
                            text = "Hides medication name and dosage when device is locked",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = privacySafe,
                        onCheckedChange = { privacySafe = it }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Schedule Live Preview Card
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "📋", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Configured Schedule Preview",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = scheduleSummaryText,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}
