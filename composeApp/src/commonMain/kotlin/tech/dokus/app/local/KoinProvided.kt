package tech.dokus.app.local

import androidx.compose.runtime.Composable
import org.koin.core.module.Module

/**
 * Provides Koin dependency injection context to the composition.
 * Platform-specific implementations handle the initialization appropriately.
 */
@Composable
expect fun KoinProvided(
    modules: List<Module>,
    content: @Composable () -> Unit
)