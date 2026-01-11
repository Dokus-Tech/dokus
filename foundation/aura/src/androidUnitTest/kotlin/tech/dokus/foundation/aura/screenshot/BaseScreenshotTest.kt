package tech.dokus.foundation.aura.screenshot

import androidx.compose.runtime.Composable
import app.cash.paparazzi.Paparazzi
import tech.dokus.domain.enums.Language
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.TestWrapper

/**
 * Helper object for Paparazzi screenshot tests.
 * Provides common configuration and helper methods for capturing component screenshots.
 */
object ScreenshotTestHelper {

    /**
     * Light theme parameters for testing.
     */
    val lightTheme = PreviewParameters(
        isDarkMode = false,
        language = Language.En
    )

    /**
     * Dark theme parameters for testing.
     */
    val darkTheme = PreviewParameters(
        isDarkMode = true,
        language = Language.En
    )

    /**
     * Creates a Paparazzi instance for the given viewport.
     */
    fun createPaparazzi(viewport: ScreenshotViewport): Paparazzi {
        return Paparazzi(
            deviceConfig = viewport.deviceConfig,
            showSystemUi = false,
            maxPercentDifference = 0.1
        )
    }

    /**
     * Captures a screenshot with the given name and theme.
     * Wraps content in TestWrapper for proper theming.
     */
    fun Paparazzi.snapshot(
        name: String,
        theme: PreviewParameters = lightTheme,
        content: @Composable () -> Unit
    ) {
        snapshot(name) {
            TestWrapper(parameters = theme) {
                content()
            }
        }
    }

    /**
     * Captures both light and dark theme variants.
     */
    fun Paparazzi.snapshotBothThemes(
        baseName: String,
        content: @Composable () -> Unit
    ) {
        snapshot("${baseName}_light", lightTheme, content)
        snapshot("${baseName}_dark", darkTheme, content)
    }
}
