package ai.dokus.app.auth

import ai.dokus.app.auth.database.AuthDb
import ai.dokus.app.auth.domain.AccountRemoteService
import ai.dokus.app.auth.manager.AuthManagerImpl
import ai.dokus.app.auth.manager.AuthManagerMutable
import ai.dokus.app.auth.manager.TokenManagerImpl
import ai.dokus.app.auth.manager.TokenManagerMutable
import ai.dokus.app.auth.network.MockAccountRemoteService
import ai.dokus.app.auth.repository.AuthRepository
import ai.dokus.app.auth.storage.TokenStorage
import ai.dokus.app.auth.usecases.CheckAccountUseCase
import ai.dokus.app.auth.usecases.LoginUseCase
import ai.dokus.app.auth.usecases.LogoutUseCase
import ai.dokus.app.auth.usecases.RegisterAndLoginUseCase
import ai.dokus.app.auth.utils.JwtDecoder
import ai.dokus.foundation.domain.asbtractions.AuthManager
import ai.dokus.foundation.domain.asbtractions.TokenManager
import ai.dokus.foundation.domain.config.DokusEndpoint
import ai.dokus.foundation.domain.model.common.Feature
import ai.dokus.foundation.domain.rpc.OrganizationRemoteService
import ai.dokus.foundation.network.createAuthenticatedHttpClient
import ai.dokus.foundation.network.createAuthenticatedRpcClient
import ai.dokus.foundation.network.createBaseHttpClient
import ai.dokus.foundation.network.or
import ai.dokus.foundation.network.resilient.ResilientOrganizationRemoteService
import ai.dokus.foundation.network.service
import ai.dokus.foundation.sstorage.SecureStorage
import io.ktor.client.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.rpc.krpc.ktor.client.KtorRpcClient
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
    // HTTP client without authentication (for login/register)
    single<HttpClient>(Qualifiers.httpClientNoAuth) {
        createBaseHttpClient(dokusEndpoint = DokusEndpoint.Auth)
    }

    // HTTP client with authentication (for authenticated endpoints)
    single<HttpClient>(Qualifiers.httpClientAuth) {
        createAuthenticatedHttpClient(
            dokusEndpoint = DokusEndpoint.Auth,
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

    // RPC client (nullable - graceful degradation)
    factory<KtorRpcClient?>(named(Feature.Auth)) {
        val tokenManager = get<TokenManagerMutable>()
        val authManager = get<AuthManagerMutable>()
        createAuthenticatedRpcClient(
            endpoint = DokusEndpoint.Auth,
            tokenManager = tokenManager,
            onAuthenticationFailed = {
                CoroutineScope(Dispatchers.Default).launch {
                    tokenManager.onAuthenticationFailed()
                    authManager.onAuthenticationFailed()
                }
            }
        )
    }

    // AccountRemoteService with stub fallback
    single<AccountRemoteService> {
        val rpcClient = getOrNull<KtorRpcClient>(named(Feature.Auth))
        rpcClient?.service<AccountRemoteService>() or MockAccountRemoteService()
    }

    // OrganizationRemoteService (authenticated)
    single<OrganizationRemoteService> {
        val rpcClient = get<KtorRpcClient>(named(Feature.Auth))
        ResilientOrganizationRemoteService {
            rpcClient.service<OrganizationRemoteService>()
        }
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
            accountService = get(), // Provided by RPC client configuration
            organizationRemoteService = get()
        )
    }
}

val authDomainModule = module {
    single { LoginUseCase(get()) }
    single { RegisterAndLoginUseCase(get()) }
    single { LogoutUseCase(get()) }
    single { CheckAccountUseCase() }
}
