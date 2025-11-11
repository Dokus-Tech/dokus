package ai.dokus.invoicing.backend.database.repository

import ai.dokus.invoicing.backend.database.mappers.ClientMapper.toClient
import ai.dokus.invoicing.backend.database.tables.ClientsTable
import ai.dokus.foundation.ktor.database.dbQuery
import ai.dokus.foundation.domain.ClientId
import ai.dokus.foundation.domain.TenantId
import ai.dokus.foundation.domain.VatRate
import ai.dokus.foundation.domain.model.Client
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
class ClientRepository {
    private val logger = LoggerFactory.getLogger(ClientRepository::class.java)

    suspend fun create(
        tenantId: TenantId,
        name: String,
        email: String? = null,
        phone: String? = null,
        vatNumber: String? = null,
        addressLine1: String? = null,
        city: String? = null,
        postalCode: String? = null,
        country: String? = "BE",
        companyNumber: String? = null,
        defaultPaymentTerms: Int = 30,
        defaultVatRate: VatRate? = null,
        peppolId: String? = null,
        peppolEnabled: Boolean = false
    ): ClientId = dbQuery {
        val javaUuid = tenantId.value.toJavaUuid()

        val clientId = ClientsTable.insertAndGetId {
            it[ClientsTable.tenantId] = javaUuid
            it[ClientsTable.name] = name
            it[ClientsTable.email] = email
            it[ClientsTable.phone] = phone
            it[ClientsTable.vatNumber] = vatNumber
            it[ClientsTable.addressLine1] = addressLine1
            it[ClientsTable.city] = city
            it[ClientsTable.postalCode] = postalCode
            it[ClientsTable.country] = country
            it[ClientsTable.companyNumber] = companyNumber
            it[ClientsTable.defaultPaymentTerms] = defaultPaymentTerms
            it[ClientsTable.defaultVatRate] = defaultVatRate?.let { rate -> BigDecimal(rate.value) }
            it[ClientsTable.peppolId] = peppolId
            it[ClientsTable.peppolEnabled] = peppolEnabled
        }.value

        logger.info("Created client: $clientId for tenant: $tenantId")
        ClientId(clientId.toKotlinUuid())
    }

    suspend fun findById(id: ClientId, tenantId: TenantId): Client? = dbQuery {
        val javaClientId = id.value.toJavaUuid()
        val javaTenantId = tenantId.value.toJavaUuid()

        ClientsTable
            .selectAll()
            .where {
                (ClientsTable.id eq javaClientId) and
                (ClientsTable.tenantId eq javaTenantId)
            }
            .singleOrNull()
            ?.toClient()
    }

    suspend fun findByTenant(
        tenantId: TenantId,
        search: String? = null,
        isActive: Boolean? = null,
        limit: Int = 100,
        offset: Int = 0
    ): List<Client> = dbQuery {
        val javaTenantId = tenantId.value.toJavaUuid()

        ClientsTable
            .selectAll()
            .where {
                var condition: Op<Boolean> = ClientsTable.tenantId eq javaTenantId

                // Apply search filter
                search?.let { searchTerm ->
                    condition = condition and (
                        (ClientsTable.name like "%$searchTerm%") or
                        (ClientsTable.email like "%$searchTerm%") or
                        (ClientsTable.vatNumber like "%$searchTerm%")
                    )
                }

                // Apply active filter
                isActive?.let { active ->
                    condition = condition and (ClientsTable.isActive eq active)
                }

                condition
            }
            .orderBy(ClientsTable.name)
            .limit(limit)
            .offset(offset.toLong())
            .map { it.toClient() }
    }

    suspend fun update(
        id: ClientId,
        tenantId: TenantId,
        name: String? = null,
        email: String? = null,
        phone: String? = null,
        vatNumber: String? = null,
        addressLine1: String? = null,
        city: String? = null,
        postalCode: String? = null,
        country: String? = null,
        peppolId: String? = null,
        peppolEnabled: Boolean? = null,
        defaultPaymentTerms: Int? = null,
        defaultVatRate: VatRate? = null,
        tags: String? = null,
        notes: String? = null,
        isActive: Boolean? = null
    ): Boolean = dbQuery {
        val javaClientId = id.value.toJavaUuid()
        val javaTenantId = tenantId.value.toJavaUuid()

        val updated = ClientsTable.update({
            (ClientsTable.id eq javaClientId) and
            (ClientsTable.tenantId eq javaTenantId)
        }) {
            name?.let { value -> it[ClientsTable.name] = value }
            email?.let { value -> it[ClientsTable.email] = value }
            phone?.let { value -> it[ClientsTable.phone] = value }
            vatNumber?.let { value -> it[ClientsTable.vatNumber] = value }
            addressLine1?.let { value -> it[ClientsTable.addressLine1] = value }
            city?.let { value -> it[ClientsTable.city] = value }
            postalCode?.let { value -> it[ClientsTable.postalCode] = value }
            country?.let { value -> it[ClientsTable.country] = value }
            peppolId?.let { value -> it[ClientsTable.peppolId] = value }
            peppolEnabled?.let { value -> it[ClientsTable.peppolEnabled] = value }
            defaultPaymentTerms?.let { value -> it[ClientsTable.defaultPaymentTerms] = value }
            defaultVatRate?.let { rate -> it[ClientsTable.defaultVatRate] = BigDecimal(rate.value) }
            tags?.let { value -> it[ClientsTable.tags] = value }
            notes?.let { value -> it[ClientsTable.notes] = value }
            isActive?.let { value -> it[ClientsTable.isActive] = value }
        }

        if (updated > 0) {
            logger.info("Updated client: $id for tenant: $tenantId")
        }

        updated > 0
    }

    suspend fun delete(id: ClientId, tenantId: TenantId): Boolean = dbQuery {
        val javaClientId = id.value.toJavaUuid()
        val javaTenantId = tenantId.value.toJavaUuid()

        // Soft delete by setting isActive = false
        val deleted = ClientsTable.update({
            (ClientsTable.id eq javaClientId) and
            (ClientsTable.tenantId eq javaTenantId)
        }) {
            it[isActive] = false
        }

        if (deleted > 0) {
            logger.info("Deleted (soft) client: $id for tenant: $tenantId")
        }

        deleted > 0
    }

    suspend fun findByPeppolId(peppolId: String, tenantId: TenantId): Client? = dbQuery {
        val javaTenantId = tenantId.value.toJavaUuid()

        ClientsTable
            .selectAll()
            .where {
                (ClientsTable.peppolId eq peppolId) and
                (ClientsTable.tenantId eq javaTenantId)
            }
            .singleOrNull()
            ?.toClient()
    }

    suspend fun countByTenant(tenantId: TenantId, isActive: Boolean? = null): Long = dbQuery {
        val javaTenantId = tenantId.value.toJavaUuid()

        ClientsTable
            .selectAll()
            .where {
                var condition: Op<Boolean> = ClientsTable.tenantId eq javaTenantId

                isActive?.let { active ->
                    condition = condition and (ClientsTable.isActive eq active)
                }

                condition
            }
            .count()
    }
}
