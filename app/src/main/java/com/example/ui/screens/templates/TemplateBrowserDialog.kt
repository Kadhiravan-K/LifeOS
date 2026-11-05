package com.example.ui.screens.templates

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.AlertDialog
import com.example.domain.templates.LifeTemplate
import com.example.domain.templates.TemplateCatalog
import com.example.domain.templates.TemplateCategory
import com.example.domain.templates.TemplateImportSummary

import com.example.domain.templates.TemplateBundle
import com.example.domain.essentials.EssentialsCatalog
import com.example.domain.essentials.TrackerTemplate
import com.example.domain.essentials.SubscriptionTemplate
import com.example.domain.qr.QrCodePayload
import com.example.domain.qr.QrCodeTemplate
import com.example.domain.qr.QrCodeTemplateCatalog
import android.graphics.Bitmap

enum class TemplateBrowserTab(val title: String, val emoji: String) {
    INDIVIDUAL("Individual Templates", "📋"),
    BUNDLES("Productivity Bundles", "📦"),
    SPECIAL_BUCKET_LISTS("Special Bucket Lists", "✨"),
    IMPORTED("Imported", "✅"),
    QR_CODES("QR Codes", "▦"),
    TRACKERS("Trackers", "📊"),
    SUBSCRIPTIONS("Subscriptions", "💳")
}

@Composable
fun TemplateBrowserDialog(
    installedTemplateIds: Set<String>,
    onInstallTemplate: (LifeTemplate, Set<Int>, Set<Int>, (TemplateImportSummary) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    initialTab: TemplateBrowserTab = TemplateBrowserTab.INDIVIDUAL,
    installedQrTemplateIds: Set<String> = emptySet(),
    onImportQrTemplates: (List<QrCodeTemplate>, (Int, Int) -> Unit) -> Unit = { _, _ -> },
    onImportTrackerTemplate: (TrackerTemplate) -> Unit = {},
    onImportSubscriptionTemplate: (SubscriptionTemplate) -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(initialTab) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<TemplateCategory?>(null) }
    var previewTemplate by remember { mutableStateOf<LifeTemplate?>(null) }
    var lastImportSummary by remember { mutableStateOf<TemplateImportSummary?>(null) }
    var previewQrTemplate by remember { mutableStateOf<QrCodeTemplate?>(null) }
    var showQrBundlePreview by remember { mutableStateOf(false) }
    var qrImportSummary by remember { mutableStateOf<String?>(null) }

    val visibleQrTemplates = remember(searchQuery) {
        QrCodeTemplateCatalog.testingBundle.filter {
            searchQuery.isBlank() || it.title.contains(searchQuery, true) ||
                    it.description.contains(searchQuery, true) || it.type.displayName.contains(searchQuery, true)
        }
    }

    val visibleTrackerTemplates = remember(searchQuery) {
        EssentialsCatalog.trackerTemplates.filter {
            searchQuery.isBlank() || it.title.contains(searchQuery, true) ||
                    it.description.contains(searchQuery, true) || it.category.contains(searchQuery, true)
        }
    }

    val visibleSubscriptionTemplates = remember(searchQuery) {
        EssentialsCatalog.subscriptionTemplates.filter {
            searchQuery.isBlank() || it.title.contains(searchQuery, true) ||
                    it.category.contains(searchQuery, true) || it.notes.contains(searchQuery, true)
        }
    }

    val individualTemplates = remember(searchQuery, selectedCategory) {
        TemplateCatalog.allTemplates.filter { template ->
            template.category != TemplateCategory.BUCKET_LIST &&
                    (selectedCategory == null || template.category == selectedCategory) &&
                    (searchQuery.isBlank() ||
                            template.title.contains(searchQuery, ignoreCase = true) ||
                            template.description.contains(searchQuery, ignoreCase = true) ||
                            template.tags.any { it.contains(searchQuery, ignoreCase = true) })
        }
    }

    val bucketListTemplates = remember(searchQuery) {
        TemplateCatalog.allTemplates.filter { template ->
            template.category == TemplateCategory.BUCKET_LIST &&
                    (searchQuery.isBlank() ||
                            template.title.contains(searchQuery, ignoreCase = true) ||
                            template.description.contains(searchQuery, ignoreCase = true) ||
                            template.tags.any { it.contains(searchQuery, ignoreCase = true) })
        }
    }

    val productivityBundles = remember(searchQuery) {
        TemplateCatalog.allBundles.filter { bundle ->
            searchQuery.isBlank() ||
                    bundle.title.contains(searchQuery, ignoreCase = true) ||
                    bundle.description.contains(searchQuery, ignoreCase = true)
        }
    }

    val importedTemplates = remember(installedTemplateIds, searchQuery) {
        TemplateCatalog.allTemplates.filter { template ->
            installedTemplateIds.contains(template.id) &&
                    (searchQuery.isBlank() ||
                            template.title.contains(searchQuery, ignoreCase = true) ||
                            template.description.contains(searchQuery, ignoreCase = true))
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .testTag("template_browser_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "📋 Routine & Habit Templates",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Curated offline master routines for your LifeOS",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Shared module and template categories
                val tabScrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(tabScrollState),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TemplateBrowserTab.entries.forEach { tab ->
                        FilterChip(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            label = { Text("${tab.emoji} ${tab.title}") },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            when (selectedTab) {
                                TemplateBrowserTab.INDIVIDUAL -> "Search individual templates..."
                                TemplateBrowserTab.BUNDLES -> "Search 7 productivity bundles..."
                                TemplateBrowserTab.SPECIAL_BUCKET_LISTS -> "Search 4 special bucket lists..."
                                TemplateBrowserTab.IMPORTED -> "Search imported templates..."
                                TemplateBrowserTab.QR_CODES -> "Search QR code templates..."
                                TemplateBrowserTab.TRACKERS -> "Search tracker templates..."
                                TemplateBrowserTab.SUBSCRIPTIONS -> "Search subscription templates..."
                            }
                        )
                    },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                when (selectedTab) {
                    TemplateBrowserTab.INDIVIDUAL -> {
                        val chipScrollState = rememberScrollState()
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(chipScrollState),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedCategory == null,
                                onClick = { selectedCategory = null },
                                label = { Text("All (${TemplateCatalog.allTemplates.count { it.category != TemplateCategory.BUCKET_LIST }})") },
                                shape = RoundedCornerShape(10.dp)
                            )
                            TemplateCategory.entries.filter { it != TemplateCategory.BUCKET_LIST }.forEach { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                                    label = { Text("${cat.emoji} ${cat.displayName}") },
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(individualTemplates, key = { it.id }) { template ->
                                TemplateCard(
                                    template = template,
                                    isInstalled = template.id in installedTemplateIds,
                                    onPreviewClick = { previewTemplate = template }
                                )
                            }
                        }
                    }

                    TemplateBrowserTab.BUNDLES -> {
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(productivityBundles, key = { it.id }) { bundle ->
                                val allBundleInstalled = bundle.templateIds.all { it in installedTemplateIds }
                                BundleCard(
                                    bundle = bundle,
                                    isInstalled = allBundleInstalled,
                                    onInstallClick = {
                                        val templatesToInstall = bundle.templateIds.mapNotNull {
                                            TemplateCatalog.getTemplateById(it)
                                        }
                                        var count = 0
                                        templatesToInstall.forEach { template ->
                                            onInstallTemplate(
                                                template,
                                                template.tasks.indices.toSet(),
                                                template.habits.indices.toSet()
                                            ) {
                                                count++
                                                if (count == templatesToInstall.size) {
                                                    lastImportSummary = TemplateImportSummary(
                                                        templateId = bundle.id,
                                                        templateTitle = "${bundle.emoji} ${bundle.title} Bundle",
                                                        tasksImported = templatesToInstall.sumOf { it.tasks.size },
                                                        habitsImported = templatesToInstall.sumOf { it.habits.size },
                                                        shortcutsImported = templatesToInstall.sumOf { it.shortcuts.size },
                                                        projectsImported = templatesToInstall.sumOf { it.projects.size },
                                                        challengeConfigured = templatesToInstall.any { it.challenge != null },
                                                        xpAwarded = bundle.bonusXp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }

                    TemplateBrowserTab.SPECIAL_BUCKET_LISTS -> {
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(bucketListTemplates, key = { it.id }) { template ->
                                TemplateCard(
                                    template = template,
                                    isInstalled = template.id in installedTemplateIds,
                                    onPreviewClick = { previewTemplate = template }
                                )
                            }
                        }
                    }

                    TemplateBrowserTab.IMPORTED -> {
                        if (importedTemplates.isEmpty()) {
                            Box(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("📥", fontSize = 48.sp)
                                    Spacer(Modifier.height(12.dp))
                                    Text("No Templates Installed Yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "Select templates from Individual, Bundles, or Special Bucket Lists.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(importedTemplates, key = { it.id }) { template ->
                                    TemplateCard(template, true) { previewTemplate = template }
                                }
                            }
                        }
                    }

                    TemplateBrowserTab.QR_CODES -> {
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item(key = "qr_testing_bundle") {
                                QrTestingBundleCard(
                                    alreadyImported = QrCodeTemplateCatalog.testingBundle.all {
                                        it.id in installedQrTemplateIds
                                    },
                                    onPreview = { showQrBundlePreview = true }
                                )
                            }
                            items(visibleQrTemplates, key = { it.id }) { template ->
                                QrTemplateCard(template, template.id in installedQrTemplateIds) {
                                    previewQrTemplate = template
                                }
                            }
                        }
                    }

                    TemplateBrowserTab.TRACKERS -> {
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(visibleTrackerTemplates, key = { it.id }) { template ->
                                ModuleTemplateCard(
                                    title = template.title,
                                    emoji = template.emoji,
                                    description = "${template.category} • ${template.description}",
                                    detail = buildList {
                                        if (template.measurements.trackDuration) add("Cycle")
                                        if (template.measurements.trackDistance) add(template.distanceUnit.displayName)
                                        if (template.measurements.trackQuantity) add(template.quantityUnit.displayName)
                                    }.joinToString(" • "),
                                    onImport = { onImportTrackerTemplate(template) }
                                )
                            }
                        }
                    }

                    TemplateBrowserTab.SUBSCRIPTIONS -> {
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(visibleSubscriptionTemplates, key = { it.id }) { template ->
                                ModuleTemplateCard(
                                    title = template.title,
                                    emoji = template.emoji,
                                    description = template.category,
                                    detail = "${template.currency} ${template.defaultAmount} • ${template.frequency.displayName}",
                                    onImport = { onImportSubscriptionTemplate(template) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Deep Preview & Customization Dialog
    previewTemplate?.let { template ->
        TemplatePreviewDialog(
            template = template,
            isAlreadyInstalled = installedTemplateIds.contains(template.id),
            onDismiss = { previewTemplate = null },
            onConfirmInstall = { selectedTasks, selectedHabits ->
                onInstallTemplate(template, selectedTasks, selectedHabits) { summary ->
                    previewTemplate = null
                    lastImportSummary = summary
                }
            }
        )
    }

    // Import Summary Success Dialog
    lastImportSummary?.let { summary ->
        ImportSuccessDialog(
            summary = summary,
            onDismiss = { lastImportSummary = null }
        )
    }

    previewQrTemplate?.let { template ->
        QrTemplatePreviewDialog(
            template = template,
            alreadyImported = template.id in installedQrTemplateIds,
            onDismiss = { previewQrTemplate = null },
            onImport = {
                onImportQrTemplates(listOf(template)) { imported, alreadyPresent ->
                    previewQrTemplate = null
                    qrImportSummary = if (imported > 0) {
                        "Imported $imported QR code."
                    } else {
                        "$alreadyPresent QR code was already imported; nothing was duplicated."
                    }
                }
            }
        )
    }

    if (showQrBundlePreview) {
        QrBundlePreviewDialog(
            onDismiss = { showQrBundlePreview = false },
            onConfirm = {
                onImportQrTemplates(QrCodeTemplateCatalog.testingBundle) { imported, alreadyPresent ->
                    showQrBundlePreview = false
                    qrImportSummary = when {
                        imported == 0 -> "All 8 QR codes were already imported; no duplicates were created."
                        alreadyPresent == 0 -> "Imported all $imported QR codes."
                        else -> "Imported $imported QR codes; skipped $alreadyPresent already-imported codes."
                    }
                }
            }
        )
    }

    qrImportSummary?.let { summary ->
        AlertDialog(
            onDismissRequest = { qrImportSummary = null },
            title = { Text("QR template import") },
            text = { Text(summary) },
            confirmButton = { TextButton(onClick = { qrImportSummary = null }) { Text("Done") } }
        )
    }
}

@Composable
private fun QrTestingBundleCard(alreadyImported: Boolean, onPreview: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().testTag("qr_testing_bundle_card"),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("QR Code Testing Bundle", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "QR Codes • 8 sample records across URL, vCard, Wi-Fi, text, email, phone, SMS, and location. " +
                    "Fictional, offline-safe test data.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Button(onClick = onPreview, modifier = Modifier.fillMaxWidth()) {
                Text(if (alreadyImported) "Preview / Re-import safely" else "Preview 8-code bundle")
            }
        }
    }
}

@Composable
private fun QrTemplateCard(template: QrCodeTemplate, isInstalled: Boolean, onPreview: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().testTag("qr_template_${template.id}"),
        onClick = onPreview,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text("${template.type.displayName} • ${template.title}", fontWeight = FontWeight.Bold)
            Text(template.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(if (isInstalled) "Imported" else "Preview", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun ModuleTemplateCard(
    title: String,
    emoji: String,
    description: String,
    detail: String,
    onImport: () -> Unit
) {
    var showPreview by remember(title) { mutableStateOf(false) }
    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 26.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(detail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            Button(onClick = { showPreview = true }) { Text("Preview") }
        }
    }
    if (showPreview) {
        AlertDialog(
            onDismissRequest = { showPreview = false },
            title = { Text("$emoji $title") },
            text = { Text("$description\n\n$detail") },
            confirmButton = {
                Button(onClick = {
                    showPreview = false
                    onImport()
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showPreview = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun QrTemplatePreviewDialog(
    template: QrCodeTemplate,
    alreadyImported: Boolean,
    onDismiss: () -> Unit,
    onImport: () -> Unit
) {
    val matrix = remember(template) {
        runCatching {
            val payload = when (val result = QrCodePayload.validateAndEncode(template.toDraft())) {
                is com.example.domain.qr.QrCodeValidation.Valid -> result.payload
                is com.example.domain.qr.QrCodeValidation.Invalid -> error(result.message)
            }
            QrCodePayload.encodeQr(payload, 320)
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(template.title) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val bitMatrix = matrix.getOrNull()
                if (bitMatrix != null) {
                    val bitmap = remember(bitMatrix) { bitMatrixToBitmap(bitMatrix) }
                    Image(
                        bitmap.asImageBitmap(),
                        contentDescription = "Preview QR code: ${template.title}",
                        modifier = Modifier.size(200.dp)
                    )
                } else {
                    Text("QR preview could not be generated.", color = MaterialTheme.colorScheme.error)
                }
                Text("${template.type.displayName} • functional QR payload", modifier = Modifier.padding(top = 8.dp))
                Text(template.description, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = onImport) { Text(if (alreadyImported) "Import again safely" else "Import") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun QrBundlePreviewDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("QR Code Testing Bundle") },
        text = {
            Column(modifier = Modifier.height(380.dp).verticalScroll(rememberScrollState())) {
                Text("QR Codes — 8 records", fontWeight = FontWeight.Bold)
                Text("All payloads are validated locally before the import transaction. Existing imported template records are skipped; manual QR records are preserved.")
                Spacer(Modifier.height(8.dp))
                QrCodeTemplateCatalog.testingBundle.forEach { template ->
                    Text("• ${template.type.displayName}: ${template.title}", style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "This bundle imports only QR codes. Other module templates remain available in their categories.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = { Button(onClick = onConfirm) { Text("Import 8 QR codes") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun bitMatrixToBitmap(matrix: com.google.zxing.common.BitMatrix): Bitmap {
    val bitmap = Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.ARGB_8888)
    val pixels = IntArray(matrix.width * matrix.height) { index ->
        if (matrix[index % matrix.width, index / matrix.width]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
    }
    bitmap.setPixels(pixels, 0, matrix.width, 0, 0, matrix.width, matrix.height)
    return bitmap
}

@Composable
fun TemplateCard(
    template: LifeTemplate,
    isInstalled: Boolean,
    onPreviewClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPreviewClick() }
            .testTag("template_card_${template.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = template.iconEmoji, fontSize = 22.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = template.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isInstalled) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Installed",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "Installed",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "${template.category.emoji} ${template.category.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFEF3C7)
                ) {
                    Text(
                        text = "+25 XP",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB45309),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = template.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Badges row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (template.tasks.isNotEmpty()) {
                        DetailChip(text = "📌 ${template.tasks.size} Tasks")
                    }
                    if (template.habits.isNotEmpty()) {
                        DetailChip(text = "✨ ${template.habits.size} Habits")
                    }
                    if (template.shortcuts.isNotEmpty()) {
                        DetailChip(text = "⬡ ${template.shortcuts.size} Hex")
                    }
                }

                Button(
                    onClick = onPreviewClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = if (isInstalled) {
                        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                    } else {
                        ButtonDefaults.buttonColors()
                    }
                ) {
                    Text(text = if (isInstalled) "View / Re-add" else "Preview", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun BundleCard(
    bundle: TemplateBundle,
    isInstalled: Boolean,
    onInstallClick: () -> Unit
) {
    val includedTemplates = remember(bundle) {
        bundle.templateIds.mapNotNull { TemplateCatalog.getTemplateById(it) }
    }
    val totalTasks = remember(includedTemplates) { includedTemplates.sumOf { it.tasks.size } }
    val totalHabits = remember(includedTemplates) { includedTemplates.sumOf { it.habits.size } }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("bundle_card_${bundle.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = bundle.emoji, fontSize = 22.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = bundle.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (isInstalled) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Installed",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "Installed",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        text = "Includes ${includedTemplates.size} modules • +${bundle.bonusXp} XP",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = bundle.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                includedTemplates.forEach { tmpl ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = "${tmpl.iconEmoji} ${tmpl.title}",
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📋 $totalTasks Tasks • 🌱 $totalHabits Habits",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                Button(
                    onClick = onInstallClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isInstalled) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primary,
                        contentColor = if (isInstalled) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = if (isInstalled) Icons.Default.Check else Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = if (isInstalled) "Re-import Bundle" else "Install Bundle", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun DetailChip(text: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun TemplatePreviewDialog(
    template: LifeTemplate,
    isAlreadyInstalled: Boolean,
    onDismiss: () -> Unit,
    onConfirmInstall: (Set<Int>, Set<Int>) -> Unit
) {
    var selectedTaskIndices by remember { mutableStateOf(template.tasks.indices.toSet()) }
    var selectedHabitIndices by remember { mutableStateOf(template.habits.indices.toSet()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f)
                .testTag("template_preview_dialog"),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = template.iconEmoji, fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = template.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${template.category.emoji} ${template.category.displayName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = template.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (isAlreadyInstalled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "You have previously imported this template. Re-importing is duplicate-protected.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))

                // Scrollable content of items to import
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Tasks
                    if (template.tasks.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Tasks (${selectedTaskIndices.size}/${template.tasks.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                TextButton(
                                    onClick = {
                                        selectedTaskIndices = if (selectedTaskIndices.size == template.tasks.size) {
                                            emptySet()
                                        } else {
                                            template.tasks.indices.toSet()
                                        }
                                    }
                                ) {
                                    Text(
                                        text = if (selectedTaskIndices.size == template.tasks.size) "Deselect All" else "Select All",
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        items(template.tasks.indices.toList()) { idx ->
                            val taskDef = template.tasks[idx]
                            val isChecked = idx in selectedTaskIndices
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedTaskIndices = if (isChecked) {
                                            selectedTaskIndices - idx
                                        } else {
                                            selectedTaskIndices + idx
                                        }
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            selectedTaskIndices = if (checked) {
                                                selectedTaskIndices + idx
                                            } else {
                                                selectedTaskIndices - idx
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = taskDef.emoji, fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = taskDef.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        val taskProjName = taskDef.projectName ?: if (template.projects.size == 1) template.projects.first().name else null
                                        val labelsText = if (taskDef.labels.isNotEmpty()) " • 🏷️ ${taskDef.labels.joinToString(", ")}" else ""
                                        Text(
                                            text = "${taskDef.dueTime ?: "Anytime"} • ${taskDef.durationMinutes}m • ${taskDef.recurrenceType.displayName}${if (taskProjName != null) " • 📁 $taskProjName" else ""}$labelsText",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Habits
                    if (template.habits.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Habits (${selectedHabitIndices.size}/${template.habits.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                TextButton(
                                    onClick = {
                                        selectedHabitIndices = if (selectedHabitIndices.size == template.habits.size) {
                                            emptySet()
                                        } else {
                                            template.habits.indices.toSet()
                                        }
                                    }
                                ) {
                                    Text(
                                        text = if (selectedHabitIndices.size == template.habits.size) "Deselect All" else "Select All",
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        items(template.habits.indices.toList()) { idx ->
                            val habitDef = template.habits[idx]
                            val isChecked = idx in selectedHabitIndices
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedHabitIndices = if (isChecked) {
                                            selectedHabitIndices - idx
                                        } else {
                                            selectedHabitIndices + idx
                                        }
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            selectedHabitIndices = if (checked) {
                                                selectedHabitIndices + idx
                                            } else {
                                                selectedHabitIndices - idx
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = habitDef.emoji, fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = habitDef.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = habitDef.reminderTime?.let { "⏰ Reminder at $it" } ?: "Daily Habit",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Project & Hexagons info
                    if (template.projects.isNotEmpty() || template.shortcuts.isNotEmpty() || template.challenge != null) {
                        item {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Additional Bundled Components",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            template.projects.forEach { proj ->
                                Text(
                                    text = "📁 Project: ${proj.emoji} ${proj.name}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (template.shortcuts.isNotEmpty()) {
                                Text(
                                    text = "⬡ Honeycomb Shortcuts: Adds ${template.shortcuts.size} quick-access hex tile(s)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            template.challenge?.let {
                                Text(
                                    text = "🔥 75-Day Challenge: Configures ${it.rules.size} discipline rules",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = { onConfirmInstall(selectedTaskIndices, selectedHabitIndices) },
                        modifier = Modifier
                            .weight(2f)
                            .testTag("confirm_install_template_button"),
                        shape = RoundedCornerShape(12.dp),
                        enabled = selectedTaskIndices.isNotEmpty() || selectedHabitIndices.isNotEmpty() || template.challenge != null
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Install Selected (+25 XP)")
                    }
                }
            }
        }
    }
}

@Composable
fun ImportSuccessDialog(
    summary: TemplateImportSummary,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD1FAE5)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = Color(0xFF059669),
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Template Installed!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "${summary.templateTitle} has been loaded into your LifeOS.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = "• ${summary.tasksImported} Task(s) scheduled", fontSize = 13.sp)
                        Text(text = "• ${summary.habitsImported} Habit(s) added", fontSize = 13.sp)
                        if (summary.projectsAdded > 0) {
                            Text(text = "• ${summary.projectsAdded} Project created", fontSize = 13.sp)
                        }
                        if (summary.shortcutsAdded > 0) {
                            Text(text = "• ${summary.shortcutsAdded} Honeycomb hex shortcut(s) pinned", fontSize = 13.sp)
                        }
                        Text(text = "• +${summary.xpAwarded} XP awarded!", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Awesome!")
                }
            }
        }
    }
}
