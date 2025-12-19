package tech.dokus.app.local

import tech.dokus.foundation.app.AppModule
import tech.dokus.foundation.app.local.LocalAppModules
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun AppModulesProvided(modules: List<AppModule>, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAppModules provides modules) {
        content()
    }
}