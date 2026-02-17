package tech.dokus.database.repository.notifications
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import tech.dokus.database.tables.notifications.NotificationsTable
import tech.dokus.domain.enums.NotificationCategory
import tech.dokus.domain.enums.NotificationReferenceType
import tech.dokus.domain.enums.NotificationType
import tech.dokus.domain.ids.NotificationId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.NotificationDto
import tech.dokus.domain.model.common.PaginatedResponse
import tech.dokus.foundation.backend.database.dbQuery
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.uuid.Uuid

class NotificationRepository {

    suspend fun create(
        tenantId: TenantId,
        userId: UserId,
        type: NotificationType,
        title: String,
        referenceType: NotificationReferenceType,
        referenceId: String,
        isRead: Boolean = false,
        emailSent: Boolean = false,
    ): Result<NotificationDto> = runCatching {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val newId = Uuid.random()

        dbQuery {
            NotificationsTable.insert {
                it[id] = newId
                it[NotificationsTable.tenantId] = tenantId.value
                it[NotificationsTable.userId] = userId.value
                it[NotificationsTable.type] = type
                it[NotificationsTable.title] = title
                it[NotificationsTable.referenceType] = referenceType
                it[NotificationsTable.referenceId] = referenceId
                it[NotificationsTable.isRead] = isRead
                it[NotificationsTable.emailSent] = emailSent
                it[createdAt] = now
            }

            NotificationsTable.selectAll()
                .where { NotificationsTable.id eq newId }
                .map { it.toDto() }
                .single()
        }
    }

    suspend fun list(
        tenantId: TenantId,
        userId: UserId,
        type: NotificationType? = null,
        category: NotificationCategory? = null,
        isRead: Boolean? = null,
        limit: Int = 20,
        offset: Int = 0,
    ): Result<PaginatedResponse<NotificationDto>> = runCatching {
        dbQuery {
            var query = NotificationsTable.selectAll().where {
                (NotificationsTable.tenantId eq tenantId.value) and
                    (NotificationsTable.userId eq userId.value)
            }

            type?.let {
                query = query.andWhere { NotificationsTable.type eq it }
            }
            category?.let {
                val categoryTypes = NotificationType.entries.filter { t -> t.category == it }
                query = query.andWhere { NotificationsTable.type inList categoryTypes }
            }
            isRead?.let {
                query = query.andWhere { NotificationsTable.isRead eq it }
            }

            val total = query.count()
            val items = query
                .orderBy(NotificationsTable.createdAt to SortOrder.DESC)
                .limit(limit + offset)
                .map { it.toDto() }
                .drop(offset)

            PaginatedResponse(
                items = items,
                total = total,
                limit = limit,
                offset = offset
            )
        }
    }

    suspend fun unreadCount(
        tenantId: TenantId,
        userId: UserId
    ): Result<Int> = runCatching {
        dbQuery {
            NotificationsTable.selectAll()
                .where {
                    (NotificationsTable.tenantId eq tenantId.value) and
                        (NotificationsTable.userId eq userId.value) and
                        (NotificationsTable.isRead eq false)
                }
                .count()
                .toInt()
        }
    }

    suspend fun markRead(
        tenantId: TenantId,
        userId: UserId,
        notificationId: NotificationId
    ): Result<Boolean> = runCatching {
        dbQuery {
            val updated = NotificationsTable.update({
                (NotificationsTable.id eq notificationId.value) and
                    (NotificationsTable.tenantId eq tenantId.value) and
                    (NotificationsTable.userId eq userId.value)
            }) {
                it[isRead] = true
            }
            updated > 0
        }
    }

    suspend fun markAllRead(
        tenantId: TenantId,
        userId: UserId
    ): Result<Int> = runCatching {
        dbQuery {
            NotificationsTable.update({
                (NotificationsTable.tenantId eq tenantId.value) and
                    (NotificationsTable.userId eq userId.value) and
                    (NotificationsTable.isRead eq false)
            }) {
                it[isRead] = true
            }
        }
    }

    suspend fun markEmailSent(
        tenantId: TenantId,
        userId: UserId,
        notificationId: NotificationId
    ): Result<Boolean> = runCatching {
        dbQuery {
            val updated = NotificationsTable.update({
                (NotificationsTable.id eq notificationId.value) and
                    (NotificationsTable.tenantId eq tenantId.value) and
                    (NotificationsTable.userId eq userId.value)
            }) {
                it[emailSent] = true
            }
            updated > 0
        }
    }

    suspend fun hasRecentEmailFor(
        tenantId: TenantId,
        userId: UserId,
        type: NotificationType,
        referenceId: String
    ): Result<Boolean> = runCatching {
        val threshold = Clock.System.now()
            .minus(1.hours)
            .toLocalDateTime(TimeZone.UTC)

        dbQuery {
            NotificationsTable.selectAll()
                .where {
                    (NotificationsTable.tenantId eq tenantId.value) and
                        (NotificationsTable.userId eq userId.value) and
                        (NotificationsTable.type eq type) and
                        (NotificationsTable.referenceId eq referenceId) and
                        (NotificationsTable.emailSent eq true) and
                        (NotificationsTable.createdAt greaterEq threshold)
                }
                .limit(1)
                .any()
        }
    }

    private fun ResultRow.toDto(): NotificationDto = NotificationDto(
        id = NotificationId(this[NotificationsTable.id].value),
        tenantId = TenantId(this[NotificationsTable.tenantId].value),
        userId = UserId(this[NotificationsTable.userId].value),
        type = this[NotificationsTable.type],
        title = this[NotificationsTable.title],
        referenceType = this[NotificationsTable.referenceType],
        referenceId = this[NotificationsTable.referenceId],
        isRead = this[NotificationsTable.isRead],
        createdAt = this[NotificationsTable.createdAt],
        emailSent = this[NotificationsTable.emailSent]
    )
}
