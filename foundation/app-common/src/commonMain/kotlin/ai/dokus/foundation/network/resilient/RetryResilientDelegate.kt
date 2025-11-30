package ai.dokus.foundation.network.resilient

import ai.dokus.foundation.domain.exceptions.DokusException

/**
 * Auth error class names that should NOT be retried.
 */
private val nonRetryableAuthErrors = setOf(
    "NotAuthenticated",
    "TokenInvalid",
    "TokenExpired",
    "RefreshTokenExpired",
    "RefreshTokenRevoked",
    "SessionExpired",
    "SessionInvalid",
    "InvalidCredentials"
)

/**
 * Generic resiliency helper that caches a service instance and retries once
 * with a fresh instance on failure. Use this to wrap RPC service calls.
 */
class RetryResilientDelegate<T : Any>(
    private val serviceProvider: () -> T
) : RemoteServiceDelegate<T> {
    @PublishedApi
    internal var cached: T? = null

    override fun get(): T = cached ?: serviceProvider().also { cached = it }

    @PublishedApi
    internal fun reset() {
        cached = null
    }

    fun resetCache() {
        reset()
    }

    override suspend fun <R> call(block: suspend (T) -> R): R {
        val first = get()
        return try {
            block(first)
        } catch (t: Throwable) {
            // Don't retry auth errors - they won't succeed on retry
            if (t.isAuthError()) {
                throw t
            }
            // Reset and retry once with a fresh instance for transient errors
            reset()
            val second = get()
            block(second)
        }
    }

    private fun Throwable.isAuthError(): Boolean {
        var current: Throwable? = this
        val visited = mutableSetOf<Throwable>()
        while (current != null && current !in visited) {
            visited.add(current)
            // Check by type
            if (current is DokusException.NotAuthenticated ||
                current is DokusException.TokenInvalid ||
                current is DokusException.TokenExpired ||
                current is DokusException.RefreshTokenExpired ||
                current is DokusException.RefreshTokenRevoked ||
                current is DokusException.SessionExpired ||
                current is DokusException.SessionInvalid ||
                current is DokusException.InvalidCredentials) {
                return true
            }
            // Check by class name (for KRPC DeserializedException)
            val className = current::class.simpleName ?: ""
            val message = current.message ?: ""
            if (className in nonRetryableAuthErrors ||
                nonRetryableAuthErrors.any { message.startsWith("$it(") }) {
                return true
            }
            current = current.cause
        }
        return false
    }
}
