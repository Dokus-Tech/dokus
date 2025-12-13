package ai.dokus.foundation.database.tables.audit

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
    val tenantId = uuid("tenant_id").references(
        ai.dokus.foundation.database.tables.auth.TenantTable.id,
        onDelete = org.jetbrains.exposed.v1.core.ReferenceOption.CASCADE
    )
    val userId = uuid("user_id").references(
        ai.dokus.foundation.database.tables.auth.UsersTable.id,
        onDelete = org.jetbrains.exposed.v1.core.ReferenceOption.SET_NULL
    ).nullable().index()

    val action = dbEnumeration<AuditAction>("action").index()
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
        index(false, tenantId, createdAt)
        index(false, entityType, entityId)
    }
}
