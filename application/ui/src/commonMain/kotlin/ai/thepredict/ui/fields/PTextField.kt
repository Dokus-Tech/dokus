package ai.thepredict.ui.fields

import ai.thepredict.domain.exceptions.PredictException
import ai.thepredict.ui.PErrorText
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
import androidx.compose.ui.text.input.VisualTransformation

@Composable
fun PTextField(
    fieldName: String,
    value: String,
    icon: ImageVector?,
    singleLine: Boolean,
    onAction: () -> Unit,
    keyboardOptions: KeyboardOptions,
    error: PredictException?,
    visualTransformation: VisualTransformation,
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