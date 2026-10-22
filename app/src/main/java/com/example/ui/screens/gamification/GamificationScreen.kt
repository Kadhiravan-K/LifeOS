package com.example.ui.screens.gamification

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.AchievementItem
import com.example.domain.model.RewardItem
import com.example.ui.viewmodel.LifeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamificationScreen(
    viewModel: LifeViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile by viewModel.gamificationProfile.collectAsStateWithLifecycle()
    val achievements by viewModel.allAchievements.collectAsStateWithLifecycle()
    val rewards by viewModel.activeRewards.collectAsStateWithLifecycle()
    val habitStreaks by viewModel.habitStreaks.collectAsStateWithLifecycle()
    val taskStreaks by viewModel.taskStreaks.collectAsStateWithLifecycle()
    val isPremiumActive by viewModel.isPremiumActive.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Achievements, 1 = Rewards Shop
    var achievementFilter by remember { mutableStateOf("ALL") } // "ALL", "UNLOCKED", "LOCKED"
    var showAddRewardDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            if (selectedTab == 1) {
                FloatingActionButton(
                    onClick = {
                        if (!isPremiumActive) {
                            Toast.makeText(context, "LifeOS Premium required for Rewards Shop", Toast.LENGTH_SHORT).show()
                        } else {
                            showAddRewardDialog = true
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("fab_add_reward")
                ) {
                    Icon(if (isPremiumActive) Icons.Default.Add else Icons.Default.Lock, contentDescription = "Add Reward")
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("gamification_screen")
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
                        text = "🏆 Hunter Ranks & Rewards",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Gamify your daily habits, discipline, and execution",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Premium-only Gamification Banner
            if (!isPremiumActive) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("gamification_premium_banner")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Premium Exclusive",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "⭐ LifeOS Premium Feature",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Hunter Ranks, XP tracking, streaks, and Reward Shop are premium perks. All core productivity tools (tasks, habits, challenges, calendar) remain 100% free.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = { viewModel.toggleSimulatedPremium(true) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Unlock Premium", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Profile Card (Hunter Rank & Coins)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
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
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(Color(profile.hunterRank.badgeColorHex))
                            ) {
                                Text(
                                    text = profile.hunterRank.rankCode,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = profile.hunterRank.title,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Level ${profile.currentLevel} • Rank ${profile.hunterRank.rankCode}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Coin balance
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFFEF3C7)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(text = "🪙", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${profile.currentCoins} Coins",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // XP Progress bar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "XP: ${profile.xpProgressInLevel} / ${profile.xpForCurrentLevel}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "Total XP: ${profile.currentXp}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { profile.levelProgressFraction },
                        color = Color(profile.hunterRank.badgeColorHex),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Streak Summary Cards
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🔥", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Habit Streak",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${habitStreaks.currentStreak} days",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEA580C)
                        )
                        Text(
                            text = "Best: ${habitStreaks.bestStreak} days",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "⚡", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Task Streak",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${taskStreaks.currentStreak} days",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD97706)
                        )
                        Text(
                            text = "Best: ${taskStreaks.bestStreak} days",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tabs: Achievements vs Rewards Shop
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Achievements (${achievements.count { it.isUnlocked }}/${achievements.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Rewards Shop (${rewards.size})") }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (selectedTab == 0) {
                // Filter Chips for achievements
                val unlockedCount = achievements.count { it.isUnlocked }
                val lockedCount = achievements.size - unlockedCount
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    FilterChip(
                        selected = achievementFilter == "ALL",
                        onClick = { achievementFilter = "ALL" },
                        label = { Text("All (${achievements.size})") }
                    )
                    FilterChip(
                        selected = achievementFilter == "UNLOCKED",
                        onClick = { achievementFilter = "UNLOCKED" },
                        label = { Text("Unlocked ($unlockedCount)") }
                    )
                    FilterChip(
                        selected = achievementFilter == "LOCKED",
                        onClick = { achievementFilter = "LOCKED" },
                        label = { Text("Locked ($lockedCount)") }
                    )
                }

                val filteredAchievements = remember(achievements, achievementFilter) {
                    when (achievementFilter) {
                        "UNLOCKED" -> achievements.filter { it.isUnlocked }
                        "LOCKED" -> achievements.filter { !it.isUnlocked }
                        else -> achievements
                    }
                }

                // Achievements List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredAchievements, key = { it.code }) { ach ->
                        AchievementCard(
                            achievement = ach,
                            currentLevel = profile.currentLevel,
                            habitStreak = habitStreaks.currentStreak,
                            taskStreak = taskStreaks.currentStreak
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            } else {
                // Rewards Shop List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(rewards, key = { it.id }) { reward ->
                        RewardCard(
                            reward = reward,
                            currentCoins = profile.currentCoins,
                            onRedeem = {
                                viewModel.redeemReward(reward.id) { success ->
                                    if (success) {
                                        Toast.makeText(context, "🎉 Redeemed: ${reward.title}!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Not enough coins!", Toast.LENGTH_SHORT).show()
                                    }
                                }
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

    if (showAddRewardDialog) {
        AddRewardDialog(
            onDismiss = { showAddRewardDialog = false },
            onAdd = { title, desc, cost, emoji ->
                viewModel.insertReward(title, desc, cost, emoji)
                showAddRewardDialog = false
            }
        )
    }
}

@Composable
fun AchievementCard(
    achievement: AchievementItem,
    currentLevel: Int = 1,
    habitStreak: Int = 0,
    taskStreak: Int = 0
) {
    val progressInfo = remember(achievement.code, currentLevel, habitStreak, taskStreak) {
        if (achievement.isUnlocked) null
        else when (achievement.code) {
            "LEVEL_10" -> {
                val fraction = (currentLevel / 10f).coerceIn(0f, 1f)
                Pair(fraction, "Level $currentLevel / 10")
            }
            "STREAK_3" -> {
                val fraction = (habitStreak / 3f).coerceIn(0f, 1f)
                Pair(fraction, "$habitStreak / 3 days")
            }
            "STREAK_7" -> {
                val fraction = (habitStreak / 7f).coerceIn(0f, 1f)
                Pair(fraction, "$habitStreak / 7 days")
            }
            "STREAK_30" -> {
                val fraction = (habitStreak / 30f).coerceIn(0f, 1f)
                Pair(fraction, "$habitStreak / 30 days")
            }
            else -> null
        }
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (achievement.isUnlocked)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (achievement.isUnlocked)
                            when (achievement.tier) {
                                "PLATINUM" -> Color(0xFFE5E7EB)
                                "GOLD" -> Color(0xFFFEF3C7)
                                "SILVER" -> Color(0xFFE2E8F0)
                                else -> Color(0xFFFED7AA)
                            }
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
            ) {
                if (achievement.isUnlocked) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Unlocked",
                        tint = when (achievement.tier) {
                            "PLATINUM" -> Color(0xFF6B7280)
                            "GOLD" -> Color(0xFFD97706)
                            "SILVER" -> Color(0xFF64748B)
                            else -> Color(0xFFB45309)
                        },
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = achievement.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (achievement.isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = achievement.tier,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = achievement.description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (progressInfo != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LinearProgressIndicator(
                            progress = { progressInfo.first },
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Text(
                            text = progressInfo.second,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.outline,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "+${achievement.xpReward} XP",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "+${achievement.coinsReward} 🪙",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD97706)
                )
            }
        }
    }
}

@Composable
fun RewardCard(
    reward: RewardItem,
    currentCoins: Int,
    onRedeem: () -> Unit
) {
    val canAfford = currentCoins >= reward.coinCost

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Text(text = reward.iconEmoji, fontSize = 28.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = reward.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (reward.description.isNotBlank()) {
                        Text(
                            text = reward.description,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (reward.redemptionCount > 0) {
                        Text(
                            text = "Redeemed ${reward.redemptionCount} times",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Button(
                onClick = onRedeem,
                enabled = canAfford,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = "🪙 ${reward.coinCost}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AddRewardDialog(
    onDismiss: () -> Unit,
    onAdd: (title: String, desc: String, cost: Int, emoji: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var costStr by remember { mutableStateOf("50") }
    var emoji by remember { mutableStateOf("🎁") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Personal Reward", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (it.isNotBlank()) isError = false
                    },
                    label = { Text("Reward Name *") },
                    placeholder = { Text("e.g. Cheat Meal, Movie Night") },
                    isError = isError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = emoji,
                        onValueChange = { emoji = it },
                        label = { Text("Emoji") },
                        modifier = Modifier.width(70.dp)
                    )
                    OutlinedTextField(
                        value = costStr,
                        onValueChange = { costStr = it },
                        label = { Text("Coin Cost") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        isError = true
                        return@Button
                    }
                    val cost = costStr.toIntOrNull() ?: 50
                    onAdd(title.trim(), description.trim(), cost, emoji.ifBlank { "🎁" })
                }
            ) {
                Text("Add Reward")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
