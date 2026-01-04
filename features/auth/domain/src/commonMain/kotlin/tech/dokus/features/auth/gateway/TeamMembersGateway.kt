package tech.dokus.features.auth.gateway

import tech.dokus.domain.enums.UserRole
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.TeamMember

/**
 * Gateway for team member listing and role management.
 */
interface TeamMembersGateway {
    suspend fun listTeamMembers(): Result<List<TeamMember>>

    suspend fun updateMemberRole(userId: UserId, newRole: UserRole): Result<Unit>

    suspend fun removeMember(userId: UserId): Result<Unit>
}
