package tech.dokus.database.tables.notifications

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime
import tech.dokus.database.tables.auth.UsersTable
import tech.dokus.domain.enums.NotificationType
import tech.dokus.foundation.backend.database.dbEnumeration

object NotificationPreferencesTable : UuidTable("user_notification_preferences") {
    val userId = reference("user_id", UsersTable, onDelete = ReferenceOption.CASCADE).index()
    val type = dbEnumeration<NotificationType>("type")
    val emailEnabled = bool("email_enabled")
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex(userId, type)
    }
}
