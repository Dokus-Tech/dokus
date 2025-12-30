package tech.dokus.foundation.aura.local

import tech.dokus.foundation.aura.style.ThemeManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * CompositionLocal providing access to [ThemeManager].
 *
 * Usage in any composable:
 * ```kotlin
 * val themeManager = LocalThemeManager.current
 * val mode by themeManager.themeMode.collectAsState()
 * ```
 */
val LocalThemeManager = staticCompositionLocalOf<ThemeManager> {
    error("ThemeManager not provided. Wrap your composable tree with ThemeManagerProvided.")
}

/**
 * Provider for [ThemeManager] CompositionLocal.
 *
 * Wraps app content and provides theme management capabilities throughout the Compose tree.
 *
 * @param themeManager The global [ThemeManager] instance (injected via Koin)
 * @param content The composable content that will have access to [LocalThemeManager]
 */
@Composable
fun ThemeManagerProvided(
    themeManager: ThemeManager,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalThemeManager provides themeManager) {
        content()
    }
}
