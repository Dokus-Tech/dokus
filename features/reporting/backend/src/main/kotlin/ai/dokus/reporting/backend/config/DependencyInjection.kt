package ai.dokus.reporting.backend.config

import ai.dokus.foundation.domain.rpc.ReportingApi
import ai.dokus.reporting.backend.services.ReportingApiImpl
import ai.dokus.reporting.backend.database.tables.*
import ai.dokus.foundation.ktor.database.DatabaseFactory
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
        DatabaseFactory(get(), "reporting-pool").apply {
            runBlocking {
                init(VatReturnsTable)
            }
        }
    }

    // API implementations
    single<ReportingApi> {
        ReportingApiImpl(
            invoiceService = get(),
            expenseService = get(),
            paymentService = get()
        )
    }
}

fun Application.configureDependencyInjection(appConfig: AppBaseConfig) {
    val coreModule = module {
        single<AppBaseConfig> { appConfig }
    }

    install(Koin) {
        modules(coreModule, appModule, redisModule(appConfig, RedisNamespace.Reporting), rpcClientModule)
    }
}
