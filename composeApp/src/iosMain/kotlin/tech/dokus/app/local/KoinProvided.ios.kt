package tech.dokus.app.local

import androidx.compose.runtime.Composable
import org.koin.compose.KoinApplication
import org.koin.core.module.Module

/**
 * iOS-specific implementation of KoinProvided.
 * Initializes Koin within the Compose context and handles async resource initialization.
 */
@Composable
actual fun KoinProvided(
    modules: List<Module>,
    content: @Composable () -> Unit
) {
    KoinApplication(
        application = {
            modules(modules)
        }
    ) {
        content()
    }
}
