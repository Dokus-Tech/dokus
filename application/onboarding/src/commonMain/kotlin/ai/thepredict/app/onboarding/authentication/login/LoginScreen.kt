package ai.thepredict.app.onboarding.authentication.login

import ai.thepredict.app.navigation.HomeNavigation
import ai.thepredict.app.navigation.OnboardingNavigation
import ai.thepredict.ui.PButton
import ai.thepredict.ui.PTopAppBar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.registry.rememberScreen
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mohamedrejeb.calf.ui.progress.AdaptiveCircularProgressIndicator
import compose.icons.FeatherIcons
import compose.icons.feathericons.AtSign
import compose.icons.feathericons.Key

internal class LoginScreen : Screen {

    @Composable
    override fun Content() {
        val viewModel = rememberScreenModel { LoginViewModel() }
        val data = viewModel.state.collectAsState()

        val navigator = LocalNavigator.currentOrThrow
        val registerScreen = rememberScreen(OnboardingNavigation.Authorization.RegisterScreen)
        val splashScreen = rememberScreen(HomeNavigation.SplashScreen)

        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }

        var passwordFocusWasRequested by remember { mutableStateOf(false) }
        val focusManager = LocalFocusManager.current
        val passwordFocusRequester = FocusRequester()

        Scaffold(
            topBar = { PTopAppBar("Login") }
        ) { innerPadding ->
            Column(
                modifier = Modifier.fillMaxWidth().padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                OutlinedTextField(
                    modifier = Modifier.padding(vertical = 16.dp),
                    value = email,
                    onValueChange = {
                        email = it
                    },
                    label = {
                        Text("Email")
                    },
                    leadingIcon = {
                        Icon(FeatherIcons.AtSign, "email")
                    },
                    singleLine = true,
                    keyboardActions = KeyboardActions(
                        onNext = {
                            passwordFocusWasRequested = true
                            passwordFocusRequester.requestFocus()
                        },
                        onDone = {
                            passwordFocusRequester.requestFocus()
                        }
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = if (passwordFocusWasRequested) ImeAction.Done else ImeAction.Next
                    ),
                )

                OutlinedTextField(
                    modifier = Modifier
                        .focusRequester(passwordFocusRequester)
                        .padding(vertical = 16.dp),
                    value = password,
                    onValueChange = {
                        password = it
                    },
                    label = {
                        Text("Password")
                    },
                    leadingIcon = {
                        Icon(FeatherIcons.Key, "password")
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                        }, onNext = {
                            focusManager.clearFocus()
                        }
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                )

                PButton("Login", modifier = Modifier.padding(vertical = 16.dp)) {
                    viewModel.login(email, password)
                }

                OutlinedButton(onClick = {
                    navigator.push(registerScreen)
                }) {
                    Text("New? Create an account")
                }

                Spacer(modifier = Modifier.fillMaxWidth())

                when (val state = data.value) {
                    is LoginViewModel.State.Loading -> {
                        AdaptiveCircularProgressIndicator()
                    }

                    is LoginViewModel.State.Authenticated -> {
                        navigator.replace(splashScreen)
                    }

                    is LoginViewModel.State.Error -> {
                        Text(state.exception.message)
                    }
                }
            }
        }
    }
}