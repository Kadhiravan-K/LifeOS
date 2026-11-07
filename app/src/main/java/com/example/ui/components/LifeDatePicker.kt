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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.DateUtils
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Vertical Month Scrolling Calendar Composable for seamless, natural vertical navigation through months.
 */
@Composable
fun VerticalMonthCalendar(
    selectedDateIso: String? = null,
    rangeStartIso: String? = null,
    rangeEndIso: String? = null,
    minDateIso: String? = null,
    maxDateIso: String? = null,
    onDateClicked: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { LocalDate.now() }
    val baseDate = remember(selectedDateIso, rangeStartIso) {
        val iso = selectedDateIso ?: rangeStartIso
        if (!iso.isNullOrBlank()) {
            runCatching { LocalDate.parse(iso) }.getOrDefault(today)
        } else today
    }

    // 12 months before current month up to 36 months after (total 49 months)
    val startYearMonth = remember { YearMonth.from(today).minusMonths(12) }
    val totalMonths = 49
    val monthsList = remember {
        (0 until totalMonths).map { startYearMonth.plusMonths(it.toLong()) }
    }

    val initialIndex = remember(baseDate) {
        val targetYm = YearMonth.from(baseDate)
        val diff = java.time.temporal.ChronoUnit.MONTHS.between(startYearMonth, targetYm).toInt()
        diff.coerceIn(0, totalMonths - 1)
    }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)

    LaunchedEffect(selectedDateIso, rangeStartIso) {
        val targetIso = selectedDateIso ?: rangeStartIso
        if (!targetIso.isNullOrBlank()) {
            val date = runCatching { LocalDate.parse(targetIso) }.getOrNull()
            if (date != null) {
                val ym = YearMonth.from(date)
                val diff = java.time.temporal.ChronoUnit.MONTHS.between(startYearMonth, ym).toInt()
                if (diff in 0 until totalMonths) {
                    listState.scrollToItem(diff)
                }
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .testTag("vertical_month_calendar_list")
    ) {
        itemsIndexed(monthsList) { _, yearMonth ->
            MonthViewBlock(
                yearMonth = yearMonth,
                today = today,
                selectedDateIso = selectedDateIso,
                rangeStartIso = rangeStartIso,
                rangeEndIso = rangeEndIso,
                minDateIso = minDateIso,
                maxDateIso = maxDateIso,
                onDateClicked = onDateClicked
            )
            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

@Composable
private fun MonthViewBlock(
    yearMonth: YearMonth,
    today: LocalDate,
    selectedDateIso: String?,
    rangeStartIso: String?,
    rangeEndIso: String?,
    minDateIso: String?,
    maxDateIso: String?,
    onDateClicked: (String) -> Unit
) {
    val monthTitle = remember(yearMonth) {
        val monthName = yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
        "$monthName ${yearMonth.year}"
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Month Header
        Text(
            text = monthTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        )

        // Day of Week Names Row (S, M, T, W, T, F, S)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")
            daysOfWeek.forEach { dayName ->
                Text(
                    text = dayName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Days Grid
        val firstDayOfMonth = yearMonth.atDay(1)
        val firstDayOfWeekVal = (firstDayOfMonth.dayOfWeek.value % 7) // Sunday = 0, Monday = 1...
        val daysInMonth = yearMonth.lengthOfMonth()
        val totalCells = firstDayOfWeekVal + daysInMonth
        val rows = (totalCells + 6) / 7

        for (rowIndex in 0 until rows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (colIndex in 0 until 7) {
                    val dayNum = rowIndex * 7 + colIndex - firstDayOfWeekVal + 1
                    if (dayNum in 1..daysInMonth) {
                        val currentDate = yearMonth.atDay(dayNum)
                        val dateIso = currentDate.format(DateTimeFormatter.ISO_LOCAL_DATE)

                        val isSelected = dateIso == selectedDateIso
                        val isRangeStart = dateIso == rangeStartIso
                        val isRangeEnd = dateIso == rangeEndIso
                        val isToday = currentDate == today

                        val isInRange = remember(dateIso, rangeStartIso, rangeEndIso) {
                            if (rangeStartIso != null && rangeEndIso != null) {
                                dateIso > rangeStartIso && dateIso < rangeEndIso
                            } else false
                        }

                        val isEnabled = remember(dateIso, minDateIso, maxDateIso) {
                            val minOk = minDateIso?.let { dateIso >= it } ?: true
                            val maxOk = maxDateIso?.let { dateIso <= it } ?: true
                            minOk && maxOk
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // In-range background fill
                            if (isInRange) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(30.dp)
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
                                )
                            }

                            // Day Cell Button
                            val cellShape = when {
                                isSelected || isRangeStart || isRangeEnd -> CircleShape
                                else -> RoundedCornerShape(8.dp)
                            }

                            val cellBackground = when {
                                isSelected || isRangeStart || isRangeEnd -> MaterialTheme.colorScheme.primary
                                else -> Color.Transparent
                            }

                            val cellTextColor = when {
                                isSelected || isRangeStart || isRangeEnd -> MaterialTheme.colorScheme.onPrimary
                                !isEnabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                isToday -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurface
                            }

                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(cellShape)
                                    .background(cellBackground)
                                    .then(
                                        if (isToday && !isSelected && !isRangeStart && !isRangeEnd) {
                                            Modifier.border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                        } else Modifier
                                    )
                                    .clickable(enabled = isEnabled) { onDateClicked(dateIso) }
                                    .testTag("cal_day_$dateIso"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayNum.toString(),
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected || isRangeStart || isRangeEnd || isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = cellTextColor,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        // Empty padding cell
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/**
 * Standard Material 3 Date Picker Dialog with vertical month scrolling, quick presets (Today, Tomorrow, +7 Days),
 * selectable date constraints, and deterministic ISO-8601 formatting.
 */
@Composable
fun LifeDatePickerDialog(
    initialDateIso: String? = null,
    minDateIso: String? = null,
    maxDateIso: String? = null,
    title: String = "Select Date",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val today = remember { DateUtils.todayString() }
    val baseIso = initialDateIso?.ifBlank { today } ?: today
    var selectedDate by remember(baseIso) { mutableStateOf(baseIso) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = DateUtils.formatDisplayDate(selectedDate),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
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
                        .padding(bottom = 8.dp)
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
                            selected = selectedDate == dateIso,
                            onClick = {
                                if (isWithinConstraints) {
                                    selectedDate = dateIso
                                }
                            },
                            enabled = isWithinConstraints,
                            label = { Text(label, fontSize = 11.sp) },
                            modifier = Modifier.testTag("date_preset_$label")
                        )
                    }
                }

                // Vertical month scrolling calendar container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                ) {
                    VerticalMonthCalendar(
                        selectedDateIso = selectedDate,
                        minDateIso = minDateIso,
                        maxDateIso = maxDateIso,
                        onDateClicked = { selectedDate = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedDate) },
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
    )
}

data class DateRangeSelection(
    val startDateIso: String,
    val endDateIso: String
)

/**
 * Standard Date Range Picker Dialog with vertical month scrolling, quick presets (Next 7 Days, This Month, Next 30 Days, Quarterly),
 * interactive start and end date selection, and validation preventing end date earlier than start date.
 */
@Composable
fun LifeDateRangePickerDialog(
    initialStartDateIso: String? = null,
    initialEndDateIso: String? = null,
    minDateIso: String? = null,
    maxDateIso: String? = null,
    title: String = "Select Date Range",
    onConfirm: (DateRangeSelection) -> Unit,
    onDismiss: () -> Unit
) {
    val today = remember { DateUtils.todayString() }
    var startDate by remember(initialStartDateIso) {
        mutableStateOf(initialStartDateIso?.ifBlank { today } ?: today)
    }
    var endDate by remember(initialEndDateIso) {
        mutableStateOf(initialEndDateIso?.takeIf { it.isNotBlank() && it >= startDate } ?: startDate)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = DateUtils.formatDisplayDate(startDate),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text("→", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = DateUtils.formatDisplayDate(endDate),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Quick presets
                val presets = listOf(
                    "Next 7 Days" to (today to DateUtils.addDays(today, 7)),
                    "This Month" to (today to DateUtils.addDays(today, 30)),
                    "Next 30 Days" to (today to DateUtils.addDays(today, 30)),
                    "Quarterly" to (today to DateUtils.addDays(today, 90))
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { (presetLabel, range) ->
                        FilterChip(
                            selected = startDate == range.first && endDate == range.second,
                            onClick = {
                                startDate = range.first
                                endDate = range.second
                            },
                            label = { Text(presetLabel, fontSize = 11.sp) }
                        )
                    }
                }

                // Vertical month scrolling calendar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                ) {
                    VerticalMonthCalendar(
                        rangeStartIso = startDate,
                        rangeEndIso = endDate,
                        minDateIso = minDateIso,
                        maxDateIso = maxDateIso,
                        onDateClicked = { clickedIso ->
                            if (startDate.isNotBlank() && endDate.isNotBlank() && startDate != endDate) {
                                // Restart selection
                                startDate = clickedIso
                                endDate = clickedIso
                            } else if (clickedIso < startDate) {
                                startDate = clickedIso
                                endDate = clickedIso
                            } else {
                                endDate = clickedIso
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val finalEnd = if (endDate >= startDate) endDate else startDate
                    onConfirm(DateRangeSelection(startDate, finalEnd))
                },
                modifier = Modifier.testTag("date_range_picker_confirm_btn")
            ) {
                Text("Confirm", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("date_range_picker_dismiss_btn")
            ) {
                Text("Cancel")
            }
        },
        modifier = Modifier.testTag("life_date_range_dialog")
    )
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

    val today = remember { DateUtils.todayString() }
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

/**
 * Standard Date Range Selector Field supporting interactive date range picker with vertical scrolling months,
 * start/end date constraints, and quick presets.
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
        LifeDateRangePickerDialog(
            initialStartDateIso = startDateIso,
            initialEndDateIso = endDateIso,
            minDateIso = minDateIso,
            maxDateIso = maxDateIso,
            onConfirm = {
                showDialog = false
                onRangeSelected(it)
            },
            onDismiss = { showDialog = false }
        )
    }
}
