package com.example.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationStateManagerUnitTest {

    @Test
    fun testInitialState() {
        val manager = NavigationStateManager(NavigationItem.HOME)
        assertEquals(NavigationItem.HOME, manager.currentDestination)
        assertEquals(1, manager.backStack.size)
        assertEquals(0, manager.settingsResetTrigger)
    }

    @Test
    fun testNavigateToNewDestination() {
        val manager = NavigationStateManager(NavigationItem.HOME)
        manager.navigateTo(NavigationItem.SETTINGS)
        assertEquals(NavigationItem.SETTINGS, manager.currentDestination)
        assertEquals(2, manager.backStack.size)
    }

    @Test
    fun testPreventAdjacentDuplicates() {
        val manager = NavigationStateManager(NavigationItem.HOME)
        manager.navigateTo(NavigationItem.SETTINGS)
        manager.navigateTo(NavigationItem.SETTINGS)
        assertEquals(2, manager.backStack.size)
        assertEquals(NavigationItem.SETTINGS, manager.currentDestination)
    }

    @Test
    fun testNavigateHomeFromThemes_BugFixVerification() {
        // Reproduces the exact bug reported in the prompt:
        // Open Settings -> Navigate to Themes -> Tap Home in bottom nav -> Home MUST open!
        val manager = NavigationStateManager(NavigationItem.HOME)
        manager.onBottomNavSelected(NavigationItem.SETTINGS)
        assertEquals(NavigationItem.SETTINGS, manager.currentDestination)

        manager.navigateTo(NavigationItem.THEMES)
        assertEquals(NavigationItem.THEMES, manager.currentDestination)

        // User taps Home in bottom navigation
        manager.onBottomNavSelected(NavigationItem.HOME)
        assertEquals(NavigationItem.HOME, manager.currentDestination)
        assertEquals(1, manager.backStack.size)
        assertEquals(NavigationItem.HOME, manager.backStack.first())
    }

    @Test
    fun testNavigateSettingsFromThemes() {
        val manager = NavigationStateManager(NavigationItem.HOME)
        manager.onBottomNavSelected(NavigationItem.SETTINGS)
        manager.navigateTo(NavigationItem.THEMES)

        manager.onBottomNavSelected(NavigationItem.SETTINGS)
        assertEquals(NavigationItem.SETTINGS, manager.currentDestination)
    }

    @Test
    fun testTabReselectionIncrementsResetTrigger() {
        val manager = NavigationStateManager(NavigationItem.HOME)
        manager.onBottomNavSelected(NavigationItem.SETTINGS)
        assertEquals(0, manager.settingsResetTrigger)

        // Reselecting SETTINGS tab while already in SETTINGS
        manager.onBottomNavSelected(NavigationItem.SETTINGS)
        assertEquals(1, manager.settingsResetTrigger)

        manager.onBottomNavSelected(NavigationItem.SETTINGS)
        assertEquals(2, manager.settingsResetTrigger)
    }

    @Test
    fun testPopBackStack() {
        val manager = NavigationStateManager(NavigationItem.HOME)
        manager.navigateTo(NavigationItem.TASKS)
        manager.navigateTo(NavigationItem.HABITS)

        assertEquals(NavigationItem.HABITS, manager.currentDestination)
        assertTrue(manager.popBack())
        assertEquals(NavigationItem.TASKS, manager.currentDestination)
        assertTrue(manager.popBack())
        assertEquals(NavigationItem.HOME, manager.currentDestination)
        assertFalse(manager.popBack()) // At root, cannot pop further
        assertEquals(NavigationItem.HOME, manager.currentDestination)
    }

    @Test
    fun testCustomizableDestinationSlotResolution() {
        val slot2 = NavigationItem.fromSlotKey("TASKS", NavigationItem.TASKS)
        assertEquals(NavigationItem.TASKS, slot2)

        val slot4 = NavigationItem.fromSlotKey("CALENDAR", NavigationItem.CALENDAR)
        assertEquals(NavigationItem.CALENDAR, slot4)

        // Invalid key falls back safely
        val fallback = NavigationItem.fromSlotKey("INVALID_KEY", NavigationItem.CALENDAR)
        assertEquals(NavigationItem.CALENDAR, fallback)

        // HOME or SETTINGS cannot be chosen as customizable slots
        val homeAttempt = NavigationItem.fromSlotKey("HOME", NavigationItem.TASKS)
        assertEquals(NavigationItem.TASKS, homeAttempt)

        // SHIFTS and GIVE_GET can be chosen as customizable slots
        val shiftsSlot = NavigationItem.fromSlotKey("SHIFTS", NavigationItem.TASKS)
        assertEquals(NavigationItem.SHIFTS, shiftsSlot)

        val giveGetSlot = NavigationItem.fromSlotKey("GIVE_GET", NavigationItem.CALENDAR)
        assertEquals(NavigationItem.GIVE_GET, giveGetSlot)

        assertTrue(NavigationItem.customizableDestinations.contains(NavigationItem.SHIFTS))
        assertTrue(NavigationItem.customizableDestinations.contains(NavigationItem.GIVE_GET))
    }
}
