package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entities.GiveGetTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GiveGetDao {

    @Query("SELECT * FROM give_get_transactions WHERE profileId = :profileId ORDER BY dateIso DESC, id DESC")
    fun getAllTransactions(profileId: Long): Flow<List<GiveGetTransactionEntity>>

    @Query("SELECT * FROM give_get_transactions WHERE profileId = :profileId ORDER BY dateIso DESC, id DESC")
    suspend fun getAllTransactionsDirect(profileId: Long): List<GiveGetTransactionEntity>

    @Query("SELECT * FROM give_get_transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): GiveGetTransactionEntity?

    @Query("SELECT * FROM give_get_transactions WHERE profileId = :profileId AND status = :status ORDER BY dateIso DESC")
    fun getTransactionsByStatus(profileId: Long, status: String): Flow<List<GiveGetTransactionEntity>>

    @Query("SELECT * FROM give_get_transactions WHERE profileId = :profileId AND personName = :personName ORDER BY dateIso DESC")
    fun getTransactionsForPerson(profileId: Long, personName: String): Flow<List<GiveGetTransactionEntity>>

    @Query("SELECT DISTINCT personName FROM give_get_transactions WHERE profileId = :profileId ORDER BY personName ASC")
    fun getDistinctPersons(profileId: Long): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: GiveGetTransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: GiveGetTransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: GiveGetTransactionEntity)

    @Query("DELETE FROM give_get_transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("UPDATE give_get_transactions SET status = :status, actualReturnDateIso = :actualReturnDateIso, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, actualReturnDateIso: String?, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE give_get_transactions SET returnedAmount = :returnedAmount, status = :status, actualReturnDateIso = :actualReturnDateIso, updatedAt = :updatedAt WHERE id = :id")
    suspend fun recordRepayment(id: Long, returnedAmount: Double, status: String, actualReturnDateIso: String?, updatedAt: Long = System.currentTimeMillis())
}
