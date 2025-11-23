package ai.dokus.app.auth.screen

import ai.dokus.app.auth.viewmodel.LoginViewModel
import ai.dokus.app.core.extensions.rememberIsValid
import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.app_name
import ai.dokus.app.resources.generated.auth_email_label
import ai.dokus.app.resources.generated.auth_forgot_password
import ai.dokus.app.resources.generated.auth_no_account_prefix
import ai.dokus.app.resources.generated.auth_password_label
import ai.dokus.app.resources.generated.auth_sign_in_button
import ai.dokus.app.resources.generated.auth_sign_up_link
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
import ai.dokus.foundation.domain.Email
import ai.dokus.foundation.domain.Password
import ai.dokus.foundation.domain.exceptions.DokusException
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun LoginScreen(
    viewModel: LoginViewModel = koinViewModel(),
) {
    Scaffold { contentPadding ->
        TwoPaneContainer(
            middleEffect = {
                // Shared background effects once
                EnhancedFloatingBubbles()
                SpotlightEffect()
            },
            left = { LoginContent(viewModel, contentPadding) },
            right = { SloganScreen() },
        )
    }
}

@Composable
private fun LoginContent(
    viewModel: LoginViewModel,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val focusManager = LocalFocusManager.current
    val navController = LocalNavController.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is LoginViewModel.Effect.NavigateToHome -> {
                    navController.replace(CoreDestination.Home)
                }
                is LoginViewModel.Effect.NavigateToCompanySelect -> {
                    navController.replace(AuthDestination.CompanySelect)
                }
            }
        }
    }

    val state by viewModel.state.collectAsState()
    val fieldsError: DokusException? =
        (state as? LoginViewModel.State.Error)?.exception

    var email by remember { mutableStateOf(Email("")) }
    var password by remember { mutableStateOf(Password("")) }
    val mutableInteractionSource = remember { MutableInteractionSource() }

    val isLoading = state is LoginViewModel.State.Loading
    val emailIsValid = email.rememberIsValid()
    val passwordIsValid = password.rememberIsValid()
    val canLogin = emailIsValid && passwordIsValid

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
                    value = email,
                    keyboardOptions = PTextFieldEmailDefaults.keyboardOptions.copy(
                        imeAction = ImeAction.Next
                    ),
                    error = fieldsError.takeIf { it is DokusException.Validation.InvalidEmail },
                    onAction = { focusManager.moveFocus(FocusDirection.Next) },
                    modifier = Modifier.fillMaxWidth()
                ) { email = it }

                Spacer(modifier = Modifier.height(16.dp))

                // Password Field
                PTextFieldPassword(
                    fieldName = stringResource(Res.string.auth_password_label),
                    value = password,
                    keyboardOptions = PTextFieldPasswordDefaults.keyboardOptions.copy(
                        imeAction = ImeAction.Done
                    ),
                    error = fieldsError.takeIf {
                        it is DokusException.Validation.WeakPassword || it is DokusException.InvalidCredentials
                    },
                    onAction = {
                        focusManager.clearFocus()
                        viewModel.login(email, password)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { password = it }

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
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.login(email, password)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // "or" divider
                Text(
                    text = "or",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Connect to Server Button
                POutlinedButton(
                    text = "Connect to server",
                    onClick = {
                        // TODO: Implement server connection dialog
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
