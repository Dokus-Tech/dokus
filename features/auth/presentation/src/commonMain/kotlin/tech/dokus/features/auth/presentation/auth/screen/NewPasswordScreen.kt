package tech.dokus.features.auth.presentation.auth.screen

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
import tech.dokus.features.auth.mvi.NewPasswordIntent
import tech.dokus.features.auth.mvi.NewPasswordState
import tech.dokus.foundation.app.state.exceptionIfError
import tech.dokus.foundation.aura.components.background.EnhancedFloatingBubbles
import tech.dokus.foundation.aura.components.background.SpotlightEffect
import tech.dokus.foundation.aura.components.layout.TwoPaneContainer

@Composable
internal fun NewPasswordScreen(
    state: NewPasswordState,
    onIntent: (NewPasswordIntent) -> Unit,
) {
    Scaffold { contentPadding ->
        TwoPaneContainer(
            middleEffect = {
                EnhancedFloatingBubbles()
                SpotlightEffect()
            },
            left = {
                NewPasswordContent(
                    state = state,
                    onIntent = onIntent,
                    contentPadding = contentPadding
                )
            },
            right = { SloganScreen() }
        )
    }
}

@Suppress("UnusedParameter", "UnusedPrivateProperty") // TODO: Wire up form submission
@Composable
private fun NewPasswordContent(
    state: NewPasswordState,
    onIntent: (NewPasswordIntent) -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
    @Suppress("UNUSED_VARIABLE")
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
        // Use onIntent(NewPasswordIntent.UpdatePassword(password)) for password updates
        // Use onIntent(NewPasswordIntent.UpdatePasswordConfirmation(password)) for confirmation updates
        // Use onIntent(NewPasswordIntent.SubmitClicked) for submit action
    }
}
