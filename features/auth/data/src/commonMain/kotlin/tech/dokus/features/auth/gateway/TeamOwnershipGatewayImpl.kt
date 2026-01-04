package tech.dokus.features.auth.gateway

import tech.dokus.domain.ids.UserId
import tech.dokus.features.auth.datasource.TeamRemoteDataSource

internal class TeamOwnershipGatewayImpl(
    private val teamRemoteDataSource: TeamRemoteDataSource
) : TeamOwnershipGateway {
    override suspend fun transferOwnership(newOwnerId: UserId): Result<Unit> {
        return teamRemoteDataSource.transferOwnership(newOwnerId)
    }
}
