package ai.dokus.app.auth.screen

import ai.dokus.app.auth.viewmodel.LoginAction
import ai.dokus.app.auth.viewmodel.LoginContainer
import ai.dokus.app.auth.viewmodel.LoginIntent
import ai.dokus.app.auth.viewmodel.LoginState
import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.action_or
import ai.dokus.app.resources.generated.app_name
import ai.dokus.app.resources.generated.auth_email_label
import ai.dokus.app.resources.generated.auth_forgot_password
import ai.dokus.app.resources.generated.auth_no_account_prefix
import ai.dokus.app.resources.generated.auth_password_label
import ai.dokus.app.resources.generated.auth_sign_in_button
import ai.dokus.app.resources.generated.auth_sign_up_link
import ai.dokus.app.resources.generated.connect_to_server
import ai.dokus.foundation.design.components.POutlinedButton
import ai.dokus.foundation.design.components.PPrimaryButton
import ai.dokus.foundation.design.components.background.EnhancedFloatingBubbles
import ai.dokus.foundation.design.components.background.SpotlightEffect
import ai.dokus.foundation.design.components.fields.PTextFieldEmail
import ai.dokus.foundation.design.components.fields.PTextFieldEmailDefaults
import ai.dokus.foundation.design.components.fields.PTextFieldPassword
import ai.dokus.foundation.design.components.fields.PTextFieldPasswordDefaults
import ai.dokus.foundation.design.components.layout.TwoPaneContainer
import ai.dokus.foundation.design.constrains.limitWidthCenteredContent
import ai.dokus.foundation.design.constrains.withContentPadding
import ai.dokus.foundation.navigation.destinations.AuthDestination
import ai.dokus.foundation.navigation.destinations.CoreDestination
import ai.dokus.foundation.navigation.local.LocalNavController
import ai.dokus.foundation.navigation.navigateTo
import ai.dokus.foundation.navigation.replace
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
import androidx.compose.runtime.getValue
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
import pro.respawn.flowmvi.api.IntentReceiver
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.foundation.app.mvi.container
import tech.dokus.foundation.app.state.exceptionIfError

@Composable
internal fun LoginScreen(
    container: LoginContainer = container()
) {
    val navController = LocalNavController.current

    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            LoginAction.NavigateToHome -> navController.replace(CoreDestination.Home)
            LoginAction.NavigateToWorkspaceSelect -> navController.replace(AuthDestination.WorkspaceSelect)
        }
    }

    Scaffold { contentPadding ->
        TwoPaneContainer(
            middleEffect = {
                EnhancedFloatingBubbles()
                SpotlightEffect()
            },
            left = {
                with(container.store) {
                    LoginContent(state, contentPadding)
                }
            },
            right = { SloganScreen() },
        )
    }
}

@Composable
private fun IntentReceiver<LoginIntent>.LoginContent(
    state: LoginState,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val focusManager = LocalFocusManager.current
    val navController = LocalNavController.current

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
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
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
                ) { intent(LoginIntent.UpdateEmail(it)) }

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
                        intent(LoginIntent.LoginClicked)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { intent(LoginIntent.UpdatePassword(it)) }

                // Forgot Password Link
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    TextButton(onClick = { navController.navigateTo(AuthDestination.ForgotPassword) }) {
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
                        intent(LoginIntent.LoginClicked)
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
                        navController.navigateTo(AuthDestination.ServerConnection())
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Sign Up Link
                TextButton(
                    onClick = { navController.navigateTo(AuthDestination.Register) }
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
