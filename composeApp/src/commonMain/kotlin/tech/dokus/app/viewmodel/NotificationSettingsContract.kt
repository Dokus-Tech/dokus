package tech.dokus.app.viewmodel

import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.domain.enums.NotificationType
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.model.NotificationPreferenceDto
import tech.dokus.foundation.app.state.DokusState

@Immutable
data class NotificationSettingsState(
    val preferences: DokusState<List<NotificationPreferenceDto>> = DokusState.loading(),
    val updatingTypes: Set<NotificationType> = emptySet(),
) : MVIState {
    companion object {
        val initial by lazy { NotificationSettingsState() }
    }
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
