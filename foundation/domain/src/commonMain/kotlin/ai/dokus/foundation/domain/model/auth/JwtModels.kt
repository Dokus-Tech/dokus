package ai.dokus.foundation.domain.model.auth

import kotlinx.serialization.Serializable

/**
 * JWT Claims data class shared between backend and frontend.
 * Maps to the JWT token payload structure.
 */
@Serializable
data class JwtClaims(
    val userId: String,
    val tenantId: String,
    val matricule: String? = null,
    val email: String,
    val fullName: String,
    val roles: Set<String> = emptySet(),
    val permissions: Set<String> = emptySet(),
    val unitCode: String? = null,
    val department: String? = null,
    val clearanceLevel: String = "INTERNAL_USE",
    val sessionId: String? = null,
    val deviceFingerprint: String? = null,
    // Standard JWT claims
    val iat: Long? = null, // issued at
    val exp: Long? = null, // expires at
    val jti: String? = null // JWT ID
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