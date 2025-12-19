package ai.dokus.app.auth.screen

import ai.dokus.app.auth.viewmodel.NewPasswordViewModel
import tech.dokus.foundation.app.state.exceptionIfError
import ai.dokus.foundation.design.components.background.EnhancedFloatingBubbles
import ai.dokus.foundation.design.components.background.SpotlightEffect
import ai.dokus.foundation.design.components.layout.TwoPaneContainer
import ai.dokus.foundation.domain.Password
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
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
internal fun NewPasswordScreen(
    viewModel: NewPasswordViewModel = koinViewModel()
) {
    Scaffold { contentPadding ->
        TwoPaneContainer(
            middleEffect = {
                EnhancedFloatingBubbles()
                SpotlightEffect()
            },
            left = { NewPasswordContent(viewModel, contentPadding) },
            right = { SloganScreen() }
        )
    }
}

@Composable
private fun NewPasswordContent(
    viewModel: NewPasswordViewModel,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val state by viewModel.state.collectAsState()
    val fieldsError = state.exceptionIfError()

    val focusManager = LocalFocusManager.current

    var password by remember { mutableStateOf(Password("")) }
    var passwordConfirmation by remember { mutableStateOf(Password("")) }
    val mutableInteractionSource = remember { MutableInteractionSource() }

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
        // TODO: Add new password fields and actions here
    }
}