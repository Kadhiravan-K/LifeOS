package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Reusable animated task completion indicator.
 * Provides:
 * - Smooth circle fill transition
 * - Checkmark appearance with gentle spring
 * - Subtle pulse/scale animation on completion
 * - Respects animation settings & reduced motion
 */
@Composable
fun TaskCompletionCircle(
    isCompleted: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    borderColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
    isAnimationEnabled: Boolean = true,
    size: Dp = 24.dp
) {
    val pulseScale = remember { Animatable(1.0f) }

    LaunchedEffect(isCompleted) {
        if (isCompleted && isAnimationEnabled) {
            // Subtle pulse on completion: 1.0f -> 1.18f -> 1.0f
            pulseScale.animateTo(
                targetValue = 1.18f,
                animationSpec = tween(durationMillis = 110, easing = FastOutSlowInEasing)
            )
            pulseScale.animateTo(
                targetValue = 1.0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            )
        } else {
            pulseScale.snapTo(1.0f)
        }
    }

    val animatedBgColor by animateColorAsState(
        targetValue = if (isCompleted) activeColor else Color.Transparent,
        animationSpec = tween(durationMillis = if (isAnimationEnabled) 200 else 0),
        label = "task_circle_bg"
    )

    val animatedBorderColor by animateColorAsState(
        targetValue = if (isCompleted) activeColor else borderColor,
        animationSpec = tween(durationMillis = if (isAnimationEnabled) 200 else 0),
        label = "task_circle_border"
    )

    val iconScale by animateFloatAsState(
        targetValue = if (isCompleted) 1.0f else 0.0f,
        animationSpec = if (isAnimationEnabled) {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        } else {
            tween(durationMillis = 0)
        },
        label = "task_circle_icon_scale"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(pulseScale.value)
            .clip(CircleShape)
            .border(width = 2.dp, color = animatedBorderColor, shape = CircleShape)
            .background(color = animatedBgColor, shape = CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggle
            ),
        contentAlignment = Alignment.Center
    ) {
        if (iconScale > 0.05f) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = if (isCompleted) "Completed" else "Incomplete",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .size(size * 0.65f)
                    .scale(iconScale)
            )
        }
    }
}
