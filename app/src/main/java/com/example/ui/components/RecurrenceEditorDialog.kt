package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.RecurrenceType
import com.example.domain.model.TaskItem
import com.example.domain.recurrence.RecurrenceEngine
import com.example.utils.DateUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomRecurrenceDialog(
    startDateIso: String,
    initialInterval: Int = 1,
    initialUnit: String = "DAYS",
    initialDaysOfWeek: List<String> = emptyList(),
    initialEndCondition: String = "NEVER",
    initialEndDateIso: String? = null,
    initialCount: Int = 1,
    onApply: (
        interval: Int,
        unit: String,
        daysOfWeek: List<String>,
        endCondition: String,
        endDateIso: String?,
        count: Int
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var interval by remember { mutableIntStateOf(if (initialInterval > 0) initialInterval else 1) }
    var unit by remember { mutableStateOf(if (initialUnit.isNotBlank()) initialUnit.uppercase() else "DAYS") }
    var selectedDays by remember { mutableStateOf(initialDaysOfWeek.toSet()) }
    var endCondition by remember { mutableStateOf(initialEndCondition) }
    var endDateIso by remember { mutableStateOf(initialEndDateIso ?: DateUtils.addDays(startDateIso, 30)) }
    var count by remember { mutableIntStateOf(if (initialCount > 0) initialCount else 10) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showUnitMenu by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )

    val units = listOf("DAYS" to "Days", "WEEKS" to "Weeks", "MONTHS" to "Months", "YEARS" to "Years")
    val weekdays = listOf(
        "MONDAY" to "M",
        "TUESDAY" to "T",
        "WEDNESDAY" to "W",
        "THURSDAY" to "T",
        "FRIDAY" to "F",
        "SATURDAY" to "S",
        "SUNDAY" to "S"
    )

    val dummyTask by remember(interval, unit, selectedDays, endCondition, endDateIso, count) {
        derivedStateOf {
            TaskItem(
                title = "Preview",
                dueDateIso = startDateIso,
                recurrenceType = RecurrenceType.CUSTOM,
                recurrenceInterval = interval,
                recurrenceUnit = unit,
                recurrenceDaysOfWeek = selectedDays.toList(),
                recurrenceEndCondition = endCondition,
                recurrenceEndDateIso = if (endCondition == "ON_DATE") endDateIso else null,
                recurrenceCount = if (endCondition == "AFTER_COUNT") count else 1
            )
        }
    }

    val previewText by remember(dummyTask) {
        derivedStateOf {
            RecurrenceEngine.formatRecurrencePreview(dummyTask)
        }
    }

    val nextOccurrences by remember(dummyTask) {
        derivedStateOf {
            RecurrenceEngine.getNextNOccurrences(dummyTask, count = 3, fromDateIso = startDateIso)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Repeat,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Custom Recurrence", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Repeat every [ N ] [ Unit ]
                Text("Repeat every", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = if (interval > 0) interval.toString() else "",
                        onValueChange = { str ->
                            val n = str.filter { it.isDigit() }.toIntOrNull()
                            if (n != null && n in 1..999) {
                                interval = n
                            } else if (str.isEmpty()) {
                                interval = 1
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .width(80.dp)
                            .testTag("recurrence_interval_input")
                    )

                    Box(modifier = Modifier.weight(1f)) {
                        Surface(
                            onClick = { showUnitMenu = true },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
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
                                    text = units.find { it.first == unit }?.second ?: "Days",
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text("▼", fontSize = 10.sp)
                            }
                        }

                        DropdownMenu(
                            expanded = showUnitMenu,
                            onDismissRequest = { showUnitMenu = false }
                        ) {
                            units.forEach { (uKey, uLabel) ->
                                DropdownMenuItem(
                                    text = { Text(uLabel) },
                                    onClick = {
                                        unit = uKey
                                        showUnitMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                // If unit is WEEKS, show weekday selector
                if (unit == "WEEKS") {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Repeat on", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        weekdays.forEach { (dayKey, dayInitial) ->
                            val isSel = selectedDays.contains(dayKey)
                            Surface(
                                onClick = {
                                    selectedDays = if (isSel) {
                                        if (selectedDays.size > 1) selectedDays - dayKey else selectedDays
                                    } else {
                                        selectedDays + dayKey
                                    }
                                },
                                shape = CircleShape,
                                color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = dayInitial,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // End Condition
                Text("Ends", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))

                // Never
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { endCondition = "NEVER" },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = endCondition == "NEVER",
                        onClick = { endCondition = "NEVER" },
                        modifier = Modifier.testTag("end_cond_never")
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Never")
                }

                // On date
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { endCondition = "ON_DATE" },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = endCondition == "ON_DATE",
                        onClick = { endCondition = "ON_DATE" },
                        modifier = Modifier.testTag("end_cond_on_date")
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("On date")
                    if (endCondition == "ON_DATE") {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            onClick = { showDatePicker = true },
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(DateUtils.formatDisplayDate(endDateIso), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                // After N occurrences
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { endCondition = "AFTER_COUNT" },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = endCondition == "AFTER_COUNT",
                        onClick = { endCondition = "AFTER_COUNT" },
                        modifier = Modifier.testTag("end_cond_after_count")
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("After")
                    if (endCondition == "AFTER_COUNT") {
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = if (count > 0) count.toString() else "",
                            onValueChange = { str ->
                                val n = str.filter { it.isDigit() }.toIntOrNull()
                                if (n != null && n in 1..999) {
                                    count = n
                                } else if (str.isEmpty()) {
                                    count = 1
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .width(70.dp)
                                .height(50.dp)
                                .testTag("end_count_input")
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("occurrences", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Live Preview Card
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Summary",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = previewText,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )

                        if (nextOccurrences.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Next: " + nextOccurrences.joinToString(", ") { DateUtils.formatDisplayDate(it) },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onApply(
                        interval,
                        unit,
                        selectedDays.toList(),
                        endCondition,
                        if (endCondition == "ON_DATE") endDateIso else null,
                        if (endCondition == "AFTER_COUNT") count else 1
                    )
                },
                modifier = Modifier.testTag("custom_recurrence_apply_btn")
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { ms ->
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                        endDateIso = sdf.format(Date(ms))
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
}
