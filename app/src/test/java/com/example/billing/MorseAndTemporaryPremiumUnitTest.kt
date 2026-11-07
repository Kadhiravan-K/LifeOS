package com.example.billing

import com.example.domain.gamification.TemporaryPremiumTierPolicy
import com.example.domain.projectworkspace.ProjectWorkspaceItem
import com.example.utils.MorseCodeSecretMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MorseAndTemporaryPremiumUnitTest {

    @Test
    fun morseCodeSecretMatcher_matchesUserAndStandardSequences() {
        // User exact specified string: "-.- .- - .... .. .-. .- ...- .- -."
        val userExact = MorseCodeSecretMatcher.FLATTENED_USER_SEQUENCE
        assertTrue("Exact user sequence must match", MorseCodeSecretMatcher.isSecretMatch(userExact))

        // Standard morse with D as '-..'
        val standard = MorseCodeSecretMatcher.FLATTENED_STANDARD_SEQUENCE
        assertTrue("Standard KADHIRAVAN morse must match", MorseCodeSecretMatcher.isSecretMatch(standard))

        // With leading touches before completing the pattern
        val withPrefix = "....-.-." + userExact
        assertTrue("Prefix followed by secret pattern must match", MorseCodeSecretMatcher.isSecretMatch(withPrefix))

        // Incomplete sequence
        assertFalse("Incomplete sequence must not match", MorseCodeSecretMatcher.isSecretMatch("-.-.--....-..."))

        // Blank or arbitrary touches
        assertFalse("Random taps must not match", MorseCodeSecretMatcher.isSecretMatch(".......-----"))
    }

    @Test
    fun temporaryPremiumTierPolicy_calculatesCorrectTierDurations() {
        val hourMillis = 60L * 60L * 1000L

        // Tier 1: 1st purchase (count == 0) -> 3 days (72 hours)
        assertEquals(72L * hourMillis, TemporaryPremiumTierPolicy.calculateDurationMillis(0))
        assertEquals(72L * hourMillis, TemporaryPremiumTierPolicy.calculateDurationMillis(-1))
        assertEquals("3 Days (72h)", TemporaryPremiumTierPolicy.getDurationLabel(0))

        // Tier 2: 2nd to 10th purchase (count in 1..9) -> 1 day (24 hours)
        assertEquals(24L * hourMillis, TemporaryPremiumTierPolicy.calculateDurationMillis(1))
        assertEquals(24L * hourMillis, TemporaryPremiumTierPolicy.calculateDurationMillis(5))
        assertEquals(24L * hourMillis, TemporaryPremiumTierPolicy.calculateDurationMillis(9))
        assertEquals("1 Day (24h)", TemporaryPremiumTierPolicy.getDurationLabel(1))
        assertEquals("1 Day (24h)", TemporaryPremiumTierPolicy.getDurationLabel(9))

        // Tier 3: 11th to 20th purchase (count in 10..19) -> 18 hours
        assertEquals(18L * hourMillis, TemporaryPremiumTierPolicy.calculateDurationMillis(10))
        assertEquals(18L * hourMillis, TemporaryPremiumTierPolicy.calculateDurationMillis(15))
        assertEquals(18L * hourMillis, TemporaryPremiumTierPolicy.calculateDurationMillis(19))
        assertEquals("18 Hours", TemporaryPremiumTierPolicy.getDurationLabel(10))
        assertEquals("18 Hours", TemporaryPremiumTierPolicy.getDurationLabel(19))

        // Tier 4: 21st+ purchase (count >= 20) -> 12 hours
        assertEquals(12L * hourMillis, TemporaryPremiumTierPolicy.calculateDurationMillis(20))
        assertEquals(12L * hourMillis, TemporaryPremiumTierPolicy.calculateDurationMillis(50))
        assertEquals("12 Hours", TemporaryPremiumTierPolicy.getDurationLabel(20))
        assertEquals("12 Hours", TemporaryPremiumTierPolicy.getDurationLabel(100))
    }

    @Test
    fun projectWorkspaceItem_synthesizedTaskMapping_preservesFields() {
        val taskId = 42L
        val projectId = 10L
        val synthesized = ProjectWorkspaceItem(
            id = -taskId,
            profileId = 1L,
            projectId = projectId,
            title = "Template Imported Project Task",
            status = "DONE",
            progress = 100,
            category = "Work & Career",
            version = "V1",
            dueDateIso = "2026-11-10",
            notes = "Sprint deliverables",
            priority = "P1",
            customProperties = mapOf("linkedTaskId" to taskId.toString(), "source" to "task_table"),
            orderIndex = 0
        )

        assertEquals(-42L, synthesized.id)
        assertEquals(10L, synthesized.projectId)
        assertEquals("Template Imported Project Task", synthesized.title)
        assertEquals("DONE", synthesized.status)
        assertEquals("42", synthesized.customProperties["linkedTaskId"])
        assertEquals("task_table", synthesized.customProperties["source"])
    }
}
