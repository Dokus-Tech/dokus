package tech.dokus.backend.services.auth

import tech.dokus.domain.enums.UserRole
import tech.dokus.domain.model.TenantMembership
import tech.dokus.domain.model.auth.AppSurface
import tech.dokus.domain.model.auth.SurfaceAvailability

object SurfaceResolver {

    fun resolve(memberships: List<TenantMembership>): SurfaceAvailability {
        val activeMemberships = memberships.filter { it.isActive }
        val canWorkspace = activeMemberships.any { it.role != UserRole.Accountant }
        val canConsole = activeMemberships.any { it.role == UserRole.Accountant }
        val defaultSurface = when {
            canWorkspace -> AppSurface.Workspace
            canConsole -> AppSurface.Console
            // Safe default: users with no active memberships land on the onboarding flow
            else -> AppSurface.Workspace
        }

        return SurfaceAvailability(
            canWorkspace = canWorkspace,
            canConsole = canConsole,
            defaultSurface = defaultSurface
        )
    }
}
