package ai.dokus.foundation.apispec

import ai.dokus.foundation.domain.ClientId
import ai.dokus.foundation.domain.TenantId
import ai.dokus.foundation.domain.VatRate
import ai.dokus.foundation.domain.model.Client
import kotlinx.coroutines.flow.Flow
import kotlinx.rpc.annotations.Rpc

@Rpc
interface ClientApi {

    /**
     * Create a new client
     * Supports Peppol ID registration for e-invoicing compliance
     */
    suspend fun createClient(
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
        peppolEnabled: Boolean = false,
        tags: String? = null,
        notes: String? = null
    ): Result<Client>

    /**
     * Get a client by ID
     * Enforces tenant isolation
     */
    suspend fun getClient(id: ClientId, tenantId: TenantId): Result<Client>

    /**
     * List clients for a tenant
     * Supports search, filtering, and pagination
     */
    suspend fun listClients(
        tenantId: TenantId,
        search: String? = null,
        isActive: Boolean? = null,
        limit: Int = 100,
        offset: Int = 0
    ): Result<List<Client>>

    /**
     * Update an existing client
     * All fields are optional - only provided fields will be updated
     */
    suspend fun updateClient(
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
    ): Result<Client>

    /**
     * Delete a client (soft delete)
     * Sets isActive = false instead of removing from database
     */
    suspend fun deleteClient(id: ClientId, tenantId: TenantId): Result<Unit>

    /**
     * Find client by Peppol participant ID
     * Used for incoming Peppol e-invoices
     */
    suspend fun findClientByPeppolId(peppolId: String, tenantId: TenantId): Result<Client?>

    /**
     * Get client statistics
     * Returns count of active/inactive clients
     */
    suspend fun getClientStats(tenantId: TenantId): Result<ClientStats>

    /**
     * Watch for client changes in real-time
     * Emits events when clients are created, updated, or deleted
     */
    fun watchClients(tenantId: TenantId): Flow<ClientEvent>
}

/**
 * Client statistics for dashboard
 */
@kotlinx.serialization.Serializable
data class ClientStats(
    val totalClients: Long,
    val activeClients: Long,
    val inactiveClients: Long,
    val peppolEnabledClients: Long
)

/**
 * Real-time client events for reactive UI updates
 */
@kotlinx.serialization.Serializable
sealed class ClientEvent {
    @kotlinx.serialization.Serializable
    data class ClientCreated(val client: Client) : ClientEvent()

    @kotlinx.serialization.Serializable
    data class ClientUpdated(val client: Client) : ClientEvent()

    @kotlinx.serialization.Serializable
    data class ClientDeleted(val clientId: ClientId) : ClientEvent()
}
