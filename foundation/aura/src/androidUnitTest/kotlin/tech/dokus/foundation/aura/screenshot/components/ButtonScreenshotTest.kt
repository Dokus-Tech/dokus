package tech.dokus.foundation.aura.screenshot.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import tech.dokus.foundation.aura.components.PBackButton
import tech.dokus.foundation.aura.components.PButton
import tech.dokus.foundation.aura.components.PButtonVariant
import tech.dokus.foundation.aura.components.PIconPosition
import tech.dokus.foundation.aura.components.POutlinedButton
import tech.dokus.foundation.aura.components.PPrimaryButton
import tech.dokus.foundation.aura.screenshot.ScreenshotTestHelper
import tech.dokus.foundation.aura.screenshot.ScreenshotTestHelper.snapshotAllViewports
import tech.dokus.foundation.aura.screenshot.ScreenshotViewport

/**
 * Parameterized screenshot tests for button components.
 * Runs at all viewport sizes: COMPACT, MEDIUM, EXPANDED.
 */
@RunWith(Parameterized::class)
class ButtonScreenshotTest(private val viewport: ScreenshotViewport) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun viewports() = ScreenshotViewport.entries.toList()
    }

    @get:Rule
    val paparazzi = ScreenshotTestHelper.createPaparazzi(viewport)

    @Test
    fun pButton_default() {
        paparazzi.snapshotAllViewports("PButton_default", viewport) {
            PButton(text = "Default Button", onClick = {})
        }
    }

    @Test
    fun pButton_withLeadingIcon() {
        paparazzi.snapshotAllViewports("PButton_withLeadingIcon", viewport) {
            PButton(
                text = "With Icon",
                icon = Icons.Default.Add,
                iconPosition = PIconPosition.Leading,
                onClick = {}
            )
        }
    }

    @Test
    fun pButton_withTrailingIcon() {
        paparazzi.snapshotAllViewports("PButton_withTrailingIcon", viewport) {
            PButton(
                text = "Next",
                icon = Icons.Default.Check,
                iconPosition = PIconPosition.Trailing,
                onClick = {}
            )
        }
    }

    @Test
    fun pButton_outline() {
        paparazzi.snapshotAllViewports("PButton_outline", viewport) {
            PButton(
                text = "Outlined",
                variant = PButtonVariant.Outline,
                onClick = {}
            )
        }
    }

    @Test
    fun pButton_loading() {
        paparazzi.snapshotAllViewports("PButton_loading", viewport) {
            PButton(text = "Loading", isLoading = true, onClick = {})
        }
    }

    @Test
    fun pButton_disabled() {
        paparazzi.snapshotAllViewports("PButton_disabled", viewport) {
            PButton(text = "Disabled", isEnabled = false, onClick = {})
        }
    }

    @Test
    fun pPrimaryButton_allStates() {
        paparazzi.snapshotAllViewports("PPrimaryButton_allStates", viewport) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PPrimaryButton(text = "Primary", onClick = {})
                PPrimaryButton(text = "Loading", isLoading = true, onClick = {})
                PPrimaryButton(text = "Disabled", enabled = false, onClick = {})
            }
        }
    }

    @Test
    fun pOutlinedButton_allStates() {
        paparazzi.snapshotAllViewports("POutlinedButton_allStates", viewport) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                POutlinedButton(text = "Outlined", onClick = {})
                POutlinedButton(text = "Loading", isLoading = true, onClick = {})
                POutlinedButton(text = "Disabled", enabled = false, onClick = {})
            }
        }
    }

    @Test
    fun pBackButton() {
        paparazzi.snapshotAllViewports("PBackButton", viewport) {
            PBackButton(onBackPress = {})
        }
    }
}
