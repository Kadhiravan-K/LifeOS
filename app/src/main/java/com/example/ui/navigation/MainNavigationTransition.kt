package com.example.ui.navigation

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.engine.LocalLifeTheme

/**
 * Premium Themed Navigation Transition Wrapper.
 *
 * Requirements:
 * - 1500 ms animation duration.
 * - Compact theme-specific motif badge and atmospheric aura.
 * - Destination content rendered immediately and clearly visible beneath overlay.
 * - Touch pass-through (never blocks essential user interactions).
 * - System animator duration scale check (respects reduced/disabled animations).
 * - Clean cancellation and re-triggering during rapid navigation.
 */
@Composable
fun MainNavigationTransition(
    targetItem: NavigationItem,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val activeTheme = LocalLifeTheme.current

    // Check system animation settings
    val isAnimationDisabled = remember(context) {
        try {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            scale <= 0.05f
        } catch (_: Exception) {
            false
        }
    }

    var lastRenderedItem by remember { mutableStateOf(targetItem) }
    var isTransitionRunning by remember { mutableStateOf(false) }

    val overlayAlpha = remember { Animatable(0f) }
    val motifScale = remember { Animatable(0.85f) }
    val motifFloatOffset = remember { Animatable(12f) }

    LaunchedEffect(targetItem) {
        if (targetItem != lastRenderedItem) {
            lastRenderedItem = targetItem

            if (isAnimationDisabled) {
                isTransitionRunning = false
                overlayAlpha.snapTo(0f)
                return@LaunchedEffect
            }

            // Rapid navigation safeguard: reset cleanly
            isTransitionRunning = true
            overlayAlpha.snapTo(0f)
            motifScale.snapTo(0.85f)
            motifFloatOffset.snapTo(12f)

            // Phase 1 (0..300ms): Smooth entry
            overlayAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            )
            motifScale.animateTo(
                targetValue = 1.05f,
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            )

            // Phase 2 (300..1200ms): Floating atmospheric pulse (900ms)
            motifFloatOffset.animateTo(
                targetValue = -6f,
                animationSpec = tween(durationMillis = 900, easing = LinearEasing)
            )

            // Phase 3 (1200..1500ms): Gentle departure
            motifScale.animateTo(
                targetValue = 1.15f,
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            )
            overlayAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            )

            isTransitionRunning = false
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Destination screen content: rendered immediately beneath overlay
        content()

        // Atmospheric non-blocking overlay: only visible during transition
        if (isTransitionRunning && overlayAlpha.value > 0.01f) {
            val alpha = overlayAlpha.value

            // Note: No clickable or pointerInput modifier on this Box to ensure touch events
            // pass straight through to the underlying destination content without blocking interactions!
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Color.Black.copy(alpha = alpha * 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Subtle glowing aura centered behind motif
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(motifScale.value)
                        .alpha(alpha * 0.4f)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    activeTheme.colors.glowColor.copy(alpha = 0.5f),
                                    activeTheme.colors.primary.copy(alpha = 0.2f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )

                // Compact Themed Motif Badge
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .offset(y = motifFloatOffset.value.dp)
                        .scale(motifScale.value)
                        .alpha(alpha)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(activeTheme.shapes.buttonShape)
                            .background(activeTheme.colors.surface.copy(alpha = 0.85f))
                            .border(
                                width = 1.5.dp,
                                color = activeTheme.colors.primary.copy(alpha = 0.75f),
                                shape = activeTheme.shapes.buttonShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = activeTheme.decoration.motifEmoji,
                            fontSize = 26.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = targetItem.label,
                        style = activeTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = activeTheme.colors.onSurface.copy(alpha = 0.9f),
                        modifier = Modifier
                            .background(
                                color = activeTheme.colors.surface.copy(alpha = 0.7f),
                                shape = activeTheme.shapes.chipShape
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
