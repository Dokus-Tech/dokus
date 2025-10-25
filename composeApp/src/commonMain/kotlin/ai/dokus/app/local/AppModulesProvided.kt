package ai.dokus.app.local

import ai.dokus.app.core.AppModule
import ai.dokus.app.core.local.LocalAppModules
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun AppModulesProvided(modules: List<AppModule>, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAppModules provides modules) {
        content()
    }
}