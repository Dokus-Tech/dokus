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
data class NotificationPreferencesState(
    val preferences: DokusState<Map<NotificationType, NotificationPreferenceDto>> = DokusState.loading(),
    val updatingTypes: Set<NotificationType> = emptySet(),
) : MVIState {
    fun preferenceFor(type: NotificationType): NotificationPreferenceDto {
        val prefsMap = (preferences as? DokusState.Success)?.data ?: emptyMap()
        return prefsMap[type] ?: NotificationPreferenceDto(
            type = type,
            emailEnabled = type.defaultEmailEnabled,
            emailLocked = type.emailLocked
        )
    }

    companion object {
        val initial by lazy { NotificationPreferencesState() }
    }
}

@Immutable
sealed interface NotificationPreferencesIntent : MVIIntent {
    data object Load : NotificationPreferencesIntent
    data class ToggleEmail(val type: NotificationType, val enabled: Boolean) : NotificationPreferencesIntent
}

@Immutable
sealed interface NotificationPreferencesAction : MVIAction {
    data class ShowError(val error: DokusException) : NotificationPreferencesAction
}

