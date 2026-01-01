package tech.dokus.features.auth

import tech.dokus.domain.model.common.Feature
import tech.dokus.foundation.sstorage.SecureStorage
import tech.dokus.foundation.sstorage.createSecureStorage
import org.koin.core.module.Module
import org.koin.dsl.module

actual val authPlatformModule: Module = module {
    single<SecureStorage>(Qualifiers.secureStorageAuth) { createSecureStorage(feature = Feature.Auth) }
}