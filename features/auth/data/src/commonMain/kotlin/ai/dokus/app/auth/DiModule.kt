package ai.dokus.app.auth

import ai.dokus.app.auth.database.AuthDb
import ai.dokus.app.auth.domain.AccountRemoteService
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
import ai.dokus.foundation.domain.config.DokusEndpoint
import ai.dokus.foundation.domain.model.common.Feature
import ai.dokus.foundation.sstorage.SecureStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.http.URLProtocol
import kotlinx.rpc.RpcClient
import kotlinx.rpc.krpc.ktor.client.Krpc
import kotlinx.rpc.krpc.ktor.client.rpc
import kotlinx.rpc.krpc.ktor.client.rpcConfig
import kotlinx.rpc.krpc.serialization.json.json
import kotlinx.rpc.withService
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
    // HTTP client with WebSockets and KotlinX RPC support
    single<HttpClient>(Qualifiers.httpClientAuth) {
        HttpClient(CIO) {
            install(WebSockets)
            install(Krpc)
        }
    }

    // RPC client for Auth service
    single<RpcClient>(named("authClient")) {
        val httpClient = get<HttpClient>(Qualifiers.httpClientAuth)
        val endpoint = DokusEndpoint.Auth
        httpClient.rpc {
            url {
                protocol = URLProtocol.WS
                host = endpoint.host
                port = endpoint.port
                appendPathSegments("api")
            }
            rpcConfig {
                serialization {
                    json()
                }
            }
        }
    }

    // AccountRemoteService proxy via RPC
    single<AccountRemoteService> {
        get<RpcClient>(named("authClient")).withService()
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