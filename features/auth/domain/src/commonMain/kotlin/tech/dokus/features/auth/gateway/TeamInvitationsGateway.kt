package tech.dokus.features.auth.gateway

import tech.dokus.domain.ids.InvitationId
import tech.dokus.domain.model.CreateInvitationRequest
import tech.dokus.domain.model.TenantInvitationDto

/**
 * Gateway for team invitation management.
 */
interface TeamInvitationsGateway {
    suspend fun createInvitation(request: CreateInvitationRequest): Result<TenantInvitationDto>

    suspend fun listPendingInvitations(): Result<List<TenantInvitationDto>>

    suspend fun cancelInvitation(invitationId: InvitationId): Result<Unit>
}
