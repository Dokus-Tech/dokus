package ai.dokus.foundation.database.configuration

import ai.dokus.foundation.database.services.AttachmentServiceImpl
import ai.dokus.foundation.database.services.AuditServiceImpl
import ai.dokus.foundation.database.services.ClientServiceImpl
import ai.dokus.foundation.database.services.ExpenseServiceImpl
import ai.dokus.foundation.database.services.InvoiceServiceImpl
import ai.dokus.foundation.database.services.PaymentServiceImpl
import ai.dokus.foundation.database.services.TenantServiceImpl
import ai.dokus.foundation.database.services.UserServiceImpl
import ai.dokus.foundation.database.storage.FileStorage
import ai.dokus.foundation.database.storage.LocalFileStorage
import ai.dokus.foundation.database.utils.DatabaseFactory
import ai.dokus.foundation.ktor.AppBaseConfig
import ai.dokus.foundation.ktor.StorageConfig
import ai.dokus.foundation.ktor.cache.RedisNamespace
import ai.dokus.foundation.ktor.cache.redisModule
import ai.dokus.foundation.ktor.crypto.PasswordCryptoService
import ai.dokus.foundation.ktor.crypto.PasswordCryptoService4j
import ai.dokus.foundation.ktor.services.AttachmentService
import ai.dokus.foundation.ktor.services.ClientService
import ai.dokus.foundation.ktor.services.ExpenseService
import ai.dokus.foundation.ktor.services.InvoiceService
import ai.dokus.foundation.ktor.services.PaymentService
import ai.dokus.foundation.ktor.services.TenantService
import ai.dokus.foundation.ktor.services.UserService
import io.ktor.server.application.Application
import io.ktor.server.application.install
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

fun appModule() = module {
    single { DatabaseFactory(get<AppBaseConfig>(), "dokus-database-pool") }

    // Password crypto service
    single<PasswordCryptoService> { PasswordCryptoService4j() }

    // Audit Service
    single { AuditServiceImpl() }

    // File Storage
    single<FileStorage> { LocalFileStorage(get<StorageConfig>().directory) }

    // RPC Service Implementations
    single<TenantService> { TenantServiceImpl() }
    single<UserService> { UserServiceImpl(get()) }
    single<ClientService> { ClientServiceImpl(get()) }
    single<InvoiceService> { InvoiceServiceImpl(get(), get()) }
    single<ExpenseService> { ExpenseServiceImpl(get()) }
    single<PaymentService> { PaymentServiceImpl(get()) }
    single<AttachmentService> { AttachmentServiceImpl(get()) }
}

fun Application.configureDependencyInjection(
    appConfig: AppBaseConfig,
) {
    val coreModule = module {
        single<AppBaseConfig> { appConfig }
        single<StorageConfig> { StorageConfig.load(appConfig) }
    }

    install(Koin) {
        modules(
            coreModule,
            appModule(),
            redisModule(appConfig, RedisNamespace.Auth)
        )
    }
}