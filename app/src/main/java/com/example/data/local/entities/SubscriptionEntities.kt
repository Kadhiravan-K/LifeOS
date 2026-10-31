package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.subscription.BillingFrequency
import com.example.domain.subscription.BillingOccurrenceItem
import com.example.domain.subscription.BillStatus
import com.example.domain.subscription.CustomIntervalUnit
import com.example.domain.subscription.MissingDatePolicy
import com.example.domain.subscription.PriceHistoryItem
import com.example.domain.subscription.SubscriptionItem
import com.example.domain.subscription.SubscriptionPaymentItem
import com.example.domain.subscription.SubscriptionStatus
import org.json.JSONArray

@Entity(
    tableName = "subscriptions",
    indices = [
        Index("profileId"),
        Index("projectId"),
        Index("status")
    ]
)
data class SubscriptionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val profileId: Long = 1,
    val title: String,
    val emoji: String = "💳",
    val description: String = "",
    val projectId: Long? = null,
    val labelIdsJson: String = "[]",
    val customColorHex: Long? = null,
    val amount: Double,
    val currency: String = "₹",
    val frequency: String = "MONTHLY",
    val customIntervalCount: Int = 1,
    val customIntervalUnit: String = "MONTHS",
    val anchorDateIso: String,
    val anchorDayOfMonth: Int = 1,
    val nextRenewalDateIso: String,
    val missingDatePolicy: String = "LAST_VALID_DAY",
    val autoRenew: Boolean = true,
    val paymentMethod: String = "",
    val isTrial: Boolean = false,
    val trialStartDateIso: String? = null,
    val trialEndDateIso: String? = null,
    val status: String = "ACTIVE",
    val cancellationDateIso: String? = null,
    val cancellationReason: String = "",
    val reminderDaysBefore: Int = 3,
    val overdueReminderEnabled: Boolean = true,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): SubscriptionItem {
        val labels = try {
            val arr = JSONArray(labelIdsJson)
            val list = mutableListOf<Long>()
            for (i in 0 until arr.length()) {
                list.add(arr.getLong(i))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }

        return SubscriptionItem(
            id = id,
            profileId = profileId,
            title = title,
            emoji = emoji,
            description = description,
            projectId = projectId,
            labelIds = labels,
            customColorHex = customColorHex,
            amount = amount,
            currency = currency,
            frequency = BillingFrequency.fromString(frequency),
            customIntervalCount = customIntervalCount,
            customIntervalUnit = CustomIntervalUnit.fromString(customIntervalUnit),
            anchorDateIso = anchorDateIso,
            anchorDayOfMonth = anchorDayOfMonth,
            nextRenewalDateIso = nextRenewalDateIso,
            missingDatePolicy = MissingDatePolicy.fromString(missingDatePolicy),
            autoRenew = autoRenew,
            paymentMethod = paymentMethod,
            isTrial = isTrial,
            trialStartDateIso = trialStartDateIso,
            trialEndDateIso = trialEndDateIso,
            status = SubscriptionStatus.fromString(status),
            cancellationDateIso = cancellationDateIso,
            cancellationReason = cancellationReason,
            reminderDaysBefore = reminderDaysBefore,
            overdueReminderEnabled = overdueReminderEnabled,
            notes = notes,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDomain(item: SubscriptionItem): SubscriptionEntity {
            val labelsJson = JSONArray().apply {
                item.labelIds.forEach { put(it) }
            }.toString()

            return SubscriptionEntity(
                id = item.id,
                profileId = item.profileId,
                title = item.title,
                emoji = item.emoji,
                description = item.description,
                projectId = item.projectId,
                labelIdsJson = labelsJson,
                customColorHex = item.customColorHex,
                amount = item.amount,
                currency = item.currency,
                frequency = item.frequency.name,
                customIntervalCount = item.customIntervalCount,
                customIntervalUnit = item.customIntervalUnit.name,
                anchorDateIso = item.anchorDateIso,
                anchorDayOfMonth = item.anchorDayOfMonth,
                nextRenewalDateIso = item.nextRenewalDateIso,
                missingDatePolicy = item.missingDatePolicy.name,
                autoRenew = item.autoRenew,
                paymentMethod = item.paymentMethod,
                isTrial = item.isTrial,
                trialStartDateIso = item.trialStartDateIso,
                trialEndDateIso = item.trialEndDateIso,
                status = item.status.name,
                cancellationDateIso = item.cancellationDateIso,
                cancellationReason = item.cancellationReason,
                reminderDaysBefore = item.reminderDaysBefore,
                overdueReminderEnabled = item.overdueReminderEnabled,
                notes = item.notes,
                createdAt = item.createdAt,
                updatedAt = item.updatedAt
            )
        }
    }
}

@Entity(
    tableName = "subscription_bills",
    indices = [
        Index("subscriptionId"),
        Index("dueDateIso"),
        Index("status")
    ]
)
data class BillingOccurrenceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subscriptionId: Long,
    val dueDateIso: String,
    val expectedAmount: Double,
    val currency: String = "₹",
    val status: String = "UNPAID",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): BillingOccurrenceItem {
        return BillingOccurrenceItem(
            id = id,
            subscriptionId = subscriptionId,
            dueDateIso = dueDateIso,
            expectedAmount = expectedAmount,
            currency = currency,
            status = BillStatus.fromString(status),
            notes = notes,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromDomain(item: BillingOccurrenceItem): BillingOccurrenceEntity {
            return BillingOccurrenceEntity(
                id = item.id,
                subscriptionId = item.subscriptionId,
                dueDateIso = item.dueDateIso,
                expectedAmount = item.expectedAmount,
                currency = item.currency,
                status = item.status.name,
                notes = item.notes,
                createdAt = item.createdAt
            )
        }
    }
}

@Entity(
    tableName = "subscription_payments",
    indices = [
        Index("billId"),
        Index("subscriptionId")
    ]
)
data class SubscriptionPaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val billId: Long,
    val subscriptionId: Long,
    val amountPaid: Double,
    val paymentDateIso: String,
    val paymentMethod: String = "",
    val transactionRef: String = "",
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toDomain() = SubscriptionPaymentItem(id, billId, subscriptionId, amountPaid, paymentDateIso, paymentMethod, transactionRef, notes, timestamp)

    companion object {
        fun fromDomain(item: SubscriptionPaymentItem) = SubscriptionPaymentEntity(
            item.id, item.billId, item.subscriptionId, item.amountPaid, item.paymentDateIso, item.paymentMethod, item.transactionRef, item.notes, item.timestamp
        )
    }
}

@Entity(
    tableName = "subscription_price_history",
    indices = [
        Index("subscriptionId")
    ]
)
data class PriceHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subscriptionId: Long,
    val oldAmount: Double,
    val newAmount: Double,
    val effectiveDateIso: String,
    val reason: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain() = PriceHistoryItem(id, subscriptionId, oldAmount, newAmount, effectiveDateIso, reason, createdAt)

    companion object {
        fun fromDomain(item: PriceHistoryItem) = PriceHistoryEntity(
            item.id, item.subscriptionId, item.oldAmount, item.newAmount, item.effectiveDateIso, item.reason, item.createdAt
        )
    }
}
