@file:OptIn(ExperimentalUuidApi::class)

package ai.dokus.foundation.ktor.security

import ai.dokus.foundation.domain.ids.OrganizationId
import ai.dokus.foundation.domain.ids.UserId
import ai.dokus.foundation.domain.model.AuthenticationInfo
import ai.dokus.foundation.domain.model.auth.JwtClaims
import ai.dokus.foundation.domain.model.auth.OrganizationClaimDto
import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import com.auth0.jwt.interfaces.Payload
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * JWT token validator for authentication.
 * Validates JWT tokens and extracts user claims.
 */
class JwtValidator(
    val secret: String,
    val envIssuer: String = "dokus-auth"
) {
    private val logger = LoggerFactory.getLogger(JwtValidator::class.java)
    private val algorithm = Algorithm.HMAC256(secret)
    private val json = Json { ignoreUnknownKeys = true }

    val issuer: String = "https://dokus.tech"

    val verifier: JWTVerifier = JWT.require(algorithm)
        .withIssuer(issuer)
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

            // Preferred: flat org_id claim if present
            val orgIdFromFlat: OrganizationId? = payload
                .getClaim(JwtClaims.CLAIM_ORGANIZATION_ID)
                .asString()
                ?.takeIf { it.isNotBlank() }
                ?.let { OrganizationId.parse(it) }

            // Legacy fallback: organizations claim (JSON string) → first org
            val orgsClaim = payload.getClaim(JwtClaims.CLAIM_ORGANIZATIONS).asString()
            val orgIdFromList: OrganizationId? = orgsClaim
                ?.takeIf { it.isNotBlank() }
                ?.let { runCatching { json.decodeFromString<List<OrganizationClaimDto>>(it) }.getOrNull() }
                ?.firstOrNull()
                ?.organizationId
                ?.let { OrganizationId(Uuid.parse(it)) }

            // We don't store user's name/roles in current JWT; derive minimal values
            val name = email.substringBefore('@', email)
            val roleClaim = payload.getClaim(JwtClaims.CLAIM_ROLE).asString()
            val roles: Set<String> = roleClaim?.let { setOf(it) } ?: emptySet()

            AuthenticationInfo(
                userId = UserId(userId),
                email = email,
                name = name,
                organizationId = orgIdFromFlat ?: orgIdFromList,
                roles = roles
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
