package com.example.ui.theme.engine

/**
 * Types of background rendering available for every theme.
 */
enum class ThemeBackgroundType(val idString: String, val displayName: String, val description: String) {
    ATMOSPHERE_DEFAULT(
        idString = "default",
        displayName = "Theme Atmosphere",
        description = "Atmospheric procedural canvas background tailored to the theme's core identity."
    ),
    PROCEDURAL_VARIANT_1(
        idString = "variant_1",
        displayName = "Pattern & Motifs",
        description = "Enhanced decorative canvas motifs with secondary theme geometry and subtle motion."
    ),
    PROCEDURAL_VARIANT_2(
        idString = "variant_2",
        displayName = "Ambient Focus",
        description = "Clean, low-noise ambient radial gradient for maximum readability and focus."
    ),
    CUSTOM_IMAGE(
        idString = "custom_image",
        displayName = "Custom Image",
        description = "Personal image selected from device storage with persistent access and opacity controls."
    );

    companion object {
        fun fromId(id: String?): ThemeBackgroundType {
            return entries.find { it.idString.equals(id, ignoreCase = true) } ?: ATMOSPHERE_DEFAULT
        }
    }
}

/**
 * Scale and positioning modes for personal background images.
 */
enum class BackgroundScaleMode(val displayName: String) {
    CROP("Crop & Fill"),
    FIT("Fit Center"),
    STRETCH("Stretch")
}

/**
 * Complete background configuration for a theme.
 * Supports independent persistence per theme, mode, and profile.
 */
data class ThemeBackgroundConfig(
    val themeId: ThemeId = ThemeId.DEFAULT,
    val type: ThemeBackgroundType = ThemeBackgroundType.ATMOSPHERE_DEFAULT,
    val customImageUri: String? = null,
    val opacity: Float = 0.85f,
    val scaleMode: BackgroundScaleMode = BackgroundScaleMode.CROP,
    val enableOverlayTint: Boolean = true
) {
    val isCustomImageActive: Boolean
        get() = type == ThemeBackgroundType.CUSTOM_IMAGE && !customImageUri.isNullOrBlank()
}
