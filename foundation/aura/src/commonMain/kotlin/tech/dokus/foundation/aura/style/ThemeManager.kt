package tech.dokus.foundation.aura.style

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import tech.dokus.foundation.platform.Logger
import tech.dokus.foundation.platform.persistence

/**
 * Interface for theme management.
 *
 * Provides reactive [StateFlow] for theme mode and method to update it.
 * Implementations handle persistence and actual theme application.
 */
interface ThemeManager {
    /** Current theme mode as a [StateFlow]. Collect this in Compose to react to changes. */
    val themeMode: StateFlow<ThemeMode>

    /**
     * Update theme mode.
     * Implementations should persist this preference.
     *
     * @param mode The new theme mode to apply
     */
    fun setThemeMode(mode: ThemeMode)

    companion object {
        operator fun invoke(): ThemeManager = ThemeManagerImpl()
    }
}

/**
 * Fixed theme manager for tests and previews.
 * Does not access persistence - theme mode is set directly.
 *
 * @param isDarkMode If true, uses [ThemeMode.DARK]; otherwise [ThemeMode.LIGHT].
 */
class FixedThemeManager(isDarkMode: Boolean = false) : ThemeManager {
    private val _themeMode = MutableStateFlow(if (isDarkMode) ThemeMode.DARK else ThemeMode.LIGHT)
    override val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()
    override fun setThemeMode(mode: ThemeMode) { _themeMode.value = mode }
}

/**
 * Production implementation of [ThemeManager].
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
class ThemeManagerImpl : ThemeManager {
    private val logger = Logger.withTag("ThemeManager")

    private val _themeMode = MutableStateFlow(loadSavedThemeMode())

    override val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    init {
        logger.d { "ThemeManager initialized with mode: ${_themeMode.value}" }
    }

    override fun setThemeMode(mode: ThemeMode) {
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
