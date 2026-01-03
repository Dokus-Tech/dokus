package tech.dokus.app.local

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import tech.dokus.foundation.app.AppModule
import tech.dokus.foundation.app.local.LocalAppModules

@Composable
fun AppModulesProvided(modules: List<AppModule>, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAppModules provides modules) {
        content()
    }
}
