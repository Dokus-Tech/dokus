package tech.dokus.backend.services.notifications

import tech.dokus.backend.services.auth.EmailService
import tech.dokus.backend.services.auth.EmailTemplateRenderer
import tech.dokus.database.repository.auth.UserRepository
import tech.dokus.database.repository.notifications.NotificationRepository
import tech.dokus.domain.enums.NotificationCategory
import tech.dokus.domain.enums.NotificationReferenceType
import tech.dokus.domain.enums.NotificationType
import tech.dokus.domain.ids.NotificationId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.NotificationDto
import tech.dokus.domain.model.common.PaginatedResponse
import tech.dokus.foundation.backend.utils.loggerFor

class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val userRepository: UserRepository,
    private val preferencesService: NotificationPreferencesService,
    private val emailService: EmailService,
    private val emailTemplateRenderer: EmailTemplateRenderer
) {

    private val logger = loggerFor()

    suspend fun list(
        tenantId: TenantId,
        userId: UserId,
        type: NotificationType? = null,
        category: NotificationCategory? = null,
        isRead: Boolean? = null,
        limit: Int = 20,
        offset: Int = 0
    ): Result<PaginatedResponse<NotificationDto>> {
        return notificationRepository.list(
            tenantId = tenantId,
            userId = userId,
            type = type,
            category = category,
            isRead = isRead,
            limit = limit,
            offset = offset
        )
    }

    suspend fun unreadCount(
        tenantId: TenantId,
        userId: UserId
    ): Result<Int> = notificationRepository.unreadCount(tenantId, userId)

    suspend fun markRead(
        tenantId: TenantId,
        userId: UserId,
        notificationId: NotificationId
    ): Result<Boolean> = notificationRepository.markRead(tenantId, userId, notificationId)

    suspend fun markAllRead(
        tenantId: TenantId,
        userId: UserId
    ): Result<Int> = notificationRepository.markAllRead(tenantId, userId)

    suspend fun emit(event: NotificationEmission): Result<List<NotificationDto>> = runCatching {
        val recipients = userRepository.listByTenant(event.tenantId, activeOnly = true)

        val createdNotifications = mutableListOf<NotificationDto>()
        recipients.forEach { userInTenant ->
            val user = userInTenant.user

            val notification = notificationRepository.create(
                tenantId = event.tenantId,
                userId = user.id,
                type = event.type,
                title = event.title,
                referenceType = event.referenceType,
                referenceId = event.referenceId,
                isRead = false,
                emailSent = false
            ).getOrThrow()

            createdNotifications += notification

            sendEmailIfEnabled(
                userId = user.id,
                userEmail = user.email.value,
                notification = notification,
                event = event
            )
        }

        createdNotifications
    }

    private suspend fun sendEmailIfEnabled(
        userId: UserId,
        userEmail: String,
        notification: NotificationDto,
        event: NotificationEmission
    ) {
        val emailEnabled = preferencesService.isEmailEnabled(userId, event.type)
            .getOrElse { error ->
                logger.warn("Failed to resolve notification email preferences for user {}", userId, error)
                event.type.emailLocked || event.type.defaultEmailEnabled
            }

        if (!emailEnabled) {
            return
        }

        val recentlySent = notificationRepository.hasRecentEmailFor(
            userId = userId,
            type = event.type,
            referenceId = event.referenceId
        ).getOrElse { error ->
            logger.warn("Failed email dedup check for user {}", userId, error)
            false
        }

        if (recentlySent) {
            logger.debug(
                "Skipping duplicate notification email for user {} type {} reference {}",
                userId,
                event.type,
                event.referenceId
            )
            return
        }

        val template = emailTemplateRenderer.renderNotification(
            type = event.type,
            title = event.title,
            details = event.emailDetails,
            openPath = event.openPath
        )

        emailService.send(
            to = userEmail,
            subject = template.subject,
            htmlBody = template.htmlBody,
            textBody = template.textBody
        ).onSuccess {
            notificationRepository.markEmailSent(
                tenantId = notification.tenantId,
                userId = userId,
                notificationId = notification.id
            )
        }.onFailure { error ->
            logger.error(
                "Failed to send notification email for user {} type {}",
                userId,
                event.type,
                error
            )
        }
    }
}

data class NotificationEmission(
    val tenantId: TenantId,
    val type: NotificationType,
    val title: String,
    val referenceType: NotificationReferenceType,
    val referenceId: String,
    val openPath: String,
    val emailDetails: List<String> = emptyList()
)
