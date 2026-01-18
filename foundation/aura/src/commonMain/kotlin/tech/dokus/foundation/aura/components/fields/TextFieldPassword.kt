package tech.dokus.foundation.aura.components.fields

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import compose.icons.FeatherIcons
import compose.icons.feathericons.Key
import tech.dokus.domain.Password
import tech.dokus.domain.exceptions.DokusException

object PTextFieldPasswordDefaults {
    val icon = FeatherIcons.Key
    val onAction = {}
    const val singleLine = true
    val keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Password,
        capitalization = KeyboardCapitalization.None,
        imeAction = ImeAction.Done,
    )
    val visualTransformation = PasswordVisualTransformation()
}

@Composable
fun PTextFieldPassword(
    fieldName: String,
    value: Password,
    icon: ImageVector? = PTextFieldPasswordDefaults.icon,
    singleLine: Boolean = PTextFieldPasswordDefaults.singleLine,
    onAction: () -> Unit = PTextFieldPasswordDefaults.onAction,
    keyboardOptions: KeyboardOptions = PTextFieldPasswordDefaults.keyboardOptions,
    error: DokusException? = null,
    visualTransformation: VisualTransformation = PTextFieldPasswordDefaults.visualTransformation,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null,
    showClearButton: Boolean = false,
    onValueChange: (Password) -> Unit,
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
        onValueChange = {
            onValueChange(Password(it))
        }
    )
}
