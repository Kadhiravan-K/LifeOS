package com.example.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.preferences.UserPreferences
import com.example.domain.subscription.BillStatus
import com.example.domain.subscription.BillingFrequency
import com.example.domain.subscription.BillingOccurrenceItem
import com.example.domain.subscription.MissingDatePolicy
import com.example.domain.subscription.PriceHistoryItem
import com.example.domain.subscription.SubscriptionItem
import com.example.domain.subscription.SubscriptionPaymentItem
import com.example.domain.subscription.SubscriptionStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SubscriptionRepositoryUnitTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var preferences: UserPreferences
    private lateinit var subscriptionRepository: SubscriptionRepository
    private lateinit var lifeRepository: LifeRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        preferences = UserPreferences(context)
        subscriptionRepository = SubscriptionRepository(database, preferences)
        lifeRepository = LifeRepository(
            database = database,
            preferences = preferences,
            subscriptionRepository = subscriptionRepository
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun createSampleSubscription(title: String = "Streaming Service", amount: Double = 14.99): SubscriptionItem {
        return SubscriptionItem(
            profileId = 1L,
            title = title,
            description = "Monthly streaming",
            amount = amount,
            currency = "USD",
            frequency = BillingFrequency.MONTHLY,
            anchorDateIso = "2026-01-15",
            anchorDayOfMonth = 15,
            missingDatePolicy = MissingDatePolicy.LAST_VALID_DAY,
            nextRenewalDateIso = "2026-02-15",
            status = SubscriptionStatus.ACTIVE
        )
    }

    @Test
    fun insertSubscription_createsSubscriptionAndInitialBill_standaloneRepository() = runBlocking {
        val sample = createSampleSubscription()
        val id = subscriptionRepository.insertSubscription(sample)
        assertTrue(id > 0)

        val retrieved = subscriptionRepository.getSubscriptionById(id)
        assertNotNull(retrieved)
        assertEquals("Streaming Service", retrieved?.title)
        assertEquals(14.99, retrieved?.amount ?: 0.0, 0.001)

        val bills = subscriptionRepository.getBillsForSubscriptionFlow(id).first()
        assertEquals(1, bills.size)
        assertEquals("2026-02-15", bills[0].dueDateIso)
        assertEquals(14.99, bills[0].expectedAmount, 0.001)
        assertEquals(BillStatus.UNPAID, bills[0].status)
    }

    @Test
    fun lifeRepositoryFacade_delegatesToSubscriptionRepositorySeamlessly() = runBlocking {
        val sample = createSampleSubscription(title = "Cloud Storage", amount = 2.99)
        // Insert via LifeRepository facade
        val id = lifeRepository.insertSubscription(sample)
        assertTrue(id > 0)

        // Read via both facade and standalone repository
        val fromFacade = lifeRepository.getSubscriptionById(id)
        val fromExtracted = subscriptionRepository.getSubscriptionById(id)

        assertNotNull(fromFacade)
        assertNotNull(fromExtracted)
        assertEquals(fromExtracted?.title, fromFacade?.title)
        assertEquals(2.99, fromFacade?.amount ?: 0.0, 0.001)

        // Verify status change through facade reflects in extracted repository
        lifeRepository.setSubscriptionStatus(id, SubscriptionStatus.PAUSED)
        val paused = subscriptionRepository.getSubscriptionById(id)
        assertEquals(SubscriptionStatus.PAUSED, paused?.status)
    }

    @Test
    fun recordBillPayment_updatesBillStatusAtomically() = runBlocking {
        val sample = createSampleSubscription(title = "Gym Membership", amount = 50.00)
        val subId = subscriptionRepository.insertSubscription(sample)
        val bills = subscriptionRepository.getBillsForSubscriptionFlow(subId).first()
        val billId = bills[0].id

        val payment = SubscriptionPaymentItem(
            billId = billId,
            subscriptionId = subId,
            amountPaid = 50.00,
            paymentDateIso = "2026-02-14",
            paymentMethod = "Credit Card",
            notes = "Auto-debit"
        )

        val payId = subscriptionRepository.recordBillPayment(payment, todayIso = "2026-02-14")
        assertTrue(payId > 0)

        val updatedBills = subscriptionRepository.getBillsForSubscriptionFlow(subId).first()
        assertEquals(BillStatus.PAID, updatedBills[0].status)

        val payments = subscriptionRepository.getPaymentsForBillFlow(billId).first()
        assertEquals(1, payments.size)
        assertEquals(50.00, payments[0].amountPaid, 0.001)
    }

    @Test
    fun updateSubscriptionPrice_appendsPriceHistoryAtomically() = runBlocking {
        val sample = createSampleSubscription(title = "News Subscription", amount = 10.00)
        val subId = subscriptionRepository.insertSubscription(sample)

        subscriptionRepository.updateSubscriptionPrice(
            subscriptionId = subId,
            newPrice = 12.00,
            effectiveDateIso = "2026-03-01",
            reason = "Annual rate adjustment"
        )

        val updatedSub = subscriptionRepository.getSubscriptionById(subId)
        assertEquals(12.00, updatedSub?.amount ?: 0.0, 0.001)

        val history = subscriptionRepository.getPriceHistoryFlow(subId).first()
        assertEquals(1, history.size)
        assertEquals(10.00, history[0].oldAmount, 0.001)
        assertEquals(12.00, history[0].newAmount, 0.001)
        assertEquals("Annual rate adjustment", history[0].reason)
    }

    @Test
    fun deleteSubscription_cascadesBillsDeletion() = runBlocking {
        val sample = createSampleSubscription()
        val subId = lifeRepository.insertSubscription(sample)

        val billsBefore = lifeRepository.getBillsForSubscriptionFlow(subId).first()
        assertEquals(1, billsBefore.size)

        lifeRepository.deleteSubscription(subId)

        val subAfter = lifeRepository.getSubscriptionById(subId)
        assertNull(subAfter)

        val billsAfter = lifeRepository.getBillsForSubscriptionFlow(subId).first()
        assertTrue(billsAfter.isEmpty())
    }
}
