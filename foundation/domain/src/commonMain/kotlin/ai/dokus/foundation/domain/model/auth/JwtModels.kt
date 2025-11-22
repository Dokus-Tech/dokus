package ai.dokus.foundation.domain.model.auth

import ai.dokus.foundation.domain.enums.Permission
import ai.dokus.foundation.domain.enums.SubscriptionTier
import ai.dokus.foundation.domain.enums.UserRole
import ai.dokus.foundation.domain.ids.OrganizationId
import ai.dokus.foundation.domain.ids.UserId
import kotlinx.serialization.Serializable

@Serializable
data class JwtClaims(
    val userId: UserId,
    val email: String,
    val organizations: List<OrganizationScope>,
    val iat: Long,
    val exp: Long,
    val jti: String,
    val iss: String = ISS_DEFAULT,
    val aud: String = AUD_DEFAULT
) {
    companion object {
        const val CLAIM_SUB = "sub"
        const val CLAIM_EMAIL = "email"
        const val CLAIM_ORGANIZATIONS = "organizations"
        const val CLAIM_ORGANIZATION_ID = "org_id"
        const val CLAIM_PERMISSIONS = "permissions"
        const val CLAIM_SUBSCRIPTION_TIER = "tier"
        const val CLAIM_ROLE = "role"
        const val CLAIM_IAT = "iat"
        const val CLAIM_EXP = "exp"
        const val CLAIM_JTI = "jti"
        const val CLAIM_ISS = "iss"
        const val CLAIM_AUD = "aud"

        const val ISS_DEFAULT = "dokus"
        const val AUD_DEFAULT = "dokus-api"

        const val ACCESS_TOKEN_EXPIRY_SECONDS = 3600L // 1 hour
        const val REFRESH_TOKEN_EXPIRY_DAYS = 30L
        const val REFRESH_THRESHOLD_SECONDS = 5 * 60 // 5 minutes before expiry

        const val CLAIM_TYPE = "type"
        const val TOKEN_TYPE_REFRESH = "refresh"
    }
}

@Serializable
data class OrganizationScope(
    val organizationId: OrganizationId,
    val permissions: Set<Permission>,
    val subscriptionTier: SubscriptionTier,
    val role: UserRole?,
)

@Serializable
data class OrganizationClaimDto(
    val organizationId: String,
    val permissions: List<String>,
    val subscriptionTier: String,
    val role: String? = null
)

enum class TokenStatus {
    VALID,
    EXPIRED,
    REFRESH_NEEDED,
    INVALID
}