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

object PTextFieldFreeDefaults {
    val icon = null
    val onAction = {}
    val singleLine = false
    val keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Text,
        capitalization = KeyboardCapitalization.Sentences,
        imeAction = ImeAction.Done
    )
    val isError = false
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
    isError: Boolean = PTextFieldFreeDefaults.isError,
    visualTransformation: VisualTransformation = PTextFieldFreeDefaults.visualTransformation,
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