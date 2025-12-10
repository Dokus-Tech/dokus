package ai.dokus.cashflow.backend.service

import ai.dokus.foundation.database.repository.cashflow.ClientRepository
import ai.dokus.foundation.domain.ids.ClientId
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.model.ClientDto
import ai.dokus.foundation.domain.model.ClientStats
import ai.dokus.foundation.domain.model.CreateClientRequest
import ai.dokus.foundation.domain.model.PaginatedResponse
import ai.dokus.foundation.domain.model.UpdateClientRequest
import org.slf4j.LoggerFactory

/**
 * Service for client business operations.
 *
 * Clients represent customers who receive invoices (for Cash-In).
 * This service handles all business logic related to clients
 * and delegates data access to the repository layer.
 */
class ClientService(
    private val clientRepository: ClientRepository
) {
    private val logger = LoggerFactory.getLogger(ClientService::class.java)

    /**
     * Create a new client for a tenant.
     */
    suspend fun createClient(
        tenantId: TenantId,
        request: CreateClientRequest
    ): Result<ClientDto> {
        logger.info("Creating client for tenant: $tenantId, name: ${request.name}")
        return clientRepository.createClient(tenantId, request)
            .onSuccess { logger.info("Client created: ${it.id}") }
            .onFailure { logger.error("Failed to create client for tenant: $tenantId", it) }
    }

    /**
     * Get a client by ID.
     */
    suspend fun getClient(
        clientId: ClientId,
        tenantId: TenantId
    ): Result<ClientDto?> {
        logger.debug("Fetching client: $clientId for tenant: $tenantId")
        return clientRepository.getClient(clientId, tenantId)
            .onFailure { logger.error("Failed to fetch client: $clientId", it) }
    }

    /**
     * List clients with optional filters.
     */
    suspend fun listClients(
        tenantId: TenantId,
        isActive: Boolean? = null,
        peppolEnabled: Boolean? = null,
        searchQuery: String? = null,
        limit: Int = 50,
        offset: Int = 0
    ): Result<PaginatedResponse<ClientDto>> {
        logger.debug("Listing clients for tenant: $tenantId (isActive=$isActive, peppolEnabled=$peppolEnabled, limit=$limit, offset=$offset)")
        return clientRepository.listClients(tenantId, isActive, peppolEnabled, searchQuery, limit, offset)
            .onSuccess { logger.debug("Retrieved ${it.items.size} clients (total=${it.total})") }
            .onFailure { logger.error("Failed to list clients for tenant: $tenantId", it) }
    }

    /**
     * Update a client.
     */
    suspend fun updateClient(
        clientId: ClientId,
        tenantId: TenantId,
        request: UpdateClientRequest
    ): Result<ClientDto> {
        logger.info("Updating client: $clientId for tenant: $tenantId")
        return clientRepository.updateClient(clientId, tenantId, request)
            .onSuccess { logger.info("Client updated: $clientId") }
            .onFailure { logger.error("Failed to update client: $clientId", it) }
    }

    /**
     * Update a client's Peppol settings.
     */
    suspend fun updateClientPeppol(
        clientId: ClientId,
        tenantId: TenantId,
        peppolId: String?,
        peppolEnabled: Boolean
    ): Result<ClientDto> {
        logger.info("Updating client Peppol settings: $clientId (peppolId=$peppolId, peppolEnabled=$peppolEnabled)")
        return clientRepository.updateClientPeppol(clientId, tenantId, peppolId, peppolEnabled)
            .onSuccess { logger.info("Client Peppol settings updated: $clientId") }
            .onFailure { logger.error("Failed to update client Peppol settings: $clientId", it) }
    }

    /**
     * Delete a client.
     */
    suspend fun deleteClient(
        clientId: ClientId,
        tenantId: TenantId
    ): Result<Boolean> {
        logger.info("Deleting client: $clientId")
        return clientRepository.deleteClient(clientId, tenantId)
            .onSuccess { logger.info("Client deleted: $clientId") }
            .onFailure { logger.error("Failed to delete client: $clientId", it) }
    }

    /**
     * Deactivate a client (soft delete).
     */
    suspend fun deactivateClient(
        clientId: ClientId,
        tenantId: TenantId
    ): Result<Boolean> {
        logger.info("Deactivating client: $clientId")
        return clientRepository.deactivateClient(clientId, tenantId)
            .onSuccess { logger.info("Client deactivated: $clientId") }
            .onFailure { logger.error("Failed to deactivate client: $clientId", it) }
    }

    /**
     * Reactivate a client.
     */
    suspend fun reactivateClient(
        clientId: ClientId,
        tenantId: TenantId
    ): Result<Boolean> {
        logger.info("Reactivating client: $clientId")
        return clientRepository.reactivateClient(clientId, tenantId)
            .onSuccess { logger.info("Client reactivated: $clientId") }
            .onFailure { logger.error("Failed to reactivate client: $clientId", it) }
    }

    /**
     * Check if a client exists.
     */
    suspend fun exists(
        clientId: ClientId,
        tenantId: TenantId
    ): Result<Boolean> {
        return clientRepository.exists(clientId, tenantId)
    }

    /**
     * Get client statistics for dashboard.
     */
    suspend fun getClientStats(tenantId: TenantId): Result<ClientStats> {
        logger.debug("Getting client stats for tenant: $tenantId")
        return clientRepository.getClientStats(tenantId)
            .onFailure { logger.error("Failed to get client stats for tenant: $tenantId", it) }
    }

    /**
     * List Peppol-enabled clients.
     */
    suspend fun listPeppolEnabledClients(tenantId: TenantId): Result<List<ClientDto>> {
        logger.debug("Listing Peppol-enabled clients for tenant: $tenantId")
        return clientRepository.listPeppolEnabledClients(tenantId)
            .onSuccess { logger.debug("Retrieved ${it.size} Peppol-enabled clients") }
            .onFailure { logger.error("Failed to list Peppol-enabled clients for tenant: $tenantId", it) }
    }
}
