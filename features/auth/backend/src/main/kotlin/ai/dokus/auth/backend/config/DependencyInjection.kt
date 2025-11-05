package ai.dokus.auth.backend.config

import ai.dokus.auth.backend.database.services.TenantServiceImpl
import ai.dokus.auth.backend.database.services.UserServiceImpl
import ai.dokus.auth.backend.database.tables.*
import ai.dokus.foundation.ktor.database.DatabaseFactory
import ai.dokus.auth.backend.services.*
import ai.dokus.foundation.domain.rpc.*
import ai.dokus.foundation.ktor.AppBaseConfig
import ai.dokus.foundation.ktor.cache.RedisNamespace
import ai.dokus.foundation.ktor.cache.redisModule
import ai.dokus.foundation.ktor.services.TenantService
import ai.dokus.foundation.ktor.services.UserService
import io.ktor.server.application.Application
import io.ktor.server.application.install
import kotlinx.coroutines.runBlocking
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

private val appModule = module {
    // Database
    single {
        DatabaseFactory(get(), "auth-pool").apply {
            runBlocking {
                init(TenantsTable, TenantSettingsTable, UsersTable, RefreshTokensTable)
            }
        }
    }

    // Password crypto service
    single<ai.dokus.foundation.ktor.crypto.PasswordCryptoService> {
        ai.dokus.foundation.ktor.crypto.PasswordCryptoService4j()
    }

    // Local database services
    single<TenantService> { TenantServiceImpl() }
    single<UserService> { UserServiceImpl(get()) }

    // RPC API implementations
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