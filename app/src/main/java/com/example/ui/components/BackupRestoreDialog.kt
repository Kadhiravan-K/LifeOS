package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.backup.DataExportImportManager
import com.example.ui.viewmodel.LifeViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupRestoreDialog(
    viewModel: LifeViewModel,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Export, 1 = Import
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var exportJson by remember { mutableStateOf<String?>(null) }
    var isExporting by remember { mutableStateOf(false) }

    var importInput by remember { mutableStateOf("") }
    var isImporting by remember { mutableStateOf(false) }
    var importStatusMessage by remember { mutableStateOf<String?>(null) }
    var pendingSummary by remember { mutableStateOf<DataExportImportManager.BackupSummary?>(null) }
    var showConfirmReplaceDialog by remember { mutableStateOf(false) }

    // Storage Access Framework (SAF) Launchers
    val defaultFilename = "lifeos_backup_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.json"
    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            isExporting = true
            scope.launch {
                val success = viewModel.exportBackupToUri(uri)
                isExporting = false
                if (success) {
                    Toast.makeText(context, "Backup exported successfully to file!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to export backup file.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val contentResolver = context.contentResolver
                    val json = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    if (!json.isNullOrBlank()) {
                        importInput = json
                        val validation = viewModel.validateBackupJson(json)
                        if (validation is DataExportImportManager.ValidationResult.Valid) {
                            pendingSummary = validation.summary
                            importStatusMessage = null
                        } else if (validation is DataExportImportManager.ValidationResult.Invalid) {
                            pendingSummary = null
                            importStatusMessage = "Invalid backup: ${validation.error}"
                        }
                    }
                } catch (e: Exception) {
                    importStatusMessage = "Failed to read backup file: ${e.message}"
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "💾", fontSize = 22.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Offline Backup & Restore v2", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Export v2") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Restore v2") }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (selectedTab == 0) {
                    // Export Tab
                    Text(
                        text = "Generate a verified offline backup covering all 40 LifeOS entities (tasks, habits, trackers, subscriptions, workspaces, glossary, MCP servers, and audit logs) with SHA-256 integrity.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (exportJson == null) {
                        Button(
                            onClick = {
                                isExporting = true
                                scope.launch {
                                    val json = viewModel.exportDataJson()
                                    exportJson = json
                                    isExporting = false
                                }
                            },
                            enabled = !isExporting,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isExporting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Exporting Database...")
                            } else {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Generate Backup v2")
                            }
                        }
                    } else {
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Backup Ready (${exportJson!!.length} bytes, Format v2)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = exportJson!!.take(220) + "...",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                exportFileLauncher.launch(defaultFilename)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save to File (SAF)", fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("LifeOS Backup v2", exportJson))
                                    Toast.makeText(context, "Backup copied to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy", fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, exportJson)
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "Share LifeOS Backup v2")
                                    context.startActivity(shareIntent)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share", fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    // Restore Tab
                    Text(
                        text = "Select or paste a LifeOS backup file. Restoring will validate integrity, create an automatic recovery backup, and transactionally replace all data (REPLACE_ALL).",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            importFileLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Select Backup File (SAF)")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = importInput,
                        onValueChange = {
                            importInput = it
                            if (it.isNotBlank()) {
                                val validation = viewModel.validateBackupJson(it.trim())
                                if (validation is DataExportImportManager.ValidationResult.Valid) {
                                    pendingSummary = validation.summary
                                    importStatusMessage = null
                                } else if (validation is DataExportImportManager.ValidationResult.Invalid) {
                                    pendingSummary = null
                                    importStatusMessage = "Invalid backup: ${validation.error}"
                                }
                            } else {
                                pendingSummary = null
                                importStatusMessage = null
                            }
                        },
                        placeholder = { Text("Or paste JSON backup here...", fontSize = 12.sp) },
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                    )

                    if (pendingSummary != null) {
                        val s = pendingSummary!!
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Valid Backup: Format v${s.formatVersion} (App: ${s.appVersion})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Total Records: ${s.totalRecords} across ${s.entityCounts.size} entities", fontSize = 11.sp)
                                Text(
                                    text = if (s.isChecksumValid == true) "Integrity: SHA-256 Checksum Verified" else if (s.isLegacy) "Integrity: Legacy Format (No Checksum)" else "Integrity: Unverified",
                                    fontSize = 11.sp,
                                    color = if (s.isChecksumValid == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (importStatusMessage != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = importStatusMessage!!,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (importStatusMessage!!.startsWith("Success")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (importInput.isBlank()) {
                                importStatusMessage = "Please select or paste a backup first."
                                return@Button
                            }
                            showConfirmReplaceDialog = true
                        },
                        enabled = !isImporting && importInput.isNotBlank() && pendingSummary != null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isImporting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restoring Data...")
                        } else {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Review & Restore (REPLACE_ALL)")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )

    // REPLACE_ALL Confirmation Dialog
    if (showConfirmReplaceDialog && pendingSummary != null) {
        val s = pendingSummary!!
        AlertDialog(
            onDismissRequest = { showConfirmReplaceDialog = false },
            icon = {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            },
            title = {
                Text("Confirm Database Replacement", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = "This operation will REPLACE ALL existing application data with ${s.totalRecords} records from this backup.",
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "🛡️ Safety Guarantee: LifeOS will automatically export a recovery backup of your current database to internal storage before replacement. If any error occurs, the restore will roll back completely.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmReplaceDialog = false
                        isImporting = true
                        scope.launch {
                            val result = viewModel.restoreBackupV2(importInput.trim())
                            isImporting = false
                            when (result) {
                                is DataExportImportManager.RestoreResult.Success -> {
                                    importStatusMessage = "Success! Restored ${result.restoredRecords} records across all 40 entities."
                                    pendingSummary = null
                                    importInput = ""
                                }
                                is DataExportImportManager.RestoreResult.Error -> {
                                    importStatusMessage = "Restore failed: ${result.message}"
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirm REPLACE_ALL")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmReplaceDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
