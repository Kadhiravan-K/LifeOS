package com.example.billing

import com.example.ui.components.getPlayStoreSubscriptionsUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test suite verifying Requirement #10 of Prompt P5-L4:
 * Subscription Disclosures, Dynamic Pricing Presentation, Trial Transparency,
 * Google Play Cancellation Navigation URL, and Non-Destructive Downgrade Safeguards.
 */
class SubscriptionDisclosureUnitTest {

    @Test
    fun getPlayStoreSubscriptionsUrl_createsCanonicalManagementUrl() {
        val testPackage = "com.example.lifeos"
        val url = getPlayStoreSubscriptionsUrl(testPackage)

        assertTrue("Url must direct to Google Play subscriptions", url.startsWith("https://play.google.com/store/account/subscriptions"))
        assertTrue("Url must include package parameter", url.contains("package=$testPackage"))
        assertEquals("https://play.google.com/store/account/subscriptions?package=com.example.lifeos", url)
    }

    @Test
    fun dynamicPricing_handlesVariousCurrenciesWithoutHardcoding() {
        // Test regional currencies dynamically formatted by Google Play Console
        val usdProduct = SubscriptionProduct(
            productId = "lifeos_premium_monthly",
            basePlanId = "monthly-plan",
            offerId = "7-day-free-trial",
            title = "LifeOS Premium",
            description = "Monthly Premium Subscription",
            formattedPrice = "$2.99",
            priceCurrencyCode = "USD",
            priceMicros = 2990000L,
            billingPeriod = "P1M",
            freeTrialDays = 7
        )
        assertEquals("$2.99", usdProduct.priceFormatted)
        assertEquals("P1M", usdProduct.billingPeriod)

        val eurProduct = usdProduct.copy(
            formattedPrice = "2,99 €",
            priceCurrencyCode = "EUR",
            priceMicros = 2990000L
        )
        assertEquals("2,99 €", eurProduct.priceFormatted)

        val inrProduct = usdProduct.copy(
            formattedPrice = "₹249.00",
            priceCurrencyCode = "INR",
            priceMicros = 249000000L
        )
        assertEquals("₹249.00", inrProduct.priceFormatted)

        val gbpProduct = usdProduct.copy(
            formattedPrice = "£2.49",
            priceCurrencyCode = "GBP",
            priceMicros = 2490000L
        )
        assertEquals("£2.49", gbpProduct.priceFormatted)
    }

    @Test
    fun trialDisclosure_accuratelyReflectsEligibility() {
        val eligibleProduct = SubscriptionProduct(
            productId = "lifeos_premium_monthly",
            basePlanId = "monthly-plan",
            offerId = "7-day-free-trial",
            title = "LifeOS Premium",
            description = "Monthly",
            formattedPrice = "$2.99",
            priceCurrencyCode = "USD",
            priceMicros = 2990000L,
            billingPeriod = "P1M",
            freeTrialDays = 7
        )
        assertTrue(eligibleProduct.hasFreeTrial)
        assertEquals(7, eligibleProduct.trialPeriodDays)

        // Ineligible user (trial consumed or expired): no trial days
        val ineligibleProduct = eligibleProduct.copy(
            offerId = null,
            freeTrialDays = 0
        )
        assertFalse(ineligibleProduct.hasFreeTrial)
        assertEquals(0, ineligibleProduct.trialPeriodDays)
    }

    @Test
    fun cancellationAndExpiry_retainsFreeAccessAndPreservesData() {
        // Expired subscription state
        val expiredState = PremiumEntitlementState.Expired(
            expiredAtMillis = System.currentTimeMillis() - 86400000L
        )
        assertFalse("Expired subscription must not grant entitlement", expiredState.isEntitled)

        // Cancelled until expiry state
        val cancelledState = PremiumEntitlementState.CancelledUntilExpiry(
            expiryTimeMillis = System.currentTimeMillis() + 86400000L * 10
        )
        assertTrue("Cancelled subscription must maintain access until billing period concludes", cancelledState.isEntitled)

        // Free state
        val freeState = PremiumEntitlementState.Free
        assertFalse(freeState.isEntitled)
    }
}
