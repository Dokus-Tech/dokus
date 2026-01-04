package tech.dokus.features.auth.usecases

import tech.dokus.domain.enums.UserRole
import tech.dokus.domain.ids.InvitationId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.CreateInvitationRequest
import tech.dokus.domain.model.TeamMember
import tech.dokus.domain.model.TenantInvitation

/**
 * Use case for managing team members and invitations.
 */
interface TeamSettingsUseCase {
    suspend fun listTeamMembers(): Result<List<TeamMember>>

    suspend fun createInvitation(request: CreateInvitationRequest): Result<TenantInvitation>

    suspend fun listPendingInvitations(): Result<List<TenantInvitation>>

    suspend fun cancelInvitation(invitationId: InvitationId): Result<Unit>

    suspend fun updateMemberRole(userId: UserId, newRole: UserRole): Result<Unit>

    suspend fun removeMember(userId: UserId): Result<Unit>

    suspend fun transferOwnership(newOwnerId: UserId): Result<Unit>
}
