package tech.dokus.app.local

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import tech.dokus.foundation.app.AppDataInitializer

/**
 * Handles database and async resource initialization for all app modules.
 * Shows content only after initialization is complete.
 */
@Composable
fun AppModulesInitializer(
    appDataInitializer: AppDataInitializer,
    content: @Composable () -> Unit
) {
    var isInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        appDataInitializer.initialize()
        isInitialized = true
    }

    if (isInitialized) {
        content()
    } else {
        // TODO: Splash
    }
}
