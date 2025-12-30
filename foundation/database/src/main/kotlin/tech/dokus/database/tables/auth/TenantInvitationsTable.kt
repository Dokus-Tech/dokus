package tech.dokus.database.tables.auth

import tech.dokus.domain.enums.InvitationStatus
import tech.dokus.domain.enums.UserRole
import tech.dokus.foundation.backend.database.dbEnumeration
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime

/**
 * Tenant invitations table - manages invitations for users to join tenants.
 * Supports email-based invitations with expiration and role assignment.
 *
 * OWNER: auth service
 */
object TenantInvitationsTable : UUIDTable("tenant_invitations") {
    // References
    val tenantId = reference("tenant_id", TenantTable, onDelete = ReferenceOption.CASCADE).index()
    val invitedBy = reference("invited_by", UsersTable, onDelete = ReferenceOption.CASCADE).index()

    // Core fields
    val email = varchar("email", 255).index()
    val role = dbEnumeration<UserRole>("role")
    val token = varchar("token", 255).uniqueIndex()
    val status = dbEnumeration<InvitationStatus>("status").default(InvitationStatus.Pending).index()

    // Expiration (30 days from creation)
    val expiresAt = datetime("expires_at")

    // Acceptance tracking
    val acceptedAt = datetime("accepted_at").nullable()
    val acceptedBy = reference("accepted_by", UsersTable, onDelete = ReferenceOption.SET_NULL).nullable()

    // Timestamps
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        // Composite indexes for common queries
        index(false, tenantId, status)
        index(false, email, status)
    }
}
