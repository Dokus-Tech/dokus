package tech.dokus.features.auth.gateway

import tech.dokus.domain.ids.InvitationId
import tech.dokus.domain.model.CreateInvitationRequest
import tech.dokus.domain.model.TenantInvitation

/**
 * Gateway for team invitation management.
 */
interface TeamInvitationsGateway {
    suspend fun createInvitation(request: CreateInvitationRequest): Result<TenantInvitation>

    suspend fun listPendingInvitations(): Result<List<TenantInvitation>>

    suspend fun cancelInvitation(invitationId: InvitationId): Result<Unit>
}
