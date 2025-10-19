package ai.dokus.foundation.database.configuration

import ai.dokus.foundation.database.services.*
import ai.dokus.foundation.database.storage.FileStorage
import ai.dokus.foundation.database.storage.LocalFileStorage
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

fun appModule(appConfig: AppConfig) = module {
    single { DatabaseFactory(appConfig, "dokus-database-pool") }

    // Password crypto service
    single<PasswordCryptoService> { PasswordCryptoService4j() }

    // Audit Service
    single { AuditServiceImpl() }

    // File Storage
    single<FileStorage> { LocalFileStorage(appConfig.storage.directory) }

    // RPC Service Implementations
    single<TenantService> { TenantServiceImpl() }
    single<UserService> { UserServiceImpl(get()) }
    single<ClientService> { ClientServiceImpl(get()) }
    single<InvoiceService> { InvoiceServiceImpl(get(), get()) }
    single<ExpenseService> { ExpenseServiceImpl(get()) }
    single<PaymentService> { PaymentServiceImpl(get()) }
    single<AttachmentService> { AttachmentServiceImpl(get()) }
}

fun Application.configureDependencyInjection(appConfig: AppConfig) {
    val coreModule = module {
        single<AppConfig> { appConfig }
    }

    install(Koin) {
        modules(coreModule, appModule(appConfig), redisModule(appConfig, RedisNamespace.Auth))
    }
}