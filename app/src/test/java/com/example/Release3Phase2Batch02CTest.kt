package com.example

import com.example.ui.screens.manual.ComponentCategory
import com.example.ui.screens.manual.ComponentStatus
import com.example.ui.screens.manual.MasterComponentCatalog
import com.example.ui.theme.engine.AnimationSpeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Release 3 Phase 2 / Batch 02C Unit Tests:
 * - Update Settings → Reusable UI (ComponentCatalog & ComponentDetailSheet)
 * - Verify interactive live preview registrations for PremiumCard, PremiumButton, GlassTheme, AnimationSpeed
 */
class Release3Phase2Batch02CTest {

    @Test
    fun testPhase2ComponentsInMasterCatalog() {
        val catalog = MasterComponentCatalog.items
        assertTrue("Catalog size must be at least 47", catalog.size >= 47)

        // 1. Premium Card
        val premiumCard = catalog.find { it.id == "premium_card" }
        assertNotNull("premium_card must be in catalog", premiumCard)
        assertEquals("Premium Animated Card", premiumCard!!.name)
        assertEquals(ComponentCategory.VISUAL_STYLING, premiumCard.category)
        assertEquals(ComponentStatus.EXISTING, premiumCard.status)
        assertTrue(premiumCard.tags.contains("spring"))
        assertTrue(premiumCard.parameters.any { it.name == "style" })

        // 2. Premium Button
        val premiumButton = catalog.find { it.id == "premium_button" }
        assertNotNull("premium_button must be in catalog", premiumButton)
        assertEquals("Premium Spring Animated Button", premiumButton!!.name)
        assertEquals(ComponentCategory.INPUTS_FORMS, premiumButton.category)
        assertEquals(ComponentStatus.EXISTING, premiumButton.status)
        assertTrue(premiumButton.tags.contains("loading"))

        // 3. Glass Theme Panel
        val glassPanel = catalog.find { it.id == "glass_theme_panel" }
        assertNotNull("glass_theme_panel must be in catalog", glassPanel)
        assertEquals(ComponentCategory.VISUAL_STYLING, glassPanel!!.category)
        assertTrue(glassPanel.tags.contains("glassmorphism"))

        // 4. Animation Speed System
        val animSpeed = catalog.find { it.id == "animation_speed_system" }
        assertNotNull("animation_speed_system must be in catalog", animSpeed)
        assertEquals(ComponentCategory.VISUAL_STYLING, animSpeed!!.category)
        assertTrue(animSpeed.tags.contains("speed"))
    }

    @Test
    fun testSearchPhase2Components() {
        val springResults = MasterComponentCatalog.search("spring")
        assertTrue("Searching 'spring' should find premium_card or premium_button", springResults.any { it.id == "premium_card" })
        assertTrue("Searching 'spring' should find premium_button", springResults.any { it.id == "premium_button" })

        val glassResults = MasterComponentCatalog.search("glassmorphism")
        assertTrue("Searching 'glassmorphism' should find glass_theme_panel", glassResults.any { it.id == "glass_theme_panel" })

        val speedResults = MasterComponentCatalog.search("velocity")
        assertTrue("Searching 'velocity' should find animation_speed_system", speedResults.any { it.id == "animation_speed_system" })
    }

    @Test
    fun testAnimationSpeedTiersCompleteness() {
        val tiers = AnimationSpeed.values()
        assertEquals(5, tiers.size)
        assertTrue(tiers.contains(AnimationSpeed.OFF))
        assertTrue(tiers.contains(AnimationSpeed.SLOW))
        assertTrue(tiers.contains(AnimationSpeed.NORMAL))
        assertTrue(tiers.contains(AnimationSpeed.FAST))
        assertTrue(tiers.contains(AnimationSpeed.VERY_FAST))
    }
}
