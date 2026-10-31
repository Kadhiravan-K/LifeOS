package com.example.ui.screens.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Workspaces
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.ProfileEntity
import com.example.ui.theme.engine.LocalLifeAnimationSpeed
import com.example.ui.theme.engine.ThemedAtmosphereBackground

/**
 * Requirement #9: App-Entry Honeycomb Profile Selection Screen.
 * Presents a tactile, premium workspace selection experience at app startup.
 * Prevents showing another profile's dashboard during switching and isolates all data.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppEntryProfileSelectorScreen(
    profiles: List<ProfileEntity>,
    activeProfileId: Long,
    showOnStartupPreference: Boolean,
    onProfileSelected: (Long) -> Unit,
    onEnterWorkspace: (Long) -> Unit,
    onCreateProfile: (name: String, emoji: String, colorHex: Long, type: String) -> Unit,
    onToggleShowOnStartup: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedProfileId by rememberSaveable {
        mutableLongStateOf(
            if (profiles.any { it.id == activeProfileId }) activeProfileId
            else profiles.firstOrNull()?.id ?: ProfileEntity.DEFAULT_PERSONAL_PROFILE_ID
        )
    }

    var showCreateDialog by remember { mutableStateOf(false) }

    val currentSelectedProfile = profiles.firstOrNull { it.id == selectedProfileId }
        ?: profiles.firstOrNull()
        ?: ProfileEntity.createDefaultPersonal()

    ThemedAtmosphereBackground(modifier = modifier.fillMaxSize().testTag("app_entry_profile_selector_screen")) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Branding & Header
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Workspaces,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "LifeOS Workspaces",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Select a workspace to enter your isolated tasks, projects, habits, and shortcuts.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Profile Grid / List
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    profiles.forEach { profile ->
                        val isSelected = profile.id == selectedProfileId
                        val isPersistedActive = profile.id == activeProfileId

                        HoneycombEntryProfileCard(
                            profile = profile,
                            isSelected = isSelected,
                            isPersistedActive = isPersistedActive,
                            onClick = {
                                selectedProfileId = profile.id
                                onProfileSelected(profile.id)
                            },
                            onDoubleClickOrEnter = {
                                selectedProfileId = profile.id
                                onEnterWorkspace(profile.id)
                            }
                        )
                    }

                    // Add Workspace Card
                    AddWorkspaceCard(
                        onClick = { showCreateDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Actions & Persistence Toggle
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Enter Workspace Primary Action Button
                    Button(
                        onClick = {
                            onEnterWorkspace(currentSelectedProfile.id)
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(currentSelectedProfile.colorHex),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("app_entry_enter_workspace_btn")
                    ) {
                        Text(
                            text = "Enter ${currentSelectedProfile.name}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // "Always show workspace selector on startup" Checkbox
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleShowOnStartup(!showOnStartupPreference) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Checkbox(
                            checked = showOnStartupPreference,
                            onCheckedChange = onToggleShowOnStartup,
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("app_entry_show_on_startup_checkbox")
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Always show profile selector at startup",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateProfileDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, emoji, colorHex, type ->
                showCreateDialog = false
                onCreateProfile(name, emoji, colorHex, type)
            }
        )
    }
}

@Composable
fun HoneycombEntryProfileCard(
    profile: ProfileEntity,
    isSelected: Boolean,
    isPersistedActive: Boolean,
    onClick: () -> Unit,
    onDoubleClickOrEnter: () -> Unit,
    modifier: Modifier = Modifier
) {
    val speedMultiplier = LocalLifeAnimationSpeed.current.multiplier
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else if (isSelected) 1.04f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = (450f * speedMultiplier).coerceAtLeast(120f)
        ),
        label = "entry_profile_scale"
    )

    val profileColor = Color(profile.colorHex)
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) profileColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        label = "entry_profile_border"
    )
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) profileColor.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        label = "entry_profile_bg"
    )

    Surface(
        modifier = modifier
            .padding(4.dp)
            .scale(scale)
            .width(160.dp)
            .clip(RoundedCornerShape(22.dp))
            .border(if (isSelected) 2.5.dp else 1.dp, borderColor, RoundedCornerShape(22.dp))
            .background(backgroundColor)
            .pointerInput(profile.id) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() },
                    onDoubleTap = { onDoubleClickOrEnter() }
                )
            }
            .semantics {
                contentDescription = "Workspace ${profile.name}, type ${profile.type}, ${if (isSelected) "selected" else "unselected"}"
            }
            .testTag("app_entry_profile_${profile.id}"),
        shape = RoundedCornerShape(22.dp),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hexagonal Emoji Badge
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(profileColor.copy(alpha = 0.22f))
                    .border(2.dp, profileColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = profile.emoji, fontSize = 28.sp)

                if (isPersistedActive) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(profileColor)
                            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Active",
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = profile.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = when {
                    profile.isDefault -> "Personal"
                    profile.type == ProfileEntity.TYPE_WORK -> "Work"
                    else -> "Custom"
                },
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
            )

            if (isSelected) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(profileColor)
                )
            }
        }
    }
}

@Composable
fun AddWorkspaceCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val speedMultiplier = LocalLifeAnimationSpeed.current.multiplier
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = (450f * speedMultiplier).coerceAtLeast(120f)
        ),
        label = "add_workspace_scale"
    )

    Surface(
        modifier = modifier
            .padding(4.dp)
            .scale(scale)
            .width(160.dp)
            .clip(RoundedCornerShape(22.dp))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                RoundedCornerShape(22.dp)
            )
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() }
                )
            }
            .semantics { contentDescription = "Add new workspace profile" }
            .testTag("app_entry_add_profile_card"),
        shape = RoundedCornerShape(22.dp),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "New Profile",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Add Workspace",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
            )
        }
    }
}
