package tech.dokus.foundation.ktor.cache

import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.serializer
import java.time.Duration

/**
 * Kotlin DSL for Redis operations
 * Similar to Exposed's transaction DSL
 */
@DslMarker
annotation class RedisDsl

/**
 * Redis transaction/pipeline builder
 */
@RedisDsl
class RedisOperations(@PublishedApi internal val client: RedisClient) {

    suspend inline fun <reified T> get(key: String): T? {
        val value = client.get(key) ?: return null
        return CacheSerializer.json.decodeFromString(serializer<T>(), value)
    }

    suspend inline fun <reified T> set(key: String, value: T, ttl: Duration? = null) {
        val serialized = CacheSerializer.json.encodeToString(serializer<T>(), value)
        client.set(key, serialized, ttl)
    }

    suspend fun getString(key: String): String? = client.get(key)

    suspend fun setString(key: String, value: String, ttl: Duration? = null) =
        client.set(key, value, ttl)

    suspend fun delete(vararg keys: String): Long = client.deleteMany(*keys)

    suspend fun exists(key: String): Boolean = client.exists(key)

    suspend fun expire(key: String, ttl: Duration): Boolean = client.expire(key, ttl)

    suspend fun ttl(key: String): Long = client.ttl(key)

    suspend fun increment(key: String, delta: Long = 1): Long = client.increment(key, delta)

    suspend fun decrement(key: String, delta: Long = 1): Long = client.decrement(key, delta)

    suspend inline fun <reified T> getOrSet(
        key: String,
        ttl: Duration? = null,
        producer: suspend () -> T
    ): T {
        val existing = get<T>(key)
        if (existing != null) return existing

        val value = producer()
        set(key, value, ttl)
        return value
    }

    suspend inline fun <reified T> computeIfAbsent(
        key: String,
        ttl: Duration? = null,
        producer: suspend () -> T
    ): T {
        val existing = get<T>(key)
        if (existing != null) return existing

        val value = producer()
        val serialized = CacheSerializer.json.encodeToString(serializer<T>(), value)
        return if (client.setIfAbsent(key, serialized, ttl)) {
            value
        } else {
            // Another process set it, get the new value
            get<T>(key) ?: value
        }
    }

    suspend fun lock(
        resource: String,
        ttl: Duration = Duration.ofSeconds(30),
        block: suspend () -> Unit
    ) {
        val lockKey = CacheKeyBuilder.forLock(resource)
        val acquired = client.setIfAbsent(lockKey, "locked", ttl)

        if (!acquired) {
            throw IllegalStateException("Could not acquire lock for $resource")
        }

        try {
            block()
        } finally {
            client.delete(lockKey)
        }
    }

    suspend fun tryLock(
        resource: String,
        ttl: Duration = Duration.ofSeconds(30),
        block: suspend () -> Unit
    ): Boolean {
        val lockKey = CacheKeyBuilder.forLock(resource)
        val acquired = client.setIfAbsent(lockKey, "locked", ttl)

        if (!acquired) return false

        try {
            block()
            return true
        } finally {
            client.delete(lockKey)
        }
    }
}

/**
 * Execute Redis operations in a DSL context
 * Similar to Exposed's transaction { } block
 */
suspend fun <T> RedisClient.execute(block: suspend RedisOperations.() -> T): T = coroutineScope {
    RedisOperations(this@execute).block()
}

/**
 * Batch operations DSL
 */
@RedisDsl
class RedisBatch {
    private val operations = mutableListOf<suspend RedisClient.() -> Unit>()

    fun set(key: String, value: String, ttl: Duration? = null) {
        operations += { set(key, value, ttl) }
    }

    fun delete(key: String) {
        operations += { delete(key) }
    }

    fun expire(key: String, ttl: Duration) {
        operations += { expire(key, ttl) }
    }

    suspend fun execute(client: RedisClient) {
        operations.forEach { operation ->
            client.operation()
        }
    }
}

/**
 * Execute batch operations
 */
suspend fun RedisClient.batch(block: RedisBatch.() -> Unit) {
    val batch = RedisBatch().apply(block)
    batch.execute(this)
}

/**
 * Cache builder DSL
 */
@RedisDsl
class CacheBuilder {
    var ttl: Duration = Duration.ofMinutes(5)
    var namespace: String = "default"
    var keyPrefix: String? = null

    fun key(vararg segments: String): String {
        val prefixSegments = listOfNotNull(namespace, keyPrefix)
        return CacheKeyBuilder.build(*(prefixSegments + segments).toTypedArray())
    }
}

/**
 * Build cache configuration
 */
inline fun cache(block: CacheBuilder.() -> Unit): CacheBuilder =
    CacheBuilder().apply(block)

/**
 * Session management DSL
 */
@RedisDsl
class SessionBuilder(private val client: RedisClient) {
    var ttl: Duration = Duration.ofHours(8)
    var slidingExpiration: Boolean = true

    suspend fun create(sessionId: String, data: Map<String, String>) {
        val key = CacheKeyBuilder.forSession(sessionId)
        client.set(key, data.entries.joinToString(";") { "${it.key}=${it.value}" }, ttl)
    }

    suspend fun get(sessionId: String): Map<String, String>? {
        val key = CacheKeyBuilder.forSession(sessionId)
        val data = client.get(key) ?: return null

        if (slidingExpiration) {
            client.expire(key, ttl)
        }

        return data.split(";").associate {
            val parts = it.split("=")
            parts[0] to parts[1]
        }
    }

    suspend fun destroy(sessionId: String) {
        client.delete(CacheKeyBuilder.forSession(sessionId))
    }
}

/**
 * Session management DSL
 */
suspend fun RedisClient.session(block: suspend SessionBuilder.() -> Unit) {
    SessionBuilder(this).block()
}