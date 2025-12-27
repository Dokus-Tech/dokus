package ai.dokus.app.auth

import ai.dokus.app.auth.database.AuthDatabase
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
import ai.dokus.app.auth.usecases.GetCurrentTenantIdUseCase
import ai.dokus.app.auth.usecases.GetCurrentTenantIdUseCaseImpl
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
import tech.dokus.domain.model.common.Feature
import ai.dokus.foundation.sstorage.SecureStorage
import io.ktor.client.HttpClient
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.Qualifier
import org.koin.core.qualifier.qualifier
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module
import tech.dokus.foundation.app.SharedQualifiers

internal object Qualifiers {
    val secureStorageAuth: Qualifier = qualifier(Feature.Auth)
}

expect val authPlatformModule: Module

val authNetworkModule = module {
    // Non Authenticated
    single {
        IdentityRemoteDataSourceImpl(get<HttpClient>(SharedQualifiers.httpClientNoAuth))
    } bind IdentityRemoteDataSource::class

    // Authenticated
    singleOf(::AccountRemoteDataSourceImpl) bind AccountRemoteDataSource::class
    singleOf(::TenantRemoteDataSourceImpl) bind TenantRemoteDataSource::class
    singleOf(::TeamRemoteDataSourceImpl) bind TeamRemoteDataSource::class
    singleOf(::LookupRemoteDataSourceImpl) bind LookupRemoteDataSource::class
}

val authDataModule = module {
    // Database
    singleOf(AuthDb::create) bind AuthDb::class
    single<AuthDatabase> { get<AuthDb>().get() }

    singleOf(::AuthManagerImpl) binds arrayOf(AuthManager::class, AuthManagerMutable::class)

    // JWT utilities
    singleOf(::JwtDecoder)

    // Token storage and management
    single<TokenStorage> {
        TokenStorage(get<SecureStorage>(Qualifiers.secureStorageAuth))
    }
    singleOf(::TokenManagerImpl) binds arrayOf(TokenManager::class, TokenManagerMutable::class)

    // Repositories
    singleOf(::AuthRepository)

    // Lookup repository for CBE company search
    singleOf(::LookupRepository)
}

val authDomainModule = module {
    singleOf(::LoginUseCase)
    singleOf(::RegisterAndLoginUseCase)
    singleOf(::LogoutUseCase)
    singleOf(::CheckAccountUseCase)
    singleOf(::GetCurrentTenantUseCaseImpl) bind GetCurrentTenantUseCase::class
    singleOf(::GetCurrentTenantIdUseCaseImpl) bind GetCurrentTenantIdUseCase::class
    singleOf(::SelectTenantUseCaseImpl) bind SelectTenantUseCase::class

    // Server connection use cases
    singleOf(::ValidateServerUseCase) bind ValidateServerUseCase::class
    singleOf(::ConnectToServerUseCase)
}
