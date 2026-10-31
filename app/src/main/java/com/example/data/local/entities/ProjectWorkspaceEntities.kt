package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Advanced Project Database Workspace Item Entity (Release 3 Requirement #20).
 * Stores records within a project with Notion-like attributes (Status, Progress, Category, Version, Custom Properties).
 */
@Entity(
    tableName = "project_workspace_items",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("profileId"),
        Index("projectId"),
        Index("status"),
        Index("category"),
        Index("version")
    ]
)
data class ProjectWorkspaceItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val profileId: Long = 1,
    val projectId: Long,
    val title: String,
    val status: String = STATUS_NOT_STARTED,
    val progress: Int = 0,
    val category: String = CATEGORY_FEATURE,
    val version: String = "V1",
    val dueDateIso: String? = null,
    val notes: String = "",
    val priority: String = "P2",
    val customPropertiesJson: String = "{}",
    val orderIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_NOT_STARTED = "NOT_STARTED"
        const val STATUS_IN_PROGRESS = "IN_PROGRESS"
        const val STATUS_DONE = "DONE"

        const val CATEGORY_FEATURE = "Feature"
        const val CATEGORY_BUG = "Bug"
        const val CATEGORY_IMPROVEMENT = "Improvement"
    }
}

/**
 * Saved View for Advanced Project Database Workspace (Release 3 Requirement #20).
 * Supports saving customized Table, Board, Calendar, List, and Gallery configurations.
 */
@Entity(
    tableName = "project_workspace_views",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("profileId"),
        Index("projectId")
    ]
)
data class ProjectWorkspaceViewEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val profileId: Long = 1,
    val projectId: Long,
    val name: String,
    val viewType: String = VIEW_TABLE,
    val filterStatus: String? = null,
    val filterCategory: String? = null,
    val filterVersion: String? = null,
    val sortBy: String = "orderIndex",
    val sortAscending: Boolean = true,
    val groupBy: String = "status",
    val visiblePropertiesJson: String = "[\"status\",\"progress\",\"category\",\"version\",\"dueDate\"]",
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val VIEW_TABLE = "TABLE"
        const val VIEW_BOARD = "BOARD"
        const val VIEW_CALENDAR = "CALENDAR"
        const val VIEW_LIST = "LIST"
        const val VIEW_GALLERY = "GALLERY"
    }
}

/**
 * Custom Status definition per Project (Release 3 Requirement #20).
 */
@Entity(
    tableName = "project_custom_statuses",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("profileId"),
        Index("projectId")
    ]
)
data class ProjectCustomStatusEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val profileId: Long = 1,
    val projectId: Long,
    val name: String,
    val colorHex: Long = 4282168054L,
    val orderIndex: Int = 0
)
