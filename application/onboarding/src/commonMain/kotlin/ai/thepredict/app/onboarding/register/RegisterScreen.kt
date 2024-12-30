package ai.thepredict.app.onboarding.register

import ai.thepredict.ui.PButton
import ai.thepredict.ui.PTitle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import compose.icons.FeatherIcons
import compose.icons.feathericons.AtSign
import compose.icons.feathericons.Key
import compose.icons.feathericons.User

internal class RegisterScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = rememberScreenModel { RegisterViewModel() }
        val data = viewModel.state.collectAsState()

        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var name by remember { mutableStateOf("") }

        val navigator = LocalNavigator.currentOrThrow
        val focusManager = LocalFocusManager.current

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Register") },
                    navigationIcon = {
                        IconButton(onClick = {
                            navigator.pop()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { contentPadding ->
            Column(
                modifier = Modifier.fillMaxWidth().padding(contentPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                PTitle("Let's create an account")

                OutlinedTextField(
                    modifier = Modifier.padding(16.dp),
                    value = name,
                    onValueChange = {
                        name = it
                    },
                    label = {
                        Text("Name")
                    },
                    leadingIcon = {
                        Icon(FeatherIcons.User, "name")
                    },
                    singleLine = true,
                    keyboardActions = KeyboardActions(
                        onNext = {
                            focusManager.moveFocus(FocusDirection.Next)
                        }
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                )

                OutlinedTextField(
                    modifier = Modifier.padding(16.dp),
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
                            focusManager.moveFocus(FocusDirection.Next)
                        }
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                )

                OutlinedTextField(
                    modifier = Modifier.padding(16.dp),
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

                PButton("Register", modifier = Modifier.padding(16.dp)) {
                    viewModel.createUser(email, password, name)
                }

                Spacer(modifier = Modifier.fillMaxWidth())

                when (val state = data.value) {
                    is RegisterViewModel.State.Loading -> {

                    }

                    is RegisterViewModel.State.Loaded -> {
                        PTitle(state.user.id.toString())
                    }

                    is RegisterViewModel.State.Error -> {
                        Text(state.throwable.toString())
                    }
                }
            }
        }
    }
}