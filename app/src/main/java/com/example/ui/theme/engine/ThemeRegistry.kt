package com.example.ui.theme.engine

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object ThemeRegistry {

    val DEFAULT_THEME = ThemeDefinition(
        id = ThemeId.DEFAULT,
        name = "LifeOS Minimal",
        subtitle = "Clean Executive Focus",
        description = "Modern slate minimalism with sharp contrast, focused ergonomics, and distraction-free design.",
        isPremium = false,
        colors = ThemeColors(
            primary = Color(0xFF2563EB),
            onPrimary = Color(0xFFFFFFFF),
            secondary = Color(0xFF4F46E5),
            onSecondary = Color(0xFFFFFFFF),
            accent = Color(0xFF10B981),
            background = Color(0xFF090D16),
            surface = Color(0xFF0F172A),
            surfaceVariant = Color(0xFF1E293B),
            onBackground = Color(0xFFF8FAFC),
            onSurface = Color(0xFFF8FAFC),
            onSurfaceVariant = Color(0xFF94A3B8),
            outline = Color(0xFF334155),
            glowColor = Color(0x332563EB),
            gradientPrimary = listOf(Color(0xFF2563EB), Color(0xFF3B82F6)),
            gradientSurface = listOf(Color(0xFF0F172A), Color(0xFF1E293B)),
            honeycombBorder = Color(0xFF334155),
            honeycombDefaultFill = Color(0xFF10B981),
            badgeBackground = Color(0xFF1E293B),
            isDarkPalette = true
        ),
        typography = ThemeTypography.createDefault(FontFamily.Default),
        shapes = ThemeShapes(
            cardShape = RoundedCornerShape(16.dp),
            buttonShape = RoundedCornerShape(12.dp),
            borderWidth = 1.dp,
            cornerRadiusDp = 16.dp,
            isAngularCyber = false
        ),
        motion = ThemeMotion(
            animationDurationMs = 250,
            enableParticleEffects = false,
            enableAtmosphereGlow = false,
            bounceStiffness = 300f
        ),
        decoration = ThemeDecoration(
            motifEmoji = "⚡",
            tagline = "Master your daily routines",
            taskSectionTitle = "Daily Tasks",
            habitsSectionTitle = "Habit Streaks",
            challengeSectionTitle = "75-Day Challenge",
            honeycombTitle = "My Daily Honeycomb"
        ),
        honeycombEngraving = HoneycombEngravingType.MINIMAL
    )

    val ARCANE_ACADEMY = ThemeDefinition(
        id = ThemeId.ARCANE_ACADEMY,
        name = "Arcane Academy",
        subtitle = "Mystic Spellbook & Sorcery",
        description = "Transform productivity into mastering spells. Enriched with mystic parchment, golden rune outlines, and glowing arcane sigils.",
        isPremium = true,
        colors = ThemeColors(
            primary = Color(0xFFC084FC), // Glowing lavender
            onPrimary = Color(0xFF1E0836),
            secondary = Color(0xFFF59E0B), // Antique gold
            onSecondary = Color(0xFF1A1202),
            accent = Color(0xFFE879F9), // Fuchsia glow
            background = Color(0xFF0E071A), // Midnight violet
            surface = Color(0xFF1A0F2E), // Spellbook deep violet
            surfaceVariant = Color(0xFF2B194C), // Arcane chamber
            onBackground = Color(0xFFFAF5FF), // Parchment cream
            onSurface = Color(0xFFF5E8FF),
            onSurfaceVariant = Color(0xFFD8B4FE),
            outline = Color(0xFFB45309), // Antique gold trim
            glowColor = Color(0x66C084FC),
            gradientPrimary = listOf(Color(0xFF7E22CE), Color(0xFFC084FC)),
            gradientSurface = listOf(Color(0xFF1A0F2E), Color(0xFF281446)),
            honeycombBorder = Color(0xFFF59E0B),
            honeycombDefaultFill = Color(0xFF9333EA),
            badgeBackground = Color(0xFF3B185F),
            isDarkPalette = true
        ),
        typography = ThemeTypography(
            displayLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 28.sp),
            titleLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 20.sp),
            titleMedium = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
            bodyLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal, fontSize = 15.sp),
            bodyMedium = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal, fontSize = 13.sp),
            labelMedium = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
            labelSmall = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium, fontSize = 10.sp)
        ),
        shapes = ThemeShapes(
            cardShape = RoundedCornerShape(topStart = 20.dp, topEnd = 6.dp, bottomEnd = 20.dp, bottomStart = 6.dp),
            buttonShape = RoundedCornerShape(14.dp),
            borderWidth = 1.5.dp,
            cornerRadiusDp = 18.dp,
            isAngularCyber = false
        ),
        motion = ThemeMotion(
            animationDurationMs = 350,
            enableParticleEffects = true,
            enableAtmosphereGlow = true,
            bounceStiffness = 220f
        ),
        decoration = ThemeDecoration(
            motifEmoji = "🔮",
            tagline = "Conjure focus, master knowledge",
            taskSectionTitle = "Grimoire of Directives",
            habitsSectionTitle = "Enchanted Skill Mastery",
            challengeSectionTitle = "75 Magical Ascensions",
            honeycombTitle = "Arcane Rune Honeycomb"
        ),
        honeycombEngraving = HoneycombEngravingType.RUNE
    )

    val CYBER_NEXUS = ThemeDefinition(
        id = ThemeId.CYBER_NEXUS,
        name = "Cyber Nexus",
        subtitle = "Futuristic Neural Command",
        description = "High-tech terminal aesthetic featuring neon circuitry, scanning telemetry bars, angular cybernetic panels, and holographic glow.",
        isPremium = true,
        colors = ThemeColors(
            primary = Color(0xFF06B6D4), // Neon cyan
            onPrimary = Color(0xFF021B24),
            secondary = Color(0xFFEC4899), // Neon magenta
            onSecondary = Color(0xFF1E020E),
            accent = Color(0xFF3B82F6), // Electric blue
            background = Color(0xFF04070C), // Deep cyber void
            surface = Color(0xFF090E17), // Data-grid dark
            surfaceVariant = Color(0xFF121B2C),
            onBackground = Color(0xFFE0F2FE),
            onSurface = Color(0xFFCCFBF1),
            onSurfaceVariant = Color(0xFF67E8F9),
            outline = Color(0xFF0891B2), // Neon border line
            glowColor = Color(0x6606B6D4),
            gradientPrimary = listOf(Color(0xFF06B6D4), Color(0xFF3B82F6)),
            gradientSurface = listOf(Color(0xFF090E17), Color(0xFF0D1726)),
            honeycombBorder = Color(0xFF06B6D4),
            honeycombDefaultFill = Color(0xFF0891B2),
            badgeBackground = Color(0xFF0C2438),
            isDarkPalette = true
        ),
        typography = ThemeTypography(
            displayLarge = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 26.sp),
            titleLarge = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 18.sp),
            titleMedium = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
            bodyLarge = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Normal, fontSize = 14.sp),
            bodyMedium = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Normal, fontSize = 12.sp),
            labelMedium = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp),
            labelSmall = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium, fontSize = 9.sp)
        ),
        shapes = ThemeShapes(
            cardShape = CutCornerShape(topStart = 16.dp, bottomEnd = 16.dp),
            buttonShape = CutCornerShape(8.dp),
            chipShape = CutCornerShape(4.dp),
            dialogShape = CutCornerShape(16.dp),
            borderWidth = 1.5.dp,
            cornerRadiusDp = 12.dp,
            isAngularCyber = true
        ),
        motion = ThemeMotion(
            animationDurationMs = 200,
            enableParticleEffects = true,
            enableAtmosphereGlow = true,
            bounceStiffness = 400f
        ),
        decoration = ThemeDecoration(
            motifEmoji = "⚡",
            tagline = "Execute protocols, optimize neural bandwidth",
            taskSectionTitle = "Directive Queue",
            habitsSectionTitle = "Cybernetic Performance Matrix",
            challengeSectionTitle = "75-Cycle Cyber Protocol",
            honeycombTitle = "Nexus Core Honeycomb"
        ),
        honeycombEngraving = HoneycombEngravingType.CIRCUIT
    )

    val ADVENTURE_REALM = ThemeDefinition(
        id = ThemeId.ADVENTURE_REALM,
        name = "Adventure Realm",
        subtitle = "Cozy Fantasy Guild & Quests",
        description = "Turn daily responsibilities into legendary heroic quests. Styled with aged tavern parchment, warm wood grain, and guild seals.",
        isPremium = true,
        colors = ThemeColors(
            primary = Color(0xFFF59E0B), // Warm amber gold
            onPrimary = Color(0xFF2E1700),
            secondary = Color(0xFF16A34A), // Emerald forest
            onSecondary = Color(0xFF03220E),
            accent = Color(0xFFD97706),
            background = Color(0xFF141009), // Dark tavern wood
            surface = Color(0xFF211A10), // Parchment bulletin
            surfaceVariant = Color(0xFF332819), // Oak trim
            onBackground = Color(0xFFFEF3C7), // Warm parchment
            onSurface = Color(0xFFFDE68A),
            onSurfaceVariant = Color(0xFFD97706),
            outline = Color(0xFF92400E), // Embossed leather
            glowColor = Color(0x66F59E0B),
            gradientPrimary = listOf(Color(0xFFD97706), Color(0xFFF59E0B)),
            gradientSurface = listOf(Color(0xFF211A10), Color(0xFF2C2216)),
            honeycombBorder = Color(0xFFD97706),
            honeycombDefaultFill = Color(0xFF15803D),
            badgeBackground = Color(0xFF422E14),
            isDarkPalette = true
        ),
        typography = ThemeTypography(
            displayLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 28.sp),
            titleLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 20.sp),
            titleMedium = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
            bodyLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal, fontSize = 15.sp),
            bodyMedium = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal, fontSize = 13.sp),
            labelMedium = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
            labelSmall = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium, fontSize = 10.sp)
        ),
        shapes = ThemeShapes(
            cardShape = RoundedCornerShape(18.dp),
            buttonShape = RoundedCornerShape(12.dp),
            borderWidth = 2.dp,
            cornerRadiusDp = 18.dp,
            isAngularCyber = false
        ),
        motion = ThemeMotion(
            animationDurationMs = 300,
            enableParticleEffects = true,
            enableAtmosphereGlow = true,
            bounceStiffness = 250f
        ),
        decoration = ThemeDecoration(
            motifEmoji = "⚔️",
            tagline = "Answer the call, claim the realm",
            taskSectionTitle = "Guild Quest Log",
            habitsSectionTitle = "Heroic Skill Progression",
            challengeSectionTitle = "75-League Grand Expedition",
            honeycombTitle = "Guild Badge Honeycomb"
        ),
        honeycombEngraving = HoneycombEngravingType.COMPASS_BADGE
    )

    val COSMIC_EXPLORER = ThemeDefinition(
        id = ThemeId.COSMIC_EXPLORER,
        name = "Cosmic Explorer",
        subtitle = "Deep Space Starlight Odyssey",
        description = "Chart personal frontiers through an astronautical mission interface with starfield depths, orbital rings, and celestial energy cores.",
        isPremium = true,
        colors = ThemeColors(
            primary = Color(0xFF38BDF8), // Electric star-blue
            onPrimary = Color(0xFF03223A),
            secondary = Color(0xFF818CF8), // Nebula violet
            onSecondary = Color(0xFF0C1033),
            accent = Color(0xFF2DD4BF), // Starlight cyan
            background = Color(0xFF050811), // Deep space black
            surface = Color(0xFF0B1120), // Orbital control bay
            surfaceVariant = Color(0xFF15203B),
            onBackground = Color(0xFFF0F9FF),
            onSurface = Color(0xFFE0F2FE),
            onSurfaceVariant = Color(0xFF93C5FD),
            outline = Color(0xFF1E3A8A), // Celestial ring
            glowColor = Color(0x6638BDF8),
            gradientPrimary = listOf(Color(0xFF2563EB), Color(0xFF38BDF8)),
            gradientSurface = listOf(Color(0xFF0B1120), Color(0xFF111D36)),
            honeycombBorder = Color(0xFF38BDF8),
            honeycombDefaultFill = Color(0xFF2563EB),
            badgeBackground = Color(0xFF172554),
            isDarkPalette = true
        ),
        typography = ThemeTypography.createDefault(FontFamily.SansSerif),
        shapes = ThemeShapes(
            cardShape = RoundedCornerShape(22.dp),
            buttonShape = RoundedCornerShape(16.dp),
            chipShape = RoundedCornerShape(12.dp),
            dialogShape = RoundedCornerShape(24.dp),
            borderWidth = 1.2.dp,
            cornerRadiusDp = 22.dp,
            isAngularCyber = false
        ),
        motion = ThemeMotion(
            animationDurationMs = 280,
            enableParticleEffects = true,
            enableAtmosphereGlow = true,
            bounceStiffness = 300f
        ),
        decoration = ThemeDecoration(
            motifEmoji = "🚀",
            tagline = "Explore your universe of potential",
            taskSectionTitle = "Mission Directives",
            habitsSectionTitle = "Planetary Habitation Matrix",
            challengeSectionTitle = "75-Day Deep Space Odyssey",
            honeycombTitle = "Orbital Star-Map Honeycomb"
        ),
        honeycombEngraving = HoneycombEngravingType.ORBITAL
    )

    val allThemes: List<ThemeDefinition> = listOf(
        DEFAULT_THEME,
        ARCANE_ACADEMY,
        CYBER_NEXUS,
        ADVENTURE_REALM,
        COSMIC_EXPLORER
    )

    fun getTheme(id: ThemeId): ThemeDefinition {
        return allThemes.find { it.id == id } ?: DEFAULT_THEME
    }

    fun getTheme(idString: String): ThemeDefinition {
        val themeId = ThemeId.fromId(idString)
        return getTheme(themeId)
    }
}
