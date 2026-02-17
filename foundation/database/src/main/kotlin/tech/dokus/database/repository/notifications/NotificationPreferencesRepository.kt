package tech.dokus.database.repository.notifications
import kotlin.uuid.Uuid

import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.upsert
import tech.dokus.database.tables.notifications.NotificationPreferencesTable
import tech.dokus.domain.enums.NotificationType
import tech.dokus.domain.ids.UserId
import tech.dokus.foundation.backend.database.dbQuery

class NotificationPreferencesRepository {

    suspend fun listOverrides(userId: UserId): Result<Map<NotificationType, Boolean>> = runCatching {
        dbQuery {
            NotificationPreferencesTable.selectAll()
                .where { NotificationPreferencesTable.userId eq Uuid.parse(userId.toString()) }
                .associate { row ->
                    row[NotificationPreferencesTable.type] to row[NotificationPreferencesTable.emailEnabled]
                }
        }
    }

    suspend fun setOverride(
        userId: UserId,
        type: NotificationType,
        emailEnabled: Boolean
    ): Result<Unit> = runCatching {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val userUuid = Uuid.parse(userId.toString())

        dbQuery {
            NotificationPreferencesTable.upsert(
                NotificationPreferencesTable.userId,
                NotificationPreferencesTable.type,
                onUpdate = { stmt ->
                    stmt[NotificationPreferencesTable.emailEnabled] = emailEnabled
                    stmt[NotificationPreferencesTable.updatedAt] = now
                }
            ) {
                it[NotificationPreferencesTable.userId] = userUuid
                it[NotificationPreferencesTable.type] = type
                it[NotificationPreferencesTable.emailEnabled] = emailEnabled
                it[NotificationPreferencesTable.updatedAt] = now
                it[createdAt] = now
            }
        }
    }

    suspend fun removeOverride(
        userId: UserId,
        type: NotificationType
    ): Result<Boolean> = runCatching {
        dbQuery {
            val deleted = NotificationPreferencesTable.deleteWhere {
                (NotificationPreferencesTable.userId eq Uuid.parse(userId.toString())) and
                    (NotificationPreferencesTable.type eq type)
            }
            deleted > 0
        }
    }
}
