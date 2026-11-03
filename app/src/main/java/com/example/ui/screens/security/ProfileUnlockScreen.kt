package com.example.ui.screens.security

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun ProfileUnlockScreen(
    profileName: String,
    profileEmoji: String,
    authMethod: String,
    biometricEnabled: Boolean,
    lockoutUntilEpochMs: Long,
    onUnlockAttempt: (credential: String) -> Boolean,
    onBiometricClick: () -> Unit,
    onCancelSwitch: (() -> Unit)?,
    onResetLock: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pinInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var patternSelectedPoints by remember { mutableStateOf<List<Int>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }

    val now = remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(lockoutUntilEpochMs) {
        while (lockoutUntilEpochMs > now.value) {
            delay(1000L)
            now.value = System.currentTimeMillis()
        }
    }

    val isLockedOut = lockoutUntilEpochMs > now.value
    val lockoutSecondsRemaining = if (isLockedOut) {
        (((lockoutUntilEpochMs - now.value) / 1000L) + 1).toInt()
    } else 0

    // Auto-prompt biometric if enabled and not locked out
    LaunchedEffect(biometricEnabled, isLockedOut) {
        if (biometricEnabled && !isLockedOut) {
            onBiometricClick()
        }
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("profile_unlock_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Profile Avatar and Lock Title
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 32.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = profileEmoji, fontSize = 40.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Unlock $profileName",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = if (isLockedOut) {
                        "Too many failed attempts. Try again in ${lockoutSecondsRemaining}s"
                    } else {
                        when (authMethod) {
                            "PIN" -> "Enter your 4-digit PIN"
                            "PASSWORD" -> "Enter profile password"
                            "PATTERN" -> "Draw your 3x3 pattern"
                            else -> "Enter your security credential"
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isLockedOut) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )

                if (errorMessage != null && !isLockedOut) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            // Input Section depending on authMethod
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (isLockedOut) {
                    Icon(
                        imageVector = Icons.Filled.LockClock,
                        contentDescription = "Locked out",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(64.dp)
                    )
                } else {
                    when (authMethod) {
                        "PIN" -> {
                            PinPad(
                                pin = pinInput,
                                onDigitClick = { digit ->
                                    if (pinInput.length < 6) {
                                        val newPin = pinInput + digit
                                        pinInput = newPin
                                        errorMessage = null
                                        if (newPin.length >= 4) {
                                            val ok = onUnlockAttempt(newPin)
                                            if (!ok) {
                                                errorMessage = "Incorrect PIN"
                                                pinInput = ""
                                            }
                                        }
                                    }
                                },
                                onDeleteClick = {
                                    if (pinInput.isNotEmpty()) {
                                        pinInput = pinInput.dropLast(1)
                                        errorMessage = null
                                    }
                                }
                            )
                        }

                        "PASSWORD" -> {
                            val focusManager = LocalFocusManager.current
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                            ) {
                                OutlinedTextField(
                                    value = passwordInput,
                                    onValueChange = {
                                        passwordInput = it
                                        errorMessage = null
                                    },
                                    label = { Text("Password") },
                                    singleLine = true,
                                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = {
                                        focusManager.clearFocus()
                                        val ok = onUnlockAttempt(passwordInput)
                                        if (!ok) {
                                            errorMessage = "Incorrect password"
                                            passwordInput = ""
                                        }
                                    }),
                                    trailingIcon = {
                                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                            Icon(
                                                imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("profile_unlock_password_field")
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        focusManager.clearFocus()
                                        val ok = onUnlockAttempt(passwordInput)
                                        if (!ok) {
                                            errorMessage = "Incorrect password"
                                            passwordInput = ""
                                        }
                                    },
                                    enabled = passwordInput.isNotBlank(),
                                    modifier = Modifier.fillMaxWidth().testTag("profile_unlock_submit_button")
                                ) {
                                    Text("Unlock")
                                }
                            }
                        }

                        "PATTERN" -> {
                            PatternGrid(
                                selectedPoints = patternSelectedPoints,
                                onPointSelected = { pt ->
                                    if (!patternSelectedPoints.contains(pt)) {
                                        patternSelectedPoints = patternSelectedPoints + pt
                                    }
                                },
                                onPatternCompleted = {
                                    val patternStr = patternSelectedPoints.joinToString("-")
                                    val ok = onUnlockAttempt(patternStr)
                                    if (!ok) {
                                        errorMessage = "Incorrect pattern"
                                    }
                                    patternSelectedPoints = emptyList()
                                }
                            )
                        }

                        else -> {
                            Button(onClick = { onUnlockAttempt("") }) {
                                Text("Unlock Profile")
                            }
                        }
                    }
                }
            }

            // Bottom Actions: Biometric, Reset, Cancel
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                if (biometricEnabled && !isLockedOut) {
                    FilledTonalButton(
                        onClick = onBiometricClick,
                        modifier = Modifier.padding(bottom = 8.dp).testTag("profile_unlock_biometric_button")
                    ) {
                        Icon(imageVector = Icons.Filled.Fingerprint, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Unlock with Biometrics")
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onCancelSwitch != null) {
                        TextButton(onClick = onCancelSwitch) {
                            Text("Cancel Switch")
                        }
                    }

                    TextButton(onClick = { showResetDialog = true }) {
                        Text("Forgot Credential?", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Profile Lock?") },
            text = {
                Text(
                    "Resetting will disable the lock for $profileName so you can regain access. " +
                    "Your profile data and settings will remain completely intact."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        onResetLock()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reset Lock")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PinPad(
    pin: String,
    onDigitClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        // PIN indicator dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            for (i in 0 until 4) {
                val filled = i < pin.length
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            if (filled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                )
            }
        }

        // Numeric Keypad 3x4
        val digits = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("", "0", "DEL")
        )

        for (row in digits) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.padding(vertical = 6.dp)
            ) {
                for (item in row) {
                    if (item.isEmpty()) {
                        Spacer(modifier = Modifier.size(68.dp))
                    } else if (item == "DEL") {
                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                        ) {
                            Icon(imageVector = Icons.Filled.Backspace, contentDescription = "Delete")
                        }
                    } else {
                        Surface(
                            onClick = { onDigitClick(item) },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(68.dp).testTag("pin_key_$item")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = item,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PatternGrid(
    selectedPoints: List<Int>,
    onPointSelected: (Int) -> Unit,
    onPatternCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        for (row in 0 until 3) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(36.dp),
                modifier = Modifier.padding(vertical = 18.dp)
            ) {
                for (col in 0 until 3) {
                    val index = row * 3 + col
                    val isSelected = selectedPoints.contains(index)
                    Surface(
                        onClick = {
                            onPointSelected(index)
                        },
                        shape = CircleShape,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(
                            2.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier.size(48.dp).testTag("pattern_dot_$index")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.onPrimary)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onPatternCompleted,
            enabled = selectedPoints.size >= 4,
            modifier = Modifier.testTag("pattern_done_button")
        ) {
            Text("Done")
        }
    }
}
