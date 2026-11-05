package com.example.ui.screens.templates

import com.example.ui.navigation.NavigationItem
import org.junit.Assert.assertTrue
import org.junit.Test

class UnifiedTemplateBrowserCategoryTest {
    @Test
    fun sharedBrowserIncludesAllSupportedCoreModuleCategories() {
        assertTrue(TemplateBrowserTab.entries.containsAll(
            listOf(
                TemplateBrowserTab.QR_CODES,
                TemplateBrowserTab.TRACKERS,
                TemplateBrowserTab.SUBSCRIPTIONS
            )
        ))
    }

    @Test
    fun qrCodesCanBeOpenedAsACustomNavigationDestination() {
        assertTrue(NavigationItem.QR_CODES in NavigationItem.customizableDestinations)
        assertTrue(
            NavigationItem.fromSlotKey("QR_CODES", NavigationItem.TASKS) == NavigationItem.QR_CODES
        )
    }
}
