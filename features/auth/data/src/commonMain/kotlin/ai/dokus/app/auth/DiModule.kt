package ai.dokus.app.auth

import ai.dokus.app.auth.database.AuthDb
import ai.dokus.app.auth.domain.AccountRemoteService
import ai.dokus.app.auth.network.MockAccountRemoteService
import ai.dokus.app.auth.manager.AuthManagerImpl
import ai.dokus.app.auth.manager.AuthManagerMutable
import ai.dokus.app.auth.manager.TokenManagerImpl
import ai.dokus.app.auth.manager.TokenManagerMutable
import ai.dokus.app.auth.repository.AuthRepository
import ai.dokus.app.auth.storage.TokenStorage
import ai.dokus.app.auth.usecases.CheckAccountUseCase
import ai.dokus.app.auth.usecases.LoginUseCase
import ai.dokus.app.auth.usecases.LogoutUseCase
import ai.dokus.app.auth.usecases.RegisterAndLoginUseCase
import ai.dokus.app.auth.utils.JwtDecoder
import ai.dokus.foundation.domain.asbtractions.AuthManager
import ai.dokus.foundation.domain.asbtractions.TokenManager
import ai.dokus.foundation.domain.model.common.Feature
import ai.dokus.foundation.sstorage.SecureStorage
import org.koin.core.module.Module
import org.koin.core.qualifier.Qualifier
import org.koin.core.qualifier.named
import org.koin.core.qualifier.qualifier
import org.koin.dsl.binds
import org.koin.dsl.module

internal object Qualifiers {
    val secureStorageAuth: Qualifier = qualifier(Feature.Auth)
    val httpClientAuth: Qualifier = named("http_client_auth")
    val httpClientNoAuth: Qualifier = named("http_client_no_auth")
}

expect val authPlatformModule: Module

val authNetworkModule = module {
    // TODO: Replace MockAccountRemoteService with actual RPC client
    // when backend is ready
    single<AccountRemoteService> { MockAccountRemoteService() }
}

val authDataModule = module {
    // Database
    single { AuthDb.create() }
    single { get<AuthDb>().get() }

    single { AuthManagerImpl() } binds arrayOf(AuthManager::class, AuthManagerMutable::class)

    // JWT utilities
    single<JwtDecoder> { JwtDecoder() }

    // Token storage and management
    single<TokenStorage> {
        TokenStorage(get<SecureStorage>(Qualifiers.secureStorageAuth))
    }
    single {
        TokenManagerImpl(
            get<TokenStorage>(),
            get<JwtDecoder>()
        )
    } binds arrayOf(TokenManager::class, TokenManagerMutable::class)

    // Repositories
    single<AuthRepository> {
        AuthRepository(
            tokenManager = get<TokenManagerMutable>(),
            authManager = get<AuthManagerMutable>(),
            accountService = get() // Provided by RPC client configuration
        )
    }
}

val authDomainModule = module {
    single { LoginUseCase(get()) }
    single { RegisterAndLoginUseCase(get()) }
    single { LogoutUseCase(get()) }
    single { CheckAccountUseCase() }
}