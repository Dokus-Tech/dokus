package tech.dokus.features.auth

import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import tech.dokus.domain.model.common.Feature
import tech.dokus.foundation.sstorage.SecureStorage
import tech.dokus.foundation.sstorage.createSecureStorage

actual val authPlatformModule: Module = module {
    single<SecureStorage>(Qualifiers.secureStorageAuth) {
        createSecureStorage(
            context = androidContext(),
            feature = Feature.Auth
        )
    }
}
