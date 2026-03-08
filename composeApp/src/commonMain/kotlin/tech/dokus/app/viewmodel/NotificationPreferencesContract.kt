package tech.dokus.app.viewmodel

import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.domain.asbtractions.RetryHandler
import tech.dokus.domain.enums.NotificationType
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.model.NotificationPreferenceDto

@Immutable
sealed interface NotificationPreferencesState : MVIState {
    data object Loading : NotificationPreferencesState

    @Immutable
    data class Content(
        val preferences: Map<NotificationType, NotificationPreferenceDto>,
        val updatingTypes: Set<NotificationType> = emptySet()
    ) : NotificationPreferencesState {
        fun preferenceFor(type: NotificationType): NotificationPreferenceDto {
            return preferences[type] ?: NotificationPreferenceDto(
                type = type,
                emailEnabled = type.defaultEmailEnabled,
                emailLocked = type.emailLocked
            )
        }
    }

    data class Error(
        val exception: DokusException,
        val retryHandler: RetryHandler,
    ) : NotificationPreferencesState
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

