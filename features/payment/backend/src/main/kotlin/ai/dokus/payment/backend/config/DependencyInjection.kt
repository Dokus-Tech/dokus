package ai.dokus.payment.backend.config

import ai.dokus.foundation.apispec.PaymentApi
import ai.dokus.payment.backend.services.PaymentApiImpl
import ai.dokus.foundation.ktor.AppConfig
import ai.dokus.foundation.ktor.cache.RedisNamespace
import ai.dokus.foundation.ktor.cache.redisModule
import io.ktor.server.application.*
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

private val appModule = module {
    // API implementations
    single<PaymentApi> {
        PaymentApiImpl(
            paymentService = get()
        )
    }
}

fun Application.configureDependencyInjection(appConfig: AppConfig) {
    val coreModule = module {
        single<AppConfig> { appConfig }
    }

    install(Koin) {
        modules(coreModule, appModule, redisModule(appConfig, RedisNamespace.Payment), rpcClientModule)
    }
}
