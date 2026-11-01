package com.example.notifications

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.preferences.UserPreferences
import com.example.data.repository.LifeRepository
import com.example.ui.theme.engine.LocalPremiumEntitlementRepository
import com.example.ui.viewmodel.LifeViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PreciseRemindersSettingsUnitTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var preferences: UserPreferences
    private lateinit var repository: LifeRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        preferences = UserPreferences(context)
        repository = LifeRepository(
            database,
            preferences,
            LocalPremiumEntitlementRepository(context)
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testExactAlarmStatusEnumDisplayText() {
        assertEquals("Granted", ExactAlarmStatus.GRANTED.displayText)
        assertEquals("Not granted", ExactAlarmStatus.NOT_GRANTED.displayText)
        assertEquals("Not needed", ExactAlarmStatus.NOT_NEEDED.displayText)
    }

    @Test
    fun testCheckExactAlarmStatusDoesNotCrash() {
        val status = ReminderScheduler.checkExactAlarmStatus(context)
        assertNotNull(status)
        assertTrue(status in ExactAlarmStatus.values())
    }

    @Test
    fun testOpenExactAlarmSettingsDoesNotThrow() {
        // Must handle invocation safely without uncaught exceptions
        ReminderScheduler.openExactAlarmSettings(context)
    }

    @Test
    fun testViewModelRefreshesExactAlarmStatus() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = LifeViewModel(app, repository, preferences)

        viewModel.refreshExactAlarmStatus(context)
        val status = viewModel.exactAlarmStatus.value

        assertNotNull(status)
        assertTrue(status in ExactAlarmStatus.values())
    }
}
