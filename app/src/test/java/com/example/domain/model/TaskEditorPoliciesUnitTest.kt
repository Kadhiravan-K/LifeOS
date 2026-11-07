package com.example.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskEditorPoliciesUnitTest {

    @Test
    fun testMedicinePriorityRules() {
        // Table requirements:
        // Medicine ON + QR OFF -> P1
        assertEquals(TaskPriority.P1, SharedTaskPriorityPolicy.calculateDefaultPriority(isMedicineOn = true, isQrOn = false))
        // Medicine ON + QR ON -> P1
        assertEquals(TaskPriority.P1, SharedTaskPriorityPolicy.calculateDefaultPriority(isMedicineOn = true, isQrOn = true))
        // Medicine OFF + QR ON -> P2
        assertEquals(TaskPriority.P2, SharedTaskPriorityPolicy.calculateDefaultPriority(isMedicineOn = false, isQrOn = true))
        // Medicine OFF + QR OFF -> preserve default (e.g. P3)
        assertEquals(TaskPriority.P3, SharedTaskPriorityPolicy.calculateDefaultPriority(isMedicineOn = false, isQrOn = false, fallbackDefault = TaskPriority.P3))
        assertEquals(TaskPriority.P4, SharedTaskPriorityPolicy.calculateDefaultPriority(isMedicineOn = false, isQrOn = false, fallbackDefault = TaskPriority.P4))
    }

    @Test
    fun testMedicinePrioritySelectionStateTransitions() {
        var selection = MedicinePrioritySelection(
            selectedPriority = TaskPriority.P3,
            medicineReminderEnabled = false
        )
        assertEquals(TaskPriority.P3, selection.effectivePriority)

        // Enable medicine reminder -> changes to P1
        selection = selection.setMedicineReminderEnabled(true)
        assertEquals(TaskPriority.P1, selection.effectivePriority)

        // Disable medicine reminder -> reverts to P3
        selection = selection.setMedicineReminderEnabled(false)
        assertEquals(TaskPriority.P3, selection.effectivePriority)

        // User manually overrides priority to P4
        selection = selection.selectPriority(TaskPriority.P4)
        assertEquals(TaskPriority.P4, selection.effectivePriority)

        // QR turned on with medicine off -> P2
        selection = selection.setQrReminderEnabled(true)
        assertEquals(TaskPriority.P2, selection.effectivePriority)
    }

    @Test
    fun testTaskDateRangePolicy() {
        val range = TaskDateRange("2026-11-01", "2026-11-10")

        // Valid end date update
        val updatedEnd = TaskDateRangePolicy.updateEndDate(range, "2026-11-15")
        assertNotNull(updatedEnd)
        assertEquals("2026-11-15", updatedEnd?.endDateIso)

        // Same-day range is valid
        val sameDay = TaskDateRangePolicy.updateEndDate(range, "2026-11-01")
        assertNotNull(sameDay)
        assertEquals("2026-11-01", sameDay?.endDateIso)

        // Invalid end date earlier than start date rejected
        val invalidEnd = TaskDateRangePolicy.updateEndDate(range, "2026-10-31")
        assertNull(invalidEnd)

        // Moving start date after current end date adjusts end date
        val shiftedStart = TaskDateRangePolicy.updateStartDate(range, "2026-11-12")
        assertEquals("2026-11-12", shiftedStart.startDateIso)
        assertEquals("2026-11-12", shiftedStart.endDateIso)
    }

    @Test
    fun testTaskLinkUrlValidator() {
        assertTrue(TaskLinkUrlValidator.isValid("https://example.com"))
        assertTrue(TaskLinkUrlValidator.isValid("http://localhost:8080/path?query=1#frag"))
        assertTrue(TaskLinkUrlValidator.isValid("https://sub.domain.org/nested/resource.pdf"))

        assertFalse(TaskLinkUrlValidator.isValid(""))
        assertFalse(TaskLinkUrlValidator.isValid("ftp://example.com"))
        assertFalse(TaskLinkUrlValidator.isValid("javascript:alert(1)"))
        assertFalse(TaskLinkUrlValidator.isValid("not a url"))
        assertFalse(TaskLinkUrlValidator.isValid("https://user:pass@example.com")) // raw user info forbidden
    }

    @Test
    fun testTaskLinksStorageRoundTrip() {
        val originalNotes = "Meeting notes for sprint review."
        val links = listOf(
            TaskLink("Design Doc", "https://docs.google.com/123"),
            TaskLink("PR Link", "https://github.com/org/repo/pull/456")
        )

        val encoded = TaskLinksStorage.encode(originalNotes, links)
        assertTrue(encoded.contains("LIFEOS_LINKS_V1"))

        val decoded = TaskLinksStorage.decode(encoded)
        assertEquals(originalNotes, decoded.notes)
        assertEquals(2, decoded.links.size)
        assertEquals("Design Doc", decoded.links[0].label)
        assertEquals("https://docs.google.com/123", decoded.links[0].url)
        assertEquals("PR Link", decoded.links[1].label)
        assertEquals("https://github.com/org/repo/pull/456", decoded.links[1].url)

        // Notes without links decode untouched
        val noLinks = TaskLinksStorage.decode("Plain notes")
        assertEquals("Plain notes", noLinks.notes)
        assertTrue(noLinks.links.isEmpty())
    }
}
