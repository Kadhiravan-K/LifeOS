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
            Color(0xFF27272A).copy(alpha = 0.4f), // Level 0 (empty)
            Color(0xFF0E4429),                    // Level 1
            Color(0xFF006D32),                    // Level 2
            Color(0xFF26A641),                    // Level 3
            Color(0xFF39D353)                     // Level 4 (peak)
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
    initialPalette: HeatmapPalette = HeatmapPalette.EMERALD
) {
    var selectedMetric by remember { mutableStateOf(initialMetric) }
    var selectedRange by remember { mutableStateOf(initialRange) }
    var selectedPalette by remember { mutableStateOf(initialPalette) }
    var weekOffset by remember { mutableIntStateOf(0) }
    var inspectedDay by remember { mutableStateOf<DayActivitySummary?>(null) }

    val today = remember { LocalDate.now() }
    val endSunday = remember(weekOffset) {
        // Find current or shifted week's end (Sunday)
        val baseDate = today.plusWeeks(weekOffset.toLong())
        baseDate.plusDays((7 - baseDate.dayOfWeek.value).toLong() % 7)
    }

    val totalDays = selectedRange.weeks * 7
    val startDate = remember(endSunday, selectedRange) {
        endSunday.minusDays((totalDays - 1).toLong())
    }

    // Precompute daily activity map
    val dailyActivityMap = remember(tasks, habits, startDate, endSunday) {
        val map = mutableMapOf<String, DayActivitySummary>()
        var curr = startDate
        while (!curr.isAfter(endSunday)) {
            val iso = curr.format(DateTimeFormatter.ISO_DATE)

            // Tasks completed on this date
            val matchingTasks = tasks.filter { t ->
                !t.isArchived && (
                    (t.completedDates.contains(iso)) ||
                    (t.isOneTimeCompleted && t.dueDateIso == iso)
                )
            }.map { it.title }

            // Habits completed on this date
            val matchingHabits = habits.filter { h ->
                h.completedDates.contains(iso)
            }.map { "${it.emoji} ${it.title}" }

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

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("productivity_heatmap_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with Period Navigation
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

            // User Customization Controls: Metric & Range Selection
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

            // Heatmap Grid: Horizontal scrolling columns of weeks (7 days per column)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Day of Week row headers (Mon, Wed, Fri)
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    (1..7).forEach { dayNum ->
                        val dayLabel = when (dayNum) {
                            1 -> "Mon"
                            3 -> "Wed"
                            5 -> "Fri"
                            else -> ""
                        }
                        Box(
                            modifier = Modifier.size(16.dp),
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

                // Week columns
                var datePointer = startDate
                for (w in 0 until selectedRange.weeks) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (d in 0 until 7) {
                            val iso = datePointer.format(DateTimeFormatter.ISO_DATE)
                            val summary = dailyActivityMap[iso]
                            val count = when (selectedMetric) {
                                HeatmapMetric.ALL -> summary?.totalCount ?: 0
                                HeatmapMetric.TASKS -> summary?.taskCount ?: 0
                                HeatmapMetric.HABITS -> summary?.habitCount ?: 0
                            }

                            val cellColor = when {
                                count == 0 -> selectedPalette.levels[0]
                                count == 1 -> selectedPalette.levels[1]
                                count == 2 -> selectedPalette.levels[2]
                                count == 3 -> selectedPalette.levels[3]
                                else -> selectedPalette.levels[4]
                            }

                            val isSelected = inspectedDay?.dateIso == iso

                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(cellColor)
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp))
                                        } else Modifier
                                    )
                                    .clickable { inspectedDay = summary }
                            )

                            datePointer = datePointer.plusDays(1)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Palette Selector & Intensity Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Legend
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

                // Palette picker chips
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

            // Interactive Day Tap Detail Sheet / Card
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
