package ai.dokus.auth.backend.rpc

import ai.dokus.auth.backend.database.repository.OrganizationRepository
import ai.dokus.foundation.domain.enums.Language
import ai.dokus.foundation.domain.enums.OrganizationPlan
import ai.dokus.foundation.domain.ids.InvoiceNumber
import ai.dokus.foundation.domain.ids.OrganizationId
import ai.dokus.foundation.domain.ids.VatNumber
import ai.dokus.foundation.domain.model.Organization
import ai.dokus.foundation.domain.model.OrganizationSettings
import ai.dokus.foundation.domain.rpc.OrganizationRemoteService
import ai.dokus.foundation.ktor.security.AuthInfoProvider
import ai.dokus.foundation.ktor.security.requireAuthenticatedOrganizationId

class OrganizationRemoteServiceImpl(
    private val organizationService: OrganizationRepository,
    private val authInfoProvider: AuthInfoProvider,
) : OrganizationRemoteService {

    override suspend fun createOrganization(
        name: String,
        email: String,
        plan: OrganizationPlan,
        country: String,
        language: Language,
        vatNumber: VatNumber?
    ): Organization {
        val createdTenant = organizationService.create(name, email, plan, country, language, vatNumber)
        return organizationService.findById(id = createdTenant)
            ?: throw IllegalArgumentException("Tenant not found: $createdTenant")
    }

    override suspend fun getOrganization(id: OrganizationId): Organization {
        return organizationService.findById(id) ?: throw IllegalArgumentException("Tenant not found: $id")
    }

    override suspend fun getOrganizationSettings(): OrganizationSettings {
        return authInfoProvider.withAuthInfo {
            val organizationId = requireAuthenticatedOrganizationId()
            return@withAuthInfo organizationService.getSettings(organizationId)
        }
    }

    override suspend fun updateOrganizationSettings(settings: OrganizationSettings) {
        organizationService.updateSettings(settings)
    }

    override suspend fun getNextInvoiceNumber(): InvoiceNumber {
        return authInfoProvider.withAuthInfo {
            val organizationId = requireAuthenticatedOrganizationId()
            return@withAuthInfo organizationService.getNextInvoiceNumber(organizationId)
        }
    }
}