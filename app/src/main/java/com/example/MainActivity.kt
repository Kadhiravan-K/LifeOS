package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.AppDatabase
import com.example.data.preferences.UserPreferences
import com.example.data.repository.LifeRepository
import com.example.notifications.NotificationHelper
import com.example.ui.navigation.MainScreen
import com.example.ui.theme.LifeOSTheme
import com.example.ui.viewmodel.LifeViewModel
import com.example.ui.viewmodel.LifeViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize local persistence & repository
        val database = AppDatabase.getInstance(applicationContext)
        val preferences = UserPreferences(applicationContext)
        val entitlementRepo = com.example.ui.theme.engine.LocalPremiumEntitlementRepository(applicationContext)
        val repository = LifeRepository(database, preferences, entitlementRepo)

        val viewModelFactory = LifeViewModelFactory(application, repository, preferences)
        val viewModel = ViewModelProvider(this, viewModelFactory)[LifeViewModel::class.java]

        // Create local notification channels and schedule background maintenance
        NotificationHelper.createNotificationChannels(this)
        NotificationHelper.scheduleDailyReminder(this, 8, 0)
        com.example.notifications.MaintenanceWorker.schedulePeriodic(this)

        setContent {
            val activeThemeDef by viewModel.activeThemeDefinition.collectAsStateWithLifecycle()
            val themeMode by viewModel.themeModeFlow.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val isDark = when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> systemDark
            }

            // Notification permission request for Android 13+
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { /* permission granted/denied handled gracefully */ }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            val isLegalConsentComplete by viewModel.isLegalConsentComplete.collectAsStateWithLifecycle()
            val hasCompletedOnboarding by viewModel.hasCompletedOnboarding.collectAsStateWithLifecycle()
            val animationSpeed by viewModel.animationSpeedFlow.collectAsStateWithLifecycle()
            val themeCustomization by viewModel.independentThemeCustomizationFlow.collectAsStateWithLifecycle()

            com.example.ui.theme.engine.LifeOSThemeProvider(
                theme = activeThemeDef,
                isLightMode = !isDark,
                animationSpeed = animationSpeed,
                customization = themeCustomization
            ) {
                com.example.ui.theme.engine.ThemedAtmosphereBackground {
                    if (isLegalConsentComplete == null || hasCompletedOnboarding == null) {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            androidx.compose.material3.CircularProgressIndicator(
                                color = androidx.compose.material3.MaterialTheme.colorScheme.primary
                            )
                        }
                    } else if (isLegalConsentComplete == false) {
                        com.example.ui.screens.legal.LegalConsentScreen(
                            onAcceptConsent = { termsVer, privacyVer ->
                                viewModel.recordLegalConsent(termsVer, privacyVer)
                            }
                        )
                    } else {
                        // Release 3 Phase 5 Batch 05C: Entry Routing (#9, #19)
                        val isAppEntryDismissed by viewModel.isAppEntrySelectorDismissed.collectAsStateWithLifecycle()
                        val showOnStartup by viewModel.showProfileSelectorOnLaunchFlow.collectAsStateWithLifecycle()
                        val allProfiles by viewModel.allProfilesFlow.collectAsStateWithLifecycle()
                        val activeProfileId by viewModel.activeProfileIdFlow.collectAsStateWithLifecycle()

                        if (hasCompletedOnboarding == false) {
                            com.example.ui.screens.onboarding.OnboardingSurveyScreen(
                                profileId = activeProfileId,
                                isEditMode = false,
                                onComplete = { survey ->
                                    viewModel.saveProfileSurvey(survey)
                                },
                                onSkipAll = {
                                    viewModel.skipProfileSurvey(activeProfileId)
                                }
                            )
                        } else if (showOnStartup && !isAppEntryDismissed) {
                            com.example.ui.screens.profile.AppEntryProfileSelectorScreen(
                                profiles = allProfiles,
                                activeProfileId = activeProfileId,
                                showOnStartupPreference = showOnStartup,
                                onProfileSelected = { id ->
                                    viewModel.switchProfile(id)
                                },
                                onEnterWorkspace = { id ->
                                    viewModel.switchProfile(id)
                                    viewModel.dismissAppEntryProfileSelector()
                                },
                                onCreateProfile = { name, emoji, colorHex, type ->
                                    viewModel.createProfile(name, emoji, colorHex, type)
                                },
                                onToggleShowOnStartup = { show ->
                                    viewModel.setShowProfileSelectorOnLaunch(show)
                                }
                            )
                        } else {
                            MainScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
