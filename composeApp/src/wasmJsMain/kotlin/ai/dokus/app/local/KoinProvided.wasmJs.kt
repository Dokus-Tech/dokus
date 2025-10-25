package ai.dokus.app.local

import androidx.compose.runtime.Composable
import org.koin.compose.KoinApplication
import org.koin.core.module.Module

/**
 * WasmJS-specific implementation of KoinProvided.
 * Initializes Koin within the Compose context.
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