package ai.thepredict.app.onboarding.authentication.restore

import ai.thepredict.app.core.constrains.isLargeScreen
import ai.thepredict.app.core.di
import ai.thepredict.domain.exceptions.PredictException
import ai.thepredict.ui.PPrimaryButton
import ai.thepredict.ui.brandsugar.BackgroundAnimationViewModel
import ai.thepredict.ui.brandsugar.SloganWithBackgroundWithLeftContent
import ai.thepredict.ui.fields.PTextFieldPassword
import ai.thepredict.ui.text.AppNameText
import ai.thepredict.ui.text.SectionTitle
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
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.kodein.di.instance

internal class NewPasswordScreen : Screen {
    @Composable
    override fun Content() {
        val backgroundAnimationViewModel by di.instance<BackgroundAnimationViewModel>()
        val viewModel = rememberScreenModel { NewPasswordViewModel() }

        val data = viewModel.state.collectAsState()
        val fieldsError: PredictException? =
            (data.value as? NewPasswordViewModel.State.Error)?.exception

        val navigator = LocalNavigator.currentOrThrow
        val focusManager = LocalFocusManager.current

        var password by remember { mutableStateOf("") }
        var passwordConfirmation by remember { mutableStateOf("") }
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
                        NewPasswordForm(
                            focusManager = focusManager,
                            password = password,
                            onPasswordChange = { password = it },
                            passwordConfirmation = passwordConfirmation,
                            onPasswordConfirmationChange = { passwordConfirmation = it },
                            fieldsError = fieldsError,
                            onContinueClick = { viewModel.submit(password, passwordConfirmation) }
                        )
                    }
                } else {
                    NewPasswordScreenMobileContent(
                        focusManager = focusManager,
                        password = password,
                        onPasswordChange = { password = it },
                        passwordConfirmation = passwordConfirmation,
                        onPasswordConfirmationChange = { passwordConfirmation = it },
                        fieldsError = fieldsError,
                        onContinueClick = { viewModel.submit(password, passwordConfirmation) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun NewPasswordScreenMobileContent(
    focusManager: FocusManager,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordConfirmation: String,
    onPasswordConfirmationChange: (String) -> Unit,
    onContinueClick: () -> Unit,
    fieldsError: PredictException?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        AppNameText()

        NewPasswordForm(
            focusManager = focusManager,
            password = password,
            onPasswordChange = onPasswordChange,
            passwordConfirmation = passwordConfirmation,
            onPasswordConfirmationChange = onPasswordConfirmationChange,
            onContinueClick = onContinueClick,
            fieldsError = fieldsError,
            modifier = Modifier.fillMaxSize()
        )

        Spacer(Modifier.weight(1f))
    }
}

@Composable
internal fun NewPasswordForm(
    focusManager: FocusManager,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordConfirmation: String,
    onPasswordConfirmationChange: (String) -> Unit,
    onContinueClick: () -> Unit,
    fieldsError: PredictException?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center,
    ) {
        SectionTitle("New password")

        Spacer(modifier = Modifier.height(24.dp))

        // Form fields
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            PTextFieldPassword(
                fieldName = "Password",
                value = password,
                error = fieldsError.takeIf { it is PredictException.WeakPassword },
                onAction = { focusManager.clearFocus() },
                modifier = Modifier.fillMaxWidth(),
                onValueChange = onPasswordChange
            )

            PTextFieldPassword(
                fieldName = "Confirm Password",
                value = passwordConfirmation,
                error = fieldsError.takeIf { it is PredictException.PasswordDoNotMatch },
                onAction = { focusManager.clearFocus() },
                modifier = Modifier.fillMaxWidth(),
                onValueChange = onPasswordConfirmationChange
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        PPrimaryButton(
            text = "Continue",
            modifier = Modifier.fillMaxWidth(),
            onClick = onContinueClick
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}