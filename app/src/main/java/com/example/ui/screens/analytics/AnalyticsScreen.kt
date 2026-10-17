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

    val allTasks = tasksState.allTasks
    val completedTasksCount = remember(allTasks) { allTasks.count { it.isCompletedForDate || it.isOneTimeCompleted } }
    val totalTasksCount = allTasks.size

    val categoryBreakdown = remember(allTasks) {
        allTasks.groupBy { it.category }.mapValues { it.value.size }
    }

    val priorityBreakdown = remember(allTasks) {
        allTasks.groupBy { it.priority }.mapValues { it.value.size }
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
                        text = "Real-time trends, consistency score & category breakdowns",
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

            // Category Breakdown Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Focus by Category",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    if (categoryBreakdown.isEmpty()) {
                        Text("No category data yet", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    } else {
                        categoryBreakdown.forEach { (cat, count) ->
                            val pct = if (totalTasksCount > 0) count.toFloat() / totalTasksCount.toFloat() else 0f
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(text = cat, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    Text(text = "$count tasks (${(pct * 100).toInt()}%)", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                LinearProgressIndicator(
                                    progress = { pct },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Priority Distribution Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Priority Distribution",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceAround,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(TaskPriority.P1, TaskPriority.P2, TaskPriority.P3, TaskPriority.P4).forEach { p ->
                            val count = priorityBreakdown[p] ?: 0
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Surface(
                                    color = p.composeColor.copy(alpha = 0.2f),
                                    shape = CircleShape,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(text = p.code, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = p.composeColor)
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "$count", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(text = p.displayName, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
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
