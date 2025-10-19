package ai.dokus.foundation.database.services

import ai.dokus.foundation.database.mappers.ClientMapper.toClient
import ai.dokus.foundation.database.tables.ClientsTable
import ai.dokus.foundation.database.utils.dbQuery
import ai.dokus.foundation.domain.ClientId
import ai.dokus.foundation.domain.TenantId
import ai.dokus.foundation.domain.VatNumber
import ai.dokus.foundation.domain.enums.AuditAction
import ai.dokus.foundation.domain.enums.EntityType
import ai.dokus.foundation.domain.model.Client
import ai.dokus.foundation.ktor.services.ClientService
import org.jetbrains.exposed.sql.*
import org.slf4j.LoggerFactory
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
class ClientServiceImpl(
    private val auditService: AuditServiceImpl
) : ClientService {
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

        val client = ClientsTable.selectAll()
            .where { ClientsTable.id eq clientId }
            .single()
            .toClient()

        // Audit log
        auditService.logAction(
            tenantId = tenantId,
            userId = null, // TODO: Get from authenticated context
            action = AuditAction.ClientCreated,
            entityType = EntityType.Client,
            entityId = client.id.value,
            oldValues = null,
            newValues = mapOf(
                "name" to name,
                "email" to email,
                "vatNumber" to vatNumber?.value,
                "city" to city,
                "country" to country
            )
        )

        client
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
    ) {
        val (tenantId, oldValues, newValues) = dbQuery {
            val javaUuid = clientId.value.toJavaUuid()

            // Get existing client data
            val client = ClientsTable.selectAll().where { ClientsTable.id eq javaUuid }.singleOrNull()
                ?: throw IllegalArgumentException("Client not found: $clientId")

            // Capture old/new values
            val oldVals = mutableMapOf<String, Any?>()
            val newVals = mutableMapOf<String, Any?>()

            if (name != null) {
                oldVals["name"] = client[ClientsTable.name]
                newVals["name"] = name
            }
            if (email != null) {
                oldVals["email"] = client[ClientsTable.email]
                newVals["email"] = email
            }
            if (vatNumber != null) {
                oldVals["vatNumber"] = client[ClientsTable.vatNumber]
                newVals["vatNumber"] = vatNumber.value
            }
            if (city != null) {
                oldVals["city"] = client[ClientsTable.city]
                newVals["city"] = city
            }
            if (country != null) {
                oldVals["country"] = client[ClientsTable.country]
                newVals["country"] = country
            }

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

            Triple(TenantId(client[ClientsTable.tenantId].value.toKotlinUuid()), oldVals, newVals)
        }

        logger.info("Updated client $clientId")

        // Audit log
        auditService.logAction(
            tenantId = tenantId,
            userId = null, // TODO: Get from authenticated context
            action = AuditAction.ClientUpdated,
            entityType = EntityType.Client,
            entityId = clientId.value,
            oldValues = oldValues,
            newValues = newValues
        )
    }

    override suspend fun delete(clientId: ClientId) {
        val (tenantId, clientName) = dbQuery {
            val javaUuid = clientId.value.toJavaUuid()

            // Get client info before deletion
            val client = ClientsTable.selectAll().where { ClientsTable.id eq javaUuid }.singleOrNull()
                ?: throw IllegalArgumentException("Client not found: $clientId")

            val updated = ClientsTable.update({ ClientsTable.id eq javaUuid }) {
                it[isActive] = false
            }

            if (updated == 0) {
                throw IllegalArgumentException("Client not found: $clientId")
            }

            Pair(TenantId(client[ClientsTable.tenantId].value.toKotlinUuid()), client[ClientsTable.name])
        }

        logger.info("Soft deleted client $clientId")

        // Audit log
        auditService.logAction(
            tenantId = tenantId,
            userId = null, // TODO: Get from authenticated context
            action = AuditAction.ClientDeleted,
            entityType = EntityType.Client,
            entityId = clientId.value,
            oldValues = mapOf("name" to clientName, "isActive" to true),
            newValues = mapOf("isActive" to false)
        )
    }

    override suspend fun reactivate(clientId: ClientId) {
        val (tenantId, clientName) = dbQuery {
            val javaUuid = clientId.value.toJavaUuid()

            // Get client info before reactivation
            val client = ClientsTable.selectAll().where { ClientsTable.id eq javaUuid }.singleOrNull()
                ?: throw IllegalArgumentException("Client not found: $clientId")

            val updated = ClientsTable.update({ ClientsTable.id eq javaUuid }) {
                it[isActive] = true
            }

            if (updated == 0) {
                throw IllegalArgumentException("Client not found: $clientId")
            }

            Pair(TenantId(client[ClientsTable.tenantId].value.toKotlinUuid()), client[ClientsTable.name])
        }

        logger.info("Reactivated client $clientId")

        // Audit log
        auditService.logAction(
            tenantId = tenantId,
            userId = null, // TODO: Get from authenticated context
            action = AuditAction.ClientUpdated,
            entityType = EntityType.Client,
            entityId = clientId.value,
            oldValues = mapOf("name" to clientName, "isActive" to false),
            newValues = mapOf("isActive" to true)
        )
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
