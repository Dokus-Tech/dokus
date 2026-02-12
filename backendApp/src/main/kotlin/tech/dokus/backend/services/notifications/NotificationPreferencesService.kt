package tech.dokus.backend.services.notifications

import tech.dokus.database.repository.notifications.NotificationPreferencesRepository
import tech.dokus.domain.enums.NotificationType
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.NotificationPreferenceDto
import tech.dokus.domain.model.NotificationPreferencesResponse

class NotificationPreferencesService(
    private val preferencesRepository: NotificationPreferencesRepository
) {

    suspend fun list(userId: UserId): Result<NotificationPreferencesResponse> = runCatching {
        val overrides = preferencesRepository.listOverrides(userId).getOrThrow()
        NotificationPreferencesResponse(
            preferences = NotificationType.entries.map { type ->
                NotificationPreferenceDto(
                    type = type,
                    emailEnabled = resolveEmailEnabled(type, overrides[type]),
                    emailLocked = type.emailLocked
                )
            }
        )
    }

    suspend fun update(
        userId: UserId,
        type: NotificationType,
        emailEnabled: Boolean
    ): Result<NotificationPreferenceDto> = runCatching {
        if (type.emailLocked && !emailEnabled) {
            throw DokusException.BadRequest("Email notifications for $type are required and cannot be disabled")
        }

        val resolved = if (type.emailLocked) true else emailEnabled

        if (resolved == type.defaultEmailEnabled) {
            preferencesRepository.removeOverride(userId, type).getOrThrow()
        } else {
            preferencesRepository.setOverride(userId, type, resolved).getOrThrow()
        }

        NotificationPreferenceDto(
            type = type,
            emailEnabled = resolved,
            emailLocked = type.emailLocked
        )
    }

    suspend fun isEmailEnabled(
        userId: UserId,
        type: NotificationType
    ): Result<Boolean> = runCatching {
        val overrides = preferencesRepository.listOverrides(userId).getOrThrow()
        resolveEmailEnabled(type, overrides[type])
    }

    private fun resolveEmailEnabled(type: NotificationType, overrideValue: Boolean?): Boolean {
        return when {
            type.emailLocked -> true
            overrideValue != null -> overrideValue
            else -> type.defaultEmailEnabled
        }
    }
}
