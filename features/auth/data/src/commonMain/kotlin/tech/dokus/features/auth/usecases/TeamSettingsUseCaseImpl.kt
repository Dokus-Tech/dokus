package tech.dokus.features.auth.usecases

import tech.dokus.domain.enums.UserRole
import tech.dokus.domain.ids.InvitationId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.CreateInvitationRequest
import tech.dokus.domain.model.TeamMember
import tech.dokus.domain.model.TenantInvitation
import tech.dokus.features.auth.datasource.TeamRemoteDataSource

internal class TeamSettingsUseCaseImpl(
    private val teamRemoteDataSource: TeamRemoteDataSource
) : TeamSettingsUseCase {
    override suspend fun listTeamMembers(): Result<List<TeamMember>> {
        return teamRemoteDataSource.listTeamMembers()
    }

    override suspend fun createInvitation(request: CreateInvitationRequest): Result<TenantInvitation> {
        return teamRemoteDataSource.createInvitation(request)
    }

    override suspend fun listPendingInvitations(): Result<List<TenantInvitation>> {
        return teamRemoteDataSource.listPendingInvitations()
    }

    override suspend fun cancelInvitation(invitationId: InvitationId): Result<Unit> {
        return teamRemoteDataSource.cancelInvitation(invitationId)
    }

    override suspend fun updateMemberRole(userId: UserId, newRole: UserRole): Result<Unit> {
        return teamRemoteDataSource.updateMemberRole(userId, newRole)
    }

    override suspend fun removeMember(userId: UserId): Result<Unit> {
        return teamRemoteDataSource.removeMember(userId)
    }

    override suspend fun transferOwnership(newOwnerId: UserId): Result<Unit> {
        return teamRemoteDataSource.transferOwnership(newOwnerId)
    }
}
