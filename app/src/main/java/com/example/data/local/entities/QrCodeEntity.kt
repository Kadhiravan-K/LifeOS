package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "qr_codes",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("profileId"),
        Index(value = ["profileId", "updatedAt"]),
        Index(value = ["profileId", "sourceTemplateId"], unique = true)
    ]
)
data class QrCodeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val title: String,
    val type: String,
    val fieldsJson: String,
    val payload: String,
    val sourceTemplateId: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)
