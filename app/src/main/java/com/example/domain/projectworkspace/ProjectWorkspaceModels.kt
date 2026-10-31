package com.example.domain.projectworkspace

import com.example.data.local.entities.ProjectCustomStatusEntity
import com.example.data.local.entities.ProjectWorkspaceItemEntity
import com.example.data.local.entities.ProjectWorkspaceViewEntity
import org.json.JSONArray
import org.json.JSONObject

/**
 * Supported Visual Layouts for Advanced Project Database (Release 3 Requirement #20).
 */
enum class WorkspaceViewType(val label: String) {
    TABLE("Table"),
    BOARD("Board"),
    CALENDAR("Calendar"),
    LIST("List"),
    GALLERY("Gallery")
}

/**
 * Grouping options for items in workspace views.
 */
enum class WorkspaceGroupBy(val label: String) {
    STATUS("Status"),
    CATEGORY("Category"),
    VERSION("Version"),
    PRIORITY("Priority"),
    NONE("None")
}

/**
 * Sorting options for items in workspace views.
 */
enum class WorkspaceSortBy(val label: String) {
    TITLE("Name"),
    DUE_DATE("Due Date"),
    PROGRESS("Progress"),
    STATUS("Status"),
    PRIORITY("Priority"),
    ORDER_INDEX("Manual Order")
}

/**
 * Domain representation of a Project Workspace Item.
 */
data class ProjectWorkspaceItem(
    val id: Long = 0,
    val profileId: Long = 1,
    val projectId: Long,
    val title: String,
    val status: String = "NOT_STARTED",
    val progress: Int = 0,
    val category: String = "Feature",
    val version: String = "V1",
    val dueDateIso: String? = null,
    val notes: String = "",
    val priority: String = "P2",
    val customProperties: Map<String, String> = emptyMap(),
    val orderIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toEntity(): ProjectWorkspaceItemEntity {
        val json = JSONObject()
        customProperties.forEach { (k, v) -> json.put(k, v) }
        return ProjectWorkspaceItemEntity(
            id = id,
            profileId = profileId,
            projectId = projectId,
            title = title,
            status = status,
            progress = progress.coerceIn(0, 100),
            category = category,
            version = version,
            dueDateIso = dueDateIso,
            notes = notes,
            priority = priority,
            customPropertiesJson = json.toString(),
            orderIndex = orderIndex,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromEntity(entity: ProjectWorkspaceItemEntity): ProjectWorkspaceItem {
            val map = mutableMapOf<String, String>()
            try {
                val json = JSONObject(entity.customPropertiesJson)
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    map[key] = json.optString(key, "")
                }
            } catch (_: Exception) {}

            return ProjectWorkspaceItem(
                id = entity.id,
                profileId = entity.profileId,
                projectId = entity.projectId,
                title = entity.title,
                status = entity.status,
                progress = entity.progress,
                category = entity.category,
                version = entity.version,
                dueDateIso = entity.dueDateIso,
                notes = entity.notes,
                priority = entity.priority,
                customProperties = map,
                orderIndex = entity.orderIndex,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
        }
    }
}

/**
 * Domain representation of a Saved View configuration.
 */
data class ProjectWorkspaceView(
    val id: Long = 0,
    val profileId: Long = 1,
    val projectId: Long,
    val name: String,
    val viewType: WorkspaceViewType = WorkspaceViewType.TABLE,
    val filterStatus: String? = null,
    val filterCategory: String? = null,
    val filterVersion: String? = null,
    val sortBy: WorkspaceSortBy = WorkspaceSortBy.ORDER_INDEX,
    val sortAscending: Boolean = true,
    val groupBy: WorkspaceGroupBy = WorkspaceGroupBy.STATUS,
    val visibleProperties: Set<String> = setOf("status", "progress", "category", "version", "dueDate"),
    val isDefault: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toEntity(): ProjectWorkspaceViewEntity {
        val jsonArray = JSONArray()
        visibleProperties.forEach { jsonArray.put(it) }
        return ProjectWorkspaceViewEntity(
            id = id,
            profileId = profileId,
            projectId = projectId,
            name = name,
            viewType = viewType.name,
            filterStatus = filterStatus,
            filterCategory = filterCategory,
            filterVersion = filterVersion,
            sortBy = sortBy.name,
            sortAscending = sortAscending,
            groupBy = groupBy.name,
            visiblePropertiesJson = jsonArray.toString(),
            isDefault = isDefault,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromEntity(entity: ProjectWorkspaceViewEntity): ProjectWorkspaceView {
            val props = mutableSetOf<String>()
            try {
                val jsonArray = JSONArray(entity.visiblePropertiesJson)
                for (i in 0 until jsonArray.length()) {
                    props.add(jsonArray.getString(i))
                }
            } catch (_: Exception) {}

            val viewType = try {
                WorkspaceViewType.valueOf(entity.viewType)
            } catch (_: Exception) {
                WorkspaceViewType.TABLE
            }

            val sortBy = try {
                WorkspaceSortBy.valueOf(entity.sortBy)
            } catch (_: Exception) {
                WorkspaceSortBy.ORDER_INDEX
            }

            val groupBy = try {
                WorkspaceGroupBy.valueOf(entity.groupBy)
            } catch (_: Exception) {
                WorkspaceGroupBy.STATUS
            }

            return ProjectWorkspaceView(
                id = entity.id,
                profileId = entity.profileId,
                projectId = entity.projectId,
                name = entity.name,
                viewType = viewType,
                filterStatus = entity.filterStatus,
                filterCategory = entity.filterCategory,
                filterVersion = entity.filterVersion,
                sortBy = sortBy,
                sortAscending = entity.sortAscending,
                groupBy = groupBy,
                visibleProperties = if (props.isEmpty()) setOf("status", "progress", "category", "version", "dueDate") else props,
                isDefault = entity.isDefault,
                updatedAt = entity.updatedAt
            )
        }
    }
}

/**
 * Domain representation of a Project Custom Status.
 */
data class ProjectCustomStatus(
    val id: Long = 0,
    val profileId: Long = 1,
    val projectId: Long,
    val name: String,
    val colorHex: Long = 0xFF6366F1,
    val orderIndex: Int = 0
) {
    fun toEntity(): ProjectCustomStatusEntity = ProjectCustomStatusEntity(
        id = id,
        profileId = profileId,
        projectId = projectId,
        name = name,
        colorHex = colorHex,
        orderIndex = orderIndex
    )

    companion object {
        fun fromEntity(entity: ProjectCustomStatusEntity): ProjectCustomStatus = ProjectCustomStatus(
            id = entity.id,
            profileId = entity.profileId,
            projectId = entity.projectId,
            name = entity.name,
            colorHex = entity.colorHex,
            orderIndex = entity.orderIndex
        )
    }
}

/**
 * Configuration for generating Structured Markdown Prompts for AI Coding Agents.
 */
data class AiPromptConfig(
    val selectedCategories: Set<String> = setOf("Feature", "Bug", "Improvement"),
    val selectedVersions: Set<String> = emptySet(), // empty means all versions
    val selectedStatuses: Set<String> = emptySet(), // empty means all statuses
    val explicitItemIds: Set<Long>? = null, // null means use criteria; if present, only selected IDs
    val includeNotes: Boolean = true,
    val includeProgress: Boolean = true,
    val objectiveDescription: String = ""
)

/**
 * Engine for generating structured Markdown prompts for AI coding agents (Release 3 Requirement #20).
 * Never modifies database records.
 */
object AiPromptGenerator {

    fun generateMarkdownPrompt(
        projectName: String,
        projectDescription: String = "",
        items: List<ProjectWorkspaceItem>,
        config: AiPromptConfig
    ): String {
        val filtered = items.filter { item ->
            val matchesId = config.explicitItemIds == null || config.explicitItemIds.contains(item.id)
            val matchesCategory = config.selectedCategories.isEmpty() || config.selectedCategories.contains(item.category)
            val matchesVersion = config.selectedVersions.isEmpty() || config.selectedVersions.contains(item.version)
            val matchesStatus = config.selectedStatuses.isEmpty() || config.selectedStatuses.contains(item.status)
            matchesId && matchesCategory && matchesVersion && matchesStatus
        }

        val features = filtered.filter { it.category.equals("Feature", ignoreCase = true) }
        val bugs = filtered.filter { it.category.equals("Bug", ignoreCase = true) }
        val improvements = filtered.filter { it.category.equals("Improvement", ignoreCase = true) }
        val others = filtered.filter { !it.category.equals("Feature", ignoreCase = true) &&
                !it.category.equals("Bug", ignoreCase = true) &&
                !it.category.equals("Improvement", ignoreCase = true) }

        val sb = StringBuilder()
        sb.append("# TASK CONTEXT: ").append(projectName).append("\n\n")

        if (projectDescription.isNotBlank()) {
            sb.append("> ").append(projectDescription).append("\n\n")
        }

        if (config.objectiveDescription.isNotBlank()) {
            sb.append("## Objective\n").append(config.objectiveDescription).append("\n\n")
        } else {
            val versionsStr = if (config.selectedVersions.isNotEmpty()) config.selectedVersions.joinToString(", ") else "Current Milestone"
            sb.append("## Objective\nImplement and verify the requirements for ").append(versionsStr).append(".\n\n")
        }

        sb.append("## Work Items & Acceptance Criteria\n\n")

        fun renderSection(title: String, sectionItems: List<ProjectWorkspaceItem>) {
            if (sectionItems.isEmpty()) return
            sb.append("### ").append(title).append(" (").append(sectionItems.size).append(")\n\n")
            sectionItems.forEach { item ->
                val statusFormatted = when (item.status) {
                    "NOT_STARTED" -> "Not Started"
                    "IN_PROGRESS" -> "In Progress"
                    "DONE" -> "Completed"
                    else -> item.status
                }
                val check = if (item.status == "DONE") "[x]" else "[ ]"
                sb.append("- ").append(check).append(" **").append(item.title).append("**")
                sb.append(" `[").append(item.version).append("]`")
                sb.append(" `[").append(item.priority).append("]`")
                sb.append(" (Status: ").append(statusFormatted)
                if (config.includeProgress && item.progress > 0) {
                    sb.append(", Progress: ").append(item.progress).append("%")
                }
                if (!item.dueDateIso.isNullOrBlank()) {
                    sb.append(", Due: ").append(item.dueDateIso)
                }
                sb.append(")\n")

                if (config.includeNotes && item.notes.isNotBlank()) {
                    sb.append("  - *Details:* ").append(item.notes.replace("\n", " ")).append("\n")
                }
                if (item.customProperties.isNotEmpty()) {
                    val propsStr = item.customProperties.entries.joinToString(", ") { "${it.key}: ${it.value}" }
                    sb.append("  - *Properties:* ").append(propsStr).append("\n")
                }
            }
            sb.append("\n")
        }

        renderSection("Features", features)
        renderSection("Bug Fixes", bugs)
        renderSection("Improvements", improvements)
        renderSection("Additional Tasks", others)

        sb.append("## Implementation Guidelines\n")
        sb.append("- Preserve existing architecture, Room migration paths, and multi-profile isolation.\n")
        sb.append("- Write unit tests for all business logic and verify clean Gradle compilation.\n")
        sb.append("- Maintain non-destructive database updates.\n")

        return sb.toString().trim()
    }
}
