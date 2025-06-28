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
import androidx.compose.runtime.rememberCoroutineScope
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
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(60.dp))

                    // Logo
                    Text(
                        text = "Predict",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.displaySmall
                    )

                    Spacer(modifier = Modifier.height(60.dp))

                    // Title
                    Text(
                        text = "Login to account",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Form fields
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
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
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Login Button
                    PButton("Login", modifier = Modifier.fillMaxWidth()) {
                        viewModel.login(email, password)
                        focusManager.clearFocus()
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Divider with "or"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Divider(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        Text(
                            text = "or",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Divider(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Sign up text (clickable)
                    TextButton(
                        onClick = {
                            navigator.push(registerScreen)
                        }
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

                    Spacer(modifier = Modifier.height(24.dp))

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
