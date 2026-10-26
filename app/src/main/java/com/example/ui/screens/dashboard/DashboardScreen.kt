package com.example.ui.screens.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.HoneycombShortcut
import com.example.domain.model.ShortcutType
import com.example.domain.model.TaskItem
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.example.ui.components.CustomizeDashboardDialog
import com.example.ui.components.HoneycombContextMenuDialog
import com.example.ui.components.HoneycombTaskGrid
import com.example.ui.components.PriorityBadge
import com.example.ui.components.QuickHydrationDialog
import com.example.ui.components.TaskCompletionCircle
import com.example.ui.components.WaterTrackerCard
import com.example.ui.screens.templates.TemplateBrowserDialog
import com.example.ui.viewmodel.DashboardUiState
import com.example.ui.viewmodel.LifeViewModel
import com.example.utils.DateUtils

@Composable
fun DashboardScreen(
    viewModel: LifeViewModel,
    uiState: DashboardUiState,
    onNavigateToTasks: () -> Unit,
    onNavigateToHabits: () -> Unit,
    onNavigateToChallenge: () -> Unit,
    onNavigateToCalendar: (() -> Unit)? = null,
    onNavigateToPlanning: (() -> Unit)? = null,
    onNavigateToGamification: (() -> Unit)? = null,
    onNavigateToAnalytics: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val animatedProgress by animateFloatAsState(
        targetValue = uiState.overallProgressPercentage,
        label = "overall_progress"
    )

    val appearance by viewModel.honeycombAppearance.collectAsStateWithLifecycle()
    val gamificationProfile by viewModel.gamificationProfile.collectAsStateWithLifecycle()
    val allPlans by viewModel.allPlansFlow.collectAsStateWithLifecycle()
    val activeTheme = com.example.ui.theme.engine.LocalLifeTheme.current
    var contextMenuShortcut by remember { mutableStateOf<HoneycombShortcut?>(null) }
    var showCustomizeDialog by remember { mutableStateOf(false) }
    var showHydrationDialog by remember { mutableStateOf(false) }
    var showTemplateBrowserDialog by remember { mutableStateOf(false) }

    if (uiState.isLoading) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp)
                .testTag("dashboard_loading"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(activeTheme.shapes.buttonShape)
                        .background(activeTheme.colors.badgeBackground)
                        .border(1.dp, activeTheme.colors.outline, activeTheme.shapes.buttonShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = activeTheme.decoration.motifEmoji, fontSize = 28.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 3.dp,
                    color = activeTheme.colors.primary
                )
            }
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("dashboard_screen")
    ) {
        // --- 1. Header & Greeting ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(activeTheme.shapes.buttonShape)
                        .background(activeTheme.colors.badgeBackground)
                        .border(1.dp, activeTheme.colors.outline, activeTheme.shapes.buttonShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = activeTheme.decoration.motifEmoji, fontSize = 22.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = uiState.greeting,
                        style = activeTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${uiState.todayDateFormatted} • ${activeTheme.decoration.tagline}",
                        style = activeTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Overall Progress Circle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(54.dp)
            ) {
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    strokeWidth = 5.dp,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxSize()
                )
                Text(
                    text = "${(animatedProgress * 100).toInt()}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- Current & Next Task Live Focus Card ---
        if (uiState.currentTask != null || uiState.nextTask != null) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.currentTask != null)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    else
                        MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    if (uiState.currentTask != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Current",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "NOW IN PROGRESS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${uiState.currentTask.emoji} ${uiState.currentTask.title}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "⏰ ${uiState.currentTask.dueTime} (${uiState.currentTask.durationMinutes} min) • ${uiState.currentTask.category}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (uiState.currentTask != null && uiState.nextTask != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    if (uiState.nextTask != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "⏳ Next Up:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${uiState.nextTask.emoji} ${uiState.nextTask.title} at ${uiState.nextTask.dueTime}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // --- ⬡ HONEYCOMB TASK DASHBOARD ---
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("honeycomb_dashboard_card")
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "⬡",
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = activeTheme.decoration.honeycombTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (uiState.isHoneycombEditMode) "Layout Edit Mode active" else "Tap to toggle • Long press for options",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!uiState.isHoneycombEditMode) {
                            TextButton(
                                onClick = { showCustomizeDialog = true },
                                modifier = Modifier.testTag("customize_honeycomb_button")
                            ) {
                                Text("Customize", fontSize = 12.sp)
                            }
                            IconButton(
                                onClick = { viewModel.setHoneycombEditMode(true) },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("edit_honeycomb_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Layout",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                HoneycombTaskGrid(
                    shortcuts = uiState.honeycombShortcuts,
                    appearance = appearance,
                    isEditMode = uiState.isHoneycombEditMode,
                    onShortcutClick = { shortcut ->
                        if (shortcut.type == ShortcutType.WATER) {
                            showHydrationDialog = true
                        } else {
                            viewModel.toggleHoneycombShortcut(shortcut)
                        }
                    },
                    onShortcutLongClick = { shortcut ->
                        contextMenuShortcut = shortcut
                    },
                    onRemoveShortcut = { id ->
                        viewModel.removeHoneycombShortcut(id)
                    },
                    onSwapShortcuts = { id1, id2 ->
                        viewModel.swapHoneycombShortcuts(id1, id2)
                    },
                    onMoveShortcutToCoord = { id, q, r ->
                        viewModel.moveHoneycombShortcutToCoord(id, q, r)
                    },
                    onAddShortcutClick = {
                        showCustomizeDialog = true
                    },
                    onResetLayoutClick = {
                        viewModel.resetHoneycombLayoutToDefault()
                    },
                    onDoneEditClick = {
                        viewModel.setHoneycombEditMode(false)
                    },
                    onApplyTemplate = { tmpl ->
                        viewModel.applyHoneycombTemplate(tmpl)
                    },
                    onBrowseTemplatesClick = {
                        showTemplateBrowserDialog = true
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- Overdue Tasks Banner ---
        if (uiState.overdueTasks.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = "Overdue",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "${uiState.overdueTasks.size} Overdue Task(s)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF991B1B)
                            )
                            Text(
                                text = "Catch up to keep your life schedule clean",
                                fontSize = 11.sp,
                                color = Color(0xFF7F1D1D)
                            )
                        }
                    }
                    FilledTonalButton(
                        onClick = onNavigateToTasks,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Review", fontSize = 11.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // --- First Launch Onboarding Banner ---
        if (!uiState.hasCompletedOnboarding) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("first_launch_banner")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "🚀", fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Welcome to LifeOS!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Your personal life operating system is ready. Start by adding your first task or browse our 11+ curated master templates.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { showTemplateBrowserDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("onboarding_browse_templates_button")
                        ) {
                            Text("Browse Templates", fontSize = 12.sp)
                        }
                        FilledTonalButton(
                            onClick = onNavigateToTasks,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("+ Create Task", fontSize = 12.sp)
                        }
                        TextButton(
                            onClick = { viewModel.setHasCompletedOnboarding(true) },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Dismiss", fontSize = 12.sp)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // --- 2. Today's Top 3 Priorities ---
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🎯", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = activeTheme.decoration.taskSectionTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "View All",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onNavigateToTasks() }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (uiState.topPriorities.isEmpty()) {
                    Text(
                        text = "No pending priority tasks for today. Tap + to add!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    uiState.topPriorities.forEach { task ->
                        val animEnabled by viewModel.completionAnimationEnabled.collectAsStateWithLifecycle()
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .padding(vertical = 6.dp)
                        ) {
                            TaskCompletionCircle(
                                isCompleted = task.isCompletedForDate,
                                onToggle = { viewModel.toggleTaskCompletion(task.id, uiState.todayDateIso) },
                                activeColor = MaterialTheme.colorScheme.primary,
                                isAnimationEnabled = animEnabled,
                                size = 26.dp
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.toggleTaskCompletion(task.id, uiState.todayDateIso) }
                            ) {
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Medium,
                                        textDecoration = if (task.isCompletedForDate) TextDecoration.LineThrough else null
                                    ),
                                    color = if (task.isCompletedForDate)
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    else
                                        MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (!task.dueTime.isNullOrBlank()) "⏰ ${task.dueTime} (${task.durationMinutes}m) • ${task.category}" else task.category,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))
                            PriorityBadge(task.priority)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- Time Grouping Agenda (Morning, Afternoon, Evening) ---
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Today's Schedule Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                TimeGroupSection("🌅 Morning (5:00 AM – 12:00 PM)", uiState.morningTasks, uiState.todayDateIso, viewModel)
                TimeGroupSection("☀️ Afternoon (12:00 PM – 5:00 PM)", uiState.afternoonTasks, uiState.todayDateIso, viewModel)
                TimeGroupSection("🌙 Evening (5:00 PM – 9:30 PM)", uiState.eveningTasks, uiState.todayDateIso, viewModel)
                if (uiState.noTimeTasks.isNotEmpty()) {
                    TimeGroupSection("📌 Anytime Today", uiState.noTimeTasks, uiState.todayDateIso, viewModel)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- 3. 75-Day Challenge Snapshot ---
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToChallenge() }
                .testTag("dashboard_challenge_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFEE2E2))
                        ) {
                            Text(text = "🔥", fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "75-Day Discipline Challenge",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Day ${uiState.challengeDayNumber} of ${uiState.challengeTargetDays} • ${uiState.challengeRulesCompletedToday}/${uiState.challengeRulesTotal} Done Today",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Go to challenge",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LinearProgressIndicator(
                    progress = { uiState.challengeProgressPercentage },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFFDC2626),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- 4. Daily Habits Snapshot ---
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "⚡", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Daily Habits (${uiState.habitsCompletedTodayCount}/${uiState.totalHabitsCount})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Habit Tracker",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onNavigateToHabits() }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (uiState.habits.isEmpty()) {
                    Text(
                        text = "No habits configured yet. Install default habits or add your own!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(uiState.habits, key = { it.id }) { habit ->
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (habit.isCompletedToday)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { viewModel.toggleHabit(habit.id, uiState.todayDateIso) }
                            ) {
                                if (habit.isCompletedToday) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Done",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                } else {
                                    Text(text = habit.emoji, fontSize = 20.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- 5. Water Intake Card ---
        WaterTrackerCard(
            currentMl = uiState.waterCurrentMl,
            goalMl = uiState.waterGoalMl,
            onAdd = { delta -> viewModel.addWater(delta) },
            onReset = { viewModel.resetWater() }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // --- 6. Hunter Ranks & Gamification Snapshot ---
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToGamification?.invoke() }
                .testTag("dashboard_gamification_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        ) {
                            Text(text = "🏆", fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Hunter Rank: ${gamificationProfile.hunterRank}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "• Level ${gamificationProfile.currentLevel}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = "🪙 ${gamificationProfile.currentCoins} Coins Available • ${gamificationProfile.currentXp % 100}/100 XP to next level",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Hunter Ranks",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                val xpInCurrentLevel = (gamificationProfile.currentXp % 100).toFloat() / 100f
                LinearProgressIndicator(
                    progress = { xpInCurrentLevel },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- 7. Strategic Horizon Planning Snapshot ---
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToPlanning?.invoke() }
                .testTag("dashboard_planning_card")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEF3C7))
                    ) {
                        Text(text = "🎯", fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Multi-Horizon Planning",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${allPlans.size} Goals & Reviews across Horizons",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Planning",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- 8. Analytics & Productivity Trends Card ---
        if (onNavigateToAnalytics != null) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToAnalytics.invoke() }
                    .testTag("dashboard_analytics_card")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE0E7FF))
                        ) {
                            Text(text = "📊", fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Productivity Analytics",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Completion rates, heatmaps & time trends",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Analytics",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // --- Honeycomb Context Menu Modal ---
    if (contextMenuShortcut != null) {
        val sc = contextMenuShortcut!!
        HoneycombContextMenuDialog(
            shortcut = sc,
            onDismiss = { contextMenuShortcut = null },
            onToggleCompletion = {
                viewModel.toggleHoneycombShortcut(sc)
            },
            onEnterEditMode = {
                viewModel.setHoneycombEditMode(true)
            },
            onChangeEmoji = { newEmoji ->
                viewModel.updateHoneycombShortcutIcon(sc.id, newEmoji)
            },
            onChangeColor = { newColor ->
                viewModel.updateHoneycombShortcutColor(sc.id, newColor)
            },
            onRemoveFromDashboard = {
                viewModel.removeHoneycombShortcut(sc.id)
            },
            onRenameTile = { newName ->
                viewModel.updateShortcutCustomTileName(sc.id, newName)
            },
            onChangeShape = { newShape ->
                viewModel.updateHoneycombShortcutShape(sc.id, newShape)
            },
            onChangeColorSource = { source, customColor ->
                viewModel.updateHoneycombShortcutColorSource(sc.id, source, customColor)
            }
        )
    }

    // --- Customize Dashboard Modal ---
    if (showCustomizeDialog) {
        val allTasks by viewModel.allTasks.collectAsStateWithLifecycle()
        val allHabits by viewModel.habits.collectAsStateWithLifecycle()
        val challengeState by viewModel.challengeState.collectAsStateWithLifecycle()

        CustomizeDashboardDialog(
            currentShortcuts = uiState.honeycombShortcuts,
            allTasks = allTasks,
            allHabits = allHabits,
            challengeState = challengeState,
            onAddShortcut = { newShortcut ->
                viewModel.addHoneycombShortcut(newShortcut)
            },
            onRemoveShortcut = { id ->
                viewModel.removeHoneycombShortcut(id)
            },
            onResetDefaults = {
                viewModel.resetHoneycombLayoutToDefault()
            },
            onDismiss = { showCustomizeDialog = false }
        )
    }

    // --- Quick Hydration Modal ---
    if (showHydrationDialog) {
        QuickHydrationDialog(
            currentMl = uiState.waterCurrentMl,
            goalMl = uiState.waterGoalMl,
            onAddWater = { delta ->
                viewModel.addWater(delta)
            },
            onResetWater = {
                viewModel.resetWater()
            },
            onDismiss = { showHydrationDialog = false }
        )
    }

    if (showTemplateBrowserDialog) {
        val installedTemplateIds by viewModel.installedTemplateIds.collectAsStateWithLifecycle()
        TemplateBrowserDialog(
            installedTemplateIds = installedTemplateIds,
            onInstallTemplate = { template, selectedTasks, selectedHabits, onDone ->
                viewModel.installTemplate(template, selectedTasks, selectedHabits, onDone)
            },
            onDismiss = { showTemplateBrowserDialog = false }
        )
    }
}


@Composable
private fun TimeGroupSection(
    title: String,
    tasks: List<TaskItem>,
    todayDateIso: String,
    viewModel: LifeViewModel
) {
    if (tasks.isEmpty()) return

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        tasks.forEach { task ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.toggleTaskCompletion(task.id, todayDateIso) }
                    .padding(vertical = 4.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(if (task.isCompletedForDate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (task.isCompletedForDate) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
                    } else {
                        Text(text = task.emoji, fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${if (!task.dueTime.isNullOrBlank()) "${task.dueTime} — " else ""}${task.title}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        textDecoration = if (task.isCompletedForDate) TextDecoration.LineThrough else null
                    ),
                    color = if (task.isCompletedForDate) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (task.durationMinutes > 0) {
                    Text(
                        text = "${task.durationMinutes}m",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}


