package ai.dokus.payment.backend.config

import ai.dokus.foundation.domain.rpc.PaymentApi
import ai.dokus.foundation.ktor.services.PaymentService
import ai.dokus.payment.backend.database.services.PaymentServiceImpl
import ai.dokus.payment.backend.database.tables.*
import ai.dokus.foundation.ktor.database.DatabaseFactory
import ai.dokus.payment.backend.services.PaymentApiImpl
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
        DatabaseFactory(get(), "payment-pool").apply {
            runBlocking {
                init(PaymentsTable)
            }
        }
    }

    // Local database services
    single<PaymentService> { PaymentServiceImpl(get()) }

    // API implementations
    single<PaymentApi> {
        PaymentApiImpl(
            paymentService = get(),
            invoiceService = get()
        )
    }
}

fun Application.configureDependencyInjection(appConfig: AppBaseConfig) {
    val coreModule = module {
        single<AppBaseConfig> { appConfig }
    }

    install(Koin) {
        modules(coreModule, appModule, redisModule(appConfig, RedisNamespace.Payment), rpcClientModule)
    }
}
