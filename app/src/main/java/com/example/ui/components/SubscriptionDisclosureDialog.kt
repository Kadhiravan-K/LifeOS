package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.billing.SubscriptionProduct

/**
 * Policy-compliant subscription disclosure modal for LifeOS Release 4.
 *
 * Implements all mandatory Google Play subscription disclosure obligations:
 * 1. Prominently displays the localized subscription price and monthly billing cadence.
 * 2. Clearly specifies the 7-day free trial conditions, when charging commences, and auto-renewal.
 * 3. Transparently documents how to cancel via Google Play with a direct action link.
 * 4. Assures users that existing data and free core functionality remain 100% accessible on expiry.
 * 5. Uses dynamic catalog data from Google Play (no hardcoded currencies or prices).
 */
@Composable
fun SubscriptionDisclosureDialog(
    subscriptionProduct: SubscriptionProduct?,
    onDismissRequest: () -> Unit,
    onConfirmPurchase: () -> Unit,
    onRestorePurchases: () -> Unit,
    onDevUnlock: (() -> Unit)? = null,
    previewAction: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val priceText = subscriptionProduct?.formattedPrice ?: "Regional Google Play Price"
    val hasTrial = subscriptionProduct?.hasFreeTrial ?: true
    val trialDays = subscriptionProduct?.freeTrialDays ?: 7

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.testTag("subscription_disclosure_dialog"),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "LifeOS Premium",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp)
            ) {
                // Feature benefits
                Text(
                    text = "Unlock the complete LifeOS experience:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                DisclosureBenefitItem("All 13 handcrafted theme environments and shaders")
                DisclosureBenefitItem("Exclusive custom cosmetic rewards in Gamification")
                DisclosureBenefitItem("Support offline-first open-source productivity development")
                DisclosureBenefitItem("100% Free Core: Tasks, habits, alarms, calendar, and backups always stay free")

                Spacer(modifier = Modifier.height(14.dp))

                // Price and Billing Cadence Box
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Monthly Subscription",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "$priceText / month",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Billed monthly • Automatic recurring renewal",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 7-Day Free Trial Notice
                if (hasTrial) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "🎁 $trialDays-Day Free Trial Included",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Start today at no initial cost. Unless cancelled at least 24 hours before your $trialDays-day trial ends, Google Play will automatically charge $priceText every month until you cancel.",
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Transparent Cancellation & Data Preservation
                Text(
                    text = "Cancellation Policy & Data Safety:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "• Cancel anytime in Google Play > Subscriptions.\n" +
                            "• If cancelled, your Premium access remains active until the end of your billing cycle.\n" +
                            "• When your subscription expires, your theme smoothly reverts to Genesis. None of your tasks, habits, reminders, notes, or backups are ever deleted.",
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Direct link to Google Play Subscriptions management
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { openPlayStoreSubscriptions(context) },
                        modifier = Modifier.testTag("manage_subscriptions_btn")
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Manage in Google Play", fontSize = 11.sp)
                    }

                    TextButton(
                        onClick = onRestorePurchases,
                        modifier = Modifier.testTag("restore_purchases_btn")
                    ) {
                        Text("Restore Purchases", fontSize = 11.sp)
                    }
                }

                // Developer Mode Bypass (Debug Builds ONLY)
                if (BuildConfig.DEBUG && onDevUnlock != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = onDevUnlock,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dev_unlock_btn")
                    ) {
                        Text("🛠️ Enable (Debug Dev Mode)", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmPurchase,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("confirm_subscription_button")
            ) {
                Text(
                    text = if (hasTrial) "Start $trialDays-Day Free Trial" else "Subscribe ($priceText/mo)",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            if (previewAction != null) {
                TextButton(onClick = previewAction) {
                    Text("Preview Theme")
                }
            } else {
                TextButton(onClick = onDismissRequest) {
                    Text("Not Now")
                }
            }
        }
    )
}

@Composable
private fun DisclosureBenefitItem(text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(16.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Canonical Google Play Subscriptions management URL generator.
 */
fun getPlayStoreSubscriptionsUrl(packageName: String): String {
    return "https://play.google.com/store/account/subscriptions?package=$packageName"
}

/**
 * Opens Google Play Subscriptions management for the current app.
 */
fun openPlayStoreSubscriptions(context: Context) {
    val url = getPlayStoreSubscriptionsUrl(context.packageName)
    val uri = Uri.parse(url)
    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        if (context !is Activity) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        val fallback = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/account/subscriptions")).apply {
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        try {
            context.startActivity(fallback)
        } catch (_: Exception) {}
    }
}
