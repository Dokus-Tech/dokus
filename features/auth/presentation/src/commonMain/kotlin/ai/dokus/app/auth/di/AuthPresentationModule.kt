package ai.dokus.app.auth.di

import ai.dokus.app.auth.AuthInitializer
import org.koin.dsl.module

val authPresentationModule = module {
    single<AuthInitializer> { AuthInitializer() }
}