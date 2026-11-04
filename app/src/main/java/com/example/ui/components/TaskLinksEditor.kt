package com.example.ui.components

import android.content.ActivityNotFoundException
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import com.example.domain.model.TaskLink
import com.example.domain.model.TaskLinkUrlValidator
import androidx.core.net.toUri
import androidx.compose.ui.unit.dp

@Composable
fun TaskLinksEditor(
    links: List<TaskLink>,
    onLinksChanged: (List<TaskLink>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(modifier = modifier.fillMaxWidth()) {
        links.forEachIndexed { index, link ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_link_item_$index")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = link.label,
                        onValueChange = { label ->
                            onLinksChanged(links.updateAt(index) { it.copy(label = label) })
                        },
                        label = { Text("Label") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("task_link_label_$index")
                    )
                    IconButton(
                        onClick = {
                            onLinksChanged(links.filterIndexed { linkIndex, _ -> linkIndex != index })
                        },
                        modifier = Modifier.testTag("task_link_delete_$index")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove link")
                    }
                }
                OutlinedTextField(
                    value = link.url,
                    onValueChange = { url ->
                        onLinksChanged(links.updateAt(index) { it.copy(url = url) })
                    },
                    label = { Text("URL") },
                    placeholder = { Text("https://example.com") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    isError = link.url.isNotBlank() && !TaskLinkUrlValidator.isValid(link.url),
                    supportingText = if (link.url.isNotBlank() && !TaskLinkUrlValidator.isValid(link.url)) {
                        { Text("Enter a valid http:// or https:// URL.") }
                    } else null,
                    trailingIcon = {
                        IconButton(
                            enabled = TaskLinkUrlValidator.isValid(link.url),
                            onClick = {
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, link.url.trim().toUri()))
                                } catch (_: ActivityNotFoundException) {
                                    Log.w("TaskLinksEditor", "No app can open the saved task URL")
                                    Toast.makeText(context, "No app is available to open this link.", Toast.LENGTH_SHORT).show()
                                } catch (error: SecurityException) {
                                    Log.e("TaskLinksEditor", "Unable to open the saved task URL", error)
                                    Toast.makeText(context, "This link could not be opened.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.testTag("task_link_open_$index")
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = "Open link")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_link_url_$index")
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        TextButton(
            onClick = { onLinksChanged(links + TaskLink(label = "", url = "")) },
            modifier = Modifier.testTag("task_link_add")
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Text("Add link", modifier = Modifier.padding(start = 4.dp))
        }
    }
}

private inline fun List<TaskLink>.updateAt(index: Int, transform: (TaskLink) -> TaskLink): List<TaskLink> =
    mapIndexed { currentIndex, link -> if (currentIndex == index) transform(link) else link }
