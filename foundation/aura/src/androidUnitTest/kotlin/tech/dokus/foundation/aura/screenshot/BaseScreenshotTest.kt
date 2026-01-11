package tech.dokus.foundation.aura.screenshot

import androidx.compose.runtime.Composable
import app.cash.paparazzi.Paparazzi
import tech.dokus.foundation.aura.local.ScreenSize

/**
 * Helper object for Paparazzi screenshot tests.
 * Provides common configuration and helper methods for capturing component screenshots.
 */
object ScreenshotTestHelper {

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
     * Maps ScreenshotViewport to ScreenSize for LocalScreenSize provider.
     */
    fun ScreenshotViewport.toScreenSize(): ScreenSize = when (this) {
        ScreenshotViewport.COMPACT -> ScreenSize.SMALL
        ScreenshotViewport.MEDIUM -> ScreenSize.MEDIUM
        ScreenshotViewport.EXPANDED -> ScreenSize.LARGE
    }

    /**
     * Captures a screenshot with the given name and theme.
     * Wraps content in ScreenshotTestWrapper for proper theming.
     */
    fun Paparazzi.snapshot(
        name: String,
        isDarkMode: Boolean = false,
        screenSize: ScreenSize = ScreenSize.MEDIUM,
        content: @Composable () -> Unit
    ) {
        snapshot(name) {
            ScreenshotTestWrapper(isDarkMode = isDarkMode, screenSize = screenSize) {
                content()
            }
        }
    }

    /**
     * Captures both light and dark theme variants.
     */
    fun Paparazzi.snapshotBothThemes(
        baseName: String,
        screenSize: ScreenSize = ScreenSize.MEDIUM,
        content: @Composable () -> Unit
    ) {
        snapshot("${baseName}_light", isDarkMode = false, screenSize = screenSize, content = content)
        snapshot("${baseName}_dark", isDarkMode = true, screenSize = screenSize, content = content)
    }

    /**
     * Captures screenshots at all viewports (light theme only).
     * Use with parameterized tests where each test class handles one viewport.
     */
    fun Paparazzi.snapshotAllViewports(
        baseName: String,
        viewport: ScreenshotViewport,
        content: @Composable () -> Unit
    ) {
        val screenSize = viewport.toScreenSize()
        snapshot("${baseName}_${viewport.displayName}_light", isDarkMode = false, screenSize = screenSize, content = content)
        snapshot("${baseName}_${viewport.displayName}_dark", isDarkMode = true, screenSize = screenSize, content = content)
    }
}
