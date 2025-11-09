package ai.dokus.app.local

import ai.dokus.app.core.local.LocalAppModules
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    var isInitialized by remember { mutableStateOf(false) }

    KoinApplication(
        application = {
            modules(modules)
        }
    ) {
        val appModules = LocalAppModules.current

        LaunchedEffect(Unit) {
            appModules.forEach { module ->
                module.initializeData()
            }
            isInitialized = true
        }

        if (isInitialized) {
            content()
        }
    }
}