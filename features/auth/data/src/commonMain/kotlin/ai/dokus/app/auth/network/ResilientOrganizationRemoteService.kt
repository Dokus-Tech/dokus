package ai.dokus.app.auth.network

import ai.dokus.app.auth.domain.OrganizationRemoteService
import ai.dokus.foundation.domain.Email
import ai.dokus.foundation.domain.LegalName
import ai.dokus.foundation.domain.enums.Country
import ai.dokus.foundation.domain.enums.Language
import ai.dokus.foundation.domain.enums.OrganizationPlan
import ai.dokus.foundation.domain.ids.InvoiceNumber
import ai.dokus.foundation.domain.ids.OrganizationId
import ai.dokus.foundation.domain.ids.VatNumber
import ai.dokus.foundation.domain.model.Organization
import ai.dokus.foundation.domain.model.OrganizationSettings
import ai.dokus.foundation.network.resilient.RemoteServiceDelegate
import ai.dokus.foundation.network.resilient.invoke

class ResilientOrganizationRemoteService(
    private val delegate: RemoteServiceDelegate<OrganizationRemoteService>,
) : OrganizationRemoteService {

    override suspend fun listMyOrganizations(): List<Organization> = delegate { it.listMyOrganizations() }

    override suspend fun createOrganization(
        legalName: LegalName,
        email: Email,
        plan: OrganizationPlan,
        country: Country,
        language: Language,
        vatNumber: VatNumber
    ): Organization = delegate { it.createOrganization(legalName, email, plan, country, language, vatNumber) }

    override suspend fun getOrganization(id: OrganizationId): Organization = delegate { it.getOrganization(id) }

    override suspend fun getOrganizationSettings(): OrganizationSettings = delegate { it.getOrganizationSettings() }

    override suspend fun updateOrganizationSettings(settings: OrganizationSettings) = delegate {
        it.updateOrganizationSettings(settings)
    }

    override suspend fun getNextInvoiceNumber(): InvoiceNumber = delegate { it.getNextInvoiceNumber() }
}
