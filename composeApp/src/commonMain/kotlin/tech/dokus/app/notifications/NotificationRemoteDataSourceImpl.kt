package tech.dokus.app.notifications

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.resources.get
import io.ktor.client.plugins.resources.patch
import io.ktor.client.plugins.resources.post
import io.ktor.client.request.setBody
import tech.dokus.domain.enums.NotificationCategory
import tech.dokus.domain.enums.NotificationType
import tech.dokus.domain.ids.NotificationId
import tech.dokus.domain.model.NotificationDto
import tech.dokus.domain.model.NotificationPreferenceDto
import tech.dokus.domain.model.NotificationPreferencesResponse
import tech.dokus.domain.model.UnreadCountResponse
import tech.dokus.domain.model.UpdateNotificationPreferenceRequest
import tech.dokus.domain.model.common.PaginatedResponse
import tech.dokus.domain.routes.Notifications

class NotificationRemoteDataSourceImpl(
    private val httpClient: HttpClient
) : NotificationRemoteDataSource {

    override suspend fun listNotifications(
        type: NotificationType?,
        category: NotificationCategory?,
        isRead: Boolean?,
        limit: Int,
        offset: Int
    ): Result<PaginatedResponse<NotificationDto>> = runCatching {
        httpClient.get(
            Notifications(
                type = type,
                category = category,
                isRead = isRead,
                limit = limit,
                offset = offset
            )
        ).body()
    }

    override suspend fun unreadCount(): Result<Int> = runCatching {
        httpClient.get(Notifications.UnreadCount()).body<UnreadCountResponse>().count
    }

    override suspend fun markRead(notificationId: NotificationId): Result<Unit> = runCatching {
        httpClient.patch(
            Notifications.MarkRead(
                id = notificationId.toString()
            )
        ).body<Unit>()
    }

    override suspend fun markAllRead(): Result<Int> = runCatching {
        httpClient.post(Notifications.MarkAllRead())
            .body<Map<String, Int>>()["updated"] ?: 0
    }

    override suspend fun getPreferences(): Result<NotificationPreferencesResponse> = runCatching {
        httpClient.get(Notifications.Preferences()).body()
    }

    override suspend fun updatePreference(
        type: NotificationType,
        emailEnabled: Boolean
    ): Result<NotificationPreferenceDto> = runCatching {
        httpClient.patch(
            Notifications.Preferences.Type(
                parent = Notifications.Preferences(),
                type = type
            )
        ) {
            setBody(
                UpdateNotificationPreferenceRequest(
                    emailEnabled = emailEnabled
                )
            )
        }.body()
    }
}
