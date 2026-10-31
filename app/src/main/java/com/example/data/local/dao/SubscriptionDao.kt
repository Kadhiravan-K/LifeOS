package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.BillingOccurrenceEntity
import com.example.data.local.entities.PriceHistoryEntity
import com.example.data.local.entities.SubscriptionEntity
import com.example.data.local.entities.SubscriptionPaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionDao {

    // --- Subscription CRUD ---
    @Query("SELECT * FROM subscriptions WHERE status != 'ARCHIVED' ORDER BY updatedAt DESC")
    fun getActiveSubscriptionsFlow(): Flow<List<SubscriptionEntity>>

    @Query("SELECT * FROM subscriptions WHERE profileId = :profileId AND status != 'ARCHIVED' ORDER BY updatedAt DESC")
    fun getActiveSubscriptionsForProfileFlow(profileId: Long): Flow<List<SubscriptionEntity>>

    @Query("SELECT * FROM subscriptions ORDER BY status ASC, updatedAt DESC")
    fun getAllSubscriptionsFlow(): Flow<List<SubscriptionEntity>>

    @Query("SELECT * FROM subscriptions WHERE profileId = :profileId ORDER BY status ASC, updatedAt DESC")
    fun getAllSubscriptionsForProfileFlow(profileId: Long): Flow<List<SubscriptionEntity>>

    @Query("SELECT * FROM subscriptions WHERE id = :id LIMIT 1")
    suspend fun getSubscriptionById(id: Long): SubscriptionEntity?

    @Query("SELECT * FROM subscriptions ORDER BY id ASC")
    suspend fun getAllSubscriptionsList(): List<SubscriptionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(subscription: SubscriptionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscriptions(subscriptions: List<SubscriptionEntity>)

    @Update
    suspend fun updateSubscription(subscription: SubscriptionEntity)

    @Query("UPDATE subscriptions SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setSubscriptionStatus(id: Long, status: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM subscriptions WHERE id = :id")
    suspend fun deleteSubscriptionById(id: Long)

    @Query("DELETE FROM subscriptions WHERE profileId = :profileId")
    suspend fun deleteAllSubscriptionsForProfile(profileId: Long)

    @Query("UPDATE subscriptions SET projectId = NULL WHERE projectId = :projectId")
    suspend fun unassignSubscriptionsFromProject(projectId: Long)

    // --- Billing Occurrences (Bills) ---
    @Query("SELECT * FROM subscription_bills WHERE subscriptionId = :subscriptionId ORDER BY dueDateIso DESC")
    fun getBillsForSubscriptionFlow(subscriptionId: Long): Flow<List<BillingOccurrenceEntity>>

    @Query("SELECT * FROM subscription_bills WHERE subscriptionId = :subscriptionId ORDER BY dueDateIso DESC")
    suspend fun getBillsForSubscriptionList(subscriptionId: Long): List<BillingOccurrenceEntity>

    @Query("SELECT * FROM subscription_bills ORDER BY dueDateIso ASC")
    fun getAllBillsFlow(): Flow<List<BillingOccurrenceEntity>>

    @Query("SELECT * FROM subscription_bills ORDER BY id ASC")
    suspend fun getAllBillsList(): List<BillingOccurrenceEntity>

    @Query("SELECT * FROM subscription_bills WHERE id = :id LIMIT 1")
    suspend fun getBillById(id: Long): BillingOccurrenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: BillingOccurrenceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBills(bills: List<BillingOccurrenceEntity>)

    @Update
    suspend fun updateBill(bill: BillingOccurrenceEntity)

    @Query("UPDATE subscription_bills SET status = :status WHERE id = :id")
    suspend fun setBillStatus(id: Long, status: String)

    @Query("DELETE FROM subscription_bills WHERE id = :id")
    suspend fun deleteBillById(id: Long)

    @Query("DELETE FROM subscription_bills WHERE subscriptionId = :subscriptionId")
    suspend fun deleteBillsForSubscription(subscriptionId: Long)

    // --- Payments ---
    @Query("SELECT * FROM subscription_payments WHERE billId = :billId ORDER BY timestamp ASC")
    fun getPaymentsForBillFlow(billId: Long): Flow<List<SubscriptionPaymentEntity>>

    @Query("SELECT * FROM subscription_payments WHERE billId = :billId ORDER BY timestamp ASC")
    suspend fun getPaymentsForBillList(billId: Long): List<SubscriptionPaymentEntity>

    @Query("SELECT * FROM subscription_payments WHERE subscriptionId = :subscriptionId ORDER BY timestamp ASC")
    fun getPaymentsForSubscriptionFlow(subscriptionId: Long): Flow<List<SubscriptionPaymentEntity>>

    @Query("SELECT * FROM subscription_payments ORDER BY id ASC")
    suspend fun getAllPaymentsList(): List<SubscriptionPaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: SubscriptionPaymentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayments(payments: List<SubscriptionPaymentEntity>)

    @Query("DELETE FROM subscription_payments WHERE id = :id")
    suspend fun deletePaymentById(id: Long)

    // --- Price History ---
    @Query("SELECT * FROM subscription_price_history WHERE subscriptionId = :subscriptionId ORDER BY effectiveDateIso DESC")
    fun getPriceHistoryFlow(subscriptionId: Long): Flow<List<PriceHistoryEntity>>

    @Query("SELECT * FROM subscription_price_history ORDER BY id ASC")
    suspend fun getAllPriceHistoryList(): List<PriceHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPriceHistory(item: PriceHistoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPriceHistories(items: List<PriceHistoryEntity>)
}
