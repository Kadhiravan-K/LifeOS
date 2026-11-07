package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.HabitItem
import com.example.domain.model.TaskItem
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

enum class HeatmapMetric(val displayName: String, val emoji: String) {
    ALL("All Activity", "⚡"),
    TASKS("Tasks Only", "✅"),
    HABITS("Habits Only", "🔥")
}

enum class HeatmapDateRange(val displayName: String, val weeks: Int) {
    FOUR_WEEKS("4 Weeks", 4),
    TWELVE_WEEKS("12 Weeks", 12),
    TWENTY_SIX_WEEKS("6 Months", 26)
}

enum class HeatmapPalette(val displayName: String, val levels: List<Color>) {
    EMERALD(
        "GitHub Emerald",
        listOf(
            Color(0xFF27272A).copy(alpha = 0.4f),
            Color(0xFF0E4429),
            Color(0xFF006D32),
            Color(0xFF26A641),
            Color(0xFF39D353)
        )
    ),
    OCEAN(
        "Ocean Blue",
        listOf(
            Color(0xFF27272A).copy(alpha = 0.4f),
            Color(0xFF0C2D48),
            Color(0xFF145DA0),
            Color(0xFF2E8BC0),
            Color(0xFF60A5FA)
        )
    ),
    SOLAR(
        "Solar Amber",
        listOf(
            Color(0xFF27272A).copy(alpha = 0.4f),
            Color(0xFF78350F),
            Color(0xFFB45309),
            Color(0xFFF59E0B),
            Color(0xFFFBBF24)
        )
    ),
    AMETHYST(
        "Amethyst Purple",
        listOf(
            Color(0xFF27272A).copy(alpha = 0.4f),
            Color(0xFF4C1D95),
            Color(0xFF6D28D9),
            Color(0xFF8B5CF6),
            Color(0xFFA78BFA)
        )
    )
}

data class DayActivitySummary(
    val dateIso: String,
    val taskCount: Int,
    val habitCount: Int,
    val totalCount: Int,
    val completedTasks: List<String>,
    val completedHabits: List<String>
)

data class HeatmapMonthHeader(
    val label: String,
    val weekIndex: Int,
    val month: java.time.YearMonth
)

data class HeatmapDateCell(
    val date: LocalDate,
    val dayOfMonth: Int,
    val count: Int,
    val taskCount: Int,
    val habitCount: Int
)

object HeatmapCalendarUtils {
    fun monthLabelFor(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("MMM", Locale.US))

    fun buildMonthHeaders(startDate: LocalDate, endDate: LocalDate): List<HeatmapMonthHeader> {
        val headers = mutableListOf<HeatmapMonthHeader>()
        var cursor = startDate
        var weekIndex = 0

        while (!cursor.isAfter(endDate)) {
            val month = java.time.YearMonth.from(cursor)
            val lastHeader = headers.lastOrNull()
            if (lastHeader == null || lastHeader.month != month) {
                headers.add(HeatmapMonthHeader(label = monthLabelFor(cursor), weekIndex = weekIndex, month = month))
            }
            cursor = cursor.plusWeeks(1)
            weekIndex += 1
        }

        return headers
    }

    fun buildDayCells(
        startDate: LocalDate,
        endDate: LocalDate,
        tasks: List<TaskItem>,
        habits: List<HabitItem>,
        metric: HeatmapMetric
    ): List<HeatmapDateCell> {
        val dailyActivityMap = mutableMapOf<String, DayActivitySummary>()
        var cursor = startDate
        while (!cursor.isAfter(endDate)) {
            val iso = cursor.format(DateTimeFormatter.ISO_DATE)

            val matchingTasks = tasks.filter { t ->
                !t.isArchived && ((t.completedDates.contains(iso)) || (t.isOneTimeCompleted && t.dueDateIso == iso))
            }.map { it.title }

            val matchingHabits = habits.filter { h -> h.completedDates.contains(iso) }
                .map { "${it.emoji} ${it.title}" }

            val total = matchingTasks.size + matchingHabits.size
            dailyActivityMap[iso] = DayActivitySummary(
                dateIso = iso,
                taskCount = matchingTasks.size,
                habitCount = matchingHabits.size,
                totalCount = total,
                completedTasks = matchingTasks,
                completedHabits = matchingHabits
            )
            cursor = cursor.plusDays(1)
        }

        val days = mutableListOf<HeatmapDateCell>()
        cursor = startDate
        while (!cursor.isAfter(endDate)) {
            val iso = cursor.format(DateTimeFormatter.ISO_DATE)
            val summary = dailyActivityMap.getValue(iso)
            val count = when (metric) {
                HeatmapMetric.ALL -> summary.totalCount
                HeatmapMetric.TASKS -> summary.taskCount
                HeatmapMetric.HABITS -> summary.habitCount
            }
            days.add(
                HeatmapDateCell(
                    date = cursor,
                    dayOfMonth = cursor.dayOfMonth,
                    count = count,
                    taskCount = summary.taskCount,
                    habitCount = summary.habitCount
                )
            )
            cursor = cursor.plusDays(1)
        }
        return days
    }

    fun buildDateWeeks(startDate: LocalDate, endDate: LocalDate): List<List<HeatmapDateCell>> {
        val allDays = buildDayCells(startDate, endDate, emptyList(), emptyList(), HeatmapMetric.ALL)
        val weeks = mutableListOf<List<HeatmapDateCell>>()
        var cursor = 0
        while (cursor < allDays.size) {
            weeks.add(allDays.subList(cursor, minOf(cursor + 7, allDays.size)))
            cursor += 7
        }
        return weeks
    }

    fun resolveCellColor(count: Int, palette: HeatmapPalette): Color = when {
        count == 0 -> palette.levels[0]
        count == 1 -> palette.levels[1]
        count == 2 -> palette.levels[2]
        count == 3 -> palette.levels[3]
        else -> palette.levels[4]
    }
}

/**
 * Polished GitHub-Style Productivity Consistency Heatmap Composable (#17, Issue #5).
 * Features:
 * - 7-row contribution grid by weeks
 * - 5 intensity color levels
 * - Customization: metric selection, date ranges, and 4 distinct color palettes
 * - Period navigation (prev/next window)
 * - Interactive tap to inspect day details
 */
@Composable
fun ProductivityHeatmap(
    tasks: List<TaskItem>,
    habits: List<HabitItem>,
    modifier: Modifier = Modifier,
    initialMetric: HeatmapMetric = HeatmapMetric.ALL,
    initialRange: HeatmapDateRange = HeatmapDateRange.TWELVE_WEEKS,
    initialPalette: HeatmapPalette = HeatmapPalette.EMERALD,
    showDates: Boolean = false,
    onToggleShowDates: ((Boolean) -> Unit)? = null
) {
    var internalShowDates by remember { mutableStateOf(showDates) }
    val effectiveShowDates = onToggleShowDates?.let { showDates } ?: internalShowDates

    var selectedMetric by remember { mutableStateOf(initialMetric) }
    var selectedRange by remember { mutableStateOf(initialRange) }
    var selectedPalette by remember { mutableStateOf(initialPalette) }
    var weekOffset by remember { mutableIntStateOf(0) }
    var inspectedDay by remember { mutableStateOf<DayActivitySummary?>(null) }

    val today = remember { LocalDate.now() }
    val endSunday = remember(weekOffset) {
        val baseDate = today.plusWeeks(weekOffset.toLong())
        baseDate.plusDays((7 - baseDate.dayOfWeek.value).toLong() % 7)
    }

    val totalDays = selectedRange.weeks * 7
    val startDate = remember(endSunday, selectedRange) {
        endSunday.minusDays((totalDays - 1).toLong())
    }

    val dailyActivityMap = remember(tasks, habits, startDate, endSunday) {
        val map = mutableMapOf<String, DayActivitySummary>()
        var curr = startDate
        while (!curr.isAfter(endSunday)) {
            val iso = curr.format(DateTimeFormatter.ISO_DATE)
            val matchingTasks = tasks.filter { t ->
                !t.isArchived && (
                    (t.completedDates.contains(iso)) ||
                        (t.isOneTimeCompleted && t.dueDateIso == iso)
                    )
            }.map { it.title }
            val matchingHabits = habits.filter { h -> h.completedDates.contains(iso) }
                .map { "${it.emoji} ${it.title}" }

            val total = matchingTasks.size + matchingHabits.size
            map[iso] = DayActivitySummary(
                dateIso = iso,
                taskCount = matchingTasks.size,
                habitCount = matchingHabits.size,
                totalCount = total,
                completedTasks = matchingTasks,
                completedHabits = matchingHabits
            )
            curr = curr.plusDays(1)
        }
        map
    }

    val heatmapWeeks = remember(startDate, endSunday, dailyActivityMap, selectedMetric) {
        val allDates = mutableListOf<LocalDate>()
        var cursor = startDate
        while (!cursor.isAfter(endSunday)) {
            allDates.add(cursor)
            cursor = cursor.plusDays(1)
        }
        allDates.chunked(7)
            .map { weekDates ->
                weekDates.map { date ->
                    val iso = date.format(DateTimeFormatter.ISO_DATE)
                    val summary = dailyActivityMap[iso]
                    val count = when (selectedMetric) {
                        HeatmapMetric.ALL -> summary?.totalCount ?: 0
                        HeatmapMetric.TASKS -> summary?.taskCount ?: 0
                        HeatmapMetric.HABITS -> summary?.habitCount ?: 0
                    }
                    HeatmapDateCell(
                        date = date,
                        dayOfMonth = date.dayOfMonth,
                        count = count,
                        taskCount = summary?.taskCount ?: 0,
                        habitCount = summary?.habitCount ?: 0
                    )
                }
            }
    }

    val monthHeaders = remember(startDate, endSunday) {
        HeatmapCalendarUtils.buildMonthHeaders(startDate, endSunday)
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("productivity_heatmap_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "📅 Consistency Heatmap",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${startDate.format(DateTimeFormatter.ofPattern("MMM d"))} – ${endSunday.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    HeatmapVisibilityEyeButton(
                        showDates = effectiveShowDates,
                        onToggle = { newShow ->
                            if (onToggleShowDates != null) {
                                onToggleShowDates.invoke(newShow)
                            } else {
                                internalShowDates = newShow
                            }
                        }
                    )
                    IconButton(
                        onClick = { weekOffset -= selectedRange.weeks },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Period")
                    }
                    if (weekOffset != 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .clickable { weekOffset = 0 }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Today", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    IconButton(
                        onClick = { weekOffset += selectedRange.weeks },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next Period")
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HeatmapMetric.entries.forEach { metric ->
                    FilterChip(
                        selected = selectedMetric == metric,
                        onClick = { selectedMetric = metric },
                        label = { Text("${metric.emoji} ${metric.displayName}", fontSize = 11.sp) },
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                HeatmapDateRange.entries.forEach { range ->
                    FilterChip(
                        selected = selectedRange == range,
                        onClick = { selectedRange = range },
                        label = { Text(range.displayName, fontSize = 11.sp) },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Box(modifier = Modifier.height(14.dp), contentAlignment = Alignment.Center) {
                        Text("", fontSize = 9.sp)
                    }
                    (1..7).forEach { dayNum ->
                        val dayLabel = when (dayNum) {
                            1 -> "Mon"
                            3 -> "Wed"
                            5 -> "Fri"
                            else -> ""
                        }
                        Box(
                            modifier = Modifier.size(18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dayLabel,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                heatmapWeeks.forEachIndexed { weekIndex, week ->
                    val monthLabel = monthHeaders.firstOrNull { it.weekIndex == weekIndex }?.label
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .height(14.dp)
                                .width(18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (monthLabel != null) {
                                Text(
                                    text = monthLabel,
                                    fontSize = 8.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }

                        week.forEach { cell ->
                            val cellColor = HeatmapCalendarUtils.resolveCellColor(cell.count, selectedPalette)
                            val isSelected = inspectedDay?.dateIso == cell.date.format(DateTimeFormatter.ISO_DATE)
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(cellColor)
                                    .then(
                                        if (isSelected) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp)) else Modifier
                                    )
                                    .clickable {
                                        val iso = cell.date.format(DateTimeFormatter.ISO_DATE)
                                        inspectedDay = dailyActivityMap.getValue(iso)
                                    }
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    if (effectiveShowDates) {
                                        Text(
                                            text = cell.dayOfMonth.toString(),
                                            fontSize = 9.sp,
                                            color = if (cell.count == 0) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text("Less", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    selectedPalette.levels.forEach { col ->
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(col)
                        )
                    }
                    Text("More", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    HeatmapPalette.entries.forEach { pal ->
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(pal.levels[3])
                                .then(
                                    if (selectedPalette == pal) {
                                        Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    } else Modifier
                                )
                                .clickable { selectedPalette = pal }
                        )
                    }
                }
            }

            AnimatedVisibility(visible = inspectedDay != null) {
                inspectedDay?.let { summary ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("heatmap_day_detail_card")
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = summary.dateIso,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = selectedPalette.levels[3].copy(alpha = 0.25f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, selectedPalette.levels[3])
                                    ) {
                                        Text(
                                            text = "${summary.totalCount} completed",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = selectedPalette.levels[3],
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { inspectedDay = null },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(16.dp))
                                }
                            }

                            if (summary.completedTasks.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Tasks (${summary.completedTasks.size}):", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                summary.completedTasks.forEach { t ->
                                    Text("• $t", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            if (summary.completedHabits.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Habits (${summary.completedHabits.size}):", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.tertiary)
                                summary.completedHabits.forEach { h ->
                                    Text("• $h", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            if (summary.totalCount == 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("No activity logged on this date.", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            }
        }
    }
}
