package ai.dokus.app.media.di

import ai.dokus.app.media.domain.MediaRepository
import ai.dokus.app.media.network.ResilientMediaRemoteService
import ai.dokus.app.media.repository.MediaRepositoryImpl
import ai.dokus.foundation.domain.asbtractions.AuthManager
import ai.dokus.foundation.domain.asbtractions.TokenManager
import ai.dokus.foundation.domain.config.DokusEndpoint
import ai.dokus.foundation.domain.model.common.Feature
import ai.dokus.foundation.domain.rpc.MediaRemoteService
import ai.dokus.foundation.network.createAuthenticatedRpcClient
import ai.dokus.foundation.network.service
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.rpc.krpc.ktor.client.KtorRpcClient
import org.koin.core.qualifier.named
import org.koin.dsl.module

val mediaNetworkModule = module {
    factory<KtorRpcClient>(named(Feature.Media)) {
        createAuthenticatedRpcClient(
            endpoint = DokusEndpoint.Media,
            tokenManager = get<TokenManager>(),
            onAuthenticationFailed = {
                val authManager = get<AuthManager>()
                CoroutineScope(Dispatchers.Default).launch {
                    authManager.onAuthenticationFailed()
                }
            },
            waitForServices = false
        )
    }

    single<MediaRemoteService> {
        ResilientMediaRemoteService(
            serviceProvider = {
                val rpcClient = get<KtorRpcClient>(named(Feature.Media))
                rpcClient.service<MediaRemoteService>()
            },
            tokenManager = get(),
            authManager = get()
        )
    }
}

val mediaDataModule = module {
    includes(mediaNetworkModule)

    single<MediaRepository> {
        MediaRepositoryImpl(
            remoteService = get()
        )
    }
}
