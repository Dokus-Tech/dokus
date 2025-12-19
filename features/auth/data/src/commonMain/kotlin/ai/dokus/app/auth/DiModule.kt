package ai.dokus.app.auth

import ai.dokus.app.auth.database.AuthDb
import ai.dokus.app.auth.datasource.AccountRemoteDataSource
import ai.dokus.app.auth.datasource.AccountRemoteDataSourceImpl
import ai.dokus.app.auth.datasource.IdentityRemoteDataSource
import ai.dokus.app.auth.datasource.IdentityRemoteDataSourceImpl
import ai.dokus.app.auth.datasource.TeamRemoteDataSource
import ai.dokus.app.auth.datasource.TeamRemoteDataSourceImpl
import ai.dokus.app.auth.datasource.TenantRemoteDataSource
import ai.dokus.app.auth.datasource.TenantRemoteDataSourceImpl
import ai.dokus.app.auth.manager.AuthManagerImpl
import ai.dokus.app.auth.manager.AuthManagerMutable
import ai.dokus.app.auth.manager.TokenManagerImpl
import ai.dokus.app.auth.manager.TokenManagerMutable
import ai.dokus.app.auth.repository.AuthRepository
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
import ai.dokus.app.core.database.LocalDatabaseCleaner
import ai.dokus.foundation.domain.asbtractions.AuthManager
import ai.dokus.foundation.domain.asbtractions.TokenManager
import ai.dokus.foundation.domain.config.DynamicDokusEndpointProvider
import ai.dokus.foundation.domain.config.ServerConfigManager
import ai.dokus.foundation.domain.model.common.Feature
import ai.dokus.foundation.platform.Persistence
import ai.dokus.foundation.sstorage.SecureStorage
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.module.Module
import org.koin.core.qualifier.Qualifier
import org.koin.core.qualifier.named
import org.koin.core.qualifier.qualifier
import org.koin.dsl.binds
import org.koin.dsl.module
import tech.dokus.foundation.app.network.createDynamicAuthenticatedHttpClient
import tech.dokus.foundation.app.network.createDynamicBaseHttpClient

internal object Qualifiers {
    val secureStorageAuth: Qualifier = qualifier(Feature.Auth)
    val httpClientAuth: Qualifier = named("http_client_auth")
    val httpClientNoAuth: Qualifier = named("http_client_no_auth")
}

expect val authPlatformModule: Module

val authNetworkModule = module {
    // HTTP client without authentication (for login/register)
    // Uses factory pattern for dynamic server support - new client created on each injection
    factory<HttpClient>(Qualifiers.httpClientNoAuth) {
        createDynamicBaseHttpClient(
            endpointProvider = get<DynamicDokusEndpointProvider>()
        )
    }

    // HTTP client with authentication (for authenticated endpoints)
    // Uses factory pattern for dynamic server support - new client created on each injection
    factory<HttpClient>(Qualifiers.httpClientAuth) {
        createDynamicAuthenticatedHttpClient(
            endpointProvider = get<DynamicDokusEndpointProvider>(),
            tokenManager = get<TokenManagerMutable>(),
            onAuthenticationFailed = {
                val authManager = get<AuthManagerMutable>()
                val tokenManager = get<TokenManagerMutable>()
                CoroutineScope(Dispatchers.Default).launch {
                    tokenManager.onAuthenticationFailed()
                    authManager.onAuthenticationFailed()
                }
            }
        )
    }

    // IdentityRemoteDataSource (unauthenticated - uses httpClientNoAuth)
    single<IdentityRemoteDataSource> {
        IdentityRemoteDataSourceImpl(
            httpClient = get<HttpClient>(Qualifiers.httpClientNoAuth)
        )
    }

    // AccountRemoteDataSource (authenticated)
    single<AccountRemoteDataSource> {
        AccountRemoteDataSourceImpl(
            httpClient = get<HttpClient>(Qualifiers.httpClientAuth)
        )
    }

    // TenantRemoteDataSource (authenticated)
    single<TenantRemoteDataSource> {
        TenantRemoteDataSourceImpl(
            httpClient = get<HttpClient>(Qualifiers.httpClientAuth)
        )
    }

    // TeamRemoteDataSource (authenticated)
    single<TeamRemoteDataSource> {
        TeamRemoteDataSourceImpl(
            httpClient = get<HttpClient>(Qualifiers.httpClientAuth)
        )
    }
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
            accountDataSource = get<AccountRemoteDataSource>(),
            identityDataSource = get<IdentityRemoteDataSource>(),
            tenantDataSource = get<TenantRemoteDataSource>()
        )
    }
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
