package com.example.domain.model

import java.net.URI
import java.net.URISyntaxException
import java.time.LocalDate
import java.util.Base64

/**
 * Shared task priority resolution policy conforming to:
 * | Medicine reminder | QR Quick Reminder | Default priority |
 * | ON                | OFF               | P1               |
 * | ON                | ON                | P1               |
 * | OFF               | ON                | P2               |
 * | OFF               | OFF               | Preserved default|
 */
object SharedTaskPriorityPolicy {
    fun calculateDefaultPriority(
        isMedicineOn: Boolean,
        isQrOn: Boolean,
        fallbackDefault: TaskPriority = TaskPriority.P3
    ): TaskPriority {
        return when {
            isMedicineOn -> TaskPriority.P1
            isQrOn -> TaskPriority.P2
            else -> fallbackDefault
        }
    }
}

data class MedicinePrioritySelection(
    val selectedPriority: TaskPriority,
    val medicineReminderEnabled: Boolean,
    val isQrQuickReminderEnabled: Boolean = false,
    val isManuallyOverridden: Boolean = false
) {
    val effectivePriority: TaskPriority
        get() = when {
            medicineReminderEnabled -> TaskPriority.P1
            isQrQuickReminderEnabled -> TaskPriority.P2
            else -> selectedPriority
        }

    fun selectPriority(priority: TaskPriority): MedicinePrioritySelection =
        copy(selectedPriority = priority, isManuallyOverridden = true)

    fun setMedicineReminderEnabled(enabled: Boolean): MedicinePrioritySelection =
        copy(
            medicineReminderEnabled = enabled,
            selectedPriority = if (enabled) TaskPriority.P1 else if (!isManuallyOverridden) {
                SharedTaskPriorityPolicy.calculateDefaultPriority(false, isQrQuickReminderEnabled, TaskPriority.P3)
            } else selectedPriority
        )

    fun setQrReminderEnabled(enabled: Boolean): MedicinePrioritySelection =
        copy(
            isQrQuickReminderEnabled = enabled,
            selectedPriority = if (!medicineReminderEnabled && !isManuallyOverridden) {
                SharedTaskPriorityPolicy.calculateDefaultPriority(false, enabled, TaskPriority.P3)
            } else selectedPriority
        )
}

data class TaskDateRange(
    val startDateIso: String,
    val endDateIso: String?
)

object TaskDateRangePolicy {
    fun updateStartDate(range: TaskDateRange, newStartDateIso: String): TaskDateRange {
        val newStart = LocalDate.parse(newStartDateIso)
        val currentEnd = range.endDateIso?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val adjustedEnd = when {
            currentEnd == null -> null
            currentEnd.isBefore(newStart) -> newStartDateIso
            else -> range.endDateIso
        }
        return TaskDateRange(
            startDateIso = newStartDateIso,
            endDateIso = adjustedEnd
        )
    }

    fun updateEndDate(range: TaskDateRange, newEndDateIso: String): TaskDateRange? {
        val start = LocalDate.parse(range.startDateIso)
        val end = LocalDate.parse(newEndDateIso)
        if (end.isBefore(start)) return null
        return range.copy(endDateIso = newEndDateIso)
    }
}

object TaskLinkUrlValidator {
    fun isValid(url: String): Boolean {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return false
        return try {
            val uri = URI(trimmed)
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
        } catch (_: Exception) {
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
