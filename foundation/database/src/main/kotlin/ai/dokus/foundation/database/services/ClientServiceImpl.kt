package ai.dokus.foundation.database.services

import ai.dokus.foundation.database.mappers.ClientMapper.toClient
import ai.dokus.foundation.database.tables.ClientsTable
import ai.dokus.foundation.database.utils.dbQuery
import ai.dokus.foundation.domain.ClientId
import ai.dokus.foundation.domain.TenantId
import ai.dokus.foundation.domain.VatNumber
import ai.dokus.foundation.domain.model.Client
import ai.dokus.foundation.ktor.services.ClientService
import org.jetbrains.exposed.sql.*
import org.slf4j.LoggerFactory
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid

@OptIn(ExperimentalUuidApi::class)
class ClientServiceImpl : ClientService {
    private val logger = LoggerFactory.getLogger(ClientServiceImpl::class.java)

    override suspend fun create(
        tenantId: TenantId,
        name: String,
        email: String?,
        vatNumber: VatNumber?,
        addressLine1: String?,
        addressLine2: String?,
        city: String?,
        postalCode: String?,
        country: String?,
        contactPerson: String?,
        phone: String?,
        notes: String?
    ): Client = dbQuery {
        val clientId = ClientsTable.insertAndGetId {
            it[ClientsTable.tenantId] = tenantId.value.toJavaUuid()
            it[ClientsTable.name] = name
            it[ClientsTable.email] = email
            it[ClientsTable.vatNumber] = vatNumber?.value
            it[ClientsTable.addressLine1] = addressLine1
            it[ClientsTable.addressLine2] = addressLine2
            it[ClientsTable.city] = city
            it[ClientsTable.postalCode] = postalCode
            it[ClientsTable.country] = country
            it[ClientsTable.contactPerson] = contactPerson
            it[ClientsTable.phone] = phone
            it[ClientsTable.notes] = notes
            it[isActive] = true
        }.value

        logger.info("Created client $clientId for tenant $tenantId: $name")

        ClientsTable.selectAll()
            .where { ClientsTable.id eq clientId }
            .single()
            .toClient()
    }

    override suspend fun update(
        clientId: ClientId,
        name: String?,
        email: String?,
        vatNumber: VatNumber?,
        addressLine1: String?,
        addressLine2: String?,
        city: String?,
        postalCode: String?,
        country: String?,
        contactPerson: String?,
        phone: String?,
        notes: String?
    ) = dbQuery {
        val javaUuid = clientId.value.toJavaUuid()
        val updated = ClientsTable.update({ ClientsTable.id eq javaUuid }) {
            if (name != null) it[ClientsTable.name] = name
            if (email != null) it[ClientsTable.email] = email
            if (vatNumber != null) it[ClientsTable.vatNumber] = vatNumber.value
            if (addressLine1 != null) it[ClientsTable.addressLine1] = addressLine1
            if (addressLine2 != null) it[ClientsTable.addressLine2] = addressLine2
            if (city != null) it[ClientsTable.city] = city
            if (postalCode != null) it[ClientsTable.postalCode] = postalCode
            if (country != null) it[ClientsTable.country] = country
            if (contactPerson != null) it[ClientsTable.contactPerson] = contactPerson
            if (phone != null) it[ClientsTable.phone] = phone
            if (notes != null) it[ClientsTable.notes] = notes
        }

        if (updated == 0) {
            throw IllegalArgumentException("Client not found: $clientId")
        }

        logger.info("Updated client $clientId")
    }

    override suspend fun delete(clientId: ClientId) = dbQuery {
        val javaUuid = clientId.value.toJavaUuid()
        val updated = ClientsTable.update({ ClientsTable.id eq javaUuid }) {
            it[isActive] = false
        }

        if (updated == 0) {
            throw IllegalArgumentException("Client not found: $clientId")
        }

        logger.info("Soft deleted client $clientId")
    }

    override suspend fun reactivate(clientId: ClientId) = dbQuery {
        val javaUuid = clientId.value.toJavaUuid()
        val updated = ClientsTable.update({ ClientsTable.id eq javaUuid }) {
            it[isActive] = true
        }

        if (updated == 0) {
            throw IllegalArgumentException("Client not found: $clientId")
        }

        logger.info("Reactivated client $clientId")
    }

    override suspend fun findById(id: ClientId): Client? = dbQuery {
        val javaUuid = id.value.toJavaUuid()
        ClientsTable.selectAll()
            .where { ClientsTable.id eq javaUuid }
            .singleOrNull()
            ?.toClient()
    }

    override suspend fun listByTenant(tenantId: TenantId, activeOnly: Boolean): List<Client> = dbQuery {
        val javaUuid = tenantId.value.toJavaUuid()
        val query = ClientsTable.selectAll().where { ClientsTable.tenantId eq javaUuid }

        val finalQuery = if (activeOnly) {
            query.andWhere { ClientsTable.isActive eq true }
        } else {
            query
        }

        finalQuery.map { it.toClient() }
    }

    override suspend fun search(tenantId: TenantId, query: String, activeOnly: Boolean): List<Client> = dbQuery {
        val javaUuid = tenantId.value.toJavaUuid()
        val searchQuery = ClientsTable.selectAll()
            .where { ClientsTable.tenantId eq javaUuid }
            .andWhere { ClientsTable.name.lowerCase() like "%${query.lowercase()}%" }

        val finalQuery = if (activeOnly) {
            searchQuery.andWhere { ClientsTable.isActive eq true }
        } else {
            searchQuery
        }

        finalQuery.map { it.toClient() }
    }

    override suspend fun findByEmail(tenantId: TenantId, email: String): Client? = dbQuery {
        val javaUuid = tenantId.value.toJavaUuid()
        ClientsTable.selectAll()
            .where { ClientsTable.tenantId eq javaUuid }
            .andWhere { ClientsTable.email eq email }
            .singleOrNull()
            ?.toClient()
    }

    override suspend fun findByVatNumber(tenantId: TenantId, vatNumber: VatNumber): Client? = dbQuery {
        val javaUuid = tenantId.value.toJavaUuid()
        ClientsTable.selectAll()
            .where { ClientsTable.tenantId eq javaUuid }
            .andWhere { ClientsTable.vatNumber eq vatNumber.value }
            .singleOrNull()
            ?.toClient()
    }
}
