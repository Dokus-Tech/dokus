@file:OptIn(ExperimentalUuidApi::class)

package ai.dokus.foundation.ktor.security

import ai.dokus.foundation.domain.TenantId
import ai.dokus.foundation.domain.UserId
import ai.dokus.foundation.domain.model.AuthenticationInfo
import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import org.slf4j.LoggerFactory
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * JWT token validator for authentication.
 * Validates JWT tokens and extracts user claims.
 */
class JwtValidator(
    val secret: String,
    val issuer: String = "dokus-auth"
) {
    private val logger = LoggerFactory.getLogger(JwtValidator::class.java)
    private val algorithm = Algorithm.HMAC256(secret)

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
     * Extracts user authentication info from a decoded JWT.
     *
     * @param jwt The decoded JWT token
     * @return AuthenticationInfo containing user details
     */
    fun extractAuthInfo(jwt: DecodedJWT): AuthenticationInfo? {
        return try {
            val userId = jwt.subject ?: return null
            val email = jwt.getClaim("email").asString() ?: return null
            val name = jwt.getClaim("name").asString() ?: return null
            val tenantId = jwt.getClaim("tenant_id").asString() ?: return null
            val roles = jwt.getClaim("groups").asList(String::class.java) ?: emptyList()

            AuthenticationInfo(
                userId = UserId(userId),
                email = email,
                name = name,
                tenantId = TenantId(Uuid.parse(tenantId)),
                roles = roles.toSet()
            )
        } catch (e: Exception) {
            logger.error("Failed to extract auth info from JWT: ${e.message}", e)
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
