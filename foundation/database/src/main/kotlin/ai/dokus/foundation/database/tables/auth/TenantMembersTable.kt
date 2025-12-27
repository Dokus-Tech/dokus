package ai.dokus.foundation.database.tables.auth

import tech.dokus.domain.enums.UserRole
import tech.dokus.foundation.ktor.database.dbEnumeration
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime

/**
 * Tenant membership table - links users to tenants with roles.
 * A user can belong to multiple tenants with different roles.
 *
 * OWNER: auth service
 */
object TenantMembersTable : UUIDTable("tenant_members") {
    val userId = reference("user_id", UsersTable, onDelete = ReferenceOption.CASCADE).index()
    val tenantId = reference("tenant_id", TenantTable, onDelete = ReferenceOption.CASCADE).index()
    val role = dbEnumeration<UserRole>("role")
    val isActive = bool("is_active").default(true)

    // Timestamps
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex(userId, tenantId)
        index(false, tenantId, isActive) // For active members query
    }
}
