package ai.dokus.app.local

import ai.dokus.app.core.AppModule
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

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
        Surface {}
    }
}
