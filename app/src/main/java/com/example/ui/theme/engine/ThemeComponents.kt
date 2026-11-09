package com.example.ui.theme.engine

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Card variants for themed elevation and outlines.
 */
enum class ThemeCardVariant {
    DEFAULT,
    ELEVATED,
    OUTLINED
}

/**
 * ThemedCard adapts its border, corners, gradient surface, and subtle glow
 * dynamically to the active ThemeDefinition.
 */
@Composable
fun ThemedCard(
    modifier: Modifier = Modifier,
    variant: ThemeCardVariant = ThemeCardVariant.DEFAULT,
    shape: Shape? = null,
    borderWidth: Dp? = null,
    borderColor: Color? = null,
    containerColor: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val theme = LocalLifeTheme.current
    val effectiveShape = shape ?: theme.shapes.cardShape
    val effectiveBorderWidth = borderWidth ?: theme.shapes.borderWidth
    val effectiveBorderColor = borderColor ?: when (variant) {
        ThemeCardVariant.OUTLINED -> theme.colors.outline
        ThemeCardVariant.ELEVATED -> theme.colors.primary.copy(alpha = 0.5f)
        ThemeCardVariant.DEFAULT -> theme.colors.outline
    }
    val surfaceGradient = theme.colors.getSurfaceBrush()

    Box(
        modifier = modifier
            .clip(effectiveShape)
            .border(
                width = effectiveBorderWidth,
                color = effectiveBorderColor,
                shape = effectiveShape
            )
            .then(
                if (containerColor != null) {
                    Modifier.background(containerColor)
                } else if (variant == ThemeCardVariant.OUTLINED) {
                    Modifier.background(theme.colors.surface.copy(alpha = 0.4f))
                } else {
                    Modifier.background(brush = surfaceGradient)
                }
            ),
        content = content
    )
}

/**
 * ThemedInteractiveCard provides enter/selection animations, elevation, and click states.
 */
@Composable
fun ThemedInteractiveCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    enabled: Boolean = true,
    shape: Shape? = null,
    borderWidth: Dp? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val theme = LocalLifeTheme.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = if (isPressed) 0.98f else 1f

    val effectiveShape = shape ?: theme.shapes.cardShape
    val effectiveBorderWidth = borderWidth ?: if (isSelected) 2.dp else theme.shapes.borderWidth
    val effectiveBorderColor = if (isSelected) theme.colors.primary else theme.colors.outline
    val surfaceGradient = if (isSelected) {
        Brush.verticalGradient(listOf(theme.colors.surfaceVariant, theme.colors.surface))
    } else {
        theme.colors.getSurfaceBrush()
    }

    Box(
        modifier = modifier
            .scale(scale)
            .clip(effectiveShape)
            .border(
                width = effectiveBorderWidth,
                color = effectiveBorderColor,
                shape = effectiveShape
            )
            .background(brush = surfaceGradient)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
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
 * Primary theme button with custom theme shape, gradient fill, and pressed state.
 */
@Composable
fun ThemedPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    text: String? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    content: (@Composable () -> Unit)? = null
) {
    val theme = LocalLifeTheme.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = if (isPressed && enabled && !isLoading) 0.96f else 1f
    val shape = theme.shapes.buttonShape

    val borderStroke = when (theme.id) {
        ThemeId.ARCANE_ACADEMY -> androidx.compose.foundation.BorderStroke(1.5.dp, theme.colors.outline)
        ThemeId.CYBER_NEXUS -> androidx.compose.foundation.BorderStroke(1.5.dp, theme.colors.primary)
        ThemeId.ADVENTURE_REALM -> androidx.compose.foundation.BorderStroke(1.5.dp, theme.colors.outline)
        ThemeId.GLASS -> androidx.compose.foundation.BorderStroke(1.dp, theme.colors.outline)
        else -> null
    }

    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .scale(scale)
            .alpha(if (enabled) 1f else 0.45f)
            .semantics { role = Role.Button },
        shape = shape,
        color = Color.Transparent,
        border = borderStroke
    ) {
        Box(
            modifier = Modifier
                .background(brush = theme.colors.getPrimaryBrush())
                .clickable(
                    enabled = enabled && !isLoading,
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
                .padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = theme.colors.onPrimary,
                    strokeWidth = 2.dp
                )
            } else if (content != null) {
                content()
            } else if (text != null) {
                Text(
                    text = text,
                    style = theme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = theme.colors.onPrimary
                )
            }
        }
    }
}

/**
 * Secondary theme button with tonal surface background and themed outline.
 */
@Composable
fun ThemedSecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    text: String? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
    content: (@Composable () -> Unit)? = null
) {
    val theme = LocalLifeTheme.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = if (isPressed && enabled) 0.96f else 1f

    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = 44.dp)
            .scale(scale)
            .alpha(if (enabled) 1f else 0.45f)
            .semantics { role = Role.Button },
        shape = theme.shapes.buttonShape,
        color = theme.colors.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(theme.shapes.borderWidth, theme.colors.outline.copy(alpha = 0.6f))
    ) {
        Box(
            modifier = Modifier
                .clickable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
                .padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            if (content != null) {
                content()
            } else if (text != null) {
                Text(
                    text = text,
                    style = theme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = theme.colors.onSurface
                )
            }
        }
    }
}

/**
 * Outlined theme button with transparent interior and themed border.
 */
@Composable
fun ThemedOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    text: String? = null,
    borderColor: Color? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    content: (@Composable () -> Unit)? = null
) {
    val theme = LocalLifeTheme.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = if (isPressed && enabled) 0.96f else 1f
    val strokeColor = borderColor ?: theme.colors.primary

    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = 44.dp)
            .scale(scale)
            .alpha(if (enabled) 1f else 0.45f)
            .semantics { role = Role.Button },
        shape = theme.shapes.buttonShape,
        color = Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(theme.shapes.borderWidth, strokeColor)
    ) {
        Box(
            modifier = Modifier
                .clickable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
                .padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            if (content != null) {
                content()
            } else if (text != null) {
                Text(
                    text = text,
                    style = theme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = strokeColor
                )
            }
        }
    }
}

/**
 * Text button with theme typography and subtle press scaling.
 */
@Composable
fun ThemedTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    text: String? = null,
    color: Color? = null,
    content: (@Composable () -> Unit)? = null
) {
    val theme = LocalLifeTheme.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = if (isPressed && enabled) 0.95f else 1f
    val textColor = color ?: theme.colors.primary

    Box(
        modifier = modifier
            .scale(scale)
            .alpha(if (enabled) 1f else 0.45f)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        if (content != null) {
            content()
        } else if (text != null) {
            Text(
                text = text,
                style = theme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

/**
 * Icon button wrapped in theme shape, elevation, and click feedback.
 */
@Composable
fun ThemedIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
    size: Dp = 44.dp,
    content: @Composable () -> Unit
) {
    val theme = LocalLifeTheme.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = if (isPressed && enabled) 0.92f else 1f

    Surface(
        modifier = modifier
            .size(size)
            .scale(scale)
            .alpha(if (enabled) 1f else 0.45f)
            .semantics {
                role = Role.Button
                if (contentDescription != null) this.contentDescription = contentDescription
            },
        shape = theme.shapes.buttonShape,
        color = theme.colors.surfaceVariant.copy(alpha = 0.8f),
        border = androidx.compose.foundation.BorderStroke(theme.shapes.borderWidth, theme.colors.outline.copy(alpha = 0.5f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

/**
 * Themed Floating Action Button with theme gradient and elevation.
 */
@Composable
fun ThemedFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "Action",
    size: Dp = 56.dp,
    content: @Composable () -> Unit
) {
    val theme = LocalLifeTheme.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = if (isPressed) 0.94f else 1f
    val shape = theme.shapes.fabShape

    Surface(
        modifier = modifier
            .size(size)
            .scale(scale)
            .shadow(elevation = 8.dp, shape = shape)
            .semantics {
                role = Role.Button
                this.contentDescription = contentDescription
            },
        shape = shape,
        color = Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(1.dp, theme.colors.primary.copy(alpha = 0.5f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = theme.colors.getPrimaryBrush())
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

/**
 * Themed Destructive Button for deleting items and irreversible actions.
 */
@Composable
fun ThemedDestructiveButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    text: String? = null,
    content: (@Composable () -> Unit)? = null
) {
    val theme = LocalLifeTheme.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = if (isPressed && enabled) 0.96f else 1f
    val errorRed = Color(0xFFEF4444)

    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = 44.dp)
            .scale(scale)
            .alpha(if (enabled) 1f else 0.45f)
            .semantics { role = Role.Button },
        shape = theme.shapes.buttonShape,
        color = errorRed.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, errorRed.copy(alpha = 0.6f))
    ) {
        Box(
            modifier = Modifier
                .clickable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            if (content != null) {
                content()
            } else {
                Text(
                    text = text ?: "Delete",
                    style = theme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = errorRed
                )
            }
        }
    }
}

/**
 * Animated Loading Indicator themed for each design system:
 * - Arcane: Rotating magical concentric sigil
 * - Cyber: Rotating digital scanning rings
 * - Adventure: Rotating fantasy compass needle
 * - Cosmic: Orbiting celestial bodies
 * - Glass: Rotating prismatic refraction crystal
 * - Default: Standard Material 3 circular spinner
 */
@Composable
fun ThemedLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp
) {
    val theme = LocalLifeTheme.current
    val speed = LocalLifeAnimationSpeed.current

    if (theme.id == ThemeId.DEFAULT) {
        CircularProgressIndicator(
            modifier = modifier.size(size),
            color = theme.colors.primary,
            strokeWidth = 3.dp
        )
        return
    }

    if (speed == AnimationSpeed.OFF) {
        // Safe static rendering when animation speed is Off
        Canvas(modifier = modifier.size(size)) {
            val centerOffset = Offset(size.toPx() / 2f, size.toPx() / 2f)
            val radius = (size.toPx() / 2f) * 0.85f
            when (theme.id) {
                ThemeId.ARCANE_ACADEMY -> ThemeAssetManager.drawArcaneSigil(this, centerOffset, radius, 0f, theme.colors.primary, theme.colors.secondary)
                ThemeId.CYBER_NEXUS -> ThemeAssetManager.drawCyberGrid(this, size.toPx(), size.toPx(), 0.5f, theme.colors.primary, theme.colors.outline)
                ThemeId.ADVENTURE_REALM -> ThemeAssetManager.drawFantasyCompass(this, centerOffset, radius, 0f, theme.colors.primary, theme.colors.secondary)
                ThemeId.COSMIC_EXPLORER -> ThemeAssetManager.drawCosmicOrbits(this, centerOffset, radius, 0.5f, theme.colors.primary, theme.colors.secondary)
                ThemeId.GLASS -> ThemeAssetManager.drawPrismRefraction(this, centerOffset, radius, 0f, theme.colors.primary, theme.colors.secondary)
                ThemeId.DEFAULT -> {}
            }
        }
        return
    }

    val rotationDuration = speed.calculateDuration(2400)
    val scanDuration = speed.calculateDuration(1800)

    val infiniteTransition = rememberInfiniteTransition(label = "theme_loading")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = rotationDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val scanProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = scanDuration, easing = FastOutSlowInEasing),
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
            ThemeId.GLASS -> {
                ThemeAssetManager.drawPrismRefraction(
                    scope = this,
                    center = centerOffset,
                    radius = radius,
                    rotationDegrees = rotation,
                    primaryCyan = theme.colors.primary,
                    secondaryViolet = theme.colors.secondary
                )
            }
            ThemeId.DEFAULT -> {}
        }
    }
}

/**
 * Fullscreen atmospheric background with theme-specific ambient effects,
 * built-in procedural variants, and custom personal image backgrounds.
 */
@Composable
fun ThemedAtmosphereBackground(
    modifier: Modifier = Modifier,
    backgroundConfig: ThemeBackgroundConfig = ThemeBackgroundConfig(),
    content: @Composable BoxScope.() -> Unit
) {
    val theme = LocalLifeTheme.current
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(theme.colors.background)
    ) {
        // 1. Custom Image Background Rendering (if active and accessible)
        if (backgroundConfig.isCustomImageActive) {
            val customBitmap = remember(backgroundConfig.customImageUri) {
                try {
                    val uri = Uri.parse(backgroundConfig.customImageUri)
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                } catch (e: Throwable) {
                    null
                }
            }

            if (customBitmap != null) {
                val contentScale = when (backgroundConfig.scaleMode) {
                    BackgroundScaleMode.CROP -> ContentScale.Crop
                    BackgroundScaleMode.FIT -> ContentScale.Fit
                    BackgroundScaleMode.STRETCH -> ContentScale.FillBounds
                }
                Image(
                    bitmap = customBitmap.asImageBitmap(),
                    contentDescription = "Theme Custom Background",
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(backgroundConfig.opacity),
                    contentScale = contentScale
                )

                if (backgroundConfig.enableOverlayTint) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(theme.colors.background.copy(alpha = 0.55f))
                    )
                }
            }
        }

        // 2. Procedural & Thematic Canvas Backgrounds (Built-in)
        if (!backgroundConfig.isCustomImageActive && theme.motion.enableAtmosphereGlow) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (size.width > 0f && size.height > 0f) {
                    when (backgroundConfig.type) {
                        ThemeBackgroundType.PROCEDURAL_VARIANT_1 -> {
                            when (theme.id) {
                                ThemeId.ARCANE_ACADEMY -> ThemeAssetManager.drawArcaneBackgroundPattern(this, size.width, size.height, theme.colors.primary, theme.colors.secondary)
                                ThemeId.CYBER_NEXUS -> ThemeAssetManager.drawCyberMatrixBackground(this, size.width, size.height, theme.colors.primary, theme.colors.secondary)
                                ThemeId.ADVENTURE_REALM -> ThemeAssetManager.drawAdventureMapBackground(this, size.width, size.height, theme.colors.primary, theme.colors.secondary)
                                ThemeId.COSMIC_EXPLORER -> ThemeAssetManager.drawCosmicDeepSpaceBackground(this, size.width, size.height, theme.colors.primary, theme.colors.secondary)
                                ThemeId.DEFAULT -> ThemeAssetManager.drawMinimalExecutiveBackground(this, size.width, size.height, theme.colors.primary, theme.colors.outline)
                                ThemeId.GLASS -> ThemeAssetManager.drawGlassPrismMesh(this, size.width, size.height, theme.colors.primary, theme.colors.secondary)
                            }
                        }
                        ThemeBackgroundType.PROCEDURAL_VARIANT_2 -> {
                            // Minimal ambient focus radial glow
                            val radius = size.width * 0.8f
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(theme.colors.glowColor.copy(alpha = 0.35f), Color.Transparent),
                                    center = Offset(size.width * 0.5f, size.height * 0.3f),
                                    radius = radius
                                )
                            )
                        }
                        ThemeBackgroundType.ATMOSPHERE_DEFAULT, ThemeBackgroundType.CUSTOM_IMAGE -> {
                            // Default core atmospheric motifs
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
                                    val radius = size.width * 0.7f
                                    if (radius > 0f) {
                                        drawCircle(
                                            brush = Brush.radialGradient(
                                                colors = listOf(theme.colors.glowColor, Color.Transparent),
                                                center = Offset(size.width * 0.85f, size.height * 0.15f),
                                                radius = radius
                                            )
                                        )
                                    }
                                }
                            }
                        }
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

