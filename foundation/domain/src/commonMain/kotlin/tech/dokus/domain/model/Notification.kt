package tech.dokus.domain.model

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import tech.dokus.domain.enums.NotificationCategory
import tech.dokus.domain.enums.NotificationReferenceType
import tech.dokus.domain.enums.NotificationType
import tech.dokus.domain.ids.NotificationId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId

@Serializable
data class NotificationDto(
    val id: NotificationId,
    val tenantId: TenantId,
    val userId: UserId,
    val type: NotificationType,
    val category: NotificationCategory = type.category,
    val title: String,
    val referenceType: NotificationReferenceType,
    val referenceId: String,
    val isRead: Boolean,
    val createdAt: LocalDateTime,
    val emailSent: Boolean
)

@Serializable
data class UnreadCountResponse(
    val count: Int
)

@Serializable
data class UpdateNotificationPreferenceRequest(
    val emailEnabled: Boolean
)

@Serializable
data class NotificationPreferenceDto(
    val type: NotificationType,
    val category: NotificationCategory = type.category,
    val inAppEnabled: Boolean = true,
    val emailEnabled: Boolean,
    val emailLocked: Boolean = type.emailLocked,
    val defaultEmailEnabled: Boolean = type.defaultEmailEnabled
)

@Serializable
data class NotificationPreferencesResponse(
    val preferences: List<NotificationPreferenceDto>
)
