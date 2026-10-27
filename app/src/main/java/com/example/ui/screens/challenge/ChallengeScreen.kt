package com.example.ui.screens.challenge

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Lock
import androidx.compose.ui.draw.alpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FilledTonalButton
import com.example.domain.model.ChallengeState
import com.example.domain.model.DefaultChallengeRules
import com.example.ui.components.CustomizeChallengePresetsDialog
import com.example.ui.components.RestartChallengeDialog
import com.example.ui.viewmodel.LifeViewModel
import com.example.utils.DateUtils

@Composable
fun ChallengeScreen(
    viewModel: LifeViewModel,
    challenge: ChallengeState,
    modifier: Modifier = Modifier
) {
    var showRestartDialog by remember { mutableStateOf(false) }
    var showPresetsDialog by remember { mutableStateOf(false) }
    var selectedDayNumber by remember(challenge.currentDayNumber) { mutableStateOf(challenge.currentDayNumber) }
    val todayIso = DateUtils.todayString()
    val selectedDateIso = DateUtils.addDays(challenge.startDateIso, selectedDayNumber - 1)
    val isToday = selectedDateIso == todayIso
    val isFutureDay = DateUtils.daysBetween(todayIso, selectedDateIso) > 0
    val selectedLog = challenge.logs[selectedDateIso]
    val completedRulesForDay = selectedLog?.completedRuleIds ?: emptySet()
    val allRulesDoneForDay = challenge.rules.isNotEmpty() && challenge.rules.all { completedRulesForDay.contains(it.id) }

    val animatedProgress by animateFloatAsState(
        targetValue = challenge.progressPercentage,
        label = "challenge_progress"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("challenge_screen")
    ) {
        // Title & Program label
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = "75-Day Challenge",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Customized 75-Day Discipline Program",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilledTonalButton(
                    onClick = { showPresetsDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("customize_challenge_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Presets",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Presets", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = { showRestartDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.5f)),
                    modifier = Modifier.testTag("restart_challenge_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = "Restart",
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Restart", fontSize = 12.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Hero Progress Banner
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "DAY ${challenge.currentDayNumber}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFDC2626)
                        )
                        Text(
                            text = "Started: ${DateUtils.formatDisplayDate(challenge.startDateIso)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Streak Badge
                    Surface(
                        color = Color(0xFFFEE2E2),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(text = "🔥", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${challenge.currentStreak} Day Streak",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626),
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Progress Bar
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    color = Color(0xFFDC2626),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${challenge.completedDaysCount} Days Completed",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${(challenge.progressPercentage * 100).toInt()}% Total Progress",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Milestones Banner
        val currentMilestone = DefaultChallengeRules.milestones.entries
            .filter { challenge.currentDayNumber >= it.key }
            .maxByOrNull { it.key }

        if (currentMilestone != null) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Milestone",
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = currentMilestone.value,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF92400E)
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // 75-Day Interactive Timeline Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Day Selector (1 to ${challenge.targetDays})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (selectedDayNumber != challenge.currentDayNumber) {
                TextButton(
                    onClick = { selectedDayNumber = challenge.currentDayNumber }
                ) {
                    Text("Jump to Today", fontSize = 12.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))

        val daysRange = (1..challenge.targetDays).toList()
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(daysRange) { dayNum ->
                val dayDate = DateUtils.addDays(challenge.startDateIso, dayNum - 1)
                val dayLog = challenge.logs[dayDate]
                val isDone = challenge.rules.isNotEmpty() && challenge.rules.all { dayLog?.completedRuleIds?.contains(it.id) == true }
                val isSelected = dayNum == selectedDayNumber
                val isDayToday = dayDate == todayIso
                val isDayFuture = DateUtils.daysBetween(todayIso, dayDate) > 0

                val bgColor = when {
                    isSelected -> Color(0xFFDC2626)
                    isDone -> Color(0xFFDC2626).copy(alpha = 0.18f)
                    isDayFuture -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                val textColor = when {
                    isSelected -> Color.White
                    isDone -> Color(0xFFDC2626)
                    isDayFuture -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    else -> MaterialTheme.colorScheme.onSurface
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(width = 46.dp, height = 52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(bgColor)
                        .clickable { selectedDayNumber = dayNum }
                        .then(
                            if (isDayToday && !isSelected)
                                Modifier.border(1.5.dp, Color(0xFFDC2626), RoundedCornerShape(12.dp))
                            else Modifier
                        )
                        .testTag("challenge_day_tab_$dayNum")
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isDayToday) "TODAY" else "D$dayNum",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        if (isDone) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Day Complete",
                                tint = textColor,
                                modifier = Modifier.size(16.dp)
                            )
                        } else if (isDayFuture) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Future Day",
                                tint = textColor,
                                modifier = Modifier.size(14.dp)
                            )
                        } else {
                            val doneCount = dayLog?.completedRuleIds?.size ?: 0
                            Text(
                                text = "$doneCount/${challenge.rules.size}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textColor
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Selected Day Status Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    allRulesDoneForDay -> Color(0xFFDC2626).copy(alpha = 0.12f)
                    isFutureDay -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    else -> MaterialTheme.colorScheme.surface
                }
            ),
            border = if (allRulesDoneForDay)
                androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.4f))
            else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(14.dp)
            ) {
                Text(
                    text = when {
                        allRulesDoneForDay -> "🔥"
                        isFutureDay -> "🔒"
                        else -> "📋"
                    },
                    fontSize = 26.sp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            allRulesDoneForDay -> "Day $selectedDayNumber Complete!"
                            isFutureDay -> "Day $selectedDayNumber: Future Day"
                            else -> "Day $selectedDayNumber: ${completedRulesForDay.size} of ${challenge.rules.size} Rules Completed"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (allRulesDoneForDay) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = when {
                            allRulesDoneForDay -> "All discipline criteria met. Unbroken streak preserved!"
                            isFutureDay -> "Future days cannot be pre-recorded. Check back on ${DateUtils.formatDisplayDate(selectedDateIso)}."
                            else -> "Tap the completion circles below to mark today's rules."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Discipline Rules with Interactive Circular Completion Indicator
        Text(
            text = "Day $selectedDayNumber Checklist",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Every rule must be completed each day to keep your streak unbroken.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(10.dp))

        challenge.rules.forEach { rule ->
            val isChecked = completedRulesForDay.contains(rule.id)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isChecked)
                        Color(0xFFDC2626).copy(alpha = 0.08f)
                    else
                        MaterialTheme.colorScheme.surface
                ),
                border = if (isChecked)
                    androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFDC2626).copy(alpha = 0.35f))
                else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isChecked) 0.dp else 1.5.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .clickable(enabled = !isFutureDay) {
                        viewModel.toggleChallengeRule(rule.id, selectedDateIso)
                    }
                    .testTag("challenge_rule_${rule.id}")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(14.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isFutureDay -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    isChecked -> Color(0xFFDC2626)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                            .clickable(enabled = !isFutureDay) {
                                viewModel.toggleChallengeRule(rule.id, selectedDateIso)
                            }
                            .testTag("challenge_rule_toggle_${rule.id}")
                    ) {
                        if (isChecked) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Done",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        } else {
                            Text(
                                text = rule.emoji,
                                fontSize = 22.sp,
                                modifier = Modifier.alpha(if (isFutureDay) 0.4f else 1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = rule.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                textDecoration = if (isChecked) TextDecoration.LineThrough else null
                            ),
                            color = if (isChecked)
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            else if (isFutureDay)
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = rule.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isFutureDay) 0.45f else 1f)
                        )
                    }

                    if (isFutureDay) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Locked",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Locked",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    } else if (isChecked) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFDC2626).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Done ✓",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = "Tap Circle",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }

    if (showRestartDialog) {
        RestartChallengeDialog(
            onDismiss = { showRestartDialog = false },
            onConfirmRestart = { newStartDateIso ->
                viewModel.restartChallenge(newStartDateIso)
                showRestartDialog = false
            }
        )
    }

    if (showPresetsDialog) {
        CustomizeChallengePresetsDialog(
            challengeState = challenge,
            onApplyPreset = { preset, resetProgress ->
                viewModel.applyChallengePreset(preset, resetProgress)
            },
            onUpdateRules = { rules, resetProgress ->
                viewModel.updateChallengeRules(rules, resetProgress)
            },
            onDismiss = { showPresetsDialog = false }
        )
    }
}

