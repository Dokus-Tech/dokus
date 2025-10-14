package ai.dokus.auth.backend.services

import ai.dokus.foundation.apispec.ClientApi
import ai.dokus.foundation.apispec.ClientEvent
import ai.dokus.foundation.apispec.ClientStats
import ai.dokus.foundation.domain.ClientId
import ai.dokus.foundation.domain.TenantId
import ai.dokus.foundation.domain.VatNumber
import ai.dokus.foundation.domain.VatRate
import ai.dokus.foundation.domain.model.Client
import ai.dokus.foundation.ktor.services.ClientService
import kotlinx.coroutines.flow.Flow

class ClientApiImpl(
    private val clientService: ClientService
) : ClientApi {

    override suspend fun createClient(
        tenantId: TenantId,
        name: String,
        email: String?,
        phone: String?,
        vatNumber: String?,
        addressLine1: String?,
        city: String?,
        postalCode: String?,
        country: String?,
        companyNumber: String?,
        defaultPaymentTerms: Int,
        defaultVatRate: VatRate?,
        peppolId: String?,
        peppolEnabled: Boolean,
        tags: String?,
        notes: String?
    ): Result<Client> = runCatching {
        // Convert String vatNumber to VatNumber value class if provided
        val vatNumberValue = vatNumber?.let { VatNumber(it) }

        // Note: ClientService doesn't support all the new Peppol fields yet
        // For now, we'll use the available fields and TODO: update ClientService
        clientService.create(
            tenantId = tenantId,
            name = name,
            email = email,
            vatNumber = vatNumberValue,
            addressLine1 = addressLine1,
            addressLine2 = null, // Not in new API
            city = city,
            postalCode = postalCode,
            country = country,
            contactPerson = null, // Not in new API
            phone = phone,
            notes = notes
        )
    }

    override suspend fun getClient(id: ClientId, tenantId: TenantId): Result<Client> = runCatching {
        val client = clientService.findById(id)
            ?: throw IllegalArgumentException("Client not found: $id")

        // Verify tenant isolation
        if (client.tenantId != tenantId) {
            throw IllegalArgumentException("Client does not belong to tenant: $tenantId")
        }

        client
    }

    override suspend fun listClients(
        tenantId: TenantId,
        search: String?,
        isActive: Boolean?,
        limit: Int,
        offset: Int
    ): Result<List<Client>> = runCatching {
        if (search != null) {
            // Use search method if search query provided
            clientService.search(tenantId, search, isActive ?: true)
        } else {
            // Otherwise list all clients for tenant
            clientService.listByTenant(tenantId, isActive ?: true)
        }
        // Note: ClientService doesn't support limit/offset pagination yet
        // TODO: Add pagination support to ClientService
    }

    override suspend fun updateClient(
        id: ClientId,
        tenantId: TenantId,
        name: String?,
        email: String?,
        phone: String?,
        vatNumber: String?,
        addressLine1: String?,
        city: String?,
        postalCode: String?,
        country: String?,
        peppolId: String?,
        peppolEnabled: Boolean?,
        defaultPaymentTerms: Int?,
        defaultVatRate: VatRate?,
        tags: String?,
        notes: String?,
        isActive: Boolean?
    ): Result<Client> = runCatching {
        // Verify client exists and belongs to tenant
        val existingClient = clientService.findById(id)
            ?: throw IllegalArgumentException("Client not found: $id")

        if (existingClient.tenantId != tenantId) {
            throw IllegalArgumentException("Client does not belong to tenant: $tenantId")
        }

        // Convert String vatNumber to VatNumber value class if provided
        val vatNumberValue = vatNumber?.let { VatNumber(it) }

        // Note: ClientService doesn't support all the new Peppol fields yet
        // For now, we'll use the available fields and TODO: update ClientService
        clientService.update(
            clientId = id,
            name = name,
            email = email,
            vatNumber = vatNumberValue,
            addressLine1 = addressLine1,
            addressLine2 = null, // Not in new API
            city = city,
            postalCode = postalCode,
            country = country,
            contactPerson = null, // Not in new API
            phone = phone,
            notes = notes
        )

        // Return updated client
        clientService.findById(id)
            ?: throw IllegalStateException("Client disappeared after update: $id")
    }

    override suspend fun deleteClient(id: ClientId, tenantId: TenantId): Result<Unit> = runCatching {
        // Verify client exists and belongs to tenant
        val client = clientService.findById(id)
            ?: throw IllegalArgumentException("Client not found: $id")

        if (client.tenantId != tenantId) {
            throw IllegalArgumentException("Client does not belong to tenant: $tenantId")
        }

        clientService.delete(id)
    }

    override suspend fun findClientByPeppolId(peppolId: String, tenantId: TenantId): Result<Client?> = runCatching {
        // Note: ClientService doesn't have findByPeppolId method yet
        // We'd need to add this to the service layer or use repository directly
        // For now, return null to indicate not found
        // TODO: Add findByPeppolId to ClientService
        null
    }

    override suspend fun getClientStats(tenantId: TenantId): Result<ClientStats> = runCatching {
        val allClients = clientService.listByTenant(tenantId, activeOnly = false)
        val activeClients = allClients.filter { it.isActive }
        val inactiveClients = allClients.filter { !it.isActive }
        val peppolEnabledClients = allClients.filter { it.peppolEnabled }

        ClientStats(
            totalClients = allClients.size.toLong(),
            activeClients = activeClients.size.toLong(),
            inactiveClients = inactiveClients.size.toLong(),
            peppolEnabledClients = peppolEnabledClients.size.toLong()
        )
    }

    override fun watchClients(tenantId: TenantId): Flow<ClientEvent> {
        // Note: ClientService doesn't have a watch method yet
        // This would require implementing a Flow-based real-time update mechanism
        // TODO: Add watchClients to ClientService
        throw NotImplementedError("Real-time client watching not yet implemented")
    }
}
