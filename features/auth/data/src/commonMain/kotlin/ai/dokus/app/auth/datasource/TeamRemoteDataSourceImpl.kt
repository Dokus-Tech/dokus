package ai.dokus.app.auth.datasource

import ai.dokus.foundation.domain.enums.InvitationStatus
import ai.dokus.foundation.domain.enums.UserRole
import ai.dokus.foundation.domain.ids.InvitationId
import ai.dokus.foundation.domain.ids.UserId
import ai.dokus.foundation.domain.model.CreateInvitationRequest
import ai.dokus.foundation.domain.model.TeamMember
import ai.dokus.foundation.domain.model.TenantInvitation
import ai.dokus.foundation.domain.model.TransferOwnershipRequest
import ai.dokus.foundation.domain.model.UpdateMemberRoleRequest
import ai.dokus.foundation.domain.routes.Team
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.resources.delete
import io.ktor.client.plugins.resources.get
import io.ktor.client.plugins.resources.post
import io.ktor.client.plugins.resources.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlin.uuid.ExperimentalUuidApi

/**
 * HTTP implementation of TeamRemoteDataSource.
 * Uses authenticated Ktor HttpClient with type-safe routing to communicate with the team management API.
 */
@OptIn(ExperimentalUuidApi::class)
internal class TeamRemoteDataSourceImpl(
    private val httpClient: HttpClient,
) : TeamRemoteDataSource {

    override suspend fun listTeamMembers(): Result<List<TeamMember>> {
        return runCatching {
            httpClient.get(Team.Members()).body()
        }
    }

    override suspend fun createInvitation(request: CreateInvitationRequest): Result<TenantInvitation> {
        return runCatching {
            httpClient.post(Team.Invitations()) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }
    }

    override suspend fun listPendingInvitations(): Result<List<TenantInvitation>> {
        return runCatching {
            httpClient.get(Team.Invitations(status = InvitationStatus.Pending)).body()
        }
    }

    override suspend fun cancelInvitation(id: InvitationId): Result<Unit> {
        return runCatching {
            val invitations = Team.Invitations()
            httpClient.delete(Team.Invitations.Id(parent = invitations, id = id.value.toString()))
        }
    }

    override suspend fun updateMemberRole(userId: UserId, newRole: UserRole): Result<Unit> {
        return runCatching {
            val members = Team.Members()
            val memberId = Team.Members.Id(parent = members, userId = userId.value.toString())
            httpClient.put(Team.Members.Id.Role(parent = memberId)) {
                contentType(ContentType.Application.Json)
                setBody(UpdateMemberRoleRequest(newRole))
            }
        }
    }

    override suspend fun removeMember(userId: UserId): Result<Unit> {
        return runCatching {
            val members = Team.Members()
            httpClient.delete(Team.Members.Id(parent = members, userId = userId.value.toString()))
        }
    }

    override suspend fun transferOwnership(newOwnerId: UserId): Result<Unit> {
        return runCatching {
            httpClient.post(Team.TransferOwnership()) {
                contentType(ContentType.Application.Json)
                setBody(TransferOwnershipRequest(newOwnerId))
            }
        }
    }
}
