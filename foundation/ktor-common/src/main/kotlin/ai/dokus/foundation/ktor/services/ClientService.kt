package ai.dokus.foundation.ktor.services

import ai.dokus.foundation.domain.ids.ClientId
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.ids.VatNumber
import ai.dokus.foundation.domain.model.Client
import kotlinx.rpc.annotations.Rpc

@Rpc
interface ClientService {
    /**
     * Creates a new client for a tenant
     *
     * @param tenantId The tenant ID this client belongs to
     * @param name The client's name or company name
     * @param email The client's email address (optional)
     * @param vatNumber The client's VAT number (optional)
     * @param addressLine1 First line of address (optional)
     * @param addressLine2 Second line of address (optional)
     * @param city The client's city (optional)
     * @param postalCode The client's postal code (optional)
     * @param country The client's country code (ISO 3166-1 alpha-2) (optional)
     * @param contactPerson The name of the contact person (optional)
     * @param phone The client's phone number (optional)
     * @param notes Additional notes about the client (optional)
     * @return The created client
     */
    suspend fun create(
        tenantId: TenantId,
        name: String,
        email: String? = null,
        vatNumber: VatNumber? = null,
        addressLine1: String? = null,
        addressLine2: String? = null,
        city: String? = null,
        postalCode: String? = null,
        country: String? = null,
        contactPerson: String? = null,
        phone: String? = null,
        notes: String? = null
    ): Client

    /**
     * Updates an existing client
     *
     * @param clientId The client's unique identifier
     * @param name The client's name or company name (optional)
     * @param email The client's email address (optional)
     * @param vatNumber The client's VAT number (optional)
     * @param addressLine1 First line of address (optional)
     * @param addressLine2 Second line of address (optional)
     * @param city The client's city (optional)
     * @param postalCode The client's postal code (optional)
     * @param country The client's country code (optional)
     * @param contactPerson The name of the contact person (optional)
     * @param phone The client's phone number (optional)
     * @param notes Additional notes about the client (optional)
     * @throws IllegalArgumentException if client not found
     */
    suspend fun update(
        clientId: ClientId,
        name: String? = null,
        email: String? = null,
        vatNumber: VatNumber? = null,
        addressLine1: String? = null,
        addressLine2: String? = null,
        city: String? = null,
        postalCode: String? = null,
        country: String? = null,
        contactPerson: String? = null,
        phone: String? = null,
        notes: String? = null
    )

    /**
     * Soft deletes a client by marking them as inactive
     * Preserves all historical data and invoices
     *
     * @param clientId The client's unique identifier
     * @throws IllegalArgumentException if client not found
     */
    suspend fun delete(clientId: ClientId)

    /**
     * Reactivates a previously deleted client
     *
     * @param clientId The client's unique identifier
     * @throws IllegalArgumentException if client not found
     */
    suspend fun reactivate(clientId: ClientId)

    /**
     * Finds a client by their unique ID
     *
     * @param id The client's unique identifier
     * @return The client if found, null otherwise
     */
    suspend fun findById(id: ClientId): Client?

    /**
     * Lists all clients for a tenant
     *
     * @param tenantId The tenant's unique identifier
     * @param activeOnly If true, only returns active clients (defaults to true)
     * @return List of clients
     */
    suspend fun listByTenant(tenantId: TenantId, activeOnly: Boolean = true): List<Client>

    /**
     * Searches for clients by name
     *
     * @param tenantId The tenant's unique identifier
     * @param query The search query (matches against client name)
     * @param activeOnly If true, only searches active clients (defaults to true)
     * @return List of matching clients
     */
    suspend fun search(
        tenantId: TenantId,
        query: String,
        activeOnly: Boolean = true
    ): List<Client>

    /**
     * Finds a client by their email address
     *
     * @param tenantId The tenant's unique identifier
     * @param email The client's email address
     * @return The client if found, null otherwise
     */
    suspend fun findByEmail(tenantId: TenantId, email: String): Client?

    /**
     * Finds a client by their VAT number
     *
     * @param tenantId The tenant's unique identifier
     * @param vatNumber The client's VAT number
     * @return The client if found, null otherwise
     */
    suspend fun findByVatNumber(tenantId: TenantId, vatNumber: VatNumber): Client?
}
