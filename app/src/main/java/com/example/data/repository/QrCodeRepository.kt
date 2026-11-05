package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.entities.QrCodeEntity
import com.example.domain.qr.QrCodeDraft
import com.example.domain.qr.QrCodeImportSummary
import com.example.domain.qr.QrCodePayload
import com.example.domain.qr.QrCodeRecord
import com.example.domain.qr.QrCodeTemplate
import com.example.domain.qr.QrCodeType
import com.example.domain.qr.QrCodeValidation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class QrCodeRepository(private val database: AppDatabase) {
    private val dao = database.qrCodeDao()

    fun observeForProfile(profileId: Long): Flow<List<QrCodeRecord>> =
        dao.observeForProfile(profileId).map { codes -> codes.map { it.toDomain() } }

    suspend fun save(profileId: Long, draft: QrCodeDraft, id: Long? = null): QrCodeRecord {
        val normalizedDraft = draft.copy(
            title = draft.title.trim(),
            fields = draft.fields.map(String::trim)
        )
        val payload = when (val validation = QrCodePayload.validateAndEncode(normalizedDraft)) {
            is QrCodeValidation.Valid -> validation.payload
            is QrCodeValidation.Invalid -> throw IllegalArgumentException(validation.message)
        }
        val now = System.currentTimeMillis()
        return database.withTransaction {
            if (id == null) {
                val newId = dao.insert(normalizedDraft.toEntity(profileId, payload, now))
                dao.getById(newId, profileId)?.toDomain()
                    ?: error("The QR code was saved but could not be reloaded.")
            } else {
                val existing = dao.getById(id, profileId)
                    ?: throw IllegalArgumentException("This QR code is no longer available in the active profile.")
                val changed = dao.update(
                    id = id,
                    profileId = profileId,
                    title = normalizedDraft.title,
                    type = normalizedDraft.type.name,
                    fieldsJson = QrCodePayload.fieldsToJson(normalizedDraft.fields),
                    payload = payload,
                    updatedAt = now
                )
                check(changed == 1) { "The QR code could not be updated." }
                existing.copy(
                    title = normalizedDraft.title,
                    type = normalizedDraft.type.name,
                    fieldsJson = QrCodePayload.fieldsToJson(normalizedDraft.fields),
                    payload = payload,
                    updatedAt = now
                ).toDomain()
            }
        }
    }

    suspend fun importTemplates(
        profileId: Long,
        templates: List<QrCodeTemplate>
    ): QrCodeImportSummary {
        require(profileId > 0) { "Select a valid profile before importing QR codes." }
        require(templates.map(QrCodeTemplate::id).distinct().size == templates.size) {
            "The QR template bundle contains duplicate identifiers."
        }
        require(templates.all { it.id.isNotBlank() }) {
            "The QR template bundle contains an empty identifier."
        }
        val validated = templates.map { template ->
            val draft = template.toDraft().copy(
                title = template.title.trim(),
                fields = template.fields.map(String::trim)
            )
            val payload = when (val result = QrCodePayload.validateAndEncode(draft)) {
                is QrCodeValidation.Valid -> result.payload
                is QrCodeValidation.Invalid -> throw IllegalArgumentException(
                    "${template.title}: ${result.message}"
                )
            }
            template to (draft to payload)
        }
        val now = System.currentTimeMillis()
        return database.withTransaction {
            val existingIds = dao.getImportedTemplateIds(profileId).toSet()
            val newRecords = validated.filterNot { (template, _) -> template.id in existingIds }
            if (newRecords.isNotEmpty()) {
                dao.insertAll(newRecords.map { (template, draftAndPayload) ->
                    val (draft, payload) = draftAndPayload
                    draft.toEntity(profileId, payload, now).copy(sourceTemplateId = template.id)
                })
            }
            QrCodeImportSummary(
                imported = newRecords.size,
                alreadyPresent = validated.size - newRecords.size
            )
        }
    }

    suspend fun importTemplate(profileId: Long, template: QrCodeTemplate): QrCodeImportSummary =
        importTemplates(profileId, listOf(template))

    suspend fun delete(profileId: Long, id: Long) {
        check(dao.delete(id, profileId) == 1) {
            "This QR code is no longer available in the active profile."
        }
    }

    private fun QrCodeDraft.toEntity(profileId: Long, payload: String, now: Long) = QrCodeEntity(
        profileId = profileId,
        title = title.trim(),
        type = type.name,
        fieldsJson = QrCodePayload.fieldsToJson(fields),
        payload = payload,
        createdAt = now,
        updatedAt = now
    )

    private fun QrCodeEntity.toDomain(): QrCodeRecord {
        val codeType = QrCodeType.fromStoredValue(type)
        return QrCodeRecord(
            id = id,
            profileId = profileId,
            title = title,
            type = codeType,
            fields = QrCodePayload.fieldsFromJson(fieldsJson, codeType),
            payload = payload,
            sourceTemplateId = sourceTemplateId,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
