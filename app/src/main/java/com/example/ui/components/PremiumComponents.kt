package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.engine.AnimationSpeed
import com.example.ui.theme.engine.LocalLifeAnimationSpeed
import com.example.ui.theme.engine.LocalLifeTheme
import com.example.ui.theme.engine.ThemeId

/**
 * Visual styling modes for PremiumCard.
 */
enum class PremiumCardStyle {
    DEFAULT,
    ELEVATED,
    GLASS,
    ACCENT_BORDER,
    THEME_BESPOKE
}

/**
 * Visual styling modes for PremiumButton.
 */
enum class PremiumButtonStyle {
    PRIMARY,
    SECONDARY,
    OUTLINED,
    GLASS,
    DESTRUCTIVE
}

/**
 * PremiumCard provides tactile spring physics press feedback, distinct surface finishes
 * (including frosted glass, elevation, and theme-bespoke shapes), animated selection states,
 * and content expansion transitions.
 */
@Composable
fun PremiumCard(
    modifier: Modifier = Modifier,
    style: PremiumCardStyle = PremiumCardStyle.THEME_BESPOKE,
    isSelected: Boolean = false,
    isCompleted: Boolean = false,
    isEnabled: Boolean = true,
    shape: Shape? = null,
    onClick: (() -> Unit)? = null,
    badgeText: String? = null,
    headerEmoji: String? = null,
    headerTitle: String? = null,
    headerAction: (@Composable RowScope.() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val theme = LocalLifeTheme.current
    val speed = LocalLifeAnimationSpeed.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Animation scaling: if speed is OFF, scale duration to 0
    val durationMs = if (speed == AnimationSpeed.OFF) 0 else speed.calculateDuration(220)

    val scale by animateFloatAsState(
        targetValue = if (isPressed && isEnabled && onClick != null) 0.975f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = if (speed == AnimationSpeed.OFF) Spring.StiffnessHigh else theme.motion.bounceStiffness
        ),
        label = "premium_card_scale"
    )

    val effectiveShape = shape ?: when (style) {
        PremiumCardStyle.GLASS -> RoundedCornerShape(20.dp)
        PremiumCardStyle.THEME_BESPOKE -> theme.shapes.cardShape
        PremiumCardStyle.ACCENT_BORDER -> RoundedCornerShape(theme.shapes.cornerRadiusDp)
        PremiumCardStyle.ELEVATED -> RoundedCornerShape(16.dp)
        PremiumCardStyle.DEFAULT -> RoundedCornerShape(16.dp)
    }

    val animatedBorderColor by animateColorAsState(
        targetValue = when {
            !isEnabled -> theme.colors.outline.copy(alpha = 0.25f)
            isSelected -> theme.colors.primary
            style == PremiumCardStyle.ACCENT_BORDER -> theme.colors.primary
            style == PremiumCardStyle.GLASS -> Color.White.copy(alpha = 0.22f)
            else -> theme.colors.outline.copy(alpha = 0.5f)
        },
        animationSpec = tween(durationMillis = durationMs),
        label = "premium_card_border_color"
    )

    val animatedBorderWidth by animateDpAsState(
        targetValue = when {
            isSelected -> 2.dp
            style == PremiumCardStyle.ACCENT_BORDER -> 1.5.dp
            style == PremiumCardStyle.GLASS -> 1.dp
            else -> theme.shapes.borderWidth
        },
        animationSpec = tween(durationMillis = durationMs),
        label = "premium_card_border_width"
    )

    val containerAlpha = when {
        !isEnabled -> 0.45f
        isCompleted -> 0.65f
        else -> 1f
    }

    val elevationDp = if (style == PremiumCardStyle.ELEVATED && isEnabled) 4.dp else 0.dp

    val surfaceModifier = when (style) {
        PremiumCardStyle.GLASS -> Modifier.background(
            brush = Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.12f),
                    Color.White.copy(alpha = 0.04f)
                )
            )
        ).background(theme.colors.surface.copy(alpha = 0.65f))

        PremiumCardStyle.THEME_BESPOKE -> {
            if (theme.shapes.isGlass) {
                Modifier.background(
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.14f),
                            Color.White.copy(alpha = 0.04f)
                        )
                    )
                ).background(theme.colors.surface.copy(alpha = 0.65f))
            } else {
                Modifier.background(brush = theme.colors.getSurfaceBrush())
            }
        }

        PremiumCardStyle.ELEVATED -> Modifier.background(theme.colors.surface)
        PremiumCardStyle.ACCENT_BORDER -> Modifier.background(theme.colors.surface)
        PremiumCardStyle.DEFAULT -> Modifier.background(theme.colors.surfaceVariant.copy(alpha = 0.4f))
    }

    Box(
        modifier = modifier
            .scale(scale)
            .shadow(elevation = elevationDp, shape = effectiveShape)
            .clip(effectiveShape)
            .border(
                border = BorderStroke(animatedBorderWidth, animatedBorderColor),
                shape = effectiveShape
            )
            .then(surfaceModifier)
            .then(
                if (isSelected) Modifier.background(theme.colors.primary.copy(alpha = 0.12f))
                else Modifier
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        enabled = isEnabled,
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            )
            .alpha(containerAlpha)
            .animateContentSize(
                animationSpec = tween(durationMillis = durationMs)
            )
            .padding(contentPadding)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (headerTitle != null || headerEmoji != null || badgeText != null || headerAction != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        if (headerEmoji != null) {
                            Text(text = headerEmoji, fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        if (headerTitle != null) {
                            Text(
                                text = headerTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = theme.colors.onSurface
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (badgeText != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = theme.colors.primary.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, theme.colors.primary.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = badgeText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = theme.colors.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        headerAction?.invoke(this)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            content()
        }
    }
}

/**
 * PremiumButton provides interactive physics scale bounce, loading spinners that preserve
 * button geometry, animated success feedback checkmarks, and theme-consistent styling.
 */
@Composable
fun PremiumButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: PremiumButtonStyle = PremiumButtonStyle.PRIMARY,
    text: String? = null,
    icon: ImageVector? = null,
    isLoading: Boolean = false,
    isSuccess: Boolean = false,
    enabled: Boolean = true,
    shape: Shape? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    content: (@Composable RowScope.() -> Unit)? = null
) {
    val theme = LocalLifeTheme.current
    val speed = LocalLifeAnimationSpeed.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !isLoading) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = if (speed == AnimationSpeed.OFF) Spring.StiffnessHigh else theme.motion.bounceStiffness
        ),
        label = "premium_button_scale"
    )

    val effectiveShape = shape ?: when (style) {
        PremiumButtonStyle.GLASS -> RoundedCornerShape(14.dp)
        else -> theme.shapes.buttonShape
    }

    val backgroundModifier = when {
        !enabled -> Modifier.background(theme.colors.surfaceVariant.copy(alpha = 0.4f))
        isSuccess -> Modifier.background(Color(0xFF10B981))
        style == PremiumButtonStyle.PRIMARY -> Modifier.background(brush = theme.colors.getPrimaryBrush())
        style == PremiumButtonStyle.SECONDARY -> Modifier.background(theme.colors.surfaceVariant)
        style == PremiumButtonStyle.OUTLINED -> Modifier.background(Color.Transparent)
        style == PremiumButtonStyle.DESTRUCTIVE -> Modifier.background(MaterialTheme.colorScheme.error)
        style == PremiumButtonStyle.GLASS -> Modifier.background(
            brush = Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.20f),
                    Color.White.copy(alpha = 0.08f)
                )
            )
        ).background(theme.colors.surface.copy(alpha = 0.6f))
        else -> Modifier.background(theme.colors.primary)
    }

    val borderStroke = when {
        !enabled -> BorderStroke(1.dp, theme.colors.outline.copy(alpha = 0.25f))
        isSuccess -> null
        style == PremiumButtonStyle.OUTLINED -> BorderStroke(1.5.dp, theme.colors.primary)
        style == PremiumButtonStyle.GLASS -> BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
        style == PremiumButtonStyle.SECONDARY -> BorderStroke(1.dp, theme.colors.outline.copy(alpha = 0.5f))
        else -> null
    }

    val contentColor = when {
        !enabled -> theme.colors.onSurfaceVariant.copy(alpha = 0.4f)
        isSuccess -> Color.White
        style == PremiumButtonStyle.PRIMARY -> theme.colors.onPrimary
        style == PremiumButtonStyle.DESTRUCTIVE -> MaterialTheme.colorScheme.onError
        style == PremiumButtonStyle.OUTLINED -> theme.colors.primary
        style == PremiumButtonStyle.GLASS -> theme.colors.onSurface
        else -> theme.colors.onSurface
    }

    Box(
        modifier = modifier
            .scale(scale)
            .clip(effectiveShape)
            .then(if (borderStroke != null) Modifier.border(borderStroke, effectiveShape) else Modifier)
            .then(backgroundModifier)
            .clickable(
                enabled = enabled && !isLoading,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(18.dp)
                        .testTag("premium_button_loading"),
                    strokeWidth = 2.dp,
                    color = contentColor
                )
                if (!text.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor
                    )
                }
            } else if (isSuccess) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Success",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                if (!text.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            } else if (content != null) {
                content()
            } else {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    if (!text.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
                if (!text.isNullOrBlank()) {
                    Text(
                        text = text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor
                    )
                }
            }
        }
    }
}
