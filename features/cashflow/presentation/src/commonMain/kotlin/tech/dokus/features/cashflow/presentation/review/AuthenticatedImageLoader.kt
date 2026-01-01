package tech.dokus.features.cashflow.presentation.review

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.compose.LocalPlatformContext
import coil3.memory.MemoryCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import io.ktor.client.HttpClient
import org.koin.compose.koinInject

/**
 * Creates a remembered [ImageLoader] configured for authenticated image requests.
 *
 * Uses the Koin-injected [HttpClient] which has [DynamicBearerAuthPlugin] configured,
 * so all image requests will automatically include JWT authentication headers.
 *
 * The ImageLoader is configured with:
 * - Memory cache (25% of available memory)
 * - Ktor network fetcher for authenticated HTTP requests
 *
 * @return An [ImageLoader] configured for authenticated image loading
 */
@Composable
fun rememberAuthenticatedImageLoader(): ImageLoader {
    val httpClient = koinInject<HttpClient>()
    val context = LocalPlatformContext.current

    return remember(httpClient) {
        createAuthenticatedImageLoader(context, httpClient)
    }
}

/**
 * Creates an [ImageLoader] with the given [HttpClient] for authenticated requests.
 */
private fun createAuthenticatedImageLoader(
    context: PlatformContext,
    httpClient: HttpClient
): ImageLoader {
    return ImageLoader.Builder(context)
        .components {
            add(KtorNetworkFetcherFactory(httpClient))
        }
        .memoryCache {
            MemoryCache.Builder()
                .maxSizePercent(context, 0.25)
                .build()
        }
        .build()
}
