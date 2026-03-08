@file:OptIn(ExperimentalUuidApi::class)

package tech.dokus.foundation.backend.security

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import com.auth0.jwt.interfaces.Payload
import kotlinx.serialization.builtins.ListSerializer
import tech.dokus.domain.ids.SessionId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.auth.AuthenticationInfo
import tech.dokus.domain.model.auth.JwtClaims
import tech.dokus.domain.model.auth.JwtFirmMembershipClaim
import tech.dokus.domain.model.auth.JwtTenantMembershipClaim
import tech.dokus.domain.utils.json
import tech.dokus.foundation.backend.config.JwtConfig
import tech.dokus.foundation.backend.utils.loggerFor
import kotlin.uuid.ExperimentalUuidApi

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
            val tenantMemberships = payload.getClaim(JwtClaims.CLAIM_TENANTS)
                .asString()
                ?.let {
                    runCatching {
                        json.decodeFromString(
                            ListSerializer(JwtTenantMembershipClaim.serializer()),
                            it
                        )
                    }.getOrDefault(emptyList())
                }
                ?: emptyList()
            val firmMemberships = payload.getClaim(JwtClaims.CLAIM_FIRMS)
                .asString()
                ?.let {
                    runCatching {
                        json.decodeFromString(
                            ListSerializer(JwtFirmMembershipClaim.serializer()),
                            it
                        )
                    }.getOrDefault(emptyList())
                }
                ?: emptyList()

            // We don't store user's name/roles in current JWT; derive minimal values
            val name = email.substringBefore('@', email)
            val sessionId = payload.getClaim(JwtClaims.CLAIM_SESSION_ID)
                .asString()
                ?.let { runCatching { SessionId(it) }.getOrNull() }
            val sessionJti = payload.getClaim(JwtClaims.CLAIM_JTI).asString()

            AuthenticationInfo(
                userId = UserId(userId),
                email = email,
                name = name,
                globalRoles = emptySet(),
                tenantMemberships = tenantMemberships,
                firmMemberships = firmMemberships,
                sessionId = sessionId,
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
