package ai.dokus.auth.backend.services

import ai.dokus.foundation.apispec.TenantApi
import ai.dokus.foundation.domain.InvoiceNumber
import ai.dokus.foundation.domain.TenantId
import ai.dokus.foundation.domain.VatNumber
import ai.dokus.foundation.domain.enums.Language
import ai.dokus.foundation.domain.enums.TenantPlan
import ai.dokus.foundation.domain.model.Tenant
import ai.dokus.foundation.domain.model.TenantSettings
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
    ): Result<Tenant> = runCatching {
        tenantService.createTenant(name, email, plan, country, language, vatNumber)
    }

    override suspend fun getTenant(id: TenantId): Result<Tenant> = runCatching {
        tenantService.findById(id) ?: throw IllegalArgumentException("Tenant not found: $id")
    }

    override suspend fun getTenantSettings(tenantId: TenantId): Result<TenantSettings> = runCatching {
        tenantService.getSettings(tenantId)
    }

    override suspend fun updateTenantSettings(settings: TenantSettings): Result<Unit> = runCatching {
        tenantService.updateSettings(settings)
    }

    override suspend fun getNextInvoiceNumber(tenantId: TenantId): Result<InvoiceNumber> = runCatching {
        tenantService.getNextInvoiceNumber(tenantId)
    }
}
