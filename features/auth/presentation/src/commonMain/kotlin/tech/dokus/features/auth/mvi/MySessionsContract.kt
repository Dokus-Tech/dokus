package tech.dokus.features.auth.mvi

import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.SessionId
import tech.dokus.domain.model.auth.SessionDto
import tech.dokus.foundation.app.state.DokusState

@Immutable
data class MySessionsState(
    val sessions: DokusState<List<SessionDto>> = DokusState.loading(),
    val isRevokingOthers: Boolean = false,
) : MVIState

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
    data class ShowError(val error: DokusException) : MySessionsAction
}
