package be.police.pulse.foundation.ktor.cache

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import java.time.Duration
import kotlin.time.toJavaDuration
import kotlin.time.Duration as KotlinDuration

/**
 * Extension functions for Redis operations
 * More Kotlin-idiomatic approach
 */

// ============== Conversion Extensions ==============

/**
 * Convert Kotlin Duration to Java Duration
 */
fun KotlinDuration.toJava(): Duration = this.toJavaDuration()

/**
 * Convert Java Duration to Kotlin Duration
 */
fun Duration.toKotlin(): KotlinDuration = KotlinDuration.parse(this.toString())

// ============== Cache Extensions ==============

/**
 * Cache a suspend function result
 */
suspend inline fun <reified T> RedisClient.cache(
    key: String,
    ttl: Duration = Duration.ofMinutes(5),
    crossinline producer: suspend () -> T
): T = with(CacheSerializer) {
    getTyped<T>(key) ?: producer().also { setTyped(key, it, ttl) }
}

/**
 * Cache with custom serialization
 */
suspend fun <T> RedisClient.cacheWith(
    key: String,
    serializer: (T) -> String,
    deserializer: (String) -> T,
    ttl: Duration = Duration.ofMinutes(5),
    producer: suspend () -> T
): T {
    val cached = get(key)?.let(deserializer)
    return cached ?: producer().also {
        set(key, serializer(it), ttl)
    }
}

// ============== Rate Limiting Extensions ==============

/**
 * Rate limiting with sliding window
 */
suspend fun RedisClient.rateLimit(
    identifier: String,
    maxRequests: Int,
    window: Duration
): Boolean {
    val key = CacheKeyBuilder.forRateLimit("api", identifier)
    val count = increment(key)

    if (count == 1L) {
        expire(key, window)
    }

    return count <= maxRequests
}

/**
 * Check remaining rate limit
 */
suspend fun RedisClient.getRateLimitInfo(
    identifier: String,
    maxRequests: Int
): RateLimitInfo {
    val key = CacheKeyBuilder.forRateLimit("api", identifier)
    val count = get(key)?.toLongOrNull() ?: 0L
    val ttlSeconds = ttl(key)

    return RateLimitInfo(
        remaining = (maxRequests - count).coerceAtLeast(0),
        resetInSeconds = if (ttlSeconds > 0) ttlSeconds else 0,
        limit = maxRequests
    )
}

@Serializable
data class RateLimitInfo(
    val remaining: Long,
    val resetInSeconds: Long,
    val limit: Int
)

// ============== Locking Extensions ==============

/**
 * Try to acquire a lock with automatic release
 */
suspend inline fun <T> RedisClient.withLock(
    resource: String,
    ttl: Duration = Duration.ofSeconds(30),
    retries: Int = 0,
    retryDelay: KotlinDuration = KotlinDuration.parse("100ms"),
    block: suspend () -> T
): T? {
    val lockKey = CacheKeyBuilder.forLock(resource)

    repeat(retries + 1) { attempt ->
        if (setIfAbsent(lockKey, "locked", ttl)) {
            try {
                return block()
            } finally {
                delete(lockKey)
            }
        }

        if (attempt < retries) {
            delay(retryDelay)
        }
    }

    return null
}

// ============== Collection Extensions ==============

/**
 * Get all values matching a pattern as a Flow
 */
fun RedisClient.keysFlow(pattern: String): Flow<String> = flow {
    keys(pattern).forEach { emit(it) }
}

/**
 * Get values for keys as a Flow
 */
fun RedisClient.valuesFlow(vararg keys: String): Flow<Pair<String, String?>> = flow {
    keys.forEach { key ->
        emit(key to get(key))
    }
}

// ============== Session Extensions ==============

/**
 * Session data class
 */
@Serializable
data class SessionData(
    val userId: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastActivity: Long = System.currentTimeMillis(),
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Create or update a session
 */
suspend fun RedisClient.createSession(
    sessionId: String,
    data: SessionData,
    ttl: Duration = Duration.ofHours(8)
) = with(CacheSerializer) {
    val key = CacheKeyBuilder.forSession(sessionId)
    setTyped(key, data, ttl)
}

/**
 * Get and refresh session (sliding expiration)
 */
suspend fun RedisClient.getSession(
    sessionId: String,
    slidingTtl: Duration? = Duration.ofHours(8)
): SessionData? = with(CacheSerializer) {
    val key = CacheKeyBuilder.forSession(sessionId)
    val session = getTyped<SessionData>(key)

    if (session != null && slidingTtl != null) {
        // Update last activity and refresh TTL
        val updated = session.copy(lastActivity = System.currentTimeMillis())
        setTyped(key, updated, slidingTtl)
        return updated
    }

    return session
}

/**
 * Destroy a session
 */
suspend fun RedisClient.destroySession(sessionId: String): Boolean {
    val key = CacheKeyBuilder.forSession(sessionId)
    return delete(key)
}

// ============== Pub/Sub Extensions (if needed) ==============

/**
 * Simple pub/sub pattern using Redis lists
 */
suspend fun RedisClient.publish(channel: String, message: String) {
    val key = "pubsub:$channel"
    set("$key:${System.currentTimeMillis()}", message, Duration.ofMinutes(1))
}

/**
 * Poll messages from a channel
 */
suspend fun RedisClient.poll(channel: String, since: Long = 0): List<String> {
    val pattern = "pubsub:$channel:*"
    return keys(pattern)
        .filter { it.substringAfterLast(":").toLongOrNull() ?: 0 > since }
        .sortedBy { it.substringAfterLast(":").toLongOrNull() ?: 0 }
        .mapNotNull { get(it) }
}

// ============== Monitoring Extensions ==============

/**
 * Health check for Redis connection
 */
suspend fun RedisClient.isHealthy(): Boolean = try {
    ping()
} catch (e: Exception) {
    false
}

/**
 * Get Redis statistics for a namespace
 */
suspend fun RedisClient.getStats(namespace: String? = null): RedisStats {
    val pattern = namespace?.let { "$it:*" } ?: "*"
    val allKeys = keys(pattern)

    return RedisStats(
        keyCount = allKeys.size,
        namespaces = allKeys.map { it.substringBefore(":") }.distinct(),
        patterns = allKeys.groupingBy { it.substringBefore(":") }.eachCount()
    )
}

@Serializable
data class RedisStats(
    val keyCount: Int,
    val namespaces: List<String>,
    val patterns: Map<String, Int>
)

// ============== Operator Overloading ==============

/**
 * Get operator for Redis client
 */
suspend operator fun RedisClient.get(key: String): String? = get(key)

/**
 * Set operator for Redis client
 */
suspend operator fun RedisClient.set(key: String, value: String) = set(key, value)

/**
 * Contains operator for Redis client
 */
suspend operator fun RedisClient.contains(key: String): Boolean = exists(key)

/**
 * Plus assign operator for incrementing
 */
suspend operator fun RedisClient.plusAssign(key: String) {
    increment(key)
}

/**
 * Minus assign operator for decrementing
 */
suspend operator fun RedisClient.minusAssign(key: String) {
    decrement(key)
}