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
import compose.icons.feathericons.Briefcase
import tech.dokus.domain.exceptions.DokusException
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

object PTextFieldWorkspaceNameDefaults {
    val icon = FeatherIcons.Briefcase
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
fun PTextFieldWorkspaceName(
    fieldName: String,
    value: String,
    icon: ImageVector? = PTextFieldWorkspaceNameDefaults.icon,
    singleLine: Boolean = PTextFieldWorkspaceNameDefaults.singleLine,
    onAction: () -> Unit = PTextFieldWorkspaceNameDefaults.onAction,
    keyboardOptions: KeyboardOptions = PTextFieldWorkspaceNameDefaults.keyboardOptions,
    error: DokusException? = null,
    visualTransformation: VisualTransformation = PTextFieldWorkspaceNameDefaults.visualTransformation,
    enabled: Boolean = true,
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
        enabled = enabled,
        modifier = modifier,
        onClear = onClear,
        showClearButton = showClearButton,
        onValueChange = onValueChange
    )
}

@Preview
@Composable
private fun PTextFieldWorkspaceNamePreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        PTextFieldWorkspaceName(
            fieldName = "Workspace Name",
            value = "My Company",
            onValueChange = {}
        )
    }
}
