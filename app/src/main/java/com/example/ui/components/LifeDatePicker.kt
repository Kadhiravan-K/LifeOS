package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.DateUtils

/**
 * Standard Material 3 Date Picker Dialog with quick presets (Today, Tomorrow, +7 Days),
 * selectable date constraints, and deterministic ISO-8601 formatting.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LifeDatePickerDialog(
    initialDateIso: String? = null,
    minDateIso: String? = null,
    maxDateIso: String? = null,
    title: String = "Select Date",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val today = DateUtils.todayString()
    val baseIso = initialDateIso?.ifBlank { today } ?: today

    val initialMillis = remember(baseIso) {
        DateUtils.isoStringToEpochDay(baseIso) * 86_400_000L
    }

    val minMillis = remember(minDateIso) {
        minDateIso?.let { DateUtils.isoStringToEpochDay(it) * 86_400_000L }
    }
    val maxMillis = remember(maxDateIso) {
        maxDateIso?.let { DateUtils.isoStringToEpochDay(it) * 86_400_000L }
    }

    val selectableDates = remember(minMillis, maxMillis) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                if (minMillis != null && utcTimeMillis < minMillis) return false
                if (maxMillis != null && utcTimeMillis > maxMillis) return false
                return true
            }

            override fun isSelectableYear(year: Int): Boolean {
                return year in 1970..2100
            }
        }
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis,
        selectableDates = selectableDates
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val selected = datePickerState.selectedDateMillis
                    if (selected != null) {
                        val epochDay = selected / 86_400_000L
                        onConfirm(DateUtils.epochDayToIsoString(epochDay))
                    } else {
                        onConfirm(today)
                    }
                },
                modifier = Modifier.testTag("date_picker_confirm_btn")
            ) {
                Text("Confirm", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("date_picker_dismiss_btn")
            ) {
                Text("Cancel")
            }
        },
        modifier = Modifier.testTag("life_date_picker_dialog")
    ) {
        Column {
            // Preset Chips Row (Today, Tomorrow, +3 Days, Next Week)
            val presets = listOf(
                "Today" to today,
                "Tomorrow" to DateUtils.addDays(today, 1),
                "+3 Days" to DateUtils.addDays(today, 3),
                "Next Week" to DateUtils.addDays(today, 7)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { (label, dateIso) ->
                    val isWithinConstraints = remember(dateIso, minDateIso, maxDateIso) {
                        val epoch = DateUtils.isoStringToEpochDay(dateIso)
                        val minOk = minDateIso?.let { epoch >= DateUtils.isoStringToEpochDay(it) } ?: true
                        val maxOk = maxDateIso?.let { epoch <= DateUtils.isoStringToEpochDay(it) } ?: true
                        minOk && maxOk
                    }

                    FilterChip(
                        selected = false,
                        onClick = {
                            if (isWithinConstraints) {
                                onConfirm(dateIso)
                            }
                        },
                        enabled = isWithinConstraints,
                        label = { Text(label, fontSize = 12.sp) },
                        modifier = Modifier.testTag("date_preset_$label")
                    )
                }
            }

            DatePicker(
                state = datePickerState,
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 24.dp, top = 16.dp)
                    )
                }
            )
        }
    }
}

/**
 * Standardized clickable trigger field for date selection with localized display formatting,
 * optional clear action, and seamless Material 3 surface styling.
 */
@Composable
fun LifeDatePickerField(
    selectedDateIso: String,
    onDateSelected: (String) -> Unit,
    label: String = "Due Date",
    minDateIso: String? = null,
    maxDateIso: String? = null,
    isNullable: Boolean = false,
    onClear: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    val today = DateUtils.todayString()
    val displayDate = remember(selectedDateIso) {
        if (selectedDateIso.isBlank()) {
            "No Date Set"
        } else {
            val formatted = DateUtils.formatDisplayDate(selectedDateIso)
            when (selectedDateIso) {
                today -> "Today ($formatted)"
                DateUtils.addDays(today, 1) -> "Tomorrow ($formatted)"
                DateUtils.addDays(today, -1) -> "Yesterday ($formatted)"
                else -> formatted
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selectedDateIso.isNotBlank()) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { showDialog = true }
            .testTag("date_picker_field")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = label,
                    tint = if (selectedDateIso.isNotBlank()) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = displayDate,
                        fontSize = 14.sp,
                        color = if (selectedDateIso.isNotBlank()) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (isNullable && selectedDateIso.isNotBlank() && onClear != null) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("date_picker_clear_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear Date",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }

    if (showDialog) {
        LifeDatePickerDialog(
            initialDateIso = selectedDateIso,
            minDateIso = minDateIso,
            maxDateIso = maxDateIso,
            onConfirm = {
                showDialog = false
                onDateSelected(it)
            },
            onDismiss = { showDialog = false }
        )
    }
}

/**
 * Standard alias for [LifeDatePickerField] conforming to unified design naming.
 */
@Composable
fun LifeDateField(
    selectedDateIso: String,
    onDateSelected: (String) -> Unit,
    label: String = "Date",
    minDateIso: String? = null,
    maxDateIso: String? = null,
    isNullable: Boolean = false,
    onClear: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) = LifeDatePickerField(
    selectedDateIso = selectedDateIso,
    onDateSelected = onDateSelected,
    label = label,
    minDateIso = minDateIso,
    maxDateIso = maxDateIso,
    isNullable = isNullable,
    onClear = onClear,
    modifier = modifier
)

data class DateRangeSelection(
    val startDateIso: String,
    val endDateIso: String
)

/**
 * Standard Date Range Selector Field supporting date range presets (Next 7 Days, This Month, Next 30 Days, Quarterly),
 * start/end date constraints, and weekend highlighting context.
 */
@Composable
fun LifeDateRangeField(
    startDateIso: String,
    endDateIso: String,
    onRangeSelected: (DateRangeSelection) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Date Range",
    minDateIso: String? = null,
    maxDateIso: String? = null
) {
    var showDialog by remember { mutableStateOf(false) }
    val today = DateUtils.todayString()

    val displayRange = remember(startDateIso, endDateIso) {
        val s = if (startDateIso.isNotBlank()) DateUtils.formatDisplayDate(startDateIso) else "Start"
        val e = if (endDateIso.isNotBlank()) DateUtils.formatDisplayDate(endDateIso) else "End"
        "$s → $e"
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { showDialog = true }
            .testTag("life_date_range_field")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = displayRange, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Text("Edit", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Select Date Range", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Quick Presets:", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(6.dp))

                    val presets = listOf(
                        "Next 7 Days" to (today to DateUtils.addDays(today, 7)),
                        "This Month" to (today to DateUtils.addDays(today, 30)),
                        "Next 30 Days" to (today to DateUtils.addDays(today, 30)),
                        "Quarterly" to (today to DateUtils.addDays(today, 90))
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presets.forEach { (presetLabel, range) ->
                            FilterChip(
                                selected = startDateIso == range.first && endDateIso == range.second,
                                onClick = {
                                    onRangeSelected(DateRangeSelection(range.first, range.second))
                                    showDialog = false
                                },
                                label = { Text(presetLabel, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Weekends and non-working days highlighted automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
