package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.MedicationDoseEntity
import com.example.data.local.entities.MedicationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {

    // --- Medication CRUD ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedication(medication: MedicationEntity): Long

    @Update
    suspend fun updateMedication(medication: MedicationEntity)

    @Delete
    suspend fun deleteMedication(medication: MedicationEntity)

    @Query("DELETE FROM medications WHERE taskId = :taskId")
    suspend fun deleteMedicationByTaskId(taskId: Long)

    @Query("SELECT * FROM medications WHERE id = :id LIMIT 1")
    suspend fun getMedicationById(id: Long): MedicationEntity?

    @Query("SELECT * FROM medications WHERE taskId = :taskId LIMIT 1")
    suspend fun getMedicationByTaskId(taskId: Long): MedicationEntity?

    @Query("SELECT * FROM medications WHERE taskId = :taskId LIMIT 1")
    fun getMedicationByTaskIdFlow(taskId: Long): Flow<MedicationEntity?>

    @Query("SELECT * FROM medications ORDER BY createdAt DESC")
    fun getAllMedicationsFlow(): Flow<List<MedicationEntity>>

    @Query("SELECT * FROM medications WHERE isPaused = 0")
    suspend fun getActiveMedications(): List<MedicationEntity>

    // --- Dose Occurrences ---

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDose(dose: MedicationDoseEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDoses(doses: List<MedicationDoseEntity>): List<Long>

    @Update
    suspend fun updateDose(dose: MedicationDoseEntity)

    @Query("SELECT * FROM medication_doses WHERE id = :id LIMIT 1")
    suspend fun getDoseById(id: Long): MedicationDoseEntity?

    @Query("SELECT * FROM medication_doses WHERE scheduledDateIso = :dateIso ORDER BY scheduledTime ASC")
    fun getDosesForDateFlow(dateIso: String): Flow<List<MedicationDoseEntity>>

    @Query("SELECT * FROM medication_doses WHERE scheduledDateIso = :dateIso ORDER BY scheduledTime ASC")
    suspend fun getDosesForDate(dateIso: String): List<MedicationDoseEntity>

    @Query("SELECT * FROM medication_doses WHERE medicationId = :medicationId ORDER BY scheduledDateIso ASC, scheduledTime ASC")
    fun getDosesForMedicationFlow(medicationId: Long): Flow<List<MedicationDoseEntity>>

    @Query("SELECT * FROM medication_doses WHERE medicationId = :medicationId ORDER BY scheduledDateIso ASC, scheduledTime ASC")
    suspend fun getDosesForMedication(medicationId: Long): List<MedicationDoseEntity>

    @Query("UPDATE medication_doses SET status = :status, actionTimestamp = :timestamp, notes = :notes WHERE id = :id")
    suspend fun updateDoseStatus(id: Long, status: String, timestamp: Long, notes: String = "")

    @Query("UPDATE medication_doses SET snoozeCount = snoozeCount + 1, status = 'SNOOZED' WHERE id = :id")
    suspend fun incrementSnoozeCount(id: Long)

    @Query("UPDATE medication_doses SET followUpCount = followUpCount + 1 WHERE id = :id")
    suspend fun incrementFollowUpCount(id: Long)

    @Query("DELETE FROM medication_doses WHERE medicationId = :medicationId AND scheduledDateIso >= :fromDateIso AND status = 'SCHEDULED'")
    suspend fun deleteFutureScheduledDoses(medicationId: Long, fromDateIso: String)

    @Query("DELETE FROM medication_doses WHERE taskId = :taskId")
    suspend fun deleteDosesByTaskId(taskId: Long)

    @Query("SELECT COUNT(*) FROM medication_doses WHERE scheduledDateIso = :dateIso AND status = 'TAKEN'")
    suspend fun getTakenDoseCountForDate(dateIso: String): Int

    @Query("SELECT COUNT(*) FROM medication_doses WHERE scheduledDateIso = :dateIso")
    suspend fun getTotalDoseCountForDate(dateIso: String): Int
}
