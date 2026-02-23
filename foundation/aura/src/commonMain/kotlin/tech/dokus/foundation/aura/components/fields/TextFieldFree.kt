package tech.dokus.foundation.aura.components.fields

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import tech.dokus.domain.exceptions.DokusException
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

object PTextFieldFreeDefaults {
    val icon = null
    val onAction = {}
    const val singleLine = false
    val keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Text,
        capitalization = KeyboardCapitalization.Sentences,
        imeAction = ImeAction.Done
    )
    val visualTransformation = VisualTransformation.None
}

@Composable
fun PTextFieldFree(
    fieldName: String,
    value: String,
    icon: ImageVector? = PTextFieldFreeDefaults.icon,
    singleLine: Boolean = PTextFieldFreeDefaults.singleLine,
    onAction: () -> Unit = PTextFieldFreeDefaults.onAction,
    keyboardOptions: KeyboardOptions = PTextFieldFreeDefaults.keyboardOptions,
    error: DokusException? = null,
    visualTransformation: VisualTransformation = PTextFieldFreeDefaults.visualTransformation,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null,
    showClearButton: Boolean = false,
    onValueChange: (String) -> Unit,
) {
    PTextField(
        fieldName = fieldName,
        value = value,
        icon = icon,
        singleLine = singleLine,
        minLines = 3,
        onAction = onAction,
        keyboardOptions = keyboardOptions,
        error = error,
        visualTransformation = visualTransformation,
        modifier = modifier,
        onClear = onClear,
        showClearButton = showClearButton,
        onValueChange = onValueChange
    )
}

@Preview
@Composable
private fun PTextFieldFreePreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        PTextFieldFree(
            fieldName = "Notes",
            value = "Some free text content",
            onValueChange = {}
        )
    }
}
