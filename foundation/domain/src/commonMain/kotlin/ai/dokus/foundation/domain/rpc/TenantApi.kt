package ai.dokus.foundation.domain.rpc

import ai.dokus.foundation.domain.InvoiceNumber
import ai.dokus.foundation.domain.TenantId
import ai.dokus.foundation.domain.VatNumber
import ai.dokus.foundation.domain.enums.Language
import ai.dokus.foundation.domain.enums.TenantPlan
import ai.dokus.foundation.domain.model.Tenant
import ai.dokus.foundation.domain.model.TenantSettings
import kotlinx.rpc.annotations.Rpc

@Rpc
interface TenantApi {

    suspend fun createTenant(
        name: String,
        email: String,
        plan: TenantPlan = TenantPlan.Free,
        country: String = "BE",
        language: Language = Language.En,
        vatNumber: VatNumber? = null
    ): Result<Tenant>

    suspend fun getTenant(id: TenantId): Result<Tenant>

    suspend fun getTenantSettings(tenantId: TenantId): Result<TenantSettings>

    suspend fun updateTenantSettings(settings: TenantSettings): Result<Unit>

    suspend fun getNextInvoiceNumber(tenantId: TenantId): Result<InvoiceNumber>
}
