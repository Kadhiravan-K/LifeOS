package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.TrackerRepository
import com.example.domain.tracker.CycleStatus
import com.example.domain.tracker.InventoryAdjustmentItem
import com.example.domain.tracker.TrackerItem
import com.example.domain.tracker.TrackerPurchaseItem
import com.example.domain.tracker.UsageCycleItem
import com.example.domain.tracker.UsageEntryItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Feature-scoped ViewModel for Smart Trackers, usage cycles, consumption logs,
 * purchases, and inventory adjustments (P7-A3 incremental architecture decomposition).
 */
class TrackerViewModel(
    private val repository: TrackerRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val allTrackers: StateFlow<List<TrackerItem>> = repository.allTrackersFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val activeTrackers: StateFlow<List<TrackerItem>> = repository.activeTrackersFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun clearError() {
        _errorMessage.value = null
    }

    fun saveTracker(tracker: TrackerItem, onComplete: ((Long) -> Unit)? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val id = if (tracker.id == 0L) {
                    repository.insertTracker(tracker)
                } else {
                    repository.updateTracker(tracker)
                    tracker.id
                }
                onComplete?.invoke(id)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to save tracker"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setTrackerArchived(trackerId: Long, isArchived: Boolean, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                repository.setTrackerArchived(trackerId, isArchived)
                onComplete?.invoke()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to update tracker archive state"
            }
        }
    }

    fun deleteTracker(trackerId: Long, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.deleteTracker(trackerId)
                onComplete?.invoke()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to delete tracker"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getCyclesForTracker(trackerId: Long): Flow<List<UsageCycleItem>> {
        return repository.getCyclesForTrackerFlow(trackerId)
    }

    fun saveCycle(cycle: UsageCycleItem, onComplete: ((Long) -> Unit)? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val id = if (cycle.id == 0L) {
                    repository.insertCycle(cycle)
                } else {
                    repository.updateCycle(cycle)
                    cycle.id
                }
                onComplete?.invoke(id)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to save cycle"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun finishCycle(
        cycle: UsageCycleItem,
        finishDateIso: String,
        finishOdometer: Double?,
        finalRemainingQuantity: Double?,
        notes: String = "",
        onComplete: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                val updated = cycle.copy(
                    status = CycleStatus.FINISHED,
                    finishDateIso = finishDateIso,
                    finishOdometer = finishOdometer ?: cycle.currentOdometer,
                    remainingQuantity = finalRemainingQuantity ?: cycle.remainingQuantity,
                    notes = if (notes.isNotBlank()) "${cycle.notes}\n$notes".trim() else cycle.notes,
                    updatedAt = System.currentTimeMillis()
                )
                repository.updateCycle(updated)
                onComplete?.invoke()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to finish cycle"
            }
        }
    }

    fun pauseResumeCycle(cycle: UsageCycleItem, isPaused: Boolean, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val updated = cycle.copy(
                    status = if (isPaused) CycleStatus.PAUSED else CycleStatus.ACTIVE,
                    isPaused = isPaused,
                    updatedAt = System.currentTimeMillis()
                )
                repository.updateCycle(updated)
                onComplete?.invoke()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to update cycle pause state"
            }
        }
    }

    fun logUsageEntry(entry: UsageEntryItem, cycle: UsageCycleItem, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                repository.insertUsageEntry(entry)
                var newOdometer = cycle.currentOdometer
                if (entry.odometerReading != null && entry.odometerReading > newOdometer) {
                    newOdometer = entry.odometerReading
                }
                var newRemaining = cycle.remainingQuantity
                if (entry.manualRemainingQuantity != null) {
                    newRemaining = entry.manualRemainingQuantity
                } else if (entry.quantityDeducted != null) {
                    newRemaining = (newRemaining - entry.quantityDeducted).coerceAtLeast(0.0)
                }
                repository.updateCycle(
                    cycle.copy(
                        currentOdometer = newOdometer,
                        remainingQuantity = newRemaining,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                onComplete?.invoke()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to log usage entry"
            }
        }
    }

    fun getPurchasesForTracker(trackerId: Long): Flow<List<TrackerPurchaseItem>> {
        return repository.getPurchasesForTrackerFlow(trackerId)
    }

    fun recordPurchase(purchase: TrackerPurchaseItem, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                repository.insertPurchase(purchase)
                onComplete?.invoke()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to record purchase"
            }
        }
    }

    fun getAdjustmentsForTracker(trackerId: Long): Flow<List<InventoryAdjustmentItem>> {
        return repository.getAdjustmentsForTrackerFlow(trackerId)
    }

    fun recordStockAdjustment(adjustment: InventoryAdjustmentItem, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                repository.insertAdjustment(adjustment)
                onComplete?.invoke()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to record stock adjustment"
            }
        }
    }
}

/**
 * Factory for creating [TrackerViewModel] instances with a [TrackerRepository].
 */
class TrackerViewModelFactory(
    private val repository: TrackerRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TrackerViewModel::class.java)) {
            return TrackerViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
