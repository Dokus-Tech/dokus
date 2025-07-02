package ai.thepredict.app.onboarding.authentication.restore

import ai.thepredict.app.core.constrains.isLargeScreen
import ai.thepredict.app.core.di
import ai.thepredict.app.navigation.OnboardingNavigation
import ai.thepredict.domain.exceptions.PredictException
import ai.thepredict.ui.PBackButton
import ai.thepredict.ui.PPrimaryButton
import ai.thepredict.ui.brandsugar.BackgroundAnimationViewModel
import ai.thepredict.ui.brandsugar.SloganWithBackgroundWithLeftContent
import ai.thepredict.ui.fields.PTextFieldEmail
import ai.thepredict.ui.fields.PTextFieldEmailDefaults
import ai.thepredict.ui.text.AppNameText
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.registry.rememberScreen
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.kodein.di.instance

internal class ForgotPasswordScreen : Screen {
    @Composable
    override fun Content() {
        val backgroundAnimationViewModel by di.instance<BackgroundAnimationViewModel>()
        val viewModel = rememberScreenModel { ForgotPasswordViewModel() }

        val data = viewModel.state.collectAsState()
        val fieldsError: PredictException? =
            (data.value as? ForgotPasswordViewModel.State.Error)?.exception

        val navigator = LocalNavigator.currentOrThrow
        val focusManager = LocalFocusManager.current

        val loginScreen = rememberScreen(OnboardingNavigation.Authorization.LoginScreen)

        var email by remember { mutableStateOf("") }
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
                        ForgotPasswordForm(
                            focusManager = focusManager,
                            email = email,
                            onEmailChange = { email = it },
                            fieldsError = fieldsError,
                            onSubmit = { viewModel.submit(email) },
                            onBackPress = { navigator.replace(loginScreen) },
                        )
                    }
                } else {
                    RegisterScreenMobileContent(
                        focusManager = focusManager,
                        email = email,
                        onEmailChange = { email = it },
                        fieldsError = fieldsError,
                        onSubmit = { viewModel.submit(email) },
                        onBackPress = { navigator.replace(loginScreen) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun RegisterScreenMobileContent(
    focusManager: FocusManager,
    email: String,
    onEmailChange: (String) -> Unit,
    fieldsError: PredictException?,
    onSubmit: () -> Unit,
    onBackPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        AppNameText()

        ForgotPasswordForm(
            focusManager = focusManager,
            email = email,
            onEmailChange = onEmailChange,
            fieldsError = fieldsError,
            onSubmit = onSubmit,
            onBackPress = onBackPress,
            modifier = Modifier.fillMaxSize()
        )

        Spacer(Modifier.weight(1f))
    }
}

@Composable
internal fun ForgotPasswordForm(
    focusManager: FocusManager,
    email: String,
    onEmailChange: (String) -> Unit,
    fieldsError: PredictException?,
    onSubmit: () -> Unit,
    onBackPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center,
    ) {
        // Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            PBackButton(modifier = Modifier.padding(end = 12.dp), onBackPress = onBackPress)
            Text(
                text = "Forgot password",
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.headlineSmall
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Insert your email address and we will send you a link to reset the password",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Form fields
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            PTextFieldEmail(
                fieldName = "Email address",
                error = fieldsError.takeIf { it is PredictException.InvalidEmail },
                value = email,
                keyboardOptions = PTextFieldEmailDefaults.keyboardOptions.copy(imeAction = ImeAction.Next),
                onAction = { onSubmit() },
                modifier = Modifier.fillMaxWidth(),
                onValueChange = onEmailChange
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        PPrimaryButton(
            text = "Continue",
            modifier = Modifier.fillMaxWidth(),
            onClick = onSubmit
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}