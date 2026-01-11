package tech.dokus.features.auth.presentation.screenshot

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.navigation.compose.rememberNavController
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.foundation.aura.local.ScreenSize
import tech.dokus.foundation.aura.local.ThemeManagerProvided
import tech.dokus.foundation.aura.style.FixedThemeManager
import tech.dokus.foundation.aura.style.Themed
import tech.dokus.navigation.local.LocalNavController

/**
 * Test wrapper for Paparazzi screenshot tests.
 * Uses [FixedThemeManager] to provide theme without accessing persistence.
 * Provides a mock NavController for screens that require navigation.
 */
@Composable
fun ScreenshotTestWrapper(
    isDarkMode: Boolean = false,
    screenSize: ScreenSize = ScreenSize.MEDIUM,
    content: @Composable () -> Unit
) {
    val navController = rememberNavController()
    CompositionLocalProvider(
        LocalInspectionMode provides true,
        LocalScreenSize provides screenSize,
        LocalNavController provides navController
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
