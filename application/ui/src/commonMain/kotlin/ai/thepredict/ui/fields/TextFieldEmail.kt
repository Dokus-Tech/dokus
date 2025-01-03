package ai.thepredict.ui.fields

import ai.thepredict.domain.exceptions.PredictException
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
        onValueChange = onValueChange
    )
}