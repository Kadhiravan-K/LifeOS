package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entities.QrCodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QrCodeDao {
    @Query("SELECT * FROM qr_codes WHERE profileId = :profileId ORDER BY updatedAt DESC, id DESC")
    fun observeForProfile(profileId: Long): Flow<List<QrCodeEntity>>

    @Query("SELECT * FROM qr_codes WHERE id = :id AND profileId = :profileId LIMIT 1")
    suspend fun getById(id: Long, profileId: Long): QrCodeEntity?

    @Query("SELECT sourceTemplateId FROM qr_codes WHERE profileId = :profileId AND sourceTemplateId IS NOT NULL")
    suspend fun getImportedTemplateIds(profileId: Long): List<String>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(code: QrCodeEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(codes: List<QrCodeEntity>): List<Long>

    @Query("UPDATE qr_codes SET title = :title, type = :type, fieldsJson = :fieldsJson, payload = :payload, updatedAt = :updatedAt WHERE id = :id AND profileId = :profileId")
    suspend fun update(
        id: Long,
        profileId: Long,
        title: String,
        type: String,
        fieldsJson: String,
        payload: String,
        updatedAt: Long
    ): Int

    @Query("DELETE FROM qr_codes WHERE id = :id AND profileId = :profileId")
    suspend fun delete(id: Long, profileId: Long): Int

    @Query("SELECT * FROM qr_codes ORDER BY id ASC")
    suspend fun getAll(): List<QrCodeEntity>

    @Query("DELETE FROM qr_codes")
    suspend fun clearAll()
}
