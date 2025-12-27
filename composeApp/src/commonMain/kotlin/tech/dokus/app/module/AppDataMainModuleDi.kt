package tech.dokus.app.module

import ai.dokus.app.auth.manager.AuthManagerMutable
import ai.dokus.app.auth.manager.TokenManagerMutable
import tech.dokus.domain.config.DynamicDokusEndpointProvider
import ai.dokus.foundation.platform.platformModule
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.dsl.module
import tech.dokus.foundation.app.AppDataModuleDi
import tech.dokus.foundation.app.SharedQualifiers
import tech.dokus.foundation.app.network.ServerConnectionMonitor
import tech.dokus.foundation.app.network.createDynamicAuthenticatedHttpClient
import tech.dokus.foundation.app.network.createDynamicBaseHttpClient

internal object AppDataMainModuleDi : AppDataModuleDi {
    override val platform = platformModule
    override val network = networkModule
    override val data = null
}

private val networkModule = module {
    // Connection monitor tracks connection state based on actual API request results
    single { ServerConnectionMonitor() }

    single<HttpClient>(SharedQualifiers.httpClientNoAuth) {
        createDynamicBaseHttpClient(
            endpointProvider = get<DynamicDokusEndpointProvider>(),
            connectionMonitor = get<ServerConnectionMonitor>()
        )
    }

    single<HttpClient> {
        createDynamicAuthenticatedHttpClient(
            endpointProvider = get<DynamicDokusEndpointProvider>(),
            tokenManager = get<TokenManagerMutable>(),
            connectionMonitor = get<ServerConnectionMonitor>(),
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
}
