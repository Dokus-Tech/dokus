package ai.dokus.auth.backend.database.mappers

import ai.dokus.auth.backend.database.tables.UsersTable
import ai.dokus.foundation.domain.Email
import ai.dokus.foundation.domain.ids.OrganizationId
import ai.dokus.foundation.domain.ids.UserId
import ai.dokus.foundation.domain.model.BusinessUser
import org.jetbrains.exposed.v1.core.ResultRow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
object FinancialMappers {

    fun ResultRow.toBusinessUser(): BusinessUser = BusinessUser(
        id = UserId(this[UsersTable.id].value.toString()),
        organizationId = OrganizationId(this[UsersTable.organizationId].value.toKotlinUuid()),
        email = Email(this[UsersTable.email]),
        role = this[UsersTable.role],
        firstName = this[UsersTable.firstName],
        lastName = this[UsersTable.lastName],
        emailVerified = this[UsersTable.emailVerified],
        isActive = this[UsersTable.isActive],
        lastLoginAt = this[UsersTable.lastLoginAt],
        createdAt = this[UsersTable.createdAt],
        updatedAt = this[UsersTable.updatedAt]
    )
}
