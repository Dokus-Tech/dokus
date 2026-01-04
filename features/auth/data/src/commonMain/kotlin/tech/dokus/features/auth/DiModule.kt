package tech.dokus.features.auth

import io.ktor.client.HttpClient
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.Qualifier
import org.koin.core.qualifier.qualifier
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module
import tech.dokus.domain.asbtractions.AuthManager
import tech.dokus.domain.asbtractions.TokenManager
import tech.dokus.domain.model.common.Feature
import tech.dokus.domain.usecases.SearchCompanyUseCase
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
import tech.dokus.features.auth.gateway.AuthGateway
import tech.dokus.features.auth.initializer.AuthDataInitializer
import tech.dokus.features.auth.repository.AuthRepository
import tech.dokus.features.auth.storage.TokenStorage
import tech.dokus.features.auth.usecases.AuthSessionUseCase
import tech.dokus.features.auth.usecases.AuthSessionUseCaseImpl
import tech.dokus.features.auth.usecases.ConnectToServerUseCase
import tech.dokus.features.auth.usecases.ConnectToServerUseCaseImpl
import tech.dokus.features.auth.usecases.CreateTenantUseCase
import tech.dokus.features.auth.usecases.CreateTenantUseCaseImpl
import tech.dokus.features.auth.usecases.GetCurrentUserUseCase
import tech.dokus.features.auth.usecases.GetCurrentUserUseCaseImpl
import tech.dokus.features.auth.usecases.GetCurrentTenantIdUseCase
import tech.dokus.features.auth.usecases.GetCurrentTenantIdUseCaseImpl
import tech.dokus.features.auth.usecases.GetCurrentTenantUseCase
import tech.dokus.features.auth.usecases.GetCurrentTenantUseCaseImpl
import tech.dokus.features.auth.usecases.GetInvoiceNumberPreviewUseCase
import tech.dokus.features.auth.usecases.GetInvoiceNumberPreviewUseCaseImpl
import tech.dokus.features.auth.usecases.HasFreelancerTenantUseCase
import tech.dokus.features.auth.usecases.HasFreelancerTenantUseCaseImpl
import tech.dokus.features.auth.usecases.LoginUseCase
import tech.dokus.features.auth.usecases.LoginUseCaseImpl
import tech.dokus.features.auth.usecases.LogoutUseCase
import tech.dokus.features.auth.usecases.LogoutUseCaseImpl
import tech.dokus.features.auth.usecases.RegisterAndLoginUseCase
import tech.dokus.features.auth.usecases.RegisterAndLoginUseCaseImpl
import tech.dokus.features.auth.usecases.SearchCompanyUseCaseImpl
import tech.dokus.features.auth.usecases.SelectTenantUseCase
import tech.dokus.features.auth.usecases.SelectTenantUseCaseImpl
import tech.dokus.features.auth.usecases.ListMyTenantsUseCase
import tech.dokus.features.auth.usecases.ListMyTenantsUseCaseImpl
import tech.dokus.features.auth.usecases.UpdateProfileUseCase
import tech.dokus.features.auth.usecases.UpdateProfileUseCaseImpl
import tech.dokus.features.auth.usecases.ValidateServerUseCase
import tech.dokus.features.auth.usecases.ValidateServerUseCaseImpl
import tech.dokus.features.auth.usecases.WorkspaceSettingsUseCase
import tech.dokus.features.auth.usecases.WorkspaceSettingsUseCaseImpl
import tech.dokus.features.auth.usecases.TeamSettingsUseCase
import tech.dokus.features.auth.usecases.TeamSettingsUseCaseImpl
import tech.dokus.features.auth.utils.JwtDecoder
import tech.dokus.foundation.app.AppDataInitializer
import tech.dokus.foundation.app.SharedQualifiers
import tech.dokus.foundation.sstorage.SecureStorage

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
    single { AuthDb.create() }
    single<AuthDatabase> { get<AuthDb>().get() }

    singleOf(::AuthManagerImpl) binds arrayOf(AuthManager::class, AuthManagerMutable::class)

    // JWT utilities
    singleOf(::JwtDecoder)

    // Token storage and management
    single<TokenStorage> {
        TokenStorage(get<SecureStorage>(Qualifiers.secureStorageAuth))
    }
    singleOf(::TokenManagerImpl) binds arrayOf(TokenManager::class, TokenManagerMutable::class)

    // Initialization
    singleOf(::AuthDataInitializer) bind AppDataInitializer::class

    // Repositories
    singleOf(::AuthRepository) bind AuthGateway::class
}

val authDomainModule = module {
    singleOf(::AuthSessionUseCaseImpl) bind AuthSessionUseCase::class
    singleOf(::LoginUseCaseImpl) bind LoginUseCase::class
    singleOf(::RegisterAndLoginUseCaseImpl) bind RegisterAndLoginUseCase::class
    singleOf(::LogoutUseCaseImpl) bind LogoutUseCase::class
    singleOf(::GetCurrentUserUseCaseImpl) bind GetCurrentUserUseCase::class
    singleOf(::UpdateProfileUseCaseImpl) bind UpdateProfileUseCase::class
    singleOf(::HasFreelancerTenantUseCaseImpl) bind HasFreelancerTenantUseCase::class
    singleOf(::CreateTenantUseCaseImpl) bind CreateTenantUseCase::class
    singleOf(::ListMyTenantsUseCaseImpl) bind ListMyTenantsUseCase::class
    singleOf(::GetInvoiceNumberPreviewUseCaseImpl) bind GetInvoiceNumberPreviewUseCase::class
    singleOf(::WorkspaceSettingsUseCaseImpl) bind WorkspaceSettingsUseCase::class
    singleOf(::TeamSettingsUseCaseImpl) bind TeamSettingsUseCase::class
    singleOf(::GetCurrentTenantUseCaseImpl) bind GetCurrentTenantUseCase::class
    singleOf(::GetCurrentTenantIdUseCaseImpl) bind GetCurrentTenantIdUseCase::class
    singleOf(::SelectTenantUseCaseImpl) bind SelectTenantUseCase::class

    // Server connection use cases
    singleOf(::ValidateServerUseCaseImpl) bind ValidateServerUseCase::class
    singleOf(::ConnectToServerUseCaseImpl) bind ConnectToServerUseCase::class

    // Company lookup use case (CBE API)
    singleOf(::SearchCompanyUseCaseImpl) bind SearchCompanyUseCase::class
}
