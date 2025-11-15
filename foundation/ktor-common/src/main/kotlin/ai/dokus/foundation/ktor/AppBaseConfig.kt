package ai.dokus.foundation.ktor

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

data class AppBaseConfig(
    val ktor: KtorConfig,
    val database: DatabaseConfig,
    val flyway: FlywayConfig,
    val jwt: JwtConfig,
    val auth: AuthConfig,
    val logging: LoggingConfig,
    val metrics: MetricsConfig,
    val security: SecurityConfig,
    val caching: CachingConfig,
    val config: Config,
) {
    companion object Companion {
        fun fromConfig(config: Config): AppBaseConfig {
            return AppBaseConfig(
                ktor = KtorConfig.fromConfig(config.getConfig("ktor")),
                database = DatabaseConfig.fromConfig(config.getConfig("database")),
                flyway = FlywayConfig.fromConfig(config.getConfig("flyway")),
                jwt = JwtConfig.fromConfig(config.getConfig("jwt")),
                auth = AuthConfig.fromConfig(config.getConfig("auth")),
                logging = LoggingConfig.fromConfig(config.getConfig("logging")),
                metrics = MetricsConfig.fromConfig(config.getConfig("metrics")),
                security = SecurityConfig.fromConfig(config.getConfig("security")),
                caching = CachingConfig.fromConfig(config.getConfig("caching")),
                config = config,
            )
        }

        fun load(): AppBaseConfig {
            val environment = System.getenv("ENVIRONMENT") ?: "local"
            val configBaseName = "application-$environment"
            return fromConfig(ConfigFactory.load(configBaseName))
        }
    }
}

data class KtorConfig(
    val deployment: DeploymentConfig
) {
    data class DeploymentConfig(
        val port: Int,
        val host: String,
        val environment: String
    )

    companion object {
        fun fromConfig(config: Config): KtorConfig {
            val deploymentConfig = config.getConfig("deployment")
            return KtorConfig(
                deployment = DeploymentConfig(
                    port = deploymentConfig.getInt("port"),
                    host = deploymentConfig.getString("host"),
                    environment = deploymentConfig.getString("environment")
                )
            )
        }
    }
}

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

data class FlywayConfig(
    val enabled: Boolean,
    val baselineOnMigrate: Boolean,
    val baselineVersion: String,
    val locations: List<String>,
    val schemas: List<String>
) {
    companion object {
        fun fromConfig(config: Config): FlywayConfig {
            return FlywayConfig(
                enabled = config.getBoolean("enabled"),
                baselineOnMigrate = config.getBoolean("baselineOnMigrate"),
                baselineVersion = config.getString("baselineVersion"),
                locations = config.getStringList("locations"),
                schemas = config.getStringList("schemas")
            )
        }
    }
}

data class JwtConfig(
    val issuer: String,
    val audience: String,
    val realm: String,
    val secret: String,
    val publicKeyPath: String?,
    val privateKeyPath: String?,
    val algorithm: String
) {
    companion object {
        fun fromConfig(config: Config): JwtConfig {
            return JwtConfig(
                issuer = config.getString("issuer"),
                audience = config.getString("audience"),
                realm = config.getString("realm"),
                secret = config.getString("secret"),
                publicKeyPath = if (config.hasPath("publicKeyPath")) config.getString("publicKeyPath") else null,
                privateKeyPath = if (config.hasPath("privateKeyPath")) config.getString("privateKeyPath") else null,
                algorithm = if (config.hasPath("algorithm")) config.getString("algorithm") else "HS256"
            )
        }
    }
}

data class AuthConfig(
    val maxLoginAttempts: Int,
    val lockDurationMinutes: Int,
    val sessionDurationHours: Int,
    val rememberMeDurationDays: Int,
    val maxConcurrentSessions: Int,
    val password: PasswordConfig,
    val rateLimit: RateLimitConfig,
    val enableDeviceFingerprinting: Boolean,
    val enableSessionSlidingExpiration: Boolean,
    val sessionActivityWindowMinutes: Int,
    val logSecurityEvents: Boolean,
    val enableDebugMode: Boolean
) {
    data class PasswordConfig(
        val expiryDays: Int,
        val minLength: Int,
        val requireUppercase: Boolean,
        val requireLowercase: Boolean,
        val requireDigits: Boolean,
        val requireSpecialChars: Boolean,
        val historySize: Int
    )

    data class RateLimitConfig(
        val windowSeconds: Int,
        val maxAttempts: Int
    )

    companion object {
        fun fromConfig(config: Config): AuthConfig {
            val passwordConfig = config.getConfig("password")
            val rateLimitConfig = config.getConfig("rateLimit")

            return AuthConfig(
                maxLoginAttempts = config.getInt("maxLoginAttempts"),
                lockDurationMinutes = config.getInt("lockDurationMinutes"),
                sessionDurationHours = config.getInt("sessionDurationHours"),
                rememberMeDurationDays = config.getInt("rememberMeDurationDays"),
                maxConcurrentSessions = config.getInt("maxConcurrentSessions"),
                password = PasswordConfig(
                    expiryDays = passwordConfig.getInt("expiryDays"),
                    minLength = passwordConfig.getInt("minLength"),
                    requireUppercase = passwordConfig.getBoolean("requireUppercase"),
                    requireLowercase = passwordConfig.getBoolean("requireLowercase"),
                    requireDigits = passwordConfig.getBoolean("requireDigits"),
                    requireSpecialChars = passwordConfig.getBoolean("requireSpecialChars"),
                    historySize = passwordConfig.getInt("historySize")
                ),
                rateLimit = RateLimitConfig(
                    windowSeconds = rateLimitConfig.getInt("windowSeconds"),
                    maxAttempts = rateLimitConfig.getInt("maxAttempts")
                ),
                enableDeviceFingerprinting = config.getBoolean("enableDeviceFingerprinting"),
                enableSessionSlidingExpiration = config.getBoolean("enableSessionSlidingExpiration"),
                sessionActivityWindowMinutes = config.getInt("sessionActivityWindowMinutes"),
                logSecurityEvents = config.getBoolean("logSecurityEvents"),
                enableDebugMode = config.getBoolean("enableDebugMode")
            )
        }
    }
}

data class LoggingConfig(
    val level: String,
    val consoleJson: Boolean
) {
    companion object {
        fun fromConfig(config: Config): LoggingConfig {
            return LoggingConfig(
                level = config.getString("level"),
                consoleJson = config.getBoolean("consoleJson")
            )
        }
    }
}

data class MetricsConfig(
    val enabled: Boolean,
    val prometheusPath: String
) {
    companion object {
        fun fromConfig(config: Config): MetricsConfig {
            return MetricsConfig(
                enabled = config.getBoolean("enabled"),
                prometheusPath = config.getString("prometheusPath")
            )
        }
    }
}

data class SecurityConfig(
    val cors: Cors,
) {
    data class Cors(
        val allowedHosts: List<String>
    )

    companion object {
        fun fromConfig(config: Config): SecurityConfig {
            val corsConfig = config.getConfig("cors")

            return SecurityConfig(
                cors = Cors(
                    allowedHosts = corsConfig.getStringList("allowedHosts")
                )
            )
        }
    }
}

data class CachingConfig(
    val type: String,
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
                type = config.getString("type"),
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

data class StorageConfig(
    val type: String,
    val directory: String
) {
    companion object {
        fun fromConfig(config: Config): StorageConfig {
            val storage = config.getConfig("storage")
            return StorageConfig(
                type = config.getString("type"),
                directory = config.getString("directory")
            )
        }

        fun load(baseConfig: AppBaseConfig): StorageConfig {
            return fromConfig(baseConfig.config.getConfig("storage"))
        }
    }
}