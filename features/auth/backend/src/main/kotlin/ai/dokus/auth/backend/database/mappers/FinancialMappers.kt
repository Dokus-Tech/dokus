package ai.dokus.auth.backend.database.mappers

import ai.dokus.auth.backend.database.tables.UsersTable
import ai.dokus.foundation.domain.*
import ai.dokus.foundation.domain.model.BusinessUser
import org.jetbrains.exposed.sql.ResultRow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
object FinancialMappers {

    fun ResultRow.toBusinessUser(): BusinessUser = BusinessUser(
        id = BusinessUserId(this[UsersTable.id].value.toKotlinUuid()),
        tenantId = TenantId(this[UsersTable.tenantId].value.toKotlinUuid()),
        email = Email(this[UsersTable.email]),
        role = this[UsersTable.role],
        firstName = this[UsersTable.firstName],
        lastName = this[UsersTable.lastName],
        isActive = this[UsersTable.isActive],
        lastLoginAt = this[UsersTable.lastLoginAt],
        createdAt = this[UsersTable.createdAt],
        updatedAt = this[UsersTable.updatedAt]
    )
}
