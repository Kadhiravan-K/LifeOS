package com.example.ui.theme

import androidx.compose.ui.graphics.luminance
import com.example.ui.theme.engine.BackgroundScaleMode
import com.example.ui.theme.engine.ThemeBackgroundConfig
import com.example.ui.theme.engine.ThemeBackgroundType
import com.example.ui.theme.engine.ThemeId
import com.example.ui.theme.engine.ThemeRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeEngineRedesignUnitTest {

    @Test
    fun themeRegistry_containsExactlySixThemes() {
        val allThemes = ThemeRegistry.allThemes
        assertEquals("ThemeRegistry must register exactly 6 complete design systems", 6, allThemes.size)

        val expectedIds = setOf(
            ThemeId.DEFAULT,
            ThemeId.ARCANE_ACADEMY,
            ThemeId.CYBER_NEXUS,
            ThemeId.ADVENTURE_REALM,
            ThemeId.COSMIC_EXPLORER,
            ThemeId.GLASS
        )
        val registeredIds = allThemes.map { it.id }.toSet()
        assertEquals("All 6 required theme IDs must be present", expectedIds, registeredIds)
    }

    @Test
    fun themeRegistry_glassTheme_hasProperDisplayNameAndSubtitle() {
        val glassTheme = ThemeRegistry.getTheme(ThemeId.GLASS)
        assertNotNull("Glass theme must be registered", glassTheme)
        assertEquals("Glassmorphism", glassTheme.name)
        assertEquals("Frosted Specular Elegance", glassTheme.subtitle)
        assertTrue("Glass theme must have glassmorphism shape enabled", glassTheme.shapes.isGlass)
    }

    @Test
    fun themeRegistry_allThemesHaveDistinctLightAndDarkPalettes() {
        ThemeRegistry.allThemes.forEach { theme ->
            val darkColors = ThemeRegistry.getEffectiveColors(theme, isLightMode = false)
            val lightColors = ThemeRegistry.getEffectiveColors(theme, isLightMode = true)

            assertNotNull("${theme.name} dark colors must not be null", darkColors)
            assertNotNull("${theme.name} light colors must not be null", lightColors)

            // Verify background luminance difference
            val darkLuminance = darkColors.background.luminance()
            val lightLuminance = lightColors.background.luminance()
            assertTrue(
                "${theme.name}: Light mode background ($lightLuminance) should be significantly brighter than dark mode ($darkLuminance)",
                lightLuminance > darkLuminance
            )

            // Verify surface text contrast
            val darkOnSurfaceLuminance = darkColors.onSurface.luminance()
            val lightOnSurfaceLuminance = lightColors.onSurface.luminance()
            assertTrue(
                "${theme.name}: Dark mode onSurface should have high luminance on dark surface",
                darkOnSurfaceLuminance > darkColors.surface.luminance()
            )
            assertTrue(
                "${theme.name}: Light mode onSurface should have lower luminance than light surface",
                lightOnSurfaceLuminance < lightColors.surface.luminance()
            )

            // Primary color must be defined and distinct from background
            assertNotEquals(darkColors.primary, darkColors.background)
            assertNotEquals(lightColors.primary, lightColors.background)
        }
    }

    @Test
    fun themeShapes_haveThemeSpecificTokens() {
        val cyberTheme = ThemeRegistry.getTheme(ThemeId.CYBER_NEXUS)
        assertTrue("Cyber Nexus must have angular cut corners", cyberTheme.shapes.isAngularCyber)
        assertNotNull(cyberTheme.shapes.fabShape)

        val cosmicTheme = ThemeRegistry.getTheme(ThemeId.COSMIC_EXPLORER)
        assertTrue("Cosmic Explorer must have orbital cosmic pill shapes", cosmicTheme.shapes.isOrbitalCosmic)

        val arcaneTheme = ThemeRegistry.getTheme(ThemeId.ARCANE_ACADEMY)
        assertTrue("Arcane Academy must have runic arcane shapes", arcaneTheme.shapes.isRunicArcane)

        val adventureTheme = ThemeRegistry.getTheme(ThemeId.ADVENTURE_REALM)
        assertTrue("Adventure Realm must have embossed adventure shapes", adventureTheme.shapes.isEmbossedAdventure)

        val glassTheme = ThemeRegistry.getTheme(ThemeId.GLASS)
        assertTrue("Glass Theme must have isGlass = true", glassTheme.shapes.isGlass)
    }

    @Test
    fun themeBackgroundConfig_defaultValuesAreValid() {
        val config = ThemeBackgroundConfig(themeId = ThemeId.DEFAULT)
        assertEquals(ThemeBackgroundType.ATMOSPHERE_DEFAULT, config.type)
        assertEquals(null, config.customImageUri)
        assertEquals(0.85f, config.opacity, 0.01f)
        assertEquals(BackgroundScaleMode.CROP, config.scaleMode)
        assertTrue(config.enableOverlayTint)
        assertFalse("Default background should not flag custom image active", config.isCustomImageActive)
    }

    @Test
    fun themeBackgroundConfig_customImageActivation() {
        val activeConfig = ThemeBackgroundConfig(
            themeId = ThemeId.CYBER_NEXUS,
            type = ThemeBackgroundType.CUSTOM_IMAGE,
            customImageUri = "content://media/external/images/media/42"
        )
        assertTrue(activeConfig.isCustomImageActive)

        val blankUriConfig = ThemeBackgroundConfig(
            themeId = ThemeId.CYBER_NEXUS,
            type = ThemeBackgroundType.CUSTOM_IMAGE,
            customImageUri = "   "
        )
        assertFalse(blankUriConfig.isCustomImageActive)

        val wrongTypeConfig = ThemeBackgroundConfig(
            themeId = ThemeId.CYBER_NEXUS,
            type = ThemeBackgroundType.ATMOSPHERE_DEFAULT,
            customImageUri = "content://media/external/images/media/42"
        )
        assertFalse(wrongTypeConfig.isCustomImageActive)
    }

    @Test
    fun themeBackgroundType_fromId_handlesExpectedAndFallbackValues() {
        assertEquals(ThemeBackgroundType.ATMOSPHERE_DEFAULT, ThemeBackgroundType.fromId("default"))
        assertEquals(ThemeBackgroundType.PROCEDURAL_VARIANT_1, ThemeBackgroundType.fromId("variant_1"))
        assertEquals(ThemeBackgroundType.PROCEDURAL_VARIANT_2, ThemeBackgroundType.fromId("variant_2"))
        assertEquals(ThemeBackgroundType.CUSTOM_IMAGE, ThemeBackgroundType.fromId("custom_image"))
        // Case insensitivity
        assertEquals(ThemeBackgroundType.CUSTOM_IMAGE, ThemeBackgroundType.fromId("CUSTOM_IMAGE"))
        // Fallback for null or unknown
        assertEquals(ThemeBackgroundType.ATMOSPHERE_DEFAULT, ThemeBackgroundType.fromId(null))
        assertEquals(ThemeBackgroundType.ATMOSPHERE_DEFAULT, ThemeBackgroundType.fromId("non_existent_variant"))
    }

    @Test
    fun backgroundScaleMode_entriesCoverAllOptions() {
        val modes = BackgroundScaleMode.entries.map { it.name }.toSet()
        assertEquals(setOf("CROP", "FIT", "STRETCH"), modes)
    }

    @Test
    fun opacityClamping_limitsValidRange() {
        val rawLow = 0.02f
        val clampedLow = rawLow.coerceIn(0.1f, 1.0f)
        assertEquals(0.1f, clampedLow, 0.001f)

        val rawHigh = 1.5f
        val clampedHigh = rawHigh.coerceIn(0.1f, 1.0f)
        assertEquals(1.0f, clampedHigh, 0.001f)

        val rawValid = 0.65f
        val clampedValid = rawValid.coerceIn(0.1f, 1.0f)
        assertEquals(0.65f, clampedValid, 0.001f)
    }

    @Test
    fun themeId_serializationAndLookup() {
        ThemeId.entries.forEach { id ->
            val fromId = ThemeId.fromId(id.idString)
            assertEquals("ThemeId.fromId must match idString for $id", id, fromId)
        }
        assertEquals("Unknown id string must fallback to DEFAULT", ThemeId.DEFAULT, ThemeId.fromId("random_non_existent"))
        assertEquals("Null id string must fallback to DEFAULT", ThemeId.DEFAULT, ThemeId.fromId(null))
    }
}
