package ai.dokus.foundation.network.resilient

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
            println(t)
            // Reset and retry once with a fresh instance
            reset()
            val second = get()
            block(second)
        }
    }
}
