package ai.dokus.foundation.domain.model.auth

import ai.dokus.foundation.domain.OrganizationId
import ai.dokus.foundation.domain.TenantId
import ai.dokus.foundation.domain.UserId
import ai.dokus.foundation.domain.enums.Permission
import ai.dokus.foundation.domain.enums.SubscriptionTier
import ai.dokus.foundation.domain.enums.UserRole
import kotlinx.serialization.Serializable

/**
 * JWT Claims data class shared between backend and frontend.
 * Maps to the JWT token payload structure.
 *
 * This JWT represents access to ONE SPECIFIC TENANT and ONE SPECIFIC ORGANIZATION within that tenant.
 */
@Serializable
data class JwtClaims(
    // User identity
    val userId: UserId,
    val email: String,

    // Tenant context - THIS JWT IS FOR ONE SPECIFIC TENANT
    val tenantId: TenantId,
    val organizationId: OrganizationId, // Company/business entity within tenant
    val organizationName: String, // For UI display

    // Permissions for THIS organization only
    val role: UserRole, // Role in THIS organization
    val permissions: Set<Permission>, // What user can do in THIS org

    // Belgian context
    val matricule: String? = null, // This organization's business number
    val locale: String = "nl-BE", // Full locale code (e.g., "nl-BE", "fr-BE", "en-US")

    // Subscription tier of THIS organization
    val subscriptionTier: SubscriptionTier,
    val featureFlags: Set<String> = emptySet(),

    // Accountant context (if applicable)
    val isAccountantAccess: Boolean = false, // True if accessing client's org
    val accountantOrganizationId: OrganizationId? = null, // Accountant's own org ID

    // Standard JWT claims
    val iat: Long,
    val exp: Long,
    val jti: String,
    val iss: String = "dokus",
    val aud: String = "dokus-api"
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