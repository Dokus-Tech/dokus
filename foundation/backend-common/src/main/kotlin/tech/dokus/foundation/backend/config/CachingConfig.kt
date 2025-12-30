package tech.dokus.foundation.backend.config

import com.typesafe.config.Config

data class CachingConfig(
    val ttl: Long,
    val maxSize: Long,
    val redis: RedisConfig,
) {

    data class RedisConfig(
        val host: String,
        val port: Int,
        val password: String?,
        val database: Int,
        val pool: PoolConfig,
        val timeout: TimeoutConfig
    )

    data class PoolConfig(
        val maxTotal: Int,
        val maxIdle: Int,
        val minIdle: Int,
        val testOnBorrow: Boolean
    )

    data class TimeoutConfig(
        val connection: Long,
        val socket: Long,
        val command: Long
    )

    companion object {
        fun fromConfig(config: Config): CachingConfig {
            val redisConfig = config.getConfig("redis")
            val poolConfig = redisConfig.getConfig("pool")
            val timeoutConfig = redisConfig.getConfig("timeout")

            return CachingConfig(
                ttl = config.getLong("ttl"),
                maxSize = config.getLong("maxSize"),
                redis = RedisConfig(
                    host = redisConfig.getString("host"),
                    port = redisConfig.getInt("port"),
                    password = if (redisConfig.hasPath("password")) redisConfig.getString("password") else null,
                    database = if (redisConfig.hasPath("database")) redisConfig.getInt("database") else 0,
                    pool = PoolConfig(
                        maxTotal = poolConfig.getInt("maxTotal"),
                        maxIdle = poolConfig.getInt("maxIdle"),
                        minIdle = poolConfig.getInt("minIdle"),
                        testOnBorrow = poolConfig.getBoolean("testOnBorrow")
                    ),
                    timeout = TimeoutConfig(
                        connection = timeoutConfig.getLong("connection"),
                        socket = timeoutConfig.getLong("socket"),
                        command = timeoutConfig.getLong("command")
                    )
                )
            )
        }
    }
}