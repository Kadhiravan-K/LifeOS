package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entities.ShiftAssignmentEntity
import com.example.data.local.entities.ShiftEntity
import com.example.data.local.entities.ShiftRotationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShiftDao {

    // --- Shifts ---
    @Query("SELECT * FROM shifts WHERE profileId = :profileId ORDER BY startTime ASC")
    fun getShiftsForProfile(profileId: Long): Flow<List<ShiftEntity>>

    @Query("SELECT * FROM shifts WHERE profileId = :profileId ORDER BY startTime ASC")
    suspend fun getShiftsForProfileDirect(profileId: Long): List<ShiftEntity>

    @Query("SELECT * FROM shifts WHERE id = :id LIMIT 1")
    suspend fun getShiftById(id: Long): ShiftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShift(shift: ShiftEntity): Long

    @Update
    suspend fun updateShift(shift: ShiftEntity)

    @Delete
    suspend fun deleteShift(shift: ShiftEntity)

    @Query("DELETE FROM shifts WHERE id = :id")
    suspend fun deleteShiftById(id: Long)

    // --- Rotations ---
    @Query("SELECT * FROM shift_rotations WHERE profileId = :profileId AND isActive = 1 LIMIT 1")
    fun getActiveRotation(profileId: Long): Flow<ShiftRotationEntity?>

    @Query("SELECT * FROM shift_rotations WHERE profileId = :profileId AND isActive = 1 LIMIT 1")
    suspend fun getActiveRotationDirect(profileId: Long): ShiftRotationEntity?

    @Query("SELECT * FROM shift_rotations WHERE profileId = :profileId ORDER BY createdAt DESC")
    fun getAllRotationsForProfile(profileId: Long): Flow<List<ShiftRotationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRotation(rotation: ShiftRotationEntity): Long

    @Update
    suspend fun updateRotation(rotation: ShiftRotationEntity)

    @Query("UPDATE shift_rotations SET isActive = 0 WHERE profileId = :profileId")
    suspend fun deactivateAllRotationsForProfile(profileId: Long)

    @Delete
    suspend fun deleteRotation(rotation: ShiftRotationEntity)

    // --- Assignments & Exceptions ---
    @Query("SELECT * FROM shift_assignments WHERE profileId = :profileId AND dateIso = :dateIso LIMIT 1")
    fun getAssignmentForDate(profileId: Long, dateIso: String): Flow<ShiftAssignmentEntity?>

    @Query("SELECT * FROM shift_assignments WHERE profileId = :profileId AND dateIso = :dateIso LIMIT 1")
    suspend fun getAssignmentForDateDirect(profileId: Long, dateIso: String): ShiftAssignmentEntity?

    @Query("SELECT * FROM shift_assignments WHERE profileId = :profileId AND dateIso >= :startDateIso AND dateIso <= :endDateIso ORDER BY dateIso ASC")
    fun getAssignmentsInRange(profileId: Long, startDateIso: String, endDateIso: String): Flow<List<ShiftAssignmentEntity>>

    @Query("SELECT * FROM shift_assignments WHERE profileId = :profileId AND dateIso >= :startDateIso AND dateIso <= :endDateIso ORDER BY dateIso ASC")
    suspend fun getAssignmentsInRangeDirect(profileId: Long, startDateIso: String, endDateIso: String): List<ShiftAssignmentEntity>

    @Query("SELECT * FROM shift_assignments WHERE profileId = :profileId ORDER BY dateIso DESC")
    fun getAllAssignmentsForProfile(profileId: Long): Flow<List<ShiftAssignmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAssignment(assignment: ShiftAssignmentEntity): Long

    @Delete
    suspend fun deleteAssignment(assignment: ShiftAssignmentEntity)

    @Query("DELETE FROM shift_assignments WHERE profileId = :profileId AND dateIso = :dateIso")
    suspend fun deleteAssignmentForDate(profileId: Long, dateIso: String)
}
