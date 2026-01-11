package tech.dokus.foundation.aura.screenshot.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import tech.dokus.foundation.aura.components.fields.PTextField
import tech.dokus.foundation.aura.screenshot.ScreenshotTestHelper
import tech.dokus.foundation.aura.screenshot.ScreenshotTestHelper.snapshotAllViewports
import tech.dokus.foundation.aura.screenshot.ScreenshotViewport

@RunWith(Parameterized::class)
class TextFieldScreenshotTest(private val viewport: ScreenshotViewport) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun viewports() = ScreenshotViewport.entries.toList()
    }

    @get:Rule
    val paparazzi = ScreenshotTestHelper.createPaparazzi(viewport)

    @Test
    fun pTextField_empty() {
        paparazzi.snapshotAllViewports("PTextField_empty", viewport) {
            PTextField(
                fieldName = "Email",
                value = "",
                icon = Icons.Default.Email,
                singleLine = true,
                minLines = 1,
                onAction = {},
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                error = null,
                visualTransformation = VisualTransformation.None,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                onValueChange = {}
            )
        }
    }

    @Test
    fun pTextField_withValue() {
        paparazzi.snapshotAllViewports("PTextField_withValue", viewport) {
            PTextField(
                fieldName = "Email",
                value = "user@example.com",
                icon = Icons.Default.Email,
                singleLine = true,
                minLines = 1,
                onAction = {},
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                error = null,
                visualTransformation = VisualTransformation.None,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                onValueChange = {}
            )
        }
    }

    @Test
    fun pTextField_withoutIcon() {
        paparazzi.snapshotAllViewports("PTextField_withoutIcon", viewport) {
            PTextField(
                fieldName = "Name",
                value = "John Doe",
                icon = null,
                singleLine = true,
                minLines = 1,
                onAction = {},
                keyboardOptions = KeyboardOptions.Default,
                error = null,
                visualTransformation = VisualTransformation.None,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                onValueChange = {}
            )
        }
    }

    @Test
    fun pTextField_disabled() {
        paparazzi.snapshotAllViewports("PTextField_disabled", viewport) {
            PTextField(
                fieldName = "Email",
                value = "disabled@example.com",
                icon = Icons.Default.Email,
                singleLine = true,
                minLines = 1,
                onAction = {},
                keyboardOptions = KeyboardOptions.Default,
                error = null,
                visualTransformation = VisualTransformation.None,
                enabled = false,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                onValueChange = {}
            )
        }
    }

    @Test
    fun pTextField_multiline() {
        paparazzi.snapshotAllViewports("PTextField_multiline", viewport) {
            PTextField(
                fieldName = "Description",
                value = "This is a longer text that spans multiple lines to demonstrate multiline input capability.",
                icon = null,
                singleLine = false,
                minLines = 3,
                onAction = {},
                keyboardOptions = KeyboardOptions.Default,
                error = null,
                visualTransformation = VisualTransformation.None,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                onValueChange = {}
            )
        }
    }

    @Test
    fun pTextField_noClearButton() {
        paparazzi.snapshotAllViewports("PTextField_noClearButton", viewport) {
            PTextField(
                fieldName = "Read Only",
                value = "Cannot clear this",
                icon = null,
                singleLine = true,
                minLines = 1,
                onAction = {},
                keyboardOptions = KeyboardOptions.Default,
                error = null,
                visualTransformation = VisualTransformation.None,
                showClearButton = false,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                onValueChange = {}
            )
        }
    }

    @Test
    fun pTextField_allVariants() {
        paparazzi.snapshotAllViewports("PTextField_allVariants", viewport) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PTextField(
                    fieldName = "Name",
                    value = "",
                    icon = Icons.Default.Person,
                    singleLine = true,
                    minLines = 1,
                    onAction = {},
                    keyboardOptions = KeyboardOptions.Default,
                    error = null,
                    visualTransformation = VisualTransformation.None,
                    modifier = Modifier.fillMaxWidth(),
                    onValueChange = {}
                )
                PTextField(
                    fieldName = "Email",
                    value = "filled@example.com",
                    icon = Icons.Default.Email,
                    singleLine = true,
                    minLines = 1,
                    onAction = {},
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    error = null,
                    visualTransformation = VisualTransformation.None,
                    modifier = Modifier.fillMaxWidth(),
                    onValueChange = {}
                )
                PTextField(
                    fieldName = "Disabled",
                    value = "Disabled field",
                    icon = null,
                    singleLine = true,
                    minLines = 1,
                    onAction = {},
                    keyboardOptions = KeyboardOptions.Default,
                    error = null,
                    visualTransformation = VisualTransformation.None,
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    onValueChange = {}
                )
            }
        }
    }
}
