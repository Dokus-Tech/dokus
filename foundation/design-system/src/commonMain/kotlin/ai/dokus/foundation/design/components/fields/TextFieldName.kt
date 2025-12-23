package ai.dokus.foundation.design.components.fields

import ai.dokus.foundation.domain.Name
import ai.dokus.foundation.domain.exceptions.DokusException
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.intl.Locale
import compose.icons.FeatherIcons
import compose.icons.feathericons.User

object PTextFieldNameDefaults {
    val icon = FeatherIcons.User
    val onAction = {}
    val singleLine = true
    val keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Text,
        capitalization = KeyboardCapitalization.Words,
        imeAction = ImeAction.Done
    )
    val visualTransformation = VisualTransformation.None
}

@Composable
fun PTextFieldName(
    fieldName: String,
    value: Name,
    icon: ImageVector? = PTextFieldNameDefaults.icon,
    singleLine: Boolean = PTextFieldNameDefaults.singleLine,
    onAction: () -> Unit = PTextFieldNameDefaults.onAction,
    keyboardOptions: KeyboardOptions = PTextFieldNameDefaults.keyboardOptions,
    error: DokusException? = null,
    visualTransformation: VisualTransformation = PTextFieldNameDefaults.visualTransformation,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null,
    onValueChange: (Name) -> Unit,
) {
    val locale = Locale.current
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
        onValueChange = { value -> onValueChange(Name(value.capitalize(locale))) }
    )
}