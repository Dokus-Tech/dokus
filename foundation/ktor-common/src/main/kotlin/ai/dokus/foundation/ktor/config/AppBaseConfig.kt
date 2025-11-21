package ai.dokus.foundation.ktor.config

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
    val rabbitmq: RabbitMQConfig,
    val config: Config,
) {
    companion object Companion {
        fun fromConfig(config: Config): AppBaseConfig {
            return AppBaseConfig(
                ktor = KtorConfig.Companion.fromConfig(config.getConfig("ktor")),
                database = DatabaseConfig.fromConfig(config.getConfig("database")),
                flyway = FlywayConfig.Companion.fromConfig(config.getConfig("flyway")),
                jwt = JwtConfig.Companion.fromConfig(config.getConfig("jwt")),
                auth = AuthConfig.fromConfig(config.getConfig("auth")),
                logging = LoggingConfig.Companion.fromConfig(config.getConfig("logging")),
                metrics = MetricsConfig.Companion.fromConfig(config.getConfig("metrics")),
                security = SecurityConfig.Companion.fromConfig(config.getConfig("security")),
                caching = CachingConfig.fromConfig(config.getConfig("caching")),
                rabbitmq = RabbitMQConfig.Companion.fromConfig(config.getConfig("rabbitmq")),
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

