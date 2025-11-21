package ai.dokus.foundation.ktor.security

import ai.dokus.foundation.domain.model.AuthenticationInfo
import ai.dokus.foundation.ktor.auth.AuthenticationInfoPrincipal
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.principal

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
                val principal = call.principal<AuthenticationInfoPrincipal>()
                    ?: throw IllegalStateException("No authentication info found in the request context")

                // Inject the auth info into coroutine context
                return withAuthContext(principal.authInfo, block)
            }
        }
    }
}
