package tech.dokus.database.entity

import kotlinx.datetime.LocalDateTime
import tech.dokus.domain.Email
import tech.dokus.domain.enums.InvitationStatus
import tech.dokus.domain.enums.UserRole
import tech.dokus.domain.ids.InvitationId
import tech.dokus.domain.ids.TenantId

/**
 * Database entity for tenant invitations.
 * Maps directly to the tenant_invitations table.
 */
data class TenantInvitationEntity(
    val id: InvitationId,
    val tenantId: TenantId,
    val email: Email,
    val role: UserRole,
    val invitedByName: String,
    val status: InvitationStatus,
    val expiresAt: LocalDateTime,
    val createdAt: LocalDateTime,
) {
    companion object
}
