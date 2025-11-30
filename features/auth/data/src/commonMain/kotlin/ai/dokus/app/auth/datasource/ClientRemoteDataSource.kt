package ai.dokus.app.auth.datasource

import ai.dokus.foundation.domain.ids.ClientId
import ai.dokus.foundation.domain.model.ClientDto
import ai.dokus.foundation.domain.model.ClientStats
import ai.dokus.foundation.domain.model.CreateClientRequest
import ai.dokus.foundation.domain.model.UpdateClientRequest
import io.ktor.client.HttpClient

/**
 * Remote data source for client operations
 * Provides HTTP-based access to client management endpoints
 */
interface ClientRemoteDataSource {

    /**
     * Create a new client
     * POST /api/v1/clients
     */
    suspend fun createClient(request: CreateClientRequest): Result<ClientDto>

    /**
     * Get a client by ID
     * GET /api/v1/clients/{id}
     */
    suspend fun getClient(id: ClientId): Result<ClientDto>

    /**
     * List clients with optional filtering
     * GET /api/v1/clients?search={search}&activeOnly={activeOnly}
     */
    suspend fun listClients(
        search: String? = null,
        activeOnly: Boolean = true
    ): Result<List<ClientDto>>

    /**
     * Update an existing client
     * PUT /api/v1/clients/{id}
     */
    suspend fun updateClient(
        id: ClientId,
        request: UpdateClientRequest
    ): Result<ClientDto>

    /**
     * Delete a client (soft delete)
     * DELETE /api/v1/clients/{id}
     */
    suspend fun deleteClient(id: ClientId): Result<Unit>

    /**
     * Find client by Peppol participant ID
     * GET /api/v1/clients/by-peppol/{peppolId}
     */
    suspend fun findClientByPeppolId(peppolId: String): Result<ClientDto?>

    /**
     * Get client statistics
     * GET /api/v1/clients/stats
     */
    suspend fun getClientStats(): Result<ClientStats>

    companion object {
        internal fun create(httpClient: HttpClient): ClientRemoteDataSource {
            return ClientRemoteDataSourceImpl(httpClient)
        }
    }
}
