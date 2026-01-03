package tech.dokus.foundation.backend.config

import com.typesafe.config.Config

data class DatabaseConfig(
    val url: String,
    val username: String,
    val password: String,
    val pool: PoolConfig,
    val driver: String = "org.postgresql.Driver"
) {
    data class PoolConfig(
        val maxSize: Int,
        val minSize: Int,
        val acquisitionTimeout: Long,
        val idleTimeout: Long,
        val maxLifetime: Long,
        val leakDetectionThreshold: Long
    )

    companion object {
        fun fromConfig(config: Config): DatabaseConfig {
            val poolConfig = config.getConfig("pool")
            return DatabaseConfig(
                url = config.getString("url"),
                username = config.getString("username"),
                password = config.getString("password"),
                driver = if (config.hasPath("driver")) config.getString("driver") else "org.postgresql.Driver",
                pool = PoolConfig(
                    maxSize = poolConfig.getInt("maxSize"),
                    minSize = poolConfig.getInt("minSize"),
                    acquisitionTimeout = poolConfig.getLong("acquisitionTimeout"),
                    idleTimeout = poolConfig.getLong("idleTimeout"),
                    maxLifetime = poolConfig.getLong("maxLifetime"),
                    leakDetectionThreshold = poolConfig.getLong("leakDetectionThreshold")
                )
            )
        }
    }
}
