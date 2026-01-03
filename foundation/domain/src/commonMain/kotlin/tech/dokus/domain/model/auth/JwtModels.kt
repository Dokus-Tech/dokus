package tech.dokus.domain.model.auth

import kotlinx.serialization.Serializable
import tech.dokus.domain.enums.Permission
import tech.dokus.domain.enums.SubscriptionTier
import tech.dokus.domain.enums.UserRole
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId

@Serializable
data class JwtClaims(
    val userId: UserId,
    val email: String,
    val tenant: TenantScope?,
    val iat: Long,
    val exp: Long,
    val jti: String,
    val iss: String = ISS_DEFAULT,
    val aud: String = AUD_DEFAULT
) {
    companion object {
        const val CLAIM_SUB = "sub"
        const val CLAIM_EMAIL = "email"
        const val CLAIM_TENANTS = "tenants"
        const val CLAIM_TENANT_ID = "tenant_id"
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
data class TenantScope(
    val tenantId: TenantId,
    val permissions: Set<Permission>,
    val subscriptionTier: SubscriptionTier,
    val role: UserRole?,
)

@Serializable
data class TenantClaimDto(
    val tenantId: String,
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
