package com.example.domain.modules

/**
 * Lifecycle states for LifeOS downloadable and modular feature packs (#2).
 */
enum class ModuleStatus {
    AVAILABLE,
    DOWNLOADING,
    VERIFYING,
    INSTALLING,
    INSTALLED,
    UPDATE_AVAILABLE,
    FAILED,
    DISABLED,
    REMOVING;

    val isInstalledOrActive: Boolean
        get() = this == INSTALLED || this == UPDATE_AVAILABLE
}

/**
 * High-level categorization of feature modules.
 */
enum class ModuleCategory(val displayName: String, val emoji: String) {
    PRODUCTIVITY_FRAMEWORK("Productivity & Systems", "⚡"),
    SYSTEMS_THINKING("Mental Models & Logic", "🧠"),
    NEUROSCIENCE("Habits & Behavioral Design", "🌱"),
    SOFTWARE_ENGINEERING("Engineering Principles", "💻"),
    PHILOSOPHY("Resilience & Philosophy", "🏛️"),
    GENERAL("General Knowledge", "📚");

    companion object {
        fun fromString(value: String?): ModuleCategory {
            return entries.find {
                it.name.equals(value, ignoreCase = true) ||
                it.displayName.equals(value, ignoreCase = true)
            } ?: GENERAL
        }
    }
}

/**
 * Standard manifest contract for all downloadable feature modules (#2, #10).
 * Strictly declarative; zero arbitrary bytecode execution permitted.
 */
data class ModuleManifest(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val version: Int = 1,
    val versionName: String = "1.0.0",
    val author: String = "LifeOS Curators",
    val minAppVersion: Int = 1,
    val schemaVersion: Int = 1,
    val sha256: String = "",
    val packageSizeBytes: Long = 0L,
    val itemCount: Int = 0
)

/**
 * Complete runtime representation of a feature module in the LifeOS registry.
 */
data class FeatureModule(
    val manifest: ModuleManifest,
    val status: ModuleStatus = ModuleStatus.AVAILABLE,
    val installedVersion: Int? = null,
    val installedAt: Long? = null,
    val isVerified: Boolean = false,
    val errorMessage: String? = null
) {
    val id: String get() = manifest.id
    val name: String get() = manifest.name
    val description: String get() = manifest.description
    val version: Int get() = manifest.version
    val hasUpdate: Boolean
        get() = installedVersion != null && manifest.version > installedVersion
}
