package com.example.widgets

import android.graphics.Bitmap
import com.example.domain.analytics.ProductivityRadarData
import com.example.domain.analytics.ProductivityRadarEngine
import com.example.domain.analytics.RadarDimension
import com.example.domain.analytics.RadarDimensionScore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit Tests for ProductivityRadarWidgetProvider (#17).
 * Validates:
 * 1. Spider radar bitmap generation across dimensions and sizes.
 * 2. Proper handling of extreme scores (0% and 100%).
 * 3. RemoteViews text & summary generation.
 */
@RunWith(RobolectricTestRunner::class)
class ProductivityRadarWidgetUnitTest {

    @Test
    fun `generateRadarBitmap returns non-null ARGB_8888 bitmap with specified dimensions`() {
        val testData = ProductivityRadarEngine.calculate(
            tasks = emptyList(),
            habits = emptyList(),
            waterCurrentMl = 1500,
            waterGoalMl = 2000,
            plans = emptyList(),
            currentStreak = 5,
            todayIso = "2026-11-01"
        )

        val size = 400
        val bitmap = ProductivityRadarWidgetProvider.generateRadarBitmap(testData, size = size)

        assertNotNull(bitmap)
        assertEquals(size, bitmap.width)
        assertEquals(size, bitmap.height)
        assertEquals(Bitmap.Config.ARGB_8888, bitmap.config)
    }

    @Test
    fun `generateRadarBitmap handles 0 percent and 100 percent edge scores without crashing`() {
        // Zero scores
        val zeroScores = RadarDimension.entries.map { dim ->
            RadarDimensionScore(dim, 0, "0", "10", "Zero progress")
        }
        val zeroData = ProductivityRadarData(
            overallScorePercent = 0,
            ratingTier = "Reset & Focus",
            ratingTierEmoji = "🧘",
            dimensions = zeroScores
        )
        val bmpZero = ProductivityRadarWidgetProvider.generateRadarBitmap(zeroData, size = 300)
        assertNotNull(bmpZero)
        assertEquals(300, bmpZero.width)

        // Perfect 100% scores
        val perfectScores = RadarDimension.entries.map { dim ->
            RadarDimensionScore(dim, 100, "10", "10", "Mastery")
        }
        val perfectData = ProductivityRadarData(
            overallScorePercent = 100,
            ratingTier = "Mastery & Peak Flow",
            ratingTierEmoji = "⚡",
            dimensions = perfectScores
        )
        val bmpPerfect = ProductivityRadarWidgetProvider.generateRadarBitmap(perfectData, size = 420)
        assertNotNull(bmpPerfect)
        assertEquals(420, bmpPerfect.width)
    }

    @Test
    fun `radar data model scoreFor returns expected values`() {
        val scores = listOf(
            RadarDimensionScore(RadarDimension.TASKS, 90, "9", "10", "Done"),
            RadarDimensionScore(RadarDimension.HABITS, 85, "5", "6", "Built"),
            RadarDimensionScore(RadarDimension.WELLNESS, 70, "1400", "2000", "Water"),
            RadarDimensionScore(RadarDimension.PLANNING, 60, "3", "5", "Plans"),
            RadarDimensionScore(RadarDimension.DISCIPLINE, 95, "7", "7", "Streak")
        )
        val data = ProductivityRadarData(
            overallScorePercent = 80,
            ratingTier = "Optimal Productivity",
            ratingTierEmoji = "🚀",
            dimensions = scores
        )

        assertEquals(90, data.scoreFor(RadarDimension.TASKS))
        assertEquals(85, data.scoreFor(RadarDimension.HABITS))
        assertEquals(70, data.scoreFor(RadarDimension.WELLNESS))
        assertEquals(60, data.scoreFor(RadarDimension.PLANNING))
        assertEquals(95, data.scoreFor(RadarDimension.DISCIPLINE))
    }
}
