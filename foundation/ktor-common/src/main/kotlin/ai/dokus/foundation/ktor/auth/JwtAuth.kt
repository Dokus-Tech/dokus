@file:OptIn(ExperimentalUuidApi::class)

package ai.dokus.foundation.ktor.auth

import ai.dokus.foundation.domain.ids.OrganizationId
import ai.dokus.foundation.domain.ids.UserId
import ai.dokus.foundation.domain.model.AuthenticationInfo
import ai.dokus.foundation.ktor.security.JwtValidator
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import org.slf4j.LoggerFactory
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private val logger = LoggerFactory.getLogger("ServiceAuthConfig")

/**
 * Configures JWT authentication using Ktor's built-in JWT auth plugin.
 *
 * This function sets up:
 * 1. JWT token verification using the shared secret
 * 2. Token validation and extraction of authentication info
 * 3. Storage of AuthenticationInfo in call principal
 *
 * The authentication is performed locally using JwtValidator - no RPC calls are made.
 * This is fast and efficient for inter-service authentication.
 *
 * @param jwtValidator The JWT validator with configured secret and issuer
 * @param providerName Optional name for the JWT authentication provider (default: "jwt-auth")
 */
fun Application.configureJwtAuth(
    jwtValidator: JwtValidator,
    providerName: String = "jwt-auth"
) {
    install(Authentication) {
        jwt(providerName) {
            // Set the realm for WWW-Authenticate header
            realm = jwtValidator.issuer

            // Configure the JWT verifier
            verifier(
                JWT.require(Algorithm.HMAC256(jwtValidator.secret))
                    .withIssuer(jwtValidator.issuer)
                    .build()
            )

            // Validate JWT and extract authentication info using JwtValidator
            validate { credential ->
                try {
                    val authInfo = jwtValidator.extractAuthInfo(credential.payload)
                    if (authInfo != null) {
                        logger.debug(
                            "JWT validated for user: {}, tenant: {}",
                            authInfo.userId.value,
                            authInfo.organizationId.value
                        )
                        AuthenticationInfoPrincipal(authInfo)
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    logger.error("Error extracting auth info from JWT", e)
                    null
                }
            }

            // Challenge configuration for authentication failures
            challenge { _, _ ->
                // No custom challenge response needed - use Ktor's default 401
            }
        }
    }

    logger.info("JWT authentication configured with provider: $providerName")
}

/**
 * Principal that wraps AuthenticationInfo for Ktor's auth system.
 * This allows AuthenticationInfo to be used as a Ktor Principal.
 */
data class AuthenticationInfoPrincipal(
    val authInfo: AuthenticationInfo
) : io.ktor.server.auth.Principal
