package tech.dokus.features.auth.presentation.auth.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_or
import tech.dokus.aura.resources.auth_email_label
import tech.dokus.aura.resources.auth_new_to_dokus_prefix
import tech.dokus.aura.resources.auth_forgot_password
import tech.dokus.aura.resources.auth_onboarding_belgian_infrastructure
import tech.dokus.aura.resources.auth_onboarding_connect_own_server
import tech.dokus.aura.resources.auth_onboarding_data_encrypted
import tech.dokus.aura.resources.auth_onboarding_self_hosted_subtitle
import tech.dokus.aura.resources.auth_onboarding_welcome_back
import tech.dokus.aura.resources.auth_password_label
import tech.dokus.aura.resources.auth_create_account_link
import tech.dokus.aura.resources.auth_sign_in_button
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.features.auth.mvi.LoginIntent
import tech.dokus.features.auth.mvi.LoginState
import tech.dokus.features.auth.presentation.auth.components.onboarding.OnboardingBrandVariant
import tech.dokus.features.auth.presentation.auth.components.onboarding.OnboardingSplitShell
import tech.dokus.foundation.app.state.exceptionIfError
import tech.dokus.foundation.aura.components.DokusGlassSurface
import tech.dokus.foundation.aura.components.PPrimaryButton
import tech.dokus.foundation.aura.components.fields.PTextFieldEmail
import tech.dokus.foundation.aura.components.fields.PTextFieldEmailDefaults
import tech.dokus.foundation.aura.components.fields.PTextFieldPassword
import tech.dokus.foundation.aura.components.fields.PTextFieldPasswordDefaults
import tech.dokus.foundation.aura.components.text.DokusLogo
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.extensions.dismissKeyboardOnTapOutside

@Composable
internal fun LoginScreen(
    state: LoginState,
    onIntent: (LoginIntent) -> Unit,
    onForgotPassword: () -> Unit,
    onConnectToServer: () -> Unit,
    onRegister: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val fieldsError = state.exceptionIfError()

    val isLoading = state is LoginState.Authenticating
    val canLogin = state.email.isValid && state.password.isValid

    OnboardingSplitShell(
        brandVariant = OnboardingBrandVariant.Primary,
        modifier = Modifier.dismissKeyboardOnTapOutside()
    ) {
        DokusLogo.Full()

        Spacer(modifier = Modifier.height(Constraints.Spacing.xxLarge))

        Text(
            text = stringResource(Res.string.auth_onboarding_welcome_back),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(Constraints.Spacing.small))

        Text(
            text = stringResource(Res.string.auth_onboarding_belgian_infrastructure),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(Constraints.Spacing.xxLarge))

        PTextFieldEmail(
            fieldName = stringResource(Res.string.auth_email_label),
            value = state.email,
            keyboardOptions = PTextFieldEmailDefaults.keyboardOptions.copy(imeAction = ImeAction.Next),
            error = fieldsError.takeIf { it is DokusException.Validation.InvalidEmail },
            onAction = { focusManager.moveFocus(FocusDirection.Next) },
            modifier = Modifier.fillMaxWidth(),
            onValueChange = { onIntent(LoginIntent.UpdateEmail(it)) },
        )

        Spacer(modifier = Modifier.height(Constraints.Spacing.large))

        PTextFieldPassword(
            fieldName = stringResource(Res.string.auth_password_label),
            value = state.password,
            keyboardOptions = PTextFieldPasswordDefaults.keyboardOptions.copy(imeAction = ImeAction.Done),
            error = fieldsError.takeIf {
                it is DokusException.Validation.WeakPassword || it is DokusException.InvalidCredentials
            },
            onAction = {
                focusManager.clearFocus()
                onIntent(LoginIntent.LoginClicked)
            },
            modifier = Modifier.fillMaxWidth(),
            onValueChange = { onIntent(LoginIntent.UpdatePassword(it)) },
        )

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            TextButton(onClick = onForgotPassword) {
                Text(
                    text = stringResource(Res.string.auth_forgot_password),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Spacer(modifier = Modifier.height(Constraints.Spacing.large))

        PPrimaryButton(
            text = stringResource(Res.string.auth_sign_in_button),
            enabled = canLogin && !isLoading,
            isLoading = isLoading,
            onClick = {
                focusManager.clearFocus()
                onIntent(LoginIntent.LoginClicked)
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(Constraints.Spacing.large))

        Text(
            text = stringResource(Res.string.action_or),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        Spacer(modifier = Modifier.height(Constraints.Spacing.large))

        DokusGlassSurface(
            onClick = onConnectToServer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
            ) {
                Spacer(modifier = Modifier.height(Constraints.Spacing.large))
                Text(
                    text = stringResource(Res.string.auth_onboarding_connect_own_server),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(Res.string.auth_onboarding_self_hosted_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(Constraints.Spacing.large))
            }
        }

        Spacer(modifier = Modifier.height(Constraints.Spacing.xLarge))

        Text(
            text = stringResource(Res.string.auth_onboarding_data_encrypted),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        Spacer(modifier = Modifier.height(Constraints.Spacing.small))

        TextButton(
            onClick = onRegister,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Normal,
                        ),
                    ) {
                        append(stringResource(Res.string.auth_new_to_dokus_prefix))
                    }
                    withStyle(
                        SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                        ),
                    ) {
                        append(stringResource(Res.string.auth_create_account_link))
                    }
                },
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun LoginScreenPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(
        tech.dokus.foundation.aura.tooling.PreviewParametersProvider::class,
    ) parameters: tech.dokus.foundation.aura.tooling.PreviewParameters,
) {
    tech.dokus.foundation.aura.tooling.TestWrapper(parameters) {
        LoginScreen(
            state = LoginState.Idle(),
            onIntent = {},
            onForgotPassword = {},
            onConnectToServer = {},
            onRegister = {},
        )
    }
}
