package com.example.ui.screens.glossary

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.glossary.GlossaryModuleManager
import com.example.domain.glossary.GlossaryPack
import com.example.domain.glossary.GlossaryTerm
import com.example.domain.modules.FeatureModule
import com.example.domain.modules.FeatureModuleManager
import com.example.domain.modules.ModuleManifest
import com.example.ui.components.FeatureModuleCard
import com.example.ui.viewmodel.LifeViewModel

/**
 * Downloadable Feature Library — Modular Offline Knowledge Reader Screen (#2).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlossaryScreen(
    viewModel: LifeViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allTerms by viewModel.allGlossaryTerms.collectAsStateWithLifecycle()
    val allPacks by viewModel.allGlossaryPacks.collectAsStateWithLifecycle()
    val allFeatureModules by viewModel.allFeatureModules.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var showOnlyBookmarked by remember { mutableStateOf(false) }
    var selectedPackId by remember { mutableStateOf<String?>(null) }

    var selectedTermForDetail by remember { mutableStateOf<GlossaryTerm?>(null) }
    var showPackManagerSheet by remember { mutableStateOf(false) }
    var showImportJsonDialog by remember { mutableStateOf(false) }

    // Categories derived dynamically from current terms
    val categories = remember(allTerms) {
        allTerms.map { it.category }.distinct().sorted()
    }

    // Filter terms
    val filteredTerms = remember(allTerms, searchQuery, selectedCategory, showOnlyBookmarked, selectedPackId) {
        allTerms.filter { term ->
            val matchesQuery = searchQuery.isBlank() ||
                    term.term.contains(searchQuery, ignoreCase = true) ||
                    term.definition.contains(searchQuery, ignoreCase = true) ||
                    term.category.contains(searchQuery, ignoreCase = true) ||
                    term.tags.any { it.contains(searchQuery, ignoreCase = true) }

            val matchesCategory = selectedCategory == null || term.category.equals(selectedCategory, ignoreCase = true)
            val matchesBookmark = !showOnlyBookmarked || term.isBookmarked
            val matchesPack = selectedPackId == null || term.packId == selectedPackId

            matchesQuery && matchesCategory && matchesBookmark && matchesPack
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Knowledge Glossary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${filteredTerms.size} terms available offline",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showPackManagerSheet = true },
                        modifier = Modifier.testTag("glossary_packs_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = "Knowledge Packs",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search terms, definitions, tags...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("glossary_search_field")
            )

            // Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = selectedCategory == null && !showOnlyBookmarked && selectedPackId == null,
                    onClick = {
                        selectedCategory = null
                        showOnlyBookmarked = false
                        selectedPackId = null
                    },
                    label = { Text("All (${allTerms.size})", fontSize = 11.sp) }
                )

                FilterChip(
                    selected = showOnlyBookmarked,
                    onClick = { showOnlyBookmarked = !showOnlyBookmarked },
                    leadingIcon = {
                        Icon(
                            imageVector = if (showOnlyBookmarked) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    label = { Text("Bookmarked", fontSize = 11.sp) }
                )

                categories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = {
                            selectedCategory = if (selectedCategory == cat) null else cat
                        },
                        label = { Text(cat, fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Terms List
            if (filteredTerms.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "📚 No Glossary Terms Found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No definitions match \"$searchQuery\"." else "Install or import a verified knowledge pack to explore.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        FilledTonalButton(onClick = { showPackManagerSheet = true }) {
                            Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Explore Knowledge Packs")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredTerms, key = { it.id }) { term ->
                        GlossaryTermCard(
                            term = term,
                            onCardClick = { selectedTermForDetail = term },
                            onToggleBookmark = {
                                viewModel.toggleGlossaryBookmark(term.id, !term.isBookmarked)
                            }
                        )
                    }
                }
            }
        }
    }

    // Detail Dialog
    selectedTermForDetail?.let { term ->
        LaunchedEffect(term.id) {
            viewModel.recordGlossaryTermView(term.id)
        }
        GlossaryTermDetailDialog(
            term = term,
            onDismiss = { selectedTermForDetail = null },
            onToggleBookmark = {
                val next = !term.isBookmarked
                viewModel.toggleGlossaryBookmark(term.id, next)
                selectedTermForDetail = term.copy(isBookmarked = next)
            },
            onSavePersonalNote = { note ->
                viewModel.saveGlossaryPersonalNote(term.id, note)
                selectedTermForDetail = term.copy(personalNote = note)
            },
            onSelectRelatedTerm = { relTerm ->
                searchQuery = relTerm
                selectedTermForDetail = null
            }
        )
    }

    // Knowledge Packs Management BottomSheet
    if (showPackManagerSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showPackManagerSheet = false },
            sheetState = sheetState
        ) {
            GlossaryPackManagerContent(
                featureModules = allFeatureModules,
                onInstallModule = { manifest, packJson ->
                    viewModel.installFeatureModule(manifest, packJson)
                },
                onUninstallModule = { moduleId ->
                    viewModel.uninstallFeatureModule(moduleId)
                },
                onOpenImportDialog = {
                    showImportJsonDialog = true
                },
                onClose = { showPackManagerSheet = false }
            )
        }
    }

    // Import JSON Dialog
    if (showImportJsonDialog) {
        ImportGlossaryJsonDialog(
            onDismiss = { showImportJsonDialog = false },
            onImport = { jsonStr, sha ->
                try {
                    val parsed = GlossaryModuleManager.parsePackJson(jsonStr)
                    viewModel.installGlossaryPack(
                        packJson = parsed,
                        expectedSha256 = sha.ifBlank { null },
                        rawContent = jsonStr
                    )
                    showImportJsonDialog = false
                } catch (_: Exception) {}
            }
        )
    }
}

/**
 * Clean Card rendering an individual glossary term snippet.
 */
@Composable
fun GlossaryTermCard(
    term: GlossaryTerm,
    onCardClick: () -> Unit,
    onToggleBookmark: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick)
            .testTag("glossary_term_card_${term.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = term.term,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!term.phonetic.isNullOrBlank()) {
                        Text(
                            text = term.phonetic,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (term.personalNote.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(
                                text = "📝 Note",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Text(
                            text = term.category,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    IconButton(
                        onClick = onToggleBookmark,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (term.isBookmarked) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Bookmark",
                            tint = if (term.isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = term.definition,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (term.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    term.tags.take(3).forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = "#$tag",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Detailed modal sheet/dialog for a single glossary definition.
 */
@Composable
fun GlossaryTermDetailDialog(
    term: GlossaryTerm,
    onDismiss: () -> Unit,
    onToggleBookmark: () -> Unit,
    onSavePersonalNote: (String) -> Unit,
    onSelectRelatedTerm: (String) -> Unit
) {
    var isEditingNote by remember { mutableStateOf(false) }
    var noteText by remember(term.id, term.personalNote) { mutableStateOf(term.personalNote) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = term.term,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (!term.phonetic.isNullOrBlank()) {
                        Text(
                            text = term.phonetic,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                IconButton(onClick = onToggleBookmark) {
                    Icon(
                        imageVector = if (term.isBookmarked) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Bookmark",
                        tint = if (term.isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Category Tag
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "📁 ${term.category}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                // Definition
                Column {
                    Text(
                        text = "Definition",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = term.definition,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp
                    )
                }

                // Detailed Explanation
                if (term.detailedExplanation.isNotBlank()) {
                    Column {
                        Text(
                            text = "Deep Dive & Mechanism",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = term.detailedExplanation,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }

                // Practical Examples
                if (term.examples.isNotEmpty()) {
                    Column {
                        Text(
                            text = "Practical Examples",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        term.examples.forEach { eg ->
                            Row(
                                modifier = Modifier.padding(vertical = 2.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text("• ", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                Text(
                                    text = eg,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                // Profile-isolated Personal Note
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Personal Reflection (Profile Isolated)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (isEditingNote) {
                            TextButton(
                                onClick = {
                                    onSavePersonalNote(noteText)
                                    isEditingNote = false
                                },
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("Save", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            TextButton(
                                onClick = { isEditingNote = true },
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text(if (noteText.isBlank()) "+ Add Note" else "Edit", fontSize = 11.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    if (isEditingNote) {
                        OutlinedTextField(
                            value = noteText,
                            onValueChange = { noteText = it },
                            placeholder = { Text("Record personal insights or study notes...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp),
                            shape = RoundedCornerShape(8.dp)
                        )
                    } else if (noteText.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = noteText,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "No personal reflections for this profile yet. Tap '+ Add Note' to create one.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                // Related Terms
                if (term.relatedTerms.isNotEmpty()) {
                    Column {
                        Text(
                            text = "Related Mental Models",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            term.relatedTerms.forEach { rel ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.clickable { onSelectRelatedTerm(rel) }
                                ) {
                                    Text(
                                        text = rel,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

/**
 * BottomSheet content for managing knowledge pack modules.
 */
@Composable
fun GlossaryPackManagerContent(
    featureModules: List<FeatureModule>,
    onInstallModule: (ModuleManifest, com.example.domain.glossary.GlossaryPackJson) -> Unit,
    onUninstallModule: (String) -> Unit,
    onOpenImportDialog: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "📦 Feature Modules & Packs",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Verified offline declarative packs (#2, #10)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onClose) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
            }
        }

        // Action: Import custom pack JSON
        OutlinedButton(
            onClick = onOpenImportDialog,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(imageVector = Icons.Default.LibraryBooks, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Import Custom JSON Pack (with SHA-256 Check)")
        }

        Text(
            text = "Curated Feature Modules (${featureModules.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        val catalogMap = remember {
            FeatureModuleManager.CURATED_CATALOG.associate { it.first.id to it.second }
        }

        featureModules.forEach { module ->
            val packJson = catalogMap[module.id] ?: return@forEach
            FeatureModuleCard(
                module = module,
                onInstall = { onInstallModule(module.manifest, packJson) },
                onUninstall = { onUninstallModule(module.id) },
                onUpdate = { onInstallModule(module.manifest, packJson) }
            )
        }
    }
}

/**
 * Dialog to import custom offline JSON pack with optional SHA-256 verification.
 */
@Composable
fun ImportGlossaryJsonDialog(
    onDismiss: () -> Unit,
    onImport: (String, String) -> Unit
) {
    var rawJson by remember { mutableStateOf("") }
    var sha256Input by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Knowledge Pack JSON") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Paste pure JSON pack content. Data is verified and stored in local Room database. Zero executable code is loaded.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = rawJson,
                    onValueChange = {
                        rawJson = it
                        errorMessage = null
                    },
                    label = { Text("Pack JSON Content") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = sha256Input,
                    onValueChange = { sha256Input = it },
                    label = { Text("Expected SHA-256 (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    try {
                        val parsed = GlossaryModuleManager.parsePackJson(rawJson)
                        if (sha256Input.isNotBlank()) {
                            val computed = GlossaryModuleManager.computeSha256(rawJson)
                            if (!computed.equals(sha256Input.trim(), ignoreCase = true)) {
                                errorMessage = "SHA-256 verification mismatch!"
                                return@Button
                            }
                        }
                        onImport(rawJson, sha256Input)
                    } catch (e: Exception) {
                        errorMessage = e.message ?: "Invalid JSON"
                    }
                },
                enabled = rawJson.isNotBlank()
            ) {
                Text("Verify & Install")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
