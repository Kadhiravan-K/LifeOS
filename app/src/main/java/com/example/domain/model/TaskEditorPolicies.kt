package com.example.domain.model

import java.net.URI
import java.net.URISyntaxException
import java.util.Base64

data class MedicinePrioritySelection(
    val selectedPriority: TaskPriority,
    val medicineReminderEnabled: Boolean
) {
    val effectivePriority: TaskPriority
        get() = if (medicineReminderEnabled) TaskPriority.P1 else selectedPriority

    fun selectPriority(priority: TaskPriority): MedicinePrioritySelection =
        copy(selectedPriority = priority)

    fun setMedicineReminderEnabled(enabled: Boolean): MedicinePrioritySelection =
        copy(medicineReminderEnabled = enabled)
}

data class TaskDateRange(
    val startDateIso: String,
    val endDateIso: String?
)

object TaskDateRangePolicy {
    fun updateStartDate(range: TaskDateRange, newStartDateIso: String): TaskDateRange {
        val newStart = java.time.LocalDate.parse(newStartDateIso)
        val currentEnd = range.endDateIso?.let(java.time.LocalDate::parse)
        return TaskDateRange(
            startDateIso = newStartDateIso,
            endDateIso = currentEnd?.takeUnless { it.isBefore(newStart) }?.toString()
                ?: currentEnd?.let { newStartDateIso }
        )
    }

    fun updateEndDate(range: TaskDateRange, newEndDateIso: String): TaskDateRange? {
        val start = java.time.LocalDate.parse(range.startDateIso)
        val end = java.time.LocalDate.parse(newEndDateIso)
        if (end.isBefore(start)) return null
        return range.copy(endDateIso = newEndDateIso)
    }
}

object TaskLinkUrlValidator {
    fun isValid(url: String): Boolean {
        return try {
            val uri = URI(url.trim())
            uri.scheme?.lowercase() in setOf("http", "https") &&
                !uri.host.isNullOrBlank() &&
                uri.rawUserInfo == null
        } catch (_: URISyntaxException) {
            false
        }
    }
}

data class TaskNotesWithLinks(
    val notes: String,
    val links: List<TaskLink>
)

object TaskLinksStorage {
    private const val MARKER = "\n\u001eLIFEOS_LINKS_V1\u001e\n"
    private val encoder = Base64.getUrlEncoder().withoutPadding()
    private val decoder = Base64.getUrlDecoder()

    fun decode(encodedNotes: String): TaskNotesWithLinks {
        val markerIndex = encodedNotes.lastIndexOf(MARKER)
        if (markerIndex < 0) return TaskNotesWithLinks(encodedNotes, emptyList())
        val payload = encodedNotes.substring(markerIndex + MARKER.length)
        return try {
            val links = payload.lineSequence()
                .filter(String::isNotBlank)
                .map { line ->
                    val separator = line.indexOf('.')
                    require(separator > 0)
                    TaskLink(
                        label = decodePart(line.substring(0, separator)),
                        url = decodePart(line.substring(separator + 1))
                    )
                }
                .toList()
            TaskNotesWithLinks(encodedNotes.substring(0, markerIndex), links)
        } catch (_: IllegalArgumentException) {
            TaskNotesWithLinks(encodedNotes, emptyList())
        }
    }

    fun encode(notes: String, links: List<TaskLink>): String {
        val cleanNotes = decode(notes).notes
        if (links.isEmpty()) return cleanNotes
        val payload = links.joinToString("\n") { link ->
            "${encodePart(link.label)}.${encodePart(link.url)}"
        }
        return cleanNotes + MARKER + payload
    }

    private fun encodePart(value: String): String =
        encoder.encodeToString(value.toByteArray(Charsets.UTF_8))

    private fun decodePart(value: String): String =
        decoder.decode(value).toString(Charsets.UTF_8)
}
