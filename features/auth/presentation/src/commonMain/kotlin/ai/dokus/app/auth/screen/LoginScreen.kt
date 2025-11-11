package ai.dokus.app.auth.screen

import ai.dokus.app.auth.viewmodel.LoginViewModel
import ai.dokus.foundation.design.components.PPrimaryButton
import ai.dokus.foundation.design.components.fields.PTextFieldEmail
import ai.dokus.foundation.design.components.fields.PTextFieldEmailDefaults
import ai.dokus.foundation.design.components.fields.PTextFieldPassword
import ai.dokus.foundation.design.components.fields.PTextFieldPasswordDefaults
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
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LoginScreen() {
    val viewModel = koinViewModel<LoginViewModel>()

    val focusManager = LocalFocusManager.current
    val navController = LocalNavController.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is LoginViewModel.Effect.NavigateToHome -> {
                    navController.replace(CoreDestination.Home)
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
    val canLogin = email.value.isNotBlank() && password.value.isNotBlank()

    Scaffold { contentPadding ->
        Box(
            Modifier
                .padding(contentPadding)
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = mutableInteractionSource
                ) {
                    focusManager.clearFocus()
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // App Title
                Text(
                    text = "Dokus",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Sign in to your account",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Email Field
                PTextFieldEmail(
                    fieldName = "Email",
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
                    fieldName = "Password",
                    value = password,
                    keyboardOptions = PTextFieldPasswordDefaults.keyboardOptions.copy(
                        imeAction = ImeAction.Done
                    ),
                    error = fieldsError.takeIf {
                        it is DokusException.Validation.WeakPassword || it is DokusException.InvalidCredentials
                    },
                    onAction = {
                        focusManager.clearFocus()
                        if (canLogin && !isLoading) {
                            viewModel.login(email, password)
                        }
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
                            text = "Forgot password?",
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
                    text = "Sign In",
                    enabled = canLogin && !isLoading,
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.login(email, password)
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
                                append("Don't have an account? ")
                            }
                            withStyle(
                                SpanStyle(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                append("Sign up")
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}