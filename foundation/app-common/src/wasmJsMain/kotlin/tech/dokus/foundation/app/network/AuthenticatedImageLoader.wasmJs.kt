@file:OptIn(coil3.annotation.ExperimentalCoilApi::class)

package tech.dokus.foundation.app.network

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.memory.MemoryCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import io.ktor.client.HttpClient
import org.koin.core.module.Module
import org.koin.dsl.module

actual val imageLoaderModule: Module = module {
    single<ImageLoader> {
        createAuthenticatedWasmImageLoader(
            context = PlatformContext.INSTANCE,
            httpClient = get(),
        )
    }
}

private fun createAuthenticatedWasmImageLoader(
    context: PlatformContext,
    httpClient: HttpClient,
): ImageLoader {
    return ImageLoader.Builder(context)
        .components {
            add(KtorNetworkFetcherFactory(httpClient))
            add(WasmSafeSkiaImageDecoder.Factory())
        }
        .memoryCache {
            MemoryCache.Builder()
                .maxSizePercent(context, MemoryCacheSizePercent)
                .build()
        }
        .diskCache(null)
        .build()
}
