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
import compose.icons.feathericons.Hash
import tech.dokus.domain.exceptions.DokusException
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

object PTextFieldTaxNumberDefaults {
    val icon = FeatherIcons.Hash
    val onAction = {}
    const val singleLine = true
    val keyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Text,
        capitalization = KeyboardCapitalization.Words,
        imeAction = ImeAction.Done
    )
    val visualTransformation = VisualTransformation.None
}

@Composable
fun PTextFieldTaxNumber(
    fieldName: String,
    value: String,
    icon: ImageVector? = PTextFieldTaxNumberDefaults.icon,
    singleLine: Boolean = PTextFieldTaxNumberDefaults.singleLine,
    onAction: () -> Unit = PTextFieldTaxNumberDefaults.onAction,
    keyboardOptions: KeyboardOptions = PTextFieldTaxNumberDefaults.keyboardOptions,
    error: DokusException? = null,
    visualTransformation: VisualTransformation = PTextFieldTaxNumberDefaults.visualTransformation,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null,
    showClearButton: Boolean = false,
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
        onClear = onClear,
        showClearButton = showClearButton,
        onValueChange = onValueChange
    )
}

@Preview
@Composable
private fun PTextFieldTaxNumberPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        PTextFieldTaxNumber(
            fieldName = "VAT Number",
            value = "BE0123456789",
            onValueChange = {}
        )
    }
}
