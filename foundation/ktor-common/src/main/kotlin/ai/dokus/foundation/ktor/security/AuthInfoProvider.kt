package ai.dokus.foundation.ktor.security

import ai.dokus.foundation.domain.exceptions.DokusException
import io.ktor.server.application.*
import io.ktor.server.request.*

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
         *
         * Note:
         * - Authentication should be enforced at the routing level using authenticate("jwt-auth").
         * - This provider intentionally does not parse JWTs itself. The JWT plugin is the
         *   single source of truth for principal creation. If no principal is present,
         *   the route is either unauthenticated or misconfigured.
         */
        operator fun invoke(call: ApplicationCall, jwtValidator: JwtValidator): AuthInfoProvider {
            return object : AuthInfoProvider {
                override suspend fun <T> withAuthInfo(block: suspend () -> T): T {
                    val token = call.request.authorization()?.substringAfter("Bearer ")
                    if (token.isNullOrEmpty()) throw DokusException.NotAuthenticated()
                    val authInfo = jwtValidator.validateAndExtract(token)
                    if (authInfo == null) throw DokusException.InvalidCredentials()

                    // Inject the auth info into a coroutine context
                    return withAuthContext(authInfo, block)
                }
            }
        }
    }
}
