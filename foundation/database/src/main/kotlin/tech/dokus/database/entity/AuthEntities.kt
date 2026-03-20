package tech.dokus.database.entity

import kotlinx.datetime.LocalDateTime
import tech.dokus.database.repository.auth.RefreshTokenInfo
import tech.dokus.database.tables.auth.WelcomeEmailJobsTable.JobStatus
import tech.dokus.domain.Email
import tech.dokus.domain.enums.InvitationStatus
import tech.dokus.domain.enums.UserRole
import tech.dokus.domain.ids.InvitationId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import java.util.UUID

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

internal data class ActiveTokenEntity(
    val rowId: UUID,
    val token: RefreshTokenInfo,
) {
    companion object
}

data class WelcomeEmailJobEntity(
    val id: UUID,
    val userId: UserId,
    val tenantId: TenantId,
    val status: JobStatus,
    val scheduledAt: LocalDateTime,
    val nextAttemptAt: LocalDateTime,
    val attemptCount: Int,
    val lastError: String?,
    val sentAt: LocalDateTime?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object
}
