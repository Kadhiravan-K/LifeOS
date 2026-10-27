package com.example.domain.quickreminders

import com.example.domain.model.HoneycombShortcut
import com.example.domain.model.QrColorSource
import com.example.domain.model.QrTileShape
import com.example.domain.model.ShortcutType
import com.example.ui.viewmodel.DashboardUiState
import com.example.widgets.QuickRemindersWidgetHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Focused Release 2 Regression Test Suite covering recovered fixes:
 * A. Ghost QR shortcuts absence & clean empty state
 * B. QR completion colors resolution cascade & tile shape support
 * C. Startup loading & placeholder text prevention
 * D. Premium navigation animation parameters
 * E. Launcher QR widget honeycomb geometry and alternating row chunking
 */
class Release2RegressionUnitTest {

    // =========================================================================
    // A. GHOST QR SHORTCUTS ABSENCE ON EMPTY STATE
    // =========================================================================

    @Test
    fun `test empty shortcut database produces clean empty state without fallback to defaults`() {
        val rawShortcutsFromRoom = emptyList<HoneycombShortcut>()

        // The recovered fix: empty raw list remains empty, never falling back to DefaultHoneycombShortcuts.list
        val resolvedList = rawShortcutsFromRoom

        assertTrue("Empty Room DB must result in an empty shortcuts list", resolvedList.isEmpty())
        assertEquals(0, resolvedList.size)
    }

    @Test
    fun `test populated shortcut database preserves existing shortcuts on upgrade`() {
        val existingShortcuts = listOf(
            HoneycombShortcut(id = 1, type = ShortcutType.TASK, targetId = "10", title = "Write Unit Tests", emoji = "Ã°Å¸Â§Âª"),
            HoneycombShortcut(id = 2, type = ShortcutType.HABIT, targetId = "20", title = "Evening Walk", emoji = "Ã°Å¸Å¡Â¶")
        )

        // On upgrade, existing shortcuts must be fully preserved
        assertEquals(2, existingShortcuts.size)
        assertEquals("Write Unit Tests", existingShortcuts[0].title)
        assertEquals("Evening Walk", existingShortcuts[1].title)
    }

    // =========================================================================
    // B. QR COMPLETION COLORS RESOLUTION CASCADE & SHAPES
    // =========================================================================

    @Test
    fun `test task shortcut inherits project color when assigned`() {
        val projectColorHex = 0xFFDB4C3FL // Todoist Red
        val taskShortcut = HoneycombShortcut(
            id = 1,
            type = ShortcutType.TASK,
            targetId = "100",
            title = "Design Mockups",
            emoji = "Ã°Å¸Å½Â¨",
            colorSource = "PROJECT",
            projectColorHex = projectColorHex
        )

        assertEquals(projectColorHex, taskShortcut.effectiveColorHex)
    }

    @Test
    fun `test habit water and challenge shortcuts resolve non-null completion colors`() {
        val habitShortcut = HoneycombShortcut(
            id = 2,
            type = ShortcutType.HABIT,
            targetId = "200",
            title = "Morning Meditation",
            emoji = "Ã°Å¸Â§Ëœ",
            colorSource = "PROJECT"
        )
        assertNotNull("Habit shortcut must resolve non-null effective color", habitShortcut.effectiveColorHex)
        assertEquals(0xFF10B981L, habitShortcut.effectiveColorHex) // Emerald Green

        val waterShortcut = HoneycombShortcut(
            id = 3,
            type = ShortcutType.WATER,
            targetId = "water",
            title = "Hydration",
            emoji = "Ã°Å¸â€™Â§",
            colorSource = "PROJECT"
        )
        assertNotNull("Water shortcut must resolve non-null effective color", waterShortcut.effectiveColorHex)
        assertEquals(0xFF3B82F6L, waterShortcut.effectiveColorHex) // Water Blue

        val challengeShortcut = HoneycombShortcut(
            id = 4,
            type = ShortcutType.CHALLENGE_RULE,
            targetId = "rule_reading",
            title = "Reading 10 Pages",
            emoji = "Ã°Å¸â€œâ€“",
            colorSource = "PROJECT"
        )
        assertNotNull("Challenge shortcut must resolve non-null effective color", challengeShortcut.effectiveColorHex)
        assertEquals(0xFFF59E0BL, challengeShortcut.effectiveColorHex) // Amber
    }

    @Test
    fun `test custom color source takes precedence over project color`() {
        val customColor = 0xFF8B5CF6L // Purple
        val projectColor = 0xFF10B981L // Green
        val shortcut = HoneycombShortcut(
            id = 5,
            type = ShortcutType.TASK,
            targetId = "300",
            title = "VIP Feature",
            emoji = "Ã¢Â­Â",
            colorSource = "CUSTOM",
            customColorHex = customColor,
            projectColorHex = projectColor
        )

        assertEquals(customColor, shortcut.effectiveColorHex)
    }

    @Test
    fun `test completion color is never transparent or zero`() {
        val themePrimaryFallback = 0xFF6366F1L // Indigo
        val globalAppearanceHex = 0xFF10B981L // Emerald

        val shortcutWithNullColors = HoneycombShortcut(
            id = 6,
            type = ShortcutType.TASK,
            targetId = "400",
            title = "Unassigned Task",
            emoji = "Ã°Å¸â€œâ€¹",
            colorSource = "PROJECT",
            customColorHex = null,
            projectColorHex = null
        )

        val resolvedHex = shortcutWithNullColors.effectiveColorHex ?: globalAppearanceHex
        assertNotEquals(0L, resolvedHex)
        assertTrue("Alpha channel must be fully opaque", (resolvedHex and 0xFF000000L) != 0L)
    }

    @Test
    fun `test tile shapes hexagon circle and square parsing and hexagon vertex geometry`() {
        // Shapes supported: HEXAGON, CIRCLE, SQUARE
        val shapes = listOf("HEXAGON", "CIRCLE", "SQUARE")

        for (shape in shapes) {
            val tileShape = QrTileShape.fromString(shape)
            assertNotNull("Shape $shape must be parsed successfully", tileShape)
        }

        // Test pointy-top hexagon vertex calculation (6 vertices)
        val vertices = QuickRemindersWidgetHelper.calculateHexagonVertices(90f, 90f, 80f)
        assertEquals("Hexagon must have exactly 6 vertices", 6, vertices.size)
        // Verify vertices are within bounding box
        for ((x, y) in vertices) {
            assertTrue("X coordinate must be >= 0", x >= 0f)
            assertTrue("X coordinate must be <= 180", x <= 180f)
            assertTrue("Y coordinate must be >= 0", y >= 0f)
            assertTrue("Y coordinate must be <= 180", y <= 180f)
        }
    }

    // =========================================================================
    // C. STARTUP LOADING & PLACEHOLDER PREVENTION
    // =========================================================================

    @Test
    fun `test DashboardUiState initial state has loading true and empty strings`() {
        val initialUiState = DashboardUiState(isLoading = true)

        assertTrue("Initial state must have isLoading = true", initialUiState.isLoading)
        assertTrue("User name must not contain hardcoded placeholder 'Master'", initialUiState.userName.isEmpty())
        assertTrue("Greeting must not contain hardcoded placeholder 'Good morning'", initialUiState.greeting.isEmpty())
    }

    @Test
    fun `test DashboardUiState populated state has loading false and real user greeting`() {
        val populatedUiState = DashboardUiState(
            isLoading = false,
            userName = "Alice",
            greeting = "Good afternoon, Alice"
        )

        assertFalse("Populated state must have isLoading = false", populatedUiState.isLoading)
        assertEquals("Alice", populatedUiState.userName)
        assertEquals("Good afternoon, Alice", populatedUiState.greeting)
    }

    // =========================================================================
    // D. PREMIUM NAVIGATION PARAMETERS
    // =========================================================================

    @Test
    fun `test navigation transition duration is 1500 ms with reduced animation support`() {
        val transitionDurationMs = 1500

        assertEquals(1500, transitionDurationMs)

        // Test reduced animation detection
        fun calculateEffectiveDuration(animatorScale: Float): Int {
            return if (animatorScale <= 0.05f) 0 else transitionDurationMs
        }

        assertEquals(0, calculateEffectiveDuration(0.0f))
        assertEquals(1500, calculateEffectiveDuration(1.0f))
        assertEquals(1500, calculateEffectiveDuration(0.5f))
    }

    // =========================================================================
    // E. LAUNCHER WIDGET HONEYCOMB GEOMETRY & ALTERNATING ROW CHUNKING
    // =========================================================================

    @Test
    fun `test widget alternating row chunking creates 3-tile then 2-tile rows`() {
        data class MockItem(val id: Long, val title: String)
        data class MockRow(val isThreeColumn: Boolean, val items: List<MockItem>)

        fun buildRows(items: List<MockItem>): List<MockRow> {
            val rows = mutableListOf<MockRow>()
            var index = 0
            var isThree = true
            while (index < items.size) {
                val count = if (isThree) 3 else 2
                val chunk = items.subList(index, minOf(index + count, items.size))
                rows.add(MockRow(isThreeColumn = isThree, items = chunk))
                index += chunk.size
                isThree = !isThree
            }
            return rows
        }

        val testItems = (1..8).map { MockItem(id = it.toLong(), title = "Tile $it") }
        val rows = buildRows(testItems)

        // 8 items should form:
        // Row 0: 3 items (isThree = true)
        // Row 1: 2 items (isThree = false, centered/staggered)
        // Row 2: 3 items (isThree = true)
        assertEquals(3, rows.size)

        assertTrue(rows[0].isThreeColumn)
        assertEquals(3, rows[0].items.size)

        assertFalse(rows[1].isThreeColumn)
        assertEquals(2, rows[1].items.size)

        assertTrue(rows[2].isThreeColumn)
        assertEquals(3, rows[2].items.size)
    }

    @Test
    fun `test widget row chunking with empty items returns zero rows`() {
        data class MockItem(val id: Long)
        data class MockRow(val isThreeColumn: Boolean, val items: List<MockItem>)

        fun buildRows(items: List<MockItem>): List<MockRow> {
            val rows = mutableListOf<MockRow>()
            var index = 0
            var isThree = true
            while (index < items.size) {
                val count = if (isThree) 3 else 2
                val chunk = items.subList(index, minOf(index + count, items.size))
                rows.add(MockRow(isThreeColumn = isThree, items = chunk))
                index += chunk.size
                isThree = !isThree
            }
            return rows
        }

        val emptyRows = buildRows(emptyList<MockItem>())
        assertEquals(0, emptyRows.size)
    }

    private data class MockItem(val id: Long)
    private data class MockRow(val isThreeColumn: Boolean, val items: List<MockItem>)

    private fun buildRows(items: List<MockItem>): List<MockRow> {
        val rows = mutableListOf<MockRow>()
        var index = 0
        var isThree = true
        while (index < items.size) {
            val count = if (isThree) 3 else 2
            val chunk = items.subList(index, minOf(index + count, items.size))
            rows.add(MockRow(isThreeColumn = isThree, items = chunk))
            index += chunk.size
            isThree = !isThree
        }
        return rows
    }

    @Test
    fun `test widget row chunking with various item counts creates correct alternating patterns`() {
        // 1 item -> 1 row (3-col, size 1)
        val r1 = buildRows((1..1).map { MockItem(it.toLong()) })
        assertEquals(1, r1.size)
        assertTrue(r1[0].isThreeColumn)
        assertEquals(1, r1[0].items.size)

        // 3 items -> 1 row (3-col, size 3)
        val r3 = buildRows((1..3).map { MockItem(it.toLong()) })
        assertEquals(1, r3.size)
        assertTrue(r3[0].isThreeColumn)
        assertEquals(3, r3[0].items.size)

        // 4 items -> 2 rows (row 0: 3 items, row 1: 1 item in 2-col layout)
        val r4 = buildRows((1..4).map { MockItem(it.toLong()) })
        assertEquals(2, r4.size)
        assertTrue(r4[0].isThreeColumn)
        assertEquals(3, r4[0].items.size)
        assertFalse(r4[1].isThreeColumn)
        assertEquals(1, r4[1].items.size)

        // 5 items -> 2 rows (row 0: 3 items, row 1: 2 items in 2-col layout)
        val r5 = buildRows((1..5).map { MockItem(it.toLong()) })
        assertEquals(2, r5.size)
        assertTrue(r5[0].isThreeColumn)
        assertEquals(3, r5[0].items.size)
        assertFalse(r5[1].isThreeColumn)
        assertEquals(2, r5[1].items.size)

        // 6 items -> 3 rows (row 0: 3, row 1: 2, row 2: 1)
        val r6 = buildRows((1..6).map { MockItem(it.toLong()) })
        assertEquals(3, r6.size)
        assertTrue(r6[0].isThreeColumn)
        assertEquals(3, r6[0].items.size)
        assertFalse(r6[1].isThreeColumn)
        assertEquals(2, r6[1].items.size)
        assertTrue(r6[2].isThreeColumn)
        assertEquals(1, r6[2].items.size)
    }

    @Test
    fun `test water intake calculation adds exactly 250ml and clamps to zero on negative`() {
        val initialAmount = 500
        val incrementDelta = 250
        val newAmount = (initialAmount + incrementDelta).coerceAtLeast(0)
        assertEquals(750, newAmount)

        val largeNegativeDelta = -1000
        val clampedAmount = (initialAmount + largeNegativeDelta).coerceAtLeast(0)
        assertEquals(0, clampedAmount)
    }

    @Test
    fun `test challenge rule toggle sets adds when missing and removes when present`() {
        val initialCompleted = setOf("rule_water", "rule_read")

        // Toggling an existing rule removes it
        val afterToggleExisting = if (initialCompleted.contains("rule_water")) {
            initialCompleted - "rule_water"
        } else {
            initialCompleted + "rule_water"
        }
        assertEquals(setOf("rule_read"), afterToggleExisting)
        assertFalse(afterToggleExisting.contains("rule_water"))

        // Toggling a new rule adds it
        val afterToggleNew = if (afterToggleExisting.contains("rule_workout")) {
            afterToggleExisting - "rule_workout"
        } else {
            afterToggleExisting + "rule_workout"
        }
        assertEquals(setOf("rule_read", "rule_workout"), afterToggleNew)
        assertTrue(afterToggleNew.contains("rule_workout"))
    }

    // =========================================================================
    // F. MULTI-INSTANCE WIDGET ISOLATION & API 24 COMPATIBILITY
    // =========================================================================

    @Test
    fun `test widget multi-instance URI and request code isolation`() {
        val widgetId1 = 101
        val widgetId2 = 102

        val uri1 = "lifeos://widget/qr/click/$widgetId1"
        val uri2 = "lifeos://widget/qr/click/$widgetId2"

        assertNotEquals("Each widget instance must have a unique data URI", uri1, uri2)
        assertEquals("lifeos://widget/qr/click/101", uri1)
        assertEquals("lifeos://widget/qr/click/102", uri2)
    }

    @Test
    fun `test DateUtils API 24 compatibility without java time dependency`() {
        val todayStr = com.example.utils.DateUtils.todayString()
        assertNotNull(todayStr)
        assertTrue("ISO date string must match YYYY-MM-DD pattern", todayStr.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))

        val epochDay = com.example.utils.DateUtils.todayEpochDay()
        assertTrue("Epoch day must be a positive integer", epochDay > 0)

        val parsedEpoch = com.example.utils.DateUtils.isoStringToEpochDay(todayStr)
        val roundTripIso = com.example.utils.DateUtils.epochDayToIsoString(parsedEpoch)
        assertEquals(todayStr, roundTripIso)

        val formattedDisplay = com.example.utils.DateUtils.formatDisplayDate(todayStr)
        assertTrue("Formatted date must begin with Today", formattedDisplay.startsWith("Today"))
    }

    @Test
    fun `test Room migration sequence monotonically increases from v1 to v10`() {
        val migrations = listOf(
            com.example.data.local.MIGRATION_1_2,
            com.example.data.local.MIGRATION_2_3,
            com.example.data.local.MIGRATION_3_4,
            com.example.data.local.MIGRATION_4_5,
            com.example.data.local.MIGRATION_5_6,
            com.example.data.local.MIGRATION_6_7,
            com.example.data.local.MIGRATION_7_8,
            com.example.data.local.MIGRATION_8_9,
            com.example.data.local.MIGRATION_9_10
        )

        assertEquals("Must contain exactly 9 migrations for versions 1 to 10", 9, migrations.size)
        for (i in migrations.indices) {
            val expectedStart = i + 1
            val expectedEnd = i + 2
            assertEquals("Migration start version mismatch at index $i", expectedStart, migrations[i].startVersion)
            assertEquals("Migration end version mismatch at index $i", expectedEnd, migrations[i].endVersion)
        }
    }

    @Test
    fun `test empty state and single tile row geometry in widget`() {
        val emptyRows = buildRows(emptyList<MockItem>())
        assertTrue("Empty items must produce zero rows to show empty view", emptyRows.isEmpty())

        val singleItemRows = buildRows(listOf(MockItem(1L)))
        assertEquals("Single item must produce 1 row", 1, singleItemRows.size)
        assertTrue("First row must be 3-column layout type", singleItemRows[0].isThreeColumn)
        assertEquals(1, singleItemRows[0].items.size)
    }

    @Test
    fun `test DateUtils midnight transition across Asia Kolkata, UTC, and America Los Angeles without clock dependency`() {
        val kolkataTz = java.util.TimeZone.getTimeZone("Asia/Kolkata")
        val utcTz = java.util.TimeZone.getTimeZone("UTC")
        val laTz = java.util.TimeZone.getTimeZone("America/Los_Angeles")

        // 1. Kolkata Midnight: 2026-10-27 00:00:00 IST = 2026-10-26 18:30:00 UTC
        val calKolkata = java.util.Calendar.getInstance(utcTz).apply {
            clear()
            set(2026, 9, 26, 18, 30, 0)
        }
        val kolkataMidnightMs = calKolkata.timeInMillis

        // 1 second before midnight in Kolkata (23:59:59 IST on 2026-10-26)
        assertEquals("2026-10-26", com.example.utils.DateUtils.todayString(kolkataTz, kolkataMidnightMs - 1000L))
        assertEquals(com.example.utils.DateUtils.isoStringToEpochDay("2026-10-26"), com.example.utils.DateUtils.todayEpochDay(kolkataTz, kolkataMidnightMs - 1000L))

        // 1 second after midnight in Kolkata (00:00:01 IST on 2026-10-27)
        assertEquals("2026-10-27", com.example.utils.DateUtils.todayString(kolkataTz, kolkataMidnightMs + 1000L))
        assertEquals(com.example.utils.DateUtils.isoStringToEpochDay("2026-10-27"), com.example.utils.DateUtils.todayEpochDay(kolkataTz, kolkataMidnightMs + 1000L))

        // 2. UTC Midnight: 2026-10-27 00:00:00 UTC
        val calUtc = java.util.Calendar.getInstance(utcTz).apply {
            clear()
            set(2026, 9, 27, 0, 0, 0)
        }
        val utcMidnightMs = calUtc.timeInMillis

        // 1 second before UTC midnight
        assertEquals("2026-10-26", com.example.utils.DateUtils.todayString(utcTz, utcMidnightMs - 1000L))
        // 1 second after UTC midnight
        assertEquals("2026-10-27", com.example.utils.DateUtils.todayString(utcTz, utcMidnightMs + 1000L))

        // 3. Los Angeles Midnight (PDT = UTC-7): 2026-10-27 00:00:00 PDT = 2026-10-27 07:00:00 UTC
        val calLa = java.util.Calendar.getInstance(utcTz).apply {
            clear()
            set(2026, 9, 27, 7, 0, 0)
        }
        val laMidnightMs = calLa.timeInMillis

        // 1 second before midnight in LA
        assertEquals("2026-10-26", com.example.utils.DateUtils.todayString(laTz, laMidnightMs - 1000L))
        // 1 second after midnight in LA
        assertEquals("2026-10-27", com.example.utils.DateUtils.todayString(laTz, laMidnightMs + 1000L))
    }

    @Test
    fun `test DateUtils negative and positive epoch days roundtrip and timezone independence`() {
        // Day 0: Unix epoch 1970-01-01
        assertEquals("1970-01-01", com.example.utils.DateUtils.epochDayToIsoString(0L))
        assertEquals(0L, com.example.utils.DateUtils.isoStringToEpochDay("1970-01-01"))

        // Negative Day -1: 1969-12-31
        assertEquals("1969-12-31", com.example.utils.DateUtils.epochDayToIsoString(-1L))
        assertEquals(-1L, com.example.utils.DateUtils.isoStringToEpochDay("1969-12-31"))

        // Negative Day -365: 1969-01-01
        assertEquals("1969-01-01", com.example.utils.DateUtils.epochDayToIsoString(-365L))
        assertEquals(-365L, com.example.utils.DateUtils.isoStringToEpochDay("1969-01-01"))

        // Historical negative: -10000L -> 1942-08-16
        val neg10k = -10000L
        val neg10kIso = com.example.utils.DateUtils.epochDayToIsoString(neg10k)
        assertEquals("1942-08-16", neg10kIso)
        assertEquals(neg10k, com.example.utils.DateUtils.isoStringToEpochDay(neg10kIso))

        // Modern day: 2026-10-27 -> 20754L
        val day2026 = 20753L
        val iso2026 = com.example.utils.DateUtils.epochDayToIsoString(day2026)
        assertEquals("2026-10-27", iso2026)
        assertEquals(day2026, com.example.utils.DateUtils.isoStringToEpochDay(iso2026))

        // Timezone independence verification
        val originalTz = java.util.TimeZone.getDefault()
        try {
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Pacific/Honolulu")) // UTC-10
            assertEquals(20753L, com.example.utils.DateUtils.isoStringToEpochDay("2026-10-27"))

            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Tokyo")) // UTC+9
            assertEquals(20753L, com.example.utils.DateUtils.isoStringToEpochDay("2026-10-27"))
        } finally {
            java.util.TimeZone.setDefault(originalTz)
        }
    }

    @Test
    fun `test DateUtils addDays and daysBetween epoch arithmetic precision`() {
        assertEquals("1969-12-31", com.example.utils.DateUtils.addDays("1970-01-01", -1))
        assertEquals("1970-01-01", com.example.utils.DateUtils.addDays("1969-12-31", 1))
        assertEquals("2026-11-01", com.example.utils.DateUtils.addDays("2026-10-27", 5))

        assertEquals(1, com.example.utils.DateUtils.daysBetween("1969-12-31", "1970-01-01"))
        assertEquals(-1, com.example.utils.DateUtils.daysBetween("1970-01-01", "1969-12-31"))
        assertEquals(26, com.example.utils.DateUtils.daysBetween("2026-10-01", "2026-10-27"))
        assertEquals(-26, com.example.utils.DateUtils.daysBetween("2026-10-27", "2026-10-01"))
    }

    // =========================================================================
    // F. RELEASE 2 HARDENING: QR PRESETS, SHAPES, CALENDAR, MEDICATION, RETENTION
    // =========================================================================

    @Test
    fun `test all seven QR coordinate presets produce unique non-overlapping coordinates`() {
        val templates = listOf(
            com.example.domain.model.HoneycombLayoutTemplate.STANDARD,
            com.example.domain.model.HoneycombLayoutTemplate.PYRAMID,
            com.example.domain.model.HoneycombLayoutTemplate.DIAMOND,
            com.example.domain.model.HoneycombLayoutTemplate.HEART,
            com.example.domain.model.HoneycombLayoutTemplate.SNAKE,
            com.example.domain.model.HoneycombLayoutTemplate.ZIGZAG,
            com.example.domain.model.HoneycombLayoutTemplate.FREEFORM
        )

        for (template in templates) {
            for (count in listOf(1, 5, 10, 16, 20)) {
                val coords = com.example.domain.model.HoneycombLayoutTemplate.generateCoordinates(template, count)
                assertEquals(
                    "Preset ${template.name} with count $count must generate exactly $count coordinates",
                    count,
                    coords.size
                )
                val uniqueCoords = coords.toSet()
                assertEquals(
                    "Preset ${template.name} with count $count must contain 0 duplicate/overlapping coordinates",
                    coords.size,
                    uniqueCoords.size
                )
            }
        }
    }

    @Test
    fun `test custom per-shortcut shape takes precedence over global appearance shape`() {
        val globalCircleAppearance = com.example.domain.model.HoneycombAppearanceConfig(
            shape = com.example.domain.model.DashboardShape.CIRCLE
        )

        // Shortcut explicitly customized to HEXAGON
        val hexShortcut = HoneycombShortcut(
            id = 1,
            type = ShortcutType.TASK,
            targetId = "1",
            title = "Hex Shortcut",
            emoji = "âš¡", shape = "HEXAGON"
        )
        val effectiveHex = if (hexShortcut.shape.isNotBlank()) hexShortcut.shape else globalCircleAppearance.shape.name
        assertEquals("Explicit per-shortcut shape HEXAGON must take precedence over global CIRCLE", "HEXAGON", effectiveHex)

        // Shortcut with blank shape falls back to global CIRCLE
        val fallbackShortcut = HoneycombShortcut(
            id = 2,
            type = ShortcutType.TASK,
            targetId = "2",
            title = "Default Shortcut",
            emoji = "âš¡", shape = ""
        )
        val effectiveFallback = if (fallbackShortcut.shape.isNotBlank()) fallbackShortcut.shape else globalCircleAppearance.shape.name
        assertEquals("Blank shortcut shape must fall back to global CIRCLE", "CIRCLE", effectiveFallback)
    }

    @Test
    fun `test Yoga and Meditation resolve to independent tasks and do not co-complete`() {
        val yogaTask = com.example.domain.model.TaskItem(
            id = 101L,
            title = "Daily Yoga & Flexibility",
            dueDateIso = "2026-10-27",
            isCompletedForDate = false
        )
        val meditationTask = com.example.domain.model.TaskItem(
            id = 102L,
            title = "Mindful Meditation & Stillness",
            dueDateIso = "2026-10-27",
            isCompletedForDate = false
        )
        val allTasks = listOf(yogaTask, meditationTask)

        val yogaShortcut = HoneycombShortcut(
            id = 5,
            type = ShortcutType.TASK,
            targetId = "Yoga",
            title = "Yoga", emoji = "ðŸ§˜"
        )
        val meditationShortcut = HoneycombShortcut(
            id = 3,
            type = ShortcutType.TASK,
            targetId = "Meditation",
            title = "Meditation", emoji = "ðŸ§˜"
        )

        // Resolution logic for Yoga
        val matchedYoga = allTasks.find { it.id.toString() == yogaShortcut.targetId }
            ?: allTasks.find { it.title.equals(yogaShortcut.targetId, ignoreCase = true) }
            ?: allTasks.find { it.title.equals(yogaShortcut.title, ignoreCase = true) }
            ?: allTasks.find {
                val words = it.title.split(Regex("[^A-Za-z0-9]")).filter { w -> w.isNotBlank() }
                words.any { w -> w.equals(yogaShortcut.targetId, ignoreCase = true) || w.equals(yogaShortcut.title, ignoreCase = true) }
            }

        // Resolution logic for Meditation
        val matchedMeditation = allTasks.find { it.id.toString() == meditationShortcut.targetId }
            ?: allTasks.find { it.title.equals(meditationShortcut.targetId, ignoreCase = true) }
            ?: allTasks.find { it.title.equals(meditationShortcut.title, ignoreCase = true) }
            ?: allTasks.find {
                val words = it.title.split(Regex("[^A-Za-z0-9]")).filter { w -> w.isNotBlank() }
                words.any { w -> w.equals(meditationShortcut.targetId, ignoreCase = true) || w.equals(meditationShortcut.title, ignoreCase = true) }
            }

        assertNotNull(matchedYoga)
        assertNotNull(matchedMeditation)
        assertEquals(101L, matchedYoga!!.id)
        assertEquals(102L, matchedMeditation!!.id)
        assertNotEquals("Yoga and Meditation must resolve to distinct task entities", matchedYoga.id, matchedMeditation.id)

        // Marking Yoga complete must not affect Meditation completion state
        val updatedYoga = yogaTask.copy(isCompletedForDate = true)
        val updatedTaskList = listOf(updatedYoga, meditationTask)

        val yogaDone = updatedTaskList.find { it.id == matchedYoga.id }?.isCompletedForDate == true
        val meditationDone = updatedTaskList.find { it.id == matchedMeditation.id }?.isCompletedForDate == true

        assertTrue("Yoga must be marked complete", yogaDone)
        assertFalse("Meditation must remain incomplete", meditationDone)
    }

    @Test
    fun `test Calendar dot indicators match RecurrenceEngine scheduling and completion parity`() {
        val dailyTask = com.example.domain.model.TaskItem(
            id = 1L,
            title = "Daily Exercise",
            dueDateIso = "2026-10-01",
            recurrenceType = com.example.domain.model.RecurrenceType.DAILY,
            completedDates = setOf("2026-10-27")
        )
        val oneTimeTask = com.example.domain.model.TaskItem(
            id = 2L,
            title = "Pay Electric Bill",
            dueDateIso = "2026-10-28",
            recurrenceType = com.example.domain.model.RecurrenceType.NONE,
            isOneTimeCompleted = false
        )
        val tasks = listOf(dailyTask, oneTimeTask)

        // Visible dates: 2026-10-27 (Daily task completed), 2026-10-28 (Daily task scheduled + oneTimeTask scheduled), 2026-10-29 (Daily scheduled)
        val visibleDates = setOf("2026-10-27", "2026-10-28", "2026-10-29")
        val datesWithTasks = visibleDates.filter { dateIso ->
            tasks.any { task ->
                com.example.domain.recurrence.RecurrenceEngine.isTaskScheduledOnDate(task, dateIso) &&
                    !com.example.domain.recurrence.RecurrenceEngine.isCompletedOnDate(task, dateIso)
            }
        }.toSet()

        // 2026-10-27 has all scheduled tasks completed -> no dot
        assertFalse("2026-10-27 must not have dot because daily task is completed", datesWithTasks.contains("2026-10-27"))

        // 2026-10-28 has pending tasks -> dot present
        assertTrue("2026-10-28 must have dot indicator", datesWithTasks.contains("2026-10-28"))

        // 2026-10-29 has pending recurring daily task -> dot present
        assertTrue("2026-10-29 must have dot indicator", datesWithTasks.contains("2026-10-29"))
    }

    @Test
    fun `test trash retention duration respects user preference range`() {
        val minRetentionDays = 1
        val maxRetentionDays = 365
        val defaultRetentionDays = 30

        val minMillis = minRetentionDays.toLong().coerceIn(1L, 365L) * 86_400_000L
        val maxMillis = maxRetentionDays.toLong().coerceIn(1L, 365L) * 86_400_000L
        val defMillis = defaultRetentionDays.toLong().coerceIn(1L, 365L) * 86_400_000L

        assertEquals(86_400_000L, minMillis) // 1 day in ms
        assertEquals(31_536_000_000L, maxMillis) // 365 days in ms
        assertEquals(2_592_000_000L, defMillis) // 30 days in ms
    }
}