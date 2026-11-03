package com.example.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.engine.ThemeDefinition

/**
 * Redesigned 5-Position Bottom Navigation Bar with elevated floating center Quick-Add (+) button,
 * semicircular cradle cutout, and customizable Slots 2 and 4.
 *
 * Positions:
 * 1: Home (Fixed)
 * 2: Customizable Destination A
 * 3: Centre Quick Add (+) (Fixed, elevated, themed)
 * 4: Customizable Destination B
 * 5: Settings (Fixed)
 */
@Composable
fun LifeOSBottomNavigation(
    currentDestination: NavigationItem,
    slot2Item: NavigationItem,
    slot4Item: NavigationItem,
    activeTheme: ThemeDefinition,
    isThemeAnimationEnabled: Boolean,
    onItemSelected: (NavigationItem) -> Unit,
    onQuickAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val barHeight = 64.dp
    val cutoutRadius = 32.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("lifeos_bottom_navigation_container"),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Cutout Navigation Bar Surface
        Surface(
            shape = BottomNavigationCutoutShape(cutoutRadius = cutoutRadius),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .height(barHeight)
                .testTag("bottom_navigation_bar")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(barHeight)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // Position 1: Home (Fixed)
                BottomNavItem(
                    item = NavigationItem.HOME,
                    isSelected = currentDestination == NavigationItem.HOME,
                    onClick = { onItemSelected(NavigationItem.HOME) },
                    modifier = Modifier.weight(1f)
                )

                // Position 2: Configurable Destination A
                BottomNavItem(
                    item = slot2Item,
                    isSelected = currentDestination == slot2Item,
                    onClick = { onItemSelected(slot2Item) },
                    modifier = Modifier.weight(1f)
                )

                // Position 3 Cradle Spacing (Cutout clearance for floating FAB)
                Spacer(modifier = Modifier.size(cutoutRadius * 2 + 12.dp))

                // Position 4: Configurable Destination B
                BottomNavItem(
                    item = slot4Item,
                    isSelected = currentDestination == slot4Item,
                    onClick = { onItemSelected(slot4Item) },
                    modifier = Modifier.weight(1f)
                )

                // Position 5: Settings (Fixed)
                BottomNavItem(
                    item = NavigationItem.SETTINGS,
                    isSelected = currentDestination == NavigationItem.SETTINGS,
                    onClick = { onItemSelected(NavigationItem.SETTINGS) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Floating Elevated Centre Quick-Add Button inside Semicircular Cutout Cradle
        Box(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .offset(y = -(barHeight / 2 - 8.dp))
                .align(Alignment.BottomCenter)
        ) {
            ThemedQuickAddButton(
                theme = activeTheme,
                isAnimationEnabled = isThemeAnimationEnabled,
                onClick = onQuickAddClick
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    item: NavigationItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val primaryColor = MaterialTheme.colorScheme.primary
    val unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = primaryColor.copy(alpha = 0.2f)),
                onClick = onClick
            )
            .padding(vertical = 6.dp)
            .semantics {
                selected = isSelected
                role = Role.Tab
                contentDescription = item.label
            }
            .testTag("nav_item_${item.route}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (isSelected) primaryColor.copy(alpha = 0.15f) else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                contentDescription = null,
                tint = if (isSelected) primaryColor else unselectedColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = item.label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) primaryColor else unselectedColor,
            maxLines = 1
        )
    }
}
