package com.example.ui.theme.engine

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Color tokens defining the complete palette for an immersive theme.
 */
data class ThemeColors(
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val onSecondary: Color,
    val accent: Color,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val onBackground: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val glowColor: Color,
    val gradientPrimary: List<Color>,
    val gradientSurface: List<Color>,
    val honeycombBorder: Color,
    val honeycombDefaultFill: Color,
    val badgeBackground: Color,
    val isDarkPalette: Boolean = true
) {
    fun getPrimaryBrush(): Brush = Brush.horizontalGradient(gradientPrimary)
    fun getSurfaceBrush(): Brush = Brush.verticalGradient(gradientSurface)
}

/**
 * Typography tokens tailored to the theme's aesthetic.
 */
data class ThemeTypography(
    val displayLarge: TextStyle,
    val titleLarge: TextStyle,
    val titleMedium: TextStyle,
    val bodyLarge: TextStyle,
    val bodyMedium: TextStyle,
    val bodySmall: TextStyle = TextStyle(fontSize = 11.sp),
    val labelMedium: TextStyle,
    val labelSmall: TextStyle
) {
    companion object {
        fun createDefault(fontFamily: FontFamily = FontFamily.Default): ThemeTypography {
            return ThemeTypography(
                displayLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold, fontSize = 28.sp),
                titleLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
                titleMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 16.sp),
                bodyLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 15.sp),
                bodyMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 13.sp),
                bodySmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 11.sp),
                labelMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                labelSmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 10.sp)
            )
        }
    }
}

/**
 * Geometry and shape tokens for card borders, corners, buttons, and badges.
 */
data class ThemeShapes(
    val cardShape: Shape = RoundedCornerShape(16.dp),
    val buttonShape: Shape = RoundedCornerShape(12.dp),
    val chipShape: Shape = RoundedCornerShape(8.dp),
    val dialogShape: Shape = RoundedCornerShape(20.dp),
    val borderWidth: Dp = 1.dp,
    val cornerRadiusDp: Dp = 16.dp,
    val isAngularCyber: Boolean = false,
    val isGlass: Boolean = false
)

/**
 * Global loading and transition animation speed preferences.
 */
enum class AnimationSpeed(val multiplier: Float, val label: String) {
    OFF(0.0f, "Off (0×)"),
    SLOW(0.5f, "0.5× (Slow)"),
    NORMAL(1.0f, "1.0× (Normal)"),
    FAST(1.5f, "1.5× (Fast)"),
    VERY_FAST(2.0f, "2.0× (Very Fast)");

    companion object {
        fun fromMultiplier(multiplier: Float?): AnimationSpeed {
            if (multiplier == null) return NORMAL
            return entries.minByOrNull { kotlin.math.abs(it.multiplier - multiplier) } ?: NORMAL
        }
    }

    fun calculateDuration(baseMs: Int): Int {
        if (multiplier <= 0f) return 0
        return (baseMs / multiplier).toInt()
    }
}

/**
 * Motion parameters for physics springs and animation durations.
 */
data class ThemeMotion(
    val animationDurationMs: Int = 300,
    val enableParticleEffects: Boolean = true,
    val enableAtmosphereGlow: Boolean = true,
    val bounceStiffness: Float = 300f,
    val speedMultiplier: Float = 1.0f
) {
    fun getEffectiveDuration(baseMs: Int = animationDurationMs): Int {
        if (speedMultiplier <= 0f) return 0
        return (baseMs / speedMultiplier).toInt()
    }
}

/**
 * Visual motifs and decorative tags for the theme.
 */
data class ThemeDecoration(
    val motifEmoji: String,
    val tagline: String,
    val taskSectionTitle: String,
    val habitsSectionTitle: String,
    val challengeSectionTitle: String,
    val honeycombTitle: String
)
