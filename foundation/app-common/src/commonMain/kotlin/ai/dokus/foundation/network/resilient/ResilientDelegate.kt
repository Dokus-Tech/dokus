package ai.dokus.foundation.network.resilient

/**
 * Generic resiliency helper that caches a service instance and retries once
 * with a fresh instance on failure. Use this to wrap RPC service calls.
 */
class ResilientDelegate<T : Any>(
    private val serviceProvider: () -> T
) {
    @PublishedApi
    internal var cached: T? = null

    fun get(): T = cached ?: serviceProvider().also { cached = it }

    @PublishedApi
    internal fun reset() { cached = null }

    suspend inline fun <R> withRetry(crossinline block: suspend (T) -> R): R {
        val first = get()
        return try {
            block(first)
        } catch (t: Throwable) {
            // Reset and retry once with a fresh instance
            reset()
            val second = get()
            block(second)
        }
    }
}
