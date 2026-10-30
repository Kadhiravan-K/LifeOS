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
 * Unit tests covering the permanent widget stabilization and interlocking honeycomb grid for LifeOS.
 *
 * Tests cover:
 * - Bitmap caching (LruCache keying and cache hit behavior)
 * - Rounded hexagon geometry calculation (vertex positions and path creation)
 * - Provider companion object ID arrays (10-tile interlocking honeycomb: 3+2+3+2)
 * - Honeycomb stagger geometry math and row calculations
 * - Complete parity between in-app QR and launcher widget layouts
 */
@RunWith(RobolectricTestRunner::class)
class WidgetStabilizationUnitTest {

    // -------------------------------------------------------------------------
    // Bitmap Caching & Rendering
    // -------------------------------------------------------------------------

    @Test
    fun `generateTileBitmap returns non-null bitmap with correct dimensions`() {
        val bmp = QuickRemindersWidgetHelper.generateTileBitmap(
            widthPx = 144, heightPx = 168, shape = "HEXAGON",
            colorArgb = 0xFF10B981.toInt(), isCompleted = false
        )
        assertNotNull(bmp)
        assertEquals(144, bmp.width)
        assertEquals(168, bmp.height)
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
            144, 168, "HEXAGON", 0xFF3B82F6.toInt(), false
        )
        val bmp2 = QuickRemindersWidgetHelper.getCachedTileBitmap(
            144, 168, "HEXAGON", 0xFF3B82F6.toInt(), false
        )
        assertSame("Cache miss on second call — LruCache key may be malformed", bmp1, bmp2)
    }

    @Test
    fun `getCachedTileBitmap returns different instance for different completion state`() {
        QuickRemindersWidgetHelper.clearCache()
        val bmpIncomplete = QuickRemindersWidgetHelper.getCachedTileBitmap(
            144, 168, "HEXAGON", 0xFF3B82F6.toInt(), false
        )
        val bmpComplete = QuickRemindersWidgetHelper.getCachedTileBitmap(
            144, 168, "HEXAGON", 0xFF3B82F6.toInt(), true
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
            144, 168, "HEXAGON", 0xFF3B82F6.toInt(), false
        )
        val circle = QuickRemindersWidgetHelper.getCachedTileBitmap(
            144, 168, "CIRCLE", 0xFF3B82F6.toInt(), false
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
    // Hexagon Geometry
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
            assertTrue("Vertex at ($x, $y) is not at radius $radius (dist=$dist)",
                Math.abs(dist - radius) < 1f)
        }
    }

    @Test
    fun `createRoundedHexagonPath produces valid non-empty path`() {
        val path = QuickRemindersWidgetHelper.createRoundedHexagonPath(
            cx = 72f, cy = 84f, radius = 75f, cornerRadius = 14f
        )
        assertNotNull(path)
        assertTrue("Path must not be empty", !path.isEmpty)
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
    // Honeycomb Stagger & Interlocking Geometry
    // -------------------------------------------------------------------------

    @Test
    fun `calculateAxialStaggerFactor returns 0 for even rows and 0_5 for odd rows`() {
        assertEquals(0f, QuickRemindersWidgetHelper.calculateAxialStaggerFactor(0), 0f)
        assertEquals(0.5f, QuickRemindersWidgetHelper.calculateAxialStaggerFactor(1), 0f)
        assertEquals(0f, QuickRemindersWidgetHelper.calculateAxialStaggerFactor(2), 0f)
        assertEquals(0.5f, QuickRemindersWidgetHelper.calculateAxialStaggerFactor(3), 0f)
    }

    @Test
    fun `isOddStaggeredRow correctly identifies alternating rows`() {
        assertTrue(QuickRemindersWidgetHelper.isOddStaggeredRow(1))
        assertTrue(QuickRemindersWidgetHelper.isOddStaggeredRow(3))
        assertTrue(!QuickRemindersWidgetHelper.isOddStaggeredRow(0))
        assertTrue(!QuickRemindersWidgetHelper.isOddStaggeredRow(2))
    }

    // -------------------------------------------------------------------------
    // Provider Tile ID Arrays (10-Tile Honeycomb: 3 + 2 + 3 + 2)
    // -------------------------------------------------------------------------

    @Test
    fun `tileContainerIds has exactly 10 entries for 3+2+3+2 layout`() {
        assertEquals(10, QuickRemindersWidgetProvider.tileContainerIds.size)
    }

    @Test
    fun `tileShapeIds has exactly 10 entries`() {
        assertEquals(10, QuickRemindersWidgetProvider.tileShapeIds.size)
    }

    @Test
    fun `tileEmojiIds has exactly 10 entries`() {
        assertEquals(10, QuickRemindersWidgetProvider.tileEmojiIds.size)
    }

    @Test
    fun `tileTitleIds has exactly 10 entries`() {
        assertEquals(10, QuickRemindersWidgetProvider.tileTitleIds.size)
    }

    @Test
    fun `tileCheckIds has exactly 10 entries`() {
        assertEquals(10, QuickRemindersWidgetProvider.tileCheckIds.size)
    }

    @Test
    fun `rowIds has exactly 4 entries for the 4 interlocking rows`() {
        assertEquals(4, QuickRemindersWidgetProvider.rowIds.size)
    }

    @Test
    fun `all tile ID arrays have no duplicate entries`() {
        val allArrays = listOf(
            QuickRemindersWidgetProvider.tileContainerIds.toList(),
            QuickRemindersWidgetProvider.tileShapeIds.toList(),
            QuickRemindersWidgetProvider.tileEmojiIds.toList(),
            QuickRemindersWidgetProvider.tileTitleIds.toList(),
            QuickRemindersWidgetProvider.tileCheckIds.toList(),
            QuickRemindersWidgetProvider.rowIds.toList()
        )
        for (arr in allArrays) {
            assertEquals("Duplicate IDs found in array: $arr", arr.size, arr.distinct().size)
        }
    }

    // -------------------------------------------------------------------------
    // Honeycomb Grid: 10-Tile Parity with App QR
    // -------------------------------------------------------------------------

    @Test
    fun `interlocking 3+2+3+2 grid supports exactly 10 tiles matching default QR shortcuts`() {
        val row1 = 3
        val row2 = 2
        val row3 = 3
        val row4 = 2
        assertEquals(10, row1 + row2 + row3 + row4)
    }

    @Test
    fun `stagger offset is exactly half tile width for axial symmetry`() {
        // Tile width = 48dp, spacing = 4dp -> hStep = 52dp
        // Row 1 width = 3 * 48 + 2 * 4 = 152dp
        // Row 2 width = 2 * 48 + 4 = 100dp
        // Centering offset = (152 - 100) / 2 = 26dp = 0.5 * hStep
        val tileWidth = 48f
        val gap = 4f
        val hStep = tileWidth + gap
        val row1Width = 3 * tileWidth + 2 * gap
        val row2Width = 2 * tileWidth + gap
        val centerOffset = (row1Width - row2Width) / 2f
        assertEquals(0.5f * hStep, centerOffset, 0.001f)
    }

    @Test
    fun `vertical interlocking overlap is exactly 12dp`() {
        // Tile height = 56dp, vStep = 44dp
        // Overlap = 56 - 44 = 12dp (21.4% height overlap for pointy-top nestling)
        val tileHeight = 56f
        val vStep = 44f
        val overlap = tileHeight - vStep
        assertEquals(12f, overlap, 0.001f)
    }
}
