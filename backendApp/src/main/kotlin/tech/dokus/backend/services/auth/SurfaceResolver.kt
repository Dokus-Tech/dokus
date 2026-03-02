package tech.dokus.backend.services.auth

import tech.dokus.domain.enums.UserRole
import tech.dokus.domain.model.TenantMembership
import tech.dokus.domain.model.auth.AppSurface
import tech.dokus.domain.model.auth.SurfaceAvailability

object SurfaceResolver {

    fun resolve(memberships: List<TenantMembership>): SurfaceAvailability {
        val activeMemberships = memberships.filter { it.isActive }
        val canCompanyManager = activeMemberships.any { it.role != UserRole.Accountant }
        val canBookkeeperConsole = activeMemberships.any { it.role == UserRole.Accountant }
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
