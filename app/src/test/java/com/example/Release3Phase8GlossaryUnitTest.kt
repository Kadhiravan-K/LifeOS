package com.example

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entities.GlossaryItemEntity
import com.example.data.local.entities.GlossaryPackEntity
import com.example.domain.glossary.GlossaryModuleManager
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

@RunWith(RobolectricTestRunner::class)
class Release3Phase8GlossaryUnitTest {

    private lateinit var database: AppDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun bundledCorePack_hasValidStructureAndIntegrity() {
        val corePack = GlossaryModuleManager.BUNDLED_CORE_PACK
        assertEquals("lifeos_core", corePack.id)
        assertEquals("LifeOS Productivity & Systems Thinking", corePack.title)
        assertEquals(8, corePack.terms.size)

        // Verify key productivity principles exist
        val termNames = corePack.terms.map { it.term }.toSet()
        assertTrue(termNames.contains("Deep Work"))
        assertTrue(termNames.contains("Time Blocking"))
        assertTrue(termNames.contains("Pareto Principle (80/20 Rule)"))
        assertTrue(termNames.contains("Parkinson's Law"))
        assertTrue(termNames.contains("Eisenhower Matrix"))
        assertTrue(termNames.contains("Habit Stacking"))
        assertTrue(termNames.contains("Two-Minute Rule"))
        assertTrue(termNames.contains("Flow State"))

        for (term in corePack.terms) {
            assertTrue(term.term.isNotBlank())
            assertTrue(term.definition.isNotBlank())
            assertTrue(term.category.isNotBlank())
            assertTrue(term.detailedExplanation.isNotBlank())
            assertTrue(term.tags.isNotEmpty())
        }
    }

    @Test
    fun jsonPackParser_parsesValidPackAndRejectsCorruptedJson() {
        val validJson = """
            {
              "id": "deep_work_principles",
              "title": "Deep Work Mastery",
              "category": "Focus",
              "version": 1,
              "description": "High productivity focus techniques",
              "author": "LifeOS Curators",
              "terms": [
                {
                  "term": "Attention Residue",
                  "category": "Focus",
                  "definition": "The mental lag when switching tasks.",
                  "detailedExplanation": "Group similar tasks together to minimize context switching overhead.",
                  "examples": ["Batching emails"],
                  "relatedTerms": ["Context Switching", "Batching"],
                  "tags": ["focus", "productivity"]
                }
              ]
            }
        """.trimIndent()

        val parsed = GlossaryModuleManager.parsePackJson(validJson)
        assertNotNull(parsed)
        assertEquals("deep_work_principles", parsed.id)
        assertEquals("Deep Work Mastery", parsed.title)
        assertEquals(1, parsed.terms.size)
        assertEquals("Attention Residue", parsed.terms[0].term)
        assertEquals(listOf("focus", "productivity"), parsed.terms[0].tags)

        // Test checksum calculation & verification
        val checksum = GlossaryModuleManager.computeSha256(validJson)
        assertTrue(checksum.isNotBlank())
        assertEquals(checksum, GlossaryModuleManager.computeSha256(validJson))

        // Invalid JSON throws IllegalArgumentException
        var errorThrown = false
        try {
            GlossaryModuleManager.parsePackJson("{ invalid: json ")
        } catch (e: IllegalArgumentException) {
            errorThrown = true
        }
        assertTrue("Corrupted JSON must throw IllegalArgumentException", errorThrown)
    }

    @Test
    fun installPack_withChecksumValidation() = runBlocking {
        val dao = database.glossaryDao()

        val rawJson = """
            {
              "id": "systems_thinking",
              "title": "Systems Thinking",
              "category": "Systems",
              "version": 1,
              "description": "Frameworks for complex systems",
              "author": "Donella Meadows",
              "terms": [
                {
                  "term": "Feedback Loop",
                  "category": "Systems",
                  "definition": "A mechanism where an output feeds back into an input.",
                  "detailedExplanation": "Reinforcing and balancing loops shape system behavior.",
                  "examples": ["Compound interest", "Thermostat heating"],
                  "relatedTerms": ["Leverage Point"],
                  "tags": ["systems", "mental-models"]
                }
              ]
            }
        """.trimIndent()

        val parsed = GlossaryModuleManager.parsePackJson(rawJson)
        val validSha256 = GlossaryModuleManager.computeSha256(rawJson)

        // 1. Install with mismatched checksum should fail
        val failedResult = GlossaryModuleManager.installPack(
            dao = dao,
            packJson = parsed,
            expectedSha256 = "invalid_hash_value_12345",
            rawJsonContent = rawJson
        )
        assertTrue(failedResult.isFailure)

        // 2. Install with matching checksum should succeed
        val successResult = GlossaryModuleManager.installPack(
            dao = dao,
            packJson = parsed,
            expectedSha256 = validSha256,
            rawJsonContent = rawJson
        )
        assertTrue(successResult.isSuccess)

        // Verify installed pack in DAO
        val pack = dao.getPackById("systems_thinking")
        assertNotNull(pack)
        assertEquals("Systems Thinking", pack!!.title)
        assertEquals(1, pack.itemCount)

        // Verify installed items in DAO
        val items = dao.getItemsForPackFlow("systems_thinking").first()
        assertEquals(1, items.size)
        assertEquals("Feedback Loop", items[0].term)

        // 3. Uninstall pack cascades to items
        GlossaryModuleManager.uninstallPack(dao, "systems_thinking")
        assertNull(dao.getPackById("systems_thinking"))
        val remainingItems = dao.getItemsForPackFlow("systems_thinking").first()
        assertTrue(remainingItems.isEmpty())
    }

    @Test
    fun glossaryDao_crudOperationsAndSearch() = runBlocking {
        val dao = database.glossaryDao()

        // 1. Insert pack
        val pack = GlossaryPackEntity(
            id = "test_pack",
            title = "Test Pack",
            version = 1,
            description = "Pack for unit tests",
            category = "Testing",
            author = "Tester",
            itemCount = 2,
            sha256 = "mock_hash",
            isInstalled = true,
            installedAt = 1000L,
            sizeBytes = 500L
        )
        dao.insertPack(pack)

        val fetchedPack = dao.getPackById("test_pack")
        assertNotNull(fetchedPack)
        assertEquals("Test Pack", fetchedPack!!.title)

        // 2. Insert items
        val item1 = GlossaryItemEntity(
            packId = "test_pack",
            term = "Alpha Term",
            phonetic = "/ˈælfə/",
            category = "General",
            definition = "Definition of Alpha",
            detailedExplanation = "Apply Alpha daily for optimal outcomes.",
            examplesJson = "[\"Alpha example\"]",
            relatedTermsJson = "[\"Beta\"]",
            tagsJson = "[\"test\", \"alpha\"]",
            isBookmarked = false
        )
        val item2 = GlossaryItemEntity(
            packId = "test_pack",
            term = "Beta Principle",
            phonetic = null,
            category = "Advanced",
            definition = "Definition of Beta",
            detailedExplanation = "Apply Beta weekly.",
            examplesJson = "[]",
            relatedTermsJson = "[]",
            tagsJson = "[\"test\", \"beta\"]",
            isBookmarked = false
        )
        dao.insertItems(listOf(item1, item2))

        val items = dao.getItemsForPackFlow("test_pack").first()
        assertEquals(2, items.size)

        // 3. Search items
        val searchAlpha = dao.searchItemsFlow("Alpha").first()
        assertEquals(1, searchAlpha.size)
        assertEquals("Alpha Term", searchAlpha[0].term)

        val searchTag = dao.searchItemsFlow("beta").first()
        assertEquals(1, searchTag.size)
        assertEquals("Beta Principle", searchTag[0].term)

        // 4. Bookmark toggle
        val firstItemId = items[0].id
        dao.toggleBookmark(firstItemId, true)
        val bookmarkedItems = dao.getBookmarkedItemsFlow().first()
        assertEquals(1, bookmarkedItems.size)
        assertEquals(firstItemId, bookmarkedItems[0].id)

        // 5. Cascade delete pack
        dao.deletePack("test_pack")
        assertNull(dao.getPackById("test_pack"))
        val remainingItems = dao.getItemsForPackFlow("test_pack").first()
        assertTrue("Cascade deletion must remove all items belonging to pack", remainingItems.isEmpty())
    }
}
