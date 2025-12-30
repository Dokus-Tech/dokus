package ai.dokus.app.auth.components

import ai.dokus.app.auth.model.RegisterFormFields
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.auth_email_label
import tech.dokus.aura.resources.auth_password_label
import tech.dokus.foundation.aura.components.fields.PTextFieldEmail
import tech.dokus.foundation.aura.components.fields.PTextFieldEmailDefaults
import tech.dokus.foundation.aura.components.fields.PTextFieldPassword
import tech.dokus.foundation.aura.components.fields.PTextFieldPasswordDefaults
import tech.dokus.domain.exceptions.DokusException
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun RegisterCredentialsFields(
    focusManager: FocusManager,
    fields: RegisterFormFields,
    onFieldsUpdate: (RegisterFormFields) -> Unit,
    onRegisterClick: () -> Unit,
    error: DokusException?,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxWidth()
    ) {
        PTextFieldEmail(
            fieldName = stringResource(Res.string.auth_email_label),
            value = fields.email,
            error = error.takeIf {
                it is DokusException.Validation.InvalidEmail ||
                it is DokusException.UserAlreadyExists
            },
            keyboardOptions = PTextFieldEmailDefaults.keyboardOptions.copy(imeAction = ImeAction.Next),
            onAction = { focusManager.moveFocus(FocusDirection.Next) },
        ) { onFieldsUpdate(fields.copy(email = it)) }
        Spacer(modifier = Modifier.height(16.dp))
        PTextFieldPassword(
            fieldName = stringResource(Res.string.auth_password_label),
            value = fields.password,
            error = error.takeIf { it is DokusException.Validation.WeakPassword },
            keyboardOptions = PTextFieldPasswordDefaults.keyboardOptions.copy(imeAction = ImeAction.Next),
            onAction = {
                focusManager.clearFocus()
                onRegisterClick()
            },
        ) { onFieldsUpdate(fields.copy(password = it)) }
    }
}
