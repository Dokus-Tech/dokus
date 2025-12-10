package ai.dokus.foundation.database.tables.auth

import ai.dokus.foundation.domain.enums.InvitationStatus
import ai.dokus.foundation.domain.enums.UserRole
import ai.dokus.foundation.ktor.database.dbEnumeration
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
    val tenantId = reference("tenant_id", TenantTable, onDelete = ReferenceOption.CASCADE)
    val invitedBy = reference("invited_by", UsersTable, onDelete = ReferenceOption.CASCADE)

    // Core fields
    val email = varchar("email", 255)
    val role = dbEnumeration<UserRole>("role")
    val token = varchar("token", 255).uniqueIndex()
    val status = dbEnumeration<InvitationStatus>("status").default(InvitationStatus.Pending)

    // Expiration (30 days from creation)
    val expiresAt = datetime("expires_at")

    // Acceptance tracking
    val acceptedAt = datetime("accepted_at").nullable()
    val acceptedBy = reference("accepted_by", UsersTable, onDelete = ReferenceOption.SET_NULL).nullable()

    // Timestamps
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        // Index all foreign keys for query performance
        index(false, tenantId)
        index(false, invitedBy)

        // Index for email lookups
        index(false, email)

        // Index for status filtering
        index(false, status)

        // Composite indexes for common queries
        index(false, tenantId, status)
        index(false, email, status)
    }
}
