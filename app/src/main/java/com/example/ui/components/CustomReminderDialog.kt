package com.example.ui.components

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.SchedulingMode
import com.example.domain.model.TaskReminderItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomReminderDialog(
    schedulingMode: SchedulingMode = SchedulingMode.DURATION,
    onAddReminder: (TaskReminderItem) -> Unit,
    onDismiss: () -> Unit
) {
    // Mode: "PRESET", "CUSTOM_OFFSET", "SPECIFIC_TIME"
    var mode by remember { mutableStateOf("PRESET") }
    var selectedPresetMinutes by remember { mutableIntStateOf(15) }

    var customOffsetAmount by remember { mutableIntStateOf(30) }
    var customOffsetUnit by remember { mutableStateOf("MINUTES") } // MINUTES, HOURS, DAYS
    var showUnitMenu by remember { mutableStateOf(false) }

    var specificTime by remember { mutableStateOf("09:00") }
    var showTimePicker by remember { mutableStateOf(false) }

    var referenceType by remember {
        mutableStateOf(if (schedulingMode == SchedulingMode.DEADLINE) "DEADLINE" else "START_TIME")
    }



    val presets = listOf(
        0 to if (referenceType == "DEADLINE") "At deadline" else "At time of task",
        5 to "5 minutes before",
        10 to "10 minutes before",
        15 to "15 minutes before",
        30 to "30 minutes before",
        60 to "1 hour before",
        120 to "2 hours before",
        1440 to "1 day before"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Alarm, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Reminder", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // If deadline mode, let user choose reference
                if (schedulingMode == SchedulingMode.DEADLINE) {
                    Text("Relative to", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            onClick = { referenceType = "DEADLINE" },
                            shape = RoundedCornerShape(8.dp),
                            color = if (referenceType == "DEADLINE") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Deadline", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Surface(
                            onClick = { referenceType = "START_TIME" },
                            shape = RoundedCornerShape(8.dp),
                            color = if (referenceType == "START_TIME") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Start Time", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Type 1: Preset offsets
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { mode = "PRESET" },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = mode == "PRESET",
                        onClick = { mode = "PRESET" },
                        modifier = Modifier.testTag("reminder_mode_preset")
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Standard Preset", fontWeight = FontWeight.SemiBold)
                }

                if (mode == "PRESET") {
                    Column(modifier = Modifier.padding(start = 32.dp, top = 4.dp)) {
                        presets.forEach { (mins, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedPresetMinutes = mins }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedPresetMinutes == mins,
                                    onClick = { selectedPresetMinutes = mins }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(label, fontSize = 13.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Type 2: Custom offset (e.g. 45 minutes before, 3 days before)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { mode = "CUSTOM_OFFSET" },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = mode == "CUSTOM_OFFSET",
                        onClick = { mode = "CUSTOM_OFFSET" },
                        modifier = Modifier.testTag("reminder_mode_custom_offset")
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Custom Offset", fontWeight = FontWeight.SemiBold)
                }

                if (mode == "CUSTOM_OFFSET") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 32.dp, top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = if (customOffsetAmount > 0) customOffsetAmount.toString() else "",
                            onValueChange = { str ->
                                val n = str.filter { it.isDigit() }.toIntOrNull()
                                if (n != null && n in 1..999) {
                                    customOffsetAmount = n
                                } else if (str.isEmpty()) {
                                    customOffsetAmount = 1
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .width(75.dp)
                                .testTag("reminder_custom_offset_amount")
                        )

                        Box {
                            Surface(
                                onClick = { showUnitMenu = true },
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = when (customOffsetUnit) {
                                            "HOURS" -> "Hours before"
                                            "DAYS" -> "Days before"
                                            else -> "Minutes before"
                                        },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("▼", fontSize = 10.sp)
                                }
                            }

                            DropdownMenu(expanded = showUnitMenu, onDismissRequest = { showUnitMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Minutes before") },
                                    onClick = {
                                        customOffsetUnit = "MINUTES"
                                        showUnitMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Hours before") },
                                    onClick = {
                                        customOffsetUnit = "HOURS"
                                        showUnitMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Days before") },
                                    onClick = {
                                        customOffsetUnit = "DAYS"
                                        showUnitMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Type 3: Specific time of day
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { mode = "SPECIFIC_TIME" },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = mode == "SPECIFIC_TIME",
                        onClick = { mode = "SPECIFIC_TIME" },
                        modifier = Modifier.testTag("reminder_mode_specific_time")
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Specific Time", fontWeight = FontWeight.SemiBold)
                }

                if (mode == "SPECIFIC_TIME") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 32.dp, top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = { showTimePicker = true },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.testTag("reminder_pick_time_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("At $specificTime", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val reminderItem = when (mode) {
                        "SPECIFIC_TIME" -> TaskReminderItem(
                            customTime = specificTime,
                            type = "SPECIFIC_TIME",
                            referenceType = referenceType,
                            isEnabled = true
                        )
                        "CUSTOM_OFFSET" -> {
                            val totalMinutes = when (customOffsetUnit) {
                                "HOURS" -> customOffsetAmount * 60
                                "DAYS" -> customOffsetAmount * 1440
                                else -> customOffsetAmount
                            }
                            TaskReminderItem(
                                minutesBefore = totalMinutes,
                                type = "OFFSET",
                                referenceType = referenceType,
                                isEnabled = true
                            )
                        }
                        else -> TaskReminderItem(
                            minutesBefore = selectedPresetMinutes,
                            type = "OFFSET",
                            referenceType = referenceType,
                            isEnabled = true
                        )
                    }
                    onAddReminder(reminderItem)
                    onDismiss()
                },
                modifier = Modifier.testTag("custom_reminder_confirm_btn")
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showTimePicker) {
        LifeTimePickerDialog(
            initialTime = specificTime,
            onConfirm = {
                specificTime = it
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false },
            title = "Select Reminder Time"
        )
    }
}
