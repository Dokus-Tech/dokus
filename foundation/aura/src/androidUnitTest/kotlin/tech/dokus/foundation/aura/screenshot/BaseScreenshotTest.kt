package tech.dokus.foundation.aura.screenshot

import androidx.compose.runtime.Composable
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import tech.dokus.domain.enums.Language
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.TestWrapper

/**
 * Base class for Paparazzi screenshot tests.
 * Provides common configuration and helper methods for capturing component screenshots.
 */
abstract class BaseScreenshotTest {

    /**
     * Subclasses must provide the Paparazzi rule with their desired viewport.
     */
    @get:Rule
    abstract val paparazzi: Paparazzi

    /**
     * Light theme parameters for testing.
     */
    protected val lightTheme = PreviewParameters(
        isDarkMode = false,
        language = Language.En
    )

    /**
     * Dark theme parameters for testing.
     */
    protected val darkTheme = PreviewParameters(
        isDarkMode = true,
        language = Language.En
    )

    /**
     * Captures a screenshot with the given name and theme.
     * Wraps content in TestWrapper for proper theming.
     */
    protected fun snapshot(
        name: String,
        theme: PreviewParameters = lightTheme,
        content: @Composable () -> Unit
    ) {
        paparazzi.snapshot(name) {
            TestWrapper(parameters = theme) {
                content()
            }
        }
    }

    /**
     * Captures both light and dark theme variants.
     */
    protected fun snapshotBothThemes(
        baseName: String,
        content: @Composable () -> Unit
    ) {
        snapshot("${baseName}_light", lightTheme, content)
        snapshot("${baseName}_dark", darkTheme, content)
    }

    companion object {
        /**
         * Creates a Paparazzi rule for the given viewport.
         */
        fun createPaparazzi(viewport: ScreenshotViewport): Paparazzi {
            return Paparazzi(
                deviceConfig = viewport.deviceConfig,
                showSystemUi = false,
                maxPercentDifference = 0.1
            )
        }
    }
}
