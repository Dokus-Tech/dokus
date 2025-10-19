package ai.dokus.reporting.backend.config

import ai.dokus.foundation.apispec.ReportingApi
import ai.dokus.reporting.backend.services.ReportingApiImpl
import ai.dokus.foundation.ktor.AppBaseConfig
import ai.dokus.foundation.ktor.cache.RedisNamespace
import ai.dokus.foundation.ktor.cache.redisModule
import io.ktor.server.application.*
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

private val appModule = module {
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
