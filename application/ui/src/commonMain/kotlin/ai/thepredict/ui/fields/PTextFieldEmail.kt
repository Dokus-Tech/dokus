package ai.thepredict.ui.fields

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import compose.icons.FeatherIcons
import compose.icons.feathericons.AtSign
import compose.icons.feathericons.User

object PTextFieldEmailDefaults {
    val icon = FeatherIcons.AtSign
    val onAction = {}
    val singleLine = true
    val keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Email,
        capitalization = KeyboardCapitalization.None,
        imeAction = ImeAction.Done
    )
    val isError = false
    val visualTransformation = VisualTransformation.None
}

@Composable
fun PTextFieldEmail(
    fieldName: String,
    value: String,
    icon: ImageVector? = PTextFieldEmailDefaults.icon,
    singleLine: Boolean = PTextFieldEmailDefaults.singleLine,
    onAction: () -> Unit = PTextFieldEmailDefaults.onAction,
    keyboardOptions: KeyboardOptions = PTextFieldEmailDefaults.keyboardOptions,
    isError: Boolean = PTextFieldEmailDefaults.isError,
    visualTransformation: VisualTransformation = PTextFieldEmailDefaults.visualTransformation,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        modifier = modifier,
        value = value,
        onValueChange = onValueChange,
        isError = isError,
        visualTransformation = visualTransformation,
        label = {
            Text(fieldName)
        },
        leadingIcon = {
            if (icon != null) {
                Icon(icon, fieldName)
            }
        },
        singleLine = singleLine,
        keyboardActions = KeyboardActions(
            onNext = {
                onAction()
            },
            onDone = {
                onAction()
            }
        ),
        keyboardOptions = keyboardOptions,
    )
}