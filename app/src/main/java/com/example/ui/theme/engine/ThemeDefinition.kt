package com.example.ui.theme.engine

enum class HoneycombEngravingType {
    MINIMAL,
    RUNE,
    CIRCUIT,
    COMPASS_BADGE,
    ORBITAL
}

data class ThemeDefinition(
    val id: ThemeId,
    val name: String,
    val subtitle: String,
    val description: String,
    val isPremium: Boolean,
    val colors: ThemeColors,
    val typography: ThemeTypography,
    val shapes: ThemeShapes,
    val motion: ThemeMotion,
    val decoration: ThemeDecoration,
    val honeycombEngraving: HoneycombEngravingType
)
