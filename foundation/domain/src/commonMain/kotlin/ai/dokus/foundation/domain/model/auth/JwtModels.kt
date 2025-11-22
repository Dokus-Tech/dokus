package ai.dokus.foundation.domain.model.auth

import ai.dokus.foundation.domain.enums.Permission
import ai.dokus.foundation.domain.enums.SubscriptionTier
import ai.dokus.foundation.domain.enums.UserRole
import ai.dokus.foundation.domain.ids.OrganizationId
import ai.dokus.foundation.domain.ids.UserId
import kotlinx.serialization.Serializable

/**
 * JWT Claims data class shared between backend and frontend.
 * Maps to the JWT token payload structure.
 */
@Serializable
data class JwtClaims(
    val userId: UserId,
    val email: String,

    val organizations: List<OrganizationScope>,

    // Standard JWT claims
    val iat: Long,
    val exp: Long,
    val jti: String,
    val iss: String = "dokus",
    val aud: String = "dokus-api"
)

@Serializable
data class OrganizationScope(
    val organizationId: OrganizationId,
    val permissions: Set<Permission>,
    val subscriptionTier: SubscriptionTier,
    val role: UserRole?,
)

/**
 * Token validation status.
 */
enum class TokenStatus {
    VALID,
    EXPIRED,
    REFRESH_NEEDED,
    INVALID
}