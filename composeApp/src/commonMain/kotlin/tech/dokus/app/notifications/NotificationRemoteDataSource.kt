package tech.dokus.app.notifications

import tech.dokus.domain.enums.NotificationCategory
import tech.dokus.domain.enums.NotificationType
import tech.dokus.domain.ids.NotificationId
import tech.dokus.domain.model.NotificationDto
import tech.dokus.domain.model.NotificationPreferenceDto
import tech.dokus.domain.model.NotificationPreferencesResponse
import tech.dokus.domain.model.common.PaginatedResponse

interface NotificationRemoteDataSource {
    suspend fun listNotifications(
        type: NotificationType? = null,
        category: NotificationCategory? = null,
        isRead: Boolean? = null,
        limit: Int = 20,
        offset: Int = 0,
    ): Result<PaginatedResponse<NotificationDto>>

    suspend fun unreadCount(): Result<Int>

    suspend fun markRead(notificationId: NotificationId): Result<Unit>

    suspend fun markAllRead(): Result<Int>

    suspend fun getPreferences(): Result<NotificationPreferencesResponse>

    suspend fun updatePreference(
        type: NotificationType,
        emailEnabled: Boolean
    ): Result<NotificationPreferenceDto>
}
