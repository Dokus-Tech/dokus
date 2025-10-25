package ai.dokus.expense.backend.config

import ai.dokus.foundation.domain.rpc.ExpenseApi
import ai.dokus.expense.backend.services.ExpenseApiImpl
import ai.dokus.foundation.ktor.AppBaseConfig
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

fun Application.configureDependencyInjection(appConfig: AppBaseConfig) {
    val coreModule = module {
        single<AppBaseConfig> { appConfig }
    }

    install(Koin) {
        modules(coreModule, appModule, redisModule(appConfig, RedisNamespace.Expense), rpcClientModule)
    }
}
