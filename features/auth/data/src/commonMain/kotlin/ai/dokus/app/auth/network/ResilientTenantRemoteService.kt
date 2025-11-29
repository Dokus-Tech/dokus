package ai.dokus.app.auth.network

import ai.dokus.app.auth.domain.TenantRemoteService
import ai.dokus.foundation.domain.DisplayName
import ai.dokus.foundation.domain.LegalName
import ai.dokus.foundation.domain.enums.Language
import ai.dokus.foundation.domain.enums.TenantPlan
import ai.dokus.foundation.domain.enums.TenantType
import ai.dokus.foundation.domain.ids.InvoiceNumber
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.ids.VatNumber
import ai.dokus.foundation.domain.model.Tenant
import ai.dokus.foundation.domain.model.TenantSettings
import ai.dokus.foundation.network.resilient.RemoteServiceDelegate
import ai.dokus.foundation.network.resilient.invoke

class ResilientTenantRemoteService(
    private val delegate: RemoteServiceDelegate<TenantRemoteService>,
) : TenantRemoteService {

    override suspend fun listMyTenants(): List<Tenant> = delegate { it.listMyTenants() }

    override suspend fun createTenant(
        type: TenantType,
        legalName: LegalName,
        displayName: DisplayName,
        plan: TenantPlan,
        language: Language,
        vatNumber: VatNumber
    ): Tenant = delegate { it.createTenant(type, legalName, displayName, plan, language, vatNumber) }

    override suspend fun hasFreelancerTenant(): Boolean = delegate { it.hasFreelancerTenant() }

    override suspend fun getTenant(id: TenantId): Tenant = delegate { it.getTenant(id) }

    override suspend fun getTenantSettings(): TenantSettings = delegate { it.getTenantSettings() }

    override suspend fun updateTenantSettings(settings: TenantSettings) = delegate {
        it.updateTenantSettings(settings)
    }

    override suspend fun getNextInvoiceNumber(): InvoiceNumber = delegate { it.getNextInvoiceNumber() }
}
