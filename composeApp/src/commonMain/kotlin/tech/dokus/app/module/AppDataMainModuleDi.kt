package tech.dokus.app.module

import ai.dokus.app.auth.manager.AuthManagerMutable
import ai.dokus.app.auth.manager.TokenManagerMutable
import ai.dokus.foundation.domain.config.DynamicDokusEndpointProvider
import ai.dokus.foundation.platform.platformModule
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.dsl.module
import tech.dokus.foundation.app.AppDataModuleDi
import tech.dokus.foundation.app.SharedQualifiers
import tech.dokus.foundation.app.network.createDynamicAuthenticatedHttpClient
import tech.dokus.foundation.app.network.createDynamicBaseHttpClient

internal object AppDataMainModuleDi : AppDataModuleDi {
    override val platform = platformModule
    override val network = networkModule
    override val data = null
}

private val networkModule = module {
    single<HttpClient>(SharedQualifiers.httpClientNoAuth) {
        createDynamicBaseHttpClient(
            endpointProvider = get<DynamicDokusEndpointProvider>()
        )
    }

    single<HttpClient> {
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
}
