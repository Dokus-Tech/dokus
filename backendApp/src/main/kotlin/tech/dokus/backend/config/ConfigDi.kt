package tech.dokus.backend.config

import org.koin.dsl.bind
import org.koin.dsl.module
import tech.dokus.foundation.backend.config.AIConfig
import tech.dokus.foundation.backend.config.AppBaseConfig
import tech.dokus.foundation.backend.config.AuthConfig
import tech.dokus.foundation.backend.config.CachingConfig
import tech.dokus.foundation.backend.config.DatabaseConfig
import tech.dokus.foundation.backend.config.FlywayConfig

private fun configureConfigDi(appConfig: AppBaseConfig) = module {
    single { appConfig.ai } bind AIConfig::class
    single { appConfig.auth } bind AuthConfig::class
    single { appConfig.caching } bind CachingConfig::class
    single { appConfig.database } bind DatabaseConfig::class
    single { appConfig.flyway } bind FlywayConfig::class
}