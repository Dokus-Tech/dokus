package tech.dokus.features.cashflow.presentation.screenshot

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.foundation.aura.local.ScreenSize
import tech.dokus.foundation.aura.local.ThemeManagerProvided
import tech.dokus.foundation.aura.style.Themed

/**
 * Test wrapper for Paparazzi screenshot tests.
 * Uses [FakeThemeManager] to provide theme without accessing persistence.
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
        ThemeManagerProvided(themeManager = FakeThemeManager(isDarkMode)) {
            Themed {
                Surface {
                    content()
                }
            }
        }
    }
}
