package com.example.domain.modules

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entities.ProfileEntity
import com.example.domain.glossary.GlossaryPackJson
import com.example.domain.glossary.GlossaryTermJson
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

/**
 * Comprehensive verification of Feature Module security guarantees, lifecycle states,
 * and profile-isolated personal notes / bookmarks (#2, #8, #10).
 */
@RunWith(RobolectricTestRunner::class)
class FeatureModuleSecurityAndLifecycleUnitTest {

    private lateinit var database: AppDatabase

    @Before
    fun setup() {
        runBlocking {
            val context: android.content.Context = ApplicationProvider.getApplicationContext()
            database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()

            // Seed Profile 1 and Profile 2 for isolation tests
            database.profileDao().insertProfile(
                ProfileEntity(id = 1, name = "Primary Profile", isDefault = true)
            )
            database.profileDao().insertProfile(
                ProfileEntity(id = 2, name = "Secondary Profile", isDefault = false)
            )
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun computeSha256_computesDeterministicAndAccurateDigest() {
        val payload = "LifeOS sovereign offline data module test payload"
        val hash1 = FeatureModuleManager.computeSha256(payload)
        val hash2 = FeatureModuleManager.computeSha256(payload)

        assertEquals(hash1, hash2)
        assertEquals(64, hash1.length)
        assertTrue(hash1.matches(Regex("^[a-f0-9]{64}$")))
    }

    @Test
    fun isSafePath_correctlyValidatesIdentifiersAndRejectsTraversal() {
        assertTrue(FeatureModuleManager.isSafePath("mental_models"))
        assertTrue(FeatureModuleManager.isSafePath("lifeos-core-pack-v1"))
        assertTrue(FeatureModuleManager.isSafePath("stoic_resilience_2026"))

        // Path traversal attempts must be rejected
        assertFalse(FeatureModuleManager.isSafePath("../evil_pack"))
        assertFalse(FeatureModuleManager.isSafePath("packs/../../system"))
        assertFalse(FeatureModuleManager.isSafePath("/root/pack"))
        assertFalse(FeatureModuleManager.isSafePath("\\windows\\pack"))
        assertFalse(FeatureModuleManager.isSafePath("C:/data/pack"))
        assertFalse(FeatureModuleManager.isSafePath("D:\\pack"))
        assertFalse(FeatureModuleManager.isSafePath(""))
        assertFalse(FeatureModuleManager.isSafePath("   "))
    }

    @Test
    fun parseManifest_parsesValidJsonAndEnforcesSecurityRules() {
        val validManifestJson = """
            {
              "id": "habit_neuroscience",
              "name": "Habit Neuroscience",
              "description": "Neurobiology of habit loops",
              "category": "Neuroscience",
              "version": 2,
              "versionName": "2.0.0",
              "author": "James Clear",
              "itemCount": 5,
              "packageSizeBytes": 8192
            }
        """.trimIndent()

        val manifest = FeatureModuleManager.parseManifest(validManifestJson)
        assertEquals("habit_neuroscience", manifest.id)
        assertEquals("Habit Neuroscience", manifest.name)
        assertEquals(2, manifest.version)
        assertEquals("2.0.0", manifest.versionName)
        assertEquals("James Clear", manifest.author)
        assertEquals(5, manifest.itemCount)

        // Traversal ID in JSON must throw SecurityException
        val maliciousManifestJson = """
            {
              "id": "../malicious_module",
              "name": "Exploit Pack"
            }
        """.trimIndent()

        var caughtSecurity = false
        try {
            FeatureModuleManager.parseManifest(maliciousManifestJson)
        } catch (e: SecurityException) {
            caughtSecurity = true
        }
        assertTrue("Manifest with path traversal in ID must throw SecurityException", caughtSecurity)

        // Unsupported schema version must throw IllegalArgumentException
        val futureSchemaJson = """
            {
              "id": "future_pack",
              "name": "Future Pack",
              "schemaVersion": 999
            }
        """.trimIndent()

        var caughtVersion = false
        try {
            FeatureModuleManager.parseManifest(futureSchemaJson)
        } catch (e: IllegalArgumentException) {
            caughtVersion = true
        }
        assertTrue("Unsupported schemaVersion must throw IllegalArgumentException", caughtVersion)
    }

    @Test
    fun installModule_enforcesPackageSizeLimit() = runBlocking {
        val dao = database.glossaryDao()
        val oversizedManifest = ModuleManifest(
            id = "oversized_pack",
            name = "Oversized Pack",
            description = "Too big to be safe",
            category = "Test",
            packageSizeBytes = FeatureModuleManager.MAX_PACKAGE_SIZE_BYTES + 1024L
        )
        val dummyPack = GlossaryPackJson(
            id = "oversized_pack",
            title = "Oversized",
            description = "Oversized dummy",
            category = "Test",
            terms = emptyList()
        )

        val result = FeatureModuleManager.installModule(
            dao = dao,
            manifest = oversizedManifest,
            packJson = dummyPack
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("exceeds maximum allowed size"))
    }

    @Test
    fun installModule_verifiesSha256ChecksumIntegrity() = runBlocking {
        val dao = database.glossaryDao()
        val manifest = ModuleManifest(
            id = "agile_principles",
            name = "Agile Principles",
            description = "Software engineering principles",
            category = "Engineering",
            version = 1
        )
        val packJson = GlossaryPackJson(
            id = "agile_principles",
            title = "Agile Principles",
            description = "Engineering paradigms",
            category = "Engineering",
            terms = listOf(
                GlossaryTermJson(
                    term = "YAGNI",
                    category = "Engineering",
                    definition = "You aren't gonna need it",
                    detailedExplanation = "Avoid speculative debt."
                )
            )
        )
        val rawContent = "{\"id\":\"agile_principles\"}"
        val correctHash = FeatureModuleManager.computeSha256(rawContent)
        val invalidHash = "deadbeef1234567890abcdefdeadbeef1234567890abcdefdeadbeef12345678"

        // 1. Mismatched SHA-256 fails and does not install
        val failureResult = FeatureModuleManager.installModule(
            dao = dao,
            manifest = manifest,
            packJson = packJson,
            expectedSha256 = invalidHash,
            rawContent = rawContent
        )
        assertTrue("Installation must fail when hash mismatches", failureResult.isFailure)
        assertNull(dao.getPackById("agile_principles"))

        // 2. Matching SHA-256 succeeds
        val successResult = FeatureModuleManager.installModule(
            dao = dao,
            manifest = manifest,
            packJson = packJson,
            expectedSha256 = correctHash,
            rawContent = rawContent
        )
        assertTrue("Installation must succeed when hash matches", successResult.isSuccess)
        val installedModule = successResult.getOrNull()
        assertNotNull(installedModule)
        assertEquals(ModuleStatus.INSTALLED, installedModule!!.status)
        assertTrue(installedModule.isVerified)

        val packEntity = dao.getPackById("agile_principles")
        assertNotNull(packEntity)
        assertEquals("Agile Principles", packEntity!!.title)
        assertEquals(1, packEntity.itemCount)

        val items = dao.getItemsForPackFlow("agile_principles").first()
        assertEquals(1, items.size)
        assertEquals("YAGNI", items[0].term)
    }

    @Test
    fun installAndUninstallModule_cascadesDeletionProperly() = runBlocking {
        val dao = database.glossaryDao()
        val manifest = ModuleManifest(
            id = "stoic_resilience",
            name = "Stoic Resilience",
            description = "Emotional resilience",
            category = "Philosophy",
            version = 1
        )
        val packJson = GlossaryPackJson(
            id = "stoic_resilience",
            title = "Stoic Resilience",
            description = "Emotional resilience",
            category = "Philosophy",
            terms = listOf(
                GlossaryTermJson(term = "Amor Fati", category = "Philosophy", definition = "Love of fate"),
                GlossaryTermJson(term = "Dichotomy of Control", category = "Philosophy", definition = "Focus on what you control")
            )
        )

        // Install
        val installRes = FeatureModuleManager.installModule(dao, manifest, packJson)
        assertTrue(installRes.isSuccess)
        assertNotNull(dao.getPackById("stoic_resilience"))
        assertEquals(2, dao.getItemsForPackFlow("stoic_resilience").first().size)

        // Uninstall
        val uninstallRes = FeatureModuleManager.uninstallModule(dao, "stoic_resilience")
        assertTrue(uninstallRes.isSuccess)
        assertNull(dao.getPackById("stoic_resilience"))
        assertTrue(dao.getItemsForPackFlow("stoic_resilience").first().isEmpty())
    }

    @Test
    fun resolveAllModules_accuratelyDetectsInstallationAndUpdates() = runBlocking {
        val dao = database.glossaryDao()

        // Initially no packs installed: all 5 curated modules should be AVAILABLE
        val initialModules = FeatureModuleManager.resolveAllModules(dao)
        assertEquals(5, initialModules.size)
        assertTrue(initialModules.all { it.status == ModuleStatus.AVAILABLE })

        // Install core pack
        val (coreManifest, corePackJson) = FeatureModuleManager.CURATED_CATALOG[0]
        FeatureModuleManager.installModule(dao, coreManifest, corePackJson)

        // Now core pack should report INSTALLED, others AVAILABLE
        val afterInstall = FeatureModuleManager.resolveAllModules(dao)
        val coreModule = afterInstall.first { it.id == coreManifest.id }
        assertEquals(ModuleStatus.INSTALLED, coreModule.status)
        assertEquals(coreManifest.version, coreModule.installedVersion)
        assertTrue(coreModule.isVerified)

        val uninstalledModules = afterInstall.filter { it.id != coreManifest.id }
        assertTrue(uninstalledModules.all { it.status == ModuleStatus.AVAILABLE })
    }

    @Test
    fun profileIsolation_bookmarksAndPersonalNotesAreStrictlyProfileScoped() = runBlocking {
        val dao = database.glossaryDao()

        // 1. Install pack
        val (coreManifest, corePackJson) = FeatureModuleManager.CURATED_CATALOG[0]
        FeatureModuleManager.installModule(dao, coreManifest, corePackJson)
        val items = dao.getItemsForPackFlow(coreManifest.id).first()
        assertTrue(items.isNotEmpty())
        val termId = items[0].id

        // 2. Profile 1 bookmarks term and writes personal note
        dao.setBookmark(profileId = 1, termId = termId, isBookmarked = true)
        dao.setPersonalNote(profileId = 1, termId = termId, note = "Deep work block at 9 AM daily.")

        // Verify Profile 1 data
        val profile1Data = dao.getUserDataForProfileFlow(profileId = 1).first().associateBy { it.termId }
        assertTrue("Profile 1 must have term bookmarked", profile1Data[termId]?.isBookmarked == true)
        assertEquals("Deep work block at 9 AM daily.", profile1Data[termId]?.personalNote)

        // Verify Profile 2 has ZERO data for this term (strict isolation)
        val profile2Data = dao.getUserDataForProfileFlow(profileId = 2).first().associateBy { it.termId }
        assertNull("Profile 2 must not see Profile 1 bookmark or note", profile2Data[termId])

        // 3. Profile 2 sets its own independent note
        dao.setPersonalNote(profileId = 2, termId = termId, note = "Profile 2 distinct note.")
        val profile2Updated = dao.getUserDataForProfileFlow(profileId = 2).first().associateBy { it.termId }
        assertEquals("Profile 2 distinct note.", profile2Updated[termId]?.personalNote)
        assertFalse("Profile 2 bookmark should default to false", profile2Updated[termId]?.isBookmarked == true)

        // Verify Profile 1's note was completely undisturbed
        val profile1Check = dao.getUserDataForProfileFlow(profileId = 1).first().associateBy { it.termId }
        assertEquals("Deep work block at 9 AM daily.", profile1Check[termId]?.personalNote)
        assertTrue(profile1Check[termId]?.isBookmarked == true)
    }
}
