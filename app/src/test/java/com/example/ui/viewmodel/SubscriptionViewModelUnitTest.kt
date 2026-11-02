package com.example.ui.viewmodel

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.preferences.UserPreferences
import com.example.data.repository.SubscriptionRepository
import com.example.domain.subscription.BillStatus
import com.example.domain.subscription.BillingFrequency
import com.example.domain.subscription.MissingDatePolicy
import com.example.domain.subscription.SubscriptionItem
import com.example.domain.subscription.SubscriptionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SubscriptionViewModelUnitTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var preferences: UserPreferences
    private lateinit var subscriptionRepository: SubscriptionRepository
    private lateinit var subscriptionViewModel: SubscriptionViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        preferences = UserPreferences(context)
        subscriptionRepository = SubscriptionRepository(
            database = database,
            preferences = preferences,
            ioDispatcher = Dispatchers.Unconfined,
            activeProfileIdFlow = kotlinx.coroutines.flow.flowOf(1L)
        )
        subscriptionViewModel = SubscriptionViewModel(subscriptionRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        database.close()
    }

    private fun createSampleSubscription(title: String = "Test Music", amount: Double = 9.99): SubscriptionItem {
        return SubscriptionItem(
            profileId = 1L,
            title = title,
            description = "High resolution audio streaming",
            amount = amount,
            currency = "USD",
            frequency = BillingFrequency.MONTHLY,
            anchorDateIso = "2026-03-01",
            anchorDayOfMonth = 1,
            missingDatePolicy = MissingDatePolicy.LAST_VALID_DAY,
            nextRenewalDateIso = "2026-04-01",
            status = SubscriptionStatus.ACTIVE
        )
    }

    @Test
    fun initialState_flowsAreEmpty() = runTest {
        assertTrue(subscriptionViewModel.allSubscriptions.value.isEmpty())
        assertTrue(subscriptionViewModel.activeSubscriptions.value.isEmpty())
        assertTrue(subscriptionViewModel.allBills.value.isEmpty())
        assertFalse(subscriptionViewModel.isLoading.value)
        assertNull(subscriptionViewModel.errorMessage.value)
    }

    @Test
    fun saveSubscription_insertsAndUpdatesStateFlow() = runTest {
        val sample = createSampleSubscription()
        var savedId = 0L
        val latch = CountDownLatch(1)

        subscriptionViewModel.saveSubscription(sample) { id ->
            savedId = id
            latch.countDown()
        }
        assertTrue("Timed out waiting for saveSubscription", latch.await(5, TimeUnit.SECONDS))
        assertTrue(savedId > 0)

        val bills = subscriptionViewModel.getBillsForSubscription(savedId).first()
        assertEquals(1, bills.size)
        assertEquals(savedId, bills[0].subscriptionId)

        val sub = subscriptionRepository.getSubscriptionById(savedId)
        assertNotNull(sub)
        assertEquals("Test Music", sub?.title)
        assertEquals(9.99, sub?.amount ?: 0.0, 0.001)
    }

    @Test
    fun saveSubscription_existingSubscription_updatesData() = runTest {
        val sample = createSampleSubscription()
        var savedId = 0L
        val latch1 = CountDownLatch(1)
        subscriptionViewModel.saveSubscription(sample) { id ->
            savedId = id
            latch1.countDown()
        }
        assertTrue(latch1.await(5, TimeUnit.SECONDS))

        val inserted = subscriptionRepository.getSubscriptionById(savedId)
        assertNotNull(inserted)
        val updated = inserted!!.copy(title = "Updated Music Title", amount = 11.99)

        val latch2 = CountDownLatch(1)
        subscriptionViewModel.saveSubscription(updated) {
            latch2.countDown()
        }
        assertTrue(latch2.await(5, TimeUnit.SECONDS))

        val retrieved = subscriptionRepository.getSubscriptionById(savedId)
        assertNotNull(retrieved)
        assertEquals("Updated Music Title", retrieved?.title)
        assertEquals(11.99, retrieved?.amount ?: 0.0, 0.001)
    }

    @Test
    fun setSubscriptionStatus_updatesState() = runTest {
        val sample = createSampleSubscription()
        var savedId = 0L
        val latch = CountDownLatch(1)
        subscriptionViewModel.saveSubscription(sample) { id ->
            savedId = id
            latch.countDown()
        }
        assertTrue(latch.await(5, TimeUnit.SECONDS))

        subscriptionViewModel.setSubscriptionStatus(savedId, SubscriptionStatus.PAUSED)

        var updated = subscriptionRepository.getSubscriptionById(savedId)
        var attempts = 0
        while (updated?.status != SubscriptionStatus.PAUSED && attempts < 20) {
            delay(50)
            updated = subscriptionRepository.getSubscriptionById(savedId)
            attempts++
        }
        assertEquals(SubscriptionStatus.PAUSED, updated?.status)
    }

    @Test
    fun deleteSubscription_removesSubscriptionAndAssociatedBills() = runTest {
        val sample = createSampleSubscription()
        var savedId = 0L
        val latch1 = CountDownLatch(1)
        subscriptionViewModel.saveSubscription(sample) { id ->
            savedId = id
            latch1.countDown()
        }
        assertTrue(latch1.await(5, TimeUnit.SECONDS))

        val latch2 = CountDownLatch(1)
        subscriptionViewModel.deleteSubscription(savedId) {
            latch2.countDown()
        }
        assertTrue(latch2.await(5, TimeUnit.SECONDS))

        val deleted = subscriptionRepository.getSubscriptionById(savedId)
        assertNull(deleted)
        val bills = subscriptionViewModel.getBillsForSubscription(savedId).first()
        assertTrue(bills.isEmpty())
    }

    @Test
    fun recordBillPayment_updatesBillStatusAndCreatesPaymentRecord() = runTest {
        val sample = createSampleSubscription()
        var savedId = 0L
        val latch = CountDownLatch(1)
        subscriptionViewModel.saveSubscription(sample) { id ->
            savedId = id
            latch.countDown()
        }
        assertTrue(latch.await(5, TimeUnit.SECONDS))

        val bills = subscriptionViewModel.getBillsForSubscription(savedId).first()
        assertEquals(1, bills.size)
        val bill = bills[0]
        assertEquals(BillStatus.UNPAID, bill.status)

        subscriptionViewModel.recordBillPayment(
            billId = bill.id,
            subscriptionId = savedId,
            amountPaid = 9.99,
            paymentDateIso = "2026-04-01",
            paymentMethod = "Credit Card",
            transactionRef = "TX-9999",
            notes = "Paid in full"
        )

        var updatedBills = subscriptionViewModel.getBillsForSubscription(savedId).first()
        var attempts = 0
        while (updatedBills.firstOrNull()?.status != BillStatus.PAID && attempts < 20) {
            delay(50)
            updatedBills = subscriptionViewModel.getBillsForSubscription(savedId).first()
            attempts++
        }
        assertEquals(BillStatus.PAID, updatedBills[0].status)

        val payments = subscriptionViewModel.getPaymentsForSubscription(savedId).first()
        assertEquals(1, payments.size)
        assertEquals(9.99, payments[0].amountPaid, 0.001)
        assertEquals("TX-9999", payments[0].transactionRef)
    }

    @Test
    fun updateBillStatusAndAmount_updatesValuesCorrectly() = runTest {
        val sample = createSampleSubscription()
        var savedId = 0L
        val latch = CountDownLatch(1)
        subscriptionViewModel.saveSubscription(sample) { id ->
            savedId = id
            latch.countDown()
        }
        assertTrue(latch.await(5, TimeUnit.SECONDS))

        val bill = subscriptionViewModel.getBillsForSubscription(savedId).first()[0]
        subscriptionViewModel.updateBillStatus(bill, BillStatus.PAID)

        var updatedBills = subscriptionViewModel.getBillsForSubscription(savedId).first()
        var attempts = 0
        while (updatedBills[0].status != BillStatus.PAID && attempts < 20) {
            delay(50)
            updatedBills = subscriptionViewModel.getBillsForSubscription(savedId).first()
            attempts++
        }
        assertEquals(BillStatus.PAID, updatedBills[0].status)

        val paidBill = updatedBills[0]
        subscriptionViewModel.updateBillAmount(paidBill, 15.00)

        attempts = 0
        while (updatedBills[0].expectedAmount != 15.00 && attempts < 20) {
            delay(50)
            updatedBills = subscriptionViewModel.getBillsForSubscription(savedId).first()
            attempts++
        }
        assertEquals(15.00, updatedBills[0].expectedAmount, 0.001)
        assertEquals(BillStatus.PAID, updatedBills[0].status)
    }

    @Test
    fun updateSubscriptionPrice_recordsPriceHistoryAndUpdatesAmount() = runTest {
        val sample = createSampleSubscription(amount = 9.99)
        var savedId = 0L
        val latch = CountDownLatch(1)
        subscriptionViewModel.saveSubscription(sample) { id ->
            savedId = id
            latch.countDown()
        }
        assertTrue(latch.await(5, TimeUnit.SECONDS))

        subscriptionViewModel.updateSubscriptionPrice(
            subscriptionId = savedId,
            newPrice = 12.99,
            effectiveDateIso = "2026-05-01",
            reason = "Price hike"
        )

        var history = subscriptionViewModel.getPriceHistory(savedId).first()
        var attempts = 0
        while (history.isEmpty() && attempts < 20) {
            delay(50)
            history = subscriptionViewModel.getPriceHistory(savedId).first()
            attempts++
        }
        assertEquals(1, history.size)
        assertEquals(9.99, history[0].oldAmount, 0.001)
        assertEquals(12.99, history[0].newAmount, 0.001)
        assertEquals("Price hike", history[0].reason)

        val updated = subscriptionRepository.getSubscriptionById(savedId)
        assertEquals(12.99, updated?.amount ?: 0.0, 0.001)
    }

    @Test
    fun clearError_resetsErrorMessage() {
        subscriptionViewModel.clearError()
        assertNull(subscriptionViewModel.errorMessage.value)
    }

    @Test
    fun factory_createsSubscriptionViewModelCorrectly() {
        val factory = SubscriptionViewModelFactory(subscriptionRepository)
        val created = factory.create(SubscriptionViewModel::class.java)
        assertNotNull(created)
    }
}
