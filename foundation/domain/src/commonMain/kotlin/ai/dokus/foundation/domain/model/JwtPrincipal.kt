package ai.dokus.foundation.domain.model

import ai.dokus.foundation.domain.ids.OrganizationId
import ai.dokus.foundation.domain.enums.UserRole
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * JWT Principal - represents the authenticated user from JWT token claims
 *
 * This model is used across all microservices for authentication and authorization.
 * It contains all the necessary information extracted from the JWT token.
 */
@Serializable
data class JwtPrincipal(
    // User identification
    val userId: String,                    // Subject (sub) - User UUID
    val sessionId: String,                 // Session UUID for session validation
    val email: String,
    val organizationId: String,                  // Tenant UUID - critical for multi-tenancy

    // User profile
    val firstName: String? = null,
    val lastName: String? = null,
    val fullName: String? = null,

    // Authorization
    val roles: Set<String> = emptySet(),          // Claim: "groups" or "roles"
    val permissions: Set<String> = emptySet(),     // Specific permissions

    // Standard JWT claims
    val jti: String? = null,               // JWT ID (for token blacklisting)
    val iat: Long? = null,                 // Issued at timestamp
    val exp: Long? = null,                 // Expires at timestamp
    val iss: String? = null,               // Issuer
    val aud: String? = null                // Audience
) {
    /**
     * Get tenant ID as OrganizationId type
     */
    val organizationIdTyped: OrganizationId
        get() = OrganizationId.parse(organizationId)

    /**
     * Check if user has specific role
     */
    fun hasRole(role: UserRole): Boolean {
        return roles.contains(role.name)
    }

    /**
     * Check if user has any of the specified roles
     */
    fun hasAnyRole(vararg roles: UserRole): Boolean {
        return roles.any { hasRole(it) }
    }

    /**
     * Check if user has all of the specified roles
     */
    fun hasAllRoles(vararg roles: UserRole): Boolean {
        return roles.all { hasRole(it) }
    }

    /**
     * Check if user has specific permission
     */
    fun hasPermission(permission: String): Boolean {
        return permissions.contains(permission)
    }

    /**
     * Check if token is expired
     */
    @OptIn(ExperimentalTime::class)
    val isExpired: Boolean
        get() = exp?.let { expMillis ->
            val currentMillis = Clock.System.now().toEpochMilliseconds()
            expMillis * 1000 < currentMillis
        } ?: false

    companion object {
        /**
         * Standard JWT claim names mapping
         */
        object Claims {
            const val USER_ID = "sub"              // Subject
            const val SESSION_ID = "session_id"    // Custom claim
            const val EMAIL = "email"              // Standard OIDC claim
            const val TENANT_ID = "organization_id"      // Custom claim - critical!
            const val FIRST_NAME = "given_name"    // Standard OIDC claim
            const val LAST_NAME = "family_name"    // Standard OIDC claim
            const val FULL_NAME = "name"           // Standard OIDC claim
            const val ROLES = "roles"              // Custom claim
            const val GROUPS = "groups"            // Alternative for roles
            const val PERMISSIONS = "permissions"  // Custom claim
            const val JTI = "jti"                  // JWT ID
            const val IAT = "iat"                  // Issued at
            const val EXP = "exp"                  // Expires at
            const val ISS = "iss"                  // Issuer
            const val AUD = "aud"                  // Audience
        }
    }
}
