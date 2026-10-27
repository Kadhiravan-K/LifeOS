package com.example.ui.screens.themes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import kotlinx.coroutines.delay
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.engine.LocalLifeTheme
import com.example.ui.theme.engine.ThemedButton
import com.example.ui.theme.engine.ThemedCard
import com.example.ui.theme.engine.ThemeDefinition
import com.example.ui.theme.engine.ThemeId
import com.example.ui.theme.engine.ThemedLoadingIndicator
import com.example.ui.theme.engine.ThemedProgressBar
import com.example.ui.theme.engine.ThemeRegistry
import com.example.ui.viewmodel.LifeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeGalleryScreen(
    viewModel: LifeViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeThemeDef by viewModel.activeThemeDefinition.collectAsStateWithLifecycle()
    val previewThemeId by viewModel.previewThemeId.collectAsStateWithLifecycle()
    val isPremiumActive by viewModel.isPremiumActive.collectAsStateWithLifecycle()
    val persistedThemeId by viewModel.activeThemeIdFlow.collectAsStateWithLifecycle()

    var showUnlockModalForTheme by remember { mutableStateOf<ThemeDefinition?>(null) }
    var transitioningTheme by remember { mutableStateOf<ThemeDefinition?>(null) }

    LaunchedEffect(transitioningTheme) {
        if (transitioningTheme != null) {
            delay(1500)
            transitioningTheme = null
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = activeThemeDef.colors.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Theme Gallery",
                            style = activeThemeDef.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.dismissThemePreview()
                            onNavigateBack()
                        },
                        modifier = Modifier.testTag("back_from_theme_gallery")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = activeThemeDef.colors.surface,
                    titleContentColor = activeThemeDef.colors.onSurface
                )
            )
        },
        bottomBar = {
            // Interactive Preview Banner if currently previewing a theme
            AnimatedVisibility(
                visible = previewThemeId != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    color = activeThemeDef.colors.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, activeThemeDef.colors.primary),
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("theme_preview_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Previewing: ${activeThemeDef.name}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = activeThemeDef.colors.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Temporary",
                                    fontSize = 10.sp,
                                    color = activeThemeDef.colors.primary
                                )
                            }
                            Text(
                                text = "Explore the full app in this theme without saving.",
                                fontSize = 11.sp,
                                color = activeThemeDef.colors.onSurfaceVariant
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.dismissThemePreview() },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Exit", fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    val success = viewModel.applyPreviewedTheme()
                                    if (!success) {
                                        showUnlockModalForTheme = activeThemeDef
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = activeThemeDef.colors.primary),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("Apply", fontSize = 12.sp, color = activeThemeDef.colors.onPrimary)
                            }
                        }
                    }
                }
            }
        },
        containerColor = activeThemeDef.colors.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Header Description
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = activeThemeDef.colors.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Immersive Design Systems",
                            style = activeThemeDef.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = activeThemeDef.colors.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Each theme is a handcrafted complete visual environment with custom typography, shapes, atmospheric backgrounds, and interactive honeycomb mechanics.",
                            style = activeThemeDef.typography.bodyMedium,
                            color = activeThemeDef.colors.onSurfaceVariant
                        )
                    }
                }
            }

            // Developer Entitlement Simulator Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = activeThemeDef.colors.surfaceVariant.copy(alpha = 0.7f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, activeThemeDef.colors.outline.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("developer_entitlement_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (isPremiumActive) Color(0xFFF59E0B) else activeThemeDef.colors.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Developer Entitlement Simulator",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = activeThemeDef.colors.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isPremiumActive) "Premium themes unlocked for local testing." else "Simulate active premium entitlement to test unlock flows.",
                                fontSize = 11.sp,
                                color = activeThemeDef.colors.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = isPremiumActive,
                            onCheckedChange = { active ->
                                viewModel.toggleSimulatedPremium(active)
                            },
                            modifier = Modifier.testTag("simulate_premium_switch")
                        )
                    }
                }
            }

            // Theme Cards
            items(ThemeRegistry.allThemes, key = { it.id.idString }) { theme ->
                val isCurrentlyPersisted = persistedThemeId.equals(theme.id.idString, ignoreCase = true)
                val isCurrentlyPreviewing = previewThemeId == theme.id
                val isUnlocked = viewModel.isThemeUnlocked(theme.id)

                ThemeCardItem(
                    theme = theme,
                    isPersisted = isCurrentlyPersisted,
                    isPreviewing = isCurrentlyPreviewing,
                    isUnlocked = isUnlocked,
                    onPreviewClick = {
                        transitioningTheme = theme
                        viewModel.startThemePreview(theme.id)
                    },
                    onApplyClick = {
                        val success = viewModel.selectTheme(theme.id)
                        if (!success) {
                            showUnlockModalForTheme = theme
                        } else {
                            transitioningTheme = theme
                        }
                    }
                )
            }

            // Interactive Live Showcase Card (shows active components live)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = activeThemeDef.colors.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, activeThemeDef.colors.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🎨 Active Theme Component Showcase",
                                style = activeThemeDef.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = activeThemeDef.colors.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Previewing live theme components: Loading sigil, themed button, and gradient progress.",
                            style = activeThemeDef.typography.bodySmall,
                            color = activeThemeDef.colors.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Loading Motif", fontSize = 11.sp, color = activeThemeDef.colors.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(8.dp))
                                com.example.ui.theme.engine.ThemedLoadingIndicator(size = 48.dp)
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Themed Button", fontSize = 11.sp, color = activeThemeDef.colors.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(8.dp))
                                com.example.ui.theme.engine.ThemedButton(
                                    onClick = {},
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "${activeThemeDef.decoration.motifEmoji} Action",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = activeThemeDef.colors.onPrimary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Progress Bar", fontSize = 11.sp, color = activeThemeDef.colors.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(6.dp))
                        com.example.ui.theme.engine.ThemedProgressBar(progress = 0.75f)
                    }
                }
            }
        }
    }

    // Modal when user tries to apply a locked premium theme without active entitlement
    showUnlockModalForTheme?.let { lockedTheme ->
        AlertDialog(
            onDismissRequest = { showUnlockModalForTheme = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "👑 ${lockedTheme.name}")
                }
            },
            text = {
                Column {
                    Text(
                        text = "This theme is a Premium cosmetic enhancement. All core task, habit, and schedule features remain 100% free.",
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "You can preview it for free anytime! To test full unlock in development mode, activate the Developer Entitlement Simulator toggle.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.toggleSimulatedPremium(true)
                        viewModel.selectTheme(lockedTheme.id)
                        showUnlockModalForTheme = null
                    }
                ) {
                    Text("Enable & Apply (Dev Mode)")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.startThemePreview(lockedTheme.id)
                        showUnlockModalForTheme = null
                    }
                ) {
                    Text("Preview Theme")
                }
            }
        )
    }

    if (transitioningTheme != null) {
        ThemeTransitionOverlay(theme = transitioningTheme!!)
    }
    }
}

@Composable
private fun ThemeTransitionOverlay(
    theme: ThemeDefinition,
    modifier: Modifier = Modifier
) {
    val overlayAlpha = remember { Animatable(0f) }
    val motifScale = remember { Animatable(0.85f) }
    val motifFloatOffset = remember { Animatable(12f) }

    LaunchedEffect(theme) {
        overlayAlpha.snapTo(0f)
        motifScale.snapTo(0.85f)
        motifFloatOffset.snapTo(12f)

        // Phase 1 (0..300ms)
        overlayAlpha.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
        motifScale.animateTo(1.05f, tween(300, easing = FastOutSlowInEasing))

        // Phase 2 (300..1200ms)
        motifFloatOffset.animateTo(-6f, tween(900, easing = LinearEasing))

        // Phase 3 (1200..1500ms)
        motifScale.animateTo(1.15f, tween(300, easing = FastOutSlowInEasing))
        overlayAlpha.animateTo(0f, tween(300, easing = FastOutSlowInEasing))
    }

    CompositionLocalProvider(LocalLifeTheme provides theme) {
        val alpha = overlayAlpha.value
        if (alpha > 0.01f) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = alpha * 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .scale(motifScale.value)
                        .alpha(alpha * 0.45f)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    theme.colors.glowColor.copy(alpha = 0.6f),
                                    theme.colors.primary.copy(alpha = 0.25f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .offset(y = motifFloatOffset.value.dp)
                        .scale(motifScale.value)
                        .alpha(alpha)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(theme.shapes.buttonShape)
                            .background(theme.colors.surface.copy(alpha = 0.9f))
                            .border(
                                width = 2.dp,
                                color = theme.colors.primary.copy(alpha = 0.85f),
                                shape = theme.shapes.buttonShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        ThemedLoadingIndicator(size = 44.dp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "${theme.decoration.motifEmoji} ${theme.name}",
                        style = theme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = theme.colors.onSurface.copy(alpha = 0.95f),
                        modifier = Modifier
                            .background(
                                color = theme.colors.surface.copy(alpha = 0.75f),
                                shape = theme.shapes.chipShape
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ThemeCardItem(
    theme: ThemeDefinition,
    isPersisted: Boolean,
    isPreviewing: Boolean,
    isUnlocked: Boolean,
    onPreviewClick: () -> Unit,
    onApplyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = theme.shapes.cardShape,
        colors = CardDefaults.cardColors(containerColor = theme.colors.surface),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isPersisted || isPreviewing) 2.dp else 1.dp,
            color = if (isPersisted) theme.colors.primary else if (isPreviewing) Color(0xFFF59E0B) else theme.colors.outline
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("theme_card_${theme.id.idString}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Emoji Motif, Theme Name, Subtitle, and Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    // Motif Box
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(theme.shapes.buttonShape)
                            .background(theme.colors.badgeBackground)
                            .border(1.dp, theme.colors.outline, theme.shapes.buttonShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = theme.decoration.motifEmoji, fontSize = 20.sp)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = theme.name,
                                style = theme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = theme.colors.onSurface
                            )
                            if (theme.isPremium) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFF59E0B).copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "👑 PREMIUM",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFF59E0B)
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(theme.colors.primary.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "FREE",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = theme.colors.primary
                                    )
                                }
                            }
                        }
                        Text(
                            text = theme.subtitle,
                            style = theme.typography.labelSmall,
                            color = theme.colors.onSurfaceVariant
                        )
                    }
                }

                // Active / Preview Badge
                if (isPersisted) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(theme.colors.primary.copy(alpha = 0.2f))
                            .border(1.dp, theme.colors.primary, RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = theme.colors.primary, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Active", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = theme.colors.primary)
                        }
                    }
                } else if (isPreviewing) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF59E0B).copy(alpha = 0.2f))
                            .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Previewing", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Description
            Text(
                text = theme.description,
                style = theme.typography.bodyMedium,
                color = theme.colors.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Signature Palette Swatches
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Palette:",
                    fontSize = 11.sp,
                    color = theme.colors.onSurfaceVariant
                )

                val signatureColors = listOf(
                    theme.colors.primary,
                    theme.colors.secondary,
                    theme.colors.accent,
                    theme.colors.surfaceVariant,
                    theme.colors.outline
                )

                signatureColors.forEach { c ->
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(c)
                            .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons: Preview & Apply
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onPreviewClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("preview_theme_${theme.id.idString}"),
                    shape = theme.shapes.buttonShape,
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Preview", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = onApplyClick,
                    enabled = !isPersisted,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("apply_theme_${theme.id.idString}"),
                    shape = theme.shapes.buttonShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = theme.colors.primary,
                        disabledContainerColor = theme.colors.surfaceVariant
                    ),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    if (isPersisted) {
                        Text("Current Theme", fontSize = 12.sp, color = theme.colors.onSurfaceVariant)
                    } else if (!isUnlocked) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Apply (Locked)", fontSize = 12.sp)
                    } else {
                        Text("Apply Theme", fontSize = 12.sp, color = theme.colors.onPrimary)
                    }
                }
            }
        }
    }
}
