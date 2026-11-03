package com.example.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.ProfileEntity
import com.example.data.local.entities.ProfileSecurityEntity
import com.example.security.ProfileSecurityManager
import com.example.ui.viewmodel.LifeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileLockSettingsScreen(
    viewModel: LifeViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val allProfiles by viewModel.allProfilesFlow.collectAsState()
    val activeProfileId by viewModel.activeProfileIdFlow.collectAsState()
    val activeProfile = allProfiles.find { it.id == activeProfileId } ?: ProfileEntity.createDefaultPersonal()
    val securityConfig by viewModel.activeProfileSecurityFlow.collectAsState()

    val biometricStatus = remember(context) { ProfileSecurityManager.checkBiometricStatus(context) }
    val isBiometricAvailable = biometricStatus == ProfileSecurityManager.BiometricStatus.AVAILABLE

    var showSetupDialog by remember { mutableStateOf(false) }
    var showChangeCredentialDialog by remember { mutableStateOf(false) }
    var showDisableLockDialog by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf<String?>(null) }
    var actionSuccess by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile Lock") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("profile_lock_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier.fillMaxSize().testTag("profile_lock_settings_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Profile Header Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = activeProfile.emoji, fontSize = 28.sp)
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = activeProfile.name,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${activeProfile.type} Workspace",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (actionSuccess != null) {
                Text(
                    text = actionSuccess!!,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            if (actionError != null) {
                Text(
                    text = actionError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // Lock Toggle Section
            val isLockEnabled = securityConfig?.isLockEnabled == true
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Lock this Profile",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Require authentication when opening or switching to this profile",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = isLockEnabled,
                        onCheckedChange = { checked ->
                            actionError = null
                            actionSuccess = null
                            if (checked) {
                                showSetupDialog = true
                            } else {
                                showDisableLockDialog = true
                            }
                        },
                        modifier = Modifier.testTag("profile_lock_master_toggle")
                    )
                }
            }

            if (isLockEnabled) {
                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Security Configuration",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Current Method Info Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Primary Authentication",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = securityConfig?.authMethod ?: "PIN",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            TextButton(
                                onClick = { showChangeCredentialDialog = true },
                                modifier = Modifier.testTag("change_credential_button")
                            ) {
                                Text("Change")
                            }
                        }
                    }
                }

                // Biometrics Toggle
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Biometric Unlock",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = when (biometricStatus) {
                                    ProfileSecurityManager.BiometricStatus.AVAILABLE -> "Fingerprint or face authentication"
                                    ProfileSecurityManager.BiometricStatus.NONE_ENROLLED -> "No biometrics enrolled in device settings"
                                    ProfileSecurityManager.BiometricStatus.NOT_SUPPORTED -> "Biometric hardware not available on device"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = securityConfig?.biometricEnabled == true,
                            enabled = isBiometricAvailable,
                            onCheckedChange = { enabled ->
                                coroutineScope.launch {
                                    viewModel.updateBiometricEnabled(activeProfileId, enabled)
                                    actionSuccess = if (enabled) "Biometrics enabled" else "Biometrics disabled"
                                }
                            },
                            modifier = Modifier.testTag("profile_lock_biometric_toggle")
                        )
                    }
                }

                // Auto-Lock Timeout Selector
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Auto-Lock Timeout",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Lock workspace after backgrounding or inactivity",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        val currentTimeout = securityConfig?.autoLockTimeoutSeconds ?: 300
                        val timeouts = listOf(
                            0 to "Immediately",
                            60 to "1 minute",
                            300 to "5 minutes",
                            900 to "15 minutes"
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            timeouts.forEach { (seconds, label) ->
                                val selected = currentTimeout == seconds
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        coroutineScope.launch {
                                            viewModel.updateAutoLockTimeout(activeProfileId, seconds)
                                        }
                                    },
                                    label = { Text(label, fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Setup Wizard Dialog
    if (showSetupDialog) {
        ProfileLockSetupDialog(
            isBiometricAvailable = isBiometricAvailable,
            onDismiss = { showSetupDialog = false },
            onConfirm = { method, credential, biometric, timeout ->
                showSetupDialog = false
                coroutineScope.launch {
                    val ok = viewModel.enableProfileLock(
                        profileId = activeProfileId,
                        authMethod = method,
                        rawCredential = credential,
                        biometricEnabled = biometric,
                        autoLockTimeoutSeconds = timeout
                    )
                    if (ok) {
                        actionSuccess = "Profile lock enabled with $method"
                    } else {
                        actionError = "Failed to enable profile lock"
                    }
                }
            }
        )
    }

    // Change Credential Dialog
    if (showChangeCredentialDialog) {
        ChangeCredentialDialog(
            authMethod = securityConfig?.authMethod ?: "PIN",
            onDismiss = { showChangeCredentialDialog = false },
            onConfirm = { oldCred, newCred ->
                coroutineScope.launch {
                    val ok = viewModel.changeProfileCredential(activeProfileId, oldCred, newCred)
                    if (ok) {
                        showChangeCredentialDialog = false
                        actionSuccess = "Credential updated successfully"
                    } else {
                        actionError = "Incorrect existing credential"
                    }
                }
            }
        )
    }

    // Disable Lock Dialog
    if (showDisableLockDialog) {
        DisableLockDialog(
            authMethod = securityConfig?.authMethod ?: "PIN",
            onDismiss = { showDisableLockDialog = false },
            onConfirm = { cred ->
                coroutineScope.launch {
                    val ok = viewModel.disableProfileLock(activeProfileId, cred)
                    if (ok) {
                        showDisableLockDialog = false
                        actionSuccess = "Profile lock removed"
                    } else {
                        actionError = "Incorrect credential"
                    }
                }
            }
        )
    }
}

@Composable
private fun ProfileLockSetupDialog(
    isBiometricAvailable: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (method: String, credential: String, biometric: Boolean, timeout: Int) -> Unit
) {
    var selectedMethod by remember { mutableStateOf("PIN") }
    var credentialInput by remember { mutableStateOf("") }
    var confirmInput by remember { mutableStateOf("") }
    var biometricEnabled by remember { mutableStateOf(isBiometricAvailable) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure Profile Lock") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Select Authentication Method",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("PIN", "PASSWORD", "PATTERN").forEach { method ->
                        FilterChip(
                            selected = selectedMethod == method,
                            onClick = {
                                selectedMethod = method
                                credentialInput = ""
                                confirmInput = ""
                                errorMessage = null
                            },
                            label = { Text(method, fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (selectedMethod) {
                    "PIN" -> {
                        OutlinedTextField(
                            value = credentialInput,
                            onValueChange = { if (it.length <= 6 && it.all { char -> char.isDigit() }) credentialInput = it },
                            label = { Text("Enter 4-6 digit PIN") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            modifier = Modifier.fillMaxWidth().testTag("setup_pin_field")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = confirmInput,
                            onValueChange = { if (it.length <= 6 && it.all { char -> char.isDigit() }) confirmInput = it },
                            label = { Text("Confirm PIN") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            modifier = Modifier.fillMaxWidth().testTag("setup_pin_confirm_field")
                        )
                    }

                    "PASSWORD" -> {
                        OutlinedTextField(
                            value = credentialInput,
                            onValueChange = { credentialInput = it },
                            label = { Text("Enter Password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth().testTag("setup_password_field")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = confirmInput,
                            onValueChange = { confirmInput = it },
                            label = { Text("Confirm Password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth().testTag("setup_password_confirm_field")
                        )
                    }

                    "PATTERN" -> {
                        Text(
                            text = "Enter a dot sequence pattern (e.g. 0-1-2-5-8)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = credentialInput,
                            onValueChange = { credentialInput = it },
                            label = { Text("Pattern String") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("setup_pattern_field")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = confirmInput,
                            onValueChange = { confirmInput = it },
                            label = { Text("Confirm Pattern String") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (isBiometricAvailable) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = biometricEnabled,
                            onCheckedChange = { biometricEnabled = it },
                            modifier = Modifier.testTag("setup_biometric_checkbox")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enable Biometric Unlock as shortcut", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (credentialInput.isBlank()) {
                        errorMessage = "Credential cannot be empty"
                        return@Button
                    }
                    if (selectedMethod == "PIN" && credentialInput.length < 4) {
                        errorMessage = "PIN must be at least 4 digits"
                        return@Button
                    }
                    if (credentialInput != confirmInput) {
                        errorMessage = "Credentials do not match"
                        return@Button
                    }
                    onConfirm(selectedMethod, credentialInput, biometricEnabled, 300)
                },
                modifier = Modifier.testTag("setup_confirm_button")
            ) {
                Text("Enable Lock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ChangeCredentialDialog(
    authMethod: String,
    onDismiss: () -> Unit,
    onConfirm: (oldCredential: String, newCredential: String) -> Unit
) {
    var oldInput by remember { mutableStateOf("") }
    var newInput by remember { mutableStateOf("") }
    var confirmInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change $authMethod") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = oldInput,
                    onValueChange = { oldInput = it },
                    label = { Text("Current $authMethod") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().testTag("change_cred_old_field")
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newInput,
                    onValueChange = { newInput = it },
                    label = { Text("New $authMethod") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().testTag("change_cred_new_field")
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmInput,
                    onValueChange = { confirmInput = it },
                    label = { Text("Confirm New $authMethod") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().testTag("change_cred_confirm_field")
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (oldInput.isBlank() || newInput.isBlank()) {
                        errorMessage = "Fields cannot be empty"
                        return@Button
                    }
                    if (newInput != confirmInput) {
                        errorMessage = "New credentials do not match"
                        return@Button
                    }
                    onConfirm(oldInput, newInput)
                },
                modifier = Modifier.testTag("change_cred_submit_button")
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun DisableLockDialog(
    authMethod: String,
    onDismiss: () -> Unit,
    onConfirm: (credential: String) -> Unit
) {
    var credentialInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Remove Profile Lock") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Enter your current $authMethod to remove lock protection for this profile.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                OutlinedTextField(
                    value = credentialInput,
                    onValueChange = { credentialInput = it },
                    label = { Text("Current $authMethod") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().testTag("disable_cred_field")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(credentialInput) },
                enabled = credentialInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("disable_cred_submit_button")
            ) {
                Text("Remove Lock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
