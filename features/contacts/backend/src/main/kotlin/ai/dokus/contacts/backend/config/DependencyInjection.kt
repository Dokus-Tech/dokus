package ai.dokus.contacts.backend.config

import ai.dokus.contacts.backend.service.ContactMatchingService
import ai.dokus.contacts.backend.service.ContactNoteService
import ai.dokus.contacts.backend.service.ContactService
import ai.dokus.contacts.backend.database.ContactsTables
import ai.dokus.foundation.database.di.repositoryModuleContacts
import ai.dokus.foundation.ktor.config.AppBaseConfig
import ai.dokus.foundation.ktor.database.DatabaseFactory
import ai.dokus.foundation.ktor.security.JwtValidator
import io.ktor.server.application.*
import kotlinx.coroutines.runBlocking
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

fun Application.configureDependencyInjection(appConfig: AppBaseConfig) {
    install(Koin) {
        modules(
            coreModule(appConfig),
            databaseModule,
            repositoryModuleContacts,
            serviceModule
        )
    }
}

/**
 * Core module - provides base configuration and security
 */
fun coreModule(appConfig: AppBaseConfig) = module {
    single { appConfig }

    // JWT validator for local token validation
    single {
        JwtValidator(appConfig.jwt)
    }
}

/**
 * Database module - provides database factory and connection
 */
val databaseModule = module {
    single {
        DatabaseFactory(get(), "contacts-pool").apply {
            runBlocking {
                connect()
                ContactsTables.initialize()
            }
        }
    }
}

/**
 * Service module - business logic services
 */
val serviceModule = module {
    single { ContactService(get()) }
    single { ContactNoteService(get()) }
    single { ContactMatchingService(get()) }
}
