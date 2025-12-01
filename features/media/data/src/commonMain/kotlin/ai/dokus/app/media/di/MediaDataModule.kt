package ai.dokus.app.media.di

import ai.dokus.app.media.datasource.MediaRemoteDataSource
import ai.dokus.app.media.datasource.MediaRemoteDataSourceImpl
import ai.dokus.app.media.repository.MediaRepositoryImpl
import ai.dokus.foundation.domain.asbtractions.AuthManager
import ai.dokus.foundation.domain.asbtractions.TokenManager
import ai.dokus.foundation.domain.config.DokusEndpoint
import ai.dokus.foundation.domain.repository.MediaRepository
import ai.dokus.foundation.network.createAuthenticatedHttpClient
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.qualifier.named
import org.koin.dsl.module

val mediaNetworkModule = module {
    // Authenticated HTTP client for Media service
    single<HttpClient>(named("media_http_client")) {
        createAuthenticatedHttpClient(
            dokusEndpoint = DokusEndpoint.Media,
            tokenManager = get<TokenManager>(),
            onAuthenticationFailed = {
                val authManager = get<AuthManager>()
                CoroutineScope(Dispatchers.Default).launch {
                    authManager.onAuthenticationFailed()
                }
            }
        )
    }

    // MediaRemoteDataSource using HTTP client
    single<MediaRemoteDataSource> {
        MediaRemoteDataSourceImpl(
            httpClient = get<HttpClient>(named("media_http_client"))
        )
    }
}

/**
 * Media data module providing repository implementation.
 *
 * The MediaRepository implementation is bound to the interface
 * from foundation/domain, allowing any module to inject it.
 */
val mediaDataModule = module {
    includes(mediaNetworkModule)

    single<MediaRepository> {
        MediaRepositoryImpl(
            remoteDataSource = get()
        )
    }
}
