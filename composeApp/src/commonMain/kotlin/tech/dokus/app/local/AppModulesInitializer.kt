package tech.dokus.app.local

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import tech.dokus.foundation.app.AppModule

/**
 * Handles database and async resource initialization for all app modules.
 * Shows content only after initialization is complete.
 */
@Composable
fun AppModulesInitializer(
    modules: List<AppModule>,
    content: @Composable () -> Unit
) {
    var isInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        modules.forEach { module ->
            module.initializeData()
        }
        isInitialized = true
    }

    if (isInitialized) {
        content()
    } else {
        // TODO: Splash
    }
}
