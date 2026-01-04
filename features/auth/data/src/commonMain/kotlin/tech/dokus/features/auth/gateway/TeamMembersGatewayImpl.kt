package tech.dokus.features.auth.gateway

import tech.dokus.domain.enums.UserRole
import tech.dokus.domain.ids.UserId
import tech.dokus.features.auth.datasource.TeamRemoteDataSource

internal class TeamMembersGatewayImpl(
    private val teamRemoteDataSource: TeamRemoteDataSource
) : TeamMembersGateway {
    override suspend fun listTeamMembers() = teamRemoteDataSource.listTeamMembers()

    override suspend fun updateMemberRole(userId: UserId, newRole: UserRole): Result<Unit> {
        return teamRemoteDataSource.updateMemberRole(userId, newRole)
    }

    override suspend fun removeMember(userId: UserId): Result<Unit> {
        return teamRemoteDataSource.removeMember(userId)
    }
}
