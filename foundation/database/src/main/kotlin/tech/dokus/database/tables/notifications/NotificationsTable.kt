package tech.dokus.database.tables.notifications

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.database.tables.auth.UsersTable
import tech.dokus.domain.enums.NotificationReferenceType
import tech.dokus.domain.enums.NotificationType
import tech.dokus.foundation.backend.database.dbEnumeration

object NotificationsTable : UUIDTable("user_notifications") {
    val tenantId = reference("tenant_id", TenantTable, onDelete = ReferenceOption.CASCADE).index()
    val userId = reference("user_id", UsersTable, onDelete = ReferenceOption.CASCADE).index()

    val type = dbEnumeration<NotificationType>("type").index()
    val title = varchar("title", 255)
    val referenceType = dbEnumeration<NotificationReferenceType>("reference_type")
    val referenceId = varchar("reference_id", 255)
    val isRead = bool("is_read").default(false).index()
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val emailSent = bool("email_sent").default(false)

    init {
        index(false, tenantId, userId, isRead, createdAt)
        index(false, tenantId, userId, type, referenceId, createdAt)
    }
}
