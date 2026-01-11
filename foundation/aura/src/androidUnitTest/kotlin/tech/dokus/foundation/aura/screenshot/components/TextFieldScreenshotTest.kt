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
import tech.dokus.foundation.aura.components.fields.PTextField
import tech.dokus.foundation.aura.screenshot.BaseScreenshotTest
import tech.dokus.foundation.aura.screenshot.ScreenshotViewport

class TextFieldScreenshotTest : BaseScreenshotTest() {

    @get:Rule
    override val paparazzi = createPaparazzi(ScreenshotViewport.MEDIUM)

    @Test
    fun pTextField_empty() {
        snapshotBothThemes("PTextField_empty") {
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
        snapshotBothThemes("PTextField_withValue") {
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
        snapshotBothThemes("PTextField_withoutIcon") {
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
        snapshotBothThemes("PTextField_disabled") {
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
        snapshotBothThemes("PTextField_multiline") {
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
        snapshotBothThemes("PTextField_noClearButton") {
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
        snapshotBothThemes("PTextField_allVariants") {
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
