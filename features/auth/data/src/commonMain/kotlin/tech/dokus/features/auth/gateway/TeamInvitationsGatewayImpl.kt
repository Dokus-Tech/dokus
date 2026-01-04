package tech.dokus.features.auth.gateway

import tech.dokus.domain.ids.InvitationId
import tech.dokus.domain.model.CreateInvitationRequest
import tech.dokus.features.auth.datasource.TeamRemoteDataSource

internal class TeamInvitationsGatewayImpl(
    private val teamRemoteDataSource: TeamRemoteDataSource
) : TeamInvitationsGateway {
    override suspend fun createInvitation(request: CreateInvitationRequest) =
        teamRemoteDataSource.createInvitation(request)

    override suspend fun listPendingInvitations() = teamRemoteDataSource.listPendingInvitations()

    override suspend fun cancelInvitation(invitationId: InvitationId): Result<Unit> {
        return teamRemoteDataSource.cancelInvitation(invitationId)
    }
}
