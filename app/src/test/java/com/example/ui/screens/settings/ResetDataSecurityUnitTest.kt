package com.example.ui.screens.settings

import com.example.domain.reset.ResetCategory
import com.example.domain.reset.ResetExecutionResult
import com.example.domain.reset.ResetSecurityPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * LifeOS v4.0.1 — Issue 05: Reset Data Features & Secure Confirmation Unit Tests.
 * Verifies:
 * - 1. Username security policy normalization, case-insensitivity, whitespace trimming, and fallback.
 * - 2. ResetExecutionResult flags (complete success, partial success, complete failure).
 * - 3. ResetCategory metadata integrity (all 13 categories well-defined with title, description, emoji).
 * - 4. Isolation policy (no protected entities in ResetCategory).
 */
class ResetDataSecurityUnitTest {

    @Test
    fun testUsernameConfirmation_exactMatch() {
        assertTrue(ResetSecurityPolicy.isUsernameConfirmed("Alex", "Alex"))
    }

    @Test
    fun testUsernameConfirmation_caseInsensitive() {
        assertTrue(ResetSecurityPolicy.isUsernameConfirmed("alex", "Alex"))
        assertTrue(ResetSecurityPolicy.isUsernameConfirmed("ALEX", "alex"))
        assertTrue(ResetSecurityPolicy.isUsernameConfirmed("aLeX", "AlEx"))
    }

    @Test
    fun testUsernameConfirmation_trimsWhitespace() {
        assertTrue(ResetSecurityPolicy.isUsernameConfirmed("  Alex  ", "Alex"))
        assertTrue(ResetSecurityPolicy.isUsernameConfirmed("Alex", "  Alex  "))
        assertTrue(ResetSecurityPolicy.isUsernameConfirmed("  Alex  ", "  Alex  "))
    }

    @Test
    fun testUsernameConfirmation_mismatchRejection() {
        assertFalse(ResetSecurityPolicy.isUsernameConfirmed("Bob", "Alex"))
        assertFalse(ResetSecurityPolicy.isUsernameConfirmed("Alexander", "Alex"))
        assertFalse(ResetSecurityPolicy.isUsernameConfirmed("Ale", "Alex"))
    }

    @Test
    fun testUsernameConfirmation_emptyAndBlankRejection() {
        assertFalse(ResetSecurityPolicy.isUsernameConfirmed("", "Alex"))
        assertFalse(ResetSecurityPolicy.isUsernameConfirmed("   ", "Alex"))
        assertFalse(ResetSecurityPolicy.isUsernameConfirmed("\t\n", "Alex"))
    }

    @Test
    fun testUsernameConfirmation_nullOrBlankProfileName_fallsBackToConfirm() {
        assertEquals("CONFIRM", ResetSecurityPolicy.getRequiredConfirmationPhrase(null))
        assertEquals("CONFIRM", ResetSecurityPolicy.getRequiredConfirmationPhrase(""))
        assertEquals("CONFIRM", ResetSecurityPolicy.getRequiredConfirmationPhrase("   "))

        assertTrue(ResetSecurityPolicy.isUsernameConfirmed("CONFIRM", null))
        assertTrue(ResetSecurityPolicy.isUsernameConfirmed("confirm", ""))
        assertTrue(ResetSecurityPolicy.isUsernameConfirmed(" Confirm ", "   "))
        assertFalse(ResetSecurityPolicy.isUsernameConfirmed("Alex", null))
    }

    @Test
    fun testResetExecutionResult_completeSuccess() {
        val result = ResetExecutionResult(
            totalRequested = 3,
            successfulCategories = listOf(ResetCategory.TASKS, ResetCategory.HABITS, ResetCategory.PLANS),
            failedCategories = emptyMap()
        )
        assertTrue(result.isCompleteSuccess)
        assertFalse(result.isPartialSuccess)
        assertFalse(result.isCompleteFailure)
        assertEquals(3, result.successfulCategories.size)
        assertEquals(0, result.failedCategories.size)
    }

    @Test
    fun testResetExecutionResult_partialSuccess() {
        val result = ResetExecutionResult(
            totalRequested = 2,
            successfulCategories = listOf(ResetCategory.TASKS),
            failedCategories = mapOf(ResetCategory.HABITS to "Database busy")
        )
        assertFalse(result.isCompleteSuccess)
        assertTrue(result.isPartialSuccess)
        assertFalse(result.isCompleteFailure)
    }

    @Test
    fun testResetExecutionResult_completeFailure() {
        val result = ResetExecutionResult(
            totalRequested = 2,
            successfulCategories = emptyList(),
            failedCategories = mapOf(
                ResetCategory.TASKS to "Disk error",
                ResetCategory.HABITS to "Transaction aborted"
            )
        )
        assertFalse(result.isCompleteSuccess)
        assertFalse(result.isPartialSuccess)
        assertTrue(result.isCompleteFailure)
    }

    @Test
    fun testResetCategoryMetadata_containsExpectedModules() {
        val categories = ResetCategory.values()
        assertEquals(13, categories.size)

        val names = categories.map { it.name }.toSet()
        val expectedNames = setOf(
            "TASKS", "HABITS", "PROJECTS", "LABELS", "PLANS", "SHIFTS",
            "GIVE_GET", "TRACKERS", "SUBSCRIPTIONS", "MEDICATIONS",
            "HONEYCOMB", "GAMIFICATION", "TRASH"
        )
        assertEquals(expectedNames, names)

        // Ensure all categories have non-blank titles, descriptions, and emojis
        categories.forEach { category ->
            assertTrue("${category.name} has valid title", category.title.isNotBlank())
            assertTrue("${category.name} has valid description", category.description.isNotBlank())
            assertTrue("${category.name} has valid emoji", category.iconEmoji.isNotBlank())
        }

        // Verify protected data is NOT exposed as a reset category
        assertFalse("Profile identity must never be a reset category", names.contains("PROFILE"))
        assertFalse("Security credentials must never be a reset category", names.contains("SECURITY"))
        assertFalse("Survey responses must never be a reset category", names.contains("SURVEY"))
    }
}
