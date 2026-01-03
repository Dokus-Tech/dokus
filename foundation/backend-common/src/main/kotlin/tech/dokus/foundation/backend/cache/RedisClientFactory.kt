package tech.dokus.foundation.backend.cache

import tech.dokus.foundation.backend.config.CachingConfig

/**
 * Simple factory for creating Redis client instances
 * Designed for dependency injection with Koin
 */
object RedisClientFactory {

    /**
     * Create a new Redis client instance
     */
    fun createClient(config: CachingConfig.RedisConfig, namespace: RedisNamespace): RedisClient =
        RedisClientImpl(config, namespace.value)
}
