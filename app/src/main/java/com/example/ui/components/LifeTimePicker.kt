package com.example.ui.components

import android.text.format.DateFormat
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/**
 * Standard preset time options for quick selection.
 */
val DefaultTimePresets = listOf(
    "09:00" to "Morning (09:00)",
    "12:00" to "Noon (12:00)",
    "15:00" to "Afternoon (15:00)",
    "18:00" to "Evening (18:00)",
    "21:00" to "Night (21:00)"
)

/**
 * Parses a "HH:mm" string into an (hour, minute) pair.
 * Safely defaults to (9, 0) if unparseable.
 */
fun parseTimeToHourMinute(time: String?): Pair<Int, Int> {
    if (time.isNullOrBlank()) return 9 to 0
    val parts = time.trim().split(":")
    if (parts.size != 2) return 9 to 0
    val hour = parts[0].toIntOrNull()?.coerceIn(0, 23) ?: return 9 to 0
    val minute = parts[1].toIntOrNull()?.coerceIn(0, 59) ?: return 9 to 0
    return hour to minute
}

/**
 * Formats a "HH:mm" 24-hour time string for user display, respecting 12-hour or 24-hour settings.
 * E.g., "14:30" -> "2:30 PM" (12h) or "14:30" (24h).
 */
fun formatTimeDisplay(time: String?, is24Hour: Boolean): String {
    if (time.isNullOrBlank()) return ""
    val (hour, minute) = parseTimeToHourMinute(time)
    return if (is24Hour) {
        String.format(Locale.ROOT, "%02d:%02d", hour, minute)
    } else {
        val amPm = if (hour >= 12) "PM" else "AM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        String.format(Locale.ROOT, "%d:%02d %s", displayHour, minute, amPm)
    }
}

/**
 * Unified Material 3 Time Picker Dialog for LifeOS.
 * Features:
 * - Analog Clock Face dial mode & Numeric Keypad input mode toggle.
 * - System 12-hour / 24-hour auto-detection via [DateFormat.is24HourFormat].
 * - Quick preset chips for rapid 1-tap time selection.
 * - Confirms formatted "HH:mm" string.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LifeTimePickerDialog(
    initialTime: String = "09:00",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Select Time",
    is24Hour: Boolean = DateFormat.is24HourFormat(LocalContext.current),
    presets: List<Pair<String, String>> = DefaultTimePresets
) {
    val (parsedHour, parsedMinute) = remember(initialTime) { parseTimeToHourMinute(initialTime) }
    var currentHour by remember(initialTime) { mutableIntStateOf(parsedHour) }
    var currentMinute by remember(initialTime) { mutableIntStateOf(parsedMinute) }
    var isInputMode by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("life_time_picker_dialog"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                IconButton(
                    onClick = { isInputMode = !isInputMode },
                    modifier = Modifier.testTag("life_time_picker_mode_toggle")
                ) {
                    Icon(
                        imageVector = if (isInputMode) Icons.Default.Schedule else Icons.Default.Keyboard,
                        contentDescription = if (isInputMode) "Switch to clock dial" else "Switch to text input",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (presets.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presets.forEach { (presetTime, label) ->
                            val (presetH, presetM) = parseTimeToHourMinute(presetTime)
                            val isSelected = currentHour == presetH && currentMinute == presetM
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    currentHour = presetH
                                    currentMinute = presetM
                                },
                                label = { Text(label, fontSize = 11.sp) },
                                modifier = Modifier.defaultMinSize(minHeight = 36.dp)
                            )
                        }
                    }
                }

                // Render TimePicker (dial) or TimeInput (keyboard) keyed to current hour/minute
                key(currentHour, currentMinute, is24Hour) {
                    val timePickerState = rememberTimePickerState(
                        initialHour = currentHour,
                        initialMinute = currentMinute,
                        is24Hour = is24Hour
                    )

                    if (isInputMode) {
                        TimeInput(
                            state = timePickerState,
                            modifier = Modifier.testTag("life_time_input_view")
                        )
                    } else {
                        TimePicker(
                            state = timePickerState,
                            modifier = Modifier.testTag("life_time_picker_view")
                        )
                    }

                    // Keep currentHour/currentMinute synced when user alters picker state
                    LaunchedEffectStateSync(timePickerState) { h, m ->
                        currentHour = h
                        currentMinute = m
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val formatted = String.format(Locale.ROOT, "%02d:%02d", currentHour, currentMinute)
                    onConfirm(formatted)
                },
                modifier = Modifier.testTag("life_time_picker_confirm_btn")
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("life_time_picker_cancel_btn")
            ) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Helper to observe TimePickerState changes and synchronize parent state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LaunchedEffectStateSync(
    state: androidx.compose.material3.TimePickerState,
    onChanged: (Int, Int) -> Unit
) {
    androidx.compose.runtime.LaunchedEffect(state.hour, state.minute) {
        onChanged(state.hour, state.minute)
    }
}

/**
 * Standardized clickable Time Picker Trigger Field.
 * Renders an accessible touch target showing label, clock icon, and formatted time.
 */
@Composable
fun LifeTimePickerField(
    time: String?,
    onTimeSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Time",
    placeholder: String = "Pick time",
    is24Hour: Boolean = DateFormat.is24HourFormat(LocalContext.current),
    onClear: (() -> Unit)? = null,
    enabled: Boolean = true
) {
    var showDialog by remember { mutableStateOf(false) }

    Surface(
        onClick = { if (enabled) showDialog = true },
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .testTag("life_time_picker_field")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = if (time != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val formatted = remember(time, is24Hour) { formatTimeDisplay(time, is24Hour) }
                    Text(
                        text = if (formatted.isNotBlank()) formatted else placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (time != null) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (time != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                    )
                }
            }

            if (time != null && onClear != null) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(32.dp).testTag("life_time_picker_clear_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear time",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }

    if (showDialog) {
        LifeTimePickerDialog(
            initialTime = time ?: "09:00",
            onConfirm = { selected ->
                onTimeSelected(selected)
                showDialog = false
            },
            onDismiss = { showDialog = false },
            title = "Select $label",
            is24Hour = is24Hour
        )
    }
}
