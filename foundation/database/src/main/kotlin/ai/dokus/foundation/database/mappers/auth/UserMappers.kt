package ai.dokus.foundation.database.mappers.auth

import ai.dokus.foundation.database.tables.auth.TenantMembersTable
import ai.dokus.foundation.database.tables.auth.UsersTable
import tech.dokus.domain.Email
import tech.dokus.domain.Name
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.TenantMembership
import tech.dokus.domain.model.User
import tech.dokus.domain.model.UserInTenant
import org.jetbrains.exposed.v1.core.ResultRow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
object UserMappers {

    /**
     * Maps a ResultRow from UsersTable to User.
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
     * Maps a ResultRow from TenantMembersTable to TenantMembership.
     */
    fun ResultRow.toTenantMembership(): TenantMembership = TenantMembership(
        userId = UserId(this[TenantMembersTable.userId].value.toString()),
        tenantId = TenantId(this[TenantMembersTable.tenantId].value.toKotlinUuid()),
        role = this[TenantMembersTable.role],
        isActive = this[TenantMembersTable.isActive],
        createdAt = this[TenantMembersTable.createdAt],
        updatedAt = this[TenantMembersTable.updatedAt]
    )

    /**
     * Maps joined UsersTable + TenantMembersTable to UserInTenant.
     */
    fun ResultRow.toUserInTenant(): UserInTenant = UserInTenant(
        user = this.toUser(),
        tenantId = TenantId(this[TenantMembersTable.tenantId].value.toKotlinUuid()),
        role = this[TenantMembersTable.role],
        membershipActive = this[TenantMembersTable.isActive]
    )
}
