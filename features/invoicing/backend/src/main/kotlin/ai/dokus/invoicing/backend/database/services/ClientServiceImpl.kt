@file:OptIn(kotlin.time.ExperimentalTime::class)

package ai.dokus.invoicing.backend.database.services

import ai.dokus.foundation.domain.ClientId
import ai.dokus.foundation.domain.TenantId
import ai.dokus.foundation.domain.VatNumber
import ai.dokus.foundation.domain.model.Client
import ai.dokus.foundation.ktor.services.ClientService
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class ClientServiceImpl(
    private val auditService: ai.dokus.foundation.ktor.services.AuditService
) : ClientService {

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
    ): Client {
        TODO("Not yet implemented")
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
        TODO("Not yet implemented")
    }

    override suspend fun delete(clientId: ClientId) {
        TODO("Not yet implemented")
    }

    override suspend fun reactivate(clientId: ClientId) {
        TODO("Not yet implemented")
    }

    override suspend fun findById(id: ClientId): Client? {
        TODO("Not yet implemented")
    }

    override suspend fun listByTenant(tenantId: TenantId, activeOnly: Boolean): List<Client> {
        TODO("Not yet implemented")
    }

    override suspend fun search(tenantId: TenantId, query: String, activeOnly: Boolean): List<Client> {
        TODO("Not yet implemented")
    }

    override suspend fun findByEmail(tenantId: TenantId, email: String): Client? {
        TODO("Not yet implemented")
    }

    override suspend fun findByVatNumber(tenantId: TenantId, vatNumber: VatNumber): Client? {
        TODO("Not yet implemented")
    }
}
