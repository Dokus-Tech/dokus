package ai.dokus.foundation.network.resilient

import ai.dokus.foundation.domain.enums.Language
import ai.dokus.foundation.domain.enums.OrganizationPlan
import ai.dokus.foundation.domain.ids.InvoiceNumber
import ai.dokus.foundation.domain.ids.OrganizationId
import ai.dokus.foundation.domain.ids.VatNumber
import ai.dokus.foundation.domain.model.Organization
import ai.dokus.foundation.domain.model.OrganizationSettings
import ai.dokus.foundation.domain.rpc.OrganizationRemoteService

class ResilientOrganizationRemoteService(
    serviceProvider: () -> OrganizationRemoteService
) : OrganizationRemoteService {

    private val delegate = ResilientDelegate(serviceProvider)

    private fun get(): OrganizationRemoteService = delegate.get()
    private suspend inline fun <R> withRetry(crossinline block: suspend (OrganizationRemoteService) -> R): R =
        delegate.withRetry(block)

    override suspend fun createOrganization(
        name: String,
        email: String,
        plan: OrganizationPlan,
        country: String,
        language: Language,
        vatNumber: VatNumber?
    ): Organization = withRetry { it.createOrganization(name, email, plan, country, language, vatNumber) }

    override suspend fun getOrganization(id: OrganizationId): Organization = withRetry { it.getOrganization(id) }

    override suspend fun getOrganizationSettings(): OrganizationSettings = withRetry { it.getOrganizationSettings() }

    override suspend fun updateOrganizationSettings(settings: OrganizationSettings) = withRetry {
        it.updateOrganizationSettings(settings)
    }

    override suspend fun getNextInvoiceNumber(): InvoiceNumber = withRetry { it.getNextInvoiceNumber() }
}
