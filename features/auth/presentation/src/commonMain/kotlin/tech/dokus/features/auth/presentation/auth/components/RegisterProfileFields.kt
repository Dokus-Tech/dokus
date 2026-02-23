package tech.dokus.features.auth.presentation.auth.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.auth_first_name_label
import tech.dokus.aura.resources.auth_last_name_label
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.features.auth.presentation.auth.model.RegisterFormFields
import tech.dokus.foundation.aura.components.fields.PTextFieldName
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

@Composable
internal fun RegisterProfileFields(
    focusManager: FocusManager,
    fields: RegisterFormFields,
    onFieldsUpdate: (RegisterFormFields) -> Unit,
    error: DokusException?,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxWidth()
    ) {
        PTextFieldName(
            fieldName = stringResource(Res.string.auth_first_name_label),
            value = fields.firstName,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            error = error.takeIf { it is DokusException.Validation.InvalidFirstName },
            onAction = { focusManager.moveFocus(FocusDirection.Next) },
            onValueChange = { onFieldsUpdate(fields.copy(firstName = it)) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        PTextFieldName(
            fieldName = stringResource(Res.string.auth_last_name_label),
            value = fields.lastName,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            error = error.takeIf { it is DokusException.Validation.InvalidLastName },
            onAction = { onSubmit() },
            onValueChange = { onFieldsUpdate(fields.copy(lastName = it)) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview
@Composable
private fun RegisterProfileFieldsPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        val focusManager = LocalFocusManager.current
        RegisterProfileFields(
            focusManager = focusManager,
            fields = RegisterFormFields(),
            onFieldsUpdate = {},
            error = null,
            onSubmit = {},
        )
    }
}
