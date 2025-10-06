package ai.dokus.foundation.ui.fields

import ai.dokus.foundation.domain.exceptions.PredictException
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation

object PTextFieldFreeDefaults {
    val icon = null
    val onAction = {}
    val singleLine = false
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
    error: PredictException? = null,
    visualTransformation: VisualTransformation = PTextFieldFreeDefaults.visualTransformation,
    modifier: Modifier = Modifier,
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
        onValueChange = onValueChange
    )
}