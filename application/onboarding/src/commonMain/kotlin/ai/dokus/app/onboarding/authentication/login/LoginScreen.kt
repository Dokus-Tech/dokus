package ai.dokus.app.onboarding.authentication.login

import ai.dokus.app.core.constrains.isLargeScreen
import ai.dokus.app.core.flags.FeatureFlags
import ai.dokus.app.navigation.AppNavigator
import ai.dokus.foundation.domain.exceptions.PredictException
import ai.dokus.foundation.ui.PPrimaryButton
import ai.dokus.foundation.ui.brandsugar.BackgroundAnimationViewModel
import ai.dokus.foundation.ui.brandsugar.SloganWithBackgroundWithLeftContent
import ai.dokus.foundation.ui.fields.PTextFieldEmail
import ai.dokus.foundation.ui.fields.PTextFieldEmailDefaults
import ai.dokus.foundation.ui.fields.PTextFieldPassword
import ai.dokus.foundation.ui.text.AppNameText
import ai.dokus.foundation.ui.text.SectionTitle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun LoginScreen(navigator: AppNavigator) {
    val backgroundAnimationViewModel = koinInject<BackgroundAnimationViewModel>()
    val viewModel = remember { LoginViewModel() }

    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    val handleEffect = { effect: LoginViewModel.Effect ->
        when (effect) {
            is LoginViewModel.Effect.NavigateToWorkspaces -> navigator.navigateToWorkspacesList()
        }
    }

    LaunchedEffect("login-screen") {
        scope.launch { viewModel.effect.collect(handleEffect) }
    }

    val data = viewModel.state.collectAsState()
    val fieldsError: PredictException? =
        (data.value as? LoginViewModel.State.Error)?.exception

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val mutableInteractionSource = remember { MutableInteractionSource() }

    Scaffold { contentPadding ->
        Box(
            Modifier
                .padding(contentPadding)
                .clickable(
                    indication = null,
                    interactionSource = mutableInteractionSource
                ) {
                    focusManager.clearFocus()
                }
        ) {
            if (isLargeScreen) {
                SloganWithBackgroundWithLeftContent(backgroundAnimationViewModel) {
                    LoginForm(
                        focusManager = focusManager,
                        email = email,
                        onEmailChange = { email = it },
                        password = password,
                        onPasswordChange = { password = it },
                        fieldsError = fieldsError,
                        onLoginClick = { viewModel.login(email, password) },
                        onRegisterClick = { navigator.navigateToRegister() },
                        onForgetPasswordClick = { navigator.navigateToForgotPassword() },
                        onConnectToServerClick = { navigator.navigateToServerConnection() },
                        modifier = Modifier
                    )
                }
            } else {
                LoginScreenMobileContent(
                    focusManager = focusManager,
                    email = email,
                    onEmailChange = { email = it },
                    password = password,
                    onPasswordChange = { password = it },
                    fieldsError = fieldsError,
                    onLoginClick = { viewModel.login(email, password) },
                    onRegisterClick = { navigator.navigateToRegister() },
                    onForgetPasswordClick = { navigator.navigateToForgotPassword() },
                    onConnectToServerClick = { navigator.navigateToServerConnection() },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
internal fun LoginScreenMobileContent(
    focusManager: FocusManager,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    fieldsError: PredictException?,
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onForgetPasswordClick: () -> Unit,
    onConnectToServerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        AppNameText()

        LoginForm(
            focusManager = focusManager,
            email = email,
            onEmailChange = onEmailChange,
            password = password,
            onPasswordChange = onPasswordChange,
            fieldsError = fieldsError,
            onLoginClick = onLoginClick,
            onRegisterClick = onRegisterClick,
            onForgetPasswordClick = onForgetPasswordClick,
            onConnectToServerClick = onConnectToServerClick,
            modifier = Modifier.fillMaxSize()
        )

        Spacer(Modifier.weight(1f))
    }
}

@Composable
internal fun LoginForm(
    focusManager: FocusManager,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    fieldsError: PredictException?,
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    onForgetPasswordClick: () -> Unit,
    onConnectToServerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start, // Center content horizontally
        verticalArrangement = Arrangement.Center // Center content vertically
    ) {
        SectionTitle("Login to account")

        Spacer(modifier = Modifier.height(24.dp))

        // Form fields
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Email Field - Using OutlinedTextField with light background
            PTextFieldEmail(
                fieldName = "Email address",
                error = fieldsError.takeIf { it is PredictException.InvalidEmail },
                value = email,
                keyboardOptions = PTextFieldEmailDefaults.keyboardOptions.copy(imeAction = ImeAction.Next),
                onAction = { focusManager.moveFocus(FocusDirection.Next) },
                modifier = Modifier.fillMaxWidth(), onValueChange = onEmailChange
            )

            // Password Field - Using OutlinedTextField with light background
            PTextFieldPassword(
                fieldName = "Password",
                value = password,
                error = fieldsError.takeIf { it is PredictException.WeakPassword },
                onAction = { focusManager.clearFocus() },
                modifier = Modifier.fillMaxWidth(), onValueChange = onPasswordChange
            )
        }
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterStart
        ) {
            TextButton(onClick = onForgetPasswordClick) {
                Text(
                    text = "Forgot password?",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        PPrimaryButton(
            text = "Login",
            modifier = Modifier.fillMaxWidth(),
            onClick = onLoginClick
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Sign up text - moved up here to match Figma
        TextButton(
            modifier = Modifier.align(Alignment.Start),
            onClick = onRegisterClick
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

        // Divider with "or"
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HorizontalDivider(
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

            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Connect to server button - simple outlined button
        OutlinedButton(
            onClick = onConnectToServerClick,
            enabled = FeatureFlags.ownServers,
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "Connect to server",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}