package com.example.ui.theme.engine

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
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * ThemedCard adapts its border, corners, gradient surface, and subtle glow
 * dynamically to the active ThemeDefinition.
 */
@Composable
fun ThemedCard(
    modifier: Modifier = Modifier,
    shape: Shape? = null,
    borderWidth: Dp? = null,
    borderColor: Color? = null,
    containerColor: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val theme = LocalLifeTheme.current
    val effectiveShape = shape ?: theme.shapes.cardShape
    val effectiveBorderWidth = borderWidth ?: theme.shapes.borderWidth
    val effectiveBorderColor = borderColor ?: theme.colors.outline
    val surfaceGradient = theme.colors.getSurfaceBrush()

    Box(
        modifier = modifier
            .clip(effectiveShape)
            .border(
                width = effectiveBorderWidth,
                color = effectiveBorderColor,
                shape = effectiveShape
            )
            .background(brush = surfaceGradient)
            .then(
                if (containerColor != null) Modifier.background(containerColor) else Modifier
            ),
        content = content
    )
}

/**
 * ThemedButton provides interactive press feedback, theme gradients, and custom borders.
 */
@Composable
fun ThemedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape? = null,
    isPrimary: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    content: @Composable () -> Unit
) {
    val theme = LocalLifeTheme.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = if (isPressed) 0.96f else 1f

    val effectiveShape = shape ?: theme.shapes.buttonShape
    val backgroundBrush = if (isPrimary) {
        theme.colors.getPrimaryBrush()
    } else {
        Brush.horizontalGradient(listOf(theme.colors.surfaceVariant, theme.colors.surface))
    }

    Box(
        modifier = modifier
            .scale(scale)
            .clip(effectiveShape)
            .then(
                if (!isPrimary) Modifier.border(theme.shapes.borderWidth, theme.colors.outline, effectiveShape)
                else Modifier
            )
            .background(brush = backgroundBrush)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Animated Loading Indicator themed for each design system:
 * - Arcane: Rotating magical concentric sigil
 * - Cyber: Rotating digital scanning rings
 * - Adventure: Rotating fantasy compass needle
 * - Cosmic: Orbiting celestial bodies
 * - Default: Standard Material 3 circular spinner
 */
@Composable
fun ThemedLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp
) {
    val theme = LocalLifeTheme.current

    if (theme.id == ThemeId.DEFAULT) {
        CircularProgressIndicator(
            modifier = modifier.size(size),
            color = theme.colors.primary,
            strokeWidth = 3.dp
        )
        return
    }

    val infiniteTransition = rememberInfiniteTransition(label = "theme_loading")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val scanProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scan_progress"
    )

    Canvas(modifier = modifier.size(size)) {
        val centerOffset = Offset(size.toPx() / 2f, size.toPx() / 2f)
        val radius = (size.toPx() / 2f) * 0.85f

        when (theme.id) {
            ThemeId.ARCANE_ACADEMY -> {
                ThemeAssetManager.drawArcaneSigil(
                    scope = this,
                    center = centerOffset,
                    radius = radius,
                    rotationDegrees = rotation,
                    primaryColor = theme.colors.primary,
                    goldColor = theme.colors.secondary
                )
            }
            ThemeId.CYBER_NEXUS -> {
                ThemeAssetManager.drawCyberGrid(
                    scope = this,
                    width = size.toPx(),
                    height = size.toPx(),
                    scanLineProgress = scanProgress,
                    cyanColor = theme.colors.primary,
                    gridColor = theme.colors.outline
                )
            }
            ThemeId.ADVENTURE_REALM -> {
                ThemeAssetManager.drawFantasyCompass(
                    scope = this,
                    center = centerOffset,
                    radius = radius,
                    needleAngle = rotation,
                    woodGold = theme.colors.primary,
                    mossGreen = theme.colors.secondary
                )
            }
            ThemeId.COSMIC_EXPLORER -> {
                ThemeAssetManager.drawCosmicOrbits(
                    scope = this,
                    center = centerOffset,
                    radius = radius,
                    orbitProgress = scanProgress,
                    starColor = theme.colors.primary,
                    nebulaColor = theme.colors.secondary
                )
            }
            ThemeId.DEFAULT -> {}
        }
    }
}

/**
 * Fullscreen atmospheric background with theme-specific ambient effects
 * (Cosmic starfield, Cyber faint grid, Arcane mystical aura, Adventure warm oak grain).
 */
@Composable
fun ThemedAtmosphereBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val theme = LocalLifeTheme.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(theme.colors.background)
    ) {
        if (theme.motion.enableAtmosphereGlow) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                when (theme.id) {
                    ThemeId.COSMIC_EXPLORER -> {
                        ThemeAssetManager.drawStarfield(
                            scope = this,
                            width = size.width,
                            height = size.height,
                            starColor = theme.colors.primary
                        )
                    }
                    ThemeId.CYBER_NEXUS -> {
                        ThemeAssetManager.drawCyberGrid(
                            scope = this,
                            width = size.width,
                            height = size.height,
                            scanLineProgress = 0.5f,
                            cyanColor = theme.colors.primary.copy(alpha = 0.15f),
                            gridColor = theme.colors.outline.copy(alpha = 0.08f)
                        )
                    }
                    else -> {
                        // Soft ambient radial glow in top corner
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(theme.colors.glowColor, Color.Transparent),
                                center = Offset(size.width * 0.85f, size.height * 0.15f),
                                radius = size.width * 0.7f
                            )
                        )
                    }
                }
            }
        }

        content()
    }
}

/**
 * Themed ProgressBar with smooth gradient track and theme colors.
 */
@Composable
fun ThemedProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 10.dp
) {
    val theme = LocalLifeTheme.current
    val clampedProgress = progress.coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(theme.colors.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(clampedProgress)
                .height(height)
                .clip(RoundedCornerShape(height / 2))
                .background(theme.colors.getPrimaryBrush())
        )
    }
}

/**
 * ThemedBadge tag for categories, status indicators, and streak counts.
 */
@Composable
fun ThemedBadge(
    text: String,
    modifier: Modifier = Modifier,
    iconEmoji: String? = null,
    color: Color? = null
) {
    val theme = LocalLifeTheme.current
    val badgeColor = color ?: theme.colors.primary

    Surface(
        shape = theme.shapes.chipShape,
        color = badgeColor.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (iconEmoji != null) "$iconEmoji $text" else text,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = badgeColor
            )
        }
    }
}
