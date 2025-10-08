package be.police.pulse.foundation.ktor.cache

import be.police.pulse.foundation.ktor.CachingConfig

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