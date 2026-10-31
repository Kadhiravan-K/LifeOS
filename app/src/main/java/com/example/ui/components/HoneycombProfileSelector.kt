package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.ProfileEntity
import com.example.ui.theme.engine.LocalLifeAnimationSpeed
import com.example.ui.theme.engine.LocalLifeTheme

/**
 * Honeycomb-styled Profile / Workspace Selector.
 * Allows quick switching between isolated local profiles (Personal, Work, Custom)
 * with tactile spring animations, active profile indicators, and workspace creation.
 */
@Composable
fun HoneycombProfileSelector(
    profiles: List<ProfileEntity>,
    activeProfileId: Long,
    onProfileSelected: (Long) -> Unit,
    onAddProfileClick: () -> Unit,
    onDeleteProfileClick: ((ProfileEntity) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val activeTheme = LocalLifeTheme.current

    Column(modifier = modifier.fillMaxWidth()) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("honeycomb_profile_selector"),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
        ) {
            items(profiles, key = { it.id }) { profile ->
                val isActive = profile.id == activeProfileId
                HoneycombProfileTile(
                    profile = profile,
                    isActive = isActive,
                    onClick = { onProfileSelected(profile.id) },
                    onDeleteClick = if (!profile.isDefault && onDeleteProfileClick != null) {
                        { onDeleteProfileClick(profile) }
                    } else null
                )
            }

            // "Add Workspace" Button Tile
            item(key = "add_profile_tile") {
                AddProfileTile(
                    onClick = onAddProfileClick
                )
            }
        }
    }
}

@Composable
fun HoneycombProfileTile(
    profile: ProfileEntity,
    isActive: Boolean,
    onClick: () -> Unit,
    onDeleteClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val speedMultiplier = LocalLifeAnimationSpeed.current.multiplier
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else if (isActive) 1.02f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.75f,
            stiffness = (400f * speedMultiplier).coerceAtLeast(100f)
        ),
        label = "profile_tile_scale"
    )

    val profileColor = Color(profile.colorHex)
    val borderColor by animateColorAsState(
        targetValue = if (isActive) profileColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
        label = "profile_tile_border"
    )
    val backgroundColor by animateColorAsState(
        targetValue = if (isActive) profileColor.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        label = "profile_tile_bg"
    )

    Surface(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .border(if (isActive) 2.dp else 1.dp, borderColor, RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .pointerInput(profile.id) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() }
                )
            }
            .testTag("profile_tile_${profile.id}"),
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Hexagonal Emoji Badge
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(profileColor.copy(alpha = 0.2f))
                    .border(1.5.dp, profileColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = profile.emoji, fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                        color = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isActive) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(profileColor)
                        )
                    }
                }
                Text(
                    text = when {
                        profile.isDefault -> "Default"
                        profile.type == ProfileEntity.TYPE_WORK -> "Work"
                        else -> "Custom"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            if (onDeleteClick != null && !isActive) {
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(24.dp).testTag("delete_profile_${profile.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Profile",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AddProfileTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val speedMultiplier = LocalLifeAnimationSpeed.current.multiplier
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.75f,
            stiffness = (400f * speedMultiplier).coerceAtLeast(100f)
        ),
        label = "add_profile_scale"
    )

    Surface(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                RoundedCornerShape(16.dp)
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
            .testTag("add_profile_tile"),
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Profile",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "New Profile",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
