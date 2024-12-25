package ai.thepredict.app.onboarding.login

import ai.thepredict.ui.PButton
import ai.thepredict.ui.Title
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen

internal class LoginScreen : Screen {

    @Composable
    override fun Content() {
        val viewModel = rememberScreenModel { LoginViewModel() }
        val data = viewModel.state.collectAsState()

        LaunchedEffect("login") {
            viewModel.fetch()
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            Title("LoginScreen")

            PButton("Refresh") {
                viewModel.fetch()
            }

            when (val state = data.value) {
                is LoginViewModel.State.Loading -> {
                    Title("Loading")
                }

                is LoginViewModel.State.Loaded -> {
                    state.contacts.forEach {
                        Title(it.name)
                    }
                }

                is LoginViewModel.State.Error -> {
                    Title("Error happened")
                }
            }
        }
    }
}