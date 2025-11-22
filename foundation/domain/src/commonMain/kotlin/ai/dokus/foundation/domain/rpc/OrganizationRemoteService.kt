package ai.dokus.foundation.domain.rpc

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

    suspend fun createOrganization(
        name: String,
        email: String,
        plan: OrganizationPlan = OrganizationPlan.Free,
        country: String = "BE",
        language: Language = Language.En,
        vatNumber: VatNumber? = null
    ): Organization

    suspend fun getOrganization(id: OrganizationId): Organization

    suspend fun getOrganizationSettings(): OrganizationSettings

    suspend fun updateOrganizationSettings(settings: OrganizationSettings)

    suspend fun getNextInvoiceNumber(): InvoiceNumber
}
