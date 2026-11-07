package com.example.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * Phase 5 Reusable Heatmap Date Visibility Control.
 * Toggles date number labels inside heatmap squares.
 * Eye ON: Show date numbers inside squares.
 * Eye OFF: Hide date numbers (default).
 */
@Composable
fun HeatmapVisibilityEyeButton(
    showDates: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "heatmap_eye_toggle_btn"
) {
    IconButton(
        onClick = { onToggle(!showDates) },
        modifier = modifier
            .size(36.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = if (showDates) Icons.Default.Visibility else Icons.Default.VisibilityOff,
            contentDescription = if (showDates) "Hide date numbers on heatmap" else "Show date numbers on heatmap",
            tint = if (showDates) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
