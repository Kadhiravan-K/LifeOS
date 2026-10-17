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
        val repository = LifeRepository(database, preferences)

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

            com.example.ui.theme.engine.LifeOSThemeProvider(
                theme = activeThemeDef,
                isLightMode = !isDark
            ) {
                com.example.ui.theme.engine.ThemedAtmosphereBackground {
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }
}
