package tech.dokus.features.auth.mvi

import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.foundation.app.state.DokusState

@Immutable
data class VerifyEmailState(
    val verification: DokusState<Unit> = DokusState.loading(),
) : MVIState

@Immutable
sealed interface VerifyEmailIntent : MVIIntent {
    data object Verify : VerifyEmailIntent
}

@Immutable
sealed interface VerifyEmailAction : MVIAction {
    data object NavigateToLogin : VerifyEmailAction
}
