package ai.dokus.payment.backend.config

import ai.dokus.payment.backend.database.PaymentTables
import ai.dokus.foundation.database.di.repositoryModulePayment
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
    // Database - connect and initialize payment-owned tables only
    single {
        DatabaseFactory(get(), "payment-pool").apply {
            runBlocking {
                connect()
                PaymentTables.initialize()
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
            repositoryModulePayment,
            appModule,
            redisModule(appConfig, RedisNamespace.Payment)
        )
    }
}
