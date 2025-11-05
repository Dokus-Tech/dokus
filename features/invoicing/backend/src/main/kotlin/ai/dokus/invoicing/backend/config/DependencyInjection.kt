package ai.dokus.invoicing.backend.config

import ai.dokus.foundation.domain.rpc.InvoiceApi
import ai.dokus.foundation.ktor.services.InvoiceService
import ai.dokus.foundation.ktor.services.ClientService
import ai.dokus.invoicing.backend.database.services.InvoiceServiceImpl
import ai.dokus.invoicing.backend.database.services.ClientServiceImpl
import ai.dokus.invoicing.backend.database.tables.*
import ai.dokus.foundation.ktor.database.DatabaseFactory
import ai.dokus.invoicing.backend.services.InvoiceApiImpl
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
        DatabaseFactory(get(), "invoicing-pool").apply {
            runBlocking {
                init(InvoicesTable, InvoiceItemsTable, ClientsTable)
            }
        }
    }

    // Local database services
    single<InvoiceService> { InvoiceServiceImpl(get(), get()) }
    single<ClientService> { ClientServiceImpl(get()) }

    // API implementations
    single<InvoiceApi> {
        InvoiceApiImpl(
            invoiceService = get()
        )
    }
}

fun Application.configureDependencyInjection(appConfig: AppBaseConfig) {
    val coreModule = module {
        single<AppBaseConfig> { appConfig }
    }

    install(Koin) {
        modules(coreModule, appModule, redisModule(appConfig, RedisNamespace.Invoicing), rpcClientModule)
    }
}
