package ai.thepredict.app.onboarding.authentication.login

import ai.thepredict.app.navigation.CoreNavigation
import ai.thepredict.app.navigation.OnboardingNavigation
import ai.thepredict.domain.exceptions.PredictException
import ai.thepredict.ui.PButton
import ai.thepredict.ui.PErrorText
import ai.thepredict.ui.fields.PTextFieldEmail
import ai.thepredict.ui.fields.PTextFieldEmailDefaults
import ai.thepredict.ui.fields.PTextFieldPassword
import ai.thepredict.ui.theme.createColorScheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.registry.rememberScreen
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mohamedrejeb.calf.ui.progress.AdaptiveCircularProgressIndicator

internal class LoginScreen : Screen {

    @Composable
    override fun Content() {
        val colorScheme = createColorScheme(false)

        MaterialTheme(colorScheme = colorScheme) {
            val viewModel = rememberScreenModel { LoginViewModel() }
            val data = viewModel.state.collectAsState()
            val fieldsError: PredictException? =
                (data.value as? LoginViewModel.State.Error)?.exception

            val navigator = LocalNavigator.currentOrThrow
            val registerScreen = rememberScreen(OnboardingNavigation.Authorization.RegisterScreen)
            val splashScreen = rememberScreen(CoreNavigation.Splash)

            var email by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }

            val focusManager = LocalFocusManager.current

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(40.dp))

                    // Logo
                    Text(
                        text = "Predict",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = (-0.14).sp,
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Spacer(modifier = Modifier.height(52.dp))

                    // Title
                    Text(
                        text = "Login to account",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = (-0.14).sp,
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Form fields
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Email Field
                        PTextFieldEmail(
                            fieldName = "Email address",
                            error = fieldsError.takeIf { it is PredictException.InvalidEmail },
                            value = email,
                            keyboardOptions = PTextFieldEmailDefaults.keyboardOptions.copy(imeAction = ImeAction.Next),
                            onAction = { focusManager.moveFocus(FocusDirection.Next) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            email = it
                        }

                        // Password Field
                        PTextFieldPassword(
                            fieldName = "Password",
                            value = password,
                            error = fieldsError.takeIf { it is PredictException.WeakPassword },
                            onAction = { focusManager.clearFocus() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            password = it
                        }

                        // Forgot Password (right aligned)
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            TextButton(
                                onClick = { /* TODO: Handle forgot password */ }
                            ) {
                                Text(
                                    text = "Forgot password?",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Login Button
                    PButton(
                        text = "Login",
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        viewModel.login(email, password)
                        focusManager.clearFocus()
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Divider with "or"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Divider(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )

                        Text(
                            text = "or",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Divider(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Sign up text (clickable)
                    TextButton(
                        onClick = {
                            navigator.push(registerScreen)
                        },
                        modifier = Modifier.padding(bottom = 24.dp)
                    ) {
                        Text(
                            text = buildAnnotatedString {
                                withStyle(
                                    SpanStyle(
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontWeight = FontWeight.Normal
                                    )
                                ) {
                                    append("Don't have an account? ")
                                }
                                withStyle(
                                    SpanStyle(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                ) {
                                    append("Sign up")
                                }
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    // Loading and error states
                    when (val state = data.value) {
                        is LoginViewModel.State.Idle -> {
                            // Nothing to show
                        }

                        is LoginViewModel.State.Loading -> {
                            AdaptiveCircularProgressIndicator()
                        }

                        is LoginViewModel.State.Authenticated -> {
                            navigator.replace(splashScreen)
                        }

                        is LoginViewModel.State.Error -> {
                            PErrorText(state.exception)
                        }
                    }
                }
            }
        }
    }
}
