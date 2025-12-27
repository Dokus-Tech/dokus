package tech.dokus.foundation.ktor.cache

import tech.dokus.foundation.ktor.config.CachingConfig
import java.time.Duration

/**
 * Redis cache client interface defining common cache operations
 */
interface RedisClient {
    /**
     * Get a value from cache
     * @param key The cache key
     * @return The cached value or null if not found
     */
    suspend fun get(key: String): String?

    /**
     * Set a value in cache
     * @param key The cache key
     * @param value The value to cache
     * @param ttl Time-to-live for the cache entry
     */
    suspend fun set(key: String, value: String, ttl: Duration? = null)

    /**
     * Set a value only if it doesn't exist
     * @param key The cache key
     * @param value The value to cache
     * @param ttl Time-to-live for the cache entry
     * @return true if the value was set, false if key already exists
     */
    suspend fun setIfAbsent(key: String, value: String, ttl: Duration? = null): Boolean

    /**
     * Delete a cache entry
     * @param key The cache key
     * @return true if the key was deleted, false if it didn't exist
     */
    suspend fun delete(key: String): Boolean

    /**
     * Delete multiple cache entries
     * @param keys The cache keys to delete
     * @return Number of keys deleted
     */
    suspend fun deleteMany(vararg keys: String): Long

    /**
     * Check if a key exists in cache
     * @param key The cache key
     * @return true if the key exists
     */
    suspend fun exists(key: String): Boolean

    /**
     * Set expiration time for a key
     * @param key The cache key
     * @param ttl Time-to-live
     * @return true if the expiration was set
     */
    suspend fun expire(key: String, ttl: Duration): Boolean

    /**
     * Get remaining time-to-live for a key
     * @param key The cache key
     * @return TTL in seconds, -2 if key doesn't exist, -1 if no expiry
     */
    suspend fun ttl(key: String): Long

    /**
     * Increment a numeric value
     * @param key The cache key
     * @param delta The increment amount (default 1)
     * @return The new value after increment
     */
    suspend fun increment(key: String, delta: Long = 1): Long

    /**
     * Decrement a numeric value
     * @param key The cache key
     * @param delta The decrement amount (default 1)
     * @return The new value after decrement
     */
    suspend fun decrement(key: String, delta: Long = 1): Long

    /**
     * Get multiple values at once
     * @param keys The cache keys
     * @return Map of keys to values (missing keys will not be in the map)
     */
    suspend fun getMany(vararg keys: String): Map<String, String>

    /**
     * Set multiple values at once
     * @param values Map of keys to values
     * @param ttl Time-to-live for all entries
     */
    suspend fun setMany(values: Map<String, String>, ttl: Duration? = null)

    /**
     * Get all keys matching a pattern
     * @param pattern The pattern to match (supports * and ? wildcards)
     * @return Set of matching keys
     */
    suspend fun keys(pattern: String): Set<String>

    /**
     * Clear all keys with the client's namespace
     * Use with caution!
     */
    suspend fun clear()

    /**
     * Ping the Redis server to check connectivity
     * @return true if server is reachable
     */
    suspend fun ping(): Boolean

    /**
     * Close the Redis connection
     */
    suspend fun close()
}

/**
 * DSL for creating Redis clients
 */
@RedisDsl
class RedisClientBuilder {
    lateinit var config: CachingConfig.RedisConfig
    var namespace: RedisNamespace = RedisNamespace.Auth

    fun build(): RedisClient = RedisClientFactory.createClient(config, namespace)
}

/**
 * DSL function to create a Redis client
 */
inline fun redis(block: RedisClientBuilder.() -> Unit): RedisClient =
    RedisClientBuilder().apply(block).build()