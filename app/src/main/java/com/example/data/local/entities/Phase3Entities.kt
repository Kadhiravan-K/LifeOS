package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "projects",
    indices = [
        Index("profileId"),
        Index("orderIndex")
    ]
)
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val profileId: Long = 1,
    val name: String,
    val description: String = "",
    val colorId: String = "blue",
    val emoji: String = "📁",
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val orderIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "labels",
    indices = [
        Index("profileId"),
        Index("orderIndex")
    ]
)
data class LabelEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val profileId: Long = 1,
    val name: String,
    val colorId: String = "blue",
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val orderIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "task_label_cross_ref",
    primaryKeys = ["taskId", "labelId"],
    indices = [
        Index("taskId"),
        Index("labelId")
    ]
)
data class TaskLabelCrossRef(
    val taskId: Long,
    val labelId: Long
)

@Entity(
    tableName = "task_reminders",
    indices = [
        Index("taskId")
    ]
)
data class TaskReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val taskId: Long,
    val minutesBefore: Int = 0,
    val customTime: String? = null,
    val type: String = "OFFSET",
    val referenceType: String = "START_TIME",
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
