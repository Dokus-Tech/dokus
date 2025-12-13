package ai.dokus.reporting.backend.config

import ai.dokus.reporting.backend.database.ReportingTables
import ai.dokus.foundation.database.di.repositoryModuleReporting
import ai.dokus.foundation.ktor.cache.RedisNamespace
import ai.dokus.foundation.ktor.cache.redisModule
import ai.dokus.foundation.ktor.config.AppBaseConfig
import ai.dokus.foundation.ktor.database.DatabaseFactory
import ai.dokus.foundation.ktor.security.JwtValidator
import io.ktor.server.application.*
import kotlinx.coroutines.runBlocking
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

private val appModule = module {
    // Database - connect and initialize reporting-owned tables only
    single {
        DatabaseFactory(get(), "reporting-pool").apply {
            runBlocking {
                connect()
                ReportingTables.initialize()
            }
        }
    }
}

fun Application.configureDependencyInjection(appConfig: AppBaseConfig) {
    val coreModule = module {
        single<AppBaseConfig> { appConfig }

        // JWT validator for local token validation
        single {
            JwtValidator(appConfig.jwt)
        }
    }

    install(Koin) {
        modules(
            coreModule,
            repositoryModuleReporting,
            appModule,
            redisModule(appConfig, RedisNamespace.Reporting)
        )
    }
}
