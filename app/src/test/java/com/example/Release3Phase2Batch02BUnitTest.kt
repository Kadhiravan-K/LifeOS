package com.example

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.engine.CustomColorPalette
import com.example.ui.theme.engine.HoneycombEngravingType
import com.example.ui.theme.engine.IndependentThemeCustomization
import com.example.ui.theme.engine.ThemeId
import com.example.ui.theme.engine.ThemeRegistry
import com.example.ui.theme.engine.toMaterial3ColorScheme
import com.example.ui.theme.engine.withCustomPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Release 3 Phase 2 / Batch 02B Unit Tests:
 * - Requirement #4: Independent light/dark mode color customization.
 * - Requirement #7: New Glass theme with frosted translucency and specular prism motifs.
 * - Requirement #8: Distinctive theme-specific card designs and animations with shared functionality.
 */
class Release3Phase2Batch02BUnitTest {

    @Test
    fun testGlassThemeRegistrationAndIdentity() {
        val glassTheme = ThemeRegistry.GLASS_THEME
        assertNotNull("Glass theme definition must be non-null", glassTheme)
        assertEquals("ThemeId must be GLASS", ThemeId.GLASS, glassTheme.id)
        assertEquals("Glassmorphism", glassTheme.name)
        assertTrue("Glass theme must be premium", glassTheme.isPremium)
        assertTrue("Glass shape flag must be true", glassTheme.shapes.isGlass)
        assertEquals(HoneycombEngravingType.PRISM, glassTheme.honeycombEngraving)
        assertEquals("💎", glassTheme.decoration.motifEmoji)

        val retrievedFromRegistry = ThemeRegistry.getTheme(ThemeId.GLASS)
        assertEquals("Registry must return GLASS_THEME", glassTheme, retrievedFromRegistry)

        val allThemes = ThemeRegistry.allThemes
        assertTrue("ThemeRegistry.allThemes must contain GLASS", allThemes.any { it.id == ThemeId.GLASS })
        assertEquals("ThemeRegistry must now contain exactly 6 themes", 6, allThemes.size)

        // Verify each theme has a unique, distinctive identity (#8)
        val uniqueIds = allThemes.map { it.id }.toSet()
        assertEquals("Every theme must have a unique ID", allThemes.size, uniqueIds.size)
        allThemes.forEach { theme ->
            assertNotNull(theme.name)
            assertNotNull(theme.decoration.taskSectionTitle)
            assertNotNull(theme.honeycombEngraving)
        }
    }

    @Test
    fun testCustomColorPaletteOverriding() {
        val defaultPalette = CustomColorPalette()
        assertFalse("Empty palette must not be customized", defaultPalette.isCustomized)

        val defaultColor = Color(0xFF112233)
        assertEquals(defaultColor, defaultPalette.getPrimaryColor(defaultColor))

        val customRed = 0xFFFF0000L
        val customGreen = 0xFF00FF00L
        val customizedPalette = CustomColorPalette(
            primaryHex = customRed,
            accentHex = customGreen
        )

        assertTrue("Palette with hex values must be customized", customizedPalette.isCustomized)
        assertEquals(Color(customRed), customizedPalette.getPrimaryColor(defaultColor))
        assertEquals(Color(customGreen), customizedPalette.getAccentColor(defaultColor))
        // Unset slots fall back to default color
        assertEquals(defaultColor, customizedPalette.getSecondaryColor(defaultColor))

        // Verify withCustomPalette produces updated ThemeDefinition colors
        val baseTheme = ThemeRegistry.DEFAULT_THEME
        val modifiedTheme = baseTheme.withCustomPalette(customizedPalette)
        assertEquals(Color(customRed), modifiedTheme.colors.primary)
        assertEquals(Color(customGreen), modifiedTheme.colors.accent)
        // Non-overridden colors stay the same
        assertEquals(baseTheme.colors.secondary, modifiedTheme.colors.secondary)
    }

    @Test
    fun testIndependentThemeCustomizationModes() {
        val lightRed = 0xFFFF1111L
        val darkBlue = 0xFF1111FFL

        val lightPal = CustomColorPalette(primaryHex = lightRed)
        val darkPal = CustomColorPalette(primaryHex = darkBlue)

        val customization = IndependentThemeCustomization(
            isEnabled = true,
            lightPalette = lightPal,
            darkPalette = darkPal
        )

        assertTrue("Customization should be active and customized", customization.isAnyCustomized)

        val activeLight = customization.getActivePalette(isLightMode = true)
        assertEquals(Color(lightRed), activeLight.getPrimaryColor(Color.Black))

        val activeDark = customization.getActivePalette(isLightMode = false)
        assertEquals(Color(darkBlue), activeDark.getPrimaryColor(Color.Black))

        val disabledCustomization = customization.copy(isEnabled = false)
        assertFalse("Disabled customization must return false for isAnyCustomized", disabledCustomization.isAnyCustomized)
    }

    @Test
    fun testMaterial3ColorSchemeCustomPaletteIntegration() {
        val customLightPrimary = 0xFF123456L
        val customDarkPrimary = 0xFF654321L

        val lightPalette = CustomColorPalette(primaryHex = customLightPrimary)
        val darkPalette = CustomColorPalette(primaryHex = customDarkPrimary)

        // 1. Default theme light mode
        val defaultLightScheme = ThemeRegistry.DEFAULT_THEME.toMaterial3ColorScheme(
            isLightMode = true,
            customPalette = lightPalette
        )
        assertEquals(Color(customLightPrimary), defaultLightScheme.primary)

        // 2. Default theme dark mode
        val defaultDarkScheme = ThemeRegistry.DEFAULT_THEME.toMaterial3ColorScheme(
            isLightMode = false,
            customPalette = darkPalette
        )
        assertEquals(Color(customDarkPrimary), defaultDarkScheme.primary)

        // 3. Glass theme with dark palette
        val glassCustomScheme = ThemeRegistry.GLASS_THEME.toMaterial3ColorScheme(
            isLightMode = false,
            customPalette = darkPalette
        )
        assertEquals(Color(customDarkPrimary), glassCustomScheme.primary)
    }
}
