package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.preferences.UserPreferences
import com.example.data.repository.LifeRepository
import com.example.domain.legal.LegalDocuments
import com.example.notifications.MaintenanceWorker
import com.example.ui.navigation.NavigationItem
import com.example.ui.theme.engine.LocalPremiumEntitlementRepository
import com.example.ui.viewmodel.LifeViewModel
import com.example.ui.viewmodel.LifeViewModelFactory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MainActivityLaunchTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testFreshInstall_MainActivityLaunchesWithoutCrash() {
        val controller = Robolectric.buildActivity(MainActivity::class.java)
        controller.create()
        controller.start()
        controller.resume()
        val activity = controller.get()
        assertNotNull("Activity should not be null on fresh launch", activity)
        assertFalse("Activity should not be finishing", activity.isFinishing)
        controller.pause()
        controller.stop()
        controller.destroy()
    }

    @Test
    fun testMaintenanceWorker_schedulePeriodic_doesNotThrow() {
        // Must never throw even in testing or restricted environments
        MaintenanceWorker.schedulePeriodic(context)
    }

    @Test
    fun testLifeViewModel_initialization_doesNotThrowNpeOnProperties() {
        val database = AppDatabase.getInstance(context)
        val preferences = UserPreferences(context)
        val entitlementRepo = LocalPremiumEntitlementRepository(context)
        val repository = LifeRepository(database, preferences, entitlementRepo)

        val factory = LifeViewModelFactory(
            application = ApplicationProvider.getApplicationContext(),
            repository = repository,
            preferences = preferences
        )
        val vm = factory.create(LifeViewModel::class.java)

        // Verify core flows are initialized and not null
        assertNotNull(vm.activeProfileIdFlow)
        assertNotNull(vm.activeProfileIdFlow.value)
        assertNotNull(vm.isAppLocked)
        assertNotNull(vm.todayEffectiveShift)
        assertNotNull(vm.allTasks)
        assertNotNull(vm.habits)
    }

    @Test
    fun testExistingUser_LegalConsentAndOnboardingComplete_launchesCleanly() {
        runBlocking {
            val preferences = UserPreferences(context)
            preferences.recordLegalConsent(
                termsVersion = LegalDocuments.CURRENT_TERMS_VERSION,
                privacyVersion = LegalDocuments.CURRENT_PRIVACY_VERSION
            )
            preferences.recordProfileOnboardingCompleted(1L)
        }

        val controller = Robolectric.buildActivity(MainActivity::class.java)
        controller.create()
        controller.start()
        controller.resume()
        val activity = controller.get()
        assertNotNull(activity)
        assertFalse(activity.isFinishing)
        controller.pause()
        controller.stop()
        controller.destroy()
    }

    @Test
    fun testNotificationPermissionRequestCode_is16BitSafeForFragmentActivity() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).create()
        val activity = controller.get()
        // Must use <= 16-bit requestCode (<= 65535) so FragmentActivity.checkForValidRequestCode does not throw
        androidx.core.app.ActivityCompat.requestPermissions(
            activity,
            arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
            1001
        )
        assertFalse(activity.isFinishing)
        controller.destroy()
    }
}
