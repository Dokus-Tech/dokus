package ai.dokus.auth.backend.services

import ai.dokus.foundation.apispec.ClientApi
import ai.dokus.foundation.domain.ClientId
import ai.dokus.foundation.domain.TenantId
import ai.dokus.foundation.domain.VatNumber
import ai.dokus.foundation.domain.model.Client
import ai.dokus.foundation.ktor.services.ClientService

class ClientApiImpl(
    private val clientService: ClientService
) : ClientApi {

    override suspend fun createClient(
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
    ): Result<Client> = runCatching {
        clientService.create(
            tenantId, name, email, vatNumber, addressLine1, addressLine2,
            city, postalCode, country, contactPerson, phone, notes
        )
    }

    override suspend fun getClient(id: ClientId): Result<Client> = runCatching {
        clientService.findById(id) ?: throw IllegalArgumentException("Client not found: $id")
    }

    override suspend fun listClients(tenantId: TenantId, activeOnly: Boolean): Result<List<Client>> = runCatching {
        clientService.listByTenant(tenantId, activeOnly)
    }

    override suspend fun searchClients(tenantId: TenantId, query: String): Result<List<Client>> = runCatching {
        clientService.search(tenantId, query)
    }

    override suspend fun updateClient(
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
    ): Result<Unit> = runCatching {
        clientService.update(
            clientId, name, email, vatNumber, addressLine1, addressLine2,
            city, postalCode, country, contactPerson, phone, notes
        )
    }

    override suspend fun deleteClient(clientId: ClientId): Result<Unit> = runCatching {
        clientService.delete(clientId)
    }
}
