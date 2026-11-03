package com.example.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.LifeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationAnimationsScreen(
    viewModel: LifeViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val animArcane by viewModel.animArcaneEnabled.collectAsState()
    val animCyber by viewModel.animCyberEnabled.collectAsState()
    val animAdventure by viewModel.animAdventureEnabled.collectAsState()
    val animCosmic by viewModel.animCosmicEnabled.collectAsState()
    val animGlass by viewModel.animGlassEnabled.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Navigation Animations") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("nav_animations_back")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier.fillMaxSize().testTag("nav_animations_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Themed Center Button Animations",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Control particle effects, pulsing energy, and rotational animations on the elevated Quick Add button per theme. Disabling an animation preserves the theme's static styling.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
            )

            // Arcane Academy Control
            ThemeAnimationSwitchCard(
                title = "Arcane Academy Animation",
                subtitle = "Orbiting magic orbs and pulsating mana glow",
                checked = animArcane,
                onCheckedChange = { viewModel.setThemeAnimationEnabled("arcane_academy", it) },
                testTag = "anim_toggle_arcane"
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Cyber Nexus Control
            ThemeAnimationSwitchCard(
                title = "Cyber Nexus Animation",
                subtitle = "Neon power ring pulsing and electronic energy feedback",
                checked = animCyber,
                onCheckedChange = { viewModel.setThemeAnimationEnabled("cyber_nexus", it) },
                testTag = "anim_toggle_cyber"
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Adventure Realm Control
            ThemeAnimationSwitchCard(
                title = "Adventure Realm Animation",
                subtitle = "Compass rose rotational alignment and direction accents",
                checked = animAdventure,
                onCheckedChange = { viewModel.setThemeAnimationEnabled("adventure_realm", it) },
                testTag = "anim_toggle_adventure"
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Cosmic Explorer Control
            ThemeAnimationSwitchCard(
                title = "Cosmic Explorer Animation",
                subtitle = "Starlight particle nebula halo and cosmic glow",
                checked = animCosmic,
                onCheckedChange = { viewModel.setThemeAnimationEnabled("cosmic_explorer", it) },
                testTag = "anim_toggle_cosmic"
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Glassmorphism Control
            ThemeAnimationSwitchCard(
                title = "Glassmorphism Animation",
                subtitle = "Soft light reflection sweep and ambient glass glow",
                checked = animGlass,
                onCheckedChange = { viewModel.setThemeAnimationEnabled("glass", it) },
                testTag = "anim_toggle_glass"
            )
        }
    }
}

@Composable
private fun ThemeAnimationSwitchCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.testTag(testTag)
            )
        }
    }
}
