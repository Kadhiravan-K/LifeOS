package com.example.ui.screens.manual

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ComponentCatalogUnitTest {

    @Test
    fun catalog_totalCount_isExactly47() {
        assertEquals("Master component catalog must contain exactly 47 components", 47, MasterComponentCatalog.items.size)
    }

    @Test
    fun catalog_statusDistribution_matchesRequirements() {
        val existingCount = MasterComponentCatalog.items.count { it.status == ComponentStatus.EXISTING }
        val partialCount = MasterComponentCatalog.items.count { it.status == ComponentStatus.PARTIAL }
        val proposedCount = MasterComponentCatalog.items.count { it.status == ComponentStatus.PROPOSED }

        assertEquals("Expected exactly 45 EXISTING components", 45, existingCount)
        assertEquals("Expected exactly 1 PARTIAL component", 1, partialCount)
        assertEquals("Expected exactly 1 PROPOSED component", 1, proposedCount)
        assertEquals(47, existingCount + partialCount + proposedCount)

        // Validate the specific partial and proposed components
        val partialItem = MasterComponentCatalog.items.first { it.status == ComponentStatus.PARTIAL }
        assertEquals("file_picker_field", partialItem.id)

        val proposedItem = MasterComponentCatalog.items.first { it.status == ComponentStatus.PROPOSED }
        assertEquals("search_bar_field", proposedItem.id)
    }

    @Test
    fun catalog_allFunctionalCategoriesRepresented() {
        val functionalCategories = listOf(
            ComponentCategory.DATE_TIME,
            ComponentCategory.INPUTS_FORMS,
            ComponentCategory.SELECTION_CONTROLS,
            ComponentCategory.VISUAL_STYLING,
            ComponentCategory.TASK_PRODUCTIVITY,
            ComponentCategory.NAVIGATION_LAYOUT,
            ComponentCategory.FEEDBACK_DIALOGS,
            ComponentCategory.SPECIALIZED_LIFEOS
        )

        for (category in functionalCategories) {
            val count = MasterComponentCatalog.items.count { it.category == category }
            assertTrue("Category ${category.displayName} should contain at least 1 component", count > 0)
        }
    }

    @Test
    fun catalog_uniqueIds_noDuplicates() {
        val allIds = MasterComponentCatalog.items.map { it.id }
        val uniqueIds = allIds.toSet()
        assertEquals("All 36 component IDs must be globally unique", allIds.size, uniqueIds.size)
    }

    @Test
    fun catalog_allEntriesHaveCompleteMetadata() {
        for (item in MasterComponentCatalog.items) {
            assertTrue("Component ${item.id} must have non-blank name", item.name.isNotBlank())
            assertTrue("Component ${item.id} must have non-blank description", item.description.isNotBlank())
            assertTrue("Component ${item.id} must have non-blank whenToUse", item.whenToUse.isNotBlank())
            assertTrue("Component ${item.id} must have non-blank sourceFile", item.sourceFile.isNotBlank())
            assertTrue("Component ${item.id} must have non-blank composableName", item.composableName.isNotBlank())
            assertTrue("Component ${item.id} must have non-empty parameters list", item.parameters.isNotEmpty())
            assertTrue("Component ${item.id} must have non-empty usageLocations list", item.usageLocations.isNotEmpty())
            assertTrue("Component ${item.id} must have non-blank reuseRecommendations", item.reuseRecommendations.isNotBlank())
            assertTrue("Component ${item.id} must have non-blank codeExample", item.codeExample.isNotBlank())
            assertTrue("Component ${item.id} must have non-empty tags list", item.tags.isNotEmpty())

            for (param in item.parameters) {
                assertTrue("Parameter in ${item.id} must have non-blank name", param.name.isNotBlank())
                assertTrue("Parameter in ${item.id} must have non-blank type", param.type.isNotBlank())
                assertTrue("Parameter in ${item.id} must have non-blank description", param.description.isNotBlank())
            }
        }
    }

    @Test
    fun catalog_searchFilter_matchesByTitleTagAndComposable() {
        fun searchCatalog(query: String): List<ComponentEntry> {
            val q = query.trim().lowercase()
            if (q.isEmpty()) return MasterComponentCatalog.items
            return MasterComponentCatalog.items.filter { item ->
                item.name.lowercase().contains(q) ||
                item.description.lowercase().contains(q) ||
                item.composableName.lowercase().contains(q) ||
                item.tags.any { it.lowercase().contains(q) } ||
                item.usageLocations.any { it.lowercase().contains(q) }
            }
        }

        // Test time query
        val timeResults = searchCatalog("time")
        assertTrue("Time query should find clock_time_input", timeResults.any { it.id == "clock_time_input" })

        // Test date query
        val dateResults = searchCatalog("date")
        assertTrue("Date query should find date_trigger_field", dateResults.any { it.id == "date_trigger_field" })

        // Test honeycomb query
        val honeycombResults = searchCatalog("honeycomb")
        assertTrue("Honeycomb query should find honeycomb_tile", honeycombResults.any { it.id == "honeycomb_tile" })

        // Test priority query
        val priorityResults = searchCatalog("priority")
        assertTrue("Priority query should find priority_selector_group", priorityResults.any { it.id == "priority_selector_group" })

        // Test empty query returns all 47
        assertEquals(47, searchCatalog("").size)

        // Non-existent search query returns empty
        val nonsenseResults = searchCatalog("xyznonexistentterm999")
        assertTrue("Nonsense search query must return empty", nonsenseResults.isEmpty())
    }

    @Test
    fun catalog_categoryFiltering_worksAccurately() {
        fun filterByCategory(cat: ComponentCategory): List<ComponentEntry> {
            if (cat == ComponentCategory.ALL) return MasterComponentCatalog.items
            return MasterComponentCatalog.items.filter { it.category == cat }
        }

        assertEquals(47, filterByCategory(ComponentCategory.ALL).size)

        val dateTimeItems = filterByCategory(ComponentCategory.DATE_TIME)
        assertTrue(dateTimeItems.isNotEmpty())
        assertTrue(dateTimeItems.all { it.category == ComponentCategory.DATE_TIME })

        val specializedItems = filterByCategory(ComponentCategory.SPECIALIZED_LIFEOS)
        assertTrue(specializedItems.isNotEmpty())
        assertTrue(specializedItems.all { it.category == ComponentCategory.SPECIALIZED_LIFEOS })
    }
}
