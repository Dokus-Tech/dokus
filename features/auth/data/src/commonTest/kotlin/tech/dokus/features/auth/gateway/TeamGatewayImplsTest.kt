package tech.dokus.features.auth.gateway

import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import tech.dokus.domain.enums.InvitationStatus
import tech.dokus.domain.enums.UserRole
import tech.dokus.domain.ids.FirmId
import tech.dokus.domain.ids.InvitationId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.CreateInvitationRequest
import tech.dokus.domain.model.TeamMember
import tech.dokus.domain.model.TenantInvitation
import tech.dokus.domain.model.auth.BookkeeperFirmSearchItem
import tech.dokus.domain.model.auth.GrantBookkeeperAccessResponse
import tech.dokus.domain.model.auth.TenantBookkeeperAccessItem
import tech.dokus.domain.Email
import tech.dokus.domain.DisplayName
import tech.dokus.domain.Name
import tech.dokus.domain.ids.VatNumber
import tech.dokus.features.auth.datasource.TeamRemoteDataSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class TeamGatewayImplsTest {

    @Test
    fun teamMembersGatewayDelegatesListAndUpdates() = runTest {
        val remote = FakeTeamRemoteDataSource()
        val teamMember = sampleTeamMember()
        remote.listTeamMembersResult = Result.success(listOf(teamMember))
        remote.updateRoleResult = Result.success(Unit)
        remote.removeMemberResult = Result.success(Unit)

        val gateway = TeamMembersGatewayImpl(remote)

        assertEquals(listOf(teamMember), gateway.listTeamMembers().getOrNull())
        assertTrue(remote.listMembersCalled)

        val userId = UserId.parse("00000000-0000-0000-0000-000000000010")
        assertTrue(gateway.updateMemberRole(userId, UserRole.Admin).isSuccess)
        assertEquals(UpdateArgs(userId, UserRole.Admin), remote.lastUpdateArgs)

        assertTrue(gateway.removeMember(userId).isSuccess)
        assertEquals(userId, remote.lastRemovedUserId)
    }

    @Test
    fun teamInvitationsGatewayDelegatesInvitationCalls() = runTest {
        val remote = FakeTeamRemoteDataSource()
        val invitation = sampleInvitation()
        remote.createInvitationResult = Result.success(invitation)
        remote.listInvitationsResult = Result.success(listOf(invitation))
        remote.cancelInvitationResult = Result.success(Unit)

        val gateway = TeamInvitationsGatewayImpl(remote)
        val request = CreateInvitationRequest(
            email = Email("team@dokus.test"),
            role = UserRole.Editor
        )

        assertEquals(invitation, gateway.createInvitation(request).getOrNull())
        assertEquals(request, remote.lastInvitationRequest)

        assertEquals(listOf(invitation), gateway.listPendingInvitations().getOrNull())
        assertTrue(remote.listInvitationsCalled)

        val invitationId = InvitationId.parse("00000000-0000-0000-0000-000000000011")
        assertTrue(gateway.cancelInvitation(invitationId).isSuccess)
        assertEquals(invitationId, remote.lastCancelledInvitationId)
    }

    @Test
    fun teamOwnershipGatewayDelegatesTransfer() = runTest {
        val remote = FakeTeamRemoteDataSource()
        remote.transferOwnershipResult = Result.success(Unit)

        val gateway = TeamOwnershipGatewayImpl(remote)
        val newOwnerId = UserId.parse("00000000-0000-0000-0000-000000000012")

        assertTrue(gateway.transferOwnership(newOwnerId).isSuccess)
        assertEquals(newOwnerId, remote.lastTransferredOwnerId)
    }

    @Test
    fun teamBookkeeperAccessGatewayDelegatesSearchGrantListAndRevoke() = runTest {
        val remote = FakeTeamRemoteDataSource()
        val firmId = FirmId.parse("00000000-0000-0000-0000-000000000099")
        val searchItem = BookkeeperFirmSearchItem(
            firmId = firmId,
            name = DisplayName("Kantoor Boonen"),
            vatNumber = VatNumber("BE0777887045"),
            alreadyConnected = false,
        )
        val accessItem = TenantBookkeeperAccessItem(
            firmId = firmId,
            firmName = DisplayName("Kantoor Boonen"),
            vatNumber = VatNumber("BE0777887045"),
            grantedAt = LocalDateTime(2026, 1, 10, 12, 0),
        )
        val grantResponse = GrantBookkeeperAccessResponse(
            firmId = firmId,
            tenantId = TenantId.parse("00000000-0000-0000-0000-000000000003"),
            activated = true,
        )
        remote.searchBookkeeperFirmsResult = Result.success(listOf(searchItem))
        remote.listBookkeeperAccessResult = Result.success(listOf(accessItem))
        remote.grantBookkeeperAccessResult = Result.success(grantResponse)
        remote.revokeBookkeeperAccessResult = Result.success(Unit)

        val gateway = TeamBookkeeperAccessGatewayImpl(remote)

        assertEquals(listOf(searchItem), gateway.searchBookkeeperFirms("kantoor", 10).getOrNull())
        assertEquals(Pair("kantoor", 10), remote.lastBookkeeperSearchArgs)

        assertEquals(listOf(accessItem), gateway.listBookkeeperAccess().getOrNull())
        assertTrue(remote.listBookkeeperAccessCalled)

        assertEquals(grantResponse, gateway.grantBookkeeperAccess(firmId).getOrNull())
        assertEquals(firmId, remote.lastGrantedFirmId)

        assertTrue(gateway.revokeBookkeeperAccess(firmId).isSuccess)
        assertEquals(firmId, remote.lastRevokedFirmId)
    }

    private fun sampleTeamMember(): TeamMember {
        return TeamMember(
            userId = UserId.parse("00000000-0000-0000-0000-000000000001"),
            email = Email("member@dokus.test"),
            firstName = Name("Ada"),
            lastName = Name("Lovelace"),
            role = UserRole.Owner,
            joinedAt = LocalDateTime(2024, 1, 1, 0, 0),
            lastActiveAt = LocalDateTime(2024, 1, 2, 0, 0)
        )
    }

    private fun sampleInvitation(): TenantInvitation {
        return TenantInvitation(
            id = InvitationId.parse("00000000-0000-0000-0000-000000000002"),
            tenantId = TenantId("00000000-0000-0000-0000-000000000003"),
            email = Email("invite@dokus.test"),
            role = UserRole.Admin,
            invitedByName = "Owner",
            status = InvitationStatus.Pending,
            expiresAt = LocalDateTime(2024, 2, 1, 0, 0),
            createdAt = LocalDateTime(2024, 1, 1, 0, 0)
        )
    }

    private data class UpdateArgs(
        val userId: UserId,
        val role: UserRole
    )

    private class FakeTeamRemoteDataSource : TeamRemoteDataSource {
        var listMembersCalled = false
        var listTeamMembersResult: Result<List<TeamMember>> = Result.success(emptyList())

        var lastInvitationRequest: CreateInvitationRequest? = null
        var createInvitationResult: Result<TenantInvitation> = Result.failure(IllegalStateException("missing"))

        var listInvitationsCalled = false
        var listInvitationsResult: Result<List<TenantInvitation>> = Result.success(emptyList())

        var lastCancelledInvitationId: InvitationId? = null
        var cancelInvitationResult: Result<Unit> = Result.success(Unit)

        var lastUpdateArgs: UpdateArgs? = null
        var updateRoleResult: Result<Unit> = Result.success(Unit)

        var lastRemovedUserId: UserId? = null
        var removeMemberResult: Result<Unit> = Result.success(Unit)

        var lastTransferredOwnerId: UserId? = null
        var transferOwnershipResult: Result<Unit> = Result.success(Unit)

        var lastBookkeeperSearchArgs: Pair<String, Int>? = null
        var searchBookkeeperFirmsResult: Result<List<BookkeeperFirmSearchItem>> = Result.success(emptyList())

        var listBookkeeperAccessCalled = false
        var listBookkeeperAccessResult: Result<List<TenantBookkeeperAccessItem>> = Result.success(emptyList())

        var lastGrantedFirmId: FirmId? = null
        var grantBookkeeperAccessResult: Result<GrantBookkeeperAccessResponse> =
            Result.failure(IllegalStateException("missing"))

        var lastRevokedFirmId: FirmId? = null
        var revokeBookkeeperAccessResult: Result<Unit> = Result.success(Unit)

        override suspend fun listTeamMembers(): Result<List<TeamMember>> {
            listMembersCalled = true
            return listTeamMembersResult
        }

        override suspend fun createInvitation(
            request: CreateInvitationRequest
        ): Result<TenantInvitation> {
            lastInvitationRequest = request
            return createInvitationResult
        }

        override suspend fun listPendingInvitations(): Result<List<TenantInvitation>> {
            listInvitationsCalled = true
            return listInvitationsResult
        }

        override suspend fun cancelInvitation(id: InvitationId): Result<Unit> {
            lastCancelledInvitationId = id
            return cancelInvitationResult
        }

        override suspend fun updateMemberRole(userId: UserId, newRole: UserRole): Result<Unit> {
            lastUpdateArgs = UpdateArgs(userId, newRole)
            return updateRoleResult
        }

        override suspend fun removeMember(userId: UserId): Result<Unit> {
            lastRemovedUserId = userId
            return removeMemberResult
        }

        override suspend fun transferOwnership(newOwnerId: UserId): Result<Unit> {
            lastTransferredOwnerId = newOwnerId
            return transferOwnershipResult
        }

        override suspend fun searchBookkeeperFirms(
            query: String,
            limit: Int
        ): Result<List<BookkeeperFirmSearchItem>> {
            lastBookkeeperSearchArgs = query to limit
            return searchBookkeeperFirmsResult
        }

        override suspend fun listBookkeeperAccess(): Result<List<TenantBookkeeperAccessItem>> {
            listBookkeeperAccessCalled = true
            return listBookkeeperAccessResult
        }

        override suspend fun grantBookkeeperAccess(firmId: FirmId): Result<GrantBookkeeperAccessResponse> {
            lastGrantedFirmId = firmId
            return grantBookkeeperAccessResult
        }

        override suspend fun revokeBookkeeperAccess(firmId: FirmId): Result<Unit> {
            lastRevokedFirmId = firmId
            return revokeBookkeeperAccessResult
        }
    }
}
