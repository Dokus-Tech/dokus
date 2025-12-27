package tech.dokus.foundation.ktor.configure

import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.model.auth.JwtClaims
import tech.dokus.foundation.ktor.security.AuthMethod
import tech.dokus.foundation.ktor.security.DokusPrincipal
import tech.dokus.foundation.ktor.security.JwtValidator
import tech.dokus.foundation.ktor.security.TokenBlacklistService
import tech.dokus.foundation.ktor.utils.loggerFor
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.response.respond
import org.koin.ktor.ext.inject

private val logger = loggerFor("JwtAuthentication")

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
    val tokenBlacklistService by inject<TokenBlacklistService>()

    install(Authentication) {
        jwt(AuthMethod.JWT) {
            realm = "Dokus API"

            verifier(jwtValidator.verifier)

            validate { credential ->
                // Check if token is blacklisted (by JTI)
                val jti = credential.payload.getClaim(JwtClaims.CLAIM_JTI).asString()
                if (jti != null) {
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

    logger.info("JWT authentication configured with token blacklist")
}