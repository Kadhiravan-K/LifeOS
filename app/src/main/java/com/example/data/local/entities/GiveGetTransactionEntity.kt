package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Give & Get transaction entity for contact-based lending, borrowing, and gifting.
 *
 * Strict policy:
 * - Gifting is cleanly distinguished from lending/borrowing and never treated as an outstanding debt.
 */
@Entity(
    tableName = "give_get_transactions",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("profileId"),
        Index("personName"),
        Index("status"),
        Index("dateIso")
    ]
)
data class GiveGetTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val profileId: Long,
    val type: String,                   // GIVE_LENT_MONEY, GIVE_LENT_ITEM, GIVE_GIFT_ITEM, GIVE_GIFT_MONEY, GET_BORROWED_MONEY, GET_BORROWED_ITEM
    val personName: String,
    val personContactId: String? = null,
    val personPhoneNumber: String? = null,
    val amount: Double = 0.0,           // Monetary value; 0.0 for physical items
    val returnedAmount: Double = 0.0,   // Partially returned amount for money transactions
    val itemDescription: String = "",   // Item name or money description
    val dateIso: String,                // "YYYY-MM-DD"
    val expectedReturnDateIso: String? = null,
    val actualReturnDateIso: String? = null,
    val status: String = "PENDING",     // PENDING, PARTIALLY_RETURNED, RETURNED, GIFTED, OVERDUE, CANCELLED
    val notes: String = "",
    val hasReminder: Boolean = false,
    val reminderDaysBefore: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isMoney: Boolean
        get() = type == "GIVE_LENT_MONEY" || type == "GIVE_GIFT_MONEY" || type == "GET_BORROWED_MONEY"

    val isGift: Boolean
        get() = type == "GIVE_GIFT_ITEM" || type == "GIVE_GIFT_MONEY"

    val isLent: Boolean
        get() = type == "GIVE_LENT_MONEY" || type == "GIVE_LENT_ITEM"

    val isBorrowed: Boolean
        get() = type == "GET_BORROWED_MONEY" || type == "GET_BORROWED_ITEM"

    val remainingAmount: Double
        get() = if (isMoney && !isGift) (amount - returnedAmount).coerceAtLeast(0.0) else 0.0
}
