package com.example.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.TaskPriority
import com.example.ui.components.CalendarHeatmapView
import com.example.ui.viewmodel.LifeViewModel

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin

enum class AnalysisChartType(val displayName: String, val emoji: String) {
    PIE("Pie Chart", "🥧"),
    RADAR("Spider / Radar", "🕸️"),
    BAR("Bar Chart", "📊")
}

enum class AnalysisDimension(val displayName: String, val emoji: String) {
    PRIORITY("Priority", "⚡"),
    PROJECT("Projects", "📁"),
    LABEL("Labels", "🏷️"),
    CATEGORY("Category", "📌")
}

data class AnalysisSlice(
    val label: String,
    val count: Int,
    val color: Color
)

@Composable
fun AnalyticsScreen(
    viewModel: LifeViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tasksState by viewModel.tasksUiState.collectAsStateWithLifecycle()
    val habitsState by viewModel.habitsUiState.collectAsStateWithLifecycle()
    val dashboardState by viewModel.dashboardUiState.collectAsStateWithLifecycle()
    val profile by viewModel.gamificationProfile.collectAsStateWithLifecycle()

    var selectedChartType by remember { mutableStateOf(AnalysisChartType.PIE) }
    var selectedDimension by remember { mutableStateOf(AnalysisDimension.PRIORITY) }

    val allTasks = tasksState.allTasks
    val completedTasksCount = remember(allTasks) { allTasks.count { it.isCompletedForDate || it.isOneTimeCompleted } }
    val totalTasksCount = allTasks.size

    val activeSlices: List<AnalysisSlice> = remember(allTasks, selectedDimension, tasksState.projects, tasksState.labels) {
        val palette = listOf(
            Color(0xFF3B82F6), // Blue
            Color(0xFF10B981), // Emerald
            Color(0xFFF59E0B), // Amber
            Color(0xFFEF4444), // Red
            Color(0xFF8B5CF6), // Purple
            Color(0xFFEC4899), // Pink
            Color(0xFF06B6D4), // Cyan
            Color(0xFF14B8A6), // Teal
            Color(0xFF64748B)  // Slate
        )

        when (selectedDimension) {
            AnalysisDimension.PRIORITY -> {
                listOf(
                    AnalysisSlice("P1 - Urgent", allTasks.count { it.priority == TaskPriority.P1 }, Color(0xFFEF4444)),
                    AnalysisSlice("P2 - High", allTasks.count { it.priority == TaskPriority.P2 }, Color(0xFFF59E0B)),
                    AnalysisSlice("P3 - Medium", allTasks.count { it.priority == TaskPriority.P3 }, Color(0xFF3B82F6)),
                    AnalysisSlice("P4 - Low", allTasks.count { it.priority == TaskPriority.P4 }, Color(0xFF6B7280))
                ).filter { it.count > 0 }
            }
            AnalysisDimension.PROJECT -> {
                if (allTasks.isEmpty()) emptyList()
                else {
                    val grouped = allTasks.groupBy { task ->
                        task.projectName?.ifBlank { null }
                            ?: tasksState.projects.find { it.id == task.projectId }?.name
                            ?: "General Inbox"
                    }
                    grouped.entries.mapIndexed { idx, entry ->
                        AnalysisSlice(entry.key, entry.value.size, palette[idx % palette.size])
                    }
                }
            }
            AnalysisDimension.LABEL -> {
                if (allTasks.isEmpty()) emptyList()
                else {
                    val labelCounts = mutableMapOf<String, Int>()
                    var noLabelCount = 0
                    allTasks.forEach { task ->
                        if (task.labels.isEmpty()) {
                            noLabelCount++
                        } else {
                            task.labels.forEach { lbl ->
                                labelCounts[lbl.name] = (labelCounts[lbl.name] ?: 0) + 1
                            }
                        }
                    }
                    val result = labelCounts.entries.mapIndexed { idx, entry ->
                        AnalysisSlice(entry.key, entry.value, palette[idx % palette.size])
                    }.toMutableList()
                    if (noLabelCount > 0) {
                        result.add(AnalysisSlice("Unlabeled", noLabelCount, Color(0xFF94A3B8)))
                    }
                    result
                }
            }
            AnalysisDimension.CATEGORY -> {
                if (allTasks.isEmpty()) emptyList()
                else {
                    val grouped = allTasks.groupBy { it.category.ifBlank { "Routine" } }
                    grouped.entries.mapIndexed { idx, entry ->
                        AnalysisSlice(entry.key, entry.value.size, palette[idx % palette.size])
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
                .testTag("analytics_screen")
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "📊 Productivity Analytics",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Real-time trends, consistency score & customized visual breakdowns",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Stats Grid (4 Cards)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                StatCard(
                    title = "Tasks Done",
                    value = "$completedTasksCount / $totalTasksCount",
                    icon = "✅",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Habits Streak",
                    value = "${habitsState.habits.maxOfOrNull { it.currentStreak } ?: 0} days",
                    icon = "🔥",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                StatCard(
                    title = "Hunter Rank",
                    value = "Rank ${profile.hunterRank.rankCode}",
                    icon = "🏆",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Earned Coins",
                    value = "${profile.currentCoins} 🪙",
                    icon = "🪙",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 35-Day Consistency Heatmap
            Text(
                text = "Consistency Heatmap (Last 35 Days)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            CalendarHeatmapView(days = habitsState.heatmapDays)

            Spacer(modifier = Modifier.height(16.dp))

            // Unified Interactive Productivity Analysis Chart Card (Single customizable chart)
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("productivity_analysis_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📈 Productivity Analysis",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Customizable single-chart visualization across priority, projects, labels, or category",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 1. Chart Type Selector (Pie, Radar/Spider, Bar)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AnalysisChartType.entries.forEach { chartType ->
                            FilterChip(
                                selected = selectedChartType == chartType,
                                onClick = { selectedChartType = chartType },
                                label = { Text("${chartType.emoji} ${chartType.displayName}", fontSize = 12.sp) },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 2. Data Dimension Selector (Priority, Projects, Labels, Category)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AnalysisDimension.entries.forEach { dim ->
                            FilterChip(
                                selected = selectedDimension == dim,
                                onClick = { selectedDimension = dim },
                                label = { Text("${dim.emoji} ${dim.displayName}", fontSize = 12.sp) },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3. Single Chart Visualization Area
                    val totalCount = activeSlices.sumOf { it.count }
                    if (totalCount == 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No tasks found for ${selectedDimension.displayName}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            when (selectedChartType) {
                                AnalysisChartType.PIE -> {
                                    val surfaceColor = MaterialTheme.colorScheme.surface
                                    Canvas(modifier = Modifier.size(190.dp)) {
                                        val diameter = size.minDimension
                                        val radius = diameter / 2f
                                        val center = Offset(size.width / 2f, size.height / 2f)
                                        var startAngle = -90f
                                        activeSlices.forEach { slice ->
                                            val sweep = (slice.count.toFloat() / totalCount.toFloat()) * 360f
                                            drawArc(
                                                color = slice.color,
                                                startAngle = startAngle,
                                                sweepAngle = sweep,
                                                useCenter = true,
                                                topLeft = Offset(center.x - radius, center.y - radius),
                                                size = Size(diameter, diameter)
                                            )
                                            startAngle += sweep
                                        }
                                        // Modern Donut center
                                        drawCircle(
                                            color = surfaceColor,
                                            radius = radius * 0.52f,
                                            center = center
                                        )
                                    }
                                }

                                AnalysisChartType.RADAR -> {
                                    val gridColor = MaterialTheme.colorScheme.outlineVariant
                                    val primaryColor = MaterialTheme.colorScheme.primary
                                    val n = activeSlices.size.coerceAtLeast(3)
                                    val maxVal = activeSlices.maxOfOrNull { it.count }?.toFloat()?.coerceAtLeast(1f) ?: 1f

                                    Canvas(modifier = Modifier.size(200.dp)) {
                                        val center = Offset(size.width / 2f, size.height / 2f)
                                        val maxR = size.minDimension / 2f * 0.85f

                                        // Web grid concentric rings
                                        listOf(0.33f, 0.66f, 1f).forEach { scale ->
                                            val gridPath = Path()
                                            for (i in 0 until n) {
                                                val angle = (2 * Math.PI * i / n - Math.PI / 2).toFloat()
                                                val x = center.x + maxR * scale * cos(angle)
                                                val y = center.y + maxR * scale * sin(angle)
                                                if (i == 0) gridPath.moveTo(x, y) else gridPath.lineTo(x, y)
                                            }
                                            gridPath.close()
                                            drawPath(gridPath, color = gridColor, style = Stroke(width = 1.dp.toPx()))
                                        }

                                        // Spoke lines
                                        for (i in 0 until n) {
                                            val angle = (2 * Math.PI * i / n - Math.PI / 2).toFloat()
                                            val x = center.x + maxR * cos(angle)
                                            val y = center.y + maxR * sin(angle)
                                            drawLine(color = gridColor, start = center, end = Offset(x, y), strokeWidth = 1.dp.toPx())
                                        }

                                        // Data polygon
                                        val dataPath = Path()
                                        val points = mutableListOf<Offset>()
                                        for (i in 0 until n) {
                                            val count = if (i < activeSlices.size) activeSlices[i].count.toFloat() else 0f
                                            val fraction = count / maxVal
                                            val angle = (2 * Math.PI * i / n - Math.PI / 2).toFloat()
                                            val r = maxR * fraction
                                            val x = center.x + r * cos(angle)
                                            val y = center.y + r * sin(angle)
                                            val pt = Offset(x, y)
                                            points.add(pt)
                                            if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
                                        }
                                        dataPath.close()
                                        drawPath(dataPath, color = primaryColor.copy(alpha = 0.35f), style = Fill)
                                        drawPath(dataPath, color = primaryColor, style = Stroke(width = 2.5.dp.toPx()))
                                        points.forEach { pt ->
                                            drawCircle(color = primaryColor, radius = 4.dp.toPx(), center = pt)
                                            drawCircle(color = Color.White, radius = 2.dp.toPx(), center = pt)
                                        }
                                    }
                                }

                                AnalysisChartType.BAR -> {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        activeSlices.forEach { slice ->
                                            val pct = slice.count.toFloat() / totalCount.toFloat()
                                            Column {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(8.dp)
                                                                .clip(CircleShape)
                                                                .background(slice.color)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(text = slice.label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                                    }
                                                    Text(
                                                        text = "${slice.count} (${(pct * 100).toInt()}%)",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                LinearProgressIndicator(
                                                    progress = { pct },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(8.dp)
                                                        .clip(RoundedCornerShape(4.dp)),
                                                    color = slice.color,
                                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 4. Interactive Legend (for Pie & Radar)
                        if (selectedChartType != AnalysisChartType.BAR) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                activeSlices.chunked(2).forEach { rowSlices ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        rowSlices.forEach { slice ->
                                            val pct = ((slice.count.toFloat() / totalCount.toFloat()) * 100).toInt()
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(10.dp)
                                                        .clip(CircleShape)
                                                        .background(slice.color)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "${slice.label}: ${slice.count} ($pct%)",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = icon, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            Text(text = title, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
        }
    }
}
