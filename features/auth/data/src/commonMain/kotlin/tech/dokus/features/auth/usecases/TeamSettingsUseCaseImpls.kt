package tech.dokus.features.auth.usecases

import tech.dokus.domain.enums.UserRole
import tech.dokus.domain.ids.InvitationId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.CreateInvitationRequest
import tech.dokus.domain.model.TeamMember
import tech.dokus.domain.model.TenantInvitation
import tech.dokus.features.auth.gateway.TeamSettingsGateway

internal class ListTeamMembersUseCaseImpl(
    private val teamSettingsGateway: TeamSettingsGateway
) : ListTeamMembersUseCase {
    override suspend fun invoke(): Result<List<TeamMember>> {
        return teamSettingsGateway.listTeamMembers()
    }
}

internal class ListPendingInvitationsUseCaseImpl(
    private val teamSettingsGateway: TeamSettingsGateway
) : ListPendingInvitationsUseCase {
    override suspend fun invoke(): Result<List<TenantInvitation>> {
        return teamSettingsGateway.listPendingInvitations()
    }
}

internal class CreateInvitationUseCaseImpl(
    private val teamSettingsGateway: TeamSettingsGateway
) : CreateInvitationUseCase {
    override suspend fun invoke(request: CreateInvitationRequest): Result<TenantInvitation> {
        return teamSettingsGateway.createInvitation(request)
    }
}

internal class CancelInvitationUseCaseImpl(
    private val teamSettingsGateway: TeamSettingsGateway
) : CancelInvitationUseCase {
    override suspend fun invoke(invitationId: InvitationId): Result<Unit> {
        return teamSettingsGateway.cancelInvitation(invitationId)
    }
}

internal class UpdateTeamMemberRoleUseCaseImpl(
    private val teamSettingsGateway: TeamSettingsGateway
) : UpdateTeamMemberRoleUseCase {
    override suspend fun invoke(userId: UserId, newRole: UserRole): Result<Unit> {
        return teamSettingsGateway.updateMemberRole(userId, newRole)
    }
}

internal class RemoveTeamMemberUseCaseImpl(
    private val teamSettingsGateway: TeamSettingsGateway
) : RemoveTeamMemberUseCase {
    override suspend fun invoke(userId: UserId): Result<Unit> {
        return teamSettingsGateway.removeMember(userId)
    }
}

internal class TransferWorkspaceOwnershipUseCaseImpl(
    private val teamSettingsGateway: TeamSettingsGateway
) : TransferWorkspaceOwnershipUseCase {
    override suspend fun invoke(newOwnerId: UserId): Result<Unit> {
        return teamSettingsGateway.transferOwnership(newOwnerId)
    }
}
