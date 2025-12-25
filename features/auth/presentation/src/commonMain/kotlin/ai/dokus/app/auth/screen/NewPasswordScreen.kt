package ai.dokus.app.auth.screen

import ai.dokus.app.auth.viewmodel.NewPasswordAction
import ai.dokus.app.auth.viewmodel.NewPasswordContainer
import ai.dokus.app.auth.viewmodel.NewPasswordIntent
import ai.dokus.app.auth.viewmodel.NewPasswordState
import ai.dokus.foundation.design.components.background.EnhancedFloatingBubbles
import ai.dokus.foundation.design.components.background.SpotlightEffect
import ai.dokus.foundation.design.components.layout.TwoPaneContainer
import ai.dokus.foundation.navigation.local.LocalNavController
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
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
internal fun NewPasswordScreen(
    container: NewPasswordContainer = container()
) {
    val navController = LocalNavController.current

    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            NewPasswordAction.NavigateBack -> navController.navigateUp()
        }
    }

    Scaffold { contentPadding ->
        TwoPaneContainer(
            middleEffect = {
                EnhancedFloatingBubbles()
                SpotlightEffect()
            },
            left = {
                with(container.store) {
                    NewPasswordContent(state, contentPadding)
                }
            },
            right = { SloganScreen() }
        )
    }
}

@Composable
private fun IntentReceiver<NewPasswordIntent>.NewPasswordContent(
    state: NewPasswordState,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val fieldsError = state.exceptionIfError()

    val focusManager = LocalFocusManager.current
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
        // Use intent(NewPasswordIntent.UpdatePassword(password)) for password updates
        // Use intent(NewPasswordIntent.UpdatePasswordConfirmation(password)) for confirmation updates
        // Use intent(NewPasswordIntent.SubmitClicked) for submit action
    }
}
