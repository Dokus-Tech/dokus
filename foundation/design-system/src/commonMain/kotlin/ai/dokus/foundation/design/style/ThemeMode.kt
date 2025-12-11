package ai.dokus.foundation.design.style

import kotlinx.serialization.Serializable

/**
 * Theme mode preference for the application.
 *
 * - [LIGHT]: Always use light theme
 * - [DARK]: Always use dark theme
 * - [SYSTEM]: Follow device/system dark mode setting
 */
@Serializable
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM;

    companion object {
        /** Default theme mode (follows system setting) */
        val DEFAULT = SYSTEM

        /**
         * Parse from persistence string value.
         * Returns [DEFAULT] if value is null or invalid.
         */
        fun fromString(value: String?): ThemeMode {
            if (value == null) return DEFAULT
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: DEFAULT
        }
    }

    /**
     * Convert to persistence string value.
     */
    fun toStorageString(): String = name
}
