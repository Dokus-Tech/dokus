package tech.dokus.foundation.backend.security

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import com.auth0.jwt.interfaces.Payload
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.auth.AuthenticationInfo
import tech.dokus.domain.model.auth.JwtClaims
import tech.dokus.domain.model.auth.TenantClaimDto
import tech.dokus.domain.utils.json
import tech.dokus.foundation.backend.config.JwtConfig
import tech.dokus.foundation.backend.utils.loggerFor
import kotlin.uuid.Uuid

/**
 * JWT token validator for authentication.
 * Validates JWT tokens and extracts user claims.
 */
class JwtValidator(
    config: JwtConfig,
) {
    private val logger = loggerFor()
    private val algorithm = Algorithm.HMAC256(config.secret)

    val verifier: JWTVerifier = JWT.require(algorithm)
        .withIssuer(config.issuer)
        .withAudience(config.audience)
        .build()

    /**
     * Validates a JWT token and returns the decoded JWT.
     *
     * @param token The JWT token string to validate
     * @return DecodedJWT if valid, null if invalid
     */
    fun validate(token: String): DecodedJWT? {
        return try {
            verifier.verify(token)
        } catch (e: JWTVerificationException) {
            logger.debug("JWT validation failed: ${e.message}")
            null
        }
    }

    /**
     * Extracts user authentication info from a decoded JWT using our current claim set.
     *
     * @param jwt The decoded JWT token
     * @return AuthenticationInfo containing user details
     */
    fun extractAuthInfo(jwt: DecodedJWT): AuthenticationInfo? {
        return extractAuthInfoInternal(jwt)
    }

    /**
     * Extracts user authentication info from a JWT payload (used by Ktor JWT plugin).
     */
    fun extractAuthInfo(payload: Payload): AuthenticationInfo? {
        return extractAuthInfoInternal(payload)
    }

    private fun extractAuthInfoInternal(payload: Payload): AuthenticationInfo? {
        return try {
            val userId = payload.subject ?: return null
            val email = payload.getClaim(JwtClaims.CLAIM_EMAIL).asString() ?: return null

            // Preferred: flat tenant_id claim if present
            val tenantIdFromFlat: TenantId? = payload
                .getClaim(JwtClaims.CLAIM_TENANT_ID)
                .asString()
                ?.takeIf { it.isNotBlank() }
                ?.let { TenantId.parse(it) }

            // Legacy fallback: tenants claim (JSON string) → first tenant
            val tenantsClaim = payload.getClaim(JwtClaims.CLAIM_TENANTS).asString()
            val tenantIdFromList: TenantId? = tenantsClaim
                ?.takeIf { it.isNotBlank() }
                ?.let { runCatching { json.decodeFromString<List<TenantClaimDto>>(it) }.getOrNull() }
                ?.firstOrNull()
                ?.tenantId
                ?.let { TenantId(Uuid.parse(it)) }

            // We don't store user's name/roles in current JWT; derive minimal values
            val name = email.substringBefore('@', email)
            val roleClaim = payload.getClaim(JwtClaims.CLAIM_ROLE).asString()
            val roles: Set<String> = roleClaim?.let { setOf(it) } ?: emptySet()
            val sessionJti = payload.getClaim(JwtClaims.CLAIM_JTI).asString()

            AuthenticationInfo(
                userId = UserId(userId),
                email = email,
                name = name,
                tenantId = tenantIdFromFlat ?: tenantIdFromList,
                roles = roles,
                sessionJti = sessionJti
            )
        } catch (e: Exception) {
            logger.error("Failed to extract auth info from JWT payload: ${'$'}{e.message}", e)
            null
        }
    }

    /**
     * Validates a token and extracts authentication info in one call.
     *
     * @param token The JWT token string to validate
     * @return AuthenticationInfo if valid, null if invalid
     */
    fun validateAndExtract(token: String): AuthenticationInfo? {
        val jwt = validate(token) ?: return null
        return extractAuthInfo(jwt)
    }
}
