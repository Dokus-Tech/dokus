package tech.dokus.features.auth.mvi

import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.domain.asbtractions.RetryHandler
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.SessionId
import tech.dokus.domain.model.auth.SessionDto
import tech.dokus.foundation.app.state.DokusState

@Immutable
sealed interface MySessionsState : MVIState, DokusState<List<SessionDto>> {
    data object Loading : MySessionsState

    data class Loaded(
        val sessions: List<SessionDto>,
        val isRevokingOthers: Boolean = false
    ) : MySessionsState

    data class Error(
        override val exception: DokusException,
        override val retryHandler: RetryHandler
    ) : MySessionsState, DokusState.Error<List<SessionDto>>
}

@Immutable
sealed interface MySessionsIntent : MVIIntent {
    data object Load : MySessionsIntent
    data class RevokeSession(val sessionId: SessionId) : MySessionsIntent
    data object RevokeOthers : MySessionsIntent
    data object BackClicked : MySessionsIntent
}

@Immutable
sealed interface MySessionsAction : MVIAction {
    data object NavigateBack : MySessionsAction
    data object ShowSessionRevoked : MySessionsAction
    data object ShowRevokeOthersSuccess : MySessionsAction
}
