package tech.dokus.foundation.app.network

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalInspectionMode
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.compose.LocalPlatformContext
import coil3.memory.MemoryCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import io.ktor.client.HttpClient
import org.koin.compose.koinInject

private const val MemoryCacheSizePercent = 0.25

/**
 * Creates a remembered [ImageLoader] configured for authenticated image requests.
 *
 * Uses the Koin-injected [HttpClient] that already carries dynamic bearer auth and tenant headers.
 */
@Composable
fun rememberAuthenticatedImageLoader(): ImageLoader {
    val context = LocalPlatformContext.current
    if (LocalInspectionMode.current) {
        return remember { ImageLoader.Builder(context).build() }
    }
    val httpClient = koinInject<HttpClient>()
    return remember(httpClient) {
        createAuthenticatedImageLoader(context, httpClient)
    }
}

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
                .maxSizePercent(context, MemoryCacheSizePercent)
                .build()
        }
        .build()
}
