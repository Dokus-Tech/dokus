package tech.dokus.features.auth.usecases

import tech.dokus.domain.enums.UserRole
import tech.dokus.domain.ids.InvitationId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.CreateInvitationRequest
import tech.dokus.domain.model.TeamMember
import tech.dokus.domain.model.TenantInvitation
import tech.dokus.features.auth.gateway.TeamInvitationsGateway
import tech.dokus.features.auth.gateway.TeamMembersGateway
import tech.dokus.features.auth.gateway.TeamOwnershipGateway

internal class ListTeamMembersUseCaseImpl(
    private val teamMembersGateway: TeamMembersGateway
) : ListTeamMembersUseCase {
    override suspend fun invoke(): Result<List<TeamMember>> {
        return teamMembersGateway.listTeamMembers()
    }
}

internal class ListPendingInvitationsUseCaseImpl(
    private val teamInvitationsGateway: TeamInvitationsGateway
) : ListPendingInvitationsUseCase {
    override suspend fun invoke(): Result<List<TenantInvitation>> {
        return teamInvitationsGateway.listPendingInvitations()
    }
}

internal class CreateInvitationUseCaseImpl(
    private val teamInvitationsGateway: TeamInvitationsGateway
) : CreateInvitationUseCase {
    override suspend fun invoke(request: CreateInvitationRequest): Result<TenantInvitation> {
        return teamInvitationsGateway.createInvitation(request)
    }
}

internal class CancelInvitationUseCaseImpl(
    private val teamInvitationsGateway: TeamInvitationsGateway
) : CancelInvitationUseCase {
    override suspend fun invoke(invitationId: InvitationId): Result<Unit> {
        return teamInvitationsGateway.cancelInvitation(invitationId)
    }
}

internal class UpdateTeamMemberRoleUseCaseImpl(
    private val teamMembersGateway: TeamMembersGateway
) : UpdateTeamMemberRoleUseCase {
    override suspend fun invoke(userId: UserId, newRole: UserRole): Result<Unit> {
        return teamMembersGateway.updateMemberRole(userId, newRole)
    }
}

internal class RemoveTeamMemberUseCaseImpl(
    private val teamMembersGateway: TeamMembersGateway
) : RemoveTeamMemberUseCase {
    override suspend fun invoke(userId: UserId): Result<Unit> {
        return teamMembersGateway.removeMember(userId)
    }
}

internal class TransferWorkspaceOwnershipUseCaseImpl(
    private val teamOwnershipGateway: TeamOwnershipGateway
) : TransferWorkspaceOwnershipUseCase {
    override suspend fun invoke(newOwnerId: UserId): Result<Unit> {
        return teamOwnershipGateway.transferOwnership(newOwnerId)
    }
}
