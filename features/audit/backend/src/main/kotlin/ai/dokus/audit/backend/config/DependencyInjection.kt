package ai.dokus.audit.backend.config

import ai.dokus.foundation.database.DatabaseInitializer
import ai.dokus.foundation.database.di.repositoryModuleAudit
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
    // Database - connect and initialize all tables centrally
    single {
        DatabaseFactory(get(), "audit-pool").apply {
            runBlocking {
                connect()
                DatabaseInitializer.initializeAllTables()
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
            repositoryModuleAudit,
            appModule,
            redisModule(appConfig, RedisNamespace.Audit)
        )
    }
}
