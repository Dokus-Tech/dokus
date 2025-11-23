package ai.dokus.auth.backend.database.mappers

import ai.dokus.auth.backend.database.tables.OrganizationMembersTable
import ai.dokus.auth.backend.database.tables.UsersTable
import ai.dokus.foundation.domain.Email
import ai.dokus.foundation.domain.Name
import ai.dokus.foundation.domain.ids.OrganizationId
import ai.dokus.foundation.domain.ids.UserId
import ai.dokus.foundation.domain.model.OrganizationMembership
import ai.dokus.foundation.domain.model.User
import ai.dokus.foundation.domain.model.UserInOrganization
import org.jetbrains.exposed.v1.core.ResultRow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
object FinancialMappers {

    /**
     * Maps a ResultRow from UsersTable to BusinessUser (user identity only).
     */
    fun ResultRow.toUser(): User = User(
        id = UserId(this[UsersTable.id].value.toString()),
        email = Email(this[UsersTable.email]),
        firstName = Name(this[UsersTable.firstName]),
        lastName = Name(this[UsersTable.lastName]),
        emailVerified = this[UsersTable.emailVerified],
        isActive = this[UsersTable.isActive],
        lastLoginAt = this[UsersTable.lastLoginAt],
        createdAt = this[UsersTable.createdAt],
        updatedAt = this[UsersTable.updatedAt]
    )

    /**
     * Maps a ResultRow from OrganizationMembersTable to OrganizationMembership.
     */
    fun ResultRow.toOrganizationMembership(): OrganizationMembership = OrganizationMembership(
        userId = UserId(this[OrganizationMembersTable.userId].value.toString()),
        organizationId = OrganizationId(this[OrganizationMembersTable.organizationId].value.toKotlinUuid()),
        role = this[OrganizationMembersTable.role],
        isActive = this[OrganizationMembersTable.isActive],
        createdAt = this[OrganizationMembersTable.createdAt],
        updatedAt = this[OrganizationMembersTable.updatedAt]
    )

    /**
     * Maps joined UsersTable + OrganizationMembersTable to UserInOrganization.
     */
    fun ResultRow.toUserInOrganization(): UserInOrganization = UserInOrganization(
        user = this.toUser(),
        organizationId = OrganizationId(this[OrganizationMembersTable.organizationId].value.toKotlinUuid()),
        role = this[OrganizationMembersTable.role],
        membershipActive = this[OrganizationMembersTable.isActive]
    )
}
