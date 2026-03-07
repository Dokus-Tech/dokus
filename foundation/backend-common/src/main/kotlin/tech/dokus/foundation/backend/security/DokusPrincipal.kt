package tech.dokus.foundation.backend.security

import io.ktor.server.auth.Principal
import tech.dokus.domain.ids.SessionId
import tech.dokus.domain.model.auth.JwtFirmMembershipClaim
import tech.dokus.domain.model.auth.JwtTenantMembershipClaim
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.auth.AuthenticationInfo

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
    val globalRoles: Set<String> = emptySet(),
    val tenantMemberships: List<JwtTenantMembershipClaim> = emptyList(),
    val firmMemberships: List<JwtFirmMembershipClaim> = emptyList(),
    val sessionId: SessionId? = null,
    val sessionJti: String? = null
) : Principal {

    /**
     * Check if the user has a specific role
     */
    fun hasRole(role: String): Boolean = globalRoles.contains(role)

    /**
     * Check if the user has any of the specified roles
     */
    fun hasAnyRole(vararg roles: String): Boolean =
        roles.any { this.globalRoles.contains(it) }

    /**
     * Check if the user has all of the specified roles
     */
    fun hasAllRoles(vararg roles: String): Boolean =
        roles.all { this.globalRoles.contains(it) }

    /**
     * Resolves the stable session identity, falling back to the JWT's JTI
     * for tokens issued before session identity tracking was added.
     */
    fun currentSessionId(): SessionId? {
        return sessionId ?: sessionJti?.let { jti ->
            runCatching { SessionId(jti) }.getOrNull()
        }
    }

    companion object {
        /**
         * Create DokusPrincipal from AuthenticationInfo
         */
        fun fromAuthInfo(authInfo: AuthenticationInfo): DokusPrincipal {
            return DokusPrincipal(
                userId = authInfo.userId,
                email = authInfo.email,
                name = authInfo.name,
                globalRoles = authInfo.globalRoles,
                tenantMemberships = authInfo.tenantMemberships,
                firmMemberships = authInfo.firmMemberships,
                sessionId = authInfo.sessionId,
                sessionJti = authInfo.sessionJti
            )
        }
    }
}
