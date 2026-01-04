package tech.dokus.features.auth.usecases

import tech.dokus.domain.DisplayName
import tech.dokus.domain.LegalName
import tech.dokus.domain.enums.Language
import tech.dokus.domain.enums.TenantPlan
import tech.dokus.domain.enums.TenantType
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.Tenant
import tech.dokus.domain.model.UpsertTenantAddressRequest

/**
 * Use case for listing available tenants.
 */
interface ListMyTenantsUseCase {
    suspend operator fun invoke(): Result<List<Tenant>>
}

/**
 * Use case for creating a new tenant.
 */
interface CreateTenantUseCase {
    @Suppress("LongParameterList") // Tenant creation requires full parameter set
    suspend operator fun invoke(
        type: TenantType,
        legalName: LegalName,
        displayName: DisplayName,
        plan: TenantPlan,
        language: Language,
        vatNumber: VatNumber,
        address: UpsertTenantAddressRequest,
    ): Result<Tenant>
}

/**
 * Use case for checking freelancer tenant existence.
 */
interface HasFreelancerTenantUseCase {
    suspend operator fun invoke(): Result<Boolean>
}

/**
 * Use case for retrieving invoice number preview.
 */
interface GetInvoiceNumberPreviewUseCase {
    suspend operator fun invoke(): Result<String>
}
