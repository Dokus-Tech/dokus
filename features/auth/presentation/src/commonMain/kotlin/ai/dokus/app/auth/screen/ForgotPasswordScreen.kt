package ai.dokus.app.auth.screen

import ai.dokus.app.auth.viewmodel.ForgotPasswordAction
import ai.dokus.app.auth.viewmodel.ForgotPasswordContainer
import ai.dokus.app.auth.viewmodel.ForgotPasswordIntent
import ai.dokus.app.auth.viewmodel.ForgotPasswordState
import ai.dokus.foundation.navigation.local.LocalNavController
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
import pro.respawn.flowmvi.api.IntentReceiver
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.foundation.app.mvi.container
import tech.dokus.foundation.app.state.exceptionIfError

@Composable
internal fun ForgotPasswordScreen(
    container: ForgotPasswordContainer = container()
) {
    val navController = LocalNavController.current

    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            ForgotPasswordAction.NavigateBack -> navController.navigateUp()
        }
    }

    val focusManager = LocalFocusManager.current
    val mutableInteractionSource = remember { MutableInteractionSource() }

    Scaffold { contentPadding ->
        with(container.store) {
            ForgotPasswordContent(
                state = state,
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
}

@Composable
private fun IntentReceiver<ForgotPasswordIntent>.ForgotPasswordContent(
    state: ForgotPasswordState,
    modifier: Modifier = Modifier
) {
    val fieldsError = state.exceptionIfError()

    Box(modifier) {
        // TODO: Add forgot password fields and actions here
        // Use intent(ForgotPasswordIntent.UpdateEmail(email)) for email updates
        // Use intent(ForgotPasswordIntent.SubmitClicked) for submit action
    }
}
