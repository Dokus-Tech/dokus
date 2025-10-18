package ai.dokus.expense.backend.config

import ai.dokus.foundation.apispec.ExpenseApi
import ai.dokus.expense.backend.services.ExpenseApiImpl
import ai.dokus.foundation.ktor.AppConfig
import ai.dokus.foundation.ktor.cache.RedisNamespace
import ai.dokus.foundation.ktor.cache.redisModule
import io.ktor.server.application.*
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

private val appModule = module {
    // API implementations
    single<ExpenseApi> {
        ExpenseApiImpl(
            expenseService = get()
        )
    }
}

fun Application.configureDependencyInjection(appConfig: AppConfig) {
    val coreModule = module {
        single<AppConfig> { appConfig }
    }

    install(Koin) {
        modules(coreModule, appModule, redisModule(appConfig, RedisNamespace.Expense), rpcClientModule)
    }
}
