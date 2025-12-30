package tech.dokus.foundation.aura.components.fields

import tech.dokus.domain.exceptions.DokusException
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation

object PTextFieldStandardDefaults {
    val icon = null
    val onAction = {}
    val singleLine = true
    val keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Text,
        capitalization = KeyboardCapitalization.Sentences,
        imeAction = ImeAction.Done
    )
    val visualTransformation = VisualTransformation.None
}

@Composable
fun PTextFieldStandard(
    fieldName: String,
    value: String,
    icon: ImageVector? = PTextFieldStandardDefaults.icon,
    singleLine: Boolean = PTextFieldStandardDefaults.singleLine,
    onAction: () -> Unit = PTextFieldStandardDefaults.onAction,
    keyboardOptions: KeyboardOptions = PTextFieldStandardDefaults.keyboardOptions,
    error: DokusException? = null,
    visualTransformation: VisualTransformation = PTextFieldStandardDefaults.visualTransformation,
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
        minLines = 1,
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