package tech.dokus.backend.config

import com.typesafe.config.Config
import org.koin.dsl.bind
import org.koin.dsl.module
import tech.dokus.foundation.backend.config.AIConfig
import tech.dokus.foundation.backend.config.AppBaseConfig
import tech.dokus.foundation.backend.config.AuthConfig
import tech.dokus.foundation.backend.config.CachingConfig
import tech.dokus.foundation.backend.config.DatabaseConfig
import tech.dokus.foundation.backend.config.EmailConfig
import tech.dokus.foundation.backend.config.FlywayConfig
import tech.dokus.foundation.backend.config.JwtConfig
import tech.dokus.foundation.backend.config.KtorConfig
import tech.dokus.foundation.backend.config.LoggingConfig
import tech.dokus.foundation.backend.config.MetricsConfig
import tech.dokus.foundation.backend.config.MinioConfig
import tech.dokus.foundation.backend.config.ProcessorConfig
import tech.dokus.foundation.backend.config.SecurityConfig
import tech.dokus.foundation.backend.config.ServerInfoConfig
import tech.dokus.foundation.backend.config.StorageConfig

internal fun configureConfigDi(appConfig: AppBaseConfig) = module {
    single { appConfig.ktor } bind KtorConfig::class
    single { appConfig.database } bind DatabaseConfig::class
    single { appConfig.flyway } bind FlywayConfig::class
    single { appConfig.jwt } bind JwtConfig::class
    single { appConfig.auth } bind AuthConfig::class
    single { appConfig.logging } bind LoggingConfig::class
    single { appConfig.metrics } bind MetricsConfig::class
    single { appConfig.security } bind SecurityConfig::class
    single { appConfig.caching } bind CachingConfig::class
    single { appConfig.serverInfo } bind ServerInfoConfig::class
    single { appConfig.storage } bind StorageConfig::class
    single { appConfig.ai } bind AIConfig::class
    single { appConfig.processor } bind ProcessorConfig::class
    single { appConfig.email } bind EmailConfig::class
    single { appConfig.config } bind Config::class
    single {
        MinioConfig.loadOrNull(appConfig.config)
            ?: error("MinIO config missing. Ensure 'minio { ... }' exists in application config.")
    }
}
