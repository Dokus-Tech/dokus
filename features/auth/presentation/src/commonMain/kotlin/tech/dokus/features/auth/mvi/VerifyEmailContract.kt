package tech.dokus.features.auth.mvi

import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.domain.asbtractions.RetryHandler
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.foundation.app.state.DokusState

@Immutable
sealed interface VerifyEmailState : MVIState, DokusState<Unit> {
    data object Verifying : VerifyEmailState

    data object Success : VerifyEmailState

    data class Error(
        override val exception: DokusException,
        override val retryHandler: RetryHandler
    ) : VerifyEmailState, DokusState.Error<Unit>
}

@Immutable
sealed interface VerifyEmailIntent : MVIIntent {
    data object Verify : VerifyEmailIntent
}

@Immutable
sealed interface VerifyEmailAction : MVIAction {
    data object NavigateToLogin : VerifyEmailAction
}
