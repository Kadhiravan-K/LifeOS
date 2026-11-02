package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.dao.SubscriptionDao
import com.example.data.local.entities.BillingOccurrenceEntity
import com.example.data.local.entities.PriceHistoryEntity
import com.example.data.local.entities.SubscriptionEntity
import com.example.data.local.entities.SubscriptionPaymentEntity
import com.example.data.preferences.UserPreferences
import com.example.domain.subscription.BillStatus
import com.example.domain.subscription.BillingOccurrenceItem
import com.example.domain.subscription.PriceHistoryItem
import com.example.domain.subscription.SubscriptionItem
import com.example.domain.subscription.SubscriptionPaymentItem
import com.example.domain.subscription.SubscriptionScheduleEngine
import com.example.domain.subscription.SubscriptionStatus
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Dedicated repository for personal recurring subscriptions, billing occurrences,
 * payment logs, and price history (P7-A1 incremental architecture decomposition).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SubscriptionRepository(
    private val database: AppDatabase,
    private val preferences: UserPreferences,
    private val subscriptionDao: SubscriptionDao = database.subscriptionDao(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    val activeProfileIdFlow: Flow<Long> = preferences.activeProfileId
) {

    suspend fun getActiveProfileId(): Long = activeProfileIdFlow.first()

    val allSubscriptionsFlow: Flow<List<SubscriptionItem>> = activeProfileIdFlow.flatMapLatest { profileId ->
        subscriptionDao.getAllSubscriptionsForProfileFlow(profileId).map { list ->
            list.map { it.toDomain() }
        }
    }

    val activeSubscriptionsFlow: Flow<List<SubscriptionItem>> = activeProfileIdFlow.flatMapLatest { profileId ->
        subscriptionDao.getActiveSubscriptionsForProfileFlow(profileId).map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun getSubscriptionById(id: Long): SubscriptionItem? = withContext(ioDispatcher) {
        subscriptionDao.getSubscriptionById(id)?.toDomain()
    }

    suspend fun insertSubscription(subscription: SubscriptionItem): Long = withContext(ioDispatcher) {
        val effectiveProfileId = if (subscription.profileId > 0) subscription.profileId else getActiveProfileId()
        val safeSubscription = subscription.copy(profileId = effectiveProfileId)
        database.withTransaction {
            val subId = subscriptionDao.insertSubscription(SubscriptionEntity.fromDomain(safeSubscription))
            val initialBill = BillingOccurrenceEntity(
                subscriptionId = subId,
                dueDateIso = safeSubscription.nextRenewalDateIso.ifBlank { safeSubscription.anchorDateIso },
                expectedAmount = safeSubscription.amount,
                currency = safeSubscription.currency,
                status = BillStatus.UNPAID.name
            )
            subscriptionDao.insertBill(initialBill)
            subId
        }
    }

    suspend fun updateSubscription(subscription: SubscriptionItem) = withContext(ioDispatcher) {
        subscriptionDao.updateSubscription(SubscriptionEntity.fromDomain(subscription))
    }

    suspend fun setSubscriptionStatus(id: Long, status: SubscriptionStatus) = withContext(ioDispatcher) {
        subscriptionDao.setSubscriptionStatus(id, status.name)
    }

    suspend fun deleteSubscription(id: Long) = withContext(ioDispatcher) {
        database.withTransaction {
            subscriptionDao.deleteBillsForSubscription(id)
            subscriptionDao.deleteSubscriptionById(id)
        }
    }

    fun getBillsForSubscriptionFlow(subscriptionId: Long): Flow<List<BillingOccurrenceItem>> {
        return subscriptionDao.getBillsForSubscriptionFlow(subscriptionId).map { list ->
            list.map { it.toDomain() }
        }
    }

    val allBillsFlow: Flow<List<BillingOccurrenceItem>> = subscriptionDao.getAllBillsFlow().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun insertBill(bill: BillingOccurrenceItem): Long = withContext(ioDispatcher) {
        subscriptionDao.insertBill(BillingOccurrenceEntity.fromDomain(bill))
    }

    suspend fun updateBill(bill: BillingOccurrenceItem) = withContext(ioDispatcher) {
        subscriptionDao.updateBill(BillingOccurrenceEntity.fromDomain(bill))
    }

    suspend fun deleteBill(id: Long) = withContext(ioDispatcher) {
        subscriptionDao.deleteBillById(id)
    }

    fun getPaymentsForBillFlow(billId: Long): Flow<List<SubscriptionPaymentItem>> {
        return subscriptionDao.getPaymentsForBillFlow(billId).map { list ->
            list.map { it.toDomain() }
        }
    }

    fun getPaymentsForSubscriptionFlow(subscriptionId: Long): Flow<List<SubscriptionPaymentItem>> {
        return subscriptionDao.getPaymentsForSubscriptionFlow(subscriptionId).map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun recordBillPayment(payment: SubscriptionPaymentItem, todayIso: String): Long = withContext(ioDispatcher) {
        database.withTransaction {
            val payId = subscriptionDao.insertPayment(SubscriptionPaymentEntity.fromDomain(payment))
            val billEntity = subscriptionDao.getBillById(payment.billId)
            if (billEntity != null) {
                val allPayments = subscriptionDao.getPaymentsForBillList(payment.billId).map { it.toDomain() }
                val newStatus = SubscriptionScheduleEngine.evaluateBillStatus(
                    expectedAmount = billEntity.expectedAmount,
                    payments = allPayments,
                    dueDateIso = billEntity.dueDateIso,
                    todayIso = todayIso,
                    currentStatus = BillStatus.fromString(billEntity.status)
                )
                subscriptionDao.setBillStatus(billEntity.id, newStatus.name)
            }
            payId
        }
    }

    fun getPriceHistoryFlow(subscriptionId: Long): Flow<List<PriceHistoryItem>> {
        return subscriptionDao.getPriceHistoryFlow(subscriptionId).map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun updateSubscriptionPrice(
        subscriptionId: Long,
        newPrice: Double,
        effectiveDateIso: String,
        reason: String
    ) = withContext(ioDispatcher) {
        database.withTransaction {
            val sub = subscriptionDao.getSubscriptionById(subscriptionId)
            if (sub != null) {
                val oldPrice = sub.amount
                subscriptionDao.insertPriceHistory(
                    PriceHistoryEntity(
                        subscriptionId = subscriptionId,
                        oldAmount = oldPrice,
                        newAmount = newPrice,
                        effectiveDateIso = effectiveDateIso,
                        reason = reason
                    )
                )
                subscriptionDao.updateSubscription(sub.copy(amount = newPrice, updatedAt = System.currentTimeMillis()))
            }
        }
    }

    suspend fun deleteAllSubscriptionsForProfile(profileId: Long) = withContext(ioDispatcher) {
        subscriptionDao.deleteAllSubscriptionsForProfile(profileId)
    }

    suspend fun unassignSubscriptionsFromProject(projectId: Long) = withContext(ioDispatcher) {
        subscriptionDao.unassignSubscriptionsFromProject(projectId)
    }
}
