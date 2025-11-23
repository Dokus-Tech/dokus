package ai.dokus.audit.backend.config

import ai.dokus.audit.backend.database.services.AuditServiceImpl
import ai.dokus.audit.backend.database.tables.AuditLogsTable
import ai.dokus.foundation.ktor.cache.RedisNamespace
import ai.dokus.foundation.ktor.cache.redisModule
import ai.dokus.foundation.ktor.config.AppBaseConfig
import ai.dokus.foundation.ktor.database.DatabaseFactory
import ai.dokus.foundation.ktor.security.JwtValidator
import ai.dokus.foundation.ktor.services.AuditService
import io.ktor.server.application.*
import kotlinx.coroutines.runBlocking
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

private val appModule = module {
    // Database
    single {
        DatabaseFactory(get(), "audit-pool").apply {
            runBlocking {
                init(AuditLogsTable)
            }
        }
    }

    // Local database services
    single<AuditService> { AuditServiceImpl() }
}

fun Application.configureDependencyInjection(appConfig: AppBaseConfig) {
    val coreModule = module {
        single<AppBaseConfig> { appConfig }

        // JWT validator for local token validation
        single {
            JwtValidator(appConfig.jwt)
        }
    }

    install(Koin) {
        modules(coreModule, appModule, redisModule(appConfig, RedisNamespace.Audit))
    }
}
