package ai.dokus.cashflow.backend.config

import ai.dokus.cashflow.backend.database.tables.AttachmentsTable
import ai.dokus.cashflow.backend.database.tables.ExpensesTable
import ai.dokus.cashflow.backend.database.tables.InvoiceItemsTable
import ai.dokus.cashflow.backend.database.tables.InvoicesTable
import ai.dokus.cashflow.backend.repository.AttachmentRepository
import ai.dokus.cashflow.backend.repository.ExpenseRepository
import ai.dokus.cashflow.backend.repository.InvoiceRepository
import ai.dokus.cashflow.backend.service.DocumentStorageService
import ai.dokus.foundation.ktor.config.AppBaseConfig
import ai.dokus.foundation.ktor.database.DatabaseFactory
import ai.dokus.foundation.ktor.security.JwtValidator
import io.ktor.server.application.Application
import io.ktor.server.application.install
import kotlinx.coroutines.runBlocking
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

fun Application.configureDependencyInjection(appConfig: AppBaseConfig) {
    install(Koin) {
        modules(
            coreModule(appConfig),
            databaseModule,
            serviceModule,
            rpcClientModule
        )
    }
}

/**
 * Core module - provides base configuration and security
 */
fun coreModule(appConfig: AppBaseConfig) = module {
    single { appConfig }

    // JWT validator for local token validation
    single {
        JwtValidator(
            secret = appConfig.jwt.secret,
            issuer = appConfig.jwt.issuer
        )
    }
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
}
