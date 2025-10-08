package ai.dokus.foundation.ktor.cache

import ai.dokus.foundation.ktor.CachingConfig
import io.lettuce.core.RedisURI
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.support.ConnectionPoolSupport
import kotlinx.coroutines.future.await
import org.apache.commons.pool2.impl.GenericObjectPool
import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import java.time.Duration
import io.lettuce.core.RedisClient as LettuceRedisClient

/**
 * Redis client implementation using Lettuce library with namespace support
 */
class RedisClientImpl(
    private val config: CachingConfig.RedisConfig,
    private val namespace: String
) : RedisClient {

    private val redisClient: LettuceRedisClient
    private val connectionPool: GenericObjectPool<StatefulRedisConnection<String, String>>

    init {
        // Build Redis URI with all configuration
        val redisUri = RedisURI.builder()
            .withHost(config.host)
            .withPort(config.port)
            .withDatabase(config.database)
            .withTimeout(Duration.ofMillis(config.timeout.connection))
            .apply {
                config.password?.let { withPassword(it.toCharArray()) }
            }
            .build()

        // Create Redis client
        redisClient = LettuceRedisClient.create(redisUri)

        // Configure connection pool
        val poolConfig = GenericObjectPoolConfig<StatefulRedisConnection<String, String>>().apply {
            maxTotal = config.pool.maxTotal
            maxIdle = config.pool.maxIdle
            minIdle = config.pool.minIdle
            testOnBorrow = config.pool.testOnBorrow
            testOnReturn = true
            testWhileIdle = true
            timeBetweenEvictionRuns = Duration.ofSeconds(30)
        }

        // Create connection pool
        connectionPool = ConnectionPoolSupport.createGenericObjectPool(
            { redisClient.connect() },
            poolConfig
        )
    }

    /**
     * Build the full cache key with namespace prefix
     */
    private fun buildKey(key: String): String = "$namespace:$key"

    /**
     * Build multiple full cache keys with namespace prefix
     */
    private fun buildKeys(vararg keys: String): Array<String> =
        keys.map { buildKey(it) }.toTypedArray()

    /**
     * Execute a Redis command with a connection from the pool
     */
    private suspend fun <T> withConnection(block: suspend (RedisAsyncCommands<String, String>) -> T): T {
        val connection = connectionPool.borrowObject()
        return try {
            block(connection.async())
        } finally {
            connectionPool.returnObject(connection)
        }
    }

    override suspend fun get(key: String): String? = withConnection { commands ->
        commands.get(buildKey(key)).await()
    }

    override suspend fun set(key: String, value: String, ttl: Duration?) = withConnection { commands ->
        val fullKey = buildKey(key)
        if (ttl != null) {
            commands.setex(fullKey, ttl.seconds, value).await()
        } else {
            commands.set(fullKey, value).await()
        }
        Unit
    }

    override suspend fun setIfAbsent(key: String, value: String, ttl: Duration?): Boolean = withConnection { commands ->
        val fullKey = buildKey(key)
        val result = commands.setnx(fullKey, value).await()
        if (result && ttl != null) {
            commands.expire(fullKey, ttl.seconds).await()
        }
        result
    }

    override suspend fun delete(key: String): Boolean = withConnection { commands ->
        commands.del(buildKey(key)).await() > 0
    }

    override suspend fun deleteMany(vararg keys: String): Long = withConnection { commands ->
        if (keys.isEmpty()) return@withConnection 0L
        commands.del(*buildKeys(*keys)).await()
    }

    override suspend fun exists(key: String): Boolean = withConnection { commands ->
        commands.exists(buildKey(key)).await() > 0
    }

    override suspend fun expire(key: String, ttl: Duration): Boolean = withConnection { commands ->
        commands.expire(buildKey(key), ttl.seconds).await()
    }

    override suspend fun ttl(key: String): Long = withConnection { commands ->
        commands.ttl(buildKey(key)).await()
    }

    override suspend fun increment(key: String, delta: Long): Long = withConnection { commands ->
        commands.incrby(buildKey(key), delta).await()
    }

    override suspend fun decrement(key: String, delta: Long): Long = withConnection { commands ->
        commands.decrby(buildKey(key), delta).await()
    }

    override suspend fun getMany(vararg keys: String): Map<String, String> = withConnection { commands ->
        if (keys.isEmpty()) return@withConnection emptyMap()

        val fullKeys = buildKeys(*keys)
        val values = commands.mget(*fullKeys).await()

        keys.zip(values)
            .filter { it.second.hasValue() }
            .associate { it.first to it.second.value }
    }

    override suspend fun setMany(values: Map<String, String>, ttl: Duration?) = withConnection { commands ->
        if (values.isEmpty()) return@withConnection

        val mappedValues = values.map { (key, value) ->
            buildKey(key) to value
        }.toMap()

        commands.mset(mappedValues).await()

        if (ttl != null) {
            // Set expiration for each key
            mappedValues.keys.forEach { key ->
                commands.expire(key, ttl.seconds).await()
            }
        }
        Unit
    }

    override suspend fun keys(pattern: String): Set<String> = withConnection { commands ->
        val fullPattern = buildKey(pattern)
        val keys = commands.keys(fullPattern).await()
        val prefixLength = "$namespace:".length
        keys.map { it.substring(prefixLength) }.toSet()
    }

    override suspend fun clear() = withConnection { commands ->
        val pattern = "$namespace:*"
        val keys = commands.keys(pattern).await()
        if (keys.isNotEmpty()) {
            commands.del(*keys.toTypedArray()).await()
        }
        Unit
    }

    override suspend fun ping(): Boolean = withConnection { commands ->
        commands.ping().await() == "PONG"
    }

    override suspend fun close() {
        connectionPool.close()
        redisClient.shutdown()
    }
}