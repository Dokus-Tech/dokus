package ai.dokus.foundation.apispec

import ai.dokus.foundation.domain.ClientId
import ai.dokus.foundation.domain.TenantId
import ai.dokus.foundation.domain.VatNumber
import ai.dokus.foundation.domain.model.Client
import kotlinx.rpc.annotations.Rpc

@Rpc
interface ClientApi {

    suspend fun createClient(
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
    ): Result<Client>

    suspend fun getClient(id: ClientId): Result<Client>

    suspend fun listClients(tenantId: TenantId, activeOnly: Boolean = true): Result<List<Client>>

    suspend fun searchClients(tenantId: TenantId, query: String): Result<List<Client>>

    suspend fun updateClient(
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
    ): Result<Unit>

    suspend fun deleteClient(clientId: ClientId): Result<Unit>
}
