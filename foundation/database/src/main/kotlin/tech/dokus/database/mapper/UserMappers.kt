package tech.dokus.database.mapper

import org.jetbrains.exposed.v1.core.ResultRow
import tech.dokus.database.tables.auth.TenantMembersTable
import tech.dokus.database.tables.auth.UsersTable
import tech.dokus.domain.Email
import tech.dokus.domain.Name
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.TenantMembership
import tech.dokus.domain.model.User
import tech.dokus.domain.model.UserInTenant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
fun User.Companion.from(row: ResultRow): User = User(
    id = UserId(row[UsersTable.id].value.toString()),
    email = Email(row[UsersTable.email]),
    firstName = Name(row[UsersTable.firstName]),
    lastName = Name(row[UsersTable.lastName]),
    emailVerified = row[UsersTable.emailVerified],
    isActive = row[UsersTable.isActive],
    lastLoginAt = row[UsersTable.lastLoginAt],
    createdAt = row[UsersTable.createdAt],
    updatedAt = row[UsersTable.updatedAt]
)

@OptIn(ExperimentalUuidApi::class)
fun TenantMembership.Companion.from(row: ResultRow): TenantMembership = TenantMembership(
    userId = UserId(row[TenantMembersTable.userId].value.toString()),
    tenantId = TenantId(row[TenantMembersTable.tenantId].value.toKotlinUuid()),
    role = row[TenantMembersTable.role],
    isActive = row[TenantMembersTable.isActive],
    createdAt = row[TenantMembersTable.createdAt],
    updatedAt = row[TenantMembersTable.updatedAt]
)

@OptIn(ExperimentalUuidApi::class)
fun UserInTenant.Companion.from(row: ResultRow): UserInTenant = UserInTenant(
    user = User.from(row),
    tenantId = TenantId(row[TenantMembersTable.tenantId].value.toKotlinUuid()),
    role = row[TenantMembersTable.role],
    membershipActive = row[TenantMembersTable.isActive]
)
