package com.example.ui.screens.settings

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.notifications.ChannelHealthStatus
import com.example.notifications.ExactAlarmStatus
import com.example.notifications.ReminderScheduler
import com.example.notifications.ScheduledReminderItem
import com.example.ui.viewmodel.LifeViewModel
import com.example.utils.DateUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderHealthScreen(
    onBack: () -> Unit,
    viewModel: LifeViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val healthState by viewModel.reminderHealthState.collectAsStateWithLifecycle()
    var testFeedbackMessage by remember { mutableStateOf<String?>(null) }
    var isTestError by remember { mutableStateOf(false) }

    // Automatically refresh health diagnostics whenever the user returns to the app
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.refreshReminderHealth(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reminder Health", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("reminder_health_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshReminderHealth(context) },
                        modifier = Modifier.testTag("reminder_health_refresh_btn")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Status")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        if (healthState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "System Diagnostics & Delivery Check",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Verify that Android permissions, alarm quotas, and battery optimization allow LifeOS to trigger alerts reliably.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // SECTION 1: System Permissions & Restrictions
                DiagnosticCard(
                    title = "Notifications Permission",
                    statusText = if (healthState.areNotificationsEnabled) "Granted" else "Not granted",
                    isPositive = healthState.areNotificationsEnabled,
                    description = if (healthState.areNotificationsEnabled) {
                        "LifeOS notifications are permitted by the Android system."
                    } else {
                        "Notifications are disabled in system settings. Reminders, alarms, and progress alerts cannot be shown on your lock screen or status bar."
                    },
                    actionButtonText = if (!healthState.areNotificationsEnabled) "Fix in Settings" else null,
                    onAction = { ReminderScheduler.openNotificationSettings(context) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                DiagnosticCard(
                    title = "Precise Alarms",
                    statusText = healthState.exactAlarmStatus.displayText,
                    isPositive = healthState.exactAlarmStatus == ExactAlarmStatus.GRANTED || healthState.exactAlarmStatus == ExactAlarmStatus.NOT_NEEDED,
                    description = when (healthState.exactAlarmStatus) {
                        ExactAlarmStatus.GRANTED -> "Exact alarms are permitted. Reminders and alarms will trigger at the exact scheduled minute, even during standby."
                        ExactAlarmStatus.NOT_GRANTED -> "Exact alarm permission is restricted. Android may delay notifications by 10–15 minutes or during Doze mode to conserve battery."
                        ExactAlarmStatus.NOT_NEEDED -> "Exact timing is natively supported without special permissions on this Android version."
                    },
                    actionButtonText = if (healthState.exactAlarmStatus == ExactAlarmStatus.NOT_GRANTED) "Fix in Settings" else null,
                    onAction = { ReminderScheduler.openExactAlarmSettings(context) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                DiagnosticCard(
                    title = "Battery Optimization",
                    statusText = if (healthState.isIgnoringBatteryOptimizations) "Unrestricted" else "Optimized",
                    isPositive = healthState.isIgnoringBatteryOptimizations,
                    description = if (healthState.isIgnoringBatteryOptimizations) {
                        "LifeOS is excluded from battery optimization, ensuring alerts fire promptly without background throttling."
                    } else {
                        "Standard battery optimization is active. On devices with aggressive power management, alarms may be batched during deep sleep unless excluded."
                    },
                    actionButtonText = "Battery settings",
                    onAction = { ReminderScheduler.openBatteryOptimizationSettings(context) }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // SECTION 2: Notification Channels
                Text(
                    text = "Notification Channels",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Check whether individual alert categories are muted in Android system settings.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (healthState.channels.isEmpty()) {
                            Text(
                                text = "Notification channels are managed globally on this Android version.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            healthState.channels.forEachIndexed { index, channel ->
                                ChannelStatusRow(
                                    channel = channel,
                                    onFix = { ReminderScheduler.openChannelSettings(context, channel.id) }
                                )
                                if (index < healthState.channels.size - 1) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // SECTION 3: Next 5 Scheduled Reminders
                Text(
                    text = "Upcoming Reminders (Next 5)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Chronologically sorted active triggers scheduled in the next 7 days.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (healthState.upcomingReminders.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.height(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No upcoming reminders scheduled",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Create a task with a due time or add a medication schedule to populate alarms.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        } else {
                            healthState.upcomingReminders.forEachIndexed { index, item ->
                                UpcomingReminderRow(item = item)
                                if (index < healthState.upcomingReminders.size - 1) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // SECTION 4: Live Delivery Test
                Text(
                    text = "Live Delivery Test",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Schedule a real test reminder approximately one minute in the future. This verifies notification delivery without altering actual tasks or medication records.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        AnimatedVisibility(visible = testFeedbackMessage != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isTestError) MaterialTheme.colorScheme.errorContainer else Color(0xFF4CAF50).copy(alpha = 0.15f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {
                                Text(
                                    text = testFeedbackMessage ?: "",
                                    color = if (isTestError) MaterialTheme.colorScheme.onErrorContainer else Color(0xFF2E7D32),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        if (healthState.isTestReminderScheduled) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Test reminder active", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        text = "Firing in ~1 minute. Lock your screen or exit the app to test delivery.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                OutlinedButton(
                                    onClick = {
                                        viewModel.cancelTestReminder(context)
                                        testFeedbackMessage = "Test reminder cancelled."
                                        isTestError = false
                                    }
                                ) {
                                    Text("Cancel")
                                }
                            }
                        } else {
                            Button(
                                onClick = {
                                    if (!healthState.areNotificationsEnabled) {
                                        testFeedbackMessage = "Cannot send test: Notifications are disabled in Android settings."
                                        isTestError = true
                                    } else {
                                        val scheduled = viewModel.scheduleTestReminder(context, delaySeconds = 60L)
                                        if (scheduled) {
                                            testFeedbackMessage = "Test reminder scheduled for 1 minute from now! Lock your device or minimize LifeOS to check delivery."
                                            isTestError = false
                                        } else {
                                            testFeedbackMessage = "Failed to schedule test alarm with system AlarmManager."
                                            isTestError = true
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("send_test_reminder_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Alarm, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Send test reminder in 1 minute")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun DiagnosticCard(
    title: String,
    statusText: String,
    isPositive: Boolean,
    description: String,
    actionButtonText: String?,
    onAction: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                val badgeColor = if (isPositive) Color(0xFF4CAF50) else Color(0xFFFF9800)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = statusText,
                        color = badgeColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!actionButtonText.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onAction,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(actionButtonText, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun ChannelStatusRow(
    channel: ChannelHealthStatus,
    onFix: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(channel.name, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Text(
                text = if (channel.isEnabled) "Delivering normally" else "Channel muted by user",
                fontSize = 11.sp,
                color = if (channel.isEnabled) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
            )
        }

        if (!channel.isEnabled) {
            OutlinedButton(
                onClick = onFix,
                modifier = Modifier.height(32.dp)
            ) {
                Text("Enable", fontSize = 11.sp)
            }
        } else {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF4CAF50).copy(alpha = 0.12f)
            ) {
                Text(
                    text = "Active",
                    color = Color(0xFF2E7D32),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun UpcomingReminderRow(item: ScheduledReminderItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            val dateLabel = if (item.scheduledDateIso == DateUtils.todayString()) {
                "Today"
            } else if (item.scheduledDateIso == DateUtils.addDays(DateUtils.todayString(), 1)) {
                "Tomorrow"
            } else {
                item.scheduledDateIso
            }
            Text(
                text = "$dateLabel at ${item.scheduledTime}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val typeBadgeColor = when (item.reminderType) {
            "P1_ALARM" -> Color(0xFFE53935)
            "MEDICINE_DOSE" -> Color(0xFF1E88E5)
            else -> Color(0xFF7CB342)
        }
        val typeBadgeLabel = when (item.reminderType) {
            "P1_ALARM" -> "P1 Alarm"
            "MEDICINE_DOSE" -> "Med Dose"
            else -> "Reminder"
        }

        Surface(
            shape = RoundedCornerShape(6.dp),
            color = typeBadgeColor.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, typeBadgeColor.copy(alpha = 0.4f))
        ) {
            Text(
                text = typeBadgeLabel,
                color = typeBadgeColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
            )
        }
    }
}
