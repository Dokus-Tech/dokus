package ai.dokus.banking.backend.config

import ai.dokus.banking.backend.database.services.BankServiceImpl
import ai.dokus.banking.backend.database.tables.*
import ai.dokus.foundation.ktor.database.DatabaseFactory
import ai.dokus.foundation.ktor.config.AppBaseConfig
import ai.dokus.foundation.ktor.cache.RedisNamespace
import ai.dokus.foundation.ktor.cache.redisModule
import ai.dokus.foundation.ktor.services.BankService
import io.ktor.server.application.Application
import io.ktor.server.application.install
import kotlinx.coroutines.runBlocking
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

private val appModule = module {
    // Database
    single {
        DatabaseFactory(get(), "banking-pool").apply {
            runBlocking {
                init(BankConnectionsTable, BankTransactionsTable)
            }
        }
    }

    // Local database services
    single<BankService> { BankServiceImpl() }
}

fun Application.configureDependencyInjection(appConfig: AppBaseConfig) {
    val coreModule = module {
        single<AppBaseConfig> { appConfig }
    }

    install(Koin) {
        modules(coreModule, appModule, redisModule(appConfig, RedisNamespace.Banking))
    }
}
