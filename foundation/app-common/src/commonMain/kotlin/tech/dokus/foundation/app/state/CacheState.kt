package tech.dokus.foundation.app.state

/**
 * Represents the state of cached data with network sync status.
 *
 * Used for local-first data loading where cached data is shown immediately,
 * then refreshed from the network. The UI can react to each state appropriately.
 *
 * State flow:
 * - [Empty] -> [Loading] -> [Fresh] (first load, no cache)
 * - [Cached] -> [Refreshing] -> [Fresh] (cache hit, successful refresh)
 * - [Cached] -> [Refreshing] -> [Stale] (cache hit, refresh failed)
 *
 * Timestamps are stored as epoch milliseconds (Long) for simplicity.
 */
sealed class CacheState<out T> {

    /**
     * Fresh data just loaded from the network.
     * This is the ideal state - data is current and reliable.
     *
     * @param fetchedAtMillis Epoch milliseconds when data was fetched
     */
    data class Fresh<T>(
        val data: T,
        val fetchedAtMillis: Long
    ) : CacheState<T>()

    /**
     * Data loaded from local cache.
     * May be outdated but available for immediate display.
     *
     * @param cachedAtMillis Epoch milliseconds when data was cached (null if unknown)
     */
    data class Cached<T>(
        val data: T,
        val cachedAtMillis: Long?
    ) : CacheState<T>()

    /**
     * Currently fetching fresh data from network.
     * May contain stale data to display while loading.
     */
    data class Refreshing<T>(
        val staleData: T?
    ) : CacheState<T>()

    /**
     * Network refresh failed, showing cached data.
     * Contains the error for display/logging.
     *
     * @param cachedAtMillis Epoch milliseconds when data was cached (null if unknown)
     */
    data class Stale<T>(
        val data: T,
        val cachedAtMillis: Long?,
        val error: Throwable
    ) : CacheState<T>()

    /**
     * No cached data available.
     * Either loading for the first time or cache was cleared.
     */
    data class Empty(
        val isLoading: Boolean = false
    ) : CacheState<Nothing>()

    /**
     * Returns the data if available, regardless of freshness.
     */
    fun dataOrNull(): T? = when (this) {
        is Fresh -> data
        is Cached -> data
        is Refreshing -> staleData
        is Stale -> data
        is Empty -> null
    }

    /**
     * Returns true if currently fetching from network.
     */
    fun isRefreshing(): Boolean = when (this) {
        is Refreshing -> true
        is Empty -> isLoading
        else -> false
    }

    /**
     * Returns true if data is available (fresh, cached, or stale).
     */
    fun hasData(): Boolean = dataOrNull() != null

    /**
     * Returns true if the data may be outdated.
     */
    fun isStale(): Boolean = when (this) {
        is Stale -> true
        is Cached -> true
        else -> false
    }

    /**
     * Returns the error if refresh failed.
     */
    fun errorOrNull(): Throwable? = when (this) {
        is Stale -> error
        else -> null
    }

    companion object {
        /**
         * Create an empty state (no cache, not loading).
         */
        fun <T> empty(): CacheState<T> = Empty(isLoading = false)

        /**
         * Create a loading state (no cache, loading in progress).
         */
        fun <T> loading(): CacheState<T> = Empty(isLoading = true)

        /**
         * Create a refreshing state with optional stale data.
         */
        fun <T> refreshing(staleData: T? = null): CacheState<T> = Refreshing(staleData)
    }
}

/**
 * Transform the data inside a CacheState while preserving the state type.
 */
inline fun <T, R> CacheState<T>.map(transform: (T) -> R): CacheState<R> = when (this) {
    is CacheState.Fresh -> CacheState.Fresh(transform(data), fetchedAtMillis)
    is CacheState.Cached -> CacheState.Cached(transform(data), cachedAtMillis)
    is CacheState.Refreshing -> CacheState.Refreshing(staleData?.let(transform))
    is CacheState.Stale -> CacheState.Stale(transform(data), cachedAtMillis, error)
    is CacheState.Empty -> this
}

/**
 * Execute a block when data is available.
 */
inline fun <T> CacheState<T>.onData(block: (T) -> Unit): CacheState<T> {
    dataOrNull()?.let(block)
    return this
}

/**
 * Execute a block when refresh failed.
 */
inline fun <T> CacheState<T>.onError(block: (Throwable) -> Unit): CacheState<T> {
    errorOrNull()?.let(block)
    return this
}
