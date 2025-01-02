package ai.thepredict.ui.fields

import ai.thepredict.domain.exceptions.PredictException
import ai.thepredict.ui.PErrorText
import ai.thepredict.ui.extensions.localized
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
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
    error: PredictException? = null,
    visualTransformation: VisualTransformation = PTextFieldEmailDefaults.visualTransformation,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        modifier = modifier,
        value = value,
        onValueChange = onValueChange,
        isError = error != null,
        supportingText = {
            if (error != null) {
                PErrorText(error)
            }
        },
        visualTransformation = visualTransformation,
        label = {
            Text(fieldName)
        },
        leadingIcon = {
            if (icon != null) {
                val tint = if (error != null) {
                    MaterialTheme.colorScheme.error
                } else {
                    LocalContentColor.current
                }
                Icon(icon, fieldName, tint = tint)
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