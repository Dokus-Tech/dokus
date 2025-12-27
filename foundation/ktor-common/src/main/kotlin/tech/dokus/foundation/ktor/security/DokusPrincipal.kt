package tech.dokus.foundation.ktor.security

import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.ids.UserId
import ai.dokus.foundation.domain.model.AuthenticationInfo
import io.ktor.server.auth.Principal

/**
 * JWT Principal representing an authenticated user in Dokus.
 * This is used by Ktor's JWT authentication plugin and can be accessed
 * via `call.principal<DokusPrincipal>()` in authenticated routes.
 *
 * Wraps [AuthenticationInfo] while implementing Ktor's [Principal] interface.
 */
data class DokusPrincipal(
    val userId: UserId,
    val email: String,
    val name: String,
    val tenantId: TenantId?,
    val roles: Set<String>
) : Principal {

    /**
     * Check if the user has a specific role
     */
    fun hasRole(role: String): Boolean = roles.contains(role)

    /**
     * Check if the user has any of the specified roles
     */
    fun hasAnyRole(vararg roles: String): Boolean =
        roles.any { this.roles.contains(it) }

    /**
     * Check if the user has all of the specified roles
     */
    fun hasAllRoles(vararg roles: String): Boolean =
        roles.all { this.roles.contains(it) }

    /**
     * Require tenant to be selected, throwing if not
     */
    fun requireTenantId(): TenantId =
        tenantId ?: throw DokusException.BadRequest("Tenant context required but not selected")

    companion object {
        /**
         * Create DokusPrincipal from AuthenticationInfo
         */
        fun fromAuthInfo(authInfo: AuthenticationInfo): DokusPrincipal {
            return DokusPrincipal(
                userId = authInfo.userId,
                email = authInfo.email,
                name = authInfo.name,
                tenantId = authInfo.tenantId,
                roles = authInfo.roles
            )
        }
    }
}
