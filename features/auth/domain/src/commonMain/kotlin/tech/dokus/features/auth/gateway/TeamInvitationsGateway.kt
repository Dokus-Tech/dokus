package tech.dokus.features.auth.gateway

import tech.dokus.domain.ids.InvitationId
import tech.dokus.domain.model.CreateInvitationRequest
import tech.dokus.domain.model.TenantInvitationEntity

/**
 * Gateway for team invitation management.
 */
interface TeamInvitationsGateway {
    suspend fun createInvitation(request: CreateInvitationRequest): Result<TenantInvitationEntity>

    suspend fun listPendingInvitations(): Result<List<TenantInvitationEntity>>

    suspend fun cancelInvitation(invitationId: InvitationId): Result<Unit>
}
