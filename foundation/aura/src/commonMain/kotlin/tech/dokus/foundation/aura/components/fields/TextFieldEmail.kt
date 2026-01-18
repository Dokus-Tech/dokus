package tech.dokus.foundation.aura.components.fields

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import compose.icons.FeatherIcons
import compose.icons.feathericons.AtSign
import tech.dokus.domain.Email
import tech.dokus.domain.exceptions.DokusException

object PTextFieldEmailDefaults {
    val icon = FeatherIcons.AtSign
    val onAction = {}
    const val singleLine = true
    val keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Email,
        capitalization = KeyboardCapitalization.None,
        imeAction = ImeAction.Done
    )
    val visualTransformation = VisualTransformation.None
}

@Composable
fun PTextFieldEmail(
    fieldName: String,
    value: Email,
    icon: ImageVector? = PTextFieldEmailDefaults.icon,
    singleLine: Boolean = PTextFieldEmailDefaults.singleLine,
    onAction: () -> Unit = PTextFieldEmailDefaults.onAction,
    keyboardOptions: KeyboardOptions = PTextFieldEmailDefaults.keyboardOptions,
    error: DokusException? = null,
    visualTransformation: VisualTransformation = PTextFieldEmailDefaults.visualTransformation,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null,
    showClearButton: Boolean = false,
    onValueChange: (Email) -> Unit,
) {
    PTextField(
        fieldName = fieldName,
        value = value.value,
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
        onValueChange = { onValueChange(Email(it.lowercase())) }
    )
}
