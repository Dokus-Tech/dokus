package ai.dokus.foundation.database.tables

import ai.dokus.foundation.database.dbEnumeration
import ai.dokus.foundation.database.enums.AuditAction
import ai.dokus.foundation.database.enums.EntityType
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentDateTime
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

/**
 * Immutable audit trail for compliance
 * GDPR compliance, financial auditing, debugging
 */
object AuditLogsTable : UUIDTable("audit_logs") {
    val tenantId = reference("tenant_id", TenantsTable, onDelete = ReferenceOption.CASCADE)
    val userId = reference("user_id", UsersTable, onDelete = ReferenceOption.SET_NULL).nullable()

    val action = dbEnumeration<AuditAction>("action")
    val entityType = dbEnumeration<EntityType>("entity_type")
    val entityId = uuid("entity_id")

    // Change tracking (JSON)
    val oldValues = text("old_values").nullable()  // Before state
    val newValues = text("new_values").nullable()  // After state

    // Request context
    val ipAddress = varchar("ip_address", 45).nullable()  // IPv4/IPv6
    val userAgent = text("user_agent").nullable()

    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)

    init {
        index(false, tenantId)
        index(false, entityType, entityId)  // Entity history
        index(false, createdAt)             // Time-based queries
        index(false, userId)                // User activity
        index(false, action)                // Action filtering
    }
}