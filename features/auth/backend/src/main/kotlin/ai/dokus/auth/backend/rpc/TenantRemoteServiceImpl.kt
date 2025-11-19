package ai.dokus.auth.backend.rpc

import ai.dokus.auth.backend.database.repository.TenantRepository
import ai.dokus.foundation.domain.InvoiceNumber
import ai.dokus.foundation.domain.TenantId
import ai.dokus.foundation.domain.VatNumber
import ai.dokus.foundation.domain.enums.Language
import ai.dokus.foundation.domain.enums.TenantPlan
import ai.dokus.foundation.domain.model.Tenant
import ai.dokus.foundation.domain.model.TenantSettings
import ai.dokus.foundation.domain.rpc.TenantRemoteService
import ai.dokus.foundation.ktor.security.AuthInfoProvider
import ai.dokus.foundation.ktor.security.requireAuthenticatedTenantId

class TenantRemoteServiceImpl(
    private val tenantService: TenantRepository,
    private val authInfoProvider: AuthInfoProvider,
) : TenantRemoteService {

    override suspend fun createTenant(
        name: String,
        email: String,
        plan: TenantPlan,
        country: String,
        language: Language,
        vatNumber: VatNumber?
    ): Tenant {
        val createdTenant = tenantService.create(name, email, plan, country, language, vatNumber)
        return tenantService.findById(id = createdTenant)
            ?: throw IllegalArgumentException("Tenant not found: $createdTenant")
    }

    override suspend fun getTenant(id: TenantId): Tenant {
        return tenantService.findById(id) ?: throw IllegalArgumentException("Tenant not found: $id")
    }

    override suspend fun getTenantSettings(): TenantSettings {
        return authInfoProvider.withAuthInfo {
            val tenantId = requireAuthenticatedTenantId()
            return@withAuthInfo tenantService.getSettings(tenantId)
        }
    }

    override suspend fun updateTenantSettings(settings: TenantSettings) {
        tenantService.updateSettings(settings)
    }

    override suspend fun getNextInvoiceNumber(): InvoiceNumber {
        return authInfoProvider.withAuthInfo {
            val tenantId = requireAuthenticatedTenantId()
            return@withAuthInfo tenantService.getNextInvoiceNumber(tenantId)
        }
    }
}