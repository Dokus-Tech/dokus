package ai.dokus.cashflow.backend.config

import ai.dokus.cashflow.backend.database.tables.*
import ai.dokus.cashflow.backend.rpc.CashflowApiImpl
import ai.dokus.foundation.domain.rpc.CashflowApi
import ai.dokus.foundation.ktor.AppBaseConfig
import ai.dokus.foundation.ktor.database.DatabaseFactory
import io.ktor.server.application.*
import kotlinx.coroutines.runBlocking
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

fun Application.configureDependencyInjection(appConfig: AppBaseConfig) {
    install(Koin) {
        modules(
            databaseModule(appConfig),
            serviceModule,
            rpcModule
        )
    }
}

/**
 * Database module - provides database factory and connection
 */
fun databaseModule(appConfig: AppBaseConfig) = module {
    single { appConfig }
    single {
        DatabaseFactory(get(), "cashflow-pool").apply {
            runBlocking {
                init(
                    InvoicesTable,
                    InvoiceItemsTable,
                    ExpensesTable,
                    AttachmentsTable
                )
            }
        }
    }
}

/**
 * Service module - business logic services
 * TODO: Add InvoiceService, ExpenseService, DocumentStorageService
 */
val serviceModule = module {
    // Services will be added here as we implement them
}

/**
 * RPC module - KotlinX RPC service implementations
 */
val rpcModule = module {
    singleOf(::CashflowApiImpl) bind CashflowApi::class
}
