package com.example

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.backup.DataExportImportManager
import com.example.data.local.AppDatabase
import com.example.data.local.MIGRATION_11_12
import com.example.data.local.entities.ProfileEntity
import com.example.data.local.entities.ProfileSurveyEntity
import com.example.data.local.entities.TaskEntity
import com.example.data.preferences.UserPreferences
import com.example.data.repository.LifeRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Release3Phase5Batch05CTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var preferences: UserPreferences
    private lateinit var repository: LifeRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        preferences = UserPreferences(context)
        repository = LifeRepository(database, preferences)

        runBlocking {
            database.profileDao().insertProfile(ProfileEntity.createDefaultPersonal())
            preferences.setActiveProfileId(ProfileEntity.DEFAULT_PERSONAL_PROFILE_ID)
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testNewInstallationSurveyCompletion() = runBlocking {
        // Fresh install: onboarding not yet marked complete
        preferences.setHasCompletedOnboarding(false)
        assertFalse(preferences.hasCompletedOnboarding.first())

        // Save survey for default Personal profile
        val survey = ProfileSurveyEntity(
            profileId = ProfileEntity.DEFAULT_PERSONAL_PROFILE_ID,
            displayName = "Alex",
            gender = "Non-binary",
            sleepingTime = "22:30",
            dailyWaterGoalMl = 2500,
            discoverySource = "Social Media",
            discoverySocialPlatform = "Reddit",
            discoveryOtherDetail = null,
            isCompleted = true,
            isSkipped = false
        )

        repository.saveSurvey(survey)
        preferences.setHasCompletedOnboarding(true)

        val retrieved = repository.getSurveyForProfile(ProfileEntity.DEFAULT_PERSONAL_PROFILE_ID)
        assertNotNull(retrieved)
        assertEquals("Alex", retrieved?.displayName)
        assertEquals("Non-binary", retrieved?.gender)
        assertEquals("22:30", retrieved?.sleepingTime)
        assertEquals(2500, retrieved?.dailyWaterGoalMl)
        assertEquals("Social Media", retrieved?.discoverySource)
        assertEquals("Reddit", retrieved?.discoverySocialPlatform)
        assertTrue(retrieved!!.isCompleted)
        assertFalse(retrieved.isSkipped)
        assertTrue(preferences.hasCompletedOnboarding.first())
    }

    @Test
    fun testSurveySkippingNeverSavesSyntheticAnswers() = runBlocking {
        preferences.setHasCompletedOnboarding(false)
        val initialWaterGoal = preferences.waterGoalMl.first()
        assertEquals(2000, initialWaterGoal)

        // User skips survey
        repository.skipSurvey(ProfileEntity.DEFAULT_PERSONAL_PROFILE_ID)
        preferences.setHasCompletedOnboarding(true)

        val retrieved = repository.getSurveyForProfile(ProfileEntity.DEFAULT_PERSONAL_PROFILE_ID)
        assertNotNull(retrieved)
        assertTrue(retrieved!!.isSkipped)
        assertTrue(retrieved.isCompleted)
        assertNull("Display name must be null when skipped", retrieved.displayName)
        assertNull("Gender must not be synthetically guessed", retrieved.gender)
        assertNull("Sleeping time must not be synthetically guessed", retrieved.sleepingTime)
        assertNull("Water goal must not be synthetically overwritten", retrieved.dailyWaterGoalMl)

        // Existing preferences remain untouched
        assertEquals(2000, preferences.waterGoalMl.first())
        assertTrue(preferences.hasCompletedOnboarding.first())
    }

    @Test
    fun testRelease2UpgradeBypassesOnboardingWhenExistingDataPresent() = runBlocking {
        preferences.setHasCompletedOnboarding(false)

        // Existing Release 2 user with pre-existing task
        database.taskDao().insertTask(
            TaskEntity(
                title = "Release 2 Legacy Task",
                category = "General",
                dueDateIso = "2026-11-01",
                profileId = 1L
            )
        )

        assertTrue("User has existing tasks", repository.hasAnyExistingUserData())

        // Simulation of app launch init check
        if (!preferences.hasCompletedOnboarding.first() && repository.hasAnyExistingUserData()) {
            preferences.setHasCompletedOnboarding(true)
        }

        assertTrue("Upgraded Release 2 user must have onboarding marked complete", preferences.hasCompletedOnboarding.first())
    }

    @Test
    fun testWaterGoalValidationSanityBounds() {
        val validLow = 500
        val validHigh = 6000
        val tooLow = 499
        val tooHigh = 6001

        assertTrue(validLow in ProfileSurveyEntity.MIN_WATER_GOAL_ML..ProfileSurveyEntity.MAX_WATER_GOAL_ML)
        assertTrue(validHigh in ProfileSurveyEntity.MIN_WATER_GOAL_ML..ProfileSurveyEntity.MAX_WATER_GOAL_ML)
        assertFalse(tooLow in ProfileSurveyEntity.MIN_WATER_GOAL_ML..ProfileSurveyEntity.MAX_WATER_GOAL_ML)
        assertFalse(tooHigh in ProfileSurveyEntity.MIN_WATER_GOAL_ML..ProfileSurveyEntity.MAX_WATER_GOAL_ML)
    }

    @Test
    fun testFreeTextLengthBounded() {
        val longText = "A".repeat(150)
        val boundedName = longText.take(ProfileSurveyEntity.MAX_NAME_LENGTH)
        val boundedDetail = longText.take(ProfileSurveyEntity.MAX_DETAIL_LENGTH)

        assertEquals(ProfileSurveyEntity.MAX_NAME_LENGTH, boundedName.length)
        assertEquals(ProfileSurveyEntity.MAX_DETAIL_LENGTH, boundedDetail.length)
    }

    @Test
    fun testSurveyExportImportRoundTrip() = runBlocking {
        // Setup Work profile and its survey
        val workProfileId = repository.createProfile(name = "Work", emoji = "💼", colorHex = 0xFF8B5CF6, type = ProfileEntity.TYPE_WORK)
        val workSurvey = ProfileSurveyEntity(
            profileId = workProfileId,
            displayName = "Professional",
            gender = "Prefer not to say",
            sleepingTime = "23:00",
            dailyWaterGoalMl = 3000,
            discoverySource = "GitHub",
            discoverySocialPlatform = null,
            discoveryOtherDetail = "Saw on trending repos",
            isCompleted = true,
            isSkipped = false
        )
        repository.saveSurvey(workSurvey)

        val exportedJson = DataExportImportManager.exportAllData(database)
        assertTrue(exportedJson.contains("profile_surveys"))
        assertTrue(exportedJson.contains("Saw on trending repos"))

        // Create fresh database and import
        val newDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val importResult = DataExportImportManager.importData(newDb, exportedJson)
        assertTrue(importResult)

        val restored = newDb.profileSurveyDao().getSurveyForProfile(workProfileId)
        assertNotNull(restored)
        assertEquals("Professional", restored?.displayName)
        assertEquals("Prefer not to say", restored?.gender)
        assertEquals("23:00", restored?.sleepingTime)
        assertEquals(3000, restored?.dailyWaterGoalMl)
        assertEquals("GitHub", restored?.discoverySource)
        assertEquals("Saw on trending repos", restored?.discoveryOtherDetail)
        newDb.close()
    }

    @Test
    fun testRealSqliteMigrationV11ToV12() {
        val dbName = "test_migration_v11_v12.db"
        context.deleteDatabase(dbName)

        val factory = FrameworkSQLiteOpenHelperFactory()

        // 1. Create DB at version 11 with profiles table
        val v11Config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(11) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `profiles` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `name` TEXT NOT NULL,
                            `emoji` TEXT NOT NULL DEFAULT '👤',
                            `colorHex` INTEGER NOT NULL DEFAULT 4282168054,
                            `type` TEXT NOT NULL DEFAULT 'PERSONAL',
                            `orderIndex` INTEGER NOT NULL DEFAULT 0,
                            `isDefault` INTEGER NOT NULL DEFAULT 0,
                            `createdAt` INTEGER NOT NULL DEFAULT 0,
                            `updatedAt` INTEGER NOT NULL DEFAULT 0
                        )
                    """.trimIndent())
                    db.execSQL("""
                        INSERT INTO `profiles` (`id`, `name`, `emoji`, `colorHex`, `type`, `orderIndex`, `isDefault`, `createdAt`, `updatedAt`)
                        VALUES (1, 'Personal', '👤', 4282168054, 'PERSONAL', 0, 1, 1730000000000, 1730000000000)
                    """.trimIndent())
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helperV11 = factory.create(v11Config)
        val dbV11 = helperV11.writableDatabase
        dbV11.close()

        // 2. Open at version 12 and apply MIGRATION_11_12
        val v12Config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(12) {
                override fun onCreate(db: SupportSQLiteDatabase) {}

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    if (oldVersion == 11 && newVersion == 12) {
                        MIGRATION_11_12.migrate(db)
                    }
                }
            })
            .build()

        val helperV12 = factory.create(v12Config)
        val dbV12 = helperV12.writableDatabase

        // Verify profile_surveys table exists and supports insertion
        dbV12.execSQL("""
            INSERT INTO `profile_surveys` (`profileId`, `displayName`, `gender`, `sleepingTime`, `dailyWaterGoalMl`, `discoverySource`, `discoverySocialPlatform`, `discoveryOtherDetail`, `isCompleted`, `isSkipped`, `completedAt`, `updatedAt`)
            VALUES (1, 'Tester', 'Female', '22:00', 2200, 'Friends', NULL, NULL, 1, 0, 1730000000000, 1730000000000)
        """)

        val cursor = dbV12.query("SELECT displayName, dailyWaterGoalMl, isCompleted FROM profile_surveys WHERE profileId = 1")
        assertTrue(cursor.moveToFirst())
        assertEquals("Tester", cursor.getString(0))
        assertEquals(2200, cursor.getInt(1))
        assertEquals(1, cursor.getInt(2))
        cursor.close()
        dbV12.close()
        context.deleteDatabase(dbName)
    }
}
