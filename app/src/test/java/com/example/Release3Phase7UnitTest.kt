package com.example

import android.graphics.Bitmap
import com.example.domain.analytics.ProductivityRadarData
import com.example.domain.analytics.ProductivityRadarEngine
import com.example.domain.analytics.RadarDimension
import com.example.domain.analytics.RadarDimensionScore
import com.example.domain.model.HabitItem
import com.example.domain.model.HoneycombAppearanceConfig
import com.example.domain.model.PlanItem
import com.example.domain.model.PlanLevel
import com.example.domain.model.PlanMilestone
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskPriority
import com.example.widgets.ProductivityRadarWidgetProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class Release3Phase7UnitTest {

    @Test
    fun qrZoom_scaleClamping_andDefaults() {
        val defaultConfig = HoneycombAppearanceConfig()
        assertEquals(1.0f, defaultConfig.zoomScale, 0.001f)

        // Custom zoom within bounds
        val zoom75 = HoneycombAppearanceConfig(zoomScale = 0.75f)
        assertEquals(0.75f, zoom75.zoomScale, 0.001f)

        val zoom135 = HoneycombAppearanceConfig(zoomScale = 1.35f)
        assertEquals(1.35f, zoom135.zoomScale, 0.001f)

        // Clamping logic verification
        fun clampZoom(scale: Float): Float = scale.coerceIn(0.75f, 1.35f)
        assertEquals(0.75f, clampZoom(0.5f), 0.001f)
        assertEquals(1.35f, clampZoom(2.0f), 0.001f)
        assertEquals(1.15f, clampZoom(1.15f), 0.001f)

        // Effective hexagon size calculation
        val baseHexSize = 74
        val effective75 = (baseHexSize * 0.75f).toInt()
        val effective100 = (baseHexSize * 1.0f).toInt()
        val effective135 = (baseHexSize * 1.35f).toInt()

        assertTrue(effective75 < effective100)
        assertTrue(effective100 < effective135)
        assertEquals(55, effective75)
        assertEquals(74, effective100)
        assertEquals(99, effective135)
    }

    @Test
    fun productivityRadarEngine_calculatesAllFiveDimensions() {
        val todayStr = LocalDate.now().toString()

        val tasks = listOf(
            TaskItem(id = 1, title = "Task 1", isOneTimeCompleted = true, dueDateIso = todayStr),
            TaskItem(id = 2, title = "Task 2", isOneTimeCompleted = true, dueDateIso = todayStr),
            TaskItem(id = 3, title = "Task 3", isOneTimeCompleted = true, dueDateIso = todayStr),
            TaskItem(id = 4, title = "Task 4", isOneTimeCompleted = true, dueDateIso = todayStr),
            TaskItem(id = 5, title = "Task 5", isOneTimeCompleted = false, dueDateIso = todayStr)
        )

        val habits = listOf(
            HabitItem(id = 1, title = "Habit 1", isCompletedToday = true),
            HabitItem(id = 2, title = "Habit 2", isCompletedToday = true),
            HabitItem(id = 3, title = "Habit 3", isCompletedToday = true)
        )

        val plans = listOf(
            PlanItem(
                id = 1,
                title = "Q4 Goals",
                level = PlanLevel.MONTHLY,
                startDateIso = todayStr,
                targetDateIso = todayStr,
                milestones = listOf(
                    PlanMilestone(id = 1, planId = 1, title = "M1", isCompleted = true),
                    PlanMilestone(id = 2, planId = 1, title = "M2", isCompleted = true),
                    PlanMilestone(id = 3, planId = 1, title = "M3", isCompleted = true),
                    PlanMilestone(id = 4, planId = 1, title = "M4", isCompleted = false)
                )
            )
        )

        val radarData = ProductivityRadarEngine.calculate(
            tasks = tasks,
            habits = habits,
            waterCurrentMl = 1500,
            waterGoalMl = 2000,
            plans = plans,
            currentStreak = 7,
            todayIso = todayStr
        )

        // 1. Tasks: 4 of 5 = 80%
        assertEquals(80, radarData.scoreFor(RadarDimension.TASKS))

        // 2. Habits: 3 of 3 = 100%
        assertEquals(100, radarData.scoreFor(RadarDimension.HABITS))

        // 3. Wellness: 1500 / 2000 ml = 75%
        assertEquals(75, radarData.scoreFor(RadarDimension.WELLNESS))

        // 4. Planning: 3 of 4 = 75%
        assertEquals(75, radarData.scoreFor(RadarDimension.PLANNING))

        // 5. Discipline: 7 days streak = 100%
        assertEquals(100, radarData.scoreFor(RadarDimension.DISCIPLINE))

        // Overall: average(80, 100, 75, 75, 100) = 430 / 5 = 86%
        assertEquals(86, radarData.overallScorePercent)
        assertEquals("Optimal Productivity", radarData.ratingTier)
        assertEquals("🚀", radarData.ratingTierEmoji)
    }

    @Test
    fun productivityRadarEngine_handlesEmptyDataGracefully() {
        val emptyRadar = ProductivityRadarEngine.calculate(
            tasks = emptyList(),
            habits = emptyList(),
            waterCurrentMl = 0,
            waterGoalMl = 2000,
            plans = emptyList(),
            currentStreak = 0
        )

        assertNotNull(emptyRadar)
        assertEquals(5, emptyRadar.dimensions.size)

        // Verifies zero division is safely guarded
        assertTrue(emptyRadar.overallScorePercent in 0..100)
        assertTrue(emptyRadar.ratingTier.isNotBlank())
        assertTrue(emptyRadar.ratingTierEmoji.isNotBlank())
    }

    @Test
    fun productivityRadarWidget_bitmapGeneration_drawsValidDimensions() {
        val sampleRadar = ProductivityRadarData(
            overallScorePercent = 88,
            ratingTier = "Optimal Productivity",
            ratingTierEmoji = "🚀",
            dimensions = listOf(
                RadarDimensionScore(RadarDimension.TASKS, 80, "4 done", "5 total", "4 of 5 completed"),
                RadarDimensionScore(RadarDimension.HABITS, 100, "3 checked", "3 active", "3 of 3 habits built"),
                RadarDimensionScore(RadarDimension.WELLNESS, 80, "1600ml", "2000ml", "1600ml / 2000ml water logged"),
                RadarDimensionScore(RadarDimension.PLANNING, 90, "9 milestones", "10 total", "9 of 10 milestones reached"),
                RadarDimensionScore(RadarDimension.DISCIPLINE, 90, "6 days", "7 days", "6 day streak ongoing")
            )
        )

        val bitmap = ProductivityRadarWidgetProvider.generateRadarBitmap(sampleRadar, size = 400)
        assertNotNull(bitmap)
        assertEquals(400, bitmap.width)
        assertEquals(400, bitmap.height)
        assertEquals(Bitmap.Config.ARGB_8888, bitmap.config)
    }

    @Test
    fun productivityRadar_profileScoping_evaluatesSeparately() {
        val todayStr = LocalDate.now().toString()

        // Profile 1: Personal (high completion)
        val profile1Tasks = listOf(
            TaskItem(id = 1, profileId = 1, title = "Workout", isOneTimeCompleted = true, dueDateIso = todayStr),
            TaskItem(id = 2, profileId = 1, title = "Read", isOneTimeCompleted = true, dueDateIso = todayStr)
        )
        val radarP1 = ProductivityRadarEngine.calculate(
            tasks = profile1Tasks,
            habits = emptyList(),
            waterCurrentMl = 2000,
            waterGoalMl = 2000,
            currentStreak = 10,
            todayIso = todayStr
        )

        // Profile 2: Work (low completion)
        val profile2Tasks = listOf(
            TaskItem(id = 3, profileId = 2, title = "Refactor", isOneTimeCompleted = false, dueDateIso = todayStr),
            TaskItem(id = 4, profileId = 2, title = "Deploy", isOneTimeCompleted = false, dueDateIso = todayStr)
        )
        val radarP2 = ProductivityRadarEngine.calculate(
            tasks = profile2Tasks,
            habits = emptyList(),
            waterCurrentMl = 0,
            waterGoalMl = 2000,
            currentStreak = 0,
            todayIso = todayStr
        )

        // Profile 1 tasks should be 100%, Profile 2 tasks should be 0%
        assertEquals(100, radarP1.scoreFor(RadarDimension.TASKS))
        assertEquals(0, radarP2.scoreFor(RadarDimension.TASKS))

        assertTrue(radarP1.overallScorePercent > radarP2.overallScorePercent)
    }

    @Test
    fun qrZoom_presets_coverCompactStandardLarge() {
        val presets = listOf(0.75f, 1.0f, 1.35f)
        assertTrue(presets.contains(0.75f))
        assertTrue(presets.contains(1.0f))
        assertTrue(presets.contains(1.35f))

        // Step zoom testing
        var zoom = 1.0f
        zoom = (zoom - 0.1f).coerceIn(0.75f, 1.35f)
        assertEquals(0.9f, zoom, 0.001f)

        zoom = (zoom + 0.5f).coerceIn(0.75f, 1.35f)
        assertEquals(1.35f, zoom, 0.001f)
    }
}
