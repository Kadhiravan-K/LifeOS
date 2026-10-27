package com.example.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Standardized Secure Confirmation / Destructive Action Modal.
 * Emphasizes action consequences, distinct destructive styling, haptic warning pulse,
 * optional countdown delay, and optional typing of a confirmation phrase or username.
 */
@Composable
fun SecureConfirmationDialog(
    title: String,
    message: String,
    consequenceText: String? = null,
    requiredConfirmationPhrase: String? = null,
    countdownSeconds: Int = 0,
    enableHapticWarning: Boolean = true,
    confirmLabel: String = "Confirm",
    cancelLabel: String = "Cancel",
    isDestructive: Boolean = true,
    icon: ImageVector = Icons.Default.Warning,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var typedPhrase by remember { mutableStateOf("") }
    var remainingSeconds by remember(countdownSeconds) { mutableIntStateOf(countdownSeconds) }

    // Haptic pulse on dialog launch
    LaunchedEffect(Unit) {
        if (enableHapticWarning && isDestructive) {
            try {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vm?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }
                if (vibrator != null && vibrator.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(100)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    // Countdown timer effect
    LaunchedEffect(countdownSeconds) {
        while (remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds--
        }
    }

    val isPhraseMatching = remember(typedPhrase, requiredConfirmationPhrase) {
        if (requiredConfirmationPhrase == null) true
        else typedPhrase.trim().equals(requiredConfirmationPhrase.trim(), ignoreCase = true)
    }

    val isConfirmEnabled = remainingSeconds <= 0 && isPhraseMatching

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isDestructive) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.primaryContainer
                    )
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isDestructive) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (consequenceText != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚠️",
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = consequenceText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                if (requiredConfirmationPhrase != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Type \"$requiredConfirmationPhrase\" to confirm:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = typedPhrase,
                        onValueChange = { typedPhrase = it },
                        placeholder = { Text(requiredConfirmationPhrase) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("secure_confirmation_input")
                    )
                }
            }
        },
        confirmButton = {
            val buttonText = if (remainingSeconds > 0) {
                "$confirmLabel ($remainingSeconds)"
            } else {
                confirmLabel
            }

            Button(
                onClick = onConfirm,
                enabled = isConfirmEnabled,
                colors = if (isDestructive) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                } else {
                    ButtonDefaults.buttonColors()
                },
                modifier = Modifier.testTag("secure_dialog_confirm_btn")
            ) {
                Text(
                    text = buttonText,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("secure_dialog_cancel_btn")
            ) {
                Text(cancelLabel)
            }
        },
        modifier = Modifier.testTag("secure_confirmation_dialog")
    )
}

/**
 * Standard alias for [SecureConfirmationDialog] conforming to unified design naming.
 */
@Composable
fun LifeSafetyDialog(
    title: String,
    message: String,
    consequenceText: String? = null,
    requiredConfirmationPhrase: String? = null,
    countdownSeconds: Int = 0,
    enableHapticWarning: Boolean = true,
    confirmLabel: String = "Confirm",
    cancelLabel: String = "Cancel",
    isDestructive: Boolean = true,
    icon: ImageVector = Icons.Default.Warning,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) = SecureConfirmationDialog(
    title = title,
    message = message,
    consequenceText = consequenceText,
    requiredConfirmationPhrase = requiredConfirmationPhrase,
    countdownSeconds = countdownSeconds,
    enableHapticWarning = enableHapticWarning,
    confirmLabel = confirmLabel,
    cancelLabel = cancelLabel,
    isDestructive = isDestructive,
    icon = icon,
    onConfirm = onConfirm,
    onDismiss = onDismiss
)
