package tech.dokus.domain.model.auth

import kotlinx.serialization.Serializable
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId

/**
 * Authentication information extracted from a validated JWT token.
 * This represents the authenticated user's identity and permissions.
 *
 * @property userId The unique user identifier
 * @property email The user's email address
 * @property name The user's full name
 * @property tenantId The tenant identifier
 * @property roles Set of roles assigned to the user
 */
@Serializable
data class AuthenticationInfo(
    val userId: UserId,
    val email: String,
    val name: String,
    val tenantId: TenantId?,
    val roles: Set<String>
)
