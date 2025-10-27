package ai.dokus.app.auth

import ai.dokus.app.auth.database.DatabaseDriverFactory
import ai.dokus.foundation.domain.model.common.Feature
import ai.dokus.foundation.sstorage.SecureStorage
import ai.dokus.foundation.sstorage.createSecureStorage
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val authPlatformModule: Module = module {
    single<SecureStorage>(Qualifiers.secureStorageAuth) {
        createSecureStorage(
            context = androidContext(),
            feature = Feature.Auth
        )
    }

    // Database driver factory for Android
    single<DatabaseDriverFactory> {
        DatabaseDriverFactory(androidContext())
    }
}