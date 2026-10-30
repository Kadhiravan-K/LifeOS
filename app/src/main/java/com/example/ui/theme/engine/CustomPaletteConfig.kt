package com.example.ui.theme.engine

import androidx.compose.ui.graphics.Color

/**
 * Data model holding custom color overrides for a specific brightness mode (Light or Dark).
 */
data class CustomColorPalette(
    val primaryHex: Long? = null,
    val secondaryHex: Long? = null,
    val accentHex: Long? = null,
    val cardHex: Long? = null,
    val surfaceHex: Long? = null,
    val backgroundHex: Long? = null
) {
    val isCustomized: Boolean
        get() = primaryHex != null || secondaryHex != null || accentHex != null ||
                cardHex != null || surfaceHex != null || backgroundHex != null

    fun getPrimaryColor(default: Color): Color = primaryHex?.let { Color(it) } ?: default
    fun getSecondaryColor(default: Color): Color = secondaryHex?.let { Color(it) } ?: default
    fun getAccentColor(default: Color): Color = accentHex?.let { Color(it) } ?: default
    fun getCardColor(default: Color): Color = cardHex?.let { Color(it) } ?: default
    fun getSurfaceColor(default: Color): Color = surfaceHex?.let { Color(it) } ?: default
    fun getBackgroundColor(default: Color): Color = backgroundHex?.let { Color(it) } ?: default
}

/**
 * State holding independent customizations for Light and Dark modes.
 */
data class IndependentThemeCustomization(
    val isEnabled: Boolean = false,
    val lightPalette: CustomColorPalette = CustomColorPalette(),
    val darkPalette: CustomColorPalette = CustomColorPalette()
) {
    fun getActivePalette(isLightMode: Boolean): CustomColorPalette {
        return if (isLightMode) lightPalette else darkPalette
    }

    val isAnyCustomized: Boolean
        get() = isEnabled && (lightPalette.isCustomized || darkPalette.isCustomized)
}
