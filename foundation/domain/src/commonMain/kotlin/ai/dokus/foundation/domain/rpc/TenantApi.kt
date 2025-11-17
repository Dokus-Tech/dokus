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
    ): Tenant

    suspend fun getTenant(id: TenantId): Tenant

    suspend fun getTenantSettings(): TenantSettings

    suspend fun updateTenantSettings(settings: TenantSettings)

    suspend fun getNextInvoiceNumber(): InvoiceNumber
}
