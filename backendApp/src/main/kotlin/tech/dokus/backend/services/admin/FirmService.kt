package tech.dokus.backend.services.admin

import tech.dokus.database.repository.auth.FirmRepository
import tech.dokus.database.repository.auth.TenantRepository
import tech.dokus.domain.ids.FirmId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.Firm
import tech.dokus.domain.model.Tenant
import tech.dokus.domain.DisplayName
import tech.dokus.domain.ids.VatNumber

/**
 * Service layer for firm management operations.
 * Wraps repository calls used by FirmRoutes.
 */
class FirmService(
    private val firmRepository: FirmRepository,
    private val tenantRepository: TenantRepository,
) {
    suspend fun findTenantById(tenantId: TenantId): Tenant? =
        tenantRepository.findById(tenantId)

    suspend fun createFirm(name: DisplayName, vatNumber: VatNumber, ownerUserId: UserId): Firm =
        firmRepository.createFirm(name = name, vatNumber = vatNumber, ownerUserId = ownerUserId)

    suspend fun revokeAccess(firmId: FirmId, tenantId: TenantId): Boolean =
        firmRepository.revokeAccess(firmId, tenantId)
}
