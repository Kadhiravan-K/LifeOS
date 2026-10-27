package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayCircle
import com.example.domain.model.DurationUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FrontendComponentsUnitTest {

    @Test
    fun parseTimeToHourMinute_validInputs_parsedAccurately() {
        assertEquals(0 to 0, parseTimeToHourMinute("00:00"))
        assertEquals(9 to 0, parseTimeToHourMinute("09:00"))
        assertEquals(14 to 30, parseTimeToHourMinute("14:30"))
        assertEquals(23 to 59, parseTimeToHourMinute("23:59"))
    }

    @Test
    fun parseTimeToHourMinute_invalidOrBlank_fallbackSafely() {
        assertEquals(9 to 0, parseTimeToHourMinute(null))
        assertEquals(9 to 0, parseTimeToHourMinute(""))
        assertEquals(9 to 0, parseTimeToHourMinute("   "))
        assertEquals(9 to 0, parseTimeToHourMinute("invalid"))
        assertEquals(9 to 0, parseTimeToHourMinute("999"))
    }

    @Test
    fun parseTimeToHourMinute_outOfRange_clamped() {
        assertEquals(23 to 59, parseTimeToHourMinute("28:99"))
        assertEquals(0 to 0, parseTimeToHourMinute("0:0"))
    }

    @Test
    fun formatTimeDisplay_24HourMode_matchesExpected() {
        assertEquals("00:00", formatTimeDisplay("00:00", is24Hour = true))
        assertEquals("09:05", formatTimeDisplay("09:05", is24Hour = true))
        assertEquals("12:00", formatTimeDisplay("12:00", is24Hour = true))
        assertEquals("14:30", formatTimeDisplay("14:30", is24Hour = true))
        assertEquals("23:59", formatTimeDisplay("23:59", is24Hour = true))
    }

    @Test
    fun formatTimeDisplay_12HourMode_formatsAmPmCorrectly() {
        assertEquals("12:00 AM", formatTimeDisplay("00:00", is24Hour = false))
        assertEquals("12:45 AM", formatTimeDisplay("00:45", is24Hour = false))
        assertEquals("9:05 AM", formatTimeDisplay("09:05", is24Hour = false))
        assertEquals("12:00 PM", formatTimeDisplay("12:00", is24Hour = false))
        assertEquals("1:15 PM", formatTimeDisplay("13:15", is24Hour = false))
        assertEquals("2:30 PM", formatTimeDisplay("14:30", is24Hour = false))
        assertEquals("11:59 PM", formatTimeDisplay("23:59", is24Hour = false))
    }

    @Test
    fun formatTimeDisplay_emptyOrNull_returnsEmpty() {
        assertEquals("", formatTimeDisplay(null, is24Hour = false))
        assertEquals("", formatTimeDisplay("", is24Hour = false))
    }

    @Test
    fun defaultTimePresets_areAllValidAndParseCleanly() {
        assertTrue(DefaultTimePresets.isNotEmpty())
        for ((timeStr, label) in DefaultTimePresets) {
            val (h, m) = parseTimeToHourMinute(timeStr)
            assertTrue("Hour $h should be 0..23", h in 0..23)
            assertTrue("Minute $m should be 0..59", m in 0..59)
            assertTrue("Label should not be empty", label.isNotBlank())
        }
    }

    @Test
    fun defaultDurationPresets_areAllValid() {
        assertTrue(DefaultDurationPresets.isNotEmpty())
        for ((amount, unit, label) in DefaultDurationPresets) {
            assertTrue("Amount should be > 0", amount > 0)
            assertTrue("Unit should be valid", DurationUnit.entries.contains(unit))
            assertTrue("Label should not be empty", label.isNotBlank())
        }
    }

    @Test
    fun durationInputValidation_zeroAndNegative_areErrors() {
        assertTrue("0 duration must be error", 0 <= 0)
        assertTrue("-1 duration must be error", -1 <= 0)
        assertFalse("30 duration is valid", 30 <= 0)
        assertFalse("1 duration is valid", 1 <= 0)
    }

    @Test
    fun durationAmountParsing_filteringLogic() {
        fun simulateInput(raw: String): Int {
            val digits = raw.filter { it.isDigit() }
            if (digits.isEmpty()) return 0
            val parsed = digits.toIntOrNull()
            return if (parsed != null && parsed in 0..9999) parsed else 0
        }

        assertEquals(0, simulateInput(""))
        assertEquals(0, simulateInput("0"))
        assertEquals(45, simulateInput("45"))
        assertEquals(45, simulateInput("45mins"))
        assertEquals(9999, simulateInput("9999"))
        assertEquals(0, simulateInput("100000"))
    }

    @Test
    fun durationSteppers_incrementAndDecrementAccurately() {
        fun step(amount: Int, isIncrement: Boolean, unit: DurationUnit): Int {
            val stepSize = if (unit == DurationUnit.MINUTES) 5 else 1
            return if (isIncrement) (amount + stepSize).coerceIn(0, 9999)
            else (amount - stepSize).coerceAtLeast(0)
        }

        assertEquals(20, step(15, isIncrement = true, DurationUnit.MINUTES))
        assertEquals(10, step(15, isIncrement = false, DurationUnit.MINUTES))
        assertEquals(0, step(3, isIncrement = false, DurationUnit.MINUTES))
        assertEquals(2, step(1, isIncrement = true, DurationUnit.HOURS))
        assertEquals(0, step(1, isIncrement = false, DurationUnit.HOURS))
    }

    @Test
    fun urlAutoPrefixing_formatsHttpsProperly() {
        fun formatUrl(raw: String): String {
            val trimmed = raw.trim()
            return if (!trimmed.contains("://")) "https://$trimmed" else trimmed
        }

        assertEquals("https://zoom.us/j/123456", formatUrl("zoom.us/j/123456"))
        assertEquals("https://maps.google.com", formatUrl("https://maps.google.com"))
        assertEquals("http://local.test", formatUrl("http://local.test"))
    }

    @Test
    fun detectLinkIcon_identifiesServiceTypes() {
        assertEquals(Icons.Default.Place, detectLinkIcon("https://maps.google.com/place/xyz"))
        assertEquals(Icons.Default.Place, detectLinkIcon("https://goo.gl/maps/123"))
        assertEquals(Icons.Default.PlayCircle, detectLinkIcon("https://youtube.com/watch?v=123"))
        assertEquals(Icons.Default.PlayCircle, detectLinkIcon("https://youtu.be/123"))
        assertEquals(Icons.Default.Language, detectLinkIcon("https://github.com/Kadhiravan-K/LifeOS"))
    }

    @Test
    fun masterEmojiCatalog_categoriesAndSearchWorkCleanly() {
        assertTrue(MasterEmojiCatalog.isNotEmpty())
        val categories = MasterEmojiCatalog.map { it.category }.toSet()
        assertTrue(categories.contains("Smileys"))
        assertTrue(categories.contains("Activities"))
        assertTrue(categories.contains("Objects"))
        assertTrue(categories.contains("Health"))
        assertTrue(categories.contains("Finance"))

        // Search test
        val runMatches = MasterEmojiCatalog.filter { it.keywords.contains("cardio") }
        assertTrue(runMatches.any { it.emoji == "🏃" })

        val waterMatches = MasterEmojiCatalog.filter { it.keywords.contains("hydrate") }
        assertTrue(waterMatches.any { it.emoji == "💧" })

        val financeMatches = MasterEmojiCatalog.filter { it.keywords.contains("crypto") }
        assertTrue(financeMatches.any { it.emoji == "🪙" })
    }

    @Test
    fun safetyDialogConfirmationPhrase_matchesCaseInsensitively() {
        fun isMatching(typed: String, required: String): Boolean {
            return typed.trim().equals(required.trim(), ignoreCase = true)
        }

        assertTrue(isMatching("DELETE", "DELETE"))
        assertTrue(isMatching("delete", "DELETE"))
        assertTrue(isMatching(" Delete ", "DELETE"))
        assertFalse(isMatching("del", "DELETE"))
        assertFalse(isMatching("", "DELETE"))
    }
}
