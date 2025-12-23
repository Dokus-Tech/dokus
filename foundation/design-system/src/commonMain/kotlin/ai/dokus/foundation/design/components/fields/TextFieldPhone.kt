package ai.dokus.foundation.design.components.fields

import ai.dokus.foundation.domain.PhoneNumber
import ai.dokus.foundation.domain.exceptions.DokusException
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import compose.icons.FeatherIcons
import compose.icons.feathericons.Phone

object PTextFieldPhoneDefaults {
    val icon = FeatherIcons.Phone
    val onAction = {}
    val singleLine = true
    val keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Phone,
        imeAction = ImeAction.Done
    )
    val visualTransformation = VisualTransformation.None
}

@Composable
fun PTextFieldPhone(
    fieldName: String,
    value: PhoneNumber,
    icon: ImageVector? = PTextFieldPhoneDefaults.icon,
    singleLine: Boolean = PTextFieldPhoneDefaults.singleLine,
    onAction: () -> Unit = PTextFieldPhoneDefaults.onAction,
    keyboardOptions: KeyboardOptions = PTextFieldPhoneDefaults.keyboardOptions,
    error: DokusException? = null,
    visualTransformation: VisualTransformation = PTextFieldPhoneDefaults.visualTransformation,
    modifier: Modifier = Modifier,
    onValueChange: (PhoneNumber) -> Unit,
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
        onValueChange = { onValueChange(PhoneNumber(it)) }
    )
}
