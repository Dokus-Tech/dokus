package ai.dokus.foundation.network.resilient

import ai.dokus.foundation.domain.asbtractions.AuthManager
import ai.dokus.foundation.domain.asbtractions.TokenManager
import ai.dokus.foundation.domain.exceptions.DokusException

fun <T : Any> createRetryDelegate(serviceProvider: () -> T) = RetryResilientDelegate(serviceProvider)

fun <T : Any> RetryResilientDelegate<T>.withAuth(
    tokenManager: TokenManager,
    authManager: AuthManager
): RemoteServiceDelegate<T> = AuthenticatedResilientDelegate(
    serviceProvider = { get() },
    tokenManager = tokenManager,
    authManager = authManager
)

class AuthenticatedResilientDelegate<T : Any>(
    serviceProvider: () -> T,
    private val tokenManager: TokenManager,
    private val authManager: AuthManager
) : RemoteServiceDelegate<T> {
    private val retryDelegate = RetryResilientDelegate(serviceProvider)

    override fun get(): T = retryDelegate.get()

    override suspend fun <R> call(block: suspend (T) -> R): R {
        val first = retryDelegate.get()
        return try {
            block(first)
        } catch (t: Throwable) {
            val authError = t.findAuthError()
            if (authError != null) {
                val refreshed = runCatching { tokenManager.refreshToken() }.getOrNull()
                if (refreshed != null) {
                    retryDelegate.resetCache()
                    return block(retryDelegate.get())
                }
                runCatching {
                    tokenManager.onAuthenticationFailed()
                    authManager.onAuthenticationFailed()
                }
            } else {
                retryDelegate.resetCache()
                return block(retryDelegate.get())
            }
            throw t
        }
    }

    private fun Throwable.findAuthError(): DokusException? {
        val visited = mutableSetOf<Throwable>()
        var current: Throwable? = this
        while (current != null && current !in visited) {
            visited.add(current)
            // Direct type check
            when (current) {
                is DokusException.NotAuthenticated,
                is DokusException.TokenInvalid -> return current
            }
            // Fallback: check class name for KRPC DeserializedException wrapper
            val className = current::class.simpleName ?: ""
            val message = current.message ?: ""
            if (className == "NotAuthenticated" || className == "TokenInvalid" ||
                message.startsWith("NotAuthenticated(") || message.startsWith("TokenInvalid(")) {
                // Return a synthetic DokusException for auth error handling
                return DokusException.NotAuthenticated(message)
            }
            current = current.cause
        }
        return null
    }
}
