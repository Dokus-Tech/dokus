package ai.dokus.foundation.database.configuration

import ai.dokus.foundation.database.services.ClientServiceImpl
import ai.dokus.foundation.database.services.ExpenseServiceImpl
import ai.dokus.foundation.database.services.InvoiceServiceImpl
import ai.dokus.foundation.database.services.PaymentServiceImpl
import ai.dokus.foundation.database.services.TenantServiceImpl
import ai.dokus.foundation.database.services.UserServiceImpl
import ai.dokus.foundation.database.utils.DatabaseFactory
import ai.dokus.foundation.ktor.AppConfig
import ai.dokus.foundation.ktor.cache.RedisNamespace
import ai.dokus.foundation.ktor.cache.redisModule
import ai.dokus.foundation.ktor.crypto.PasswordCryptoService
import ai.dokus.foundation.ktor.crypto.PasswordCryptoService4j
import ai.dokus.foundation.ktor.services.*
import io.ktor.server.application.Application
import io.ktor.server.application.install
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

private val appModule = module {
    single { DatabaseFactory(get(), "dokus-database-pool") }

    // Password crypto service
    single<PasswordCryptoService> { PasswordCryptoService4j() }

    // Audit Service
    single { ai.dokus.foundation.database.services.AuditServiceImpl() }

    // RPC Service Implementations
    single<TenantService> { TenantServiceImpl() }
    single<UserService> { UserServiceImpl(get()) }
    single<ClientService> { ClientServiceImpl(get()) }
    single<InvoiceService> { InvoiceServiceImpl(get(), get()) }
    single<ExpenseService> { ExpenseServiceImpl() }
    single<PaymentService> { PaymentServiceImpl() }
}

fun Application.configureDependencyInjection(appConfig: AppConfig) {
    val coreModule = module {
        single<AppConfig> { appConfig }
    }

    install(Koin) {
        modules(coreModule, appModule, redisModule(appConfig, RedisNamespace.Auth))
    }
}