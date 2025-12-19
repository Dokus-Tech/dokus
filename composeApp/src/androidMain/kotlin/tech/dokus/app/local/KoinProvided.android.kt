package tech.dokus.app.local

import androidx.compose.runtime.Composable
import org.koin.core.module.Module

/**
 * Android-specific implementation of KoinProvided.
 * Since Koin is initialized at Application level on Android,
 * this just provides the content without reinitializing.
 */
@Composable
actual fun KoinProvided(
    modules: List<Module>,
    content: @Composable () -> Unit
) {
    // On Android, Koin is already initialized in DokusApplication
    // Just provide the content directly
    content()
}