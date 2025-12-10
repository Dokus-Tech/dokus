package ai.dokus.app.auth.datasource

import ai.dokus.foundation.domain.enums.UserRole
import ai.dokus.foundation.domain.ids.InvitationId
import ai.dokus.foundation.domain.ids.UserId
import ai.dokus.foundation.domain.model.CreateInvitationRequest
import ai.dokus.foundation.domain.model.TeamMember
import ai.dokus.foundation.domain.model.TenantInvitation

/**
 * Remote data source for team management operations.
 * All methods require authentication and return Result to handle errors gracefully.
 */
interface TeamRemoteDataSource {
    /**
     * List all active team members in the current tenant.
     * @return Result containing list of TeamMember
     */
    suspend fun listTeamMembers(): Result<List<TeamMember>>

    /**
     * Create a new invitation to join the team.
     * If the user already exists, they are added directly.
     * @param request Invitation details (email, role)
     * @return Result containing the created invitation
     */
    suspend fun createInvitation(request: CreateInvitationRequest): Result<TenantInvitation>

    /**
     * List pending invitations for the current tenant.
     * @return Result containing list of pending invitations
     */
    suspend fun listPendingInvitations(): Result<List<TenantInvitation>>

    /**
     * Cancel a pending invitation.
     * @param id Invitation ID to cancel
     * @return Result indicating success or failure
     */
    suspend fun cancelInvitation(id: InvitationId): Result<Unit>

    /**
     * Update a team member's role.
     * @param userId User whose role is being changed
     * @param newRole New role to assign
     * @return Result indicating success or failure
     */
    suspend fun updateMemberRole(userId: UserId, newRole: UserRole): Result<Unit>

    /**
     * Remove a team member from the workspace.
     * @param userId User to remove
     * @return Result indicating success or failure
     */
    suspend fun removeMember(userId: UserId): Result<Unit>

    /**
     * Transfer workspace ownership to another member.
     * @param newOwnerId User who will become the new Owner
     * @return Result indicating success or failure
     */
    suspend fun transferOwnership(newOwnerId: UserId): Result<Unit>
}
