package ai.thepredict.app.onboarding.authentication.login

import ai.thepredict.app.navigation.HomeNavigation
import ai.thepredict.app.navigation.OnboardingNavigation
import ai.thepredict.domain.exceptions.PredictException
import ai.thepredict.ui.PButton
import ai.thepredict.ui.PErrorText
import ai.thepredict.ui.common.PTopAppBar
import ai.thepredict.ui.fields.PTextFieldEmail
import ai.thepredict.ui.fields.PTextFieldEmailDefaults
import ai.thepredict.ui.fields.PTextFieldPassword
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
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
import com.mohamedrejeb.calf.ui.progress.AdaptiveCircularProgressIndicator

internal class LoginScreen : Screen {

    @Composable
    override fun Content() {
        val viewModel = rememberScreenModel { LoginViewModel() }
        val data = viewModel.state.collectAsState()
        val fieldsError: PredictException? = (data.value as? LoginViewModel.State.Error)?.exception

        val navigator = LocalNavigator.currentOrThrow
        val registerScreen = rememberScreen(OnboardingNavigation.Authorization.RegisterScreen)
        val splashScreen = rememberScreen(HomeNavigation.SplashScreen)

        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }

        val focusManager = LocalFocusManager.current

        Scaffold(
            topBar = { PTopAppBar("Let's get started") }
        ) { innerPadding ->
            Column(
                modifier = Modifier.fillMaxWidth().padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
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
                    value = password,
                    error = fieldsError.takeIf { it is PredictException.WeakPassword },
                    onAction = { focusManager.clearFocus() }
                ) {
                    password = it
                }

                PButton("Login", modifier = Modifier.padding(vertical = 16.dp)) {
                    viewModel.login(email, password)
                    focusManager.clearFocus()
                }

                OutlinedButton(onClick = {
                    navigator.push(registerScreen)
                }) {
                    Text("New? Create an account")
                }

                Spacer(modifier = Modifier.weight(1f))

                when (val state = data.value) {
                    is LoginViewModel.State.Idle -> {

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