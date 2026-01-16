package tech.dokus.features.auth.presentation.auth.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_or
import tech.dokus.aura.resources.app_name
import tech.dokus.aura.resources.auth_email_label
import tech.dokus.aura.resources.auth_forgot_password
import tech.dokus.aura.resources.auth_no_account_prefix
import tech.dokus.aura.resources.auth_password_label
import tech.dokus.aura.resources.auth_sign_in_button
import tech.dokus.aura.resources.auth_sign_up_link
import tech.dokus.aura.resources.connect_to_server
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.features.auth.mvi.LoginIntent
import tech.dokus.features.auth.mvi.LoginState
import tech.dokus.foundation.app.state.exceptionIfError
import tech.dokus.foundation.aura.components.POutlinedButton
import tech.dokus.foundation.aura.components.PPrimaryButton
import tech.dokus.foundation.aura.components.background.CalmParticleField
import tech.dokus.foundation.aura.components.fields.PTextFieldEmail
import tech.dokus.foundation.aura.components.fields.PTextFieldEmailDefaults
import tech.dokus.foundation.aura.components.fields.PTextFieldPassword
import tech.dokus.foundation.aura.components.fields.PTextFieldPasswordDefaults
import tech.dokus.foundation.aura.components.layout.TwoPaneContainer
import tech.dokus.foundation.aura.constrains.limitWidthCenteredContent
import tech.dokus.foundation.aura.constrains.withContentPadding
import tech.dokus.foundation.aura.style.brandGold

@Composable
internal fun LoginScreen(
    state: LoginState,
    onIntent: (LoginIntent) -> Unit,
    onForgotPassword: () -> Unit,
    onConnectToServer: () -> Unit,
    onRegister: () -> Unit,
) {
    Scaffold { contentPadding ->
        TwoPaneContainer(
            middleEffect = {
                CalmParticleField()
            },
            left = {
                LoginContent(
                    state = state,
                    onIntent = onIntent,
                    contentPadding = contentPadding,
                    onForgotPassword = onForgotPassword,
                    onConnectToServer = onConnectToServer,
                    onRegister = onRegister,
                )
            },
            right = { SloganScreen() },
        )
    }
}

@Composable
private fun LoginContent(
    state: LoginState,
    onIntent: (LoginIntent) -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    onForgotPassword: () -> Unit,
    onConnectToServer: () -> Unit,
    onRegister: () -> Unit,
) {
    val focusManager = LocalFocusManager.current

    val fieldsError = state.exceptionIfError()
    val mutableInteractionSource = remember { MutableInteractionSource() }

    val isLoading = state is LoginState.Authenticating
    val canLogin = state.email.isValid && state.password.isValid

    Box(
        Modifier
            .padding(contentPadding)
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = mutableInteractionSource
            ) { focusManager.clearFocus() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .withContentPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Column(
                modifier = Modifier.limitWidthCenteredContent(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // App Title
                Text(
                    text = stringResource(Res.string.app_name),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.brandGold
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Email Field
                PTextFieldEmail(
                    fieldName = stringResource(Res.string.auth_email_label),
                    value = state.email,
                    keyboardOptions = PTextFieldEmailDefaults.keyboardOptions.copy(
                        imeAction = ImeAction.Next
                    ),
                    error = fieldsError.takeIf { it is DokusException.Validation.InvalidEmail },
                    onAction = { focusManager.moveFocus(FocusDirection.Next) },
                    modifier = Modifier.fillMaxWidth()
                ) { onIntent(LoginIntent.UpdateEmail(it)) }

                Spacer(modifier = Modifier.height(16.dp))

                // Password Field
                PTextFieldPassword(
                    fieldName = stringResource(Res.string.auth_password_label),
                    value = state.password,
                    keyboardOptions = PTextFieldPasswordDefaults.keyboardOptions.copy(
                        imeAction = ImeAction.Done
                    ),
                    error = fieldsError.takeIf {
                        it is DokusException.Validation.WeakPassword || it is DokusException.InvalidCredentials
                    },
                    onAction = {
                        focusManager.clearFocus()
                        onIntent(LoginIntent.LoginClicked)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { onIntent(LoginIntent.UpdatePassword(it)) }

                // Forgot Password Link
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    TextButton(onClick = onForgotPassword) {
                        Text(
                            text = stringResource(Res.string.auth_forgot_password),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Login Button
                PPrimaryButton(
                    text = stringResource(Res.string.auth_sign_in_button),
                    enabled = canLogin && !isLoading,
                    isLoading = isLoading,
                    onClick = {
                        focusManager.clearFocus()
                        onIntent(LoginIntent.LoginClicked)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // "or" divider
                Text(
                    text = stringResource(Res.string.action_or),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Connect to Server Button
                POutlinedButton(
                    text = stringResource(Res.string.connect_to_server),
                    onClick = {
                        onConnectToServer()
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Sign Up Link
                TextButton(
                    onClick = onRegister
                ) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                SpanStyle(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            ) {
                                append(stringResource(Res.string.auth_no_account_prefix))
                            }
                            withStyle(
                                SpanStyle(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                append(stringResource(Res.string.auth_sign_up_link))
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
