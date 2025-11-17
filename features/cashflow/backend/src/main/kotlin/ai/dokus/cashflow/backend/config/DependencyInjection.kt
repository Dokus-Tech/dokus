package ai.dokus.cashflow.backend.config

import ai.dokus.cashflow.backend.database.tables.*
import ai.dokus.cashflow.backend.repository.AttachmentRepository
import ai.dokus.cashflow.backend.repository.ExpenseRepository
import ai.dokus.cashflow.backend.repository.InvoiceRepository
import ai.dokus.cashflow.backend.rpc.CashflowApiImpl
import ai.dokus.cashflow.backend.service.DocumentStorageService
import ai.dokus.foundation.domain.rpc.CashflowApi
import ai.dokus.foundation.ktor.AppBaseConfig
import ai.dokus.foundation.ktor.database.DatabaseFactory
import io.ktor.server.application.*
import kotlinx.coroutines.runBlocking
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

fun Application.configureDependencyInjection(appConfig: AppBaseConfig) {
    install(Koin) {
        modules(
            coreModule(appConfig),
            databaseModule,
            serviceModule,
            rpcModule,
            rpcClientModule
        )
    }
}

/**
 * Core module - provides base configuration
 */
fun coreModule(appConfig: AppBaseConfig) = module {
    single { appConfig }
}

/**
 * Database module - provides database factory and connection
 */
val databaseModule = module {
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
 * Service module - business logic services and repositories
 */
val serviceModule = module {
    // Repositories
    single { AttachmentRepository() }
    single { InvoiceRepository() }
    single { ExpenseRepository() }

    // Services
    single {
        DocumentStorageService(
            storageBasePath = "./storage/documents",
            maxFileSizeMb = 10
        )
    }

    // TODO: Add InvoiceService, ExpenseService when implemented
}

/**
 * RPC module - KotlinX RPC service implementations
 */
val rpcModule = module {
    single<CashflowApiImpl> {
        CashflowApiImpl(
            attachmentRepository = get(),
            documentStorageService = get(),
            invoiceRepository = get(),
            expenseRepository = get()
        )
    }
}
