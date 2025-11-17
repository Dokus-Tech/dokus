package ai.dokus.auth.backend.services

import ai.dokus.foundation.domain.InvoiceNumber
import ai.dokus.foundation.domain.TenantId
import ai.dokus.foundation.domain.VatNumber
import ai.dokus.foundation.domain.enums.Language
import ai.dokus.foundation.domain.enums.TenantPlan
import ai.dokus.foundation.domain.model.Tenant
import ai.dokus.foundation.domain.model.TenantSettings
import ai.dokus.foundation.domain.rpc.TenantApi
import ai.dokus.foundation.ktor.security.requireAuthenticatedTenantId
import ai.dokus.foundation.ktor.services.TenantService

class TenantApiImpl(
    private val tenantService: TenantService
) : TenantApi {

    override suspend fun createTenant(
        name: String,
        email: String,
        plan: TenantPlan,
        country: String,
        language: Language,
        vatNumber: VatNumber?
    ): Tenant {
        return tenantService.createTenant(name, email, plan, country, language, vatNumber)
    }

    override suspend fun getTenant(id: TenantId): Tenant {
        return tenantService.findById(id) ?: throw IllegalArgumentException("Tenant not found: $id")
    }

    override suspend fun getTenantSettings(): TenantSettings {
        val tenantId = requireAuthenticatedTenantId()
        return tenantService.getSettings(tenantId)
    }

    override suspend fun updateTenantSettings(settings: TenantSettings) {
        tenantService.updateSettings(settings)
    }

    override suspend fun getNextInvoiceNumber(): InvoiceNumber {
        val tenantId = requireAuthenticatedTenantId()
        return tenantService.getNextInvoiceNumber(tenantId)
    }
}
