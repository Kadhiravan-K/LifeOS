package com.example.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Production-ready Google Play Billing implementation (BillingClient 7.x).
 * Manages connection, product detail queries for monthly subscription + 7-day free trial,
 * purchase flow launch, and purchase queries/acknowledgements.
 */
class PlayBillingManager(
    private val context: Context,
    private val productId: String = "lifeos_premium_monthly",
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : BillingManager, PurchasesUpdatedListener {

    private val _subscriptionProduct = MutableStateFlow<SubscriptionProduct?>(null)
    override val subscriptionProduct: StateFlow<SubscriptionProduct?> = _subscriptionProduct.asStateFlow()

    private val _subscriptionPurchase = MutableStateFlow<SubscriptionPurchase?>(null)
    override val subscriptionPurchase: StateFlow<SubscriptionPurchase?> = _subscriptionPurchase.asStateFlow()

    private val _connectionState = MutableStateFlow(BillingConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<BillingConnectionState> = _connectionState.asStateFlow()

    private var cachedProductDetails: ProductDetails? = null

    private val pendingPurchasesParams = PendingPurchasesParams.newBuilder()
        .enableOneTimeProducts()
        .build()

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(pendingPurchasesParams)
        .build()

    init {
        scope.launch {
            startConnection()
        }
    }

    override suspend fun startConnection() {
        if (_connectionState.value == BillingConnectionState.CONNECTING ||
            _connectionState.value == BillingConnectionState.CONNECTED
        ) {
            return
        }

        _connectionState.value = BillingConnectionState.CONNECTING

        suspendCoroutine<Unit> { continuation ->
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        _connectionState.value = BillingConnectionState.CONNECTED
                        scope.launch {
                            querySubscriptionDetails()
                            restorePurchases()
                        }
                    } else {
                        _connectionState.value = BillingConnectionState.DISCONNECTED
                    }
                    continuation.resume(Unit)
                }

                override fun onBillingServiceDisconnected() {
                    _connectionState.value = BillingConnectionState.DISCONNECTED
                }
            })
        }
    }

    override suspend fun querySubscriptionDetails() = withContext(Dispatchers.IO) {
        if (_connectionState.value != BillingConnectionState.CONNECTED) {
            return@withContext
        }

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val details = productDetailsList.firstOrNull { it.productId == productId }
                if (details != null) {
                    cachedProductDetails = details
                    val subscriptionOfferDetails = details.subscriptionOfferDetails
                    val monthlyOffer = subscriptionOfferDetails?.firstOrNull()
                    if (monthlyOffer != null) {
                        val pricingPhase = monthlyOffer.pricingPhases.pricingPhaseList.lastOrNull()
                        val trialPhase = monthlyOffer.pricingPhases.pricingPhaseList.firstOrNull { it.priceAmountMicros == 0L }

                        val trialDays = if (trialPhase != null && trialPhase.billingPeriod.contains("P7D")) 7 else if (trialPhase != null) 7 else 0

                        _subscriptionProduct.value = SubscriptionProduct(
                            productId = details.productId,
                            basePlanId = monthlyOffer.basePlanId,
                            offerId = monthlyOffer.offerId,
                            title = details.title,
                            description = details.description,
                            formattedPrice = pricingPhase?.formattedPrice ?: "",
                            priceCurrencyCode = pricingPhase?.priceCurrencyCode ?: "",
                            priceMicros = pricingPhase?.priceAmountMicros ?: 0L,
                            billingPeriod = pricingPhase?.billingPeriod ?: "P1M",
                            freeTrialPeriod = trialPhase?.billingPeriod,
                            freeTrialDays = trialDays
                        )
                    }
                }
            }
        }
    }

    override suspend fun launchPurchaseFlow(activity: Activity): BillingResultStatus = withContext(Dispatchers.Main) {
        if (_connectionState.value != BillingConnectionState.CONNECTED) {
            return@withContext BillingResultStatus.SERVICE_UNAVAILABLE
        }

        val details = cachedProductDetails ?: return@withContext BillingResultStatus.ITEM_UNAVAILABLE
        val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken
            ?: return@withContext BillingResultStatus.ITEM_UNAVAILABLE

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .setOfferToken(offerToken)
                .build()
        )

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        val response = billingClient.launchBillingFlow(activity, flowParams)
        mapBillingResponse(response.responseCode)
    }

    override suspend fun restorePurchases(): BillingResultStatus = withContext(Dispatchers.IO) {
        if (_connectionState.value != BillingConnectionState.CONNECTED) {
            return@withContext BillingResultStatus.SERVICE_UNAVAILABLE
        }

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        suspendCoroutine { continuation ->
            billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    processPurchases(purchases)
                    continuation.resume(BillingResultStatus.OK)
                } else {
                    continuation.resume(mapBillingResponse(billingResult.responseCode))
                }
            }
        }
    }

    override suspend fun acknowledgePurchase(purchaseToken: String): Boolean = withContext(Dispatchers.IO) {
        if (_connectionState.value != BillingConnectionState.CONNECTED) {
            return@withContext false
        }

        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()

        suspendCoroutine { continuation ->
            billingClient.acknowledgePurchase(params) { result ->
                continuation.resume(result.responseCode == BillingClient.BillingResponseCode.OK)
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            processPurchases(purchases)
        }
    }

    private fun processPurchases(purchases: List<Purchase>) {
        val targetPurchase = purchases.firstOrNull { it.products.contains(productId) }
        if (targetPurchase != null) {
            val state = when (targetPurchase.purchaseState) {
                Purchase.PurchaseState.PURCHASED -> PurchaseState.PURCHASED
                Purchase.PurchaseState.PENDING -> PurchaseState.PENDING
                else -> PurchaseState.UNSPECIFIED
            }

            val current = _subscriptionPurchase.value
            if (current != null &&
                current.purchaseToken == targetPurchase.purchaseToken &&
                current.purchaseState == state &&
                current.isAcknowledged == targetPurchase.isAcknowledged &&
                current.isAutoRenewing == targetPurchase.isAutoRenewing
            ) {
                // Idempotent reconciliation: purchase already reflected in memory
                return
            }

            val mapped = SubscriptionPurchase(
                orderId = targetPurchase.orderId,
                purchaseToken = targetPurchase.purchaseToken,
                productId = productId,
                purchaseTimeMillis = targetPurchase.purchaseTime,
                purchaseState = state,
                isAcknowledged = targetPurchase.isAcknowledged,
                isAutoRenewing = targetPurchase.isAutoRenewing,
                expiryTimeMillis = null, // Populated by verification or server
                lastVerifiedTimeMillis = System.currentTimeMillis()
            )

            _subscriptionPurchase.value = mapped
            // Note: Purchase acknowledgement is strictly orchestrated post-verification by the entitlement repository.
        } else {
            _subscriptionPurchase.value = null
        }
    }

    override fun destroy() {
        if (_connectionState.value != BillingConnectionState.CLOSED) {
            _connectionState.value = BillingConnectionState.CLOSED
            billingClient.endConnection()
        }
    }

    private fun mapBillingResponse(code: Int): BillingResultStatus {
        return when (code) {
            BillingClient.BillingResponseCode.OK -> BillingResultStatus.OK
            BillingClient.BillingResponseCode.USER_CANCELED -> BillingResultStatus.USER_CANCELED
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> BillingResultStatus.SERVICE_UNAVAILABLE
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> BillingResultStatus.BILLING_UNAVAILABLE
            BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> BillingResultStatus.ITEM_UNAVAILABLE
            BillingClient.BillingResponseCode.DEVELOPER_ERROR -> BillingResultStatus.DEVELOPER_ERROR
            BillingClient.BillingResponseCode.ERROR -> BillingResultStatus.ERROR
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> BillingResultStatus.ITEM_ALREADY_OWNED
            BillingClient.BillingResponseCode.ITEM_NOT_OWNED -> BillingResultStatus.ITEM_NOT_OWNED
            BillingClient.BillingResponseCode.NETWORK_ERROR -> BillingResultStatus.NETWORK_ERROR
            else -> BillingResultStatus.ERROR
        }
    }
}
