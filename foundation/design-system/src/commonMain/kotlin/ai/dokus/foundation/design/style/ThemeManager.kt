package ai.dokus.foundation.design.style

import ai.dokus.foundation.platform.Logger
import ai.dokus.foundation.platform.persistence
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Global theme manager for the application.
 *
 * Manages theme mode preference with reactive [StateFlow] and persistence.
 * This is a singleton accessed via DI (Koin).
 *
 * Usage:
 * ```kotlin
 * // Get current theme mode
 * val mode by themeManager.themeMode.collectAsState()
 *
 * // Change theme mode (persists automatically)
 * themeManager.setThemeMode(ThemeMode.DARK)
 * ```
 */
class ThemeManager {
    private val logger = Logger.withTag("ThemeManager")

    private val _themeMode = MutableStateFlow(loadSavedThemeMode())

    /** Current theme mode as a [StateFlow]. Collect this in Compose to react to changes. */
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    init {
        logger.d { "ThemeManager initialized with mode: ${_themeMode.value}" }
    }

    /**
     * Update theme mode and persist to storage.
     * Changes apply immediately to the UI via StateFlow.
     *
     * @param mode The new theme mode to apply
     */
    fun setThemeMode(mode: ThemeMode) {
        if (_themeMode.value != mode) {
            logger.i { "Theme mode changed: ${_themeMode.value} -> $mode" }
            _themeMode.value = mode
            persistence.themeModePreference = mode.toStorageString()
        }
    }

    /**
     * Load saved theme mode from persistence.
     * Returns [ThemeMode.DEFAULT] if no preference is saved.
     */
    private fun loadSavedThemeMode(): ThemeMode {
        val saved = persistence.themeModePreference
        val mode = ThemeMode.fromString(saved)
        logger.d { "Loaded theme mode from storage: $saved -> $mode" }
        return mode
    }
}
