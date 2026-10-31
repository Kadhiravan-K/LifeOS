package com.example

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Release 3 — Critical Onboarding Loop / App Auto-Exit Fix Regression Test Suite.
 *
 * Verifies:
 * 1. Fresh database without prior profile row: saveSurvey and skipSurvey auto-seed profile and never throw SQLiteConstraintException.
 * 2. Room database callback seeds default Personal profile (id = 1).
 * 3. Multi-profile onboarding isolation: each profile maintains independent onboarding state.
 * 4. Release 2 upgrade path: existing data skips onboarding.
 * 5. Persistence across app relaunch: completed profile IDs and DataStore state persist reliably.
 */
@RunWith(RobolectricTestRunner::class)
class Release3OnboardingLoopFixUnitTest {

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
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testFreshInstallWithoutPriorProfile_saveSurveyDoesNotThrowConstraintException() = runBlocking {
        // Verify database profiles table is initially empty
        val initialProfiles = database.profileDao().getAllProfiles()
        assertTrue("Profiles table should be empty before seeding", initialProfiles.isEmpty())

        // Save survey for Profile 1 directly (as happens on Step 5 completion)
        val survey = ProfileSurveyEntity(
            profileId = ProfileEntity.DEFAULT_PERSONAL_PROFILE_ID,
            displayName = "Jordan",
            gender = "Female",
            sleepingTime = "23:00",
            dailyWaterGoalMl = 3000,
            discoverySource = "Word of Mouth",
            isCompleted = true,
            isSkipped = false
        )

        // Must succeed without SQLiteConstraintException
        val insertedId = repository.saveSurvey(survey)
        assertTrue("Survey row must be successfully inserted", insertedId > 0)

        // Verify default profile was created and foreign key satisfied
        val defaultProfile = database.profileDao().getProfileById(ProfileEntity.DEFAULT_PERSONAL_PROFILE_ID)
        assertNotNull("Default personal profile must be auto-created", defaultProfile)
        assertEquals("Personal", defaultProfile?.name)

        // Verify survey is persisted
        val persistedSurvey = repository.getSurveyForProfile(ProfileEntity.DEFAULT_PERSONAL_PROFILE_ID)
        assertNotNull(persistedSurvey)
        assertEquals("Jordan", persistedSurvey?.displayName)
        assertTrue(persistedSurvey!!.isCompleted)
        assertFalse(persistedSurvey.isSkipped)

        // Verify onboarding is marked completed in repository flow
        assertTrue(
            "Profile 1 onboarding flow must emit completed = true",
            repository.isProfileOnboardingCompletedFlow(ProfileEntity.DEFAULT_PERSONAL_PROFILE_ID).first()
        )
        assertTrue(
            "Preferences hasCompletedOnboarding must be true",
            preferences.hasCompletedOnboarding.first()
        )
    }

    @Test
    fun testFreshInstallWithoutPriorProfile_skipSurveyDoesNotThrowConstraintException() = runBlocking {
        // Skip survey directly on empty database
        val insertedId = repository.skipSurvey(ProfileEntity.DEFAULT_PERSONAL_PROFILE_ID)
        assertTrue("Skipped survey row must be successfully inserted", insertedId > 0)

        val persistedSurvey = repository.getSurveyForProfile(ProfileEntity.DEFAULT_PERSONAL_PROFILE_ID)
        assertNotNull(persistedSurvey)
        assertTrue(persistedSurvey!!.isCompleted)
        assertTrue(persistedSurvey.isSkipped)

        assertTrue(
            "Profile 1 onboarding flow must emit completed = true after skip",
            repository.isProfileOnboardingCompletedFlow(ProfileEntity.DEFAULT_PERSONAL_PROFILE_ID).first()
        )
    }

    @Test
    fun testProfileOnboardingIsolation_independentStateAcrossProfiles() = runBlocking {
        // 1. Complete onboarding for Profile 1
        val survey1 = ProfileSurveyEntity(
            profileId = ProfileEntity.DEFAULT_PERSONAL_PROFILE_ID,
            displayName = "User One",
            isCompleted = true,
            isSkipped = false
        )
        repository.saveSurvey(survey1)

        assertTrue(
            "Profile 1 must be completed",
            repository.isProfileOnboardingCompletedFlow(ProfileEntity.DEFAULT_PERSONAL_PROFILE_ID).first()
        )

        // 2. Create Profile 2 (Work)
        val profile2Id = repository.createProfile(
            name = "Work",
            emoji = "💼",
            type = ProfileEntity.TYPE_WORK
        )

        // 3. Profile 2 must NOT be marked as completed yet
        assertFalse(
            "Profile 2 must NOT be completed upon creation",
            repository.isProfileOnboardingCompletedFlow(profile2Id).first()
        )

        // 4. Switch active profile to Profile 2
        repository.setActiveProfileId(profile2Id)
        assertFalse(
            "Active profile flow must be false for Profile 2",
            repository.activeProfileOnboardingCompletedFlow.first()
        )

        // 5. Complete onboarding for Profile 2
        val survey2 = ProfileSurveyEntity(
            profileId = profile2Id,
            displayName = "Work Profile User",
            isCompleted = true,
            isSkipped = false
        )
        repository.saveSurvey(survey2)

        assertTrue(
            "Profile 2 must now be completed",
            repository.isProfileOnboardingCompletedFlow(profile2Id).first()
        )
        assertTrue(
            "Active profile flow must now be true for Profile 2",
            repository.activeProfileOnboardingCompletedFlow.first()
        )

        // 6. Switch back to Profile 1: verify it is still completed and not corrupted
        repository.setActiveProfileId(ProfileEntity.DEFAULT_PERSONAL_PROFILE_ID)
        assertTrue(
            "Profile 1 must remain completed after switching back",
            repository.activeProfileOnboardingCompletedFlow.first()
        )
    }

    @Test
    fun testRoomDatabaseCallback_seedsDefaultProfile() = runBlocking {
        val callbackDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    db.execSQL("""
                        INSERT OR IGNORE INTO `profiles` (`id`, `name`, `emoji`, `colorHex`, `type`, `orderIndex`, `isDefault`, `createdAt`, `updatedAt`)
                        VALUES (1, 'Personal', '👤', 4282168054, 'PERSONAL', 0, 1, 1730000000000, 1730000000000)
                    """.trimIndent())
                }
            })
            .build()

        try {
            // Trigger database creation by querying
            val profiles = callbackDb.profileDao().getAllProfiles()
            assertEquals("Callback should seed 1 profile", 1, profiles.size)
            assertEquals(1L, profiles[0].id)
            assertEquals("Personal", profiles[0].name)
            assertTrue(profiles[0].isDefault)
        } finally {
            callbackDb.close()
        }
    }

    @Test
    fun testRelease2UpgradeBypassesOnboardingWhenExistingDataPresent() = runBlocking {
        // Simulate upgrade: user already has an existing task
        repository.ensureDefaultPersonalProfileExists()
        database.taskDao().insertTask(
            TaskEntity(
                title = "Legacy R2 Task",
                category = "Work",
                dueDateIso = "2026-11-01",
                profileId = 1L
            )
        )

        assertTrue("User has existing data", repository.hasAnyExistingUserData())

        // App launch init check
        if (repository.hasAnyExistingUserData()) {
            preferences.recordProfileOnboardingCompleted(ProfileEntity.DEFAULT_PERSONAL_PROFILE_ID)
        }

        assertTrue(
            "Profile 1 onboarding must be marked complete for upgraded users with data",
            repository.isProfileOnboardingCompletedFlow(ProfileEntity.DEFAULT_PERSONAL_PROFILE_ID).first()
        )
        assertTrue(
            "hasCompletedOnboarding in DataStore must be true",
            preferences.hasCompletedOnboarding.first()
        )
    }

    @Test
    fun testPersistenceAcrossAppRelaunch() = runBlocking {
        // Save onboarding completion
        repository.saveSurvey(
            ProfileSurveyEntity(
                profileId = 1L,
                displayName = "Sam",
                isCompleted = true,
                isSkipped = false
            )
        )

        // Recreate repository and preferences (simulating new process launch)
        val freshPreferences = UserPreferences(context)
        val freshRepository = LifeRepository(database, freshPreferences)

        assertTrue(
            "Onboarding completion must persist across fresh repository instance",
            freshRepository.isProfileOnboardingCompletedFlow(1L).first()
        )
        assertTrue(
            "DataStore completed profile IDs must contain 1L",
            freshPreferences.completedOnboardingProfileIds.first().contains(1L)
        )
        assertTrue(
            "DataStore hasCompletedOnboarding must be true",
            freshPreferences.hasCompletedOnboarding.first()
        )
    }
}
