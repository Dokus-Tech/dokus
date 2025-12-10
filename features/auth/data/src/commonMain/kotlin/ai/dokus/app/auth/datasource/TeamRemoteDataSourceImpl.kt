package ai.dokus.app.auth.datasource

import ai.dokus.foundation.domain.enums.UserRole
import ai.dokus.foundation.domain.ids.InvitationId
import ai.dokus.foundation.domain.ids.UserId
import ai.dokus.foundation.domain.model.CreateInvitationRequest
import ai.dokus.foundation.domain.model.TeamMember
import ai.dokus.foundation.domain.model.TenantInvitation
import ai.dokus.foundation.domain.model.TransferOwnershipRequest
import ai.dokus.foundation.domain.model.UpdateMemberRoleRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlin.uuid.ExperimentalUuidApi

/**
 * HTTP implementation of TeamRemoteDataSource.
 * Uses authenticated Ktor HttpClient to communicate with the team management API.
 */
@OptIn(ExperimentalUuidApi::class)
internal class TeamRemoteDataSourceImpl(
    private val httpClient: HttpClient,
) : TeamRemoteDataSource {

    override suspend fun listTeamMembers(): Result<List<TeamMember>> {
        return runCatching {
            httpClient.get("/api/v1/team/members").body()
        }
    }

    override suspend fun createInvitation(request: CreateInvitationRequest): Result<TenantInvitation> {
        return runCatching {
            httpClient.post("/api/v1/team/invitations") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }
    }

    override suspend fun listPendingInvitations(): Result<List<TenantInvitation>> {
        return runCatching {
            httpClient.get("/api/v1/team/invitations?status=PENDING").body()
        }
    }

    override suspend fun cancelInvitation(id: InvitationId): Result<Unit> {
        return runCatching {
            httpClient.delete("/api/v1/team/invitations/${id.value}")
        }
    }

    override suspend fun updateMemberRole(userId: UserId, newRole: UserRole): Result<Unit> {
        return runCatching {
            httpClient.put("/api/v1/team/members/${userId.value}/role") {
                contentType(ContentType.Application.Json)
                setBody(UpdateMemberRoleRequest(newRole))
            }
        }
    }

    override suspend fun removeMember(userId: UserId): Result<Unit> {
        return runCatching {
            httpClient.delete("/api/v1/team/members/${userId.value}")
        }
    }

    override suspend fun transferOwnership(newOwnerId: UserId): Result<Unit> {
        return runCatching {
            httpClient.post("/api/v1/team/transfer-ownership") {
                contentType(ContentType.Application.Json)
                setBody(TransferOwnershipRequest(newOwnerId))
            }
        }
    }
}
