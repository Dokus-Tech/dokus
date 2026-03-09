package tech.dokus.database.repository.banking

import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import tech.dokus.database.tables.banking.BankConnectionsTable
import tech.dokus.domain.enums.BankAccountType
import tech.dokus.domain.enums.BankProvider
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.ids.BankConnectionId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.BankConnectionDto
import tech.dokus.foundation.backend.database.dbQuery
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi

/**
 * Repository for managing bank connections.
 *
 * CRITICAL SECURITY RULES:
 * 1. ALWAYS filter by tenant_id in every query
 * 2. Access tokens must be encrypted before storage
 * 3. Never expose raw access tokens in responses
 */
@OptIn(ExperimentalUuidApi::class)
class BankingRepository {

    suspend fun createConnection(
        tenantId: TenantId,
        provider: BankProvider,
        institutionId: String,
        institutionName: String,
        accountId: String,
        accountName: String?,
        accountType: BankAccountType?,
        currency: Currency,
        encryptedAccessToken: String
    ): Result<BankConnectionDto> = runCatching {
        dbQuery {
            val id = BankConnectionsTable.insert {
                it[BankConnectionsTable.tenantId] = UUID.fromString(tenantId.toString())
                it[BankConnectionsTable.provider] = provider
                it[BankConnectionsTable.institutionId] = institutionId
                it[BankConnectionsTable.institutionName] = institutionName
                it[BankConnectionsTable.accountId] = accountId
                it[BankConnectionsTable.accountName] = accountName
                it[BankConnectionsTable.accountType] = accountType
                it[BankConnectionsTable.currency] = currency
                it[BankConnectionsTable.accessToken] = encryptedAccessToken
            } get BankConnectionsTable.id

            BankConnectionsTable.selectAll().where {
                BankConnectionsTable.id eq id.value
            }.single().toBankConnectionDto()
        }
    }

    suspend fun getConnection(
        connectionId: BankConnectionId,
        tenantId: TenantId
    ): Result<BankConnectionDto?> = runCatching {
        dbQuery {
            BankConnectionsTable.selectAll().where {
                (BankConnectionsTable.id eq UUID.fromString(connectionId.toString())) and
                    (BankConnectionsTable.tenantId eq UUID.fromString(tenantId.toString()))
            }.singleOrNull()?.toBankConnectionDto()
        }
    }

    suspend fun listConnections(
        tenantId: TenantId,
        activeOnly: Boolean = true
    ): Result<List<BankConnectionDto>> = runCatching {
        dbQuery {
            var query = BankConnectionsTable.selectAll().where {
                BankConnectionsTable.tenantId eq UUID.fromString(tenantId.toString())
            }
            if (activeOnly) {
                query = query.andWhere { BankConnectionsTable.isActive eq true }
            }
            query.orderBy(BankConnectionsTable.createdAt, SortOrder.DESC)
                .map { it.toBankConnectionDto() }
        }
    }

    suspend fun updateLastSyncedAt(
        connectionId: BankConnectionId,
        tenantId: TenantId,
        syncedAt: LocalDateTime
    ): Result<Boolean> = runCatching {
        dbQuery {
            BankConnectionsTable.update({
                (BankConnectionsTable.id eq UUID.fromString(connectionId.toString())) and
                    (BankConnectionsTable.tenantId eq UUID.fromString(tenantId.toString()))
            }) {
                it[lastSyncedAt] = syncedAt
                it[updatedAt] = syncedAt
            } > 0
        }
    }

    suspend fun deactivateConnection(
        connectionId: BankConnectionId,
        tenantId: TenantId
    ): Result<Boolean> = runCatching {
        dbQuery {
            BankConnectionsTable.update({
                (BankConnectionsTable.id eq UUID.fromString(connectionId.toString())) and
                    (BankConnectionsTable.tenantId eq UUID.fromString(tenantId.toString()))
            }) {
                it[isActive] = false
            } > 0
        }
    }

    private fun ResultRow.toBankConnectionDto(): BankConnectionDto {
        return BankConnectionDto(
            id = BankConnectionId.parse(this[BankConnectionsTable.id].value.toString()),
            tenantId = TenantId.parse(this[BankConnectionsTable.tenantId].toString()),
            provider = this[BankConnectionsTable.provider],
            institutionId = this[BankConnectionsTable.institutionId],
            institutionName = this[BankConnectionsTable.institutionName],
            accountId = this[BankConnectionsTable.accountId],
            accountName = this[BankConnectionsTable.accountName],
            accountType = this[BankConnectionsTable.accountType],
            currency = this[BankConnectionsTable.currency],
            lastSyncedAt = this[BankConnectionsTable.lastSyncedAt],
            isActive = this[BankConnectionsTable.isActive],
            createdAt = this[BankConnectionsTable.createdAt],
            updatedAt = this[BankConnectionsTable.updatedAt]
        )
    }
}
