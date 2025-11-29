package ai.dokus.app.auth.domain

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
import kotlinx.rpc.annotations.Rpc

@Rpc
interface TenantRemoteService {
    suspend fun listMyTenants(): List<Tenant>

    suspend fun createTenant(
        type: TenantType,
        legalName: LegalName,
        displayName: DisplayName,
        plan: TenantPlan = TenantPlan.Free,
        language: Language = Language.En,
        vatNumber: VatNumber
    ): Tenant

    suspend fun getTenant(id: TenantId): Tenant

    suspend fun getTenantSettings(): TenantSettings

    suspend fun updateTenantSettings(settings: TenantSettings)

    suspend fun getNextInvoiceNumber(): InvoiceNumber

    /** Check if the current user already has a freelancer tenant */
    suspend fun hasFreelancerTenant(): Boolean
}
