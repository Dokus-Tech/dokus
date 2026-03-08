package tech.dokus.foundation.app.network

import coil3.ImageLoader
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val imageLoaderModule: Module = module {
    single<ImageLoader> {
        createAuthenticatedImageLoader(
            context = androidContext(),
            httpClient = get(),
        )
    }
}
