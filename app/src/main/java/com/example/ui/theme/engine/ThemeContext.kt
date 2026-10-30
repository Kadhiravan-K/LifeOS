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
val LocalLifeAnimationSpeed = staticCompositionLocalOf { AnimationSpeed.NORMAL }
val LocalLifeCustomization = staticCompositionLocalOf { IndependentThemeCustomization() }

/**
 * Creates a copy of ThemeDefinition with overridden colors from a CustomColorPalette.
 */
fun ThemeDefinition.withCustomPalette(palette: CustomColorPalette): ThemeDefinition {
    if (!palette.isCustomized) return this
    val newColors = colors.copy(
        primary = palette.getPrimaryColor(colors.primary),
        secondary = palette.getSecondaryColor(colors.secondary),
        accent = palette.getAccentColor(colors.accent),
        background = palette.getBackgroundColor(colors.background),
        surface = palette.getSurfaceColor(colors.surface),
        surfaceVariant = palette.cardHex?.let { Color(it) } ?: colors.surfaceVariant
    )
    return this.copy(colors = newColors)
}

/**
 * Maps a ThemeDefinition to Material 3 ColorScheme to maintain 100% interoperability with all M3 components.
 */
fun ThemeDefinition.toMaterial3ColorScheme(
    isLightMode: Boolean = false,
    customPalette: CustomColorPalette? = null
): ColorScheme {
    val effectiveTheme = if (customPalette != null && customPalette.isCustomized) {
        withCustomPalette(customPalette)
    } else this

    if (id == ThemeId.DEFAULT && isLightMode) {
        val basePrimary = customPalette?.getPrimaryColor(PrimaryLight) ?: PrimaryLight
        val baseSecondary = customPalette?.getSecondaryColor(SecondaryLight) ?: SecondaryLight
        val baseTertiary = customPalette?.getAccentColor(AccentAmberLight) ?: AccentAmberLight
        val baseBg = customPalette?.getBackgroundColor(SlateBackgroundLight) ?: SlateBackgroundLight
        val baseSurface = customPalette?.getSurfaceColor(SlateSurfaceLight) ?: SlateSurfaceLight
        val baseSurfaceVariant = customPalette?.cardHex?.let { Color(it) } ?: SlateSurfaceVariantLight

        return lightColorScheme(
            primary = basePrimary,
            onPrimary = OnPrimaryLight,
            secondary = baseSecondary,
            tertiary = baseTertiary,
            background = baseBg,
            surface = baseSurface,
            surfaceVariant = baseSurfaceVariant,
            onBackground = OnSurfaceLight,
            onSurface = OnSurfaceLight,
            onSurfaceVariant = OnSurfaceVariantLight,
            outline = SlateBorderLight
        )
    }

    if (id == ThemeId.DEFAULT) {
        val basePrimary = customPalette?.getPrimaryColor(PrimaryDark) ?: PrimaryDark
        val baseSecondary = customPalette?.getSecondaryColor(SecondaryDark) ?: SecondaryDark
        val baseTertiary = customPalette?.getAccentColor(AccentAmberDark) ?: AccentAmberDark
        val baseBg = customPalette?.getBackgroundColor(SlateBackgroundDark) ?: SlateBackgroundDark
        val baseSurface = customPalette?.getSurfaceColor(SlateSurfaceDark) ?: SlateSurfaceDark
        val baseSurfaceVariant = customPalette?.cardHex?.let { Color(it) } ?: SlateSurfaceVariantDark

        return darkColorScheme(
            primary = basePrimary,
            onPrimary = OnPrimaryDark,
            secondary = baseSecondary,
            tertiary = baseTertiary,
            background = baseBg,
            surface = baseSurface,
            surfaceVariant = baseSurfaceVariant,
            onBackground = OnSurfaceDark,
            onSurface = OnSurfaceDark,
            onSurfaceVariant = OnSurfaceVariantDark,
            outline = SlateBorderDark
        )
    }

    // Immersive themes map their bespoke color palettes into M3 tokens
    return darkColorScheme(
        primary = effectiveTheme.colors.primary,
        onPrimary = effectiveTheme.colors.onPrimary,
        secondary = effectiveTheme.colors.secondary,
        tertiary = effectiveTheme.colors.accent,
        background = effectiveTheme.colors.background,
        surface = effectiveTheme.colors.surface,
        surfaceVariant = effectiveTheme.colors.surfaceVariant,
        onBackground = effectiveTheme.colors.onBackground,
        onSurface = effectiveTheme.colors.onSurface,
        onSurfaceVariant = effectiveTheme.colors.onSurfaceVariant,
        outline = effectiveTheme.colors.outline
    )
}

@Composable
fun LifeOSThemeProvider(
    theme: ThemeDefinition,
    isLightMode: Boolean = false,
    animationSpeed: AnimationSpeed = AnimationSpeed.NORMAL,
    customization: IndependentThemeCustomization = IndependentThemeCustomization(),
    content: @Composable () -> Unit
) {
    val activePalette = if (customization.isEnabled) customization.getActivePalette(isLightMode) else null
    val effectiveTheme = if (activePalette != null && activePalette.isCustomized) {
        theme.withCustomPalette(activePalette)
    } else {
        theme
    }
    val m3ColorScheme = effectiveTheme.toMaterial3ColorScheme(isLightMode = isLightMode, customPalette = activePalette)

    CompositionLocalProvider(
        LocalLifeTheme provides effectiveTheme,
        LocalLifeAnimationSpeed provides animationSpeed,
        LocalLifeCustomization provides customization
    ) {
        MaterialTheme(
            colorScheme = m3ColorScheme,
            typography = Typography,
            content = content
        )
    }
}
