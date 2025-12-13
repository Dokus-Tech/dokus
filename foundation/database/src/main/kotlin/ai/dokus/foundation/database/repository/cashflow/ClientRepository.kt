package ai.dokus.foundation.database.repository.cashflow

import ai.dokus.foundation.database.tables.cashflow.ClientsTable
import ai.dokus.foundation.domain.Email
import ai.dokus.foundation.domain.Name
import ai.dokus.foundation.domain.VatRate
import ai.dokus.foundation.domain.ids.ClientId
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.ids.VatNumber
import ai.dokus.foundation.domain.model.ClientDto
import ai.dokus.foundation.domain.model.ClientStats
import ai.dokus.foundation.domain.model.CreateClientRequest
import ai.dokus.foundation.domain.model.PaginatedResponse
import ai.dokus.foundation.domain.model.UpdateClientRequest
import ai.dokus.foundation.ktor.database.dbQuery
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.util.UUID

/**
 * Repository for managing clients/customers
 *
 * CRITICAL SECURITY RULES:
 * 1. ALWAYS filter by tenant_id in every query
 * 2. NEVER return clients from different tenants
 * 3. All operations must be tenant-isolated
 */
class ClientRepository {

    /**
     * Create a new client
     * CRITICAL: MUST include tenant_id for multi-tenancy security
     */
    suspend fun createClient(
        tenantId: TenantId,
        request: CreateClientRequest
    ): Result<ClientDto> = runCatching {
        dbQuery {
            val clientId = ClientsTable.insertAndGetId {
                it[ClientsTable.tenantId] = UUID.fromString(tenantId.toString())
                it[name] = request.name
                it[email] = request.email
                it[phone] = request.phone
                it[vatNumber] = request.vatNumber
                it[addressLine1] = request.addressLine1
                it[addressLine2] = request.addressLine2
                it[city] = request.city
                it[postalCode] = request.postalCode
                it[country] = request.country
                it[contactPerson] = request.contactPerson
                it[notes] = request.notes
            }

            // Fetch and return the created client
            ClientsTable.selectAll().where {
                (ClientsTable.id eq clientId.value) and
                (ClientsTable.tenantId eq UUID.fromString(tenantId.toString()))
            }.single().let { row ->
                mapRowToClientDto(row)
            }
        }
    }

    /**
     * Get a single client by ID
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun getClient(
        clientId: ClientId,
        tenantId: TenantId
    ): Result<ClientDto?> = runCatching {
        dbQuery {
            ClientsTable.selectAll().where {
                (ClientsTable.id eq UUID.fromString(clientId.toString())) and
                (ClientsTable.tenantId eq UUID.fromString(tenantId.toString()))
            }.singleOrNull()?.let { row ->
                mapRowToClientDto(row)
            }
        }
    }

    /**
     * List clients for a tenant with optional filters
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun listClients(
        tenantId: TenantId,
        isActive: Boolean? = null,
        peppolEnabled: Boolean? = null,
        searchQuery: String? = null,
        limit: Int = 50,
        offset: Int = 0
    ): Result<PaginatedResponse<ClientDto>> = runCatching {
        dbQuery {
            var query = ClientsTable.selectAll().where {
                ClientsTable.tenantId eq UUID.fromString(tenantId.toString())
            }

            // Apply filters
            if (isActive != null) {
                query = query.andWhere { ClientsTable.isActive eq isActive }
            }
            if (peppolEnabled != null) {
                query = query.andWhere { ClientsTable.peppolEnabled eq peppolEnabled }
            }
            if (!searchQuery.isNullOrBlank()) {
                query = query.andWhere {
                    (ClientsTable.name like "%$searchQuery%") or
                    (ClientsTable.email like "%$searchQuery%") or
                    (ClientsTable.vatNumber like "%$searchQuery%")
                }
            }

            val total = query.count()

            val items = query.orderBy(ClientsTable.name to SortOrder.ASC)
                .limit(limit + offset)
                .map { row -> mapRowToClientDto(row) }
                .drop(offset)

            PaginatedResponse(
                items = items,
                total = total,
                limit = limit,
                offset = offset
            )
        }
    }

    /**
     * Update a client
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun updateClient(
        clientId: ClientId,
        tenantId: TenantId,
        request: UpdateClientRequest
    ): Result<ClientDto> = runCatching {
        dbQuery {
            // Verify client exists and belongs to tenant
            val exists = ClientsTable.selectAll().where {
                (ClientsTable.id eq UUID.fromString(clientId.toString())) and
                (ClientsTable.tenantId eq UUID.fromString(tenantId.toString()))
            }.count() > 0

            if (!exists) {
                throw IllegalArgumentException("Client not found or access denied")
            }

            // Update client (only non-null fields)
            ClientsTable.update({
                (ClientsTable.id eq UUID.fromString(clientId.toString())) and
                (ClientsTable.tenantId eq UUID.fromString(tenantId.toString()))
            }) {
                request.name?.let { value -> it[name] = value }
                request.email?.let { value -> it[email] = value }
                request.phone?.let { value -> it[phone] = value }
                request.vatNumber?.let { value -> it[vatNumber] = value }
                request.addressLine1?.let { value -> it[addressLine1] = value }
                request.addressLine2?.let { value -> it[addressLine2] = value }
                request.city?.let { value -> it[city] = value }
                request.postalCode?.let { value -> it[postalCode] = value }
                request.country?.let { value -> it[country] = value }
                request.contactPerson?.let { value -> it[contactPerson] = value }
                request.notes?.let { value -> it[notes] = value }
            }

            // Fetch and return the updated client
            ClientsTable.selectAll().where {
                (ClientsTable.id eq UUID.fromString(clientId.toString())) and
                (ClientsTable.tenantId eq UUID.fromString(tenantId.toString()))
            }.single().let { row ->
                mapRowToClientDto(row)
            }
        }
    }

    /**
     * Update client's Peppol settings
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun updateClientPeppol(
        clientId: ClientId,
        tenantId: TenantId,
        peppolId: String?,
        peppolEnabled: Boolean
    ): Result<ClientDto> = runCatching {
        dbQuery {
            // Verify client exists and belongs to tenant
            val exists = ClientsTable.selectAll().where {
                (ClientsTable.id eq UUID.fromString(clientId.toString())) and
                (ClientsTable.tenantId eq UUID.fromString(tenantId.toString()))
            }.count() > 0

            if (!exists) {
                throw IllegalArgumentException("Client not found or access denied")
            }

            ClientsTable.update({
                (ClientsTable.id eq UUID.fromString(clientId.toString())) and
                (ClientsTable.tenantId eq UUID.fromString(tenantId.toString()))
            }) {
                it[ClientsTable.peppolId] = peppolId
                it[ClientsTable.peppolEnabled] = peppolEnabled
            }

            // Fetch and return the updated client
            ClientsTable.selectAll().where {
                (ClientsTable.id eq UUID.fromString(clientId.toString())) and
                (ClientsTable.tenantId eq UUID.fromString(tenantId.toString()))
            }.single().let { row ->
                mapRowToClientDto(row)
            }
        }
    }

    /**
     * Delete a client
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun deleteClient(
        clientId: ClientId,
        tenantId: TenantId
    ): Result<Boolean> = runCatching {
        dbQuery {
            val deletedRows = ClientsTable.deleteWhere {
                (ClientsTable.id eq UUID.fromString(clientId.toString())) and
                (ClientsTable.tenantId eq UUID.fromString(tenantId.toString()))
            }
            deletedRows > 0
        }
    }

    /**
     * Soft-delete (deactivate) a client
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun deactivateClient(
        clientId: ClientId,
        tenantId: TenantId
    ): Result<Boolean> = runCatching {
        dbQuery {
            val updatedRows = ClientsTable.update({
                (ClientsTable.id eq UUID.fromString(clientId.toString())) and
                (ClientsTable.tenantId eq UUID.fromString(tenantId.toString()))
            }) {
                it[isActive] = false
            }
            updatedRows > 0
        }
    }

    /**
     * Reactivate a client
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun reactivateClient(
        clientId: ClientId,
        tenantId: TenantId
    ): Result<Boolean> = runCatching {
        dbQuery {
            val updatedRows = ClientsTable.update({
                (ClientsTable.id eq UUID.fromString(clientId.toString())) and
                (ClientsTable.tenantId eq UUID.fromString(tenantId.toString()))
            }) {
                it[isActive] = true
            }
            updatedRows > 0
        }
    }

    /**
     * Check if a client exists and belongs to the tenant
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun exists(
        clientId: ClientId,
        tenantId: TenantId
    ): Result<Boolean> = runCatching {
        dbQuery {
            ClientsTable.selectAll().where {
                (ClientsTable.id eq UUID.fromString(clientId.toString())) and
                (ClientsTable.tenantId eq UUID.fromString(tenantId.toString()))
            }.count() > 0
        }
    }

    /**
     * Get client statistics for dashboard
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun getClientStats(tenantId: TenantId): Result<ClientStats> = runCatching {
        dbQuery {
            val allClients = ClientsTable.selectAll().where {
                ClientsTable.tenantId eq UUID.fromString(tenantId.toString())
            }

            val totalClients = allClients.count()
            val activeClients = allClients.copy().andWhere { ClientsTable.isActive eq true }.count()
            val inactiveClients = totalClients - activeClients
            val peppolEnabledClients = allClients.copy().andWhere { ClientsTable.peppolEnabled eq true }.count()

            ClientStats(
                totalClients = totalClients,
                activeClients = activeClients,
                inactiveClients = inactiveClients,
                peppolEnabledClients = peppolEnabledClients
            )
        }
    }

    /**
     * Get Peppol-enabled clients for a tenant
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun listPeppolEnabledClients(tenantId: TenantId): Result<List<ClientDto>> = runCatching {
        dbQuery {
            ClientsTable.selectAll().where {
                (ClientsTable.tenantId eq UUID.fromString(tenantId.toString())) and
                (ClientsTable.peppolEnabled eq true) and
                (ClientsTable.peppolId.isNotNull())
            }.orderBy(ClientsTable.name to SortOrder.ASC)
                .map { row -> mapRowToClientDto(row) }
        }
    }

    /**
     * Map a database row to ClientDto
     */
    private fun mapRowToClientDto(row: org.jetbrains.exposed.v1.core.ResultRow): ClientDto {
        return ClientDto(
            id = ClientId.parse(row[ClientsTable.id].value.toString()),
            tenantId = TenantId.parse(row[ClientsTable.tenantId].toString()),
            name = Name(row[ClientsTable.name]),
            email = row[ClientsTable.email]?.let { Email(it) },
            vatNumber = row[ClientsTable.vatNumber]?.let { VatNumber(it) },
            addressLine1 = row[ClientsTable.addressLine1],
            addressLine2 = row[ClientsTable.addressLine2],
            city = row[ClientsTable.city],
            postalCode = row[ClientsTable.postalCode],
            country = row[ClientsTable.country],
            contactPerson = row[ClientsTable.contactPerson],
            phone = row[ClientsTable.phone],
            companyNumber = row[ClientsTable.companyNumber],
            defaultPaymentTerms = row[ClientsTable.defaultPaymentTerms],
            defaultVatRate = row[ClientsTable.defaultVatRate]?.let { VatRate(it.toString()) },
            peppolId = row[ClientsTable.peppolId],
            peppolEnabled = row[ClientsTable.peppolEnabled],
            tags = row[ClientsTable.tags],
            notes = row[ClientsTable.notes],
            isActive = row[ClientsTable.isActive],
            createdAt = row[ClientsTable.createdAt],
            updatedAt = row[ClientsTable.updatedAt]
        )
    }
}
