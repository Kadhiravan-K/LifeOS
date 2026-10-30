package com.example.ui.theme.engine

enum class ThemeId(val idString: String, val displayName: String, val isPremium: Boolean) {
    DEFAULT("default", "LifeOS Minimal", false),
    ARCANE_ACADEMY("arcane_academy", "Arcane Academy", true),
    CYBER_NEXUS("cyber_nexus", "Cyber Nexus", true),
    ADVENTURE_REALM("adventure_realm", "Adventure Realm", true),
    COSMIC_EXPLORER("cosmic_explorer", "Cosmic Explorer", true),
    GLASS("glass", "Glassmorphism", true);

    companion object {
        fun fromId(id: String?): ThemeId {
            return entries.find { it.idString.equals(id, ignoreCase = true) } ?: DEFAULT
        }
    }
}
