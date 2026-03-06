package tech.dokus.features.auth.gateway

import tech.dokus.domain.ids.FirmId
import tech.dokus.domain.model.auth.BookkeeperFirmSearchItem
import tech.dokus.domain.model.auth.GrantBookkeeperAccessResponse
import tech.dokus.domain.model.auth.TenantBookkeeperAccessItem
import tech.dokus.features.auth.datasource.TeamRemoteDataSource

internal class TeamBookkeeperAccessGatewayImpl(
    private val teamRemoteDataSource: TeamRemoteDataSource
) : TeamBookkeeperAccessGateway {
    override suspend fun searchBookkeeperFirms(query: String, limit: Int): Result<List<BookkeeperFirmSearchItem>> {
        return teamRemoteDataSource.searchBookkeeperFirms(query = query, limit = limit)
    }

    override suspend fun listBookkeeperAccess(): Result<List<TenantBookkeeperAccessItem>> {
        return teamRemoteDataSource.listBookkeeperAccess()
    }

    override suspend fun grantBookkeeperAccess(firmId: FirmId): Result<GrantBookkeeperAccessResponse> {
        return teamRemoteDataSource.grantBookkeeperAccess(firmId)
    }

    override suspend fun revokeBookkeeperAccess(firmId: FirmId): Result<Unit> {
        return teamRemoteDataSource.revokeBookkeeperAccess(firmId)
    }
}
