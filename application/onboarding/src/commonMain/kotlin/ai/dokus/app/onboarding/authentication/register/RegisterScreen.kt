package ai.dokus.app.onboarding.authentication.register

import ai.dokus.foundation.ui.constrains.isLargeScreen
import ai.dokus.foundation.navigation.AppNavigator
import ai.dokus.foundation.domain.Email
import ai.dokus.foundation.domain.Name
import ai.dokus.foundation.domain.Password
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.ui.PPrimaryButton
import ai.dokus.foundation.ui.brandsugar.BackgroundAnimationViewModel
import ai.dokus.foundation.ui.brandsugar.SloganWithBackgroundWithLeftContent
import ai.dokus.foundation.ui.fields.PTextFieldEmail
import ai.dokus.foundation.ui.fields.PTextFieldEmailDefaults
import ai.dokus.foundation.ui.fields.PTextFieldName
import ai.dokus.foundation.ui.fields.PTextFieldPassword
import ai.dokus.foundation.ui.text.AppNameText
import ai.dokus.foundation.ui.text.SectionTitle
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
fun RegisterScreen(navigator: AppNavigator) {
    val backgroundAnimationViewModel = koinInject<BackgroundAnimationViewModel>()
    val viewModel = remember { RegisterViewModel() }

    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    val handleEffect = { effect: RegisterViewModel.Effect ->
        when (effect) {
            is RegisterViewModel.Effect.NavigateToRegistrationConfirmation -> navigator.navigateToRegisterConfirmation()
        }
    }

    LaunchedEffect("register-screen") {
        scope.launch { viewModel.effect.collect(handleEffect) }
    }

    val data = viewModel.state.collectAsState()
    val fieldsError: DokusException? =
        (data.value as? RegisterViewModel.State.Error)?.exception

    var firstName by remember { mutableStateOf(Name("")) }
    var lastName by remember { mutableStateOf(Name("")) }
    var email by remember { mutableStateOf(Email("")) }
    var password by remember { mutableStateOf(Password("")) }
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
                    RegisterForm(
                        focusManager = focusManager,
                        email = email,
                        onEmailChange = { email = it },
                        password = password,
                        onPasswordChange = { password = it },
                        firstName = firstName,
                        onFirstNameChange = { firstName = it },
                        lastName = lastName,
                        onLastNameChange = { lastName = it },
                        fieldsError = fieldsError,
                        onLoginClick = { navigator.navigateBack() },
                        onRegisterClick = {
                            viewModel.createUser(
                                newEmail = email,
                                newPassword = password,
                                firstName = firstName,
                                lastName = lastName
                            )
                        },
                    )
                }
            } else {
                RegisterScreenMobileContent(
                    focusManager = focusManager,
                    email = email,
                    onEmailChange = { email = it },
                    password = password,
                    onPasswordChange = { password = it },
                    firstName = firstName,
                    onFirstNameChange = { firstName = it },
                    fieldsError = fieldsError,
                    onLoginClick = { navigator.navigateBack() },
                    lastName = lastName,
                    onLastNameChange = { lastName = it },
                    onRegisterClick = {
                        viewModel.createUser(
                            newEmail = email,
                            newPassword = password,
                            firstName = firstName,
                            lastName = lastName,
                        )
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
internal fun RegisterScreenMobileContent(
    focusManager: FocusManager,
    email: Email,
    onEmailChange: (Email) -> Unit,
    password: Password,
    onPasswordChange: (Password) -> Unit,
    firstName: Name,
    onFirstNameChange: (Name) -> Unit,
    lastName: Name,
    onLastNameChange: (Name) -> Unit,
    fieldsError: DokusException?,
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        AppNameText()

        RegisterForm(
            focusManager = focusManager,
            email = email,
            onEmailChange = onEmailChange,
            password = password,
            onPasswordChange = onPasswordChange,
            fieldsError = fieldsError,
            onLoginClick = onLoginClick,
            onRegisterClick = onRegisterClick,
            firstName = firstName,
            onFirstNameChange = onFirstNameChange,
            lastName = lastName,
            onLastNameChange = onLastNameChange,
            modifier = Modifier.fillMaxSize()
        )

        Spacer(Modifier.weight(1f))
    }
}

@Composable
internal fun RegisterForm(
    focusManager: FocusManager,
    email: Email,
    onEmailChange: (Email) -> Unit,
    password: Password,
    onPasswordChange: (Password) -> Unit,
    firstName: Name,
    onFirstNameChange: (Name) -> Unit,
    lastName: Name,
    onLastNameChange: (Name) -> Unit,
    fieldsError: DokusException?,
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center,
    ) {
        SectionTitle("Register")

        Spacer(modifier = Modifier.height(24.dp))

        // Form fields
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PTextFieldName(
                fieldName = "First name",
                error = fieldsError.takeIf { it is DokusException.InvalidFirstName },
                value = firstName,
                keyboardOptions = PTextFieldEmailDefaults.keyboardOptions.copy(imeAction = ImeAction.Next),
                onAction = { focusManager.moveFocus(FocusDirection.Next) },
                modifier = Modifier.fillMaxWidth(),
                onValueChange = onFirstNameChange
            )

            PTextFieldName(
                fieldName = "Last name",
                error = fieldsError.takeIf { it is DokusException.InvalidFirstName },
                value = lastName,
                keyboardOptions = PTextFieldEmailDefaults.keyboardOptions.copy(imeAction = ImeAction.Next),
                onAction = { focusManager.moveFocus(FocusDirection.Next) },
                modifier = Modifier.fillMaxWidth(),
                onValueChange = onLastNameChange
            )

            PTextFieldEmail(
                fieldName = "Email address",
                error = fieldsError.takeIf { it is DokusException.InvalidEmail },
                value = email,
                keyboardOptions = PTextFieldEmailDefaults.keyboardOptions.copy(imeAction = ImeAction.Next),
                onAction = { focusManager.moveFocus(FocusDirection.Next) },
                modifier = Modifier.fillMaxWidth(),
                onValueChange = onEmailChange
            )

            PTextFieldPassword(
                fieldName = "Password",
                value = password,
                error = fieldsError.takeIf { it is DokusException.WeakPassword },
                onAction = { focusManager.clearFocus() },
                modifier = Modifier.fillMaxWidth(),
                onValueChange = onPasswordChange
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        PPrimaryButton(
            text = "Register",
            modifier = Modifier.fillMaxWidth(),
            onClick = onRegisterClick
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Sign up text - moved up here to match Figma
        TextButton(
            modifier = Modifier.align(Alignment.Start),
            onClick = onLoginClick
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
                        append("Already have an account? ")
                    }
                    withStyle(
                        SpanStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        append("Login")
                    }
                },
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}