package tech.dokus.database.repository.notifications

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import tech.dokus.database.tables.notifications.NotificationPreferencesTable
import tech.dokus.domain.enums.NotificationType
import tech.dokus.domain.ids.UserId
import tech.dokus.foundation.backend.database.dbQuery
import java.util.UUID

class NotificationPreferencesRepository {

    suspend fun listOverrides(userId: UserId): Result<Map<NotificationType, Boolean>> = runCatching {
        dbQuery {
            NotificationPreferencesTable.selectAll()
                .where { NotificationPreferencesTable.userId eq UUID.fromString(userId.toString()) }
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
        val userUuid = UUID.fromString(userId.toString())

        dbQuery {
            val updated = NotificationPreferencesTable.update({
                (NotificationPreferencesTable.userId eq userUuid) and
                    (NotificationPreferencesTable.type eq type)
            }) {
                it[NotificationPreferencesTable.emailEnabled] = emailEnabled
                it[NotificationPreferencesTable.updatedAt] = now
            }

            if (updated == 0) {
                NotificationPreferencesTable.insert {
                    it[NotificationPreferencesTable.userId] = userUuid
                    it[NotificationPreferencesTable.type] = type
                    it[NotificationPreferencesTable.emailEnabled] = emailEnabled
                    it[NotificationPreferencesTable.updatedAt] = now
                    it[createdAt] = now
                }
            }
        }
    }

    suspend fun removeOverride(
        userId: UserId,
        type: NotificationType
    ): Result<Boolean> = runCatching {
        dbQuery {
            val deleted = NotificationPreferencesTable.deleteWhere {
                (NotificationPreferencesTable.userId eq UUID.fromString(userId.toString())) and
                    (NotificationPreferencesTable.type eq type)
            }
            deleted > 0
        }
    }
}
