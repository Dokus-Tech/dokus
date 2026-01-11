package tech.dokus.foundation.aura.screenshot.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import org.junit.Rule
import org.junit.Test
import tech.dokus.domain.enums.Language
import tech.dokus.foundation.aura.components.PBackButton
import tech.dokus.foundation.aura.components.PButton
import tech.dokus.foundation.aura.components.PButtonVariant
import tech.dokus.foundation.aura.components.PIconPosition
import tech.dokus.foundation.aura.components.POutlinedButton
import tech.dokus.foundation.aura.components.PPrimaryButton
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.TestWrapper

class ButtonScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig(
            screenWidth = 600,
            screenHeight = 960,
            density = Density.XXHIGH,
            softButtons = false
        ),
        showSystemUi = false,
        maxPercentDifference = 0.1
    )

    private val lightTheme = PreviewParameters(
        isDarkMode = false,
        language = Language.En
    )

    private val darkTheme = PreviewParameters(
        isDarkMode = true,
        language = Language.En
    )

    @Test
    fun pButton_default() {
        paparazzi.snapshot("PButton_default_light") {
            TestWrapper(parameters = lightTheme) {
                PButton(text = "Default Button", onClick = {})
            }
        }
        paparazzi.snapshot("PButton_default_dark") {
            TestWrapper(parameters = darkTheme) {
                PButton(text = "Default Button", onClick = {})
            }
        }
    }

    @Test
    fun pButton_withLeadingIcon() {
        paparazzi.snapshot("PButton_withLeadingIcon_light") {
            TestWrapper(parameters = lightTheme) {
                PButton(
                    text = "With Icon",
                    icon = Icons.Default.Add,
                    iconPosition = PIconPosition.Leading,
                    onClick = {}
                )
            }
        }
        paparazzi.snapshot("PButton_withLeadingIcon_dark") {
            TestWrapper(parameters = darkTheme) {
                PButton(
                    text = "With Icon",
                    icon = Icons.Default.Add,
                    iconPosition = PIconPosition.Leading,
                    onClick = {}
                )
            }
        }
    }

    @Test
    fun pButton_withTrailingIcon() {
        paparazzi.snapshot("PButton_withTrailingIcon_light") {
            TestWrapper(parameters = lightTheme) {
                PButton(
                    text = "Next",
                    icon = Icons.Default.Check,
                    iconPosition = PIconPosition.Trailing,
                    onClick = {}
                )
            }
        }
        paparazzi.snapshot("PButton_withTrailingIcon_dark") {
            TestWrapper(parameters = darkTheme) {
                PButton(
                    text = "Next",
                    icon = Icons.Default.Check,
                    iconPosition = PIconPosition.Trailing,
                    onClick = {}
                )
            }
        }
    }

    @Test
    fun pButton_outline() {
        paparazzi.snapshot("PButton_outline_light") {
            TestWrapper(parameters = lightTheme) {
                PButton(
                    text = "Outlined",
                    variant = PButtonVariant.Outline,
                    onClick = {}
                )
            }
        }
        paparazzi.snapshot("PButton_outline_dark") {
            TestWrapper(parameters = darkTheme) {
                PButton(
                    text = "Outlined",
                    variant = PButtonVariant.Outline,
                    onClick = {}
                )
            }
        }
    }

    @Test
    fun pButton_loading() {
        paparazzi.snapshot("PButton_loading_light") {
            TestWrapper(parameters = lightTheme) {
                PButton(text = "Loading", isLoading = true, onClick = {})
            }
        }
        paparazzi.snapshot("PButton_loading_dark") {
            TestWrapper(parameters = darkTheme) {
                PButton(text = "Loading", isLoading = true, onClick = {})
            }
        }
    }

    @Test
    fun pButton_disabled() {
        paparazzi.snapshot("PButton_disabled_light") {
            TestWrapper(parameters = lightTheme) {
                PButton(text = "Disabled", isEnabled = false, onClick = {})
            }
        }
        paparazzi.snapshot("PButton_disabled_dark") {
            TestWrapper(parameters = darkTheme) {
                PButton(text = "Disabled", isEnabled = false, onClick = {})
            }
        }
    }

    @Test
    fun pPrimaryButton_allStates() {
        paparazzi.snapshot("PPrimaryButton_allStates_light") {
            TestWrapper(parameters = lightTheme) {
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
        paparazzi.snapshot("PPrimaryButton_allStates_dark") {
            TestWrapper(parameters = darkTheme) {
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
    }

    @Test
    fun pOutlinedButton_allStates() {
        paparazzi.snapshot("POutlinedButton_allStates_light") {
            TestWrapper(parameters = lightTheme) {
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
        paparazzi.snapshot("POutlinedButton_allStates_dark") {
            TestWrapper(parameters = darkTheme) {
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
    }

    @Test
    fun pBackButton() {
        paparazzi.snapshot("PBackButton_light") {
            TestWrapper(parameters = lightTheme) {
                PBackButton(onBackPress = {})
            }
        }
        paparazzi.snapshot("PBackButton_dark") {
            TestWrapper(parameters = darkTheme) {
                PBackButton(onBackPress = {})
            }
        }
    }
}
