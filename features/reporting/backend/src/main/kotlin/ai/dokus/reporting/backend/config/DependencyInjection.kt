package ai.dokus.reporting.backend.config

import ai.dokus.foundation.ktor.AppConfig
import ai.dokus.foundation.ktor.cache.RedisNamespace
import ai.dokus.foundation.ktor.cache.redisModule
import io.ktor.server.application.*
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

private val appModule = module {
    // Reporting-specific business logic services will go here
}

fun Application.configureDependencyInjection(appConfig: AppConfig) {
    val coreModule = module {
        single<AppConfig> { appConfig }
    }

    install(Koin) {
        modules(coreModule, appModule, redisModule(appConfig, RedisNamespace.Reporting), rpcClientModule)
    }
}
