package tech.dokus.features.auth.usecases

import tech.dokus.domain.enums.UserRole
import tech.dokus.domain.ids.FirmId
import tech.dokus.domain.ids.InvitationId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.CreateInvitationRequest
import tech.dokus.domain.model.TeamMember
import tech.dokus.domain.model.TenantInvitation
import tech.dokus.domain.model.auth.BookkeeperFirmSearchItem
import tech.dokus.domain.model.auth.GrantBookkeeperAccessResponse
import tech.dokus.domain.model.auth.TenantBookkeeperAccessItem

/**
 * Use case for listing team members.
 */
interface ListTeamMembersUseCase {
    suspend operator fun invoke(): Result<List<TeamMember>>
}

/**
 * Use case for listing pending invitations.
 */
interface ListPendingInvitationsUseCase {
    suspend operator fun invoke(): Result<List<TenantInvitation>>
}

/**
 * Use case for creating a team invitation.
 */
interface CreateInvitationUseCase {
    suspend operator fun invoke(request: CreateInvitationRequest): Result<TenantInvitation>
}

/**
 * Use case for canceling a team invitation.
 */
interface CancelInvitationUseCase {
    suspend operator fun invoke(invitationId: InvitationId): Result<Unit>
}

/**
 * Use case for updating a team member role.
 */
interface UpdateTeamMemberRoleUseCase {
    suspend operator fun invoke(userId: UserId, newRole: UserRole): Result<Unit>
}

/**
 * Use case for removing a team member.
 */
interface RemoveTeamMemberUseCase {
    suspend operator fun invoke(userId: UserId): Result<Unit>
}

/**
 * Use case for transferring workspace ownership.
 */
interface TransferWorkspaceOwnershipUseCase {
    suspend operator fun invoke(newOwnerId: UserId): Result<Unit>
}

interface SearchBookkeeperFirmsUseCase {
    suspend operator fun invoke(
        query: String,
        limit: Int = 20,
    ): Result<List<BookkeeperFirmSearchItem>>
}

interface ListBookkeeperAccessUseCase {
    suspend operator fun invoke(): Result<List<TenantBookkeeperAccessItem>>
}

interface GrantBookkeeperAccessUseCase {
    suspend operator fun invoke(firmId: FirmId): Result<GrantBookkeeperAccessResponse>
}

interface RevokeBookkeeperAccessUseCase {
    suspend operator fun invoke(firmId: FirmId): Result<Unit>
}
