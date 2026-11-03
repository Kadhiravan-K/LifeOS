package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/**
 * Centralized, production-grade navigation state manager for LifeOS.
 *
 * Guarantees:
 * - Robust back-stack history traversal.
 * - Tab reselection resets nested subpages to root.
 * - Bottom navigation destinations (Home, Slot 2, Slot 4, Settings) are reachable from all screens.
 * - State survives configuration changes and Activity recreation.
 */
class NavigationStateManager(
    initialDestination: NavigationItem = NavigationItem.HOME,
    initialStack: List<NavigationItem> = listOf(initialDestination)
) {
    val backStack = mutableStateListOf<NavigationItem>().apply {
        addAll(initialStack.ifEmpty { listOf(initialDestination) })
    }

    val currentDestination: NavigationItem
        get() = backStack.lastOrNull() ?: NavigationItem.HOME

    var settingsResetTrigger by mutableIntStateOf(0)
        private set

    fun navigateTo(item: NavigationItem) {
        if (currentDestination != item) {
            backStack.add(item)
        }
    }

    fun onBottomNavSelected(item: NavigationItem) {
        if (currentDestination == item) {
            // Tab reselection: reset nested screens to root (e.g. Settings back to HUB)
            if (item == NavigationItem.SETTINGS) {
                settingsResetTrigger++
            }
            // Pop any sub-destinations that were on top of this tab
            while (backStack.size > 1 && backStack.last() != item) {
                backStack.removeAt(backStack.size - 1)
            }
        } else {
            // Material 3 bottom navigation pattern:
            // Home is the root of the app.
            if (item == NavigationItem.HOME) {
                backStack.clear()
                backStack.add(NavigationItem.HOME)
            } else {
                backStack.clear()
                backStack.add(NavigationItem.HOME)
                backStack.add(item)
            }
        }
    }

    fun popBack(): Boolean {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.size - 1)
            return true
        }
        return false
    }

    companion object {
        val Saver: Saver<NavigationStateManager, List<String>> = Saver(
            save = { manager ->
                manager.backStack.map { it.name }
            },
            restore = { savedNames ->
                val items = savedNames.mapNotNull { name ->
                    try {
                        NavigationItem.valueOf(name)
                    } catch (e: Exception) {
                        null
                    }
                }
                NavigationStateManager(
                    initialDestination = items.lastOrNull() ?: NavigationItem.HOME,
                    initialStack = items.ifEmpty { listOf(NavigationItem.HOME) }
                )
            }
        )
    }
}

@Composable
fun rememberNavigationStateManager(
    initialDestination: NavigationItem = NavigationItem.HOME
): NavigationStateManager {
    return rememberSaveable(saver = NavigationStateManager.Saver) {
        NavigationStateManager(initialDestination = initialDestination)
    }
}
