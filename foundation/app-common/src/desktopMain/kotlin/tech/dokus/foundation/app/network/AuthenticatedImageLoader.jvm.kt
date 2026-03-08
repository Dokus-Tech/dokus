package tech.dokus.foundation.app.network

import coil3.ImageLoader
import coil3.PlatformContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val imageLoaderModule: Module = module {
    single<ImageLoader> {
        createAuthenticatedImageLoader(
            context = PlatformContext.INSTANCE,
            httpClient = get(),
        )
    }
}
