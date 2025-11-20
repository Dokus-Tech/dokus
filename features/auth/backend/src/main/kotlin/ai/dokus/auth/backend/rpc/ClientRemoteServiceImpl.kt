package ai.dokus.auth.backend.rpc

import ai.dokus.foundation.domain.ClientId
import ai.dokus.foundation.domain.TenantId
import ai.dokus.foundation.domain.VatNumber
import ai.dokus.foundation.domain.VatRate
import ai.dokus.foundation.domain.model.Client
import ai.dokus.foundation.domain.model.ClientEvent
import ai.dokus.foundation.domain.model.ClientStats
import ai.dokus.foundation.domain.rpc.ClientRemoteService
import ai.dokus.foundation.ktor.security.AuthInfoProvider
import ai.dokus.foundation.ktor.security.requireAuthenticatedTenantId
import ai.dokus.foundation.ktor.services.ClientService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ClientRemoteServiceImpl(
    private val clientService: ClientService,
    private val authInfoProvider: AuthInfoProvider,
) : ClientRemoteService {

    override suspend fun createClient(
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
    ): Client {
        return authInfoProvider.withAuthInfo {
            val tenantId = requireAuthenticatedTenantId()

            // Convert String vatNumber to VatNumber value class if provided
            val vatNumberValue = vatNumber?.let { VatNumber(it) }

            // Note: ClientService doesn't support all the new Peppol fields yet
            // For now, we'll use the available fields and TODO: update ClientService
            return@withAuthInfo clientService.create(
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
    }

    override suspend fun getClient(id: ClientId): Client {
        val tenantId = requireAuthenticatedTenantId()

        val client = clientService.findById(id)
            ?: throw IllegalArgumentException("Client not found: $id")

        // Verify tenant isolation
        if (client.tenantId != tenantId) {
            throw IllegalArgumentException("Client does not belong to tenant: $tenantId")
        }

        return client
    }

    override suspend fun listClients(
        search: String?,
        isActive: Boolean?,
        limit: Int,
        offset: Int
    ): List<Client> {
        val tenantId = requireAuthenticatedTenantId()

        return if (search != null) {
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
    ): Client {
        val tenantId = requireAuthenticatedTenantId()

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
        return clientService.findById(id)
            ?: throw IllegalStateException("Client disappeared after update: $id")
    }

    override suspend fun deleteClient(id: ClientId) {
        val tenantId = requireAuthenticatedTenantId()

        // Verify client exists and belongs to tenant
        val client = clientService.findById(id)
            ?: throw IllegalArgumentException("Client not found: $id")

        if (client.tenantId != tenantId) {
            throw IllegalArgumentException("Client does not belong to tenant: $tenantId")
        }

        clientService.delete(id)
    }

    override suspend fun findClientByPeppolId(peppolId: String): Client? {
        val tenantId = requireAuthenticatedTenantId()

        // ClientService doesn't provide findByPeppolId directly
        // Search through all clients and filter by peppolId
        val allClients = clientService.listByTenant(tenantId)

        return allClients.firstOrNull { it.peppolId == peppolId }
    }

    override suspend fun getClientStats(): ClientStats {
        val tenantId = requireAuthenticatedTenantId()

        val allClients = clientService.listByTenant(tenantId, activeOnly = false)
        val activeClients = allClients.filter { it.isActive }
        val inactiveClients = allClients.filter { !it.isActive }
        val peppolEnabledClients = allClients.filter { it.peppolEnabled }

        return ClientStats(
            totalClients = allClients.size.toLong(),
            activeClients = activeClients.size.toLong(),
            inactiveClients = inactiveClients.size.toLong(),
            peppolEnabledClients = peppolEnabledClients.size.toLong()
        )
    }

    override fun watchClients(tenantId: TenantId): Flow<ClientEvent> {
        // Implement polling-based watching since ClientService doesn't provide streaming
        return flow {
            var lastSeenClients = emptyMap<ClientId, Client>()

            while (true) {
                // Poll for client changes every 5 seconds
                delay(5000)

                try {
                    val currentClients = clientService.listByTenant(tenantId, activeOnly = false)
                    val currentMap = currentClients.associateBy { it.id }

                    currentClients.forEach { client ->
                        val previous = lastSeenClients[client.id]
                        when {
                            previous == null -> {
                                // New client
                                emit(ClientEvent.ClientCreated(client))
                            }

                            previous != client -> {
                                // Updated client (comparing entire object)
                                emit(ClientEvent.ClientUpdated(client))
                            }
                        }
                    }

                    // Check for deleted clients
                    lastSeenClients.keys.forEach { oldId ->
                        if (oldId !in currentMap) {
                            emit(ClientEvent.ClientDeleted(oldId))
                        }
                    }

                    lastSeenClients = currentMap
                } catch (e: Exception) {
                    // Log error but continue polling
                    // In production, this would use proper logging
                }
            }
        }
    }
}