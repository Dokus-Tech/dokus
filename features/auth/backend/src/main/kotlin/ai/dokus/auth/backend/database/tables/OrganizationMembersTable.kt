package ai.dokus.auth.backend.database.tables

import ai.dokus.foundation.domain.enums.UserRole
import ai.dokus.foundation.ktor.database.dbEnumeration
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime

/**
 * Organization membership table - links users to organizations with roles.
 * A user can belong to multiple organizations with different roles.
 */
object OrganizationMembersTable : UUIDTable("organization_members") {
    val userId = reference("user_id", UsersTable, onDelete = ReferenceOption.CASCADE)
    val organizationId = reference("organization_id", OrganizationTable, onDelete = ReferenceOption.CASCADE)
    val role = dbEnumeration<UserRole>("role")
    val isActive = bool("is_active").default(true)

    // Timestamps
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex(userId, organizationId)
        index(false, userId)
        index(false, organizationId)
        index(false, organizationId, isActive) // For active members query
    }
}