package ai.dokus.app.auth

import ai.dokus.app.auth.database.AuthDb
import ai.dokus.app.auth.datasource.AccountRemoteDataSource
import ai.dokus.app.auth.datasource.AccountRemoteDataSourceImpl
import ai.dokus.app.auth.datasource.IdentityRemoteDataSource
import ai.dokus.app.auth.datasource.IdentityRemoteDataSourceImpl
import ai.dokus.app.auth.datasource.LookupRemoteDataSource
import ai.dokus.app.auth.datasource.LookupRemoteDataSourceImpl
import ai.dokus.app.auth.datasource.TeamRemoteDataSource
import ai.dokus.app.auth.datasource.TeamRemoteDataSourceImpl
import ai.dokus.app.auth.datasource.TenantRemoteDataSource
import ai.dokus.app.auth.datasource.TenantRemoteDataSourceImpl
import ai.dokus.app.auth.manager.AuthManagerImpl
import ai.dokus.app.auth.manager.AuthManagerMutable
import ai.dokus.app.auth.manager.TokenManagerImpl
import ai.dokus.app.auth.manager.TokenManagerMutable
import ai.dokus.app.auth.repository.AuthRepository
import ai.dokus.app.auth.repository.LookupRepository
import ai.dokus.app.auth.storage.TokenStorage
import ai.dokus.app.auth.usecases.CheckAccountUseCase
import ai.dokus.app.auth.usecases.ConnectToServerUseCase
import ai.dokus.app.auth.usecases.GetCurrentTenantUseCase
import ai.dokus.app.auth.usecases.GetCurrentTenantUseCaseImpl
import ai.dokus.app.auth.usecases.LoginUseCase
import ai.dokus.app.auth.usecases.LogoutUseCase
import ai.dokus.app.auth.usecases.RegisterAndLoginUseCase
import ai.dokus.app.auth.usecases.SelectTenantUseCase
import ai.dokus.app.auth.usecases.SelectTenantUseCaseImpl
import ai.dokus.app.auth.usecases.ValidateServerUseCase
import ai.dokus.app.auth.utils.JwtDecoder
import ai.dokus.foundation.domain.asbtractions.AuthManager
import ai.dokus.foundation.domain.asbtractions.TokenManager
import ai.dokus.foundation.domain.config.ServerConfigManager
import ai.dokus.foundation.domain.model.common.Feature
import ai.dokus.foundation.platform.Persistence
import ai.dokus.foundation.sstorage.SecureStorage
import io.ktor.client.HttpClient
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.Qualifier
import org.koin.core.qualifier.qualifier
import org.koin.dsl.binds
import org.koin.dsl.module
import tech.dokus.foundation.app.SharedQualifiers
import tech.dokus.foundation.app.database.LocalDatabaseCleaner

internal object Qualifiers {
    val secureStorageAuth: Qualifier = qualifier(Feature.Auth)
}

expect val authPlatformModule: Module

val authNetworkModule = module {
    // IdentityRemoteDataSource (unauthenticated - uses httpClientNoAuth)
    single<IdentityRemoteDataSource> {
        IdentityRemoteDataSourceImpl(
            httpClient = get<HttpClient>(SharedQualifiers.httpClientNoAuth)
        )
    }

    // AccountRemoteDataSource (authenticated)
    single<AccountRemoteDataSource> {
        AccountRemoteDataSourceImpl(
            httpClient = get<HttpClient>()
        )
    }

    // TenantRemoteDataSource (authenticated)
    single<TenantRemoteDataSource> {
        TenantRemoteDataSourceImpl(
            httpClient = get<HttpClient>()
        )
    }

    // TeamRemoteDataSource (authenticated)
    single<TeamRemoteDataSource> {
        TeamRemoteDataSourceImpl(
            httpClient = get<HttpClient>()
        )
    }

    // LookupRemoteDataSource (authenticated)
    single<LookupRemoteDataSource> {
        LookupRemoteDataSourceImpl(
            httpClient = get<HttpClient>()
        )
    }
}

val authDataModule = module {
    // Database
    single { AuthDb.create() }
    single { get<AuthDb>().get() }

    singleOf(::AuthManagerImpl) binds arrayOf(AuthManager::class, AuthManagerMutable::class)

    // JWT utilities
    singleOf(::JwtDecoder)

    // Token storage and management
    single<TokenStorage> {
        TokenStorage(get<SecureStorage>(Qualifiers.secureStorageAuth))
    }
    singleOf(::TokenManagerImpl) binds arrayOf(TokenManager::class, TokenManagerMutable::class)

    // Repositories
    single<AuthRepository> {
        AuthRepository(
            tokenManager = get<TokenManagerMutable>(),
            authManager = get<AuthManagerMutable>(),
            accountDataSource = get<AccountRemoteDataSource>(),
            identityDataSource = get<IdentityRemoteDataSource>(),
            tenantDataSource = get<TenantRemoteDataSource>()
        )
    }

    // Lookup repository for CBE company search
    single { LookupRepository(get<LookupRemoteDataSource>()) }
}

val authDomainModule = module {
    single { LoginUseCase(get()) }
    single { RegisterAndLoginUseCase(get()) }
    single { LogoutUseCase(get(), get<LocalDatabaseCleaner>()) }
    single { CheckAccountUseCase() }
    single<GetCurrentTenantUseCase> {
        GetCurrentTenantUseCaseImpl(
            get<TokenManager>(),
            get<TenantRemoteDataSource>()
        )
    }
    single<SelectTenantUseCase> { SelectTenantUseCaseImpl(get<AuthRepository>()) }

    // Server connection use cases
    single { ValidateServerUseCase() }
    single {
        ConnectToServerUseCase(
            validateServer = get<ValidateServerUseCase>(),
            serverConfigManager = get<ServerConfigManager>(),
            tokenStorage = get<TokenStorage>(),
            persistence = get<Persistence>()
        )
    }
}
