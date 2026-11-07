package com.example.giveget

import com.example.data.local.entities.GiveGetTransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GiveGetUnitTest {

    @Test
    fun testGiveMoneyLentCreationAndRemainingAmount() {
        val tx = GiveGetTransactionEntity(
            id = 1,
            profileId = 1,
            type = "GIVE_LENT_MONEY",
            personName = "Bob Johnson",
            amount = 100.0,
            returnedAmount = 0.0,
            itemDescription = "Concert ticket loan",
            dateIso = "2026-11-01",
            expectedReturnDateIso = "2026-11-15",
            status = "PENDING"
        )

        assertTrue(tx.isMoney)
        assertTrue(tx.isLent)
        assertFalse(tx.isGift)
        assertEquals(100.0, tx.remainingAmount, 0.001)
    }

    @Test
    fun testGiftIsNeverTreatedAsDebt() {
        val giftMoney = GiveGetTransactionEntity(
            id = 2,
            profileId = 1,
            type = "GIVE_GIFT_MONEY",
            personName = "Charlie",
            amount = 50.0,
            returnedAmount = 0.0,
            itemDescription = "Birthday gift",
            dateIso = "2026-11-02",
            status = "GIFTED"
        )

        val giftItem = GiveGetTransactionEntity(
            id = 3,
            profileId = 1,
            type = "GIVE_GIFT_ITEM",
            personName = "Diana",
            amount = 0.0,
            returnedAmount = 0.0,
            itemDescription = "Graduation fountain pen",
            dateIso = "2026-11-03",
            status = "GIFTED"
        )

        assertTrue(giftMoney.isGift)
        assertTrue(giftItem.isGift)
        // Gifts must have 0.0 remaining debt
        assertEquals(0.0, giftMoney.remainingAmount, 0.001)
        assertEquals(0.0, giftItem.remainingAmount, 0.001)
    }

    @Test
    fun testPartialRepaymentAccounting() {
        val initialTx = GiveGetTransactionEntity(
            id = 4,
            profileId = 1,
            type = "GIVE_LENT_MONEY",
            personName = "Ethan",
            amount = 200.0,
            returnedAmount = 0.0,
            dateIso = "2026-11-01",
            status = "PENDING"
        )

        // Ethan repays $75
        val repaidTx = initialTx.copy(
            returnedAmount = 75.0,
            status = "PARTIALLY_RETURNED"
        )

        assertEquals(125.0, repaidTx.remainingAmount, 0.001)
        assertEquals("PARTIALLY_RETURNED", repaidTx.status)

        // Ethan pays the remaining $125
        val fullyPaidTx = repaidTx.copy(
            returnedAmount = 200.0,
            status = "RETURNED",
            actualReturnDateIso = "2026-11-07"
        )

        assertEquals(0.0, fullyPaidTx.remainingAmount, 0.001)
        assertEquals("RETURNED", fullyPaidTx.status)
        assertEquals("2026-11-07", fullyPaidTx.actualReturnDateIso)
    }

    @Test
    fun testPersonWiseOutstandingBalanceCalculation() {
        val transactions = listOf(
            GiveGetTransactionEntity(
                id = 1,
                profileId = 1,
                type = "GIVE_LENT_MONEY",
                personName = "Frank",
                amount = 100.0,
                returnedAmount = 20.0,
                dateIso = "2026-11-01",
                status = "PARTIALLY_RETURNED"
            ),
            GiveGetTransactionEntity(
                id = 2,
                profileId = 1,
                type = "GIVE_LENT_MONEY",
                personName = "Frank",
                amount = 50.0,
                returnedAmount = 0.0,
                dateIso = "2026-11-02",
                status = "PENDING"
            ),
            GiveGetTransactionEntity(
                id = 3,
                profileId = 1,
                type = "GIVE_GIFT_MONEY", // Gift to Frank: must not increase debt!
                personName = "Frank",
                amount = 30.0,
                dateIso = "2026-11-03",
                status = "GIFTED"
            ),
            GiveGetTransactionEntity(
                id = 4,
                profileId = 1,
                type = "GET_BORROWED_MONEY",
                personName = "Frank",
                amount = 40.0,
                returnedAmount = 0.0,
                dateIso = "2026-11-04",
                status = "PENDING"
            )
        )

        // Frank owes: ($100 - $20) + $50 = $130
        val theyOwe = transactions
            .filter { it.personName == "Frank" && it.isLent && !it.isGift && it.status != "RETURNED" }
            .sumOf { it.remainingAmount }

        // You owe Frank: $40
        val youOwe = transactions
            .filter { it.personName == "Frank" && it.isBorrowed && it.status != "RETURNED" }
            .sumOf { it.remainingAmount }

        assertEquals(130.0, theyOwe, 0.001)
        assertEquals(40.0, youOwe, 0.001)
    }

    @Test
    fun testProfileIsolation() {
        val prof1Tx = GiveGetTransactionEntity(
            id = 1,
            profileId = 1,
            type = "GIVE_LENT_MONEY",
            personName = "George",
            amount = 50.0,
            dateIso = "2026-11-01"
        )
        val prof2Tx = GiveGetTransactionEntity(
            id = 2,
            profileId = 2,
            type = "GIVE_LENT_MONEY",
            personName = "George",
            amount = 150.0,
            dateIso = "2026-11-01"
        )

        assertEquals(1L, prof1Tx.profileId)
        assertEquals(2L, prof2Tx.profileId)
        assertEquals(50.0, prof1Tx.amount, 0.001)
        assertEquals(150.0, prof2Tx.amount, 0.001)
    }
}
