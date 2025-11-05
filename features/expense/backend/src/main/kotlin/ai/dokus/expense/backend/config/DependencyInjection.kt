package ai.dokus.expense.backend.config

import ai.dokus.foundation.domain.rpc.ExpenseApi
import ai.dokus.foundation.ktor.services.ExpenseService
import ai.dokus.expense.backend.database.services.ExpenseServiceImpl
import ai.dokus.expense.backend.database.tables.*
import ai.dokus.foundation.ktor.database.DatabaseFactory
import ai.dokus.expense.backend.services.ExpenseApiImpl
import ai.dokus.foundation.ktor.AppBaseConfig
import ai.dokus.foundation.ktor.cache.RedisNamespace
import ai.dokus.foundation.ktor.cache.redisModule
import io.ktor.server.application.*
import kotlinx.coroutines.runBlocking
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

private val appModule = module {
    // Database
    single {
        DatabaseFactory(get(), "expense-pool").apply {
            runBlocking {
                init(ExpensesTable)
            }
        }
    }

    // Local database services
    single<ExpenseService> { ExpenseServiceImpl(get()) }

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
