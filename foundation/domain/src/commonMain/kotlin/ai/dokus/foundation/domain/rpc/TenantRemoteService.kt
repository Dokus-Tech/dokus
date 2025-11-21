package ai.dokus.foundation.domain.rpc

import ai.dokus.foundation.domain.ids.InvoiceNumber
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.ids.VatNumber
import ai.dokus.foundation.domain.enums.Language
import ai.dokus.foundation.domain.enums.TenantPlan
import ai.dokus.foundation.domain.model.Tenant
import ai.dokus.foundation.domain.model.TenantSettings
import kotlinx.rpc.annotations.Rpc

@Rpc
interface TenantRemoteService {

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
