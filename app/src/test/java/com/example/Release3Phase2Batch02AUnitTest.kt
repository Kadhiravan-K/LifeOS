package com.example

import com.example.ui.components.PremiumButtonStyle
import com.example.ui.components.PremiumCardStyle
import com.example.ui.theme.engine.AnimationSpeed
import com.example.ui.theme.engine.ThemeMotion
import com.example.ui.theme.engine.ThemeShapes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Release 3 Phase 2 — Batch 02A Unit Tests.
 *
 * Covers:
 * - #1 Premium card & button primitives and animation design tokens.
 * - #23 Global animation speed token calculations, zero-speed safety, and scaling.
 */
class Release3Phase2Batch02AUnitTest {

    // =========================================================================
    // 1. ANIMATION SPEED TOKENS (#23)
    // =========================================================================

    @Test
    fun `test AnimationSpeed calculateDuration handles all speed tiers correctly`() {
        val baseDuration = 2400

        // Tier 1: OFF (0.0x) -> 0ms (instant/no animation)
        assertEquals(0, AnimationSpeed.OFF.calculateDuration(baseDuration))

        // Tier 2: SLOW (0.5x) -> 4800ms
        assertEquals(4800, AnimationSpeed.SLOW.calculateDuration(baseDuration))

        // Tier 3: NORMAL (1.0x) -> 2400ms
        assertEquals(2400, AnimationSpeed.NORMAL.calculateDuration(baseDuration))

        // Tier 4: FAST (1.5x) -> 1600ms
        assertEquals(1600, AnimationSpeed.FAST.calculateDuration(baseDuration))

        // Tier 5: VERY_FAST (2.0x) -> 1200ms
        assertEquals(1200, AnimationSpeed.VERY_FAST.calculateDuration(baseDuration))
    }

    @Test
    fun `test AnimationSpeed fromMultiplier parses valid and edge values safely`() {
        assertEquals(AnimationSpeed.OFF, AnimationSpeed.fromMultiplier(0f))
        assertEquals(AnimationSpeed.SLOW, AnimationSpeed.fromMultiplier(0.5f))
        assertEquals(AnimationSpeed.NORMAL, AnimationSpeed.fromMultiplier(1.0f))
        assertEquals(AnimationSpeed.FAST, AnimationSpeed.fromMultiplier(1.5f))
        assertEquals(AnimationSpeed.VERY_FAST, AnimationSpeed.fromMultiplier(2.0f))

        // Null and invalid fallback to NORMAL
        assertEquals(AnimationSpeed.NORMAL, AnimationSpeed.fromMultiplier(null))
        assertEquals(AnimationSpeed.NORMAL, AnimationSpeed.fromMultiplier(0.95f))
        assertEquals(AnimationSpeed.FAST, AnimationSpeed.fromMultiplier(1.4f))
    }

    @Test
    fun `test ThemeMotion getEffectiveDuration computes scaled duration`() {
        val motion = ThemeMotion(
            animationDurationMs = 300,
            speedMultiplier = 1.5f
        )
        assertEquals(200, motion.getEffectiveDuration())

        val zeroMotion = ThemeMotion(
            animationDurationMs = 300,
            speedMultiplier = 0f
        )
        assertEquals(0, zeroMotion.getEffectiveDuration())
    }

    // =========================================================================
    // 2. PREMIUM COMPONENT TOKENS (#1)
    // =========================================================================

    @Test
    fun `test PremiumCardStyle has all expected visual design styles`() {
        val styles = PremiumCardStyle.entries
        assertTrue(styles.contains(PremiumCardStyle.DEFAULT))
        assertTrue(styles.contains(PremiumCardStyle.ELEVATED))
        assertTrue(styles.contains(PremiumCardStyle.GLASS))
        assertTrue(styles.contains(PremiumCardStyle.ACCENT_BORDER))
        assertTrue(styles.contains(PremiumCardStyle.THEME_BESPOKE))
    }

    @Test
    fun `test PremiumButtonStyle has all expected button states`() {
        val styles = PremiumButtonStyle.entries
        assertTrue(styles.contains(PremiumButtonStyle.PRIMARY))
        assertTrue(styles.contains(PremiumButtonStyle.SECONDARY))
        assertTrue(styles.contains(PremiumButtonStyle.OUTLINED))
        assertTrue(styles.contains(PremiumButtonStyle.GLASS))
        assertTrue(styles.contains(PremiumButtonStyle.DESTRUCTIVE))
    }

    @Test
    fun `test ThemeShapes supports glass styling flag`() {
        val standardShapes = ThemeShapes()
        assertFalse("Default shapes must not be glass", standardShapes.isGlass)

        val glassShapes = ThemeShapes(isGlass = true)
        assertTrue("Glass shapes must report isGlass as true", glassShapes.isGlass)
    }
}
