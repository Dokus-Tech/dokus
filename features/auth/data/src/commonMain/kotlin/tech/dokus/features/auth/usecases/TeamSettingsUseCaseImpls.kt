package tech.dokus.features.auth.usecases

import tech.dokus.domain.enums.UserRole
import tech.dokus.domain.ids.FirmId
import tech.dokus.domain.ids.InvitationId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.CreateInvitationRequest
import tech.dokus.domain.model.TeamMember
import tech.dokus.domain.model.TenantInvitationDto
import tech.dokus.domain.model.auth.BookkeeperFirmSearchItem
import tech.dokus.domain.model.auth.GrantBookkeeperAccessResponse
import tech.dokus.domain.model.auth.TenantBookkeeperAccessItem
import tech.dokus.features.auth.gateway.TeamBookkeeperAccessGateway
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
    override suspend fun invoke(): Result<List<TenantInvitationDto>> {
        return teamInvitationsGateway.listPendingInvitations()
    }
}

internal class CreateInvitationUseCaseImpl(
    private val teamInvitationsGateway: TeamInvitationsGateway
) : CreateInvitationUseCase {
    override suspend fun invoke(request: CreateInvitationRequest): Result<TenantInvitationDto> {
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

internal class SearchBookkeeperFirmsUseCaseImpl(
    private val teamBookkeeperAccessGateway: TeamBookkeeperAccessGateway
) : SearchBookkeeperFirmsUseCase {
    override suspend fun invoke(query: String, limit: Int): Result<List<BookkeeperFirmSearchItem>> {
        return teamBookkeeperAccessGateway.searchBookkeeperFirms(
            query = query,
            limit = limit,
        )
    }
}

internal class ListBookkeeperAccessUseCaseImpl(
    private val teamBookkeeperAccessGateway: TeamBookkeeperAccessGateway
) : ListBookkeeperAccessUseCase {
    override suspend fun invoke(): Result<List<TenantBookkeeperAccessItem>> {
        return teamBookkeeperAccessGateway.listBookkeeperAccess()
    }
}

internal class GrantBookkeeperAccessUseCaseImpl(
    private val teamBookkeeperAccessGateway: TeamBookkeeperAccessGateway
) : GrantBookkeeperAccessUseCase {
    override suspend fun invoke(firmId: FirmId): Result<GrantBookkeeperAccessResponse> {
        return teamBookkeeperAccessGateway.grantBookkeeperAccess(firmId)
    }
}

internal class RevokeBookkeeperAccessUseCaseImpl(
    private val teamBookkeeperAccessGateway: TeamBookkeeperAccessGateway
) : RevokeBookkeeperAccessUseCase {
    override suspend fun invoke(firmId: FirmId): Result<Unit> {
        return teamBookkeeperAccessGateway.revokeBookkeeperAccess(firmId)
    }
}
