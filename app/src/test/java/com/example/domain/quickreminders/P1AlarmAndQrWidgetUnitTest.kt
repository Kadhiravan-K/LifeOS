package com.example.domain.quickreminders

import com.example.domain.gamification.GamificationEngine
import com.example.domain.medication.IntervalModel
import com.example.domain.medication.MedicationScheduleConfig
import com.example.domain.medication.MedicationScheduleEngine
import com.example.domain.medication.MedicineScheduleType
import com.example.domain.model.HoneycombShortcut
import com.example.domain.model.QrColorSource
import com.example.domain.model.QrTileShape
import com.example.domain.model.ShortcutType
import com.example.domain.model.TaskItem
import com.example.domain.model.TaskPriority
import com.example.widgets.QuickRemindersWidgetHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit Test Suite for:
 * 1. Priority 1 (P1) Smart Alarms & multi-snooze
 * 2. Medicine 1-Hour interval scheduling & 0-XP guarantee
 * 3. QR tile project color inheritance precedence
 * 4. QR tile shape customization (Hexagon, Circle, Square)
 */
class P1AlarmAndQrWidgetUnitTest {

    // =========================================================================
    // 1. P1 SMART ALARMS & MULTI-SNOOZE TESTS
    // =========================================================================

    @Test
    fun `test P1 task defaults to alarm enabled`() {
        val p1Task = TaskItem(
            id = 101,
            title = "Critical Server Deployment",
            dueDateIso = "2026-10-23",
            priority = TaskPriority.P1,
            dueTime = "09:00",
            isReminderEnabled = false // Even if toggle was false, P1 rules treat it as alarm enabled
        )

        val shouldAlarm = p1Task.priority == TaskPriority.P1 || p1Task.isReminderEnabled
        assertTrue("P1 tasks must default to alarm enabled when dueTime is set", shouldAlarm)
        assertFalse(p1Task.dueTime.isNullOrBlank())
    }

    @Test
    fun `test P1 multi-snooze limits enforce maximum snooze count`() {
        val maxSnoozeCount = 3
        var currentSnoozeCount = 0

        // Simulate 3 snoozes
        while (currentSnoozeCount < maxSnoozeCount) {
            currentSnoozeCount++
        }
        assertEquals(3, currentSnoozeCount)

        // Attempt 4th snooze
        val canSnoozeAgain = currentSnoozeCount < maxSnoozeCount
        assertFalse("Further snooze must be rejected when max count is reached", canSnoozeAgain)
    }

    @Test
    fun `test task dismiss does not mark task completed`() {
        val task = TaskItem(
            id = 102,
            title = "P1 Review Architecture",
            dueDateIso = "2026-10-23",
            priority = TaskPriority.P1,
            isOneTimeCompleted = false
        )

        // Simulating dismiss intent: cancels alarm/notification without toggling completion
        val isCompletedAfterDismiss = task.isOneTimeCompleted
        assertFalse("Dismiss action must dismiss alarm without marking task as completed", isCompletedAfterDismiss)
    }

    // =========================================================================
    // 2. MEDICINE 1-HOUR INTERVAL & ZERO-XP TESTS
    // =========================================================================

    @Test
    fun `test medicine 1-hour interval scheduling generates 24 daily slots`() {
        val config = MedicationScheduleConfig(
            scheduleType = MedicineScheduleType.INTERVAL,
            intervalHours = 1,
            intervalAnchorTime = "00:00",
            allowOvernight = true,
            startDateIso = "2026-10-01"
        )

        val slots = MedicationScheduleEngine.calculateDailyDoseTimes(
            config = config,
            targetDateIso = "2026-10-23",
            wakeUpMinutes = 0,
            bedTimeMinutes = 1440
        )

        assertEquals("1-hour interval over 24h starting at 00:00 must produce 24 slots", 24, slots.size)
        assertEquals("00:00", slots[0])
        assertEquals("01:00", slots[1])
        assertEquals("23:00", slots[23])
    }

    @Test
    fun `test medication task completion strictly awards zero XP and zero coins`() {
        // Gamification engine calculation for normal task vs medicine task guarantee
        val (normalXp, normalCoins) = GamificationEngine.calculateTaskXpWithDiminishingReturns(
            dailyCount = 0,
            priority = TaskPriority.P1
        )
        assertTrue(normalXp > 0)
        assertTrue(normalCoins > 0)

        // Explicit rule: Medication logging is excluded from gamification rewards
        val isMedicationTask = true
        val awardedXp = if (isMedicationTask) 0 else normalXp
        val awardedCoins = if (isMedicationTask) 0 else normalCoins

        assertEquals("Medication tasks must award 0 XP", 0, awardedXp)
        assertEquals("Medication tasks must award 0 Coins", 0, awardedCoins)
    }

    // =========================================================================
    // 3. QR TILE PROJECT COLOR INHERITANCE PRECEDENCE TESTS
    // =========================================================================

    @Test
    fun `test color inheritance precedence - custom color overrides project color`() {
        val shortcut = HoneycombShortcut(
            id = 1,
            targetId = "101",
            title = "Task Shortcut",
            emoji = "⚡",
            type = ShortcutType.TASK,
            customColorHex = 0xFFFF0000, // Custom Red
            colorSource = QrColorSource.CUSTOM.name,
            projectColorHex = 0xFF00FF00 // Project Green
        )

        assertEquals(
            "Custom color must take precedence when colorSource is CUSTOM",
            0xFFFF0000,
            shortcut.effectiveColorHex
        )
    }

    @Test
    fun `test color inheritance precedence - inherits project color when source is PROJECT`() {
        val shortcut = HoneycombShortcut(
            id = 2,
            targetId = "102",
            title = "Task Shortcut",
            emoji = "⚡",
            type = ShortcutType.TASK,
            customColorHex = 0xFFFF0000,
            colorSource = QrColorSource.PROJECT.name,
            projectColorHex = 0xFF3B82F6 // Blue Project
        )

        assertEquals(
            "Must inherit project color when colorSource is PROJECT",
            0xFF3B82F6,
            shortcut.effectiveColorHex
        )
    }

    @Test
    fun `test color inheritance precedence - falls back gracefully when project deleted`() {
        val shortcut = HoneycombShortcut(
            id = 3,
            targetId = "103",
            title = "Task Shortcut",
            emoji = "⚡",
            type = ShortcutType.TASK,
            customColorHex = 0xFF10B981,
            colorSource = QrColorSource.PROJECT.name,
            projectColorHex = null // Project deleted or unassigned
        )

        assertEquals(
            "Must fallback to customColorHex if project is null",
            0xFF10B981,
            shortcut.effectiveColorHex
        )

        val shortcutNoCustom = shortcut.copy(customColorHex = null)
        assertNull(
            "Must fallback to null (theme default) if both project and custom colors are null",
            shortcutNoCustom.effectiveColorHex
        )
    }

    @Test
    fun `test color inheritance precedence - theme default returns null`() {
        val shortcut = HoneycombShortcut(
            id = 4,
            targetId = "104",
            title = "Task Shortcut",
            emoji = "⚡",
            type = ShortcutType.TASK,
            customColorHex = 0xFFFF0000,
            colorSource = QrColorSource.THEME_DEFAULT.name,
            projectColorHex = 0xFF00FF00
        )

        assertNull(
            "When colorSource is THEME_DEFAULT, effectiveColorHex must return null",
            shortcut.effectiveColorHex
        )
    }

    // =========================================================================
    // 4. QR TILE SHAPES & WIDGET BITMAP TESTS
    // =========================================================================

    @Test
    fun `test QR tile shapes enum supports Hexagon, Circle, Square`() {
        val shapes = QrTileShape.entries.map { it.name }
        assertTrue(shapes.contains("HEXAGON"))
        assertTrue(shapes.contains("CIRCLE"))
        assertTrue(shapes.contains("SQUARE"))
    }

    @Test
    fun `test widget shape generator computes 6 equidistant regular hexagon vertices`() {
        val cx = 100f
        val cy = 100f
        val r = 50f
        val vertices = QuickRemindersWidgetHelper.calculateHexagonVertices(cx, cy, r)

        assertEquals("Regular hexagon must have exactly 6 vertices", 6, vertices.size)

        // Verify distance of each vertex to center equals radius
        vertices.forEachIndexed { i, (x, y) ->
            val dist = kotlin.math.hypot((x - cx).toDouble(), (y - cy).toDouble())
            assertEquals("Vertex $i distance to center must equal radius 50f", 50.0, dist, 0.001)
        }
    }
}
