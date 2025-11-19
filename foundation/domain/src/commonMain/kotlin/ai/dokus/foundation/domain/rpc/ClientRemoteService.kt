package ai.dokus.foundation.domain.rpc

import ai.dokus.foundation.domain.ClientId
import ai.dokus.foundation.domain.TenantId
import ai.dokus.foundation.domain.VatRate
import ai.dokus.foundation.domain.model.Client
import ai.dokus.foundation.domain.model.ClientEvent
import ai.dokus.foundation.domain.model.ClientStats
import kotlinx.coroutines.flow.Flow
import kotlinx.rpc.annotations.Rpc

@Rpc
interface ClientRemoteService {

    /**
     * Create a new client
     * Supports Peppol ID registration for e-invoicing compliance
     */
    suspend fun createClient(
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
        peppolEnabled: Boolean = false,
        tags: String? = null,
        notes: String? = null
    ): Client

    /**
     * Get a client by ID
     * Enforces tenant isolation
     */
    suspend fun getClient(id: ClientId): Client

    /**
     * List clients for a tenant
     * Supports search, filtering, and pagination
     */
    suspend fun listClients(
        search: String? = null,
        isActive: Boolean? = null,
        limit: Int = 100,
        offset: Int = 0
    ): List<Client>

    /**
     * Update an existing client
     * All fields are optional - only provided fields will be updated
     */
    suspend fun updateClient(
        id: ClientId,
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
    ): Client

    /**
     * Delete a client (soft delete)
     * Sets isActive = false instead of removing from database
     */
    suspend fun deleteClient(id: ClientId)

    /**
     * Find client by Peppol participant ID
     * Used for incoming Peppol e-invoices
     */
    suspend fun findClientByPeppolId(peppolId: String): Client?

    /**
     * Get client statistics
     * Returns count of active/inactive clients
     */
    suspend fun getClientStats(): ClientStats

    /**
     * Watch for client changes in real-time
     * Emits events when clients are created, updated, or deleted
     */
    fun watchClients(tenantId: TenantId): Flow<ClientEvent>
}
