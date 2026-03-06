package tech.dokus.backend.services.auth

import tech.dokus.domain.model.FirmMembership
import tech.dokus.domain.model.TenantMembership
import tech.dokus.domain.model.auth.AppSurface
import tech.dokus.domain.model.auth.SurfaceAvailability

object SurfaceResolver {

    fun resolve(
        tenantMemberships: List<TenantMembership>,
        firmMemberships: List<FirmMembership>,
    ): SurfaceAvailability {
        val activeTenantMemberships = tenantMemberships.filter { it.isActive }
        val activeFirmMemberships = firmMemberships.filter { it.isActive }
        val canCompanyManager = activeTenantMemberships.isNotEmpty()
        val canBookkeeperConsole = activeFirmMemberships.isNotEmpty()
        val defaultSurface = when {
            canCompanyManager -> AppSurface.CompanyManager
            canBookkeeperConsole -> AppSurface.BookkeeperConsole
            // Safe default: users with no active memberships land on the onboarding flow
            else -> AppSurface.CompanyManager
        }

        return SurfaceAvailability(
            canCompanyManager = canCompanyManager,
            canBookkeeperConsole = canBookkeeperConsole,
            defaultSurface = defaultSurface
        )
    }
}
