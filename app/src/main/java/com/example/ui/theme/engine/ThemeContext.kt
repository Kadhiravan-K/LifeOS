package com.example.ui.theme.engine

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.AccentAmberDark
import com.example.ui.theme.AccentAmberLight
import com.example.ui.theme.OnPrimaryDark
import com.example.ui.theme.OnPrimaryLight
import com.example.ui.theme.OnSurfaceDark
import com.example.ui.theme.OnSurfaceLight
import com.example.ui.theme.OnSurfaceVariantDark
import com.example.ui.theme.OnSurfaceVariantLight
import com.example.ui.theme.PrimaryDark
import com.example.ui.theme.PrimaryLight
import com.example.ui.theme.SecondaryDark
import com.example.ui.theme.SecondaryLight
import com.example.ui.theme.SlateBackgroundDark
import com.example.ui.theme.SlateBackgroundLight
import com.example.ui.theme.SlateBorderDark
import com.example.ui.theme.SlateBorderLight
import com.example.ui.theme.SlateSurfaceDark
import com.example.ui.theme.SlateSurfaceLight
import com.example.ui.theme.SlateSurfaceVariantDark
import com.example.ui.theme.SlateSurfaceVariantLight
import com.example.ui.theme.Typography

val LocalLifeTheme = staticCompositionLocalOf { ThemeRegistry.DEFAULT_THEME }

/**
 * Maps a ThemeDefinition to Material 3 ColorScheme to maintain 100% interoperability with all M3 components.
 */
fun ThemeDefinition.toMaterial3ColorScheme(isLightMode: Boolean = false): ColorScheme {
    if (id == ThemeId.DEFAULT && isLightMode) {
        return lightColorScheme(
            primary = PrimaryLight,
            onPrimary = OnPrimaryLight,
            secondary = SecondaryLight,
            tertiary = AccentAmberLight,
            background = SlateBackgroundLight,
            surface = SlateSurfaceLight,
            surfaceVariant = SlateSurfaceVariantLight,
            onBackground = OnSurfaceLight,
            onSurface = OnSurfaceLight,
            onSurfaceVariant = OnSurfaceVariantLight,
            outline = SlateBorderLight
        )
    }

    if (id == ThemeId.DEFAULT) {
        return darkColorScheme(
            primary = PrimaryDark,
            onPrimary = OnPrimaryDark,
            secondary = SecondaryDark,
            tertiary = AccentAmberDark,
            background = SlateBackgroundDark,
            surface = SlateSurfaceDark,
            surfaceVariant = SlateSurfaceVariantDark,
            onBackground = OnSurfaceDark,
            onSurface = OnSurfaceDark,
            onSurfaceVariant = OnSurfaceVariantDark,
            outline = SlateBorderDark
        )
    }

    // Immersive themes map their bespoke color palettes into M3 tokens
    return darkColorScheme(
        primary = colors.primary,
        onPrimary = colors.onPrimary,
        secondary = colors.secondary,
        tertiary = colors.accent,
        background = colors.background,
        surface = colors.surface,
        surfaceVariant = colors.surfaceVariant,
        onBackground = colors.onBackground,
        onSurface = colors.onSurface,
        onSurfaceVariant = colors.onSurfaceVariant,
        outline = colors.outline
    )
}

@Composable
fun LifeOSThemeProvider(
    theme: ThemeDefinition,
    isLightMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val m3ColorScheme = theme.toMaterial3ColorScheme(isLightMode = isLightMode)

    CompositionLocalProvider(
        LocalLifeTheme provides theme
    ) {
        MaterialTheme(
            colorScheme = m3ColorScheme,
            typography = Typography,
            content = content
        )
    }
}
