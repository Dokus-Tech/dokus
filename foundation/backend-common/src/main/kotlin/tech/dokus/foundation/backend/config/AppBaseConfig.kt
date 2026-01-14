package tech.dokus.foundation.backend.config

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
    val serverInfo: ServerInfoConfig,
    val storage: StorageConfig,
    val ai: AIConfig,
    val processor: ProcessorConfig,
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
                serverInfo = ServerInfoConfig.fromConfig(config.getConfig("server")),
                storage = if (config.hasPath("storage")) {
                    StorageConfig.fromConfig(config.getConfig("storage"))
                } else {
                    StorageConfig.empty()
                },
                ai = AIConfig.fromConfig(config.getConfig("ai")),
                processor = ProcessorConfig.fromConfig(config.getConfig("processor")),
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
