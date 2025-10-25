package ai.dokus.app.auth.screen

import ai.dokus.app.auth.viewmodel.LoginViewModel
import ai.dokus.foundation.domain.Email
import ai.dokus.foundation.domain.Password
import ai.dokus.foundation.domain.exceptions.DokusException
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager

@Composable
fun LoginScreen() {
    val viewModel = remember { LoginViewModel() }

    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    LaunchedEffect("login-screen") {
//        scope.launch { viewModel.effect.collect(handleEffect) }
    }

    val data = viewModel.state.collectAsState()
    val fieldsError: DokusException? =
        (data.value as? LoginViewModel.State.Error)?.exception

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

        }
    }
}