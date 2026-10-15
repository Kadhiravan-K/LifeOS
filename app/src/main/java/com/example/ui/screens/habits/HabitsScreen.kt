package com.example.ui.screens.habits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import com.example.domain.model.HabitItem
import com.example.ui.components.CalendarHeatmapView
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.HabitCard
import com.example.ui.components.HabitEditModal
import com.example.ui.viewmodel.HabitsUiState
import com.example.ui.viewmodel.LifeViewModel

@Composable
fun HabitsScreen(
    viewModel: LifeViewModel,
    uiState: HabitsUiState,
    modifier: Modifier = Modifier
) {
    var showHabitModal by remember { mutableStateOf(false) }
    var editingHabit by remember { mutableStateOf<HabitItem?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingHabit = null
                    showHabitModal = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_create_habit")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Habit")
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("habits_screen")
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Habit Master",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${uiState.completedTodayCount} of ${uiState.totalCount} completed today (${(uiState.completionPercentage * 100).toInt()}%)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Calendar Heatmap Card
            CalendarHeatmapView(days = uiState.heatmapDays)

            Spacer(modifier = Modifier.height(14.dp))

            // Habits List
            if (uiState.habits.isEmpty()) {
                EmptyStateCard(
                    emoji = "🧘",
                    title = "No habits tracked yet",
                    description = "Install the 9 default discipline habits (Surya Namaskaram, reading, hydration, 5:30 AM wake up) or create your own!",
                    actionButtonText = "Install Default Habits",
                    onActionClick = { viewModel.installDefaultRoutines() }
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.habits, key = { it.id }) { habit ->
                        HabitCard(
                            habit = habit,
                            onToggle = {
                                viewModel.toggleHabit(habit.id, uiState.selectedDateIso)
                            },
                            onEdit = {
                                editingHabit = habit
                                showHabitModal = true
                            },
                            onDelete = {
                                viewModel.deleteHabit(habit.id)
                            }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }

    if (showHabitModal) {
        HabitEditModal(
            initialHabit = editingHabit,
            onDismiss = {
                showHabitModal = false
                editingHabit = null
            },
            onSave = { title, emoji, description, reminderTime ->
                viewModel.saveHabit(
                    id = editingHabit?.id ?: 0L,
                    title = title,
                    emoji = emoji,
                    description = description,
                    reminderTime = reminderTime
                )
                showHabitModal = false
                editingHabit = null
            }
        )
    }
}
