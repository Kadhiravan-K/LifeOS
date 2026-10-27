package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class LabeledLinkItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val label: String,
    val url: String
)

val DefaultLinkLabelPresets = listOf(
    "Registration Link",
    "Meeting Link",
    "Location Map",
    "Docs & Notes",
    "Source Link"
)

fun detectLinkIcon(url: String): ImageVector {
    val u = url.lowercase()
    return when {
        u.contains("maps.google") || u.contains("goo.gl/maps") || u.contains("location") -> Icons.Default.Place
        u.contains("youtube.com") || u.contains("youtu.be") -> Icons.Default.PlayCircle
        else -> Icons.Default.Language
    }
}

/**
 * Standardized Labeled Links & URL List Editor with label preset chips,
 * automatic https:// prefixing, link type icon detection (Maps, Video, Web),
 * tap-to-test link opening, and add/remove management.
 */
@Composable
fun LabeledUrlEditor(
    links: List<LabeledLinkItem>,
    onLinksChanged: (List<LabeledLinkItem>) -> Unit,
    modifier: Modifier = Modifier,
    maxLinks: Int = 5
) {
    val context = LocalContext.current
    var newLabel by remember { mutableStateOf("Registration Link") }
    var newUrl by remember { mutableStateOf("") }
    var urlError by remember { mutableStateOf<String?>(null) }

    fun validateAndFormatUrl(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isBlank()) {
            urlError = "URL cannot be blank"
            return null
        }
        val formatted = if (!trimmed.contains("://")) "https://$trimmed" else trimmed
        urlError = null
        return formatted
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Attached Links (${links.size}/$maxLinks)",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Existing Links List
        if (links.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                links.forEach { link ->
                    val typeIcon = remember(link.url) { detectLinkIcon(link.url) }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = typeIcon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = link.label,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = link.url,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // One-Tap Link Tester
                                IconButton(
                                    onClick = {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link.url))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Cannot open URL: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.OpenInNew,
                                        contentDescription = "Test Link",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                IconButton(
                                    onClick = { onLinksChanged(links.filter { it.id != link.id }) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove Link",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Add New Link Form (if under maxLinks)
        if (links.size < maxLinks) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Add Link",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Preset Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DefaultLinkLabelPresets.forEach { preset ->
                            FilterChip(
                                selected = newLabel == preset,
                                onClick = { newLabel = preset },
                                label = { Text(preset, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newUrl,
                        onValueChange = {
                            newUrl = it
                            if (urlError != null) validateAndFormatUrl(it)
                        },
                        label = { Text("URL (e.g. zoom.us/j/..., maps.google.com)") },
                        isError = urlError != null,
                        supportingText = urlError?.let { { Text(it) } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                val formatted = validateAndFormatUrl(newUrl)
                                if (formatted != null) {
                                    onLinksChanged(links + LabeledLinkItem(label = newLabel, url = formatted))
                                    newUrl = ""
                                    urlError = null
                                }
                            },
                            enabled = newUrl.isNotBlank()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Attach Link", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Standard alias for [LabeledUrlEditor] conforming to unified design naming.
 */
@Composable
fun LabeledLinksInput(
    links: List<LabeledLinkItem>,
    onLinksChanged: (List<LabeledLinkItem>) -> Unit,
    modifier: Modifier = Modifier,
    maxLinks: Int = 5
) = LabeledUrlEditor(
    links = links,
    onLinksChanged = onLinksChanged,
    modifier = modifier,
    maxLinks = maxLinks
)
