package ai.dokus.foundation.ktor.security

import ai.dokus.foundation.domain.model.AuthenticationInfo
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authentication

interface AuthInfoProvider {
    suspend fun <T> withAuthInfo(block: suspend () -> T): T

    companion object {
        operator fun invoke(call: ApplicationCall): AuthInfoProvider = object : AuthInfoProvider {
            override suspend fun <T> withAuthInfo(block: suspend () -> T): T {
                val auth = call.authentication.principal<AuthenticationInfo>()
                if (auth != null) {
                    return withAuthContext(auth, block)
                } else {
                    throw IllegalStateException("No authentication info found in the request context")
                }
            }
        }
    }
}
