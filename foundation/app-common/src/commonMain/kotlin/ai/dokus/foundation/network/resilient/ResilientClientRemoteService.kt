package ai.dokus.foundation.network.resilient

import ai.dokus.foundation.domain.VatRate
import ai.dokus.foundation.domain.ids.ClientId
import ai.dokus.foundation.domain.ids.OrganizationId
import ai.dokus.foundation.domain.model.ClientDto
import ai.dokus.foundation.domain.model.ClientEvent
import ai.dokus.foundation.domain.model.ClientStats
import ai.dokus.foundation.domain.rpc.ClientRemoteService
import kotlinx.coroutines.flow.Flow

class ResilientClientRemoteService(
    serviceProvider: () -> ClientRemoteService
) : ClientRemoteService {

    private val delegate = RetryResilientDelegate(serviceProvider)

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
    ): ClientDto = delegate {
        it.createClient(
            name,
            email,
            phone,
            vatNumber,
            addressLine1,
            city,
            postalCode,
            country,
            companyNumber,
            defaultPaymentTerms,
            defaultVatRate,
            peppolId,
            peppolEnabled,
            tags,
            notes
        )
    }

    override suspend fun getClient(id: ClientId): ClientDto = delegate { it.getClient(id) }

    override suspend fun listClients(
        search: String?,
        isActive: Boolean?,
        limit: Int,
        offset: Int
    ): List<ClientDto> = delegate { it.listClients(search, isActive, limit, offset) }

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
    ): ClientDto = delegate {
        it.updateClient(
            id,
            name,
            email,
            phone,
            vatNumber,
            addressLine1,
            city,
            postalCode,
            country,
            peppolId,
            peppolEnabled,
            defaultPaymentTerms,
            defaultVatRate,
            tags,
            notes,
            isActive
        )
    }

    override suspend fun deleteClient(id: ClientId) = delegate { it.deleteClient(id) }

    override suspend fun findClientByPeppolId(peppolId: String): ClientDto? = delegate {
        it.findClientByPeppolId(peppolId)
    }

    override suspend fun getClientStats(): ClientStats = delegate { it.getClientStats() }

    override fun watchClients(organizationId: OrganizationId): Flow<ClientEvent> =
        delegate.get().watchClients(organizationId)
}
