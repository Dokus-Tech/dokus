package ai.dokus.foundation.ktor.security

import ai.dokus.foundation.ktor.auth.AuthenticationInfoPrincipal
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import org.koin.ktor.ext.get

/**
 * Provider interface for accessing authentication information in RPC services.
 * This extracts the AuthenticationInfo from the Ktor principal and injects it
 * into the coroutine context for use by service implementations.
 */
interface AuthInfoProvider {
    suspend fun <T> withAuthInfo(block: suspend () -> T): T

    companion object {
        /**
         * Creates an AuthInfoProvider from an ApplicationCall.
         * Extracts the AuthenticationInfoPrincipal set by JWT authentication.
         */
        operator fun invoke(call: ApplicationCall): AuthInfoProvider = object : AuthInfoProvider {
            override suspend fun <T> withAuthInfo(block: suspend () -> T): T {
                // Prefer our custom principal produced by JwtAuth
                val authPrincipal = call.principal<AuthenticationInfoPrincipal>()
                if (authPrincipal != null) {
                    return withAuthContext(authPrincipal.authInfo, block)
                }

                // Fallback 1: if only JWTPrincipal is present (e.g., plugin validated but principal mapping failed),
                // derive AuthenticationInfo from payload using JwtValidator from DI
                val jwtPrincipal = call.principal<JWTPrincipal>()
                if (jwtPrincipal != null) {
                    val jwtValidator = call.application.get<JwtValidator>()
                    jwtValidator.extractAuthInfo(jwtPrincipal.payload)?.let { derived ->
                        return withAuthContext(derived, block)
                    }
                }

                // Fallback 2: parse Authorization header directly if present
                val authHeader = call.request.headers[HttpHeaders.Authorization]
                if (authHeader?.startsWith("Bearer ") == true) {
                    val token = authHeader.removePrefix("Bearer ").trim()
                    if (token.isNotEmpty()) {
                        val jwtValidator = call.application.get<JwtValidator>()
                        jwtValidator.validateAndExtract(token)?.let { derived ->
                            return withAuthContext(derived, block)
                        }
                    }
                }

                throw IllegalStateException(
                    "No authentication info found in the request context"
                )
            }
        }
    }
}
