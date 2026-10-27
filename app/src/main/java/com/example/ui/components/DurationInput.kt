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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
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
 * Consolidates numeric amount input, unit dropdown, validation, and preset chips.
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
    isError: Boolean = durationAmount <= 0,
    errorMessage: String = "Duration must be greater than 0",
    showPresets: Boolean = true,
    inputTestTag: String = "adv_duration_amount_input"
) {
    var textDraft by remember(durationAmount) {
        mutableStateOf(if (durationAmount > 0) durationAmount.toString() else if (durationAmount == 0) "0" else "")
    }
    var showUnitMenu by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text("Expected Duration", style = MaterialTheme.typography.labelSmall)
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
                    .width(90.dp)
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag(inputTestTag)
            )

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
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (durationAmount == 1) {
                                durationUnit.singularName.replaceFirstChar { it.uppercase() }
                            } else {
                                durationUnit.displayName.replaceFirstChar { it.uppercase() }
                            },
                            fontWeight = FontWeight.SemiBold
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
    }
}
