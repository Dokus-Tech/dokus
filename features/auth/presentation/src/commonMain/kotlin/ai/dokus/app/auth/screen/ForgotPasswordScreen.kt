package ai.dokus.app.auth.screen

import ai.dokus.app.auth.viewmodel.ForgotPasswordViewModel
import ai.dokus.app.core.state.exceptionIfError
import ai.dokus.foundation.domain.Email
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun ForgotPasswordScreen(
    viewModel: ForgotPasswordViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val fieldsError = state.exceptionIfError()

    val focusManager = LocalFocusManager.current

    var email by remember { mutableStateOf(Email("")) }
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