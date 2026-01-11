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
import tech.dokus.foundation.aura.components.PBackButton
import tech.dokus.foundation.aura.components.PButton
import tech.dokus.foundation.aura.components.PButtonVariant
import tech.dokus.foundation.aura.components.PIconPosition
import tech.dokus.foundation.aura.components.POutlinedButton
import tech.dokus.foundation.aura.components.PPrimaryButton
import tech.dokus.foundation.aura.screenshot.BaseScreenshotTest
import tech.dokus.foundation.aura.screenshot.ScreenshotViewport

class ButtonScreenshotTest : BaseScreenshotTest() {

    @get:Rule
    override val paparazzi = createPaparazzi(ScreenshotViewport.MEDIUM)

    @Test
    fun pButton_default() {
        snapshotBothThemes("PButton_default") {
            PButton(text = "Default Button", onClick = {})
        }
    }

    @Test
    fun pButton_withLeadingIcon() {
        snapshotBothThemes("PButton_withLeadingIcon") {
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
        snapshotBothThemes("PButton_withTrailingIcon") {
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
        snapshotBothThemes("PButton_outline") {
            PButton(
                text = "Outlined",
                variant = PButtonVariant.Outline,
                onClick = {}
            )
        }
    }

    @Test
    fun pButton_outlineWithIcon() {
        snapshotBothThemes("PButton_outlineWithIcon") {
            PButton(
                text = "Outlined",
                variant = PButtonVariant.Outline,
                icon = Icons.Default.Add,
                onClick = {}
            )
        }
    }

    @Test
    fun pButton_loading() {
        snapshotBothThemes("PButton_loading") {
            PButton(text = "Loading", isLoading = true, onClick = {})
        }
    }

    @Test
    fun pButton_disabled() {
        snapshotBothThemes("PButton_disabled") {
            PButton(text = "Disabled", isEnabled = false, onClick = {})
        }
    }

    @Test
    fun pPrimaryButton_allStates() {
        snapshotBothThemes("PPrimaryButton_allStates") {
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
        snapshotBothThemes("POutlinedButton_allStates") {
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
        snapshotBothThemes("PBackButton") {
            PBackButton(onBackPress = {})
        }
    }
}
