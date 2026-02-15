package tech.dokus.features.auth.presentation.auth.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.profile_change_password_save
import tech.dokus.aura.resources.profile_change_password_title
import tech.dokus.aura.resources.profile_change_password_warning
import tech.dokus.aura.resources.profile_confirm_password
import tech.dokus.aura.resources.profile_current_password
import tech.dokus.aura.resources.profile_new_password
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.features.auth.mvi.ChangePasswordIntent
import tech.dokus.features.auth.mvi.ChangePasswordState
import tech.dokus.foundation.app.state.exceptionIfError
import tech.dokus.foundation.aura.components.PPrimaryButton
import tech.dokus.foundation.aura.components.common.PTopAppBar
import tech.dokus.foundation.aura.components.fields.PTextFieldPassword
import tech.dokus.foundation.aura.components.fields.PTextFieldPasswordDefaults
import tech.dokus.foundation.aura.components.layout.TwoPaneContainer
import tech.dokus.foundation.aura.constrains.limitWidthCenteredContent
import tech.dokus.foundation.aura.constrains.withContentPadding

@Composable
internal fun ChangePasswordScreen(
    state: ChangePasswordState,
    snackbarHostState: SnackbarHostState,
    onIntent: (ChangePasswordIntent) -> Unit
) {
    Scaffold(
        topBar = { PTopAppBar(Res.string.profile_change_password_title) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        TwoPaneContainer(
            left = {
                ChangePasswordContent(
                    state = state,
                    contentPadding = padding,
                    onIntent = onIntent
                )
            },
            right = { SloganScreen() }
        )
    }
}

@Composable
private fun ChangePasswordContent(
    state: ChangePasswordState,
    contentPadding: PaddingValues,
    onIntent: (ChangePasswordIntent) -> Unit
) {
    val fieldsError = state.exceptionIfError()
    val isSubmitting = state is ChangePasswordState.Submitting
    val passwordsMatch = state.newPassword.value == state.confirmPassword.value
    val canSubmit = state.currentPassword.isValid &&
        state.newPassword.isValid &&
        state.confirmPassword.isValid &&
        passwordsMatch &&
        !isSubmitting

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .withContentPadding(),
        verticalArrangement = Arrangement.Center
    ) {
        Column(modifier = Modifier.limitWidthCenteredContent()) {
            Text(
                text = stringResource(Res.string.profile_change_password_title),
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.height(24.dp))

            PTextFieldPassword(
                fieldName = stringResource(Res.string.profile_current_password),
                value = state.currentPassword,
                keyboardOptions = PTextFieldPasswordDefaults.keyboardOptions.copy(
                    imeAction = ImeAction.Next
                ),
                error = fieldsError.takeIf { it is DokusException.InvalidCredentials },
                onAction = {},
                modifier = Modifier.fillMaxWidth(),
                onValueChange = { onIntent(ChangePasswordIntent.UpdateCurrentPassword(it)) }
            )

            Spacer(Modifier.height(12.dp))

            PTextFieldPassword(
                fieldName = stringResource(Res.string.profile_new_password),
                value = state.newPassword,
                keyboardOptions = PTextFieldPasswordDefaults.keyboardOptions.copy(
                    imeAction = ImeAction.Next
                ),
                error = fieldsError.takeIf { it is DokusException.Validation.WeakPassword },
                onAction = {},
                modifier = Modifier.fillMaxWidth(),
                onValueChange = { onIntent(ChangePasswordIntent.UpdateNewPassword(it)) }
            )

            Spacer(Modifier.height(12.dp))

            PTextFieldPassword(
                fieldName = stringResource(Res.string.profile_confirm_password),
                value = state.confirmPassword,
                keyboardOptions = PTextFieldPasswordDefaults.keyboardOptions.copy(
                    imeAction = ImeAction.Done
                ),
                error = fieldsError.takeIf { it is DokusException.Validation.PasswordDoNotMatch },
                onAction = { onIntent(ChangePasswordIntent.SubmitClicked) },
                modifier = Modifier.fillMaxWidth(),
                onValueChange = { onIntent(ChangePasswordIntent.UpdateConfirmPassword(it)) }
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = stringResource(Res.string.profile_change_password_warning),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            PPrimaryButton(
                text = stringResource(Res.string.profile_change_password_save),
                enabled = canSubmit,
                isLoading = isSubmitting,
                onClick = { onIntent(ChangePasswordIntent.SubmitClicked) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
