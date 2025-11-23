package ai.dokus.foundation.network.resilient

import ai.dokus.foundation.domain.asbtractions.AuthManager
import ai.dokus.foundation.domain.asbtractions.TokenManager
import ai.dokus.foundation.domain.exceptions.DokusException

/**
 * Resiliency helper that handles auth errors by attempting a token refresh and retrying once.
 */
class AuthenticatedResilientDelegate<T : Any>(
    private val serviceProvider: () -> T,
    private val tokenManager: TokenManager,
    private val authManager: AuthManager
) {
    private val delegate = ResilientDelegate(serviceProvider)

    fun get(): T = delegate.get()

    suspend fun <R> withAuthRetry(block: suspend (T) -> R): R {
        val first = delegate.get()
        return try {
            block(first)
        } catch (t: Throwable) {
            val authError = t.findAuthError()
            if (authError != null) {
                val refreshed = runCatching { tokenManager.refreshToken() }.getOrNull()
                if (refreshed != null) {
                    delegate.resetCache()
                    return block(delegate.get())
                }
                runCatching { authManager.onAuthenticationFailed() }
            } else {
                delegate.resetCache()
                return block(delegate.get())
            }
            throw t
        }
    }

    private fun Throwable.findAuthError(): DokusException? {
        var current: Throwable? = this
        while (current != null) {
            when (current) {
                is DokusException.NotAuthenticated,
                is DokusException.TokenInvalid -> return current as DokusException
            }
            current = current.cause
        }
        return null
    }
}
