package tech.dokus.foundation.aura.screenshot

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.foundation.aura.local.ScreenSize
import tech.dokus.foundation.aura.local.ThemeManagerProvided
import tech.dokus.foundation.aura.style.FixedThemeManager
import tech.dokus.foundation.aura.style.Themed

/**
 * Test wrapper for Paparazzi screenshot tests.
 *
 * Uses [FixedThemeManager] to provide theme without accessing persistence.
 * Wraps content in [Themed] for full design system styling.
 *
 * @param isDarkMode Whether to use dark theme.
 * @param screenSize The screen size for responsive layouts.
 * @param content The content to screenshot.
 */
@Composable
fun ScreenshotTestWrapper(
    isDarkMode: Boolean = false,
    screenSize: ScreenSize = ScreenSize.MEDIUM,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalInspectionMode provides true,
        LocalScreenSize provides screenSize
    ) {
        ThemeManagerProvided(themeManager = FixedThemeManager(isDarkMode)) {
            Themed {
                Surface {
                    content()
                }
            }
        }
    }
}
