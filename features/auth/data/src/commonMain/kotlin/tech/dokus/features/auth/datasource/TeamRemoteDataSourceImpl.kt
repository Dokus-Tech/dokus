package tech.dokus.features.auth.datasource

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.resources.delete
import io.ktor.client.plugins.resources.get
import io.ktor.client.plugins.resources.post
import io.ktor.client.plugins.resources.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import tech.dokus.domain.enums.InvitationStatus
import tech.dokus.domain.enums.UserRole
import tech.dokus.domain.ids.InvitationId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.CreateInvitationRequest
import tech.dokus.domain.model.TeamMember
import tech.dokus.domain.model.TenantInvitation
import tech.dokus.domain.model.TransferOwnershipRequest
import tech.dokus.domain.model.UpdateMemberRoleRequest
import tech.dokus.domain.routes.Team

/**
 * HTTP implementation of TeamRemoteDataSource.
 * Uses authenticated Ktor HttpClient with type-safe routing to communicate with the team management API.
 */
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
            httpClient.put(Team.Owner()) {
                contentType(ContentType.Application.Json)
                setBody(TransferOwnershipRequest(newOwnerId))
            }
        }
    }
}
