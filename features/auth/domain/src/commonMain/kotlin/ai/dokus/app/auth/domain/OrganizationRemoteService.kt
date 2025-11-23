package ai.dokus.app.auth.domain

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
import kotlinx.rpc.annotations.Rpc

@Rpc
interface OrganizationRemoteService {
    suspend fun listMyOrganizations(): List<Organization>

    suspend fun createOrganization(
        legalName: LegalName,
        email: Email,
        plan: OrganizationPlan = OrganizationPlan.Free,
        country: Country,
        language: Language = Language.En,
        vatNumber: VatNumber
    ): Organization

    suspend fun getOrganization(id: OrganizationId): Organization

    suspend fun getOrganizationSettings(): OrganizationSettings

    suspend fun updateOrganizationSettings(settings: OrganizationSettings)

    suspend fun getNextInvoiceNumber(): InvoiceNumber
}