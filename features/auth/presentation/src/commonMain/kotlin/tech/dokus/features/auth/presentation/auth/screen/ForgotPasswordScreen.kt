package tech.dokus.features.auth.presentation.auth.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import tech.dokus.features.auth.mvi.ForgotPasswordIntent
import tech.dokus.features.auth.mvi.ForgotPasswordState
import tech.dokus.foundation.app.state.exceptionIfError

@Composable
internal fun ForgotPasswordScreen(
    state: ForgotPasswordState,
    onIntent: (ForgotPasswordIntent) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val mutableInteractionSource = remember { MutableInteractionSource() }

    Scaffold { contentPadding ->
        ForgotPasswordContent(
            state = state,
            onIntent = onIntent,
            modifier = Modifier
                .padding(contentPadding)
                .clickable(
                    indication = null,
                    interactionSource = mutableInteractionSource
                ) {
                    focusManager.clearFocus()
                }
        )
    }
}

@Suppress("UnusedParameter", "UnusedPrivateProperty") // TODO: Implement password reset form
@Composable
private fun ForgotPasswordContent(
    state: ForgotPasswordState,
    onIntent: (ForgotPasswordIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    @Suppress("UNUSED_VARIABLE")
    val fieldsError = state.exceptionIfError()

    Box(modifier) {
        // TODO: Add forgot password fields and actions here
        // Use onIntent(ForgotPasswordIntent.UpdateEmail(email)) for email updates
        // Use onIntent(ForgotPasswordIntent.SubmitClicked) for submit action
    }
}
