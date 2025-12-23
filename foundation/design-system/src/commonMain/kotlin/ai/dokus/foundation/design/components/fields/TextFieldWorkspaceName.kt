package ai.dokus.foundation.design.components.fields

import ai.dokus.foundation.domain.exceptions.DokusException
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

object PTextFieldWorkspaceNameDefaults {
    val icon = FeatherIcons.Briefcase
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
        onValueChange = onValueChange
    )
}