package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.SubscriptionRepository
import com.example.domain.subscription.BillStatus
import com.example.domain.subscription.BillingOccurrenceItem
import com.example.domain.subscription.PriceHistoryItem
import com.example.domain.subscription.SubscriptionItem
import com.example.domain.subscription.SubscriptionPaymentItem
import com.example.domain.subscription.SubscriptionStatus
import com.example.utils.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Feature-scoped ViewModel for personal recurring subscriptions, billing occurrences,
 * payment logs, and price history (P7-A2 incremental architecture decomposition).
 */
class SubscriptionViewModel(
    private val repository: SubscriptionRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val allSubscriptions: StateFlow<List<SubscriptionItem>> = repository.allSubscriptionsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val activeSubscriptions: StateFlow<List<SubscriptionItem>> = repository.activeSubscriptionsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allBills: StateFlow<List<BillingOccurrenceItem>> = repository.allBillsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun clearError() {
        _errorMessage.value = null
    }

    fun saveSubscription(subscription: SubscriptionItem, onComplete: ((Long) -> Unit)? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val id = if (subscription.id == 0L) {
                    repository.insertSubscription(subscription)
                } else {
                    repository.updateSubscription(subscription)
                    subscription.id
                }
                onComplete?.invoke(id)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to save subscription"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setSubscriptionStatus(subscriptionId: Long, status: SubscriptionStatus) {
        viewModelScope.launch {
            try {
                repository.setSubscriptionStatus(subscriptionId, status)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to update subscription status"
            }
        }
    }

    fun deleteSubscription(subscriptionId: Long, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.deleteSubscription(subscriptionId)
                onComplete?.invoke()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to delete subscription"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getBillsForSubscription(subscriptionId: Long): Flow<List<BillingOccurrenceItem>> {
        return repository.getBillsForSubscriptionFlow(subscriptionId)
    }

    fun getPaymentsForBill(billId: Long): Flow<List<SubscriptionPaymentItem>> {
        return repository.getPaymentsForBillFlow(billId)
    }

    fun getPaymentsForSubscription(subscriptionId: Long): Flow<List<SubscriptionPaymentItem>> {
        return repository.getPaymentsForSubscriptionFlow(subscriptionId)
    }

    fun recordBillPayment(
        billId: Long,
        subscriptionId: Long,
        amountPaid: Double,
        paymentDateIso: String,
        paymentMethod: String = "",
        transactionRef: String = "",
        notes: String = ""
    ) {
        viewModelScope.launch {
            try {
                val payment = SubscriptionPaymentItem(
                    billId = billId,
                    subscriptionId = subscriptionId,
                    amountPaid = amountPaid,
                    paymentDateIso = paymentDateIso,
                    paymentMethod = paymentMethod,
                    transactionRef = transactionRef,
                    notes = notes
                )
                repository.recordBillPayment(payment, DateUtils.todayString())
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to record payment"
            }
        }
    }

    fun updateBillStatus(bill: BillingOccurrenceItem, status: BillStatus) {
        viewModelScope.launch {
            try {
                repository.updateBill(bill.copy(status = status))
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to update bill status"
            }
        }
    }

    fun updateBillAmount(bill: BillingOccurrenceItem, newExpectedAmount: Double) {
        viewModelScope.launch {
            try {
                repository.updateBill(bill.copy(expectedAmount = newExpectedAmount))
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to update bill amount"
            }
        }
    }

    fun updateSubscriptionPrice(
        subscriptionId: Long,
        newPrice: Double,
        effectiveDateIso: String,
        reason: String
    ) {
        viewModelScope.launch {
            try {
                repository.updateSubscriptionPrice(subscriptionId, newPrice, effectiveDateIso, reason)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to update subscription price"
            }
        }
    }

    fun getPriceHistory(subscriptionId: Long): Flow<List<PriceHistoryItem>> {
        return repository.getPriceHistoryFlow(subscriptionId)
    }
}

/**
 * Factory for creating [SubscriptionViewModel] instances with a [SubscriptionRepository].
 */
class SubscriptionViewModelFactory(
    private val repository: SubscriptionRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SubscriptionViewModel::class.java)) {
            return SubscriptionViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
