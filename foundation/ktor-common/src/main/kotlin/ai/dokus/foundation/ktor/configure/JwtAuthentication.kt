package ai.dokus.foundation.ktor.configure

import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.model.auth.JwtClaims
import ai.dokus.foundation.ktor.config.JwtConfig
import ai.dokus.foundation.ktor.security.AuthMethod
import ai.dokus.foundation.ktor.security.DokusPrincipal
import ai.dokus.foundation.ktor.security.JwtValidator
import ai.dokus.foundation.ktor.security.TokenBlacklistService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.response.respond
import org.koin.ktor.ext.inject
import org.koin.ktor.ext.getKoin
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("JwtAuthentication")

/**
 * Configures JWT authentication for the application.
 * Uses Ktor's built-in JWT authentication with [DokusPrincipal] as the principal type.
 *
 * After configuration, authenticated routes can access the principal via:
 * ```
 * val principal = call.principal<DokusPrincipal>()
 * ```
 *
 * Or using the helper extension:
 * ```
 * val principal = dokusPrincipal  // throws if not authenticated
 * ```
 */
fun Application.configureJwtAuthentication() {
    val jwtValidator by inject<JwtValidator>()
    // TokenBlacklistService is optional - if not registered, blacklist checks are skipped
    val tokenBlacklistService = runCatching { getKoin().getOrNull<TokenBlacklistService>() }.getOrNull()

    install(Authentication) {
        jwt(AuthMethod.JWT) {
            realm = "Dokus API"

            verifier(jwtValidator.verifier)

            validate { credential ->
                // Check if token is blacklisted (by JTI)
                val jti = credential.payload.getClaim(JwtClaims.CLAIM_JTI).asString()
                if (jti != null && tokenBlacklistService != null) {
                    if (tokenBlacklistService.isBlacklisted(jti)) {
                        logger.debug("Rejected blacklisted token with JTI: $jti")
                        return@validate null
                    }
                }

                // Extract authentication info from the validated JWT
                val authInfo = jwtValidator.extractAuthInfo(credential.payload)
                if (authInfo == null) {
                    logger.debug("Failed to extract auth info from JWT payload")
                    return@validate null
                }

                // Return DokusPrincipal (implements Ktor's Principal interface)
                DokusPrincipal.fromAuthInfo(authInfo)
            }

            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    DokusException.NotAuthenticated("Authentication required")
                )
            }
        }
    }

    logger.info("JWT authentication configured${if (tokenBlacklistService != null) " with token blacklist" else ""}")
}

/**
 * Configures JWT authentication with a custom JwtConfig.
 * Useful when you need to provide configuration explicitly.
 */
fun Application.configureJwtAuthentication(
    jwtConfig: JwtConfig,
    tokenBlacklistService: TokenBlacklistService? = null
) {
    val jwtValidator = JwtValidator(jwtConfig)

    install(Authentication) {
        jwt(AuthMethod.JWT) {
            realm = jwtConfig.realm

            verifier(jwtValidator.verifier)

            validate { credential ->
                // Check if token is blacklisted (by JTI)
                val jti = credential.payload.getClaim(JwtClaims.CLAIM_JTI).asString()
                if (jti != null && tokenBlacklistService != null) {
                    if (tokenBlacklistService.isBlacklisted(jti)) {
                        logger.debug("Rejected blacklisted token with JTI: $jti")
                        return@validate null
                    }
                }

                val authInfo = jwtValidator.extractAuthInfo(credential.payload)
                if (authInfo == null) {
                    logger.debug("Failed to extract auth info from JWT payload")
                    return@validate null
                }

                DokusPrincipal.fromAuthInfo(authInfo)
            }

            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    DokusException.NotAuthenticated("Authentication required")
                )
            }
        }
    }

    logger.info("JWT authentication configured with custom config${if (tokenBlacklistService != null) " and token blacklist" else ""}")
}
