@file:OptIn(ExperimentalCoilApi::class)

package tech.dokus.foundation.app.network

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalInspectionMode
import coil3.annotation.ExperimentalCoilApi
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.compose.LocalPlatformContext
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import io.ktor.client.HttpClient
import okio.FileSystem
import org.koin.compose.koinInject
import org.koin.dsl.module

private const val MemoryCacheSizePercent = 0.25
private const val DiskCacheMaxSizeBytes = 256L * 1024 * 1024 // 256 MB

/**
 * Koin module that registers the authenticated [ImageLoader] as a singleton.
 * Provides memory cache (25% RAM) + disk cache (256 MB).
 */
val imageLoaderModule = module {
    single<ImageLoader> {
        createAuthenticatedImageLoader(
            context = coilPlatformContext(),
            httpClient = get(),
        )
    }
}

/**
 * Returns the Koin-managed [ImageLoader] singleton.
 * In preview/inspection mode, returns a basic loader instead.
 */
@Composable
fun rememberAuthenticatedImageLoader(): ImageLoader {
    if (LocalInspectionMode.current) {
        val context = LocalPlatformContext.current
        return remember { ImageLoader.Builder(context).build() }
    }
    return koinInject()
}

private fun createAuthenticatedImageLoader(
    context: PlatformContext,
    httpClient: HttpClient,
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
        .diskCache {
            DiskCache.Builder()
                .directory(FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "image_cache")
                .maxSizeBytes(DiskCacheMaxSizeBytes)
                .build()
        }
        .build()
}
