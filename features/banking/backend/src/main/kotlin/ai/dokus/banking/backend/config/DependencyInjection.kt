package ai.dokus.banking.backend.config

import ai.dokus.banking.backend.database.BankingTables
import ai.dokus.foundation.database.di.repositoryModuleBanking
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
    // Database - connect and initialize banking-owned tables only
    single {
        DatabaseFactory(get(), "banking-pool").apply {
            runBlocking {
                connect()
                BankingTables.initialize()
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
            repositoryModuleBanking,
            appModule,
            redisModule(appConfig, RedisNamespace.Banking)
        )
    }
}
