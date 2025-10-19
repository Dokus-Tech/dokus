package ai.dokus.auth.backend.config

import ai.dokus.auth.backend.services.*
import ai.dokus.foundation.apispec.*
import ai.dokus.foundation.ktor.AppBaseConfig
import ai.dokus.foundation.ktor.cache.RedisNamespace
import ai.dokus.foundation.ktor.cache.redisModule
import io.ktor.server.application.Application
import io.ktor.server.application.install
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

private val appModule = module {
    single<TenantApi> { TenantApiImpl(get()) }
    single<ClientApi> { ClientApiImpl(get()) }
    single<InvoiceApi> { InvoiceApiImpl(get()) }
    single<ExpenseApi> { ExpenseApiImpl(get()) }
}

fun Application.configureDependencyInjection(appConfig: AppBaseConfig) {
    val coreModule = module {
        single<AppBaseConfig> { appConfig }
    }

    install(Koin) {
        modules(coreModule, appModule, redisModule(appConfig, RedisNamespace.Auth), rpcClientModule)
    }
}