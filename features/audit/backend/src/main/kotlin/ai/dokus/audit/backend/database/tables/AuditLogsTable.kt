package ai.dokus.audit.backend.database.tables

import ai.dokus.foundation.domain.enums.AuditAction
import ai.dokus.foundation.domain.enums.EntityType
import ai.dokus.foundation.ktor.database.dbEnumeration
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime

/**
 * Audit logs for compliance and security tracking
 * Immutable records with 7-year retention for financial records
 */
object AuditLogsTable : UUIDTable("audit_logs") {
    val organizationId = uuid("organization_id")
    val userId = uuid("user_id").nullable()

    val action = dbEnumeration<AuditAction>("action")
    val entityType = dbEnumeration<EntityType>("entity_type")
    val entityId = varchar("entity_id", 255)

    // JSON columns for old and new values
    val oldValues = text("old_values").nullable()
    val newValues = text("new_values").nullable()

    // Request metadata
    val ipAddress = varchar("ip_address", 45).nullable()
    val userAgent = text("user_agent").nullable()

    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)

    init {
        index(false, organizationId, createdAt)
        index(false, entityType, entityId)
        index(false, userId)
        index(false, action)
    }
}
