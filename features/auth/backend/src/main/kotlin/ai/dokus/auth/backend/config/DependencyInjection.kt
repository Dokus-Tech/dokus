package ai.dokus.auth.backend.config

import ai.dokus.foundation.ktor.AppConfig
import ai.dokus.foundation.ktor.cache.RedisNamespace
import ai.dokus.foundation.ktor.cache.redisModule
import ai.dokus.foundation.ktor.db.DatabaseFactory
import ai.dokus.foundation.ktor.db.repository.LoginAttemptRepository
import ai.dokus.foundation.ktor.db.repository.SessionRepository
import ai.dokus.foundation.ktor.db.repository.UserRepository
import io.ktor.server.application.Application
import io.ktor.server.application.install
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

private val appModule = module {
    single<DatabaseFactory> {
        DatabaseFactory(
            appConfig = get<AppConfig>(),
            poolName = "dokus-auth-pool"
        )
    }

    // Repositories
    single<UserRepository> { UserRepository() }
    single<SessionRepository> { SessionRepository() }
    single<LoginAttemptRepository> { LoginAttemptRepository() }
}

fun Application.configureDependencyInjection(appConfig: AppConfig) {
    val coreModule = module {
        single<AppConfig> { appConfig }
    }

    install(Koin) {
        modules(coreModule, appModule, redisModule(appConfig, RedisNamespace.Auth))
    }
}