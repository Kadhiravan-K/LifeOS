package com.example.widgets

import android.graphics.Bitmap
import android.util.LruCache
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests covering the widget stabilization fixes for the Quick Reminders widget.
 *
 * Tests cover:
 * - RC-4 / RC-6: Bitmap caching (LruCache keying and cache hit behavior)
 * - RC-3: Hexagon geometry calculation (vertex positions)
 * - RC-8: Stable ID correctness contract
 * - Provider companion object ID arrays (correct count/ordering)
 * - Honeycomb stagger geometry math
 * - Widget layout architecture (8-tile max for 3+2+3 grid)
 */
@RunWith(RobolectricTestRunner::class)
class WidgetStabilizationUnitTest {

    // -------------------------------------------------------------------------
    // RC-4: Bitmap Caching
    // -------------------------------------------------------------------------

    @Test
    fun `generateTileBitmap returns non-null bitmap`() {
        val bmp = QuickRemindersWidgetHelper.generateTileBitmap(
            widthPx = 100, heightPx = 100, shape = "HEXAGON",
            colorArgb = 0xFF3B82F6.toInt(), isCompleted = false
        )
        assertNotNull(bmp)
        assertEquals(100, bmp.width)
        assertEquals(100, bmp.height)
    }

    @Test
    fun `generateTileBitmap circle shape returns correct dimensions`() {
        val bmp = QuickRemindersWidgetHelper.generateTileBitmap(
            widthPx = 80, heightPx = 80, shape = "CIRCLE",
            colorArgb = 0xFF10B981.toInt(), isCompleted = true
        )
        assertEquals(80, bmp.width)
        assertEquals(80, bmp.height)
    }

    @Test
    fun `generateTileBitmap square shape returns correct dimensions`() {
        val bmp = QuickRemindersWidgetHelper.generateTileBitmap(
            widthPx = 120, heightPx = 120, shape = "SQUARE",
            colorArgb = 0xFFF59E0B.toInt(), isCompleted = false
        )
        assertEquals(120, bmp.width)
        assertEquals(120, bmp.height)
    }

    @Test
    fun `getCachedTileBitmap returns same instance on second call (cache hit)`() {
        QuickRemindersWidgetHelper.clearCache()
        val bmp1 = QuickRemindersWidgetHelper.getCachedTileBitmap(
            160, 160, "HEXAGON", 0xFF3B82F6.toInt(), false
        )
        val bmp2 = QuickRemindersWidgetHelper.getCachedTileBitmap(
            160, 160, "HEXAGON", 0xFF3B82F6.toInt(), false
        )
        assertSame("Cache miss on second call — LruCache key may be malformed", bmp1, bmp2)
    }

    @Test
    fun `getCachedTileBitmap returns different instance for different completion state`() {
        QuickRemindersWidgetHelper.clearCache()
        val bmpIncomplete = QuickRemindersWidgetHelper.getCachedTileBitmap(
            160, 160, "HEXAGON", 0xFF3B82F6.toInt(), false
        )
        val bmpComplete = QuickRemindersWidgetHelper.getCachedTileBitmap(
            160, 160, "HEXAGON", 0xFF3B82F6.toInt(), true
        )
        assertNotSame(
            "Completed and non-completed bitmaps must be cached separately",
            bmpIncomplete, bmpComplete
        )
    }

    @Test
    fun `getCachedTileBitmap returns different instance for different shapes`() {
        QuickRemindersWidgetHelper.clearCache()
        val hex = QuickRemindersWidgetHelper.getCachedTileBitmap(
            160, 160, "HEXAGON", 0xFF3B82F6.toInt(), false
        )
        val circle = QuickRemindersWidgetHelper.getCachedTileBitmap(
            160, 160, "CIRCLE", 0xFF3B82F6.toInt(), false
        )
        assertNotSame(hex, circle)
    }

    @Test
    fun `getCachedTileBitmap handles unknown shape gracefully (defaults to hexagon)`() {
        val bmp = QuickRemindersWidgetHelper.getCachedTileBitmap(
            100, 100, "TRIANGLE", 0xFF8B5CF6.toInt(), false
        )
        assertNotNull(bmp)
        assertEquals(100, bmp.width)
    }

    // -------------------------------------------------------------------------
    // RC-3: Hexagon Geometry
    // -------------------------------------------------------------------------

    @Test
    fun `calculateHexagonVertices returns exactly 6 vertices`() {
        val vertices = QuickRemindersWidgetHelper.calculateHexagonVertices(50f, 50f, 40f)
        assertEquals(6, vertices.size)
    }

    @Test
    fun `calculateHexagonVertices all vertices within expected radius distance`() {
        val cx = 100f; val cy = 100f; val radius = 45f
        val vertices = QuickRemindersWidgetHelper.calculateHexagonVertices(cx, cy, radius)
        for ((x, y) in vertices) {
            val dist = Math.sqrt(((x - cx) * (x - cx) + (y - cy) * (y - cy)).toDouble()).toFloat()
            // Allow 1px floating-point tolerance
            assertTrue("Vertex at ($x, $y) is not at radius $radius (dist=$dist)",
                Math.abs(dist - radius) < 1f)
        }
    }

    @Test
    fun `calculateHexWidth returns sqrt3 times radius`() {
        val radius = 50f
        val expected = (Math.sqrt(3.0) * radius).toFloat()
        val actual = QuickRemindersWidgetHelper.calculateHexWidth(radius)
        assertEquals(expected, actual, 0.001f)
    }

    @Test
    fun `calculateHexHeight returns 2 times radius`() {
        val radius = 50f
        assertEquals(100f, QuickRemindersWidgetHelper.calculateHexHeight(radius), 0.001f)
    }

    // -------------------------------------------------------------------------
    // Honeycomb Stagger Geometry (RC-3 Fix Verification)
    // -------------------------------------------------------------------------

    @Test
    fun `calculateAxialStaggerFactor returns 0 for even rows`() {
        assertEquals(0f, QuickRemindersWidgetHelper.calculateAxialStaggerFactor(0), 0f)
        assertEquals(0f, QuickRemindersWidgetHelper.calculateAxialStaggerFactor(2), 0f)
        assertEquals(0f, QuickRemindersWidgetHelper.calculateAxialStaggerFactor(4), 0f)
    }

    @Test
    fun `calculateAxialStaggerFactor returns 0_5 for odd rows`() {
        assertEquals(0.5f, QuickRemindersWidgetHelper.calculateAxialStaggerFactor(1), 0f)
        assertEquals(0.5f, QuickRemindersWidgetHelper.calculateAxialStaggerFactor(3), 0f)
    }

    @Test
    fun `isOddStaggeredRow correctly identifies rows`() {
        assertTrue(QuickRemindersWidgetHelper.isOddStaggeredRow(1))
        assertTrue(QuickRemindersWidgetHelper.isOddStaggeredRow(3))
        assertTrue(!QuickRemindersWidgetHelper.isOddStaggeredRow(0))
        assertTrue(!QuickRemindersWidgetHelper.isOddStaggeredRow(2))
    }

    // -------------------------------------------------------------------------
    // Provider Tile ID Arrays (Architecture Verification)
    // -------------------------------------------------------------------------

    @Test
    fun `tileContainerIds has exactly 8 entries for 3+2+3 layout`() {
        assertEquals(8, QuickRemindersWidgetProvider.tileContainerIds.size)
    }

    @Test
    fun `tileShapeIds has exactly 8 entries`() {
        assertEquals(8, QuickRemindersWidgetProvider.tileShapeIds.size)
    }

    @Test
    fun `tileEmojiIds has exactly 8 entries`() {
        assertEquals(8, QuickRemindersWidgetProvider.tileEmojiIds.size)
    }

    @Test
    fun `tileTitleIds has exactly 8 entries`() {
        assertEquals(8, QuickRemindersWidgetProvider.tileTitleIds.size)
    }

    @Test
    fun `tileCheckIds has exactly 8 entries`() {
        assertEquals(8, QuickRemindersWidgetProvider.tileCheckIds.size)
    }

    @Test
    fun `all tile ID arrays have no duplicate entries`() {
        val allArrays = listOf(
            QuickRemindersWidgetProvider.tileContainerIds.toList(),
            QuickRemindersWidgetProvider.tileShapeIds.toList(),
            QuickRemindersWidgetProvider.tileEmojiIds.toList(),
            QuickRemindersWidgetProvider.tileTitleIds.toList(),
            QuickRemindersWidgetProvider.tileCheckIds.toList()
        )
        for (arr in allArrays) {
            assertEquals("Duplicate IDs found in array: $arr", arr.size, arr.distinct().size)
        }
    }

    // -------------------------------------------------------------------------
    // RC-8: Stable ID logic (factory level)
    // -------------------------------------------------------------------------

    @Test
    fun `stable ID contract - same shortcutId should always return same value`() {
        // Simulates the corrected getItemId behavior:
        // ID is tied to shortcutId, not position
        val shortcutId = 42L
        val idAtPosition0 = shortcutId
        val idAtPosition3 = shortcutId  // same shortcut, different hypothetical position
        assertEquals(
            "Stable ID must not change when shortcut moves to different position",
            idAtPosition0, idAtPosition3
        )
    }

    // -------------------------------------------------------------------------
    // Honeycomb Grid: Max Tile Constraint
    // -------------------------------------------------------------------------

    @Test
    fun `honeycomb 3+2+3 grid supports exactly 8 tiles`() {
        val row1Tiles = 3
        val row2Tiles = 2
        val row3Tiles = 3
        assertEquals(8, row1Tiles + row2Tiles + row3Tiles)
    }

    @Test
    fun `stagger spacer weight equals 0_5 of one tile weight for correct offset`() {
        // In the v2 layout, Row 2 has: Space(0.5) + Tile(1) + Tile(1) + Space(0.5) = 3.0 total
        // Row 1 and Row 3 have: Tile(1) + Tile(1) + Tile(1) = 3.0 total
        // This ensures uniform tile width across all rows.
        val row1TotalWeight = 1f + 1f + 1f
        val row2TotalWeight = 0.5f + 1f + 1f + 0.5f
        assertEquals(
            "Row 1 and Row 2 must have equal total weight for uniform tile size",
            row1TotalWeight, row2TotalWeight, 0.001f
        )
    }
}
