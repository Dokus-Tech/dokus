package tech.dokus.features.auth

import tech.dokus.features.auth.database.AuthDatabase
import tech.dokus.features.auth.database.AuthDb
import tech.dokus.features.auth.datasource.AccountRemoteDataSource
import tech.dokus.features.auth.datasource.AccountRemoteDataSourceImpl
import tech.dokus.features.auth.datasource.IdentityRemoteDataSource
import tech.dokus.features.auth.datasource.IdentityRemoteDataSourceImpl
import tech.dokus.features.auth.datasource.LookupRemoteDataSource
import tech.dokus.features.auth.datasource.LookupRemoteDataSourceImpl
import tech.dokus.features.auth.datasource.TeamRemoteDataSource
import tech.dokus.features.auth.datasource.TeamRemoteDataSourceImpl
import tech.dokus.features.auth.datasource.TenantRemoteDataSource
import tech.dokus.features.auth.datasource.TenantRemoteDataSourceImpl
import tech.dokus.features.auth.manager.AuthManagerImpl
import tech.dokus.features.auth.manager.AuthManagerMutable
import tech.dokus.features.auth.manager.TokenManagerImpl
import tech.dokus.features.auth.manager.TokenManagerMutable
import tech.dokus.features.auth.repository.AuthRepository
import tech.dokus.features.auth.storage.TokenStorage
import tech.dokus.features.auth.usecases.CheckAccountUseCase
import tech.dokus.features.auth.usecases.ConnectToServerUseCase
import tech.dokus.features.auth.usecases.GetCurrentTenantIdUseCase
import tech.dokus.features.auth.usecases.GetCurrentTenantIdUseCaseImpl
import tech.dokus.features.auth.usecases.GetCurrentTenantUseCase
import tech.dokus.features.auth.usecases.GetCurrentTenantUseCaseImpl
import tech.dokus.features.auth.usecases.LoginUseCase
import tech.dokus.features.auth.usecases.LogoutUseCase
import tech.dokus.features.auth.usecases.RegisterAndLoginUseCase
import tech.dokus.features.auth.usecases.SearchCompanyUseCaseImpl
import tech.dokus.features.auth.usecases.SelectTenantUseCase
import tech.dokus.features.auth.usecases.SelectTenantUseCaseImpl
import tech.dokus.features.auth.usecases.ValidateServerUseCase
import tech.dokus.domain.usecases.SearchCompanyUseCase
import tech.dokus.features.auth.utils.JwtDecoder
import tech.dokus.domain.asbtractions.AuthManager
import tech.dokus.domain.asbtractions.TokenManager
import tech.dokus.domain.model.common.Feature
import tech.dokus.foundation.sstorage.SecureStorage
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

    // Company lookup use case (CBE API)
    singleOf(::SearchCompanyUseCaseImpl) bind SearchCompanyUseCase::class
}
