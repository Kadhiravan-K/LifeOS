package com.example.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskEditorPoliciesUnitTest {

    @Test
    fun `medicine reminder forces P1 and restores selected priority when disabled`() {
        val selection = MedicinePrioritySelection(TaskPriority.P3, false)
            .setMedicineReminderEnabled(true)

        assertEquals(TaskPriority.P1, selection.effectivePriority)
        val selectedWhileEnabled = selection.selectPriority(TaskPriority.P2)
        assertEquals(TaskPriority.P1, selectedWhileEnabled.effectivePriority)
        assertEquals(TaskPriority.P2, selectedWhileEnabled.setMedicineReminderEnabled(false).effectivePriority)
    }

    @Test
    fun `date range clamps deadline when start moves beyond it`() {
        assertEquals(
            TaskDateRange("2025-03-01", "2025-03-01"),
            TaskDateRangePolicy.updateStartDate(
                TaskDateRange("2025-02-28", "2025-02-28"),
                "2025-03-01"
            )
        )
        assertNull(
            TaskDateRangePolicy.updateEndDate(
                TaskDateRange("2025-03-01", null),
                "2025-02-28"
            )
        )
        assertNotNull(
            TaskDateRangePolicy.updateEndDate(
                TaskDateRange("2025-03-01", null),
                "2025-03-01"
            )
        )
    }

    @Test
    fun `labeled links round trip alongside ordinary notes`() {
        val links = listOf(
            TaskLink("Project plan", "https://example.com/plan?a=1&b=2"),
            TaskLink("Repository", "http://example.org")
        )
        val encoded = TaskLinksStorage.encode("Keep this note.", links)

        assertEquals(TaskNotesWithLinks("Keep this note.", links), TaskLinksStorage.decode(encoded))
        assertEquals("Keep this note.", TaskLinksStorage.encode("Keep this note.", emptyList()))
        assertEquals(TaskNotesWithLinks("Legacy note", emptyList()), TaskLinksStorage.decode("Legacy note"))
    }

    @Test
    fun `task links accept only safe http and https URLs`() {
        assertTrue(TaskLinkUrlValidator.isValid("https://example.com/path"))
        assertTrue(TaskLinkUrlValidator.isValid("http://example.com"))
        assertFalse(TaskLinkUrlValidator.isValid("javascript:alert(1)"))
        assertFalse(TaskLinkUrlValidator.isValid("https://user:password@example.com"))
        assertFalse(TaskLinkUrlValidator.isValid("https://"))
    }
}
