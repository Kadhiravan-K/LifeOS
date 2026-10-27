package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.domain.model.DurationUnit

/**
 * Standard preset duration choices for rapid selection.
 */
val DefaultDurationPresets = listOf(
    Triple(15, DurationUnit.MINUTES, "15m"),
    Triple(30, DurationUnit.MINUTES, "30m"),
    Triple(45, DurationUnit.MINUTES, "45m"),
    Triple(1, DurationUnit.HOURS, "1h"),
    Triple(2, DurationUnit.HOURS, "2h"),
    Triple(1, DurationUnit.DAYS, "1d"),
    Triple(1, DurationUnit.WEEKS, "1w"),
    Triple(1, DurationUnit.MONTHS, "1mo"),
    Triple(1, DurationUnit.QUARTERS, "1q"),
    Triple(1, DurationUnit.YEARS, "1y")
)

/**
 * Reusable Duration Input component for LifeOS.
 * Consolidates numeric amount input, unit dropdown, validation, stepper buttons ([-]/[+]),
 * all-day duration toggle, and quick preset chips (15m, 30m, 45m, 1h, 2h).
 *
 * Prevents state desynchronization by immediately reporting invalid / empty / zero
 * inputs to [onDurationAmountChanged] as 0, allowing parent forms to display error
 * states and disable save triggers.
 */
@Composable
fun DurationInput(
    durationAmount: Int,
    durationUnit: DurationUnit,
    onDurationAmountChanged: (Int) -> Unit,
    onDurationUnitChanged: (DurationUnit) -> Unit,
    modifier: Modifier = Modifier,
    isAllDay: Boolean = false,
    onAllDayToggle: ((Boolean) -> Unit)? = null,
    isError: Boolean = durationAmount <= 0 && !isAllDay,
    errorMessage: String = "Duration must be greater than 0",
    showPresets: Boolean = true,
    inputTestTag: String = "adv_duration_amount_input"
) {
    var textDraft by remember(durationAmount) {
        mutableStateOf(if (durationAmount > 0) durationAmount.toString() else if (durationAmount == 0) "0" else "")
    }
    var showUnitMenu by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Expected Duration", style = MaterialTheme.typography.labelSmall)
            if (onAllDayToggle != null) {
                FilterChip(
                    selected = isAllDay,
                    onClick = { onAllDayToggle(!isAllDay) },
                    label = { Text("All-Day", fontSize = 11.sp) },
                    modifier = Modifier.testTag("duration_all_day_chip")
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))

        if (!isAllDay) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Stepper Decrement Button [-]
                FilledTonalIconButton(
                    onClick = {
                        val step = if (durationUnit == DurationUnit.MINUTES) 5 else 1
                        val next = (durationAmount - step).coerceAtLeast(0)
                        onDurationAmountChanged(next)
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("duration_decrement_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Decrement 5m",
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Numeric Input Field
                OutlinedTextField(
                    value = textDraft,
                    onValueChange = { inputStr ->
                        val digits = inputStr.filter { it.isDigit() }
                        textDraft = digits
                        if (digits.isEmpty()) {
                            onDurationAmountChanged(0)
                        } else {
                            val parsed = digits.toIntOrNull()
                            if (parsed != null && parsed in 0..9999) {
                                onDurationAmountChanged(parsed)
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = isError,
                    supportingText = if (isError) {
                        { Text(errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 11.sp) }
                    } else null,
                    modifier = Modifier
                        .width(76.dp)
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag(inputTestTag)
                )

                // Stepper Increment Button [+]
                FilledTonalIconButton(
                    onClick = {
                        val step = if (durationUnit == DurationUnit.MINUTES) 5 else 1
                        val next = (durationAmount + step).coerceIn(0, 9999)
                        onDurationAmountChanged(next)
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("duration_increment_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Increment 5m",
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Unit Selector Dropdown
                Box(modifier = Modifier.weight(1f)) {
                    Surface(
                        onClick = { showUnitMenu = true },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("duration_unit_selector")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (durationAmount == 1) {
                                    durationUnit.singularName.replaceFirstChar { it.uppercase() }
                                } else {
                                    durationUnit.displayName.replaceFirstChar { it.uppercase() }
                                },
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            Text("▼", fontSize = 10.sp)
                        }
                    }

                    DropdownMenu(
                        expanded = showUnitMenu,
                        onDismissRequest = { showUnitMenu = false }
                    ) {
                        DurationUnit.entries.forEach { u ->
                            DropdownMenuItem(
                                text = { Text(u.displayName.replaceFirstChar { it.uppercase() }) },
                                onClick = {
                                    onDurationUnitChanged(u)
                                    showUnitMenu = false
                                },
                                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                            )
                        }
                    }
                }
            }

            if (showPresets) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DefaultDurationPresets.forEach { (amt, u, label) ->
                        val isSelected = durationAmount == amt && durationUnit == u
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                onDurationAmountChanged(amt)
                                onDurationUnitChanged(u)
                            },
                            label = { Text(label, fontSize = 11.sp) },
                            modifier = Modifier.defaultMinSize(minHeight = 36.dp)
                        )
                    }
                }
            }
        } else {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Event spans entire day (no fixed hour duration)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

/**
 * Standard alias for [DurationInput] conforming to unified design naming.
 */
@Composable
fun LifeDurationPicker(
    durationAmount: Int,
    durationUnit: DurationUnit,
    onDurationAmountChanged: (Int) -> Unit,
    onDurationUnitChanged: (DurationUnit) -> Unit,
    modifier: Modifier = Modifier,
    isAllDay: Boolean = false,
    onAllDayToggle: ((Boolean) -> Unit)? = null,
    isError: Boolean = durationAmount <= 0 && !isAllDay,
    errorMessage: String = "Duration must be greater than 0",
    showPresets: Boolean = true,
    inputTestTag: String = "adv_duration_amount_input"
) = DurationInput(
    durationAmount = durationAmount,
    durationUnit = durationUnit,
    onDurationAmountChanged = onDurationAmountChanged,
    onDurationUnitChanged = onDurationUnitChanged,
    modifier = modifier,
    isAllDay = isAllDay,
    onAllDayToggle = onAllDayToggle,
    isError = isError,
    errorMessage = errorMessage,
    showPresets = showPresets,
    inputTestTag = inputTestTag
)
