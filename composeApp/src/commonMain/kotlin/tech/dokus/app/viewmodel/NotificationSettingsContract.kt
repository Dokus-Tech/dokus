package tech.dokus.app.viewmodel

import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.domain.asbtractions.RetryHandler
import tech.dokus.domain.enums.NotificationType
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.model.NotificationPreferenceDto
import tech.dokus.foundation.app.state.DokusState

@Immutable
sealed interface NotificationSettingsState : MVIState, DokusState<Nothing> {
    data object Loading : NotificationSettingsState

    data class Content(
        val preferences: List<NotificationPreferenceDto> = emptyList(),
        val updatingTypes: Set<NotificationType> = emptySet(),
    ) : NotificationSettingsState

    data class Error(
        override val exception: DokusException,
        override val retryHandler: RetryHandler,
    ) : NotificationSettingsState, DokusState.Error<Nothing>
}

@Immutable
sealed interface NotificationSettingsIntent : MVIIntent {
    data object Load : NotificationSettingsIntent
    data object Refresh : NotificationSettingsIntent
    data class ToggleEmail(val type: NotificationType, val emailEnabled: Boolean) : NotificationSettingsIntent
}

@Immutable
sealed interface NotificationSettingsAction : MVIAction {
    data class ShowError(val error: DokusException) : NotificationSettingsAction
}
