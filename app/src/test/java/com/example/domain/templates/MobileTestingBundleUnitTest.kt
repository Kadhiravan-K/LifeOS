package com.example.domain.templates

import com.example.domain.model.ShortcutType
import com.example.domain.model.TaskPriority
import com.example.ui.screens.templates.TemplateBrowserTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MobileTestingBundleUnitTest {

    @Test
    fun mobileTestingBundle_containsExactly32RecordsAcrossCoreModules() {
        val template = TemplateCatalog.getTemplateById("template_mobile_testing")
        assertNotNull("template_mobile_testing must exist in TemplateCatalog", template)
        val t = template!!

        assertEquals("Must contain exactly 8 tasks", 8, t.tasks.size)
        assertEquals("Must contain exactly 4 habits", 4, t.habits.size)
        assertEquals("Must contain exactly 8 QR quick reminders", 8, t.shortcuts.size)
        assertEquals("Must contain exactly 6 trackers", 6, t.trackers.size)
        assertEquals("Must contain exactly 6 subscriptions", 6, t.subscriptions.size)

        val totalRecords = t.tasks.size + t.habits.size + t.shortcuts.size + t.trackers.size + t.subscriptions.size
        assertEquals("Total operational records in mobile testing bundle must be exactly 32", 32, totalRecords)
    }

    @Test
    fun mobileTestingBundle_tasksSpanAllPrioritiesAndStandardProjects() {
        val template = TemplateCatalog.getTemplateById("template_mobile_testing")!!
        val priorities = template.tasks.map { it.priority }.toSet()

        assertTrue("Must include P1/URGENT tasks", priorities.contains(TaskPriority.URGENT))
        assertTrue("Must include P2/HIGH tasks", priorities.contains(TaskPriority.HIGH))
        assertTrue("Must include P3/MEDIUM tasks", priorities.contains(TaskPriority.MEDIUM))
        assertTrue("Must include P4/LOW tasks", priorities.contains(TaskPriority.LOW))

        val projectNames = template.tasks.mapNotNull { it.projectName }.toSet()
        assertTrue("Must map to standard projects", projectNames.size >= 3)
    }

    @Test
    fun mobileTestingBundle_shortcutsCoverCoreTypesAndDistinctCoords() {
        val template = TemplateCatalog.getTemplateById("template_mobile_testing")!!
        val shortcutTypes = template.shortcuts.map { it.type }.toSet()

        assertTrue("Must include WATER shortcut", shortcutTypes.contains(ShortcutType.WATER))
        assertTrue("Must include TASK shortcuts", shortcutTypes.contains(ShortcutType.TASK))
        assertTrue("Must include HABIT shortcuts", shortcutTypes.contains(ShortcutType.HABIT))

        template.shortcuts.forEach { sc ->
            assertTrue("Shortcut title cannot be blank", sc.title.isNotBlank())
            assertTrue("Shortcut emoji cannot be blank", sc.emoji.isNotBlank())
        }
    }

    @Test
    fun mobileTestingBundle_trackersAndSubscriptionsAreValid() {
        val template = TemplateCatalog.getTemplateById("template_mobile_testing")!!

        template.trackers.forEach { tr ->
            assertTrue("Tracker title cannot be blank", tr.title.isNotBlank())
            assertTrue("Tracker category cannot be blank", tr.category.isNotBlank())
        }

        template.subscriptions.forEach { sub ->
            assertTrue("Subscription title cannot be blank", sub.title.isNotBlank())
            assertTrue("Subscription defaultAmount must be positive", sub.defaultAmount > 0)
        }
    }

    @Test
    fun mobileTestingBundle_registeredInBundlesCatalog() {
        val bundle = TemplateCatalog.allBundles.find { it.id == "bundle_mobile_testing" }
        assertNotNull("bundle_mobile_testing must be registered in allBundles", bundle)
        assertEquals(listOf("template_mobile_testing"), bundle!!.templateIds)
    }

    @Test
    fun unifiedTemplateBrowser_includesAllRequiredTabs() {
        val tabEntries = TemplateBrowserTab.entries.toList()
        assertTrue("Must include INDIVIDUAL tab", tabEntries.contains(TemplateBrowserTab.INDIVIDUAL))
        assertTrue("Must include BUNDLES tab", tabEntries.contains(TemplateBrowserTab.BUNDLES))
        assertTrue("Must include SPECIAL_BUCKET_LISTS tab", tabEntries.contains(TemplateBrowserTab.SPECIAL_BUCKET_LISTS))
        assertTrue("Must include TRACKERS tab", tabEntries.contains(TemplateBrowserTab.TRACKERS))
        assertTrue("Must include SUBSCRIPTIONS tab", tabEntries.contains(TemplateBrowserTab.SUBSCRIPTIONS))
        assertTrue("Must include IMPORTED tab", tabEntries.contains(TemplateBrowserTab.IMPORTED))
    }
}
