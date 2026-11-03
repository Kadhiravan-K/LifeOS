package com.example.ui.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.engine.ThemeDefinition
import com.example.ui.theme.engine.ThemeId
import kotlin.math.cos
import kotlin.math.sin

/**
 * Premium Theme-Specific Elevated Quick Add Button with custom animations
 * for Arcane Academy, Cyber Nexus, Adventure Realm, Cosmic Explorer, and Glassmorphism.
 */
@Composable
fun ThemedQuickAddButton(
    theme: ThemeDefinition,
    isAnimationEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    elevation: Dp = 8.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressScale = remember { Animatable(1f) }

    val infiniteTransition = rememberInfiniteTransition(label = "themed_fab_transition")

    // Theme-specific animation drivers (only animated if isAnimationEnabled is true)
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isAnimationEnabled) 1.06f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val continuousRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isAnimationEnabled) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "continuous_rotation"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = if (isAnimationEnabled) 0.85f else 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Box(
        modifier = modifier
            .size(size + 16.dp) // Clearance for glow / halo
            .semantics {
                contentDescription = "Quick Add Action Menu"
                role = Role.Button
            },
        contentAlignment = Alignment.Center
    ) {
        when (theme.id) {
            ThemeId.ARCANE_ACADEMY -> {
                // Arcane theme: magical energy glow, orbiting arcane particles
                if (isAnimationEnabled) {
                    ArcaneParticlesCanvas(
                        rotationDegrees = continuousRotation,
                        glowAlpha = glowAlpha,
                        modifier = Modifier.size(size + 14.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(size)
                        .scale(pulseScale * pressScale.value)
                        .shadow(elevation, CircleShape, spotColor = Color(0xFF9333EA))
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFF9333EA), Color(0xFF581C87), Color(0xFF3B0764))
                            )
                        )
                        .border(1.5.dp, Color(0xFFC084FC).copy(alpha = glowAlpha), CircleShape)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = ripple(bounded = true, color = Color(0xFFE9D5FF)),
                            onClick = onClick
                        )
                        .testTag("themed_quick_add_arcane"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        tint = Color(0xFFFAF5FF),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            ThemeId.CYBER_NEXUS -> {
                // Cyber theme: futuristic neon power ring, cyber activation glow
                if (isAnimationEnabled) {
                    CyberEnergyRing(
                        pulseScale = pulseScale,
                        glowAlpha = glowAlpha,
                        modifier = Modifier.size(size + 12.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(size)
                        .scale(pressScale.value)
                        .shadow(elevation, CircleShape, spotColor = Color(0xFF06B6D4))
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                            )
                        )
                        .border(
                            2.dp,
                            Brush.sweepGradient(
                                listOf(
                                    Color(0xFF06B6D4),
                                    Color(0xFF3B82F6),
                                    Color(0xFFEC4899),
                                    Color(0xFF06B6D4)
                                )
                            ),
                            CircleShape
                        )
                        .clickable(
                            interactionSource = interactionSource,
                            indication = ripple(bounded = true, color = Color(0xFF22D3EE)),
                            onClick = onClick
                        )
                        .testTag("themed_quick_add_cyber"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        tint = Color(0xFF22D3EE),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            ThemeId.ADVENTURE_REALM -> {
                // Adventure theme: compass-inspired center button with directional accents
                Box(
                    modifier = Modifier
                        .size(size)
                        .scale(pressScale.value)
                        .shadow(elevation, CircleShape, spotColor = Color(0xFFD97706))
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFFB45309), Color(0xFF78350F), Color(0xFF451A03))
                            )
                        )
                        .border(1.5.dp, Color(0xFFFDE68A), CircleShape)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = ripple(bounded = true, color = Color(0xFFFEF3C7)),
                            onClick = onClick
                        )
                        .testTag("themed_quick_add_adventure"),
                    contentAlignment = Alignment.Center
                ) {
                    // Compass rose ticks
                    CompassRoseAccents(
                        rotation = if (isAnimationEnabled) continuousRotation * 0.25f else 0f,
                        modifier = Modifier.size(size)
                    )
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        tint = Color(0xFFFEF3C7),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            ThemeId.COSMIC_EXPLORER -> {
                // Cosmic theme: stars / nebula glow matching Cosmic loading animation
                if (isAnimationEnabled) {
                    CosmicNebulaHalo(
                        glowAlpha = glowAlpha,
                        modifier = Modifier.size(size + 14.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(size)
                        .scale(pulseScale * pressScale.value)
                        .shadow(elevation, CircleShape, spotColor = Color(0xFF6366F1))
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFF4F46E5), Color(0xFF312E81), Color(0xFF1E1B4B))
                            )
                        )
                        .border(1.5.dp, Color(0xFFA5B4FC).copy(alpha = glowAlpha), CircleShape)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = ripple(bounded = true, color = Color(0xFFE0E7FF)),
                            onClick = onClick
                        )
                        .testTag("themed_quick_add_cosmic"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        tint = Color(0xFFEEF2FF),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            ThemeId.GLASS -> {
                // Glassmorphism theme: translucent frosted glass button with light reflection
                Box(
                    modifier = Modifier
                        .size(size)
                        .scale(pressScale.value)
                        .shadow(elevation, CircleShape, spotColor = Color.White.copy(alpha = 0.3f))
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.25f),
                                    Color.White.copy(alpha = 0.10f)
                                )
                            )
                        )
                        .border(
                            1.dp,
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (isAnimationEnabled) glowAlpha else 0.5f),
                                    Color.White.copy(alpha = 0.1f)
                                )
                            ),
                            CircleShape
                        )
                        .clickable(
                            interactionSource = interactionSource,
                            indication = ripple(bounded = true, color = Color.White),
                            onClick = onClick
                        )
                        .testTag("themed_quick_add_glass"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            else -> {
                // Default / Minimal / Executive Theme
                Box(
                    modifier = Modifier
                        .size(size)
                        .scale(pressScale.value)
                        .shadow(elevation, CircleShape, spotColor = MaterialTheme.colorScheme.primary)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = ripple(bounded = true, color = MaterialTheme.colorScheme.onPrimary),
                            onClick = onClick
                        )
                        .testTag("themed_quick_add_default"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ArcaneParticlesCanvas(
    rotationDegrees: Float,
    glowAlpha: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.rotate(rotationDegrees)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f - 4.dp.toPx()

        // 4 floating particle orbs
        val particleCount = 4
        for (i in 0 until particleCount) {
            val angle = (i * (360f / particleCount)) * (Math.PI / 180.0)
            val px = center.x + radius * cos(angle).toFloat()
            val py = center.y + radius * sin(angle).toFloat()

            drawCircle(
                color = Color(0xFFC084FC).copy(alpha = glowAlpha * 0.7f),
                radius = 3.dp.toPx(),
                center = Offset(px, py)
            )
        }
    }
}

@Composable
private fun CyberEnergyRing(
    pulseScale: Float,
    glowAlpha: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.scale(pulseScale)) {
        drawCircle(
            color = Color(0xFF06B6D4).copy(alpha = glowAlpha * 0.35f),
            radius = size.minDimension / 2f - 2.dp.toPx(),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

@Composable
private fun CompassRoseAccents(
    rotation: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.rotate(rotation)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val r = size.minDimension / 2f - 6.dp.toPx()

        // 4 cardinal direction markers (N, S, E, W)
        val cardinalAngles = floatArrayOf(0f, 90f, 180f, 270f)
        for (deg in cardinalAngles) {
            val rad = Math.toRadians(deg.toDouble())
            val startX = center.x + (r - 4.dp.toPx()) * cos(rad).toFloat()
            val startY = center.y + (r - 4.dp.toPx()) * sin(rad).toFloat()
            val endX = center.x + r * cos(rad).toFloat()
            val endY = center.y + r * sin(rad).toFloat()

            drawLine(
                color = Color(0xFFFDE68A).copy(alpha = 0.8f),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = 1.5.dp.toPx()
            )
        }
    }
}

@Composable
private fun CosmicNebulaHalo(
    glowAlpha: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF818CF8).copy(alpha = glowAlpha * 0.45f),
                    Color(0xFF4F46E5).copy(alpha = glowAlpha * 0.2f),
                    Color.Transparent
                )
            ),
            radius = size.minDimension / 2f
        )
    }
}
