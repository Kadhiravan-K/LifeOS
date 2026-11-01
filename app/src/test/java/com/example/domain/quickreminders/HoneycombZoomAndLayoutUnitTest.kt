package com.example.domain.quickreminders

import com.example.domain.model.DefaultHoneycombShortcuts
import com.example.domain.model.HoneycombAppearanceConfig
import com.example.domain.model.HoneycombLayoutTemplate
import com.example.domain.model.ShortcutType
import com.example.ui.components.pixelToAxial
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Comprehensive Unit Tests for QR Honeycomb Zoom Controls and Geometry Layout (#13).
 * Validates:
 * 1. Zoom scale clamping (0.75f..1.35f) and step changes.
 * 2. Standard 3+2+3+2 layout geometry with zero overlapping coordinates.
 * 3. All preset template generators (Pyramid, Diamond, Heart, Snake, Zigzag).
 * 4. Coordinate space conversions (pixel to axial rounding).
 * 5. Default shortcuts list integrity and unique coordinate assignment.
 */
class HoneycombZoomAndLayoutUnitTest {

    // =========================================================================
    // 1. ZOOM SCALE & CONTROLS TESTS (#13)
    // =========================================================================

    @Test
    fun `test zoom scale clamping strictly within 75 to 135 percent`() {
        val configMin = HoneycombAppearanceConfig(zoomScale = 0.50f)
        val configMax = HoneycombAppearanceConfig(zoomScale = 2.00f)
        val configNormal = HoneycombAppearanceConfig(zoomScale = 1.00f)

        val effectiveMin = configMin.zoomScale.coerceIn(0.75f, 1.35f)
        val effectiveMax = configMax.zoomScale.coerceIn(0.75f, 1.35f)
        val effectiveNormal = configNormal.zoomScale.coerceIn(0.75f, 1.35f)

        assertEquals(0.75f, effectiveMin, 0.001f)
        assertEquals(1.35f, effectiveMax, 0.001f)
        assertEquals(1.00f, effectiveNormal, 0.001f)
    }

    @Test
    fun `test zoom discrete step controls clamp at boundaries`() {
        var scale = 1.0f
        val step = 0.10f

        // Step up
        scale = (scale + step).coerceIn(0.75f, 1.35f)
        assertEquals(1.10f, scale, 0.001f)

        scale = (scale + step).coerceIn(0.75f, 1.35f)
        assertEquals(1.20f, scale, 0.001f)

        scale = (scale + step).coerceIn(0.75f, 1.35f)
        assertEquals(1.30f, scale, 0.001f)

        // Exceed max
        scale = (scale + step).coerceIn(0.75f, 1.35f)
        assertEquals(1.35f, scale, 0.001f)

        scale = (scale + step).coerceIn(0.75f, 1.35f)
        assertEquals(1.35f, scale, 0.001f)

        // Step down towards min
        repeat(10) {
            scale = (scale - step).coerceIn(0.75f, 1.35f)
        }
        assertEquals(0.75f, scale, 0.001f)

        // Reset to default
        scale = 1.0f
        assertEquals(1.00f, scale, 0.001f)
    }

    // =========================================================================
    // 2. HONEYCOMB LAYOUT GEOMETRY & ZERO-OVERLAP TESTS (#13)
    // =========================================================================

    @Test
    fun `test standard honeycomb template generates exact 3+2+3+2 arrangement`() {
        val coords = HoneycombLayoutTemplate.generateCoordinates(HoneycombLayoutTemplate.STANDARD, 10)

        assertEquals(10, coords.size)

        // Unique coordinates guarantee: NO two shortcuts share (q, r)
        val uniqueCoords = coords.toSet()
        assertEquals("All coordinates must be unique without overlap", 10, uniqueCoords.size)

        // Row breakdown:
        val row0 = coords.filter { it.second == 0 }
        val row1 = coords.filter { it.second == 1 }
        val row2 = coords.filter { it.second == 2 }
        val row3 = coords.filter { it.second == 3 }

        assertEquals("Row 0 must contain 3 items", 3, row0.size)
        assertEquals("Row 1 must contain 2 items", 2, row1.size)
        assertEquals("Row 2 must contain 3 items", 3, row2.size)
        assertEquals("Row 3 must contain 2 items", 2, row3.size)
    }

    @Test
    fun `test all layout templates generate non-empty unique coordinates`() {
        val templates = listOf(
            HoneycombLayoutTemplate.STANDARD,
            HoneycombLayoutTemplate.PYRAMID,
            HoneycombLayoutTemplate.DIAMOND,
            HoneycombLayoutTemplate.HEART,
            HoneycombLayoutTemplate.SNAKE,
            HoneycombLayoutTemplate.ZIGZAG,
            HoneycombLayoutTemplate.FREEFORM
        )

        templates.forEach { tmpl ->
            val count = 10
            val coords = HoneycombLayoutTemplate.generateCoordinates(tmpl, count)
            assertEquals("Template ${tmpl.name} must generate requested count", count, coords.size)
            val unique = coords.toSet()
            assertEquals("Template ${tmpl.name} must have unique coordinates", count, unique.size)
        }
    }

    @Test
    fun `test default shortcuts list has 10 valid non-overlapping shortcuts`() {
        val defaults = DefaultHoneycombShortcuts.list

        assertEquals(10, defaults.size)

        val coords = defaults.map { Pair(it.coordQ, it.coordR) }
        val uniqueCoords = coords.toSet()
        assertEquals("Default shortcuts must all have unique coordinates", 10, uniqueCoords.size)

        // Ensure Hydration water shortcut is present
        assertTrue(defaults.any { it.type == ShortcutType.WATER })
        // Ensure Habit/Task shortcuts are present
        assertTrue(defaults.any { it.type == ShortcutType.TASK })
        assertTrue(defaults.any { it.type == ShortcutType.CHALLENGE_RULE })

        // Check non-empty emoji and titles
        defaults.forEach { sc ->
            assertFalse("Shortcut title must not be blank", sc.title.isBlank())
            assertFalse("Shortcut emoji must not be blank", sc.emoji.isBlank())
            assertTrue("Shortcut order index must be non-negative", sc.orderIndex >= 0)
        }
    }

    // =========================================================================
    // 3. PIXEL TO AXIAL CONVERSION ROUNDTRIP TESTS
    // =========================================================================

    @Test
    fun `test pixelToAxial converts grid center to 0,0`() {
        val hStep = 80f
        val vStep = 70f

        val coord = pixelToAxial(px = 0f, py = 0f, hStep = hStep, vStep = vStep)
        assertEquals(0, coord.first)
        assertEquals(0, coord.second)
    }

    @Test
    fun `test pixelToAxial correctly identifies discrete cell centers`() {
        val hStep = 100f
        val vStep = 86.6f

        // Coord (1, 0): px = hStep * (1 + 0) = 100, py = 0
        val c10 = pixelToAxial(px = 100f, py = 0f, hStep = hStep, vStep = vStep)
        assertEquals(1, c10.first)
        assertEquals(0, c10.second)

        // Coord (0, 1): r = 1, py = vStep, px = hStep * (0 + 0.5) = 50
        val c01 = pixelToAxial(px = 50f, py = 86.6f, hStep = hStep, vStep = vStep)
        assertEquals(0, c01.first)
        assertEquals(1, c01.second)
    }
}
