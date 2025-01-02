package ai.thepredict.app.onboarding.authentication.register

import ai.thepredict.app.navigation.OnboardingNavigation
import ai.thepredict.app.onboarding.authentication.login.LoginViewModel
import ai.thepredict.domain.exceptions.PredictException
import ai.thepredict.ui.PButton
import ai.thepredict.ui.PErrorText
import ai.thepredict.ui.PTitle
import ai.thepredict.ui.PTopAppBar
import ai.thepredict.ui.fields.PTextFieldEmail
import ai.thepredict.ui.fields.PTextFieldEmailDefaults
import ai.thepredict.ui.fields.PTextFieldFreeDefaults
import ai.thepredict.ui.fields.PTextFieldName
import ai.thepredict.ui.fields.PTextFieldPassword
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.registry.rememberScreen
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import compose.icons.FeatherIcons
import compose.icons.feathericons.User

internal class RegisterScreen : Screen {

    @Composable
    override fun Content() {
        val viewModel = rememberScreenModel { RegisterViewModel() }
        val data = viewModel.state.collectAsState()
        val fieldsError: PredictException? = (data.value as? RegisterViewModel.State.Error)?.exception

        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var name by remember { mutableStateOf("") }

        val navigator = LocalNavigator.currentOrThrow
        val workspacesOverview = rememberScreen(OnboardingNavigation.Workspaces.All)

        val focusManager = LocalFocusManager.current

        Scaffold(
            topBar = { PTopAppBar("Welcome to The Predict") }
        ) { contentPadding ->
            Column(
                modifier = Modifier.fillMaxWidth().padding(contentPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                PTitle("Let's create an account")

                PTextFieldName(
                    modifier = Modifier.padding(vertical = 16.dp),
                    fieldName = "Name",
                    error = fieldsError.takeIf { it is PredictException.InvalidName },
                    value = name,
                    icon = FeatherIcons.User,
                    keyboardOptions = PTextFieldFreeDefaults.keyboardOptions.copy(imeAction = ImeAction.Next),
                    onAction = { focusManager.moveFocus(FocusDirection.Next) }
                ) {
                    name = it
                }

                Spacer(modifier = Modifier.padding(vertical = 16.dp))

                PTextFieldEmail(
                    modifier = Modifier.padding(vertical = 16.dp),
                    fieldName = "Email",
                    error = fieldsError.takeIf { it is PredictException.InvalidEmail },
                    value = email,
                    keyboardOptions = PTextFieldEmailDefaults.keyboardOptions.copy(imeAction = ImeAction.Next),
                    onAction = { focusManager.moveFocus(FocusDirection.Next) },
                ) {
                    email = it
                }

                PTextFieldPassword(
                    fieldName = "Password",
                    error = fieldsError.takeIf { it is PredictException.WeakPassword },
                    value = password,
                    onAction = { focusManager.clearFocus() }
                ) {
                    password = it
                }

                Spacer(modifier = Modifier.padding(vertical = 16.dp))

                PButton("Register", modifier = Modifier.padding(vertical = 32.dp)) {
                    viewModel.createUser(email, password, name)
                }

                Spacer(modifier = Modifier.weight(1f))

                when (val state = data.value) {
                    is RegisterViewModel.State.Loading -> {

                    }

                    is RegisterViewModel.State.Loaded -> {
                        navigator.replace(workspacesOverview)
                    }

                    is RegisterViewModel.State.Error -> {
                        PErrorText(state.exception)
                    }
                }
            }
        }
    }
}