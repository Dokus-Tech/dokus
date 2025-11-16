package ai.dokus.foundation.domain.model

import ai.dokus.foundation.domain.BusinessUserId
import ai.dokus.foundation.domain.TenantId
import kotlinx.serialization.Serializable

/**
 * Authentication information extracted from a validated JWT token.
 * This represents the authenticated user's identity and permissions.
 *
 * @property userId The unique user identifier
 * @property email The user's email address
 * @property name The user's full name
 * @property tenantId The tenant/organization identifier
 * @property roles Set of roles assigned to the user
 */
@Serializable
data class AuthenticationInfo(
    val userId: BusinessUserId,
    val email: String,
    val name: String,
    val tenantId: TenantId,
    val roles: Set<String>
)
